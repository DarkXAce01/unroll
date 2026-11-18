package io.github.darkxace01.unroll.render.art

import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import io.github.darkxace01.unroll.model.FontKey
import io.github.darkxace01.unroll.model.Flags
import io.github.darkxace01.unroll.model.ShapeKind
import io.github.darkxace01.unroll.render.Shapes

// Pins & clips, patches, flowers, flags, speech bubbles.

private const val STEEL_LIGHT = 0xFFF2F3F5.toInt()
private const val STEEL = 0xFFB9BDC4.toInt()
private const val STEEL_DARK = 0xFF7C818A.toInt()

private fun Art.metal(x0: Float, y0: Float, x1: Float, y1: Float): Paint = fill(STEEL).apply {
    shader = LinearGradient(x0, y0, x1, y1, intArrayOf(STEEL_DARK, STEEL_LIGHT, STEEL, STEEL_LIGHT, STEEL_DARK), null, Shader.TileMode.CLAMP)
}

/** Pins, clips and staples. [color] tints the plastic or the coating. */
internal fun Art.clip() {
    val shadow = 0x40000000
    when (param) {
        "pushpin" -> {
            val r = minOf(hw, hh * 0.8f) * 0.82f
            val cy = -hh * 0.12f
            c.drawLine(r * 0.2f, cy + r * 0.2f, r * 0.9f, hh * 0.9f, stroke(0x33000000, r * 0.12f))
            val head = fill(color); head.setShadowLayer(r * 0.25f, r * 0.12f, r * 0.2f, 0x55000000)
            c.drawCircle(0f, cy, r, head)
            c.drawCircle(0f, cy, r, radial(-r * 0.35f, cy - r * 0.4f, r * 1.3f, lighter(color, 0.45f), darker(color, 0.25f)))
            c.drawCircle(0f, cy, r * 0.55f, stroke(alpha(darker(color, 0.3f), 120), r * 0.06f))
            c.drawCircle(-r * 0.35f, cy - r * 0.38f, r * 0.18f, fill(0xAAFFFFFF.toInt()))
        }
        "thumbtack" -> {
            val r = hw * 0.8f
            c.drawRect(-r * 0.08f, -hh * 0.05f, r * 0.08f, hh * 0.95f, metal(-r * 0.08f, 0f, r * 0.08f, 0f))
            val head = Path().apply { moveTo(-r, -hh * 0.05f); lineTo(r, -hh * 0.05f); lineTo(r * 0.55f, -hh * 0.4f); lineTo(r * 0.75f, -hh * 0.85f); lineTo(-r * 0.75f, -hh * 0.85f); lineTo(-r * 0.55f, -hh * 0.4f); close() }
            val hp = fill(color); hp.setShadowLayer(r * 0.2f, r * 0.1f, r * 0.15f, shadow); c.drawPath(head, hp)
            hp.clearShadowLayer(); hp.shader = LinearGradient(-r, 0f, r, 0f, intArrayOf(darker(color, 0.25f), lighter(color, 0.35f), darker(color, 0.3f)), null, Shader.TileMode.CLAMP); c.drawPath(head, hp)
        }
        "paperclip" -> {
            val sw = w * 0.11f
            val p = Path().apply {
                val l = -hw + sw; val r = hw - sw; val t = -hh + sw; val b = hh - sw; val mid = (l + r) / 2
                moveTo(mid + (r - mid) * 0.35f, t + h * 0.3f)
                lineTo(mid + (r - mid) * 0.35f, b - (r - l) * 0.25f)
                arcTo(RectF(l + (r - l) * 0.2f, b - (r - l) * 0.9f, r - (r - l) * 0.1f, b - (r - l) * 0.05f).let { RectF(l + (r - l) * 0.15f, b - (r - l) * 0.7f, mid + (r - mid) * 0.35f, b - (r - l) * 0.05f) }, 0f, 180f)
                lineTo(l + (r - l) * 0.15f, t + (r - l) * 0.5f)
                arcTo(RectF(l, t, r, t + (r - l)), 180f, 180f)
                lineTo(r, b - (r - l) * 0.15f)
            }
            val coat = stroke(color, sw); coat.setShadowLayer(sw * 0.5f, sw * 0.3f, sw * 0.5f, shadow); c.drawPath(p, coat)
            coat.clearShadowLayer(); c.drawPath(p, stroke(0x55FFFFFF, sw * 0.3f))
        }
        "binder" -> {
            val bw = w * 0.84f; val top = -hh * 0.05f
            val body = Path().apply { moveTo(-bw / 2, hh * 0.9f); lineTo(bw / 2, hh * 0.9f); lineTo(bw * 0.38f, top); lineTo(-bw * 0.38f, top); close() }
            val f = fill(color); f.setShadowLayer(w * 0.05f, w * 0.02f, w * 0.04f, shadow); c.drawPath(body, f)
            f.clearShadowLayer(); f.shader = LinearGradient(0f, top, 0f, hh, lighter(color, 0.2f), darker(color, 0.25f), Shader.TileMode.CLAMP); c.drawPath(body, f)
            val wire = Path().apply { moveTo(-bw * 0.3f, top + h * 0.04f); lineTo(-bw * 0.36f, -hh * 0.9f); lineTo(bw * 0.36f, -hh * 0.9f); lineTo(bw * 0.3f, top + h * 0.04f) }
            c.drawPath(wire, stroke(STEEL_DARK, w * 0.05f)); c.drawPath(wire, stroke(STEEL_LIGHT, w * 0.022f))
        }
        "bulldog" -> {
            val r = RectF(-hw * 0.9f, -hh * 0.1f, hw * 0.9f, hh * 0.85f)
            val f = metal(r.left, 0f, r.right, 0f); f.setShadowLayer(w * 0.04f, w * 0.02f, w * 0.03f, shadow)
            c.drawRoundRect(r, w * 0.06f, w * 0.06f, f)
            c.drawRoundRect(RectF(r.left, r.top + r.height() * 0.55f, r.right, r.bottom), w * 0.06f, w * 0.06f, fill(alpha(color, 200)))
            c.drawCircle(-hw * 0.6f, hh * 0.18f, w * 0.05f, fill(STEEL_DARK)); c.drawCircle(hw * 0.6f, hh * 0.18f, w * 0.05f, fill(STEEL_DARK))
            val handle = Path().apply { moveTo(-hw * 0.6f, hh * 0.18f); lineTo(-hw * 0.5f, -hh * 0.85f); lineTo(hw * 0.5f, -hh * 0.85f); lineTo(hw * 0.6f, hh * 0.18f) }
            c.drawPath(handle, stroke(STEEL_DARK, w * 0.06f)); c.drawPath(handle, stroke(STEEL_LIGHT, w * 0.025f))
        }
        "safety" -> {
            val sw = h * 0.08f
            val body = Path().apply { moveTo(-hw * 0.62f, -hh * 0.2f); lineTo(hw * 0.78f, -hh * 0.2f); moveTo(-hw * 0.62f, hh * 0.25f); lineTo(hw * 0.7f, -hh * 0.05f) }
            val m1 = stroke(STEEL, sw); m1.setShadowLayer(sw, sw * 0.4f, sw * 0.8f, shadow); c.drawPath(body, m1)
            c.drawPath(body, stroke(STEEL_LIGHT, sw * 0.35f))
            c.drawCircle(-hw * 0.72f, hh * 0.02f, h * 0.24f, stroke(STEEL, sw))
            val cap = RectF(hw * 0.62f, -hh * 0.55f, hw * 0.98f, hh * 0.15f)
            c.drawRoundRect(cap, h * 0.1f, h * 0.1f, fill(color))
        }
        "staple" -> {
            val col = if (luminance(color) > 0.5f) STEEL else 0xFF3A3D42.toInt()
            val p = Path().apply { moveTo(-hw * 0.9f, hh * 0.4f); lineTo(-hw * 0.9f, -hh * 0.2f); lineTo(hw * 0.9f, -hh * 0.2f); lineTo(hw * 0.9f, hh * 0.4f) }
            val s = stroke(col, h * 0.16f).apply { strokeCap = Paint.Cap.BUTT; strokeJoin = Paint.Join.MITER }
            s.setShadowLayer(h * 0.1f, 0f, h * 0.1f, shadow); c.drawPath(p, s)
            c.drawLine(-hw * 0.85f, -hh * 0.26f, hw * 0.85f, -hh * 0.26f, stroke(0x88FFFFFF.toInt(), h * 0.04f))
        }
        else -> { // heart-clip
            val sw = w * 0.07f
            val heart = Shapes.path(ShapeKind.Heart, w * 0.8f, w * 0.72f)
            c.save(); c.translate(0f, -hh + w * 0.45f)
            val s = stroke(color, sw); s.setShadowLayer(sw * 0.6f, sw * 0.3f, sw * 0.5f, shadow); c.drawPath(heart, s)
            c.restore()
            val legs = Path().apply { moveTo(0f, -hh + w * 0.8f); lineTo(-w * 0.12f, hh * 0.92f); moveTo(0f, -hh + w * 0.8f); lineTo(w * 0.12f, hh * 0.92f) }
            c.drawPath(legs, stroke(color, sw))
        }
    }
}

