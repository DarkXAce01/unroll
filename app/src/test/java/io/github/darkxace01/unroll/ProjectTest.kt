package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.MAX_SLIDES
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Ratio
import io.github.darkxace01.unroll.model.TextItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectTest {
    private fun photo(id: String, cx: Float, w: Float = 400f) = PhotoItem(id, Frame(cx, 675f, w, 600f), "u$id", 4000, 3000)
    private val p = Project("p", "Trip", Ratio.Portrait, slides = 3, items = listOf(photo("a", 540f), photo("b", 1620f), photo("c", 2700f)))

    @Test fun sizesFollowRatio() {
        assertEquals(3240, p.width)
        assertEquals(1350, p.height)
        assertEquals(1440, p.copy(ratio = Ratio.Tall).height)
    }

    @Test fun slideAtClampsToCanvas() {
        assertEquals(0, p.slideAt(-50f))
        assertEquals(1, p.slideAt(1080f))
        assertEquals(2, p.slideAt(99999f))
    }

    @Test fun rotatedFrameHitTest() {
        val f = Frame(100f, 100f, 200f, 20f, rotation = 90f)
        assertTrue(f.contains(100f, 180f)) // Now tall and thin.
        assertFalse(f.contains(180f, 100f))
        val b = f.bounds()
        assertEquals(20f, b.width, 0.01f)
        assertEquals(200f, b.height, 0.01f)
    }

    @Test fun localAndCanvasRoundTrip() {
        val f = Frame(300f, 200f, 120f, 80f, rotation = 33f)
        val l = f.toLocal(320f, 230f)
        val back = f.toCanvas(l.x, l.y)
        assertEquals(320f, back.x, 0.01f)
        assertEquals(230f, back.y, 0.01f)
    }

    @Test fun hitPicksTopmost() {
        val q = p.add(TextItem("t", Frame(540f, 675f, 300f, 100f), "Hi"))
        assertEquals("t", q.hit(540f, 675f)?.id)
        assertEquals("a", q.hit(540f, 900f)?.id)
        assertNull(q.hit(1080f, 10f))
    }

    @Test fun orderChanges() {
        assertEquals("a", p.bringToFront("a").items.last().id)
        assertEquals("c", p.sendToBack("c").items.first().id)
    }

    @Test fun insertSlidePushesLaterItems() {
        val q = p.insertSlide(0)
        assertEquals(4, q.slides)
        assertEquals(540f, q.item("a")!!.frame.cx)
        assertEquals(1620f + 1080f, q.item("b")!!.frame.cx)
        val front = p.insertSlide(-1)
        assertEquals(540f + 1080f, front.item("a")!!.frame.cx)
    }

    @Test fun removeSlideTakesItsItems() {
        val q = p.removeSlide(1)
        assertEquals(2, q.slides)
        assertNull(q.item("b"))
        assertEquals(2700f - 1080f, q.item("c")!!.frame.cx)
        assertEquals(1, Project("x", "x", slides = 1).removeSlide(0).slides)
    }

    @Test fun moveSlideSwapsContent() {
        val q = p.moveSlide(0, 2)
        assertEquals(2700f, q.item("a")!!.frame.cx)
        assertEquals(540f, q.item("b")!!.frame.cx)
        assertEquals(1620f, q.item("c")!!.frame.cx)
    }

    @Test fun neverMoreThanTwentySlides() {
        val full = Project("x", "x", slides = MAX_SLIDES)
        assertEquals(MAX_SLIDES, full.insertSlide(3).slides)
    }

    @Test fun ratioChangeKeepsItemsOnTheirSlide() {
        val q = p.withRatio(Ratio.Square)
        assertEquals(1080, q.height)
        for (id in listOf("a", "b", "c")) assertEquals(p.slideAt(p.item(id)!!.frame.cx), q.slideAt(q.item(id)!!.frame.cx))
        assertEquals(675f * 1080f / 1350f, q.item("a")!!.frame.cy, 0.01f)
    }
}
