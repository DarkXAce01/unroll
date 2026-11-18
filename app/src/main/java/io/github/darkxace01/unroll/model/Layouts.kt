package io.github.darkxace01.unroll.model

import kotlin.math.roundToInt
import kotlin.random.Random

/** A photo to lay out: its id in the result, and its shape. Faces are optional, for seam scoring. */
data class LayoutPhoto(val key: String, val aspect: Float, val faces: List<NormRect> = emptyList())

/** Built-in grids for one slide. Cells are in 0..1 of the slide's inner area (inside the margins). */
enum class Grid(val label: String, val cells: List<NormRect>) {
    One("1", listOf(NormRect(0f, 0f, 1f, 1f))),
    TwoCols("2 across", listOf(NormRect(0f, 0f, .5f, 1f), NormRect(.5f, 0f, 1f, 1f))),
    TwoRows("2 down", listOf(NormRect(0f, 0f, 1f, .5f), NormRect(0f, .5f, 1f, 1f))),
    BigLeft("1 + 2", listOf(NormRect(0f, 0f, .6f, 1f), NormRect(.6f, 0f, 1f, .5f), NormRect(.6f, .5f, 1f, 1f))),
    BigTop("1 over 2", listOf(NormRect(0f, 0f, 1f, .6f), NormRect(0f, .6f, .5f, 1f), NormRect(.5f, .6f, 1f, 1f))),
    Three("3 down", listOf(NormRect(0f, 0f, 1f, 1f / 3), NormRect(0f, 1f / 3, 1f, 2f / 3), NormRect(0f, 2f / 3, 1f, 1f))),
    Four("4", listOf(NormRect(0f, 0f, .5f, .5f), NormRect(.5f, 0f, 1f, .5f), NormRect(0f, .5f, .5f, 1f), NormRect(.5f, .5f, 1f, 1f))),
    Six("6", (0 until 6).map { i -> val c = i % 2; val r = i / 2; NormRect(c * .5f, r / 3f, (c + 1) * .5f, (r + 1) / 3f) }),
    Framed("Framed", listOf(NormRect(.06f, .06f, .94f, .8f))),
}

object Layouts {

    /** Frames for [grid] on slide [slide], with [margin] round the edge and [gap] between cells. */
    fun grid(p: Project, slide: Int, grid: Grid, margin: Float, gap: Float): List<Frame> {
        val x0 = slide * p.slideW + margin
        val y0 = margin
        val iw = p.slideW - 2 * margin
        val ih = p.slideH - 2 * margin
        return grid.cells.map { c ->
            // Half a gap off each inner edge; outer edges stay on the margin.
            val l = x0 + c.left * iw + if (c.left > 0f) gap / 2 else 0f
            val r = x0 + c.right * iw - if (c.right < 1f) gap / 2 else 0f
            val t = y0 + c.top * ih + if (c.top > 0f) gap / 2 else 0f
            val b = y0 + c.bottom * ih - if (c.bottom < 1f) gap / 2 else 0f
            Frame((l + r) / 2, (t + b) / 2, r - l, b - t)
        }
    }

    /** How many slides a panorama of this shape needs to keep its full height. */
    fun panoramaSlides(aspect: Float, ratio: Ratio): Int =
        (aspect * ratio.slideH / ratio.slideW).roundToInt().coerceIn(1, MAX_SLIDES)

    /** One photo across the whole canvas. */
    fun panorama(p: Project): Frame = Frame(p.width / 2f, p.height / 2f, p.width.toFloat(), p.height.toFloat())

    /** One photo per slide, each filling its slide inside the margins. */
    fun onePerSlide(count: Int, ratio: Ratio, margin: Float): List<Frame> = (0 until count).map { i ->
        Frame(i * ratio.slideW + ratio.slideW / 2f, ratio.slideH / 2f, ratio.slideW - 2 * margin, ratio.slideH - 2 * margin)
    }

    /** One column of the strip: one photo full height, or two stacked. */
    private data class Column(val photos: List<LayoutPhoto>)

    data class AutoResult(val slides: Int, val frames: Map<String, Frame>)