/** An embroidered patch: twill in a colour, a satin-stitch edge, stitched words. */
internal fun Art.patch() {
    val path = when (param) {
        "circle" -> Path().apply { addCircle(0f, 0f, m * 0.47f, Path.Direction.CW) }
        "shield" -> Path().apply { moveTo(-hw * 0.9f, -hh * 0.9f); lineTo(hw * 0.9f, -hh * 0.9f); lineTo(hw * 0.9f, 0f); quadTo(hw * 0.8f, hh * 0.7f, 0f, hh * 0.95f); quadTo(-hw * 0.8f, hh * 0.7f, -hw * 0.9f, 0f); close() }
        "banner" -> Path().apply { moveTo(-hw, -hh * 0.6f); lineTo(hw, -hh * 0.6f); lineTo(hw * 0.88f, 0f); lineTo(hw, hh * 0.6f); lineTo(-hw, hh * 0.6f); lineTo(-hw * 0.88f, 0f); close() }
        "cloud" -> Path().apply { addCircle(-hw * 0.4f, hh * 0.1f, m * 0.3f, Path.Direction.CW); addCircle(hw * 0.05f, -hh * 0.15f, m * 0.38f, Path.Direction.CW); addCircle(hw * 0.5f, hh * 0.15f, m * 0.28f, Path.Direction.CW); addRect(-hw * 0.4f, hh * 0.1f, hw * 0.5f, hh * 0.43f, Path.Direction.CW); op(Path(), Path.Op.UNION) }
        "star" -> Shapes.star(w * 0.98f, h * 0.98f, 5, 0.5f)
        "heart" -> Shapes.path(ShapeKind.Heart, w * 0.95f, h * 0.9f)
        else -> Path().apply { addRoundRect(-hw * 0.96f, -hh * 0.9f, hw * 0.96f, hh * 0.9f, m * 0.12f, m * 0.12f, Path.Direction.CW) }
    }
    val edge = darker(color, 0.35f)
    val f = fill(color); f.setShadowLayer(m * 0.04f, 0f, m * 0.025f, 0x55000000); c.drawPath(path, f)
    f.clearShadowLayer()
    // Twill: fine diagonal threads.
    c.save(); c.clipPath(path)
    val th = stroke(alpha(darker(color, 0.12f), 70), m * 0.006f)
    var x = -hw - h; while (x < hw + h) { c.drawLine(x, hh, x + h, -hh, th); x += m * 0.025f }
    c.restore()
    c.drawPath(path, stroke(edge, m * 0.055f))
    c.drawPath(path, stroke(alpha(lighter(edge, 0.2f), 140), m * 0.01f).apply { pathEffect = DashPathEffect(floatArrayOf(m * 0.012f, m * 0.008f), 0f) })
    val inner = stroke(alpha(0xFFFFFFFF.toInt(), 200), m * 0.012f).apply { pathEffect = DashPathEffect(floatArrayOf(m * 0.03f, m * 0.02f), 0f) }
    c.save(); c.scale(0.86f, 0.86f); c.drawPath(path, inner); c.restore()
    val thread = onColor(color).let { if (it == 0xFFFFFFFF.toInt()) 0xFFFFFBF0.toInt() else darker(color, 0.6f) }
    val tp1 = font(if (param == "banner" || param == "rect") FontKey.Display else FontKey.Hand, m * 0.3f, thread, bold = true)
    tp1.setShadowLayer(m * 0.01f, 0f, m * 0.008f, 0x66000000)
    val box = when (param) { "banner" -> RectF(-hw * 0.72f, -hh * 0.45f, hw * 0.72f, hh * 0.45f); "shield" -> RectF(-hw * 0.65f, -hh * 0.6f, hw * 0.65f, hh * 0.35f); "star" -> RectF(-hw * 0.42f, -hh * 0.2f, hw * 0.42f, hh * 0.32f); "heart" -> RectF(-hw * 0.55f, -hh * 0.42f, hw * 0.55f, hh * 0.2f); else -> RectF(-hw * 0.66f, -hh * 0.55f, hw * 0.66f, hh * 0.55f) }
    block(text, box, tp1, 0.95f)
}

