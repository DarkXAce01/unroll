package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.model.Anim
import io.github.darkxace01.unroll.model.AnimKind
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.Look
import io.github.darkxace01.unroll.model.Motion
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Point
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.RouteItem
import io.github.darkxace01.unroll.model.ScrollPlan
import io.github.darkxace01.unroll.model.TextItem
import io.github.darkxace01.unroll.model.Timeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test fun lookStartsHiddenAndSettles() {
        val a = Anim(AnimKind.Rise, 0.5f, 1f)
        val before = Motion.look(a, 0f)
        assertEquals(0f, before.alpha, 0.001f)
        assertTrue(before.dy > 0f)
        assertEquals(Look.Still.copy(), Motion.look(a, 2f))
        assertEquals(Look.Still, Motion.look(null, 0f))
    }

    @Test fun popOvershootsThenSettles() {
        val a = Anim(AnimKind.Pop, 0f, 1f)
        val peak = (1..99).maxOf { Motion.look(a, it / 100f).scale }
        assertTrue(peak > 1f)
        assertEquals(1f, Motion.look(a, 1f).scale, 0.001f)
    }

    @Test fun scrollPlanVisitsEverySlide() {
        val plan = ScrollPlan(4, hold = 1f, move = 1f, tail = 0f)
        assertEquals(7f, plan.duration, 0.001f)
        assertEquals(0f, plan.position(0.5f))
        assertEquals(1f, plan.position(2.5f))
        assertEquals(3f, plan.position(100f))
        var last = 0f
        for (i in 0..700) { val x = plan.position(i / 100f); assertTrue(x >= last - 1e-4f); last = x }
    }

    @Test fun cueComesBeforeTheCameraArrives() {
        val plan = ScrollPlan(5)
        val cue = plan.cue(2f)
        // By the time the camera holds on slide 3 (index 2), the item has been cued.
        val arrive = 2 * (plan.hold + plan.move)
        assertTrue(cue <= arrive)
        assertTrue(cue > plan.cue(1f))
        assertEquals(0f, plan.cue(0f))
    }

    private val route = RouteItem("r", listOf(Point(100f, 500f), Point(1600f, 600f), Point(3000f, 500f)), anim = Anim(AnimKind.Draw))
    private val p = Project("p", "p", slides = 3, items = listOf(
        route,
        PhotoItem("a", Frame(1620f, 675f, 400f, 400f), "u", 10, 10, anim = Anim(AnimKind.Fade)),
        TextItem("still", Frame(540f, 200f, 300f, 100f), "Hi"),
    ))

    @Test fun routeDrawsAheadOfTheCamera() {
        val plan = ScrollPlan(3)
        val early = Timeline.scrollLook(p, plan, 0f)(route).reveal
        val mid = Timeline.scrollLook(p, plan, plan.duration / 2)(route).reveal
        val end = Timeline.scrollLook(p, plan, plan.duration)(route).reveal
        assertTrue(early < mid && mid < end)
        assertEquals(1f, end, 0.001f)
    }

    @Test fun eachSlideContinuesTheRoute() {
        val len = 3f
        val s0end = Timeline.slideLook(p, 0, len, len)(route).reveal
        val s1start = Timeline.slideLook(p, 1, 0f, len)(route).reveal
        assertEquals(s0end, s1start, 0.01f)
    }

    @Test fun animatedSlidesFindsWhatMoves() {
        assertEquals(setOf(0, 1, 2), Timeline.animatedSlides(p))
        assertEquals(emptySet<Int>(), Timeline.animatedSlides(p.copy(items = p.items.filterIsInstance<TextItem>())))
    }

    @Test fun autoAnimateKeepsChosenOnes() {
        val q = Timeline.autoAnimate(p)
        assertEquals(AnimKind.Draw, q.item("r")!!.anim!!.kind)
        assertEquals(AnimKind.Fade, q.item("a")!!.anim!!.kind)
        assertEquals(AnimKind.Rise, q.item("still")!!.anim!!.kind)
    }
}
