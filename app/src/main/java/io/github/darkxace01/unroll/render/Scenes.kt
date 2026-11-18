package io.github.darkxace01.unroll.render

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import io.github.darkxace01.unroll.model.Pattern
import kotlin.math.abs
import kotlin.math.sin

/**
 * Painted stand-in photos (a beach, mountains, a city at dusk…) for showing off templates before the
 * person has picked any of their own. Deterministic per [seed] so previews don't change.
 */
object Scenes {
    const val COUNT = 10

    fun draw(c: Canvas, r: RectF, seed: Int) {
        val k = abs(seed) % COUNT
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val save = c.save()
        c.clipRect(r)
        val w = r.width(); val h = r.height()
        fun sky(top: Int, bottom: Int, horizon: Float = 1f) { p.shader = LinearGradient(0f, r.top, 0f, r.top + h * horizon, top, bottom, Shader.TileMode.CLAMP); c.drawRect(r, p); p.shader = null }
        fun sun(x: Float, y: Float, rad: Float, col: Int) {
            p.shader = RadialGradient(r.left + x * w, r.top + y * h, rad * 3f, intArrayOf(col and 0x88FFFFFF.toInt(), col and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
            c.drawCircle(r.left + x * w, r.top + y * h, rad * 3f, p); p.shader = null
            p.color = col; c.drawCircle(r.left + x * w, r.top + y * h, rad, p)
        }
        fun ridge(base: Float, amp: Float, freq: Float, phase: Float, col: Int) {
            val path = Path(); path.moveTo(r.left, r.bottom)
            val n = 40
            for (i in 0..n) { val t = i / n.toFloat(); val y = base + amp * (sin(t * freq + phase) * 0.6f + sin(t * freq * 2.3f + phase * 1.7f) * 0.4f); path.lineTo(r.left + t * w, r.top + y * h) }
            path.lineTo(r.right, r.bottom); path.close(); p.color = col; c.drawPath(path, p)
        }
        val m = minOf(w, h)
        when (k) {
            0 -> { // Beach
                sky(0xFF7EC3E8.toInt(), 0xFFF3E6D3.toInt(), 0.55f); sun(0.72f, 0.24f, m * 0.07f, 0xFFFFF4D6.toInt())
                p.shader = LinearGradient(0f, r.top + h * 0.52f, 0f, r.top + h * 0.7f, 0xFF2D8DB8.toInt(), 0xFF5FC1C9.toInt(), Shader.TileMode.CLAMP); c.drawRect(r.left, r.top + h * 0.52f, r.right, r.bottom, p); p.shader = null
                ridge(0.7f, 0.025f, 5f, 1f, 0xFFF1DDB8.toInt()); ridge(0.69f, 0.012f, 18f, 0f, 0x88FFFFFF.toInt()); ridge(0.73f, 0.02f, 4f, 2f, 0xFFEBD3A6.toInt())
            }
            1 -> { // Mountains at dawn
                sky(0xFFF6B99B.toInt(), 0xFFFCE6C8.toInt(), 0.7f); sun(0.3f, 0.42f, m * 0.08f, 0xFFFFF0D8.toInt())
                ridge(0.45f, 0.12f, 6f, 0.5f, 0xFFB98AA3.toInt()); ridge(0.58f, 0.09f, 7f, 2f, 0xFF8C6E8F.toInt()); ridge(0.72f, 0.07f, 5f, 4f, 0xFF5E4C6B.toInt()); ridge(0.86f, 0.04f, 4f, 1f, 0xFF3B3150.toInt())
            }
            2 -> { // City at dusk
                sky(0xFF2B3A67.toInt(), 0xFFF29E7A.toInt(), 0.85f); sun(0.5f, 0.68f, m * 0.1f, 0xFFFFD9A0.toInt())
                val rnd = java.util.Random(seed.toLong())
                var x = r.left
                while (x < r.right) {
                    val bw = w * (0.06f + rnd.nextFloat() * 0.08f); val bh = h * (0.2f + rnd.nextFloat() * 0.35f)
                    p.color = 0xFF1E2140.toInt(); c.drawRect(x, r.bottom - bh, x + bw, r.bottom, p)
                    p.color = 0xCCFFD37A.toInt()
                    var wy = r.bottom - bh + bw * 0.25f
                    while (wy < r.bottom - bw * 0.2f) { var wx = x + bw * 0.18f; while (wx < x + bw - bw * 0.2f) { if (rnd.nextFloat() > 0.45f) c.drawRect(wx, wy, wx + bw * 0.14f, wy + bw * 0.14f, p); wx += bw * 0.28f }; wy += bw * 0.3f }
                    x += bw + w * 0.005f
                }
            }
            3 -> { // Forest lake
                sky(0xFFBFDDE8.toInt(), 0xFFEAF2EC.toInt(), 0.5f)
                ridge(0.38f, 0.05f, 9f, 0f, 0xFF7FA38A.toInt())
                val trees = Path()
                for (i in 0..18) { val tx = r.left + w * (i / 18f); val th = h * (0.18f + (i * 37 % 10) / 60f); trees.moveTo(tx - w * 0.04f, r.top + h * 0.52f); trees.lineTo(tx, r.top + h * 0.52f - th); trees.lineTo(tx + w * 0.04f, r.top + h * 0.52f); trees.close() }
                p.color = 0xFF2F5A45.toInt(); c.drawPath(trees, p)
                p.shader = LinearGradient(0f, r.top + h * 0.52f, 0f, r.bottom, 0xFF5E8C86.toInt(), 0xFF2F5560.toInt(), Shader.TileMode.CLAMP); c.drawRect(r.left, r.top + h * 0.52f, r.right, r.bottom, p); p.shader = null
                c.save(); c.scale(1f, -1f, 0f, r.top + h * 0.52f); p.color = 0x552F5A45; c.drawPath(trees, p); c.restore()
            }
            4 -> { // Desert dunes
                sky(0xFFF8D9A8.toInt(), 0xFFFBEBD2.toInt(), 0.5f); sun(0.75f, 0.2f, m * 0.06f, 0xFFFFFBEF.toInt())
                ridge(0.5f, 0.06f, 3f, 1f, 0xFFE7A86B.toInt()); ridge(0.62f, 0.08f, 2.5f, 3f, 0xFFD98B4E.toInt()); ridge(0.78f, 0.06f, 3.2f, 0.2f, 0xFFC0703A.toInt())
            }
            5 -> { // Flower field
                sky(0xFF9BCBEB.toInt(), 0xFFE7F3F7.toInt(), 0.5f)
                ridge(0.5f, 0.03f, 4f, 0f, 0xFF8EBF73.toInt()); ridge(0.6f, 0.03f, 5f, 2f, 0xFF6FA35A.toInt())
                val rnd = java.util.Random(seed.toLong() + 9)
                for (i in 0 until 140) { val fy = 0.58f + rnd.nextFloat() * 0.42f; p.color = listOf(0xFFF3D34A.toInt(), 0xFFFFFFFF.toInt(), 0xFFE76F8E.toInt(), 0xFFB48BE0.toInt())[i % 4]; c.drawCircle(r.left + rnd.nextFloat() * w, r.top + fy * h, m * 0.004f + fy * m * 0.012f, p) }
            }
            6 -> { // Night sky
                sky(0xFF0E1433.toInt(), 0xFF3B3D7A.toInt(), 1f)
                val rnd = java.util.Random(seed.toLong() + 3)
                p.color = 0xEEFFFFFF.toInt(); for (i in 0 until 90) c.drawCircle(r.left + rnd.nextFloat() * w, r.top + rnd.nextFloat() * h * 0.7f, m * (0.002f + rnd.nextFloat() * 0.005f), p)
                sun(0.7f, 0.25f, m * 0.06f, 0xFFF6F1D6.toInt())
                ridge(0.78f, 0.05f, 5f, 1f, 0xFF15162E.toInt())
            }
            7 -> { // Cafe table (warm close-up)
                p.shader = LinearGradient(r.left, r.top, r.right, r.bottom, 0xFFD9B48F.toInt(), 0xFF9C6B47.toInt(), Shader.TileMode.CLAMP); c.drawRect(r, p); p.shader = null
                p.color = 0x22FFFFFF; for (i in 0 until 12) c.drawRect(r.left, r.top + h * i / 12f, r.right, r.top + h * i / 12f + h * 0.004f, p)
                val cx = r.left + w * 0.45f; val cy = r.top + h * 0.5f
                p.color = 0x33000000; c.drawCircle(cx + m * 0.03f, cy + m * 0.04f, m * 0.27f, p)
                p.color = 0xFFF7F3EC.toInt(); c.drawCircle(cx, cy, m * 0.27f, p)
                p.color = 0xFFF1E8DB.toInt(); c.drawCircle(cx, cy, m * 0.18f, p)
                p.color = 0xFF6B3E22.toInt(); c.drawCircle(cx, cy, m * 0.15f, p)
                p.color = 0xFFC99A6B.toInt(); c.drawOval(cx - m * 0.06f, cy - m * 0.04f, cx + m * 0.06f, cy + m * 0.05f, p)
            }
            8 -> { // Sea cliffs
                sky(0xFFA9D6F2.toInt(), 0xFFE5F2F8.toInt(), 0.45f)
                p.color = 0xFF2A7AA8.toInt(); c.drawRect(r.left, r.top + h * 0.45f, r.right, r.bottom, p)
                val cliff = Path().apply { moveTo(r.left, r.bottom); lineTo(r.left, r.top + h * 0.3f); lineTo(r.left + w * 0.25f, r.top + h * 0.34f); lineTo(r.left + w * 0.42f, r.top + h * 0.55f); lineTo(r.left + w * 0.5f, r.bottom); close() }
                p.color = 0xFFE3D6BE.toInt(); c.drawPath(cliff, p)
                p.color = 0xFFF2EDE3.toInt()
                for (i in 0 until 6) { val hx = r.left + w * (0.04f + i * 0.055f); val hy = r.top + h * (0.25f + i * 0.02f); c.drawRect(hx, hy, hx + w * 0.045f, hy + h * 0.06f, p) }
                p.color = 0xFF3B6FD8.toInt(); for (i in 0 until 3) { val hx = r.left + w * (0.06f + i * 0.11f); c.drawOval(hx, r.top + h * (0.235f + i * 0.04f), hx + w * 0.05f, r.top + h * (0.26f + i * 0.04f), p) }
            }
            else -> { // Snowy peaks
                sky(0xFF8EB6E0.toInt(), 0xFFE9F1F9.toInt(), 0.6f)
                ridge(0.42f, 0.16f, 5f, 2.4f, 0xFFF5F8FB.toInt()); ridge(0.56f, 0.1f, 6f, 1f, 0xFFB7C9DC.toInt()); ridge(0.74f, 0.05f, 4f, 0f, 0xFF4F6B5A.toInt())
            }
        }
        c.restoreToCount(save)
    }
}

/** Background textures, drawn across the whole canvas so they run unbroken over the cuts. */
object Patterns {
    fun draw(c: Canvas, pattern: Pattern, w: Float, h: Float, ink: Int, accent: Int) {
        if (pattern == Pattern.None) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun a(col: Int, alpha: Int) = (col and 0x00FFFFFF) or (alpha shl 24)
        when (pattern) {
            Pattern.Paper, Pattern.Kraft -> {
                if (pattern == Pattern.Kraft) { p.color = 0x55B68A5A; c.drawRect(0f, 0f, w, h, p) }
                val rnd = java.util.Random(42)
                val n = (w * h / 900).toInt().coerceAtMost(40000)
                for (i in 0 until n) { p.color = a(ink, 6 + rnd.nextInt(14)); c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, 0.8f + rnd.nextFloat() * 1.8f, p) }
                p.strokeWidth = 1.4f
                for (i in 0 until n / 12) { p.color = a(ink, 10 + rnd.nextInt(16)); val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h; c.drawLine(x, y, x + 6 + rnd.nextFloat() * 22, y + (rnd.nextFloat() - 0.5f) * 8, p) }
            }
            Pattern.Grid -> { p.color = a(ink, 30); p.strokeWidth = 2f; var x = 0f; while (x < w) { c.drawLine(x, 0f, x, h, p); x += 54f }; var y = 0f; while (y < h) { c.drawLine(0f, y, w, y, p); y += 54f } }
            Pattern.Dots -> { p.color = a(ink, 40); var y = 27f; while (y < h) { var x = 27f; while (x < w) { c.drawCircle(x, y, 3.2f, p); x += 54f }; y += 54f } }
            Pattern.Lines -> { p.color = a(0xFF4F7FC2.toInt(), 50); p.strokeWidth = 2.2f; var y = 120f; while (y < h) { c.drawLine(0f, y, w, y, p); y += 64f } }
            Pattern.Stripes -> { p.color = a(accent, 26); var x = 0f; while (x < w) { c.drawRect(x, 0f, x + 60f, h, p); x += 120f } }
            Pattern.Gingham -> { p.color = a(accent, 34); var x = 0f; while (x < w) { c.drawRect(x, 0f, x + 50f, h, p); x += 100f }; var y = 0f; while (y < h) { c.drawRect(0f, y, w, y + 50f, p); y += 100f } }
            Pattern.Linen -> {
                val rnd = java.util.Random(7); p.strokeWidth = 1.6f
                var x = 0f; while (x < w) { p.color = a(ink, 6 + rnd.nextInt(14)); c.drawLine(x, 0f, x, h, p); x += 5f + rnd.nextFloat() * 4 }
                var y = 0f; while (y < h) { p.color = a(ink, 6 + rnd.nextInt(14)); c.drawLine(0f, y, w, y, p); y += 5f + rnd.nextFloat() * 4 }
            }
            Pattern.Checks -> { p.color = a(accent, 30); val s = 90f; var y = 0f; var r = 0; while (y < h) { var x = (r % 2) * s; while (x < w) { c.drawRect(x, y, x + s, y + s, p); x += 2 * s }; y += s; r++ } }
            Pattern.Waves -> { p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = a(accent, 50); var y = 40f; var r = 0; while (y < h) { c.save(); c.translate(w / 2 + (r % 2) * 40f, y); c.drawPath(Shapes.wave(w + 200f, 26f), p); c.restore(); y += 80f; r++ } }
            Pattern.Confetti -> {
                val rnd = java.util.Random(3)
                val n = (w * h / 14000).toInt()
                for (i in 0 until n) { p.color = a(if (i % 3 == 0) ink else accent, 70 + rnd.nextInt(80)); val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h
                    c.save(); c.rotate(rnd.nextFloat() * 180f, x, y); if (i % 2 == 0) c.drawRoundRect(x - 10f, y - 4f, x + 10f, y + 4f, 3f, 3f, p) else c.drawCircle(x, y, 5f, p); c.restore() }
            }
            Pattern.Stars -> {
                val rnd = java.util.Random(5)
                val n = (w * h / 30000).toInt()
                for (i in 0 until n) { p.color = a(accent, 60 + rnd.nextInt(90)); val s = 14f + rnd.nextFloat() * 22f; c.save(); c.translate(rnd.nextFloat() * w, rnd.nextFloat() * h); c.drawPath(Shapes.path(io.github.darkxace01.unroll.model.ShapeKind.Sparkle, s, s), p); c.restore() }
            }
            Pattern.Halftone -> { p.color = a(ink, 40); var y = 20f; var r = 0; while (y < h) { val rad = 2f + 10f * (y / h); var x = 20f + (r % 2) * 20f; while (x < w) { c.drawCircle(x, y, rad, p); x += 40f }; y += 34f; r++ } }
            Pattern.None -> Unit
        }
    }
}