/** Flowers built petal by petal. */
internal fun Art.flower() {
    val r = m * 0.46f
    fun petals(n: Int, len: Float, wid: Float, col: Int, rot: Float = 0f, stroke: Int? = null) {
        for (i in 0 until n) {
            c.save(); c.rotate(rot + i * 360f / n)
            val o = RectF(-wid / 2, -len, wid / 2, 0f)
            c.drawOval(o, fill(col).apply { setShadowLayer(len * 0.06f, 0f, len * 0.03f, 0x30000000) })
            c.drawOval(o, fill(0).apply { shader = LinearGradient(0f, -len, 0f, 0f, 0x00FFFFFF, alpha(darker(col, 0.25f), 90), Shader.TileMode.CLAMP) })
            stroke?.let { c.drawOval(o, stroke(it, len * 0.02f)) }
            c.restore()
        }
    }
    fun centre(rad: Float, col: Int, dots: Int? = null) {
        c.drawCircle(0f, 0f, rad, radial(-rad * 0.3f, -rad * 0.3f, rad * 1.4f, lighter(col, 0.3f), darker(col, 0.3f)))
        dots?.let { d -> val rnd = java.util.Random(5); repeat(d) { val (x, y) = polar(0f, 0f, rad * 0.7f * rnd.nextFloat(), rnd.nextFloat() * 360f); c.drawCircle(x, y, rad * 0.07f, fill(darker(col, 0.5f))) } }
    }
    when (param) {
        "lavender", "leaf-sprig" -> {
            val stem = Path().apply { moveTo(0f, hh * 0.95f); quadTo(-w * 0.08f, 0f, w * 0.02f, -hh * 0.92f) }
            c.drawPath(stem, stroke(0xFF5E8A4E.toInt(), w * 0.05f))
            if (param == "lavender") {
                for (i in 0 until 16) { val t = i / 15f; val y = -hh * 0.9f + t * h * 0.5f; val side = if (i % 2 == 0) -1 else 1
                    c.save(); c.translate(side * w * 0.07f, y); c.rotate(side * 25f)
                    c.drawOval(RectF(-w * 0.1f, -h * 0.03f, w * 0.1f, h * 0.03f), fill(if (i % 3 == 0) 0xFF8E6BC8.toInt() else 0xFFA585D9.toInt())); c.restore() }
            } else {
                for (i in 0 until 7) { val t = i / 6f; val y = hh * 0.7f - t * h * 1.4f * 0.55f; val side = if (i % 2 == 0) -1f else 1f
                    c.save(); c.translate(0f, y); c.rotate(side * 50f)
                    val leaf = Path().apply { moveTo(0f, 0f); quadTo(side * w * 0.22f, -h * 0.07f, side * w * 0.42f, 0f); quadTo(side * w * 0.22f, h * 0.07f, 0f, 0f); close() }
                    c.drawPath(leaf, fill(if (i % 2 == 0) 0xFF6F9E5A.toInt() else 0xFF83B06C.toInt())); c.drawLine(0f, 0f, side * w * 0.38f, 0f, stroke(0x553A5A2E, w * 0.012f)); c.restore() }
            }
        }
        else -> {
            when (param) {
                "daisy" -> { petals(16, r, r * 0.26f, 0xFFFDFCF7.toInt(), stroke = 0x22000000); centre(r * 0.24f, 0xFFF2B630.toInt(), 30) }
                "sunflower" -> { petals(18, r, r * 0.26f, 0xFFE9A21F.toInt(), 10f); petals(18, r * 0.86f, r * 0.24f, 0xFFF6C432.toInt()); centre(r * 0.36f, 0xFF5A3A1E.toInt(), 60) }
                "cosmos" -> { petals(8, r, r * 0.5f, 0xFFF08DB5.toInt(), stroke = 0x33B0457A); centre(r * 0.16f, 0xFFF5C33B.toInt(), 12) }
                "poppy" -> { petals(4, r, r * 1.05f, 0xFFD8322A.toInt(), 45f); petals(4, r * 0.85f, r * 0.9f, 0xFFE8473A.toInt()); centre(r * 0.2f, 0xFF1F1A1C.toInt(), 14) }
                "blossom" -> { petals(5, r * 0.95f, r * 0.75f, 0xFFF9D3DE.toInt(), stroke = 0x33D27C98); centre(r * 0.16f, 0xFFE47C9C.toInt())
                    for (i in 0 until 10) { val (x, y) = polar(0f, 0f, r * 0.32f, i * 36f); c.drawLine(0f, 0f, x, y, stroke(0xFFD76A8A.toInt(), r * 0.02f)); c.drawCircle(x, y, r * 0.035f, fill(0xFFF0B13A.toInt())) } }
                "aster" -> { petals(26, r, r * 0.12f, 0xFF8D6CCF.toInt(), 7f); petals(22, r * 0.75f, r * 0.11f, 0xFFA98BE0.toInt()); centre(r * 0.2f, 0xFFF0C23A.toInt(), 14) }
                "buttercup" -> { petals(5, r * 0.95f, r * 0.82f, 0xFFF7D02F.toInt(), stroke = 0x33B08A00); c.drawCircle(-r * 0.2f, -r * 0.45f, r * 0.12f, fill(0x88FFFFFF.toInt())); centre(r * 0.18f, 0xFFDDA60F.toInt(), 10) }
                "cornflower" -> { for (k in 0 until 10) { c.save(); c.rotate(k * 36f); c.drawPath(Shapes.star(r * 0.5f, r * 1.0f, 3, 0.45f).apply { offset(0f, -r * 0.5f) }, fill(if (k % 2 == 0) 0xFF4B6FD6.toInt() else 0xFF5C82E6.toInt())); c.restore() }; centre(r * 0.18f, 0xFF2B2F6B.toInt()) }
                "camellia" -> { petals(6, r, r * 0.85f, 0xFFC7313F.toInt(), 15f, 0x33000000); petals(6, r * 0.72f, r * 0.62f, 0xFFD94654.toInt(), 45f, 0x33000000); petals(5, r * 0.45f, r * 0.42f, 0xFFE8606C.toInt(), 10f, 0x33000000); centre(r * 0.1f, 0xFFF2C23C.toInt()) }
                else -> { // marigold
                    for ((k, rr) in listOf(1f, 0.82f, 0.64f, 0.46f).withIndex()) petals(14, r * rr, r * 0.3f, listOf(0xFFE07A16.toInt(), 0xFFEE8F22.toInt(), 0xFFF5A733.toInt(), 0xFFF9BC45.toInt())[k], k * 13f, 0x22000000)
                    centre(r * 0.12f, 0xFFB45A0A.toInt())
                }
            }
        }
    }
}

