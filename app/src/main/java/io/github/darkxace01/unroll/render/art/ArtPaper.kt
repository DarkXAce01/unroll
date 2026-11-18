package io.github.darkxace01.unroll.render.art

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import io.github.darkxace01.unroll.model.FontKey
import io.github.darkxace01.unroll.render.Shapes

// Tape, stamps, postmarks, rubber stamps, tickets and labels.

private fun Art.paperShadow(path: Path, col: Int, blur: Float = m * 0.04f) {
    val f = fill(col); f.setShadowLayer(blur, 0f, blur * 0.45f, 0x40000000)
    c.drawPath(path, f)
}

/** Washi tape: a see-through strip with torn ends and a printed pattern. */
internal fun Art.tape() {
    val path = Shapes.tape(w, h)
    val base = color
    val a = when (param) { "clear" -> 0x50; "masking", "kraft" -> 0xEE; else -> 0xD8 }
    c.drawPath(path, fill(alpha(base, a)))
    c.save(); c.clipPath(path)
    val ink = if (luminance(base) > 0.55f) alpha(darker(base, 0.35f), 200) else alpha(0xFFFFFFFF.toInt(), 190)
    val u = h * 0.22f
    when (param) {
        "stripes" -> { var x = -hw - h; while (x < hw + h) { c.drawLine(x, hh, x + h, -hh, stroke(ink, u * 0.45f).apply { strokeCap = Paint.Cap.BUTT }); x += u * 1.4f } }
        "dots" -> { var y = -hh + u * 0.7f; var row = 0; while (y < hh) { var x = -hw + (row % 2) * u; while (x < hw) { c.drawCircle(x, y, u * 0.22f, fill(ink)); x += u * 2 }; y += u; row++ } }
        "grid" -> { val g = stroke(ink, u * 0.1f); var x = -hw; while (x < hw) { c.drawLine(x, -hh, x, hh, g); x += u }; var y = -hh; while (y < hh) { c.drawLine(-hw, y, hw, y, g); y += u } }
        "gingham" -> { val g = fill(alpha(ink, 90)); var x = -hw; while (x < hw) { c.drawRect(x, -hh, x + u * 0.8f, hh, g); x += u * 1.6f }; var y = -hh; while (y < hh) { c.drawRect(-hw, y, hw, y + u * 0.8f, g); y += u * 1.6f } }
        "hearts" -> { var x = -hw + u; var k = 0; while (x < hw) { c.save(); c.translate(x, if (k % 2 == 0) -u * 0.8f else u * 0.8f); c.drawPath(Shapes.path(io.github.darkxace01.unroll.model.ShapeKind.Heart, u * 1.2f, u * 1.1f), fill(ink)); c.restore(); x += u * 1.8f; k++ } }
        "stars" -> { var x = -hw + u; var k = 0; while (x < hw) { c.save(); c.translate(x, if (k % 2 == 0) -u * 0.8f else u * 0.9f); c.drawPath(Shapes.star(u * 1.3f, u * 1.3f, 5, 0.45f), fill(ink)); c.restore(); x += u * 1.7f; k++ } }
        "zigzag" -> { val z = Path(); var x = -hw; z.moveTo(x, u); var up = true; while (x < hw) { x += u; z.lineTo(x, if (up) -u else u); up = !up }; c.drawPath(z, stroke(ink, u * 0.35f)) }
        "wave" -> for (dy in listOf(-u * 1.2f, u * 1.2f)) { c.save(); c.translate(0f, dy); c.drawPath(Shapes.wave(w * 1.1f, u * 1.1f), stroke(ink, u * 0.25f)); c.restore() }
        "checker" -> { val g = fill(alpha(ink, 120)); var y = -hh; var r = 0; while (y < hh) { var x = -hw + (r % 2) * u; while (x < hw) { c.drawRect(x, y, x + u, y + u, g); x += u * 2 }; y += u; r++ } }
        "masking", "kraft" -> { val fibre = stroke(alpha(darker(base, 0.2f), 40), h * 0.012f); val rnd = java.util.Random(7); repeat(28) { val x = -hw + rnd.nextFloat() * w; val y = -hh + rnd.nextFloat() * h; c.drawLine(x, y, x + h * 0.4f, y + (rnd.nextFloat() - 0.5f) * h * 0.1f, fibre) } }
        "words" -> line(text, 0f, 0f, w * 0.8f, font(FontKey.Mono, h * 0.34f, alpha(darker(base, 0.6f), 230), spacing = 0.12f))
        else -> Unit
    }
    // Light catching the top edge.
    c.drawRect(-hw, -hh, hw, -hh + h * 0.08f, fill(0x22FFFFFF))
    c.restore()
}

