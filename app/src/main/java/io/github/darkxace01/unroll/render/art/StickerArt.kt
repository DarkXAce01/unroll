package io.github.darkxace01.unroll.render.art

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.PathParser
import io.github.darkxace01.unroll.model.ArtKind
import io.github.darkxace01.unroll.model.Doodles
import io.github.darkxace01.unroll.model.FontKey
import io.github.darkxace01.unroll.model.StickerDef
import io.github.darkxace01.unroll.model.Theme
import io.github.darkxace01.unroll.render.Fonts
import java.util.concurrent.ConcurrentHashMap

/**
 * Everything a sticker drawing needs: the canvas (origin at the sticker's centre), its size, the
 * colour and words it shows, and the fonts. One per draw, so drawing is thread-safe.
 */
class Art(
    val c: Canvas,
    val w: Float,
    val h: Float,
    /** The sticker's colour (already resolved from the item, the sticker's default or the theme). */
    val color: Int,
    val text: String,
    val param: String,
    val fonts: Fonts,
    val theme: Theme,
) {
    val hw get() = w / 2
    val hh get() = h / 2
    val m get() = minOf(w, h)
    val ink get() = theme.ink.toInt()
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)

    fun fill(color: Int): Paint = p.apply { reset(); isAntiAlias = true; style = Paint.Style.FILL; this.color = color }
    fun stroke(color: Int, width: Float): Paint = p.apply {
        reset(); isAntiAlias = true; style = Paint.Style.STROKE; this.color = color; strokeWidth = width
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }

    fun face(k: FontKey, bold: Boolean = false): Typeface = fonts.get(k, bold && !Fonts.variable(k))

    fun font(k: FontKey, size: Float, color: Int, bold: Boolean = false, spacing: Float = 0f): TextPaint = tp.apply {
        reset(); isAntiAlias = true
        typeface = face(k, bold)
        fontVariationSettings = Fonts.weight(k, bold)
        textSize = size; this.color = color; letterSpacing = spacing
        textAlign = Paint.Align.LEFT
    }

    /** One line of text centred on (x, y), shrunk to fit [maxW]. */
    fun line(s: String, x: Float, y: Float, maxW: Float, paint: TextPaint) {
        val tw = paint.measureText(s)
        if (tw > maxW && tw > 0f) paint.textSize *= maxW / tw
        val fm = paint.fontMetrics
        c.drawText(s, x - paint.measureText(s) / 2, y - (fm.ascent + fm.descent) / 2, paint)
    }

    /** Wrapped text centred in the box, sized down until it fits. */
    fun block(s: String, box: RectF, paint: TextPaint, lineHeight: Float = 1.05f, align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER) {
        var size = paint.textSize
        var layout: StaticLayout
        while (true) {
            paint.textSize = size
            layout = StaticLayout.Builder.obtain(s, 0, s.length, paint, box.width().toInt().coerceAtLeast(1))
                .setAlignment(align).setLineSpacing(0f, lineHeight).setIncludePad(false).build()
            val widest = (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: 0f
            // Never split a word across lines: shrink instead.
            val split = (0 until layout.lineCount - 1).any { val e = layout.getLineEnd(it); e in 1 until s.length && !s[e - 1].isWhitespace() && !s[e].isWhitespace() }
            if ((layout.height <= box.height() && widest <= box.width() + 1f && !split) || size < 6f) break
            size *= 0.92f
        }
        c.save()
        c.translate(box.left, box.centerY() - layout.height / 2f)
        layout.draw(c)
        c.restore()
    }

    /** A 100×100 doodle, uniformly scaled into a [size] square centred on (x, y). */
    fun doodle(key: String, x: Float, y: Float, size: Float, block: (Path, Float) -> Unit) {
        val path = StickerArt.doodle(key) ?: return
        val s = size / 100f
        c.save()
        c.translate(x - 50f * s, y - 50f * s)
        c.scale(s, s)
        block(path, s)
        c.restore()
    }
}

/** Draws stickers from the library. */
class StickerArt(private val fonts: Fonts) {

    fun draw(c: Canvas, def: StickerDef, w: Float, h: Float, color: Int, text: String, theme: Theme) {
        val a = Art(c, w, h, color, text, def.param, fonts, theme)
        when (def.kind) {
            ArtKind.Doodle -> a.chalk()
            ArtKind.Marker -> a.marker()
            ArtKind.DieCut -> a.dieCut()
            ArtKind.Bead -> a.bead(text, 0f, 0f, minOf(w, h) * 0.94f, def.param)
            ArtKind.BeadWord -> a.beadWord()
            ArtKind.Ransom -> a.ransomTile(text, 0f, 0f, w * 0.92f, h * 0.92f, def.param.toIntOrNull() ?: 0, text.hashCode())
            ArtKind.RansomWord -> a.ransomWord()
            ArtKind.Tape -> a.tape()
            ArtKind.Stamp -> a.stamp()
            ArtKind.Postmark -> a.postmark()
            ArtKind.Rubber -> a.rubber()
            ArtKind.Ticket -> a.ticket()
            ArtKind.Label -> a.label()
            ArtKind.Clip -> a.clip()
            ArtKind.Patch -> a.patch()
            ArtKind.Title -> a.title()
            ArtKind.Subtitle -> a.subtitle()
            ArtKind.Foil -> a.foil()
            ArtKind.Flower -> a.flower()
            ArtKind.Flag -> a.flag()
            ArtKind.Bubble -> a.bubble()
        }
    }

    companion object {
        private val cache = ConcurrentHashMap<String, Path>()

        /** The doodle's path on its 100×100 box (parsed once). */
        fun doodle(key: String): Path? {
            cache[key]?.let { return it }
            val d = Doodles.paths[key] ?: return null
            val p = runCatching { PathParser.createPathFromPathData(d) }.getOrNull() ?: return null
            cache[key] = p
            return p
        }

        fun closed(key: String) = Doodles.paths[key]?.contains('Z') == true
    }
}

/** Colour helpers. */
internal fun mix(a: Int, b: Int, t: Float): Int {
    fun ch(v: Int, s: Int) = (v shr s) and 0xFF
    fun l(x: Int, y: Int) = (x + (y - x) * t).toInt().coerceIn(0, 255)
    return (l(ch(a, 24), ch(b, 24)) shl 24) or (l(ch(a, 16), ch(b, 16)) shl 16) or (l(ch(a, 8), ch(b, 8)) shl 8) or l(ch(a, 0), ch(b, 0))
}

internal fun darker(c: Int, t: Float = 0.25f) = mix(c, 0xFF000000.toInt() or (c and 0xFF000000.toInt()), t).let { (it and 0x00FFFFFF) or (c and 0xFF000000.toInt()) }
internal fun lighter(c: Int, t: Float = 0.3f) = mix(c, 0xFFFFFFFF.toInt(), t).let { (it and 0x00FFFFFF) or (c and 0xFF000000.toInt()) }
internal fun alpha(c: Int, a: Int) = (c and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)
internal fun luminance(c: Int): Float = (0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)) / 255f
/** Black or white, whichever reads on [c]. */
internal fun onColor(c: Int) = if (luminance(c) > 0.6f) 0xFF1D1E22.toInt() else 0xFFFFFFFF.toInt()