/** A simplified national flag, gently lit. */
internal fun Art.flag() {
    val f = Flags.all.firstOrNull { it.code == param } ?: return
    val r = RectF(-hw, -hh, hw, hh)
    val rad = m * 0.04f
    val body = Path().apply { addRoundRect(r, rad, rad, Path.Direction.CW) }
    val sh = fill(0xFFFFFFFF.toInt()); sh.setShadowLayer(m * 0.05f, 0f, m * 0.03f, 0x44000000); c.drawPath(body, sh)
    c.save(); c.clipPath(body)
    val col = f.colors.map { it.toInt() }
    fun hStripes(cs: List<Int>, top: Float = -hh, bottom: Float = hh, left: Float = -hw, right: Float = hw) { val sH = (bottom - top) / cs.size; cs.forEachIndexed { i, cc -> c.drawRect(left, top + i * sH, right, top + (i + 1) * sH + 0.5f, fill(cc)) } }
    fun vStripes(cs: List<Int>) { val sW = w / cs.size; cs.forEachIndexed { i, cc -> c.drawRect(-hw + i * sW, -hh, -hw + (i + 1) * sW + 0.5f, hh, fill(cc)) } }
    fun star(x: Float, y: Float, s: Float, cc: Int) { c.save(); c.translate(x, y); c.drawPath(Shapes.star(s, s, 5, 0.4f), fill(cc)); c.restore() }
    when (f.layout) {
        Flags.Layout.H -> hStripes(col)
        Flags.Layout.V -> vStripes(col)
        Flags.Layout.Disc -> when (f.code) {
            "jp" -> { c.drawRect(r, fill(col[0])); c.drawCircle(0f, 0f, h * 0.3f, fill(col[1])) }
            "bd" -> { c.drawRect(r, fill(col[0])); c.drawCircle(-w * 0.05f, 0f, h * 0.32f, fill(col[1])) }
            "kr" -> { c.drawRect(r, fill(col[0])); c.drawArc(RectF(-h * 0.25f, -h * 0.25f, h * 0.25f, h * 0.25f), 180f, 180f, true, fill(col[1])); c.drawArc(RectF(-h * 0.25f, -h * 0.25f, h * 0.25f, h * 0.25f), 0f, 180f, true, fill(col[2]))
                c.drawCircle(-h * 0.125f, 0f, h * 0.125f, fill(col[2])); c.drawCircle(h * 0.125f, 0f, h * 0.125f, fill(col[1]))
                for ((x, y) in listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)) for (k in 0..2) { c.save(); c.translate(x * w * 0.32f, y * h * 0.3f); c.rotate(x * y * 35f); c.drawRect(-w * 0.06f, -h * 0.07f + k * h * 0.05f, w * 0.06f, -h * 0.045f + k * h * 0.05f, fill(0xFF000000.toInt())); c.restore() } }
            "in" -> { hStripes(col.take(3)); c.drawCircle(0f, 0f, h * 0.13f, stroke(col[3], h * 0.02f)); for (k in 0 until 12) { val (x, y) = polar(0f, 0f, h * 0.13f, k * 30f); c.drawLine(0f, 0f, x, y, stroke(col[3], h * 0.008f)) } }
            else -> { hStripes(col.take(3)); c.drawCircle(0f, 0f, h * 0.1f, fill(col.last())) }
        }
        Flags.Layout.Cross -> { c.drawRect(r, fill(col[0])); if (f.code == "ch") { val a = h * 0.12f; val l = h * 0.32f; c.drawRect(-a, -l, a, l, fill(col[1])); c.drawRect(-l, -a, l, a, fill(col[1])) } else { val a = h * 0.1f; c.drawRect(-a, -hh, a, hh, fill(col[1])); c.drawRect(-hw, -a, hw, a, fill(col[1])) }
            if (f.code == "ge") for ((x, y) in listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)) { val cx = x * w * 0.27f; val cy = y * h * 0.27f; c.drawRect(cx - h * 0.03f, cy - h * 0.09f, cx + h * 0.03f, cy + h * 0.09f, fill(col[1])); c.drawRect(cx - h * 0.09f, cy - h * 0.03f, cx + h * 0.09f, cy + h * 0.03f, fill(col[1])) } }
        Flags.Layout.Nordic -> { c.drawRect(r, fill(col[0])); val x = -hw + w * 0.36f; val a = h * 0.12f
            c.drawRect(x - a, -hh, x + a, hh, fill(col[1])); c.drawRect(-hw, -a, hw, a, fill(col[1]))
            if (col.size > 2) { val b = a * 0.5f; c.drawRect(x - b, -hh, x + b, hh, fill(col[2])); c.drawRect(-hw, -b, hw, b, fill(col[2])) } }
        Flags.Layout.Saltire -> { if (f.code == "jm") { c.drawRect(r, fill(col[0])); val tri = Path().apply { moveTo(-hw, -hh); lineTo(0f, 0f); lineTo(-hw, hh); close(); moveTo(hw, -hh); lineTo(0f, 0f); lineTo(hw, hh); close() }; c.drawPath(tri, fill(col[2])) } else c.drawRect(r, fill(col[0]))
            val s = stroke(col[1], h * 0.18f).apply { strokeCap = Paint.Cap.SQUARE }; c.drawLine(-hw, -hh, hw, hh, s); c.drawLine(-hw, hh, hw, -hh, s) }
        Flags.Layout.Union -> {
            c.drawRect(r, fill(col[0]))
            val small = f.code != "gb"
            c.save(); if (small) { c.translate(-hw / 2, -hh / 2); c.scale(0.5f, 0.5f) }
            val s1 = stroke(col[1], h * 0.2f).apply { strokeCap = Paint.Cap.SQUARE }; c.drawLine(-hw, -hh, hw, hh, s1); c.drawLine(-hw, hh, hw, -hh, s1)
            val s2 = stroke(col[2], h * 0.07f).apply { strokeCap = Paint.Cap.SQUARE }; c.drawLine(-hw, -hh, hw, hh, s2); c.drawLine(-hw, hh, hw, -hh, s2)
            c.drawRect(-h * 0.17f, -hh, h * 0.17f, hh, fill(col[1])); c.drawRect(-hw, -h * 0.17f, hw, h * 0.17f, fill(col[1]))
            c.drawRect(-h * 0.1f, -hh, h * 0.1f, hh, fill(col[2])); c.drawRect(-hw, -h * 0.1f, hw, h * 0.1f, fill(col[2]))
            c.restore()
            if (small) { val sc = if (f.code == "nz") col[2] else col[1]; for ((x, y) in listOf(0.55f to -0.5f, 0.75f to -0.15f, 0.55f to 0.55f, 0.35f to 0.0f)) star(hw * x, hh * y, h * 0.13f, if (f.code == "nz") 0xFFFFFFFF.toInt() else sc); if (f.code == "nz") for ((x, y) in listOf(0.55f to -0.5f, 0.75f to -0.15f, 0.55f to 0.55f, 0.35f to 0.0f)) star(hw * x, hh * y, h * 0.08f, sc)
                if (f.code == "au") star(-hw / 2, hh * 0.5f, h * 0.2f, col[1]) }
        }
        Flags.Layout.Stars -> {
            val n = if (f.code == "us") 13 else 14
            hStripes((0 until n).map { if (it % 2 == 0) col[0] else col[1] })
            val cw = if (f.code == "us") w * 0.4f else w * 0.5f; val chh = if (f.code == "us") h * 7 / 13f else h * 8 / 14f
            c.drawRect(-hw, -hh, -hw + cw, -hh + chh, fill(col[2]))
            if (f.code == "us") for (i in 0 until 5) for (j in 0 until 6) c.drawCircle(-hw + cw * (j + 0.5f) / 6, -hh + chh * (i + 0.5f) / 5, h * 0.018f, fill(0xFFFFFFFF.toInt()))
            else { c.drawCircle(-hw + cw * 0.4f, -hh + chh / 2, chh * 0.32f, fill(0xFFFFCC00.toInt())); c.drawCircle(-hw + cw * 0.47f, -hh + chh / 2, chh * 0.27f, fill(col[2])); star(-hw + cw * 0.75f, -hh + chh / 2, chh * 0.4f, 0xFFFFCC00.toInt()) }
        }
        Flags.Layout.Diamond -> { c.drawRect(r, fill(col[0])); c.drawPath(Path().apply { moveTo(-hw * 0.85f, 0f); lineTo(0f, -hh * 0.8f); lineTo(hw * 0.85f, 0f); lineTo(0f, hh * 0.8f); close() }, fill(col[1])); c.drawCircle(0f, 0f, h * 0.22f, fill(col[2])) }
        Flags.Layout.Crescent -> { c.drawRect(r, fill(col[0])); if (f.code == "tn") c.drawCircle(0f, 0f, h * 0.3f, fill(col[1]))
            val (bg, fg) = if (f.code == "tn") col[1] to col[0] else col[0] to col[1]
            val cx = if (f.code == "tn") -h * 0.05f else -w * 0.12f
            c.drawCircle(cx, 0f, h * 0.24f, fill(fg)); c.drawCircle(cx + h * 0.06f, 0f, h * 0.19f, fill(bg)); star(cx + h * 0.22f, 0f, h * 0.18f, fg) }
        Flags.Layout.Star -> { c.drawRect(r, fill(col[0])); if (f.code == "ma") c.drawPath(Shapes.star(h * 0.5f, h * 0.5f, 5, 0.4f), stroke(col[1], h * 0.035f)) else star(0f, 0f, h * 0.5f, col[1]) }
    }
    // Special shapes over the stripes.
    when (f.code) {
        "gr" -> { val cw = h * 5 / 9f; c.drawRect(-hw, -hh, -hw + cw, -hh + cw, fill(col[0])); c.drawRect(-hw + cw * 0.4f, -hh, -hw + cw * 0.6f, -hh + cw, fill(col[1])); c.drawRect(-hw, -hh + cw * 0.4f, -hw + cw, -hh + cw * 0.6f, fill(col[1])) }
        "cz", "ph", "za" -> { val tri = Path().apply { moveTo(-hw, -hh); lineTo(if (f.code == "cz") 0f else -hw + w * 0.42f, 0f); lineTo(-hw, hh); close() }
            c.drawPath(tri, fill(when (f.code) { "cz" -> 0xFF11457E.toInt(); "ph" -> 0xFFFFFFFF.toInt(); else -> 0xFF000000.toInt() }))
            if (f.code == "ph") c.drawCircle(-hw + w * 0.13f, 0f, h * 0.08f, fill(0xFFFCD116.toInt())) }
        "cl" -> { c.drawRect(-hw, -hh, -hw + w / 3, 0f, fill(0xFF0039A6.toInt())); star(-hw + w / 6, -hh / 2, h * 0.24f, 0xFFFFFFFF.toInt()) }
        "ae" -> c.drawRect(-hw, -hh, -hw + w * 0.25f, hh, fill(0xFFFF0000.toInt()))
        "ca" -> star(0f, 0f, h * 0.48f, col[0])
        "sg" -> { c.drawCircle(-hw + w * 0.18f, -hh * 0.5f, h * 0.15f, fill(0xFFFFFFFF.toInt())); c.drawCircle(-hw + w * 0.22f, -hh * 0.5f, h * 0.14f, fill(col[0])) }
        "pt" -> { c.drawCircle(-hw + w * 0.4f, 0f, h * 0.18f, fill(0xFFFFE000.toInt())); c.drawCircle(-hw + w * 0.4f, 0f, h * 0.12f, fill(0xFFDA1F26.toInt())) }
    }
    // Fabric light.
    c.drawRect(r, fill(0).apply { shader = LinearGradient(-hw, 0f, hw, 0f, intArrayOf(0x18FFFFFF, 0x10000000, 0x22FFFFFF, 0x14000000), null, Shader.TileMode.CLAMP) })
    c.restore()
}

