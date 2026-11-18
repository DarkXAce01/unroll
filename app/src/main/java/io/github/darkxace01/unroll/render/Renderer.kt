package io.github.darkxace01.unroll.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import io.github.darkxace01.unroll.R
import io.github.darkxace01.unroll.model.Background
import io.github.darkxace01.unroll.model.BackgroundKind
import io.github.darkxace01.unroll.model.Cap
import io.github.darkxace01.unroll.model.ColorMath
import io.github.darkxace01.unroll.model.Dash
import io.github.darkxace01.unroll.model.FontKey
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.Item
import io.github.darkxace01.unroll.model.Look
import io.github.darkxace01.unroll.model.PhotoGeometry
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Point
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.RouteItem
import io.github.darkxace01.unroll.model.RouteMath
import io.github.darkxace01.unroll.model.ShapeItem
import io.github.darkxace01.unroll.model.ShapeKind
import io.github.darkxace01.unroll.model.StickerItem
import io.github.darkxace01.unroll.model.Stickers
import io.github.darkxace01.unroll.model.TripInfo
import io.github.darkxace01.unroll.render.art.StickerArt
import io.github.darkxace01.unroll.model.TextAlign
import io.github.darkxace01.unroll.model.TextItem
import io.github.darkxace01.unroll.model.Vehicle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Typefaces for [FontKey]. Bundled fonts are all under the SIL Open Font License. */
class Fonts(context: Context) {
    companion object {
        fun variable(k: FontKey) = k == FontKey.Display || k == FontKey.Grotesk || k == FontKey.Condensed

        fun weight(k: FontKey, bold: Boolean): String? = when (k) {
            FontKey.Display -> if (bold) "'wght' 800" else "'wght' 620"
            FontKey.Grotesk -> if (bold) "'wght' 760" else "'wght' 520"
            FontKey.Condensed -> if (bold) "'wght' 800" else "'wght' 600"
            else -> null
        }
    }

    private val app = context.applicationContext
    private val loaded = java.util.concurrent.ConcurrentHashMap<FontKey, Typeface>()

    fun get(key: FontKey, bold: Boolean = false): Typeface {
        val base = loaded.getOrPut(key) {
            val res = when (key) {
                FontKey.Sans -> return@getOrPut Typeface.SANS_SERIF
                FontKey.Serif -> return@getOrPut Typeface.SERIF
                FontKey.Mono -> return@getOrPut Typeface.MONOSPACE
                FontKey.Condensed -> R.font.archivo_condensed
                FontKey.Display -> R.font.anybody
                FontKey.Elegant -> R.font.instrument_serif
                FontKey.ElegantItalic -> R.font.instrument_serif_italic
                FontKey.Hand -> R.font.reenie_beanie
                FontKey.Pixel -> R.font.vt323
                FontKey.Grotesk -> R.font.bricolage
            }
            runCatching { ResourcesCompat.getFont(app, res) }.getOrNull() ?: Typeface.DEFAULT
        }
        return if (bold) Typeface.create(base, Typeface.BOLD) else base
    }
}

/**
 * Draws a project in canvas units (1 unit = 1 exported pixel). The editor draws through this with a
 * scale, export draws each slide at full size, and video export draws every frame with [look] giving each
 * item's animated state, so the screen is always exactly the result.
 */
