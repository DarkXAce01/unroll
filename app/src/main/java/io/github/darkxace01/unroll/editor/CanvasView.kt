package io.github.darkxace01.unroll.editor

import android.graphics.Bitmap
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.Item
import io.github.darkxace01.unroll.model.PhotoGeometry
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Point
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.photoUris
import io.github.darkxace01.unroll.model.RouteItem
import io.github.darkxace01.unroll.model.RouteMath
import io.github.darkxace01.unroll.model.Seams
import io.github.darkxace01.unroll.model.Snap
import io.github.darkxace01.unroll.model.TextItem
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Where the canvas sits on screen: canvas point × [scale] + [offset] = screen point. */
class Viewport {
    var scale by mutableFloatStateOf(0f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)

    fun toCanvas(o: Offset) = Offset((o.x - offsetX) / scale, (o.y - offsetY) / scale)
    fun toScreen(x: Float, y: Float) = Offset(x * scale + offsetX, y * scale + offsetY)

    /** The slide in the middle of the screen. */
    fun centreSlide(p: Project, screenW: Float) = p.slideAt(toCanvas(Offset(screenW / 2, 0f)).x)

    /** Keeps the canvas from being flung off screen. */
    fun clamp(p: Project, w: Float, h: Float) {
        val cw = p.width * scale
        val ch = p.height * scale
        offsetX = if (cw + 96 < w) (w - cw) / 2 else offsetX.coerceIn(w - cw - 48, 48f)
        offsetY = if (ch + 96 < h) (h - ch) / 2 else offsetY.coerceIn(h - ch - 48, 48f)
    }
}

private enum class Handle { Right, Bottom, Corner, Rotate }

/**
 * The editing surface. Drawn with the same [io.github.darkxace01.unroll.render.Renderer] as export.
 *
 * Gestures: tap selects (tap empty space to deselect), double-tap a photo to crop inside its frame.
 * One finger on the selected item moves it (snapping to cuts, slide centres and other items); elsewhere it
 * scrolls. Two fingers on the selected item resize and rotate it; elsewhere they zoom. Handles on the
 * selection stretch it (right, bottom), resize it keeping its shape (corner) or rotate it (top).
 */
