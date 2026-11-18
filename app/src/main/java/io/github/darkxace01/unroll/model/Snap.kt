package io.github.darkxace01.unroll.model

import kotlin.math.abs

/**
 * Snapping while dragging: an item's edges and centre pull onto cuts, slide centres, the canvas's middle
 * and margins, and other items' edges. Returns the adjusted frame and the guide lines to draw.
 */
object Snap {
    data class Guide(val vertical: Boolean, val at: Float)
    data class Result(val frame: Frame, val guides: List<Guide>)

    fun snap(p: Project, moving: Frame, ignoreId: String?, threshold: Float, margin: Float = 48f): Result {
        val b = moving.bounds()
        val xs = buildList {
            addAll(Seams.cuts(p))
            add(0f); add(p.width.toFloat())
            for (s in 0 until p.slides) {
                add(s * p.slideW + p.slideW / 2f)
                add(s * p.slideW + margin); add((s + 1) * p.slideW - margin)
            }
            p.items.filter { it.id != ignoreId }.forEach { val o = it.frame.bounds(); add(o.left); add(o.right); add(o.cx) }
        }
        val ys = buildList {
            add(0f); add(p.height.toFloat()); add(p.height / 2f); add(margin); add(p.height - margin)
            p.items.filter { it.id != ignoreId }.forEach { val o = it.frame.bounds(); add(o.top); add(o.bottom); add(o.cy) }
        }
        val (dx, gx) = best(listOf(b.left, b.cx, b.right), xs, threshold)
        val (dy, gy) = best(listOf(b.top, b.cy, b.bottom), ys, threshold)
        val guides = listOfNotNull(gx?.let { Guide(true, it) }, gy?.let { Guide(false, it) })
        return Result(moving.moved(dx, dy), guides)
    }

    /** The smallest pull from any of [edges] onto any of [targets], within [threshold]. */
    private fun best(edges: List<Float>, targets: List<Float>, threshold: Float): Pair<Float, Float?> {
        var bestD = Float.MAX_VALUE
        var bestT: Float? = null
        for (e in edges) for (t in targets) {
            val d = t - e
            if (abs(d) <= threshold && abs(d) < abs(bestD)) { bestD = d; bestT = t }
        }
        return if (bestT == null) 0f to null else bestD to bestT
    }

    /** Rotation snaps to 0/90/180/270 within a few degrees. */
    fun rotation(deg: Float, threshold: Float = 4f): Float {
        val norm = ((deg % 360f) + 360f) % 360f
        for (t in listOf(0f, 90f, 180f, 270f, 360f)) if (abs(norm - t) <= threshold) return if (t == 360f) 0f else t
        return norm
    }
}