class Renderer(val fonts: Fonts) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val stickerArt = StickerArt(fonts)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    /**
     * [bitmap] gives the loaded photo for a URI (null while loading or missing). [placeholders] draws the
     * "+" on empty template slots (editor only). [look] animates items (video only).
     */
    fun draw(
        c: Canvas,
        p: Project,
        bitmap: (String) -> Bitmap?,
        skipId: String? = null,
        placeholders: Boolean = false,
        look: ((Item) -> Look)? = null,
        /** Fill empty slots with sample photos (template previews). */
        samples: Boolean = false,
        /** Which sample photo each empty slot gets, in order; null = in turn. */
        sampleOrder: IntArray? = null,
    ) {
        drawBackground(c, p, p.width.toFloat(), p.height.toFloat())
        var slot = 0
        for (item in p.items) {
            if (item.id == skipId) continue
            val l = look?.invoke(item) ?: Look.Still
            if (l.alpha <= 0.001f) continue
            val save = c.save()
            val animated = l != Look.Still
            if (animated) {
                val f = item.frame
                c.translate(l.dx, l.dy)
                if (l.scale != 1f || l.rotation != 0f) {
                    c.rotate(l.rotation, f.cx, f.cy)
                    c.scale(l.scale, l.scale, f.cx, f.cy)
                }
                if (l.alpha < 1f) {
                    val b = item.frame.bounds()
                    val pad = 200f
                    c.saveLayerAlpha(b.left - pad, b.top - pad, b.right + pad, b.bottom + pad, (l.alpha * 255).toInt())
                }
            }
            when (item) {
                is PhotoItem -> drawPhoto(c, p, item, if (item.isEmpty) null else bitmap(item.uri), placeholders, if (!samples) -1 else if (sampleOrder != null && sampleOrder.isNotEmpty()) sampleOrder[slot++ % sampleOrder.size] else slot++)
                is TextItem -> drawText(c, p, item, l.reveal)
                is ShapeItem -> drawShape(c, p, item)
                is StickerItem -> drawSticker(c, p, item)
                is RouteItem -> drawRoute(c, p, item, l.reveal)
            }
            c.restoreToCount(save)
        }
    }

    fun drawBackground(c: Canvas, p: Project, w: Float, h: Float) {
        val bg: Background = p.background
        paint.reset(); paint.isAntiAlias = true
        val c1 = p.color(bg.color, bg.colorRole).toInt()
        when (bg.kind) {
            BackgroundKind.Solid -> paint.color = c1
            BackgroundKind.Gradient -> {
                val r = Math.toRadians(bg.angle.toDouble())
                val dx = (cos(r) * w / 2).toFloat()
                val dy = (sin(r) * h / 2).toFloat()
                paint.shader = LinearGradient(w / 2 - dx, h / 2 - dy, w / 2 + dx, h / 2 + dy, c1, p.color(bg.color2, bg.color2Role).toInt(), Shader.TileMode.CLAMP)
            }
        }
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        Patterns.draw(c, bg.pattern, w, h, p.theme.ink.toInt(), p.theme.accent.toInt())
    }

    fun drawSticker(c: Canvas, p: Project, s: StickerItem) {
        val def = Stickers.byId(s.sticker) ?: return
        val f = s.frame
        val color = (s.color ?: s.colorRole?.let { p.theme.color(it) } ?: def.color ?: def.colorRole?.let { p.theme.color(it) } ?: p.theme.accent).toInt()
        val text = Stickers.fill(s.text ?: def.text.orEmpty(), TripInfo(title = p.name, place = p.name))
        inFrame(c, f) {
            if (s.flip) c.scale(-1f, 1f)
            stickerArt.draw(c, def, f.w, f.h, color, text, p.theme)
        }
    }

    private inline fun inFrame(c: Canvas, f: Frame, block: () -> Unit) {
        val save = c.save()
        c.translate(f.cx, f.cy)
        if (f.rotation != 0f) c.rotate(f.rotation)
        block()
        c.restoreToCount(save)
    }

    fun drawPhoto(c: Canvas, p: Project, item: PhotoItem, bmp: Bitmap?, placeholders: Boolean = false, sample: Int = -1) {
        val f = item.frame
        val s = item.style
        val radius = min(s.cornerRadius, min(f.w, f.h) / 2)
        val kind = s.frame
        val shape = Mounts.outline(kind, f.w, f.h, radius)
        val inner = PhotoGeometry.inner(item)
        val innerRect = RectF(inner.left, inner.top, inner.right, inner.bottom)
        val mounted = PhotoGeometry.mounted(kind)
        val mount = Mounts.mountColor(kind, s.mountRole, p.color(s.mountColor, s.mountRole).toInt())
        inFrame(c, f) {
            val layer = if (s.opacity < 1f) c.saveLayerAlpha(-f.w, -f.h, f.w, f.h, (s.opacity * 255).toInt()) else -1
            if (s.shadow > 0f || mounted) {
                val sh = if (s.shadow > 0f) s.shadow else if (mounted) 10f else 0f
                paint.reset(); paint.isAntiAlias = true
                paint.color = 0xFF000000.toInt()
                paint.setShadowLayer(sh, 0f, sh / 3, 0x55000000)
                c.drawPath(shape, paint)
                paint.clearShadowLayer()
            }
            if (mounted) {
                paint.reset(); paint.isAntiAlias = true
                paint.color = mount
                c.drawPath(shape, paint)
                Mounts.drawUnder(c, kind, f.w, f.h, innerRect, mount, paint)
            }
            val clip = c.save()
            if (mounted) c.clipPath(Mounts.window(kind, innerRect, f.w)) else c.clipPath(shape)
            if (bmp == null && sample >= 0) {
                val sb = SamplePhotos.bitmap(sample)
                if (sb == null) Scenes.draw(c, innerRect, sample) else {
                    // Cover the slot, centred, the way a picked photo would land.
                    val a = innerRect.width() / innerRect.height()
                    val src = if (sb.width.toFloat() / sb.height > a) {
                        val w = (sb.height * a).toInt(); val x = (sb.width - w) / 2; Rect(x, 0, x + w, sb.height)
                    } else {
                        val h = (sb.width / a).toInt(); val y = (sb.height - h) / 2; Rect(0, y, sb.width, y + h)
                    }
                    paint.reset(); paint.isAntiAlias = true; paint.isFilterBitmap = true
                    if (!item.adjust.isIdentity) paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(ColorMath.matrix(item.adjust)))
                    c.drawBitmap(sb, src, innerRect, paint)
                    paint.colorFilter = null
                }
            } else if (bmp == null) {
                paint.reset(); paint.isAntiAlias = true
                paint.color = blend(p.theme.muted, p.theme.background, 0.75f).toInt()
                c.drawRect(innerRect, paint)
                if (placeholders) {
                    paint.color = p.theme.muted.toInt()
                    paint.strokeWidth = min(innerRect.width(), innerRect.height()) * 0.025f + 2f
                    paint.strokeCap = Paint.Cap.ROUND
                    val arm = min(innerRect.width(), innerRect.height()) * 0.09f
                    c.drawLine(innerRect.centerX() - arm, innerRect.centerY(), innerRect.centerX() + arm, innerRect.centerY(), paint)
                    c.drawLine(innerRect.centerX(), innerRect.centerY() - arm, innerRect.centerX(), innerRect.centerY() + arm, paint)
                }
            } else {
                val v = PhotoGeometry.visible(item)
                val src = Rect((v.left * bmp.width).toInt(), (v.top * bmp.height).toInt(), (v.right * bmp.width).toInt(), (v.bottom * bmp.height).toInt())
                paint.reset(); paint.isAntiAlias = true; paint.isFilterBitmap = true
                if (!item.adjust.isIdentity) paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(ColorMath.matrix(item.adjust)))
                if (item.crop.flipX) c.scale(-1f, 1f, innerRect.centerX(), 0f)
                c.drawBitmap(bmp, src, innerRect, paint)
                if (item.crop.flipX) c.scale(-1f, 1f, innerRect.centerX(), 0f)
                paint.colorFilter = null
                if (item.adjust.vignette > 0f) {
                    val rad = maxOf(innerRect.width(), innerRect.height()) * 0.75f
                    paint.shader = RadialGradient(innerRect.centerX(), innerRect.centerY(), rad, intArrayOf(0, 0, (item.adjust.vignette * 200).toInt() shl 24), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                    c.drawRect(innerRect, paint)
                    paint.shader = null
                }
            }
            c.restoreToCount(clip)
            Mounts.drawOver(c, kind, f.w, f.h, innerRect, mount, paint, p.theme.accent.toInt())
            if (s.borderWidth > 0f) {
                paint.reset(); paint.isAntiAlias = true
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = s.borderWidth * 2
                paint.color = p.color(s.borderColor, s.borderRole).toInt()
                // Stroke centred on the outline, clipped inside: a border that doesn't grow the frame.
                val cs = c.save()
                c.clipPath(shape)
                c.drawPath(shape, paint)
                c.restoreToCount(cs)
                paint.style = Paint.Style.FILL
            }
            if (layer >= 0) c.restoreToCount(layer)
        }
    }

    private fun blend(a: Long, b: Long, t: Float): Long {
        fun ch(v: Long, s: Int) = ((v shr s) and 0xFF).toFloat()
        val r = ch(a, 16) + (ch(b, 16) - ch(a, 16)) * t
        val g = ch(a, 8) + (ch(b, 8) - ch(a, 8)) * t
        val bl = ch(a, 0) + (ch(b, 0) - ch(a, 0)) * t
        return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or bl.toLong()
    }

    /** Lays the text out to the frame's width. Also used to size the frame to its text. */
    fun layout(t: TextItem, width: Int = t.frame.w.toInt(), p: Project? = null, text: String = t.text): StaticLayout {
        textPaint.reset()
        textPaint.isAntiAlias = true
        val font = p?.font(t.font, t.fontRole) ?: t.font
        textPaint.typeface = fonts.get(font, t.bold && !Fonts.variable(font))
        // Variable fonts start at their thinnest; give them a proper weight.
        textPaint.fontVariationSettings = Fonts.weight(font, t.bold)
        textPaint.textSize = t.size
        textPaint.letterSpacing = t.letterSpacing
        textPaint.color = (p?.color(t.color, t.colorRole) ?: t.color).toInt()
        val align = when (t.align) {
            TextAlign.Start -> Layout.Alignment.ALIGN_NORMAL
            TextAlign.Center -> Layout.Alignment.ALIGN_CENTER
            TextAlign.End -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val s = text.ifEmpty { " " }
        return StaticLayout.Builder.obtain(s, 0, s.length, textPaint, width.coerceAtLeast(1))
            .setAlignment(align)
            .setLineSpacing(0f, t.lineHeight)
            .setIncludePad(false)
            .build()
    }

    /** Height the frame needs for its text, padding included. */
    fun textHeight(t: TextItem, p: Project? = null): Float = layout(t, (t.frame.w - pad(t) * 2).toInt(), p).height + pad(t) * 2

    /** The width of the longest line, for shrinking a new text box to its words. */
    fun textWidth(t: TextItem, p: Project? = null): Float {
        val l = layout(t, 100_000, p)
        return (0 until l.lineCount).maxOfOrNull { l.getLineWidth(it) } ?: 0f
    }

    fun pad(t: TextItem) = if (t.pillColor != null || t.pillRole != null) t.size * 0.45f else t.size * 0.1f

    fun drawText(c: Canvas, p: Project, t: TextItem, reveal: Float = 1f) {
        val f = t.frame
        inFrame(c, f) {
            val pill = t.pillRole?.let { p.theme.color(it) } ?: t.pillColor
            if (pill != null) {
                paint.reset(); paint.isAntiAlias = true; paint.color = pill.toInt()
                val r = min(t.size * 0.6f, f.h / 2)
                c.drawRoundRect(-f.w / 2, -f.h / 2, f.w / 2, f.h / 2, r, r, paint)
            }
            val pad = pad(t)
            // Typed text keeps its final layout (lines don't jump as letters appear).
            val full = layout(t, (f.w - pad * 2).toInt(), p)
            val shown = if (reveal >= 1f) full else layout(t, (f.w - pad * 2).toInt(), p, t.text.take((t.text.length * reveal).toInt()))
            c.translate(-f.w / 2 + pad, -full.height / 2f)
            if (t.outlineWidth > 0f) {
                textPaint.style = Paint.Style.STROKE
                textPaint.strokeWidth = t.outlineWidth * 2
                textPaint.strokeJoin = Paint.Join.ROUND
                textPaint.color = t.outlineColor.toInt()
                shown.draw(c)
                textPaint.style = Paint.Style.FILL
                textPaint.color = p.color(t.color, t.colorRole).toInt()
            }
            if (t.shadow > 0f) textPaint.setShadowLayer(t.shadow, 0f, t.shadow / 3, 0x88000000.toInt())
            shown.draw(c)
            textPaint.clearShadowLayer()
        }
    }

    fun drawShape(c: Canvas, p: Project, s: ShapeItem) {
        val f = s.frame
        inFrame(c, f) {
            paint.reset(); paint.isAntiAlias = true
            paint.color = p.color(s.color, s.colorRole).toInt()
            val stroke = s.stroke > 0f && s.kind != ShapeKind.Line && s.kind != ShapeKind.Arrow
            if (stroke) { paint.style = Paint.Style.STROKE; paint.strokeWidth = s.stroke }
            val w = f.w / 2
            val h = f.h / 2
            when (s.kind) {
                ShapeKind.Rect -> c.drawRect(-w, -h, w, h, paint)
                ShapeKind.Circle -> c.drawOval(-w, -h, w, h, paint)
                ShapeKind.Tape -> { paint.alpha = 190; c.drawPath(Shapes.tape(f.w, f.h), paint) }
                ShapeKind.Line, ShapeKind.Arrow -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = maxOf(s.stroke, f.h * 0.25f, 4f).coerceAtMost(f.h)
                    paint.strokeCap = Paint.Cap.ROUND
                    c.drawLine(-w, 0f, w, 0f, paint)
                    if (s.kind == ShapeKind.Arrow) {
                        val head = minOf(f.w * 0.25f, f.h * 2.5f)
                        c.drawLine(w, 0f, w - head, -head * 0.6f, paint)
                        c.drawLine(w, 0f, w - head, head * 0.6f, paint)
                    }
                }
                ShapeKind.Sun -> {
                    c.drawCircle(0f, 0f, minOf(w, h) * 0.5f, paint)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = minOf(w, h) * 0.08f
                    paint.strokeCap = Paint.Cap.ROUND
                    for (i in 0 until 12) {
                        val a = Math.toRadians(i * 30.0)
                        val r1 = minOf(w, h) * 0.66f; val r2 = minOf(w, h) * 0.95f
                        c.drawLine((cos(a) * r1).toFloat(), (sin(a) * r1).toFloat(), (cos(a) * r2).toFloat(), (sin(a) * r2).toFloat(), paint)
                    }
                }
                ShapeKind.Wave -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = maxOf(s.stroke, f.h * 0.18f)
                    paint.strokeCap = Paint.Cap.ROUND
                    c.drawPath(Shapes.wave(f.w, f.h * 0.6f), paint)
                }
                ShapeKind.Dots -> {
                    val cols = 5; val rows = 4
                    val r = minOf(f.w / cols, f.h / rows) * 0.22f
                    for (i in 0 until cols) for (j in 0 until rows) c.drawCircle(-w + (i + 0.5f) * f.w / cols, -h + (j + 0.5f) * f.h / rows, r, paint)
                }
                ShapeKind.Compass -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = minOf(w, h) * 0.06f
                    c.drawCircle(0f, 0f, minOf(w, h) * 0.92f, paint)
                    paint.style = Paint.Style.FILL
                    c.drawPath(Shapes.star(f.w * 0.8f, f.h * 0.8f, 4, 0.22f), paint)
                }
                else -> c.drawPath(Shapes.path(s.kind, f.w, f.h), paint)
            }
        }
    }

    fun drawRoute(c: Canvas, p: Project, r: RouteItem, reveal: Float = 1f) {
        if (r.points.size < 2) return
        val poly = RouteMath.sample(r)
        val shown = RouteMath.partial(poly, reveal)
        val color = p.color(r.color, r.colorRole).toInt()
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r.width
        paint.color = color
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = if (r.dash == Dash.Dashed) Paint.Cap.BUTT else Paint.Cap.ROUND
        paint.pathEffect = when (r.dash) {
            Dash.Solid -> null
            Dash.Dashed -> DashPathEffect(floatArrayOf(r.width * 3f, r.width * 2.2f), 0f)
            Dash.Dotted -> DashPathEffect(floatArrayOf(0.1f, r.width * 2.2f), 0f)
        }
        val path = Path()
        shown.forEachIndexed { i, pt -> if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y) }
        c.drawPath(path, paint)
        paint.pathEffect = null
        paint.style = Paint.Style.FILL

        if (r.stops) {
            // Stops appear once the line has reached them.
            for ((pt, at) in RouteMath.stops(r)) if (at <= reveal + 0.001f) drawStop(c, pt, r.width, color, p.theme.paper.toInt())
        }
        drawCap(c, r.start, poly.first(), RouteMath.at(poly, 0f).second + 180f, r.width, color, p.theme.paper.toInt())
        if (reveal >= 0.999f) drawCap(c, r.end, poly.last(), RouteMath.at(poly, 1f).second, r.width, color, p.theme.paper.toInt())
        if (r.vehicle != Vehicle.None) {
            val at = if (reveal < 1f) reveal else r.vehicleAt
            val (pt, angle) = RouteMath.at(poly, at)
            drawVehicle(c, r.vehicle, pt, angle, r.vehicleSize, color)
        }
    }

    private fun drawStop(c: Canvas, pt: Point, w: Float, color: Int, paper: Int) {
        paint.color = paper
        c.drawCircle(pt.x, pt.y, w * 1.6f, paint)
        paint.color = color
        c.drawCircle(pt.x, pt.y, w * 1.0f, paint)
    }

    private fun drawCap(c: Canvas, cap: Cap, pt: Point, angle: Float, w: Float, color: Int, paper: Int) {
        paint.color = color
        when (cap) {
            Cap.None -> Unit
            Cap.Dot -> c.drawCircle(pt.x, pt.y, w * 1.4f, paint)
            Cap.Ring -> {
                c.drawCircle(pt.x, pt.y, w * 2f, paint)
                paint.color = paper
                c.drawCircle(pt.x, pt.y, w * 1.1f, paint)
            }
            Cap.Pin -> {
                val size = w * 7f
                val save = c.save()
                c.translate(pt.x, pt.y - size / 2)
                c.drawPath(Shapes.path(ShapeKind.Pin, size * 0.7f, size), paint)
                c.restoreToCount(save)
            }
            Cap.Arrow -> {
                val save = c.save()
                c.translate(pt.x, pt.y)
                c.rotate(angle)
                val a = w * 3.2f
                c.drawPath(Path().apply { moveTo(a, 0f); lineTo(-a * 0.6f, -a * 0.8f); lineTo(-a * 0.6f, a * 0.8f); close() }, paint)
                c.restoreToCount(save)
            }
        }
    }

    private fun drawVehicle(c: Canvas, v: Vehicle, pt: Point, angle: Float, size: Float, color: Int) {
        val save = c.save()
        c.translate(pt.x, pt.y)
        // Planes and boats follow the heading; land vehicles stay upright and just face left or right.
        val facingLeft = angle > 90f || angle < -90f
        when (v) {
            Vehicle.Plane -> c.rotate(angle)
            Vehicle.Boat, Vehicle.Car, Vehicle.Train, Vehicle.Walk -> if (facingLeft) c.scale(-1f, 1f)
            else -> Unit
        }
        paint.color = color
        paint.style = Paint.Style.FILL
        c.drawPath(Shapes.vehicle(v, size), paint)
        c.restoreToCount(save)
    }
}

