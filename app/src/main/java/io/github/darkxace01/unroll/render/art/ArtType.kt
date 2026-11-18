package io.github.darkxace01.unroll.render.art

import android.graphics.DiscretePathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import io.github.darkxace01.unroll.model.FontKey
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Doodles, beads, cut-out letters, titles and subtitles.

/** Hand-drawn in chalk or ink: a slightly rough line, laid twice. */
internal fun Art.chalk() = doodle(param, 0f, 0f, m) { path, _ ->
    val pen = stroke(alpha(color, 235), 3.4f)
    pen.pathEffect = DiscretePathEffect(2.2f, 0.55f)
    c.drawPath(path, pen)
    c.save(); c.translate(0.45f, 0.35f)
    pen.color = alpha(color, 110); pen.strokeWidth = 2.2f
    c.drawPath(path, pen)
    c.restore()
}

/** Felt-tip: a fat colour stroke, a little off the ink outline, like a quick colouring-in. */
internal fun Art.marker() = doodle(param, 0f, 0f, m) { path, _ ->
    val closed = StickerArt.closed(param)
    c.save(); c.translate(2.6f, 2.4f)
    if (closed) c.drawPath(path, fill(alpha(color, 170)))
    else c.drawPath(path, stroke(alpha(color, 170), 9f))
    c.restore()
    c.drawPath(path, stroke(darker(ink, 0f), 2.6f))
}

/** A die-cut vinyl sticker: thick white border with a soft shadow, colour, then the outline. */
internal fun Art.dieCut() = doodle(param, 0f, 0f, m * 0.82f) { path, _ ->
    val closed = StickerArt.closed(param)
    val border = stroke(0xFFFFFFFF.toInt(), 15f)
    border.setShadowLayer(3.5f, 0f, 1.6f, 0x44000000)
    c.drawPath(path, border)
    if (closed) {
        val f = fill(0xFFFFFFFF.toInt()); f.setShadowLayer(3.5f, 0f, 1.6f, 0x44000000); c.drawPath(path, f)
        f.clearShadowLayer()
        c.drawPath(path, fill(color))
        c.drawPath(path, stroke(darker(color, 0.45f), 2.4f))
        // A little gloss.
        val g = stroke(0x66FFFFFF, 2.2f)
        c.save(); c.translate(-1.6f, -1.6f); c.clipPath(path); c.drawPath(path, g); c.restore()
    } else {
        c.drawPath(path, stroke(color, 7f))
        c.drawPath(path, stroke(0x55FFFFFF, 2f))
    }
}

private val beadPastels = intArrayOf(0xFFF6B8C8.toInt(), 0xFFB9DDF5.toInt(), 0xFFC8EBC0.toInt(), 0xFFFFE29A.toInt(), 0xFFD9C6F2.toInt(), 0xFFFFC9A8.toInt(), 0xFFA8E6DC.toInt())

/** One alphabet bead: a rounded cube, lit from the top left, with its letter. */
internal fun Art.bead(g: String, x: Float, y: Float, size: Float, style: String) {
    val (body, letter) = when (style) {
        "black" -> 0xFF1F1F22.toInt() to 0xFFFFFFFF.toInt()
        "pastel" -> beadPastels[abs(g.hashCode()) % beadPastels.size] to 0xFFFFFFFF.toInt()
        "gold" -> 0xFFD7B25A.toInt() to 0xFF4A3410.toInt()
        else -> 0xFFF7F5F0.toInt() to 0xFF1D1E22.toInt()
    }
    val r = RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2)
    val rad = size * 0.24f
    val sh = fill(body); sh.setShadowLayer(size * 0.06f, 0f, size * 0.04f, 0x55000000)
    c.drawRoundRect(r, rad, rad, sh)
    val grad = fill(body)
    grad.shader = if (style == "gold") LinearGradient(r.left, r.top, r.right, r.bottom, intArrayOf(0xFFFFF0B0.toInt(), 0xFFC9952F.toInt(), 0xFFF3D98A.toInt(), 0xFF9C6E1E.toInt()), null, Shader.TileMode.CLAMP)
    else LinearGradient(0f, r.top, 0f, r.bottom, lighter(body, 0.25f), darker(body, 0.14f), Shader.TileMode.CLAMP)
    c.drawRoundRect(r, rad, rad, grad)
    // Highlight
    c.drawOval(RectF(r.left + size * 0.12f, r.top + size * 0.07f, r.left + size * 0.5f, r.top + size * 0.24f), fill(0x55FFFFFF))
    val tpaint = font(FontKey.Sans, size * 0.6f, letter, bold = true)
    tpaint.isFakeBoldText = true
    if (style == "pastel") tpaint.setShadowLayer(size * 0.02f, 0f, size * 0.015f, 0x44000000)
    line(g, x, y + size * 0.02f, size * 0.78f, tpaint)
}