@Composable
fun CanvasView(
    project: Project,
    selectedId: String?,
    croppingId: String?,
    notices: List<Notice>,
    showCuts: Boolean,
    snapping: Boolean,
    viewport: Viewport,
    onSelect: (String?) -> Unit,
    onCrop: (String?) -> Unit,
    onPreview: (Project) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    /** Draw tool on: a finger draws a line instead of moving things. */
    drawing: Boolean = false,
    onDrawn: (List<Point>) -> Unit = {},
    /** An empty template slot was tapped: pick a photo for it. */
    onEmptySlot: (String) -> Unit = {},
    /** A route's point was touched (so "remove point" knows which). */
    onPoint: (Int?) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.app
    val density = LocalDensity.current.density
    val bitmaps = remember { mutableStateMapOf<String, Bitmap>() }
    var guides by remember { mutableStateOf<List<Snap.Guide>>(emptyList()) }
    var ink by remember { mutableStateOf<List<Point>>(emptyList()) }

    // Screen-sized copies of each photo; full size is only for export.
    val uris = project.photoUris()
    LaunchedEffect(uris) {
        for (u in uris) if (u !in bitmaps) app.photos.load(u, 1600)?.let { bitmaps[u] = it }
    }

    val latest by rememberUpdatedState(project)
    val sel by rememberUpdatedState(selectedId)
    val crop by rememberUpdatedState(croppingId)
    val snapOn by rememberUpdatedState(snapping)
    val drawOn by rememberUpdatedState(drawing)

    val cutPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
        }
    }
    val linePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE } }
    val fillPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val labelPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }

    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        // First fit: the canvas's height fills the space, starting at the first slide.
        LaunchedEffect(project.id, project.ratio, w, h) {
            val fit = (h - 64 * density) / project.height
            val fitW = (w - 48 * density) / project.slideW
            viewport.scale = minOf(fit, fitW * 1.0f)
            viewport.offsetX = 24 * density
            viewport.offsetY = (h - project.height * viewport.scale) / 2
            viewport.clamp(project, w, h)
        }
        val handleR = 22 * density

        fun handles(item: Item): Map<Handle, Offset> {
            if (item is RouteItem) return emptyMap()
            val f = item.frame
            val isText = item is TextItem
            // Stickers keep their drawn shape (tape is the exception: it stretches).
            val fixedShape = item is io.github.darkxace01.unroll.model.StickerItem && io.github.darkxace01.unroll.model.Stickers.byId(item.sticker)?.kind != io.github.darkxace01.unroll.model.ArtKind.Tape
            val s = viewport.scale
            val local = buildMap {
                if (!fixedShape) put(Handle.Right, (f.w / 2) to 0f)
                if (!isText && !fixedShape) put(Handle.Bottom, 0f to (f.h / 2))
                put(Handle.Corner, (f.w / 2) to (f.h / 2))
                put(Handle.Rotate, 0f to (-f.h / 2 - 36 * density / s))
            }
            return local.mapValues { (_, xy) -> val pt = f.toCanvas(xy.first, xy.second); viewport.toScreen(pt.x, pt.y) }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(project.id, w, h) {
                    var lastTapAt = 0L
                    var lastTapId: String? = null
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val p0 = latest
                        val start = viewport.toCanvas(down.position)
                        if (drawOn) {
                            // Freehand: collect the finger's path in canvas units.
                            val pts = mutableListOf(Point(start.x, start.y))
                            while (true) {
                                val e = awaitPointerEvent()
                                val ch = e.changes.firstOrNull() ?: break
                                if (!ch.pressed) break
                                val c = viewport.toCanvas(ch.position)
                                pts += Point(c.x, c.y)
                                ink = pts.toList()
                                ch.consume()
                            }
                            ink = emptyList()
                            if (pts.size > 2) onDrawn(pts)
                            return@awaitEachGesture
                        }
                        val tolerance = 16 * density / viewport.scale
                        val selItem = sel?.let { p0.item(it) }
                        val handle = selItem?.let { item ->
                            handles(item).entries.firstOrNull { (_, o) -> hypot(o.x - down.position.x, o.y - down.position.y) < handleR }?.key
                        }
                        val pointIndex = (selItem as? RouteItem)?.points?.indexOfFirst { pt ->
                            val o = viewport.toScreen(pt.x, pt.y); hypot(o.x - down.position.x, o.y - down.position.y) < handleR
                        }?.takeIf { it >= 0 }
                        if (selItem is RouteItem) onPoint(pointIndex)
                        val onSelected = selItem != null && (handle != null || pointIndex != null || selItem.hits(start.x, start.y, tolerance))
                        val tapTarget = p0.hit(start.x, start.y, tolerance)
                        var working: Item? = selItem
                        var moved = false
                        var travel = 0f
                        var rotationAcc = 0f
                        val startRotation = selItem?.frame?.rotation ?: 0f
                        val cropMode = crop != null && crop == sel && onSelected && handle == null

                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.none { it.pressed }) break
                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            val rot = event.calculateRotation()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            travel += hypot(pan.x, pan.y)
                            if (travel < 6 * density && event.changes.size == 1) continue
                            moved = true
                            val s = viewport.scale
                            val item = working
                            when {
                                item == null || !onSelected -> {
                                    // Scroll / zoom the canvas.
                                    if (zoom != 1f && centroid != Offset.Unspecified) {
                                        val before = viewport.toCanvas(centroid)
                                        viewport.scale = (viewport.scale * zoom).coerceIn(0.05f, 3f)
                                        viewport.offsetX = centroid.x - before.x * viewport.scale
                                        viewport.offsetY = centroid.y - before.y * viewport.scale
                                    }
                                    viewport.offsetX += pan.x
                                    viewport.offsetY += pan.y
                                    viewport.clamp(latest, w, h)
                                }
                                pointIndex != null && item is RouteItem -> {
                                    val pos = viewport.toCanvas(event.changes.first().position)
                                    working = RouteMath.movePoint(item, pointIndex, Point(pos.x, pos.y))
                                    onPreview(latest.replace(working))
                                }
                                cropMode && item is PhotoItem -> {
                                    // Slide and zoom the photo inside its frame.
                                    val f = item.frame
                                    val r = Math.toRadians(-f.rotation.toDouble())
                                    val lx = (pan.x * cos(r) - pan.y * sin(r)).toFloat() / s
                                    val ly = (pan.x * sin(r) + pan.y * cos(r)).toFloat() / s
                                    val v = PhotoGeometry.visible(item)
                                    val vw = v.right - v.left
                                    val vh = v.bottom - v.top
                                    val dir = if (item.crop.flipX) -1f else 1f
                                    val c = item.crop
                                    val nz = (c.zoom * zoom).coerceIn(1f, 8f)
                                    val px = if (vw < 0.999f) c.panX - dir * lx / f.w * vw * 2 / (1 - vw) else c.panX
                                    val py = if (vh < 0.999f) c.panY - ly / f.h * vh * 2 / (1 - vh) else c.panY
                                    working = item.copy(crop = c.copy(zoom = nz, panX = px.coerceIn(-1f, 1f), panY = py.coerceIn(-1f, 1f)))
                                    onPreview(latest.replace(working))
                                }
                                handle != null -> {
                                    val pos = viewport.toCanvas(event.changes.first().position)
                                    val f = item.frame
                                    val local = f.toLocal(pos.x, pos.y)
                                    val minSide = 40f
                                    val nf = when (handle) {
                                        Handle.Right -> {
                                            // Left edge stays put.
                                            val nw = (local.x + f.w / 2).coerceAtLeast(minSide)
                                            val shift = f.toCanvas((nw - f.w) / 2, 0f)
                                            f.copy(cx = shift.x, cy = shift.y, w = nw)
                                        }
                                        Handle.Bottom -> {
                                            val nh = (local.y + f.h / 2).coerceAtLeast(minSide)
                                            val shift = f.toCanvas(0f, (nh - f.h) / 2)
                                            f.copy(cx = shift.x, cy = shift.y, h = nh)
                                        }
                                        Handle.Corner -> {
                                            // Keeps its shape, scales about the centre.
                                            val k = (hypot(local.x, local.y) / hypot(f.w / 2, f.h / 2)).coerceAtLeast(minSide / minOf(f.w, f.h))
                                            f.copy(w = f.w * k, h = f.h * k)
                                        }
                                        Handle.Rotate -> {
                                            val a = Math.toDegrees(kotlin.math.atan2((pos.x - f.cx).toDouble(), -(pos.y - f.cy).toDouble())).toFloat()
                                            f.copy(rotation = if (snapOn) Snap.rotation(a) else a)
                                        }
                                    }
                                    working = fitText(app, latest, item.withFrame(nf), handle)
                                    onPreview(latest.replace(working))
                                }
                                else -> {
                                    // Move, pinch-resize and twist the selected item.
                                    var f = item.frame
                                    if (zoom != 1f) {
                                        f = f.copy(w = (f.w * zoom).coerceAtLeast(40f), h = (f.h * zoom).coerceAtLeast(40f))
                                    }
                                    if (rot != 0f && event.changes.size > 1) {
                                        rotationAcc += rot
                                        val a = startRotation + rotationAcc
                                        f = f.copy(rotation = if (snapOn) Snap.rotation(a) else a)
                                    }
                                    f = f.moved(pan.x / s, pan.y / s)
                                    working = item.withFrame(f)
                                    var shown = f
                                    if (snapOn && event.changes.size == 1) {
                                        val r = Snap.snap(latest, f, item.id, threshold = 10 * density / s)
                                        shown = r.frame
                                        guides = r.guides
                                    }
                                    val scaled = if (item is TextItem && zoom != 1f) item.copy(size = item.size * zoom, frame = shown) else item.withFrame(shown)
                                    if (item is TextItem && zoom != 1f) working = scaled
                                    onPreview(latest.replace(scaled))
                                }
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                        guides = emptyList()
                        if (moved) {
                            if (onSelected) onCommit()
                        } else {
                            // A tap.
                            val now = System.currentTimeMillis()
                            val id = tapTarget?.id
                            if (tapTarget is PhotoItem && tapTarget.isEmpty) {
                                onSelect(id)
                                onEmptySlot(tapTarget.id)
                            } else if (id != null && id == lastTapId && now - lastTapAt < 350 && tapTarget is PhotoItem) {
                                onCrop(if (crop == id) null else id)
                            } else if (crop != null && id != crop) {
                                onCrop(null)
                                onSelect(id)
                            } else {
                                onSelect(if (handle != null) sel else id)
                            }
                            lastTapAt = now
                            lastTapId = id
                        }
                    }
                },
        ) {
            val s = viewport.scale
            if (s <= 0f) return@Canvas
            drawIntoCanvas { cc ->
                val c = cc.nativeCanvas
                val save = c.save()
                c.translate(viewport.offsetX, viewport.offsetY)
                c.scale(s, s)
                // Drop shadow under the canvas so the edge of the post is clear.
                fillPaint.color = 0x22000000
                c.drawRect(6f / s, 8f / s, project.width + 6f / s, project.height + 8f / s, fillPaint)
                c.save()
                c.clipRect(0f, 0f, project.width.toFloat(), project.height.toFloat())
                app.renderer.draw(c, project, { bitmaps[it] }, placeholders = true)
                c.restore()

                // While cropping, show the rest of the photo faintly outside its frame.
                val cropItem = croppingId?.let { project.item(it) } as? PhotoItem
                if (cropItem != null) {
                    val bmp = bitmaps[cropItem.uri]
                    if (bmp != null) {
                        val v = PhotoGeometry.visible(cropItem)
                        val f = cropItem.frame
                        val fullW = f.w / (v.right - v.left)
                        val fullH = f.h / (v.bottom - v.top)
                        val ox = -f.w / 2 - v.left * fullW
                        val oy = -f.h / 2 - v.top * fullH
                        c.save()
                        c.translate(f.cx, f.cy)
                        c.rotate(f.rotation)
                        fillPaint.alpha = 90
                        c.drawBitmap(bmp, null, android.graphics.RectF(ox, oy, ox + fullW, oy + fullH), fillPaint)
                        fillPaint.alpha = 255
                        c.restore()
                    }
                }

                if (showCuts) {
                    cutPaint.strokeWidth = 1.5f * density / s
                    cutPaint.color = 0xCC1D1E22.toInt()
                    cutPaint.pathEffect = DashPathEffect(floatArrayOf(8 * density / s, 6 * density / s), 0f)
                    for (x in Seams.cuts(project)) c.drawLine(x, 0f, x, project.height.toFloat(), cutPaint)
                }
                // Faces / text on a cut, outlined in red.
                val warned = notices.filterIsInstance<Notice.OnCut>().map { it.itemId }.toSet()
                if (warned.isNotEmpty()) {
                    linePaint.color = 0xFFD4432A.toInt()
                    linePaint.strokeWidth = 2.5f * density / s
                    for (w in Seams.warnings(project)) if (w.itemId in warned) c.drawRect(w.region.left, w.region.top, w.region.right, w.region.bottom, linePaint)
                }
                // Snap guides.
                linePaint.color = 0xFFE0457B.toInt()
                linePaint.strokeWidth = 1.5f * density / s
                for (g in guides) {
                    if (g.vertical) c.drawLine(g.at, -40f / s, g.at, project.height + 40f / s, linePaint)
                    else c.drawLine(-40f / s, g.at, project.width + 40f / s, g.at, linePaint)
                }
                // The line being drawn.
                if (ink.size > 1) {
                    linePaint.color = project.theme.accent.toInt()
                    linePaint.strokeWidth = 10f
                    linePaint.strokeCap = Paint.Cap.ROUND
                    for (i in 1 until ink.size) c.drawLine(ink[i - 1].x, ink[i - 1].y, ink[i].x, ink[i].y, linePaint)
                    linePaint.strokeCap = Paint.Cap.BUTT
                }
                c.restoreToCount(save)

                // Slide numbers above the canvas.
                labelPaint.textSize = 12 * density
                labelPaint.color = 0x991D1E22.toInt()
                for (i in 0 until project.slides) {
                    val o = viewport.toScreen(i * project.slideW + project.slideW / 2f, 0f)
                    c.drawText("${i + 1}", o.x, o.y - 8 * density, labelPaint)
                }

                // Selection outline and handles, in screen space so they stay the same size.
                val selItem = selectedId?.let { project.item(it) }
                if (selItem is RouteItem) {
                    for (pt in selItem.points) {
                        val o = viewport.toScreen(pt.x, pt.y)
                        fillPaint.color = 0xFFFFFFFF.toInt()
                        c.drawCircle(o.x, o.y, 10 * density, fillPaint)
                        linePaint.color = 0xFF2F6FED.toInt()
                        linePaint.strokeWidth = 2.5f * density
                        c.drawCircle(o.x, o.y, 10 * density, linePaint)
                    }
                } else if (selItem != null) {
                    val corners = selItem.frame.corners().map { viewport.toScreen(it.x, it.y) }
                    linePaint.color = if (croppingId == selItem.id) 0xFFF2B33D.toInt() else 0xFF2F6FED.toInt()
                    linePaint.strokeWidth = 2 * density
                    for (i in 0 until 4) {
                        val a = corners[i]; val b = corners[(i + 1) % 4]
                        c.drawLine(a.x, a.y, b.x, b.y, linePaint)
                    }
                    if (croppingId != selItem.id) {
                        for ((hd, o) in handles(selItem)) {
                            fillPaint.color = 0xFFFFFFFF.toInt()
                            c.drawCircle(o.x, o.y, 9 * density, fillPaint)
                            linePaint.strokeWidth = 2 * density
                            c.drawCircle(o.x, o.y, 9 * density, linePaint)
                            if (hd == Handle.Rotate) {
                                fillPaint.color = 0xFF2F6FED.toInt()
                                c.drawCircle(o.x, o.y, 3.5f * density, fillPaint)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A text box keeps its height fitted to its words while it's stretched. */
private fun fitText(app: io.github.darkxace01.unroll.UnrollApp, p: Project, item: Item, handle: Handle): Item {
    if (item !is TextItem) return item
    return when (handle) {
        Handle.Right -> item.copy(frame = item.frame.copy(h = app.renderer.textHeight(item, p)))
        Handle.Corner -> {
            val k = item.frame.h / app.renderer.textHeight(item, p).coerceAtLeast(1f)
            val t = item.copy(size = item.size * k)
            t.copy(frame = t.frame.copy(h = app.renderer.textHeight(t, p)))
        }
        else -> item
    }
}
