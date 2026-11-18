package io.github.darkxace01.unroll.render

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.DiscretePathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import io.github.darkxace01.unroll.model.ColorRole
import io.github.darkxace01.unroll.model.FrameKind
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Photo frames: the outline a photo is cut to, the mount it sits on (paper, a phone, a ticket) and what
 * is drawn over it (lace, a scribble, tape). All in the item's own coordinates, centred on (0,0).
 */
object Mounts {

    /** Mounts that have their own colour when left on the theme's paper (a phone is dark, a negative orange). */
    fun natural(kind: FrameKind): Int? = when (kind) {
        FrameKind.Film -> 0xFF141414.toInt()
        FrameKind.Phone -> 0xFF1B1B1E.toInt()
        FrameKind.Camera -> 0xFF2B2B2E.toInt()
        FrameKind.Negative -> 0xFFB65F27.toInt()
        FrameKind.Message -> 0xFF3478F6.toInt()
        FrameKind.Window -> 0xFFF4EFE6.toInt()
        else -> null
    }

    fun mountColor(kind: FrameKind, role: ColorRole?, roleColor: Int): Int =
        if (role == ColorRole.Paper) natural(kind) ?: roleColor else roleColor

    /** The outside shape: what the shadow and mount fill. */
    fun outline(kind: FrameKind, w: Float, h: Float, radius: Float): Path {
        val r = RectF(-w / 2, -h / 2, w / 2, h / 2)
        val m = min(w, h)
        return when (kind) {
            FrameKind.Circle -> Path().apply { addOval(r, Path.Direction.CW) }
            FrameKind.Oval -> Path().apply { addOval(r, Path.Direction.CW) }
            FrameKind.Arch -> Path().apply {
                val rad = w / 2
                moveTo(-w / 2, h / 2)
                lineTo(-w / 2, -h / 2 + rad)
                arcTo(RectF(-w / 2, -h / 2, w / 2, -h / 2 + 2 * rad), 180f, 180f)
                lineTo(w / 2, h / 2)
                close()
            }
            FrameKind.Stamp -> Shapes.scalloped(w, h, m * 0.035f)
            FrameKind.Torn -> Shapes.torn(w, h)
            FrameKind.Heart -> Shapes.path(io.github.darkxace01.unroll.model.ShapeKind.Heart, w, h)
            FrameKind.Blob -> Shapes.path(io.github.darkxace01.unroll.model.ShapeKind.Blob, w, h)
            FrameKind.Deckle -> deckle(w, h)
            FrameKind.Lace -> lace(w, h)
            FrameKind.Phone -> Path().apply { addRoundRect(r, w * 0.13f, w * 0.13f, Path.Direction.CW) }
            FrameKind.Camera -> Path().apply { addRoundRect(r, m * 0.08f, m * 0.08f, Path.Direction.CW) }
            FrameKind.Browser, FrameKind.Popup, FrameKind.Player -> Path().apply { addRoundRect(r, m * 0.05f, m * 0.05f, Path.Direction.CW) }
            FrameKind.Message -> Path().apply {
                addRoundRect(-w / 2, -h / 2, w / 2, h / 2 - 0.07f * h, m * 0.08f, m * 0.08f, Path.Direction.CW)
                op(Path().apply { moveTo(w / 2 - m * 0.16f, h / 2 - 0.1f * h); quadTo(w / 2 - m * 0.06f, h / 2 - 0.02f * h, w / 2, h / 2); quadTo(w / 2 - m * 0.2f, h / 2 - 0.02f * h, w / 2 - m * 0.3f, h / 2 - 0.08f * h); close() }, Path.Op.UNION)
            }
            FrameKind.Ticket -> Path().apply {
                addRoundRect(r, h * 0.06f, h * 0.06f, Path.Direction.CW)
                val bites = Path(); bites.addCircle(w / 2 - 0.3f * w, -h / 2, h * 0.08f, Path.Direction.CW); bites.addCircle(w / 2 - 0.3f * w, h / 2, h * 0.08f, Path.Direction.CW)
                op(bites, Path.Op.DIFFERENCE)
            }
            FrameKind.Slide -> Path().apply { addRoundRect(r, m * 0.06f, m * 0.06f, Path.Direction.CW) }
            else -> Path().apply { if (radius > 0f) addRoundRect(r, radius, radius, Path.Direction.CW) else addRect(r, Path.Direction.CW) }
        }
    }