/** A string of beads spelling a word, on a cord. */
internal fun Art.beadWord() {
    val chars = text.uppercase().toList().map { it.toString() }
    val n = chars.size.coerceAtLeast(1)
    val size = minOf(h * 0.82f, w / (n * 1.06f + 0.6f))
    val total = n * size * 1.06f
    c.drawLine(-hw * 0.98f, 0f, hw * 0.98f, 0f, stroke(0xFFD8CFC0.toInt(), size * 0.07f))
    val styles = listOf(param)
    var x = -total / 2 + size * 0.53f
    for (ch in chars) {
        if (ch.isBlank()) {
            c.drawCircle(x, 0f, size * 0.22f, fill(0xFFF4EFE6.toInt()).apply { setShadowLayer(size * 0.04f, 0f, size * 0.03f, 0x44000000) })
        } else bead(ch, x, 0f, size, styles[0])
        x += size * 1.06f
    }
    // Little pearls at the ends.
    for (side in listOf(-1, 1)) c.drawCircle(side * (total / 2 + size * 0.2f), 0f, size * 0.16f, fill(0xFFFFFFFF.toInt()).apply { setShadowLayer(size * 0.04f, 0f, size * 0.03f, 0x44000000) })
}

private data class Look(val bg: Int, val fg: Int, val font: FontKey, val bold: Boolean)

private val looks = listOf(
    Look(0xFFF3EFE6.toInt(), 0xFF111111.toInt(), FontKey.Serif, true),
    Look(0xFFD9412E.toInt(), 0xFFFFFFFF.toInt(), FontKey.Condensed, true),
    Look(0xFF141414.toInt(), 0xFFFFFFFF.toInt(), FontKey.Display, true),
    Look(0xFFF2C230.toInt(), 0xFF141414.toInt(), FontKey.Grotesk, true),
    Look(0xFFFFFFFF.toInt(), 0xFFD9412E.toInt(), FontKey.Elegant, false),
    Look(0xFFC69C6D.toInt(), 0xFF1D1E22.toInt(), FontKey.Mono, true),
    Look(0xFF3B6FD8.toInt(), 0xFFFFFFFF.toInt(), FontKey.Sans, true),
    Look(0xFFF6C1D1.toInt(), 0xFF1D1E22.toInt(), FontKey.ElegantItalic, false),
)

/** A letter cut from a magazine: its own paper, font and tilt, with roughly cut edges. */
internal fun Art.ransomTile(g: String, x: Float, y: Float, tw: Float, th: Float, look: Int, seed: Int) {
    val lk = looks[((look % looks.size) + looks.size) % looks.size]
    val rnd = java.util.Random(seed.toLong() * 31 + look)
    c.save()
    c.translate(x, y)
    c.rotate((rnd.nextFloat() - 0.5f) * 12f)
    val path = Path()
    val jx = tw * 0.06f; val jy = th * 0.06f
    fun j(v: Float) = (rnd.nextFloat() - 0.5f) * 2 * v
    path.moveTo(-tw / 2 + j(jx), -th / 2 + j(jy))
    path.lineTo(tw / 2 + j(jx), -th / 2 + j(jy))
    path.lineTo(tw / 2 + j(jx), th / 2 + j(jy))
    path.lineTo(-tw / 2 + j(jx), th / 2 + j(jy))
    path.close()
    val f = fill(lk.bg); f.setShadowLayer(th * 0.05f, th * 0.015f, th * 0.03f, 0x44000000)
    c.drawPath(path, f)
    val tpaint = font(lk.font, th * 0.78f, lk.fg, lk.bold)
    line(g, 0f, th * 0.02f, tw * 0.86f, tpaint)
    c.restore()
}