    /**
     * The seamless strip: photos in columns (a portrait alone, landscapes often stacked in pairs) running
     * across all the slides, so pictures flow over the cuts. Column widths come from the photos' shapes,
     * then the strip is stretched a little to end exactly on a slide edge (cover-cropping absorbs it).
     *
     * [seed] gives a different arrangement each shuffle. With faces known, several arrangements are tried
     * and the one with the fewest faces on cuts wins.
     */
    fun auto(photos: List<LayoutPhoto>, ratio: Ratio, margin: Float = 48f, gap: Float = 24f, seed: Int = 0, tries: Int = 24): AutoResult {
        if (photos.isEmpty()) return AutoResult(1, emptyMap())
        val anyFaces = photos.any { it.faces.isNotEmpty() }
        val attempts = if (anyFaces) tries else 1
        var best: AutoResult? = null
        var bestScore = Int.MAX_VALUE
        for (a in 0 until attempts) {
            val r = arrange(photos, ratio, margin, gap, Random(seed * 7919 + a))
            if (!anyFaces) return r
            val score = faceHits(r, photos, ratio)
            if (score < bestScore) { best = r; bestScore = score }
            if (score == 0) break
        }
        return best!!
    }

    private fun arrange(photos: List<LayoutPhoto>, ratio: Ratio, margin: Float, gap: Float, rnd: Random): AutoResult {
        val innerH = ratio.slideH - 2 * margin
        // Group into columns. Two landscapes in a row stack (most of the time); portraits stand alone.
        val columns = mutableListOf<Column>()
        var i = 0
        while (i < photos.size) {
            val ph = photos[i]
            val next = photos.getOrNull(i + 1)
            val stackable = ph.aspect >= 1.05f && next != null && next.aspect >= 1.05f
            if (stackable && rnd.nextFloat() < 0.75f) {
                columns += Column(listOf(ph, next)); i += 2
            } else {
                columns += Column(listOf(ph)); i += 1
            }
        }
        // Natural width of each column at full inner height.
        val widths = columns.map { c ->
            if (c.photos.size == 1) c.photos[0].aspect * innerH
            else (innerH - gap) / c.photos.sumOf { 1.0 / it.aspect }.toFloat()
        }
        val natural = widths.sum() + gap * (columns.size - 1) + 2 * margin
        val slides = (natural / ratio.slideW).roundToInt().coerceIn(1, MAX_SLIDES)
        val available = slides * ratio.slideW - 2 * margin - gap * (columns.size - 1)
        val k = available / widths.sum()
        val frames = mutableMapOf<String, Frame>()
        var x = margin
        columns.forEachIndexed { ci, c ->
            val w = widths[ci] * k
            if (c.photos.size == 1) {
                frames[c.photos[0].key] = Frame(x + w / 2, ratio.slideH / 2f, w, innerH)
            } else {
                // Heights by shape, so each keeps close to its own aspect.
                val inv = c.photos.map { 1f / it.aspect }
                val total = inv.sum()
                var y = margin
                c.photos.forEachIndexed { pi, ph ->
                    val h = (innerH - gap) * inv[pi] / total
                    frames[ph.key] = Frame(x + w / 2, y + h / 2, w, h)
                    y += h + gap
                }
            }
            x += w + gap
        }
        return AutoResult(slides, frames)
    }

    /** How many faces an arrangement puts on a cut. */
    fun faceHits(r: AutoResult, photos: List<LayoutPhoto>, ratio: Ratio): Int {
        val p = Project(id = "", name = "", ratio = ratio, slides = r.slides)
        var hits = 0
        for (ph in photos) {
            val f = r.frames[ph.key] ?: continue
            val item = PhotoItem(ph.key, f, "", (ph.aspect * 1000).roundToInt().coerceAtLeast(1), 1000, faces = ph.faces)
            for (face in ph.faces) {
                val box = PhotoGeometry.regionOnCanvas(item, face) ?: continue
                hits += Seams.crossed(p, box, 12f).size
            }
        }
        return hits
    }
}