/** A postage stamp: perforated paper, a coloured picture panel, its value. */
internal fun Art.stamp() {
    val edge = Shapes.scalloped(w, h, m * 0.035f)
    paperShadow(edge, 0xFFFFFCF4.toInt())
    val inset = m * 0.1f
    val panel = RectF(-hw + inset, -hh + inset, hw - inset, hh - inset)
    c.drawRect(panel, fill(color))
    c.save(); c.clipRect(panel)
    // A soft sun / horizon behind the picture.
    c.drawCircle(panel.centerX(), panel.top + panel.height() * 0.42f, panel.width() * 0.42f, fill(alpha(lighter(color, 0.35f), 255)))
    c.drawRect(panel.left, panel.bottom - panel.height() * 0.2f, panel.right, panel.bottom, fill(darker(color, 0.18f)))
    c.restore()
    doodle(param, panel.centerX(), panel.top + panel.height() * 0.45f, panel.width() * 0.66f) { path, _ ->
        c.drawPath(path, stroke(0xFFFFFFFF.toInt(), 4.2f))
    }
    c.drawRect(RectF(panel.left + inset * 0.25f, panel.top + inset * 0.25f, panel.right - inset * 0.25f, panel.bottom - inset * 0.25f), stroke(0xAAFFFFFF.toInt(), m * 0.008f))
    val vp = font(FontKey.Display, panel.height() * 0.13f, 0xFFFFFFFF.toInt(), bold = true)
    c.drawText(text, panel.right - vp.measureText(text) - inset * 0.45f, panel.top + vp.textSize + inset * 0.3f, vp)
    val lp = font(FontKey.Condensed, panel.height() * 0.07f, 0xFFFFFFFF.toInt(), bold = true, spacing = 0.25f)
    c.drawText("POSTAGE", panel.left + inset * 0.45f, panel.bottom - inset * 0.45f, lp)
}

/** A franking mark: rings and words in slightly patchy ink. */
internal fun Art.postmark() {
    val col = alpha(color, 215)
    val layer = c.saveLayer(-hw - 4, -hh - 4, hw + 4, hh + 4, null)
    val sw = m * 0.025f
    when (param) {
        "wavy" -> {
            val r = h * 0.46f
            val cx = -hw + r + sw
            ring(cx, 0f, r, col, sw)
            val wl = stroke(col, sw)
            for (i in -2..2) { c.save(); c.translate(cx + r + (w - 2 * r) / 2 - sw, i * h * 0.15f); c.drawPath(Shapes.wave(w - 2 * r - sw * 4, h * 0.08f), wl); c.restore() }
        }
        "oval" -> {
            val r = RectF(-hw * 0.95f, -hh * 0.7f, hw * 0.95f, hh * 0.7f)
            c.drawOval(r, stroke(col, sw)); r.inset(sw * 2.4f, sw * 2.4f); c.drawOval(r, stroke(col, sw * 0.6f))
            line(text.uppercase(), 0f, -hh * 0.08f, r.width() * 0.75f, font(FontKey.Condensed, h * 0.18f, col, bold = true, spacing = 0.1f))
            line("★ AIR MAIL ★", 0f, hh * 0.22f, r.width() * 0.6f, font(FontKey.Condensed, h * 0.09f, col, bold = true, spacing = 0.2f))
        }
        "box" -> {
            val r = RectF(-hw * 0.94f, -hh * 0.6f, hw * 0.94f, hh * 0.6f)
            c.drawRect(r, stroke(col, sw).apply { strokeJoin = Paint.Join.MITER }); r.inset(sw * 2.4f, sw * 2.4f); c.drawRect(r, stroke(col, sw * 0.6f).apply { strokeJoin = Paint.Join.MITER })
            line(text.uppercase(), 0f, -hh * 0.08f, r.width() * 0.8f, font(FontKey.Condensed, h * 0.18f, col, bold = true, spacing = 0.12f))
            line("POSTED · PAR AVION", 0f, hh * 0.25f, r.width() * 0.7f, font(FontKey.Condensed, h * 0.08f, col, spacing = 0.2f))
        }
        else -> ring(0f, 0f, m * 0.46f, col, sw)
    }
    // Patchy ink: knock out a sprinkle of dots.
    val eraser = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT); color = 0xFF000000.toInt() }
    val rnd = java.util.Random(text.hashCode().toLong())
    repeat(90) { eraser.alpha = 60 + rnd.nextInt(160); c.drawCircle(-hw + rnd.nextFloat() * w, -hh + rnd.nextFloat() * h, m * (0.004f + rnd.nextFloat() * 0.012f), eraser) }
    c.restoreToCount(layer)
}

