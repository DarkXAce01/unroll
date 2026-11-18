package io.github.darkxace01.unroll.model

/**
 * When everything moves in a video. Two kinds of video:
 *
 * - **Scroll**: the camera glides across the canvas slide by slide (Reels, Stories, TikTok). Each item
 *   arrives when the camera reaches it; routes draw themselves just ahead of the camera, so the line
 *   leads you from one slide to the next.
 * - **Animated slide**: one short clip per slide for a carousel. Items on that slide arrive from the
 *   start; a route draws the stretch that crosses this slide, so swiping continues the line.
 */
object Timeline {

    private val polys = HashMap<RouteItem, List<Point>>()
    private fun poly(r: RouteItem) = polys.getOrPut(r) { if (polys.size > 64) polys.clear(); RouteMath.sample(r) }

    fun scrollLook(p: Project, plan: ScrollPlan, t: Float): (Item) -> Look {
        val pos = plan.position(t)
        val w = p.slideW.toFloat()
        return { item ->
            val a = item.anim
            when {
                a == null || a.kind == AnimKind.None -> Look.Still
                item is RouteItem && a.kind == AnimKind.Draw -> {
                    // The line's tip runs a little ahead of the middle of the view.
                    val tip = RouteMath.fractionAtX(poly(item), (pos + 0.8f) * w)
                    Look(reveal = if (pos >= p.slides - 1f) 1f else tip)
                }
                else -> {
                    val left = item.frame.bounds().left / w
                    Motion.look(a, t - plan.cue(left), unit = p.slideH * 0.06f)
                }
            }
        }
    }

    /** Items arrive on the slide they start on; ones that began on an earlier slide are already there. */
    fun slideLook(p: Project, slide: Int, t: Float, length: Float): (Item) -> Look {
        val w = p.slideW.toFloat()
        val l = slide * w
        val r = l + w
        return { item ->
            val a = item.anim
            when {
                a == null || a.kind == AnimKind.None -> Look.Still
                item is RouteItem && a.kind == AnimKind.Draw -> {
                    val poly = poly(item)
                    val from = RouteMath.fractionAtX(poly, l)
                    val to = RouteMath.fractionAtX(poly, r)
                    val k = Motion.easeInOut((t / (length * 0.8f)).coerceIn(0f, 1f))
                    Look(reveal = from + (to - from) * k)
                }
                item.frame.bounds().left >= l - 1f -> Motion.look(a, t, unit = p.slideH * 0.06f)
                else -> Look.Still
            }
        }
    }

    /** Slides with something that moves, and so are worth a clip rather than a photo. */
    fun animatedSlides(p: Project): Set<Int> {
        val out = mutableSetOf<Int>()
        for (item in p.items) {
            val a = item.anim ?: continue
            if (a.kind == AnimKind.None) continue
            val b = item.frame.bounds()
            if (item is RouteItem && a.kind == AnimKind.Draw) {
                for (s in p.slideAt(b.left)..p.slideAt(b.right - 1f)) out += s
            } else {
                out += p.slideAt(b.left.coerceAtLeast(0f))
            }
        }
        return out
    }

    /** A clip long enough for every animation on its slide, plus a beat to look at the result. */
    fun slideLength(p: Project): Float = (Motion.longest(p) + 1.4f).coerceIn(2.5f, 6f)

    /** Puts a sensible animation on everything that has none: photos fade or pop, text rises, routes draw. */
    fun autoAnimate(p: Project): Project = p.copy(items = p.items.mapIndexed { i, item ->
        val cur = item.anim
        if (cur != null && cur.kind != AnimKind.None) return@mapIndexed item
        val stagger = (i % 3) * 0.15f
        val a = when (item) {
            is PhotoItem -> Anim(if (item.frame.rotation != 0f) AnimKind.Pop else AnimKind.Fade, stagger, 0.6f)
            is TextItem -> Anim(AnimKind.Rise, 0.2f + stagger, 0.6f)
            is ShapeItem, is StickerItem -> Anim(AnimKind.Pop, 0.4f + stagger, 0.5f)
            is RouteItem -> Anim(AnimKind.Draw, 0f, 1.6f)
        }
        item.withAnim(a)
    })
}