    /** What the photo is cut to inside a mount. */
    fun window(kind: FrameKind, inner: RectF, w: Float): Path = Path().apply {
        val m = min(inner.width(), inner.height())
        when (kind) {
            FrameKind.Phone -> addRoundRect(inner, w * 0.09f, w * 0.09f, Path.Direction.CW)
            FrameKind.Camera, FrameKind.Player, FrameKind.Message -> addRoundRect(inner, m * 0.04f, m * 0.04f, Path.Direction.CW)
            FrameKind.Slide -> addRoundRect(inner, m * 0.05f, m * 0.05f, Path.Direction.CW)
            FrameKind.Lace -> addOval(inner, Path.Direction.CW)
            else -> addRect(inner, Path.Direction.CW)
        }
    }

    private fun deckle(w: Float, h: Float): Path {
        val p = Path()
        val m = min(w, h)
        val a = m * 0.012f
        val step = m * 0.025f
        fun edge(x0: Float, y0: Float, x1: Float, y1: Float, first: Boolean) {
            val len = kotlin.math.hypot(x1 - x0, y1 - y0)
            val n = (len / step).toInt().coerceAtLeast(2)
            val nx = -(y1 - y0) / len; val ny = (x1 - x0) / len
            for (i in 0..n) {
                val t = i / n.toFloat()
                val off = (if (i % 2 == 0) a else -a) * (0.6f + 0.4f * sin(i * 1.7f))
                val x = x0 + (x1 - x0) * t + nx * off; val y = y0 + (y1 - y0) * t + ny * off
                if (first && i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
        }
        edge(-w / 2, -h / 2, w / 2, -h / 2, true); edge(w / 2, -h / 2, w / 2, h / 2, false); edge(w / 2, h / 2, -w / 2, h / 2, false); edge(-w / 2, h / 2, -w / 2, -h / 2, false)
        p.close()
        return p
    }

    /** An oval doily: a scalloped ring. */
    private fun lace(w: Float, h: Float): Path {
        val p = Path()
        val n = 36
        for (i in 0..n * 8) {
            val t = i / (n * 8f)
            val a = t * 2 * Math.PI
            val bump = 1f - 0.035f * (1 - cos(t * n * 2 * Math.PI).toFloat()) / 2
            val x = (cos(a) * w / 2 * bump).toFloat(); val y = (sin(a) * h / 2 * bump).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    /** Mount details under the photo (sprockets, the phone's island…). [paint] is scratch. */
    fun drawUnder(c: Canvas, kind: FrameKind, w: Float, h: Float, inner: RectF, mount: Int, paint: Paint) {
        val m = min(w, h)
        val onMount = if (luminance(mount) > 0.55f) 0xFF2B2B2E.toInt() else 0xFFF2F0EA.toInt()
        paint.reset(); paint.isAntiAlias = true
        when (kind) {
            FrameKind.Film -> sprockets(c, w, h, 0.13f * h, 0xFFEDE6D6.toInt(), paint)
            FrameKind.Negative -> {
                sprockets(c, w, h, 0.15f * h, 0xFF2A1A10.toInt(), paint)
                text(c, "UNROLL 400  ▸ 24", -w / 2 + w * 0.06f, -h / 2 + 0.15f * h * 0.92f, 0.15f * h * 0.32f, 0xFFF6D29A.toInt(), paint)
                text(c, "◂ 25A", w / 2 - w * 0.2f, h / 2 - 0.15f * h * 0.08f, 0.15f * h * 0.32f, 0xFFF6D29A.toInt(), paint)
            }
            FrameKind.Polaroid, FrameKind.Instax, FrameKind.Wide, FrameKind.Square -> {
                // Paper sheen and a hairline round the window.
                paint.shader = LinearGradient(-w / 2, -h / 2, w / 2, h / 2, intArrayOf(0x14FFFFFF, 0x0A000000, 0x10FFFFFF), null, Shader.TileMode.CLAMP)
                c.drawRect(-w / 2, -h / 2, w / 2, h / 2, paint); paint.shader = null
                paint.color = 0x22000000; paint.style = Paint.Style.STROKE; paint.strokeWidth = m * 0.004f
                c.drawRect(inner.left - m * 0.003f, inner.top - m * 0.003f, inner.right + m * 0.003f, inner.bottom + m * 0.003f, paint)
            }
            FrameKind.Slide -> {
                paint.color = 0x14000000; c.drawRoundRect(inner.left - m * 0.03f, inner.top - m * 0.03f, inner.right + m * 0.03f, inner.bottom + m * 0.03f, m * 0.06f, m * 0.06f, paint)
                text(c, "UNROLL", -w / 2 + m * 0.06f, h / 2 - m * 0.05f, m * 0.055f, onMount, paint, bold = true)
                text(c, "24 · 36", w / 2 - m * 0.22f, h / 2 - m * 0.05f, m * 0.05f, onMount, paint)
            }
            FrameKind.Notebook -> {
                paint.color = 0x447BA7D9; paint.strokeWidth = h * 0.003f
                var y = -h / 2 + h * 0.06f
                while (y < h / 2) { c.drawLine(-w / 2, y, w / 2, y, paint); y += h * 0.045f }
                paint.color = 0x88E2675F.toInt(); c.drawLine(-w / 2 + w * 0.1f, -h / 2, -w / 2 + w * 0.1f, h / 2, paint)
                paint.color = 0xFF3C3C40.toInt()
                var sy = -h / 2 + h * 0.04f
                while (sy < h / 2 - h * 0.03f) { paint.style = Paint.Style.STROKE; paint.strokeWidth = w * 0.012f; c.drawArc(-w / 2 - w * 0.03f, sy, -w / 2 + w * 0.045f, sy + h * 0.03f, 90f, 270f, false, paint); sy += h * 0.055f }
                paint.style = Paint.Style.FILL
            }
            FrameKind.Phone -> {
                paint.color = 0xFF000000.toInt()
                c.drawRoundRect(inner, w * 0.09f, w * 0.09f, paint)
                // Side buttons.
                paint.color = darker(mount)
                c.drawRoundRect(w / 2 - w * 0.008f, -h * 0.2f, w / 2 + w * 0.012f, -h * 0.08f, w * 0.01f, w * 0.01f, paint)
                c.drawRoundRect(-w / 2 - w * 0.012f, -h * 0.26f, -w / 2 + w * 0.008f, -h * 0.18f, w * 0.01f, w * 0.01f, paint)
            }
            FrameKind.Camera -> {
                paint.color = 0xFF111113.toInt(); c.drawRoundRect(inner.left - m * 0.02f, inner.top - m * 0.02f, inner.right + m * 0.02f, inner.bottom + m * 0.02f, m * 0.05f, m * 0.05f, paint)
                val bx = w / 2 - w * 0.16f
                paint.color = 0xFF46464B.toInt(); c.drawCircle(bx, -h * 0.05f, m * 0.12f, paint)
                paint.color = 0xFF5A5A60.toInt(); c.drawCircle(bx, -h * 0.05f, m * 0.05f, paint)
                for ((i, y) in listOf(-h * 0.32f, h * 0.22f, h * 0.34f).withIndex()) { paint.color = if (i == 0) 0xFFC8312B.toInt() else 0xFF55555B.toInt(); c.drawCircle(bx + (if (i == 0) 0f else (i - 1.5f) * m * 0.12f), y, m * 0.035f, paint) }
                text(c, "MENU", bx - m * 0.12f, h * 0.45f, m * 0.035f, 0xFF9A9AA0.toInt(), paint)
            }
            FrameKind.Browser -> {
                val bar = inner.top - (-h / 2)
                paint.color = lighter(mount, 0.0f)
                for ((i, col) in listOf(0xFFFF5F57.toInt(), 0xFFFEBC2E.toInt(), 0xFF28C840.toInt()).withIndex()) { paint.color = col; c.drawCircle(-w / 2 + bar * (0.45f + i * 0.42f), -h / 2 + bar / 2, bar * 0.13f, paint) }
                paint.color = if (luminance(mount) > 0.55f) 0x14000000 else 0x22FFFFFF
                val ul = -w / 2 + bar * 1.8f; val ur = w / 2 - bar * 0.5f
                c.drawRoundRect(ul, -h / 2 + bar * 0.22f, ur, -h / 2 + bar * 0.78f, bar * 0.28f, bar * 0.28f, paint)
                text(c, "unroll.app/our-trip", ul + bar * 0.35f, -h / 2 + bar * 0.62f, bar * 0.3f, alpha(onMount, 170), paint)
            }
            FrameKind.Player -> {
                val top = inner.bottom
                val left = inner.left
                val ww = inner.width()
                val rest = h / 2 - top
                text(c, "Our trip", left, top + rest * 0.24f, rest * 0.14f, onMount, paint, bold = true)
                text(c, "On repeat · Unroll", left, top + rest * 0.38f, rest * 0.09f, alpha(onMount, 150), paint)
                paint.color = alpha(onMount, 60); c.drawRoundRect(left, top + rest * 0.5f, left + ww, top + rest * 0.53f, 4f, 4f, paint)
                paint.color = onMount; c.drawRoundRect(left, top + rest * 0.5f, left + ww * 0.42f, top + rest * 0.53f, 4f, 4f, paint)
                c.drawCircle(left + ww * 0.42f, top + rest * 0.515f, rest * 0.03f, paint)
                val cy = top + rest * 0.76f
                val s = rest * 0.1f
                // Previous, play, next.
                val tri = Path()
                fun play(cx: Float, size: Float, dir: Float) { tri.reset(); tri.moveTo(cx - size * 0.5f * dir, cy - size * 0.6f); tri.lineTo(cx + size * 0.6f * dir, cy); tri.lineTo(cx - size * 0.5f * dir, cy + size * 0.6f); tri.close(); c.drawPath(tri, paint) }
                c.drawCircle(0f + inner.centerX(), cy, s * 1.6f, paint)
                paint.color = mount; play(inner.centerX() + s * 0.12f, s * 1.1f, 1f)
                paint.color = onMount; play(inner.centerX() - ww * 0.28f, s, -1f); play(inner.centerX() - ww * 0.28f - s * 0.8f, s, -1f)
                play(inner.centerX() + ww * 0.28f, s, 1f); play(inner.centerX() + ww * 0.28f + s * 0.8f, s, 1f)
            }
            FrameKind.Message -> Unit
            FrameKind.Popup -> {
                val head = inner.top - (-h / 2)
                text(c, "Unroll would like to share", -w / 2 + w * 0.06f, -h / 2 + head * 0.45f, head * 0.2f, onMount, paint, bold = true)
                text(c, "1 photo · a day you'll want to keep", -w / 2 + w * 0.06f, -h / 2 + head * 0.78f, head * 0.15f, alpha(onMount, 150), paint)
                val foot = h / 2 - inner.bottom
                val by = inner.bottom + foot * 0.2f
                paint.color = alpha(onMount, 30); c.drawLine(-w / 2, by, w / 2, by, paint.apply { strokeWidth = h * 0.003f })
                c.drawLine(0f, by, 0f, h / 2, paint)
                paint.color = alpha(onMount, 170); textCentred(c, "Decline", -w / 4, by + (h / 2 - by) / 2, foot * 0.24f, paint)
                paint.color = 0xFF3478F6.toInt(); textCentred(c, "Accept", w / 4, by + (h / 2 - by) / 2, foot * 0.24f, paint, bold = true)
            }
            FrameKind.Postcard -> Unit
            FrameKind.Ticket -> {
                val stubX = w / 2 - 0.3f * w
                paint.color = alpha(onMount, 90); paint.strokeWidth = h * 0.012f; paint.pathEffect = DashPathEffect(floatArrayOf(h * 0.03f, h * 0.03f), 0f)
                c.drawLine(stubX, -h / 2 + h * 0.1f, stubX, h / 2 - h * 0.1f, paint); paint.pathEffect = null
                c.save(); c.translate(stubX + 0.15f * w, 0f); c.rotate(90f)
                paint.color = onMount; textCentred(c, "ADMIT ONE", 0f, 0f, 0.07f * w, paint, bold = true)
                c.restore()
            }
            FrameKind.Window -> Unit
            else -> Unit
        }
        paint.reset(); paint.isAntiAlias = true
    }

    /** Details over the photo (lace, a scribbled outline, tape, glare…). */
    fun drawOver(c: Canvas, kind: FrameKind, w: Float, h: Float, inner: RectF, mount: Int, paint: Paint, accent: Int) {
        val m = min(w, h)
        paint.reset(); paint.isAntiAlias = true
        when (kind) {
            FrameKind.Phone -> {
                paint.color = 0xFF000000.toInt()
                val iw = inner.width() * 0.3f; val ih = inner.width() * 0.085f
                c.drawRoundRect(-iw / 2, inner.top + ih * 0.5f, iw / 2, inner.top + ih * 1.5f, ih / 2, ih / 2, paint)
                paint.color = 0xCCFFFFFF.toInt()
                c.drawRoundRect(-inner.width() * 0.18f, inner.bottom - inner.width() * 0.04f, inner.width() * 0.18f, inner.bottom - inner.width() * 0.028f, 6f, 6f, paint)
                text(c, "9:41", inner.left + inner.width() * 0.09f, inner.top + ih * 1.25f, ih * 0.62f, 0xFFFFFFFF.toInt(), paint, bold = true)
            }
            FrameKind.Camera -> {
                text(c, "▶  100-0024", inner.left + inner.width() * 0.04f, inner.top + inner.height() * 0.1f, inner.height() * 0.06f, 0xEEFFFFFF.toInt(), paint)
                text(c, "F2.8  1/250  ISO 200", inner.left + inner.width() * 0.04f, inner.bottom - inner.height() * 0.05f, inner.height() * 0.06f, 0xEEFFFFFF.toInt(), paint)
            }
            FrameKind.Corners -> {
                paint.color = 0xFF1E1E20.toInt()
                val s = m * 0.16f
                for ((x, y) in listOf(inner.left to inner.top, inner.right to inner.top, inner.right to inner.bottom, inner.left to inner.bottom)) {
                    val sx = if (x == inner.left) 1f else -1f; val sy = if (y == inner.top) 1f else -1f
                    val p = Path().apply { moveTo(x - sx * m * 0.012f, y - sy * m * 0.012f); lineTo(x + sx * s, y - sy * m * 0.012f); lineTo(x - sx * m * 0.012f, y + sy * s); close() }
                    paint.setShadowLayer(m * 0.01f, 0f, m * 0.005f, 0x55000000); c.drawPath(p, paint); paint.clearShadowLayer()
                }
            }
            FrameKind.Taped -> {
                paint.color = 0xCCE9DFC6.toInt()
                for ((x, y, r) in listOf(Triple(-w / 2 + w * 0.08f, -h / 2 + h * 0.02f, -38f), Triple(w / 2 - w * 0.08f, -h / 2 + h * 0.02f, 38f))) {
                    c.save(); c.translate(x, y); c.rotate(r); c.drawPath(Shapes.tape(m * 0.34f, m * 0.1f), paint); c.restore()
                }
            }
            FrameKind.Lace -> {
                paint.style = Paint.Style.STROKE; paint.color = alpha(darker(mount), 160); paint.strokeWidth = m * 0.008f
                // Holes and a stitched ring through the lace.
                val ring = RectF(inner); ring.inset(-m * 0.045f, -m * 0.045f)
                paint.pathEffect = DashPathEffect(floatArrayOf(m * 0.015f, m * 0.012f), 0f)
                c.drawOval(ring, paint)
                paint.pathEffect = null; paint.style = Paint.Style.FILL; paint.color = alpha(darker(mount), 110)
                for (i in 0 until 36) {
                    val a = Math.toRadians(i * 10.0)
                    c.drawCircle((cos(a) * w * 0.44f).toFloat(), (sin(a) * h * 0.44f).toFloat(), m * 0.011f, paint)
                }
                paint.style = Paint.Style.STROKE; paint.strokeWidth = m * 0.006f; paint.color = 0x33000000
                c.drawOval(inner, paint)
            }
            FrameKind.Scribble -> {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = m * 0.016f; paint.color = accent
                paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
                paint.pathEffect = DiscretePathEffect(m * 0.03f, m * 0.006f)
                val r1 = RectF(-w / 2 - m * 0.025f, -h / 2 - m * 0.02f, w / 2 + m * 0.02f, h / 2 + m * 0.03f)
                c.save(); c.rotate(-1.2f); c.drawRect(r1, paint); c.restore()
                c.save(); c.rotate(0.8f); r1.inset(m * 0.012f, -m * 0.01f); c.drawRect(r1, paint); c.restore()
                paint.pathEffect = null
            }
            FrameKind.Window -> {
                paint.color = mount; paint.style = Paint.Style.FILL
                val bar = m * 0.035f
                c.drawRect(inner.centerX() - bar / 2, inner.top, inner.centerX() + bar / 2, inner.bottom, paint)
                c.drawRect(inner.left, inner.centerY() - bar / 2, inner.right, inner.centerY() + bar / 2, paint)
                paint.shader = LinearGradient(inner.left, inner.top, inner.right, inner.bottom, intArrayOf(0x33FFFFFF, 0x00FFFFFF, 0x1AFFFFFF), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                c.drawRect(inner, paint); paint.shader = null
                paint.style = Paint.Style.STROKE; paint.strokeWidth = m * 0.006f; paint.color = 0x33000000
                c.drawRect(inner, paint)
            }
            FrameKind.Postcard -> {
                // A little stamp pinned to the corner.
                val s = m * 0.2f
                c.save(); c.translate(inner.right - s * 0.6f, inner.top + s * 0.7f); c.rotate(6f)
                paint.color = 0xFFFFFCF4.toInt(); paint.setShadowLayer(m * 0.01f, 0f, m * 0.006f, 0x44000000)
                c.drawPath(Shapes.scalloped(s * 0.82f, s, s * 0.04f), paint); paint.clearShadowLayer()
                paint.color = accent; c.drawRect(-s * 0.31f, -s * 0.4f, s * 0.31f, s * 0.4f, paint)
                paint.color = 0xFFFFFFFF.toInt(); c.drawCircle(0f, -s * 0.05f, s * 0.14f, paint)
                c.restore()
            }
            FrameKind.Slide, FrameKind.Negative, FrameKind.Film -> Unit
            FrameKind.Polaroid, FrameKind.Instax, FrameKind.Wide, FrameKind.Square -> {
                // Faint glare on the print.
                paint.shader = LinearGradient(inner.left, inner.top, inner.right, inner.bottom, intArrayOf(0x22FFFFFF, 0x00FFFFFF, 0x00FFFFFF), floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
                c.drawRect(inner, paint); paint.shader = null
            }
            else -> Unit
        }
        paint.reset(); paint.isAntiAlias = true
    }

    private fun sprockets(c: Canvas, w: Float, h: Float, band: Float, col: Int, paint: Paint) {
        paint.color = col
        val hole = band * 0.38f
        val step = hole * 2.1f
        var x = -w / 2 + step / 2
        while (x + hole < w / 2) {
            for (y in listOf(-h / 2 + band / 2, h / 2 - band / 2)) c.drawRoundRect(x, y - hole / 2, x + hole * 0.8f, y + hole / 2, hole * 0.2f, hole * 0.2f, paint)
            x += step
        }
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, col: Int, paint: Paint, bold: Boolean = false) {
        paint.color = col; paint.textSize = size; paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT; paint.style = Paint.Style.FILL
        c.drawText(s, x, y, paint)
    }

    private fun textCentred(c: Canvas, s: String, x: Float, y: Float, size: Float, paint: Paint, bold: Boolean = false) {
        paint.textSize = size; paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT; paint.style = Paint.Style.FILL
        c.drawText(s, x - paint.measureText(s) / 2, y + size * 0.35f, paint)
    }

    private fun luminance(c: Int) = (0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)) / 255f
    private fun alpha(c: Int, a: Int) = (c and 0x00FFFFFF) or (a shl 24)
    private fun darker(c: Int): Int = scale(c) { it * 0.72f }
    private fun lighter(c: Int, t: Float): Int = scale(c) { it + (255 - it) * t }
    private inline fun scale(c: Int, f: (Float) -> Float): Int {
        val r = f(((c shr 16) and 0xFF).toFloat()).toInt().coerceIn(0, 255)
        val g = f(((c shr 8) and 0xFF).toFloat()).toInt().coerceIn(0, 255)
        val b = f((c and 0xFF).toFloat()).toInt().coerceIn(0, 255)
        return (c and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
    }
}