private fun Art.ring(cx: Float, cy: Float, r: Float, col: Int, sw: Float) {
    c.drawCircle(cx, cy, r, stroke(col, sw))
    c.drawCircle(cx, cy, r * 0.64f, stroke(col, sw * 0.6f))
    val arc = Path().apply { addArc(RectF(cx - r * 0.82f, cy - r * 0.82f, cx + r * 0.82f, cy + r * 0.82f), 180f, 180f) }
    val tpaint = font(FontKey.Condensed, r * 0.24f, col, bold = true, spacing = 0.12f)
    val str = text.uppercase()
    val len = Math.PI.toFloat() * r * 0.82f
    if (tpaint.measureText(str) > len * 0.92f) tpaint.textSize *= len * 0.92f / tpaint.measureText(str)
    c.drawTextOnPath(str, arc, (len - tpaint.measureText(str)) / 2, tpaint.textSize * 0.36f, tpaint)
    val bottom = Path().apply { addArc(RectF(cx - r * 0.82f, cy - r * 0.82f, cx + r * 0.82f, cy + r * 0.82f), 160f, -140f) }
    val bp = font(FontKey.Condensed, r * 0.16f, col, bold = true, spacing = 0.2f)
    val bl = (140f / 360f) * 2 * Math.PI.toFloat() * r * 0.82f
    c.drawTextOnPath("★ POST ★", bottom, (bl - bp.measureText("★ POST ★")) / 2, bp.textSize * 0.9f, bp)
    line("NOV 26", cx, cy, r * 1.1f, font(FontKey.Condensed, r * 0.3f, col, bold = true))
}

/** A rubber ink stamp: double border, capitals, uneven ink. */
internal fun Art.rubber() {
    val col = alpha(color, 225)
    val layer = c.saveLayer(-hw - 4, -hh - 4, hw + 4, hh + 4, null)
    val sw = m * 0.06f
    val r = RectF(-hw + sw, -hh + sw, hw - sw, hh - sw)
    if (param == "oval") {
        c.drawOval(r, stroke(col, sw)); val r2 = RectF(r); r2.inset(sw * 1.4f, sw * 1.4f); c.drawOval(r2, stroke(col, sw * 0.4f))
        line(text.uppercase(), 0f, 0f, r2.width() * 0.8f, font(FontKey.Condensed, h * 0.3f, col, bold = true, spacing = 0.08f))
    } else {
        c.drawRoundRect(r, sw, sw, stroke(col, sw)); val r2 = RectF(r); r2.inset(sw * 1.4f, sw * 1.4f); c.drawRoundRect(r2, sw * 0.5f, sw * 0.5f, stroke(col, sw * 0.4f))
        line(text.uppercase(), 0f, 0f, r2.width() * 0.86f, font(FontKey.Condensed, h * 0.4f, col, bold = true, spacing = 0.08f))
    }
    val eraser = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT); color = 0xFF000000.toInt() }
    val rnd = java.util.Random(text.hashCode().toLong() + 3)
    repeat(140) { eraser.alpha = 80 + rnd.nextInt(170); c.drawCircle(-hw + rnd.nextFloat() * w, -hh + rnd.nextFloat() * h, m * (0.005f + rnd.nextFloat() * 0.016f), eraser) }
    c.restoreToCount(layer)
}

private fun Art.barcode(r: RectF, col: Int, vertical: Boolean = false) {
    val rnd = java.util.Random(text.hashCode().toLong())
    val len = if (vertical) r.height() else r.width()
    var pos = 0f
    val bar = fill(col)
    while (pos < len) {
        val bw = len * (0.008f + rnd.nextInt(4) * 0.007f)
        if (vertical) c.drawRect(r.left, r.top + pos, r.right, r.top + pos + bw, bar) else c.drawRect(r.left + pos, r.top, r.left + pos + bw, r.bottom, bar)
        pos += bw + len * (0.01f + rnd.nextInt(3) * 0.006f)
    }
}

