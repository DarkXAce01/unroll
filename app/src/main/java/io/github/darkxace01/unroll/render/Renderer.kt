package io.github.darkxace01.unroll.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import io.github.darkxace01.unroll.model.ColorMath
import io.github.darkxace01.unroll.model.FontKey
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.PhotoGeometry
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.ShapeItem
import io.github.darkxace01.unroll.model.ShapeKind
import io.github.darkxace01.unroll.model.TextAlign
import io.github.darkxace01.unroll.model.TextItem
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Typefaces for [FontKey]. Bundled fonts are all under the SIL Open Font License. */
class Fonts(context: Context) {
    private val app = context.applicationContext
    private val loaded = HashMap<FontKey, Typeface>()

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
 * scale, and export draws each slide through it at full size, so the screen is exactly the result.
 *
 * [bitmap] returns the loaded photo for a URI, or null while loading / when it's missing.
 */
class Renderer(private val fonts: Fonts) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    fun draw(c: Canvas, p: Project, bitmap: (String) -> Bitmap?, skipId: String? = null) {
        drawBackground(c, p.background, p.width.toFloat(), p.height.toFloat())
        for (item in p.items) {
            if (item.id == skipId) continue
            when (item) {
                is PhotoItem -> drawPhoto(c, item, bitmap(item.uri))
                is TextItem -> drawText(c, item)
                is ShapeItem -> drawShape(c, item)
            }
        }
    }

    fun drawBackground(c: Canvas, bg: Background, w: Float, h: Float) {
        paint.reset(); paint.isAntiAlias = true
        when (bg.kind) {
            BackgroundKind.Solid -> paint.color = bg.color.toInt()
            BackgroundKind.Gradient -> {
                val r = Math.toRadians(bg.angle.toDouble())
                val dx = (cos(r) * w / 2).toFloat()
                val dy = (sin(r) * h / 2).toFloat()
                paint.shader = LinearGradient(w / 2 - dx, h / 2 - dy, w / 2 + dx, h / 2 + dy, bg.color.toInt(), bg.color2.toInt(), Shader.TileMode.CLAMP)
            }
        }
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
    }

    private inline fun inFrame(c: Canvas, f: Frame, block: () -> Unit) {
        val save = c.save()
        c.translate(f.cx, f.cy)
        if (f.rotation != 0f) c.rotate(f.rotation)
        block()
        c.restoreToCount(save)
    }