/** Speech bubbles and thought clouds. */
internal fun Art.bubble() {
    val fillCol = color
    val edge = theme.ink.toInt()
    val sw = m * 0.025f
    val body = when (param) {
        "cloud" -> Path().apply {
            for ((x, y, rr) in listOf(Triple(-0.45f, 0.05f, 0.32f), Triple(-0.15f, -0.25f, 0.36f), Triple(0.25f, -0.22f, 0.34f), Triple(0.5f, 0.08f, 0.3f), Triple(0.15f, 0.25f, 0.32f), Triple(-0.25f, 0.28f, 0.3f)))
                addCircle(hw * x, hh * y * 0.85f, m * rr * 0.62f, Path.Direction.CW)
            op(Path().apply { addOval(RectF(-hw * 0.6f, -hh * 0.4f, hw * 0.65f, hh * 0.45f), Path.Direction.CW) }, Path.Op.UNION)
        }
        "rect" -> Path().apply { addRoundRect(-hw * 0.95f, -hh * 0.85f, hw * 0.95f, hh * 0.5f, m * 0.12f, m * 0.12f, Path.Direction.CW)
            op(Path().apply { moveTo(-hw * 0.4f, hh * 0.4f); lineTo(-hw * 0.6f, hh * 0.95f); lineTo(-hw * 0.05f, hh * 0.4f); close() }, Path.Op.UNION) }
        "shout" -> Shapes.star(w * 0.98f, h * 0.98f, 14, 0.78f)
        "heart" -> Shapes.path(ShapeKind.Heart, w * 0.95f, h * 0.95f)
        else -> Path().apply { addOval(RectF(-hw * 0.95f, -hh * 0.85f, hw * 0.95f, hh * 0.55f), Path.Direction.CW)
            op(Path().apply { moveTo(-hw * 0.35f, hh * 0.35f); quadTo(-hw * 0.45f, hh * 0.8f, -hw * 0.75f, hh * 0.95f); quadTo(-hw * 0.1f, hh * 0.85f, hw * 0.05f, hh * 0.45f); close() }, Path.Op.UNION) }
    }
    val f = fill(fillCol); f.setShadowLayer(m * 0.03f, 0f, m * 0.02f, 0x33000000); c.drawPath(body, f)
    c.drawPath(body, stroke(edge, sw))
    if (param == "cloud") { c.drawCircle(-hw * 0.55f, hh * 0.72f, m * 0.06f, fill(fillCol)); c.drawCircle(-hw * 0.55f, hh * 0.72f, m * 0.06f, stroke(edge, sw)); c.drawCircle(-hw * 0.75f, hh * 0.9f, m * 0.035f, fill(fillCol)); c.drawCircle(-hw * 0.75f, hh * 0.9f, m * 0.035f, stroke(edge, sw)) }
    val box = when (param) { "shout" -> RectF(-hw * 0.5f, -hh * 0.35f, hw * 0.5f, hh * 0.35f); "heart" -> RectF(-hw * 0.5f, -hh * 0.3f, hw * 0.5f, hh * 0.25f); "rect" -> RectF(-hw * 0.8f, -hh * 0.7f, hw * 0.8f, hh * 0.35f); else -> RectF(-hw * 0.7f, -hh * 0.6f, hw * 0.7f, hh * 0.35f) }
    block(text, box, font(if (param == "shout") FontKey.Display else FontKey.Hand, m * 0.32f, edge, bold = param == "shout"), 0.95f)
}