/** Tickets and tags. [color] is the paper. */
internal fun Art.ticket() {
    val paper = color
    val dark = 0xFF1F2A44.toInt()
    val band = when (param) { "train" -> 0xFF2F6E4F.toInt(); "admit" -> 0xFFC2362B.toInt(); else -> 0xFF233063.toInt() }
    val place = text.uppercase()
    when (param) {
        "boarding", "train" -> {
            val body = Shapes.path(io.github.darkxace01.unroll.model.ShapeKind.Ticket, w, h)
            paperShadow(body, paper)
            c.save(); c.clipPath(body)
            c.drawRect(-hw, -hh, hw, -hh + h * 0.24f, fill(band))
            c.restore()
            val head = if (param == "train") "RAIL PASS" else "BOARDING PASS"
            val hp = font(FontKey.Condensed, h * 0.11f, 0xFFFFFFFF.toInt(), bold = true, spacing = 0.25f)
            c.drawText(head, -hw + w * 0.06f, -hh + h * 0.16f, hp)
            val small = font(FontKey.Condensed, h * 0.065f, alpha(dark, 160), bold = true, spacing = 0.2f)
            c.drawText("FROM", -hw + w * 0.06f, -hh + h * 0.38f, small)
            c.drawText("TO", -hw + w * 0.43f, -hh + h * 0.38f, small)
            val big = font(FontKey.Display, h * 0.19f, dark, bold = true)
            if (big.measureText("HOME") > w * 0.24f) big.textSize *= w * 0.24f / big.measureText("HOME")
            c.drawText("HOME", -hw + w * 0.06f, -hh + h * 0.58f, big)
            val tp2 = font(FontKey.Display, h * 0.19f, band, bold = true)
            val maxTo = w * 0.27f
            if (tp2.measureText(place) > maxTo) tp2.textSize *= maxTo / tp2.measureText(place)
            c.drawText(place, -hw + w * 0.43f, -hh + h * 0.58f, tp2)
            doodle(if (param == "train") "train" else "plane", -hw + w * 0.365f, -hh + h * 0.51f, h * 0.13f) { p, _ -> c.drawPath(p, stroke(dark, 6f)) }
            val info = font(FontKey.Mono, h * 0.07f, dark, spacing = 0.05f)
            val line = "GATE A7   SEAT 12A   BOARDS 07:40"
            if (info.measureText(line) > w * 0.58f) info.textSize *= w * 0.58f / info.measureText(line)
            c.drawText(line, -hw + w * 0.06f, -hh + h * 0.78f, info)
            barcode(RectF(hw * 0.52f, -hh + h * 0.2f, hw * 0.88f, hh - h * 0.2f), dark, vertical = true)
        }
        "admit" -> {
            val body = Path().apply {
                addRect(-hw, -hh, hw, hh, Path.Direction.CW)
                val bites = Path(); val r = h * 0.16f
                for ((x, y) in listOf(-hw to -hh, hw to -hh, -hw to hh, hw to hh)) bites.addCircle(x, y, r, Path.Direction.CW)
                op(bites, Path.Op.DIFFERENCE)
            }
            paperShadow(body, band)
            val inner = RectF(-hw + w * 0.06f, -hh + h * 0.14f, hw - w * 0.06f, hh - h * 0.14f)
            c.drawRect(inner, stroke(alpha(paper, 220), h * 0.02f))
            line("ADMIT ONE", 0f, -hh * 0.12f, inner.width() * 0.8f, font(FontKey.Condensed, h * 0.3f, paper, bold = true, spacing = 0.1f))
            line("№ 004217 · ${place}", 0f, hh * 0.42f, inner.width() * 0.7f, font(FontKey.Condensed, h * 0.09f, paper, bold = true, spacing = 0.2f))
        }
        "luggage" -> {
            val body = Path().apply {
                moveTo(-hw * 0.6f, -hh); lineTo(hw * 0.6f, -hh); lineTo(hw, -hh + w * 0.4f); lineTo(hw, hh); lineTo(-hw, hh); lineTo(-hw, -hh + w * 0.4f); close()
                addCircle(0f, -hh + w * 0.28f, w * 0.09f, Path.Direction.CCW)
            }
            paperShadow(body, paper)
            c.drawCircle(0f, -hh + w * 0.28f, w * 0.13f, stroke(0xFFC9B48A.toInt(), w * 0.04f))
            c.drawRect(-hw, -hh + h * 0.36f, hw, -hh + h * 0.46f, fill(band))
            line("BAGGAGE", 0f, -hh + h * 0.41f, w * 0.8f, font(FontKey.Condensed, h * 0.05f, 0xFFFFFFFF.toInt(), bold = true, spacing = 0.3f))
            val by = -hh + h * 0.69f
            c.save(); c.translate(0f, by); c.rotate(-90f)
            line(place, 0f, 0f, h * 0.34f, font(FontKey.Display, w * 0.3f, dark, bold = true))
            c.restore()
            barcode(RectF(-hw * 0.7f, hh - h * 0.12f, hw * 0.7f, hh - h * 0.05f), dark)
        }
        "receipt" -> {
            val body = Path().apply {
                val teeth = 12; val tw = w / teeth; val d = h * 0.025f
                moveTo(-hw, -hh)
                for (i in 0 until teeth) { lineTo(-hw + tw * (i + 0.5f), -hh + d); lineTo(-hw + tw * (i + 1), -hh) }
                lineTo(hw, hh)
                for (i in 0 until teeth) { lineTo(hw - tw * (i + 0.5f), hh - d); lineTo(hw - tw * (i + 1), hh) }
                close()
            }
            paperShadow(body, paper)
            val mono = font(FontKey.Mono, w * 0.07f, dark)
            val lines = listOf(place, "* * * * * * * *", "SUNSETS      x3", "GELATO       x2", "NEW FRIENDS  x1", "MEMORIES     ∞", "- - - - - - - -", "TOTAL  PRICELESS", "", "THANK YOU, COME AGAIN")
            val box = RectF(-hw + w * 0.08f, -hh + h * 0.06f, hw - w * 0.08f, hh - h * 0.14f)
            block(lines.joinToString("\n"), box, mono, 1.15f)
            barcode(RectF(-hw * 0.7f, hh - h * 0.11f, hw * 0.7f, hh - h * 0.05f), dark)
        }
        "stub" -> {
            val body = Path().apply { addRoundRect(-hw, -hh, hw, hh, h * 0.06f, h * 0.06f, Path.Direction.CW)
                val holes = Path(); var y = -hh + h * 0.1f; while (y < hh) { holes.addCircle(-hw + w * 0.24f, y, h * 0.03f, Path.Direction.CW); y += h * 0.14f }
                op(holes, Path.Op.DIFFERENCE) }
            paperShadow(body, paper)
            c.save(); c.translate(-hw + w * 0.12f, 0f); c.rotate(-90f); line("№ 0427", 0f, 0f, h * 0.8f, font(FontKey.Condensed, w * 0.08f, band, bold = true, spacing = 0.2f)); c.restore()
            line(place, w * 0.12f, -hh * 0.2f, w * 0.68f, font(FontKey.Display, h * 0.28f, dark, bold = true))
            line("ONE WAY · ECONOMY", w * 0.12f, hh * 0.45f, w * 0.6f, font(FontKey.Condensed, h * 0.1f, band, bold = true, spacing = 0.2f))
        }
        "boardingpass-vertical" -> {
            val body = Path().apply { addRoundRect(-hw, -hh, hw, hh, w * 0.06f, w * 0.06f, Path.Direction.CW)
                val n = Path(); n.addCircle(-hw, hh * 0.45f, w * 0.07f, Path.Direction.CW); n.addCircle(hw, hh * 0.45f, w * 0.07f, Path.Direction.CW); op(n, Path.Op.DIFFERENCE) }
            paperShadow(body, paper)
            c.save(); c.clipPath(body); c.drawRect(-hw, -hh, hw, -hh + h * 0.12f, fill(band)); c.restore()
            line("BOARDING PASS", 0f, -hh + h * 0.06f, w * 0.8f, font(FontKey.Condensed, h * 0.04f, 0xFFFFFFFF.toInt(), bold = true, spacing = 0.25f))
            line("HOME", 0f, -hh + h * 0.22f, w * 0.8f, font(FontKey.Display, h * 0.08f, dark, bold = true))
            doodle("plane", 0f, -hh + h * 0.32f, w * 0.26f) { p, _ -> c.save(); c.rotate(90f, 50f, 50f); c.drawPath(p, stroke(band, 6f)); c.restore() }
            line(place, 0f, -hh + h * 0.43f, w * 0.85f, font(FontKey.Display, h * 0.08f, band, bold = true))
            c.drawLine(-hw * 0.8f, hh * 0.45f, hw * 0.8f, hh * 0.45f, stroke(alpha(dark, 90), h * 0.006f).apply { pathEffect = android.graphics.DashPathEffect(floatArrayOf(h * 0.015f, h * 0.012f), 0f) })
            barcode(RectF(-hw * 0.75f, hh * 0.6f, hw * 0.75f, hh * 0.86f), dark)
        }
        else -> { // barcode
            val body = Path().apply { addRect(-hw, -hh, hw, hh, Path.Direction.CW) }
            paperShadow(body, paper)
            barcode(RectF(-hw * 0.86f, -hh * 0.7f, hw * 0.86f, hh * 0.3f), dark)
            line(text.uppercase(), 0f, hh * 0.62f, w * 0.8f, font(FontKey.Mono, h * 0.2f, dark, spacing = 0.3f))
        }
    }
}

