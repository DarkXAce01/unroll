package io.github.darkxace01.unroll.model

import kotlin.math.abs

/**
 * Where Instagram cuts the canvas, and what it would cut through. A face or a line of text split
 * between two slides looks broken when someone swipes, so the app warns and offers a nudge.
 */
object Seams {

    /** Canvas x of each cut, left to right (slides − 1 of them). */
    fun cuts(p: Project): List<Float> = (1 until p.slides).map { (it * p.slideW).toFloat() }

    /** The slide rectangle on the canvas. */
    fun slide(p: Project, index: Int) = Box((index * p.slideW).toFloat(), 0f, ((index + 1) * p.slideW).toFloat(), p.slideH.toFloat())

    /** Indices (1-based, cut n sits between slide n and n+1) of the cuts a box crosses. */
    fun crossed(p: Project, b: Box, margin: Float = 0f): List<Int> =
        cuts(p).mapIndexedNotNull { i, x -> if (b.left - margin < x && x < b.right + margin) i + 1 else null }

    enum class Kind { Face, Text }

    data class Warning(val itemId: String, val cut: Int, val kind: Kind, val region: Box)

    /**
     * Everything that sits on a cut. Faces count with a small margin (a cut right at an ear still looks
     * wrong); text counts when the cut passes through its box. Faces hidden under a later item don't
     * count, because nobody sees them.
     */
    fun warnings(p: Project, faceMargin: Float = 12f): List<Warning> {
        val out = mutableListOf<Warning>()
        p.items.forEachIndexed { index, item ->
            when (item) {
                is PhotoItem -> item.faces.orEmpty().forEach { face ->
                    val box = PhotoGeometry.regionOnCanvas(item, face) ?: return@forEach
                    if (box.width < 24f) return@forEach
                    val covered = p.items.drop(index + 1).any { it is PhotoItem && it.frame.contains(box.cx, box.cy) }
                    if (covered) return@forEach
                    crossed(p, box, faceMargin).forEach { out += Warning(item.id, it, Kind.Face, box) }
                }
                is TextItem -> {
                    val box = item.frame.bounds()
                    // Text spanning more than a slide and a half runs across the cuts on purpose.
                    if (box.width > p.slideW * 1.5f) return@forEachIndexed
                    crossed(p, box).forEach { out += Warning(item.id, it, Kind.Text, box) }
                }
                is ShapeItem, is RouteItem -> Unit
            }
        }
        return out
    }

    /**
     * The smallest sideways move that takes every face (or the text) of [itemId] off every cut, or null
     * if there's none within [maxShift]. Tries both directions and picks the shorter.
     */
    fun nudge(p: Project, itemId: String, margin: Float = 24f, maxShift: Float = p.slideW * 0.5f): Float? {
        val item = p.item(itemId) ?: return null
        val regions: List<Box> = when (item) {
            is PhotoItem -> item.faces.orEmpty().mapNotNull { PhotoGeometry.regionOnCanvas(item, it) }
            is TextItem -> listOf(item.frame.bounds())
            is ShapeItem, is RouteItem -> emptyList()
        }
        if (regions.isEmpty()) return null
        val cuts = cuts(p)
        fun clear(dx: Float) = regions.all { r -> cuts.none { x -> r.left + dx - margin < x && x < r.right + dx + margin } }
        if (clear(0f)) return 0f
        // Candidate moves: put each region's left or right edge just past each cut.
        val candidates = buildList {
            for (r in regions) for (x in cuts) {
                add(x + margin - r.left)
                add(x - margin - r.right)
            }
        }.filter { abs(it) <= maxShift }.sortedBy { abs(it) }
        return candidates.firstOrNull { clear(it) }
    }

    /** Applies [nudge] to the item; returns the project unchanged if there's no fix. */
    fun fix(p: Project, itemId: String): Project {
        val dx = nudge(p, itemId) ?: return p
        if (dx == 0f) return p
        val item = p.item(itemId) ?: return p
        return p.replace(item.withFrame(item.frame.moved(dx, 0f)))
    }
}