/** A phrase spelled out in cut-out letters, each one different. */
internal fun Art.ransomWord() {
    val words = text.split(' ').filter { it.isNotEmpty() }
    // Two lines when the phrase is long.
    val lines = if (text.length > 12 && words.size > 1) {
        var best = 1; var bestDiff = Int.MAX_VALUE
        for (i in 1 until words.size) {
            val d = abs(words.take(i).joinToString(" ").length - words.drop(i).joinToString(" ").length)
            if (d < bestDiff) { bestDiff = d; best = i }
        }
        listOf(words.take(best).joinToString(" "), words.drop(best).joinToString(" "))
    } else listOf(text)
    val longest = lines.maxOf { it.length }.coerceAtLeast(1)
    val tile = minOf(w / (longest * 0.78f + 0.2f), h / (lines.size * 1.12f))
    lines.forEachIndexed { li, ln ->
        val y = (li - (lines.size - 1) / 2f) * tile * 1.1f
        var x = -(ln.length * tile * 0.78f) / 2 + tile * 0.39f
        ln.forEachIndexed { i, ch ->
            if (ch != ' ') {
                val seed = ch.code * 7 + i * 13 + li * 101
                val look = (abs(seed * 2654435761L.toInt()) shr 3) % 8
                val s = 0.86f + ((seed % 5) * 0.04f)
                ransomTile(ch.uppercase().takeIf { look % 3 != 0 } ?: ch.toString(), x, y + ((seed % 3) - 1) * tile * 0.05f, tile * 0.74f * s, tile * s, look, seed)
            }
            x += tile * 0.78f
        }
    }
}