/** Labels and notes. [color] is the paper or tape. */
internal fun Art.label() {
    val paper = color
    val inkCol = onColor(paper).let { if (it == 0xFFFFFFFF.toInt()) it else 0xFF262421.toInt() }
    when (param) {
        "dymo" -> {
            val r = RectF(-hw, -hh, hw, hh)
            val f = fill(paper); f.setShadowLayer(h * 0.08f, 0f, h * 0.04f, 0x44000000)
            c.drawRoundRect(r, h * 0.12f, h * 0.12f, f)
            c.drawRect(-hw, -hh, hw, -hh + h * 0.14f, fill(0x22FFFFFF))
            val raised = if (luminance(paper) > 0.6f) 0xFF1D1E22.toInt() else 0xFFF5F5F2.toInt()
            val tpaint = font(FontKey.Condensed, h * 0.56f, alpha(0xFF000000.toInt(), 90), bold = true, spacing = 0.18f)
            c.save(); c.translate(h * 0.025f, h * 0.03f); line(text.uppercase(), 0f, 0f, w * 0.88f, tpaint); c.restore()
            line(text.uppercase(), 0f, 0f, w * 0.88f, font(FontKey.Condensed, h * 0.56f, raised, bold = true, spacing = 0.18f))
        }
        "price" -> {
            val body = Path().apply { moveTo(-hw, 0f); lineTo(-hw + h * 0.4f, -hh); lineTo(hw, -hh); lineTo(hw, hh); lineTo(-hw + h * 0.4f, hh); close()
                addCircle(-hw + h * 0.36f, 0f, h * 0.08f, Path.Direction.CCW) }
            paperShadow(body, paper)
            line(text, h * 0.2f, 0f, w * 0.62f, font(FontKey.Hand, h * 0.6f, inkCol))
        }
        "kraft-tag" -> {
            val body = Path().apply { moveTo(-hw, -hh + w * 0.35f); lineTo(-hw + w * 0.35f, -hh); lineTo(hw - w * 0.35f, -hh); lineTo(hw, -hh + w * 0.35f); lineTo(hw, hh); lineTo(-hw, hh); close()
                addCircle(0f, -hh + w * 0.3f, w * 0.08f, Path.Direction.CCW) }
            paperShadow(body, 0xFFC9A274.toInt())
            c.drawCircle(0f, -hh + w * 0.3f, w * 0.12f, stroke(0xFFE9DCC2.toInt(), w * 0.04f))
            val loop = Path().apply { moveTo(0f, -hh + w * 0.3f); cubicTo(-w * 0.3f, -hh - w * 0.2f, w * 0.4f, -hh - w * 0.3f, w * 0.1f, -hh - w * 0.05f) }
            c.drawPath(loop, stroke(paper, w * 0.035f))
            c.save(); c.translate(0f, h * 0.12f); c.rotate(-90f)
            block(text, RectF(-h * 0.33f, -w * 0.4f, h * 0.33f, w * 0.4f), font(FontKey.Hand, w * 0.4f, 0xFF3A2A1C.toInt()))
            c.restore()
        }
        "index", "lined" -> {
            val body = Path().apply { addRect(-hw, -hh, hw, hh, Path.Direction.CW) }
            paperShadow(body, 0xFFFFFDF8.toInt())
            val gap = h * if (param == "index") 0.14f else 0.09f
            var y = -hh + gap * 1.6f
            while (y < hh - gap * 0.3f) { c.drawLine(-hw, y, hw, y, stroke(0x557BA7D9, h * 0.006f)); y += gap }
            if (param == "index") c.drawLine(-hw, -hh + gap * 1.1f, hw, -hh + gap * 1.1f, stroke(0xAAE2675F.toInt(), h * 0.008f))
            else { c.drawLine(-hw + w * 0.16f, -hh, -hw + w * 0.16f, hh, stroke(0xAAE2675F.toInt(), w * 0.006f)); for (k in 0..2) c.drawCircle(-hw + w * 0.07f, -hh + h * (0.2f + k * 0.3f), w * 0.03f, fill(0xFFE8E2D6.toInt())) }
            c.drawRect(-hw, -hh, hw, -hh + h * 0.04f, fill(alpha(paper, 220)))
            block(text, RectF(-hw + w * (if (param == "lined") 0.2f else 0.08f), -hh + gap * 1.3f, hw - w * 0.06f, hh - gap * 0.4f), font(FontKey.Hand, h * 0.2f, 0xFF2B3A67.toInt()), 1.0f)
        }
        "typewriter" -> {
            val body = Path().apply {
                val rnd = java.util.Random(text.hashCode().toLong()); moveTo(-hw, -hh)
                var x = -hw; while (x < hw) { x += w * 0.04f; lineTo(x.coerceAtMost(hw), -hh + rnd.nextFloat() * h * 0.06f) }
                lineTo(hw, hh); x = hw; while (x > -hw) { x -= w * 0.04f; lineTo(x.coerceAtLeast(-hw), hh - rnd.nextFloat() * h * 0.06f) }; close()
            }
            paperShadow(body, 0xFFFBF8F1.toInt(), h * 0.08f)
            line(text, 0f, 0f, w * 0.9f, font(FontKey.Mono, h * 0.42f, 0xFF1F1F1F.toInt()))
        }
        "masking-label" -> {
            c.drawPath(Shapes.tape(w, h), fill(0xEEE8DCC0.toInt()))
            line(text, 0f, 0f, w * 0.84f, font(FontKey.Hand, h * 0.7f, paper.takeIf { luminance(it) < 0.75f } ?: 0xFF1D1E22.toInt()))
        }
        "sticky" -> {
            val body = Path().apply { moveTo(-hw, -hh); lineTo(hw, -hh); lineTo(hw, hh - h * 0.14f); lineTo(hw - h * 0.14f, hh); lineTo(-hw, hh); close() }
            val f = fill(lighter(paper, 0.45f)); f.setShadowLayer(m * 0.05f, m * 0.01f, m * 0.035f, 0x40000000); c.drawPath(body, f)
            c.drawRect(-hw, -hh, hw, -hh + h * 0.12f, fill(alpha(darker(paper, 0.05f), 60)))
            val fold = Path().apply { moveTo(hw, hh - h * 0.14f); lineTo(hw - h * 0.14f, hh - h * 0.12f); lineTo(hw - h * 0.14f, hh); close() }
            c.drawPath(fold, fill(darker(lighter(paper, 0.45f), 0.12f)))
            block(text, RectF(-hw + w * 0.1f, -hh + h * 0.16f, hw - w * 0.1f, hh - h * 0.12f), font(FontKey.Hand, h * 0.24f, 0xFF262421.toInt()), 1.0f)
        }
        "torn-note" -> {
            val body = Path().apply {
                val rnd = java.util.Random(11); moveTo(-hw, -hh + h * 0.04f)
                var x = -hw; while (x < hw) { x += w * 0.035f; lineTo(x.coerceAtMost(hw), -hh + rnd.nextFloat() * h * 0.08f) }
                lineTo(hw, hh); lineTo(-hw, hh); close()
            }
            paperShadow(body, lighter(paper, 0.6f))
            block(text, RectF(-hw + w * 0.1f, -hh + h * 0.15f, hw - w * 0.1f, hh - h * 0.1f), font(FontKey.Hand, h * 0.24f, 0xFF262421.toInt()), 1.0f)
        }
        else -> { // date-tab
            val r = RectF(-hw, -hh, hw, hh)
            val f = fill(paper); f.setShadowLayer(h * 0.08f, 0f, h * 0.04f, 0x33000000)
            c.drawRoundRect(r, hh, hh, f)
            line(text.uppercase(), 0f, 0f, w * 0.8f, font(FontKey.Condensed, h * 0.48f, onColor(paper), bold = true, spacing = 0.12f))
        }
    }
}