    fun drawPhoto(c: Canvas, item: PhotoItem, bmp: Bitmap?) {
        val f = item.frame
        val s = item.style
        val rect = RectF(-f.w / 2, -f.h / 2, f.w / 2, f.h / 2)
        val radius = min(s.cornerRadius, min(f.w, f.h) / 2)
        inFrame(c, f) {
            val layer = if (s.opacity < 1f) c.saveLayerAlpha(rect.left - s.shadow * 2, rect.top - s.shadow * 2, rect.right + s.shadow * 2, rect.bottom + s.shadow * 2, (s.opacity * 255).toInt()) else -1
            if (s.shadow > 0f) {
                paint.reset(); paint.isAntiAlias = true
                paint.color = 0xFF000000.toInt()
                paint.setShadowLayer(s.shadow, 0f, s.shadow / 3, 0x66000000)
                c.drawRoundRect(rect, radius, radius, paint)
                paint.clearShadowLayer()
            }
            val clip = c.save()
            if (radius > 0f) c.clipPath(Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }) else c.clipRect(rect)
            if (bmp == null) {
                paint.reset(); paint.color = 0xFFD9D6CF.toInt()
                c.drawRect(rect, paint)
            } else {
                val v = PhotoGeometry.visible(item)
                val src = Rect((v.left * bmp.width).toInt(), (v.top * bmp.height).toInt(), (v.right * bmp.width).toInt(), (v.bottom * bmp.height).toInt())
                paint.reset(); paint.isAntiAlias = true; paint.isFilterBitmap = true
                if (!item.adjust.isIdentity) paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(ColorMath.matrix(item.adjust)))
                if (item.crop.flipX) c.scale(-1f, 1f)
                c.drawBitmap(bmp, src, rect, paint)
                if (item.crop.flipX) c.scale(-1f, 1f)
                paint.colorFilter = null
                if (item.adjust.vignette > 0f) {
                    val rad = maxOf(f.w, f.h) * 0.75f
                    paint.shader = RadialGradient(0f, 0f, rad, intArrayOf(0, 0, (item.adjust.vignette * 200).toInt() shl 24), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                    c.drawRect(rect, paint)
                    paint.shader = null
                }
            }
            c.restoreToCount(clip)
            if (s.borderWidth > 0f) {
                paint.reset(); paint.isAntiAlias = true
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = s.borderWidth
                paint.color = s.borderColor.toInt()
                val inset = RectF(rect).apply { inset(s.borderWidth / 2, s.borderWidth / 2) }
                val r2 = (radius - s.borderWidth / 2).coerceAtLeast(0f)
                c.drawRoundRect(inset, r2, r2, paint)
                paint.style = Paint.Style.FILL
            }
            if (layer >= 0) c.restoreToCount(layer)
        }
    }

    /** Lays the text out to the frame's width. Also used to size the frame to its text. */
    fun layout(t: TextItem, width: Int = t.frame.w.toInt()): StaticLayout {
        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = fonts.get(t.font, t.bold)
        textPaint.textSize = t.size
        textPaint.letterSpacing = t.letterSpacing
        textPaint.color = t.color.toInt()
        val align = when (t.align) {
            TextAlign.Start -> Layout.Alignment.ALIGN_NORMAL
            TextAlign.Center -> Layout.Alignment.ALIGN_CENTER
            TextAlign.End -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val text = t.text.ifEmpty { " " }
        return StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width.coerceAtLeast(1))
            .setAlignment(align)
            .setLineSpacing(0f, t.lineHeight)
            .setIncludePad(false)
            .build()
    }

    /** Height the frame needs for its text, padding included. */
    fun textHeight(t: TextItem): Float = layout(t, (t.frame.w - pad(t) * 2).toInt()).height + pad(t) * 2

    /** The width of the longest line, for shrinking a new text box to its words. */
    fun textWidth(t: TextItem): Float {
        val l = layout(t, 100_000)
        return (0 until l.lineCount).maxOfOrNull { l.getLineWidth(it) } ?: 0f
    }

    fun pad(t: TextItem) = if (t.pillColor != null) t.size * 0.45f else t.size * 0.1f

    fun drawText(c: Canvas, t: TextItem) {
        val f = t.frame
        inFrame(c, f) {
            if (t.pillColor != null) {
                paint.reset(); paint.isAntiAlias = true; paint.color = t.pillColor.toInt()
                val r = min(t.size * 0.6f, f.h / 2)
                c.drawRoundRect(-f.w / 2, -f.h / 2, f.w / 2, f.h / 2, r, r, paint)
            }
            val pad = pad(t)
            val layout = layout(t, (f.w - pad * 2).toInt())
            c.translate(-f.w / 2 + pad, -layout.height / 2f)
            if (t.outlineWidth > 0f) {
                textPaint.style = Paint.Style.STROKE
                textPaint.strokeWidth = t.outlineWidth * 2
                textPaint.strokeJoin = Paint.Join.ROUND
                textPaint.color = t.outlineColor.toInt()
                layout.draw(c)
                textPaint.style = Paint.Style.FILL
                textPaint.color = t.color.toInt()
            }
            if (t.shadow > 0f) textPaint.setShadowLayer(t.shadow, 0f, t.shadow / 3, 0x88000000.toInt())
            layout.draw(c)
            textPaint.clearShadowLayer()
        }
    }

    fun drawShape(c: Canvas, s: ShapeItem) {
        val f = s.frame
        inFrame(c, f) {
            paint.reset(); paint.isAntiAlias = true
            paint.color = s.color.toInt()
            val stroke = s.stroke > 0f && s.kind != ShapeKind.Line && s.kind != ShapeKind.Arrow
            if (stroke) { paint.style = Paint.Style.STROKE; paint.strokeWidth = s.stroke }
            val w = f.w / 2
            val h = f.h / 2
            when (s.kind) {
                ShapeKind.Rect -> c.drawRect(-w, -h, w, h, paint)
                ShapeKind.Circle -> c.drawOval(-w, -h, w, h, paint)
                ShapeKind.Tape -> { paint.alpha = 190; c.drawRect(-w, -h, w, h, paint) }
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
                else -> c.drawPath(Shapes.path(s.kind, f.w, f.h), paint)
            }
        }
    }
}

/** Vector paths for the simple stickers, centred on (0,0). */
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
            ShapeKind.Star -> {
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) 1f else 0.42f
                    val a = Math.toRadians(-90.0 + i * 36)
                    val x = (cos(a) * hw * r).toFloat()
                    val y = (sin(a) * hh * r).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
            }
            ShapeKind.Pin -> {
                val r = hw
                p.addCircle(0f, -hh + r, r, Path.Direction.CW)
                p.moveTo(-r * 0.75f, -hh + r * 1.6f)
                p.lineTo(0f, hh)
                p.lineTo(r * 0.75f, -hh + r * 1.6f)
                p.close()
                p.fillType = Path.FillType.WINDING
                p.addCircle(0f, -hh + r, r * 0.4f, Path.Direction.CCW)
            }
            ShapeKind.Plane -> {
                // Side view, nose to the right.
                p.moveTo(hw, 0f)
                p.lineTo(hw * 0.6f, -hh * 0.12f)
                p.lineTo(-hw * 0.1f, -hh * 0.12f)
                p.lineTo(-hw * 0.45f, -hh)
                p.lineTo(-hw * 0.6f, -hh)
                p.lineTo(-hw * 0.35f, -hh * 0.12f)
                p.lineTo(-hw * 0.8f, -hh * 0.12f)
                p.lineTo(-hw, -hh * 0.5f)
                p.lineTo(-hw * 0.95f, 0f)
                p.lineTo(-hw, hh * 0.5f)
                p.lineTo(-hw * 0.8f, hh * 0.12f)
                p.lineTo(-hw * 0.35f, hh * 0.12f)
                p.lineTo(-hw * 0.6f, hh)
                p.lineTo(-hw * 0.45f, hh)
                p.lineTo(-hw * 0.1f, hh * 0.12f)
                p.lineTo(hw * 0.6f, hh * 0.12f)
                p.close()
            }
            else -> p.addRect(-hw, -hh, hw, hh, Path.Direction.CW)
        }
        return p
    }
}