/** Titles: the words in a ready-made type treatment. */
internal fun Art.title() {
    val s = text
    val col = color
    when (param) {
        "stack" -> {
            val words = s.uppercase().split(' ').filter { it.isNotEmpty() }
            val lines = if (words.size >= 2) listOf(words.take((words.size + 1) / 2).joinToString(" "), words.drop((words.size + 1) / 2).joinToString(" ")) else words
            block(lines.joinToString("\n"), RectF(-hw, -hh, hw, hh), font(FontKey.Display, h * 0.5f, col, bold = true), 0.86f)
        }
        "spaced" -> {
            val paint = font(FontKey.Condensed, h * 0.3f, col, bold = true, spacing = 0.35f)
            line(s.uppercase(), 0f, 0f, w * 0.92f, paint)
            val lw = stroke(col, h * 0.018f)
            c.drawLine(-hw * 0.5f, -hh * 0.55f, hw * 0.5f, -hh * 0.55f, lw)
            c.drawLine(-hw * 0.5f, hh * 0.55f, hw * 0.5f, hh * 0.55f, lw)
        }
        "serif-script" -> {
            val words = s.split(' ')
            val top = if (words.size > 1) words.first() else "hello"
            val rest = if (words.size > 1) words.drop(1).joinToString(" ") else s
            line(top.uppercase(), 0f, -hh * 0.5f, w * 0.7f, font(FontKey.Elegant, h * 0.2f, col, spacing = 0.3f))
            line(rest, 0f, hh * 0.18f, w * 0.96f, font(FontKey.Hand, h * 0.62f, theme.accent.toInt()))
        }
        "outline" -> {
            val p1 = font(FontKey.Display, h * 0.56f, theme.accent.toInt(), bold = true)
            c.save(); c.translate(h * 0.045f, h * 0.05f); line(s.uppercase(), 0f, 0f, w * 0.94f, p1); c.restore()
            val p2 = font(FontKey.Display, h * 0.56f, col, bold = true)
            p2.style = Paint.Style.STROKE; p2.strokeWidth = h * 0.018f; p2.strokeJoin = Paint.Join.ROUND
            line(s.uppercase(), 0f, 0f, w * 0.94f, p2)
        }
        "laurel" -> {
            line(s, 0f, 0f, w * 0.56f, font(FontKey.Elegant, h * 0.42f, col))
            for (side in listOf(-1f, 1f)) laurel(side * hw * 0.84f, 0f, h * 0.86f, side, col)
        }
        "boxed" -> {
            val r = RectF(-hw * 0.94f, -hh * 0.62f, hw * 0.94f, hh * 0.62f)
            c.drawRect(r, stroke(col, h * 0.03f).apply { strokeCap = Paint.Cap.SQUARE; strokeJoin = Paint.Join.MITER })
            line(s.uppercase(), 0f, 0f, r.width() * 0.86f, font(FontKey.Condensed, h * 0.46f, col, bold = true, spacing = 0.06f))
        }
        "script-line" -> {
            line(s, 0f, -hh * 0.1f, w * 0.96f, font(FontKey.Hand, h * 0.78f, col))
            val sw = Path().apply { moveTo(-hw * 0.7f, hh * 0.55f); cubicTo(-hw * 0.2f, hh * 0.38f, hw * 0.3f, hh * 0.75f, hw * 0.75f, hh * 0.45f) }
            c.drawPath(sw, stroke(theme.accent.toInt(), h * 0.035f))
        }
        "magazine" -> {
            val r = RectF(-hw, -hh * 0.72f, hw, hh * 0.72f)
            c.drawRect(r, fill(theme.accent.toInt()))
            line(s.uppercase(), 0f, -hh * 0.06f, w * 0.9f, font(FontKey.Condensed, h * 0.62f, onColor(theme.accent.toInt()), bold = true))
            line("VOL. 01 · SPECIAL EDITION", 0f, hh * 0.5f, w * 0.7f, font(FontKey.Sans, h * 0.08f, onColor(theme.accent.toInt()), spacing = 0.2f))
        }
        "badge-circle" -> {
            val rad = minOf(hw, hh) * 0.92f
            c.drawCircle(0f, 0f, rad, stroke(col, rad * 0.04f))
            c.drawCircle(0f, 0f, rad * 0.66f, stroke(col, rad * 0.02f))
            val arc = Path().apply { addArc(RectF(-rad * 0.8f, -rad * 0.8f, rad * 0.8f, rad * 0.8f), 180f, 180f) }
            val tpaint = font(FontKey.Condensed, rad * 0.22f, col, bold = true, spacing = 0.15f)
            val str = s.uppercase()
            val len = Math.PI.toFloat() * rad * 0.8f
            if (tpaint.measureText(str) > len * 0.95f) tpaint.textSize *= len * 0.95f / tpaint.measureText(str)
            c.drawTextOnPath(str, arc, (len - tpaint.measureText(str)) / 2, tpaint.textSize * 0.35f, tpaint)
            doodle("star", 0f, rad * 0.05f, rad * 0.6f) { pth, _ -> c.drawPath(pth, fill(theme.accent.toInt())) }
        }
        else -> { // date-slash and anything else
            line(s, 0f, 0f, w * 0.94f, font(FontKey.Display, h * 0.6f, col, bold = true))
        }
    }
}

/** A laurel branch curving round one side of the words; [side] -1 = left. */
private fun Art.laurel(x: Float, y: Float, size: Float, side: Float, col: Int) {
    c.save()
    c.translate(x, y)
    if (side > 0) c.scale(-1f, 1f)
    // An arc bowing outwards (to the left before mirroring).
    val rad = size * 0.5f
    val cx = rad * 0.75f
    val stem = Path().apply { addArc(RectF(cx - rad, -rad, cx + rad, rad), 125f, 110f) }
    c.drawPath(stem, stroke(col, size * 0.022f))
    val leaf = fill(col)
    for (i in 0 until 7) {
        val deg = 128f + i * 15f
        val (px, py) = polar(cx, 0f, rad, deg)
        for (k in listOf(-1f, 1f)) {
            c.save()
            c.translate(px, py)
            c.rotate(deg + 90f + k * 38f)
            c.drawOval(RectF(-size * 0.028f, -size * 0.11f, size * 0.028f, 0f), leaf)
            c.restore()
        }
    }
    c.restore()
}