/** Vector paths for stickers, mounts and vehicles, centred on (0,0). */
object Shapes {
    fun path(kind: ShapeKind, w: Float, h: Float): Path {
        val p = Path()
        val hw = w / 2
        val hh = h / 2
        when (kind) {
            ShapeKind.Heart -> {
                p.moveTo(0f, hh)
                p.cubicTo(-hw * 1.3f, -hh * 0.1f, -hw * 0.6f, -hh * 1.3f, 0f, -hh * 0.45f)
                p.cubicTo(hw * 0.6f, -hh * 1.3f, hw * 1.3f, -hh * 0.1f, 0f, hh)
                p.close()
            }
            ShapeKind.Star -> return star(w, h, 5, 0.42f)
            ShapeKind.Burst -> return star(w, h, 16, 0.78f)
            ShapeKind.Sparkle -> {
                p.moveTo(0f, -hh)
                p.quadTo(0f, 0f, hw, 0f)
                p.quadTo(0f, 0f, 0f, hh)
                p.quadTo(0f, 0f, -hw, 0f)
                p.quadTo(0f, 0f, 0f, -hh)
                p.close()
            }
            ShapeKind.Pin -> {
                val r = hw
                p.addCircle(0f, -hh + r, r, Path.Direction.CW)
                p.moveTo(-r * 0.75f, -hh + r * 1.6f)
                p.lineTo(0f, hh)
                p.lineTo(r * 0.75f, -hh + r * 1.6f)
                p.close()
                p.fillType = Path.FillType.EVEN_ODD
                p.addCircle(0f, -hh + r, r * 0.4f, Path.Direction.CCW)
            }
            ShapeKind.Plane -> return vehicle(io.github.darkxace01.unroll.model.Vehicle.Plane, w, h)
            ShapeKind.Ticket -> {
                p.addRoundRect(-hw, -hh, hw, hh, h * 0.08f, h * 0.08f, Path.Direction.CW)
                val notch = Path().apply {
                    addCircle(-hw, 0f, h * 0.14f, Path.Direction.CW)
                    addCircle(hw, 0f, h * 0.14f, Path.Direction.CW)
                    // Perforation between stub and body.
                    var y = -hh + h * 0.08f
                    while (y < hh - h * 0.06f) { addCircle(hw * 0.45f, y, h * 0.025f, Path.Direction.CW); y += h * 0.1f }
                }
                p.op(notch, Path.Op.DIFFERENCE)
            }
            ShapeKind.Blob -> {
                val pts = (0 until 8).map { i ->
                    val a = Math.toRadians(i * 45.0)
                    val k = floatArrayOf(1f, 0.82f, 0.95f, 0.78f, 1f, 0.86f, 0.92f, 0.8f)[i]
                    io.github.darkxace01.unroll.model.Point((cos(a) * hw * k).toFloat(), (sin(a) * hh * k).toFloat())
                }
                val loop = io.github.darkxace01.unroll.model.RouteMath.catmullRom(pts + pts.take(3), 12)
                val start = 12
                val end = loop.size - 12 * 2
                for (i in start..end) { val q = loop[i]; if (i == start) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y) }
                p.close()
            }
            else -> p.addRect(-hw, -hh, hw, hh, Path.Direction.CW)
        }
        return p
    }

    fun star(w: Float, h: Float, points: Int, inner: Float): Path {
        val p = Path()
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) 1f else inner
            val a = Math.toRadians(-90.0 + i * 180.0 / points)
            val x = (cos(a) * w / 2 * r).toFloat()
            val y = (sin(a) * h / 2 * r).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    fun wave(w: Float, amp: Float): Path {
        val p = Path()
        val n = 48
        for (i in 0..n) {
            val x = -w / 2 + w * i / n
            val y = (sin(i / n.toFloat() * Math.PI * 4) * amp / 2).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        return p
    }

    /** Washi tape: a strip with zig-zag torn ends. */
    fun tape(w: Float, h: Float): Path {
        val p = Path()
        val hw = w / 2; val hh = h / 2
        val teeth = 6
        p.moveTo(-hw, -hh)
        p.lineTo(hw, -hh)
        for (i in 1..teeth) p.lineTo(if (i % 2 == 1) hw - h * 0.08f else hw, -hh + h * i / teeth)
        p.lineTo(-hw, hh)
        for (i in 1..teeth) p.lineTo(if (i % 2 == 1) -hw + h * 0.08f else -hw, hh - h * i / teeth)
        p.close()
        return p
    }

    /** A postage stamp's perforated edge. */
    fun scalloped(w: Float, h: Float, r: Float): Path {
        val p = Path().apply { addRect(-w / 2, -h / 2, w / 2, h / 2, Path.Direction.CW) }
        val holes = Path()
        val step = r * 2.6f
        var x = -w / 2 + step / 2
        while (x < w / 2) { holes.addCircle(x, -h / 2, r, Path.Direction.CW); holes.addCircle(x, h / 2, r, Path.Direction.CW); x += step }
        var y = -h / 2 + step / 2
        while (y < h / 2) { holes.addCircle(-w / 2, y, r, Path.Direction.CW); holes.addCircle(w / 2, y, r, Path.Direction.CW); y += step }
        p.op(holes, Path.Op.DIFFERENCE)
        return p
    }

    /** Paper torn by hand: a slightly jagged rectangle. Deterministic, so it doesn't flicker. */
    fun torn(w: Float, h: Float): Path {
        val p = Path()
        val rnd = java.util.Random(((w * 31 + h) * 7).toLong())
        val j = minOf(w, h) * 0.018f
        val steps = 28
        fun jit() = (rnd.nextFloat() - 0.5f) * 2 * j
        p.moveTo(-w / 2, -h / 2)
        for (i in 1..steps) p.lineTo(-w / 2 + w * i / steps, -h / 2 + jit())
        for (i in 1..steps) p.lineTo(w / 2 + jit(), -h / 2 + h * i / steps)
        for (i in 1..steps) p.lineTo(w / 2 - w * i / steps, h / 2 + jit())
        for (i in 1..steps) p.lineTo(-w / 2 + jit(), h / 2 - h * i / steps)
        p.close()
        return p
    }

    fun vehicle(v: io.github.darkxace01.unroll.model.Vehicle, size: Float) = vehicle(v, size, size)

    /** Simple silhouettes, nose to the right. */
    fun vehicle(v: io.github.darkxace01.unroll.model.Vehicle, w: Float, h: Float): Path {
        val p = Path()
        val hw = w / 2; val hh = h / 2
        when (v) {
            io.github.darkxace01.unroll.model.Vehicle.Plane -> {
                p.moveTo(hw, 0f)
                p.lineTo(hw * 0.6f, -hh * 0.12f); p.lineTo(-hw * 0.1f, -hh * 0.12f); p.lineTo(-hw * 0.45f, -hh)
                p.lineTo(-hw * 0.6f, -hh); p.lineTo(-hw * 0.35f, -hh * 0.12f); p.lineTo(-hw * 0.8f, -hh * 0.12f)
                p.lineTo(-hw, -hh * 0.5f); p.lineTo(-hw * 0.95f, 0f); p.lineTo(-hw, hh * 0.5f)
                p.lineTo(-hw * 0.8f, hh * 0.12f); p.lineTo(-hw * 0.35f, hh * 0.12f); p.lineTo(-hw * 0.6f, hh)
                p.lineTo(-hw * 0.45f, hh); p.lineTo(-hw * 0.1f, hh * 0.12f); p.lineTo(hw * 0.6f, hh * 0.12f)
                p.close()
            }
            io.github.darkxace01.unroll.model.Vehicle.Car -> {
                p.addRoundRect(-hw, -hh * 0.15f, hw, hh * 0.45f, hh * 0.12f, hh * 0.12f, Path.Direction.CW)
                p.addRoundRect(-hw * 0.55f, -hh * 0.55f, hw * 0.35f, 0f, hh * 0.2f, hh * 0.2f, Path.Direction.CW)
                p.addCircle(-hw * 0.55f, hh * 0.5f, hh * 0.24f, Path.Direction.CW)
                p.addCircle(hw * 0.55f, hh * 0.5f, hh * 0.24f, Path.Direction.CW)
            }
            io.github.darkxace01.unroll.model.Vehicle.Train -> {
                p.addRoundRect(-hw, -hh * 0.6f, hw * 0.75f, hh * 0.4f, hh * 0.15f, hh * 0.15f, Path.Direction.CW)
                p.moveTo(hw * 0.75f, -hh * 0.2f); p.lineTo(hw, hh * 0.4f); p.lineTo(hw * 0.75f, hh * 0.4f); p.close()
                for (x in listOf(-0.7f, -0.25f, 0.2f, 0.6f)) p.addCircle(hw * x, hh * 0.55f, hh * 0.16f, Path.Direction.CW)
            }
            io.github.darkxace01.unroll.model.Vehicle.Boat -> {
                p.moveTo(-hw, hh * 0.1f); p.lineTo(hw, hh * 0.1f); p.lineTo(hw * 0.7f, hh * 0.5f); p.lineTo(-hw * 0.7f, hh * 0.5f); p.close()
                p.moveTo(-hw * 0.05f, -hh); p.lineTo(-hw * 0.05f, 0f); p.lineTo(-hw * 0.65f, 0f); p.close()
                p.moveTo(hw * 0.05f, -hh * 0.75f); p.lineTo(hw * 0.05f, 0f); p.lineTo(hw * 0.55f, 0f); p.close()
            }
            io.github.darkxace01.unroll.model.Vehicle.Walk -> {
                // Two footprints.
                p.addOval(-hw * 0.6f, -hh * 0.6f, -hw * 0.1f, hh * 0.05f, Path.Direction.CW)
                p.addCircle(-hw * 0.35f, hh * 0.25f, hh * 0.15f, Path.Direction.CW)
                p.addOval(hw * 0.1f, -hh * 0.3f, hw * 0.6f, hh * 0.35f, Path.Direction.CW)
                p.addCircle(hw * 0.35f, hh * 0.55f, hh * 0.15f, Path.Direction.CW)
            }
            io.github.darkxace01.unroll.model.Vehicle.Heart -> return path(ShapeKind.Heart, w, h)
            io.github.darkxace01.unroll.model.Vehicle.None -> Unit
        }
        return p
    }
}