/** Film subtitles: plain sans with a dark edge, one or two lines at the bottom of a frame. */
internal fun Art.subtitle() {
    val col = if (param == "yellow") 0xFFF7E14B.toInt() else 0xFFFFFFFF.toInt()
    val box = RectF(-hw, -hh, hw, hh)
    val pe = font(FontKey.Sans, h * 0.42f, 0xFF000000.toInt())
    pe.style = Paint.Style.STROKE; pe.strokeWidth = h * 0.06f; pe.strokeJoin = Paint.Join.ROUND
    block(text, box, pe)
    val size = pe.textSize
    val pf = font(FontKey.Sans, size, col)
    pf.textSize = size
    // Same size as the edge pass, so the two line up.
    c.save()
    val layout = android.text.StaticLayout.Builder.obtain(text, 0, text.length, pf, box.width().toInt().coerceAtLeast(1))
        .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, 1.05f).setIncludePad(false).build()
    c.translate(box.left, box.centerY() - layout.height / 2f)
    layout.draw(c)
    c.restore()
}

/** Foil: a shape in polished metal. */
internal fun Art.foil() {
    val (shape, metal) = param.split(':').let { it[0] to it.getOrElse(1) { "gold" } }
    val stops = when (metal) {
        "silver" -> intArrayOf(0xFF8E9196.toInt(), 0xFFF4F5F7.toInt(), 0xFFA3A6AB.toInt(), 0xFFFFFFFF.toInt(), 0xFF7E8187.toInt())
        "rose" -> intArrayOf(0xFFB76E79.toInt(), 0xFFF9D4D0.toInt(), 0xFFC4848B.toInt(), 0xFFFBE3DF.toInt(), 0xFF9E5560.toInt())
        "holo" -> intArrayOf(0xFFF7C6E8.toInt(), 0xFFC6E2FA.toInt(), 0xFFD2F7D2.toInt(), 0xFFFDF2B8.toInt(), 0xFFE4CCFA.toInt())
        else -> intArrayOf(0xFF9C7420.toInt(), 0xFFFBEA9C.toInt(), 0xFFC3922E.toInt(), 0xFFFFF5C4.toInt(), 0xFF8E6418.toInt())
    }
    val key = if (shape == "flower") "daisy" else shape
    val closedShape = shape != "flower"
    doodle(key, 0f, 0f, m * 0.92f) { path, _ ->
        val f = fill(stops[0])
        f.shader = LinearGradient(0f, 0f, 100f, 100f, stops, null, Shader.TileMode.MIRROR)
        f.setShadowLayer(2.5f, 0f, 1.2f, 0x44000000)
        if (closedShape) c.drawPath(path, f) else c.drawPath(path, stroke(stops[1], 7f).apply { shader = f.shader })
        val edge = stroke(darker(stops[0], 0.15f), 1.2f)
        c.drawPath(path, edge)
        // Sheen
        if (closedShape) {
            c.save(); c.clipPath(path)
            val sheen = fill(0)
            sheen.shader = LinearGradient(20f, 0f, 60f, 100f, intArrayOf(0x00FFFFFF, 0x99FFFFFF.toInt(), 0x00FFFFFF), floatArrayOf(0.35f, 0.5f, 0.65f), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, 100f, 100f, sheen)
            c.restore()
        }
    }
}

internal fun Art.radial(cx: Float, cy: Float, r: Float, inner: Int, outer: Int): Paint {
    val f = fill(inner)
    f.shader = RadialGradient(cx, cy, r.coerceAtLeast(0.01f), inner, outer, Shader.TileMode.CLAMP)
    return f
}

internal fun polar(cx: Float, cy: Float, r: Float, deg: Float): Pair<Float, Float> {
    val a = Math.toRadians(deg.toDouble())
    return (cx + cos(a) * r).toFloat() to (cy + sin(a) * r).toFloat()
}
