package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.model.Arranger
import io.github.darkxace01.unroll.model.Collections
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.Grid
import io.github.darkxace01.unroll.model.Layouts
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.TemplateKits
import io.github.darkxace01.unroll.model.Templates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseTwoCTest {
    @Test fun everyGridStaysOnItsSlide() {
        val p = Project("p", "p", slides = 3)
        for (g in Grid.entries) {
            val frames = Layouts.grid(p, 1, g, 40f, 20f)
            assertEquals(g.name, g.cells.size, frames.size)
            for (f in frames) {
                val b = f.bounds()
                assertTrue("${g.name} $b", b.left >= p.slideW - 0.5f && b.right <= 2 * p.slideW + 0.5f && b.top >= -0.5f && b.bottom <= p.slideH + 0.5f)
            }
        }
    }

    @Test fun photoInPhotoSitsInsideTheBigOne() {
        val p = Project("p", "p", slides = 1)
        val (big, small) = Layouts.grid(p, 0, Grid.Inset, 0f, 20f).map { it.bounds() }
        assertTrue(small.left > big.left && small.right <= big.right && small.top > big.top && small.bottom <= big.bottom)
        // Applied to a slide with two photos: the first fills it, the second goes in the corner.
        val photos = (0 until 2).map { PhotoItem("p$it", Frame(300f + it * 400f, 600f, 300f, 300f), "u$it", 1200, 900) }
        val out = Arranger.gridSlide(p.copy(items = photos), 0, Grid.Inset, 0f, 20f)
        assertEquals(p.slideW.toFloat(), (out.item("p0") as PhotoItem).frame.w, 0.5f)
        assertTrue((out.item("p1") as PhotoItem).frame.w < p.slideW * 0.5f)
    }

    @Test fun newLayoutsBuildForEveryKitThatUsesThem() {
        for (id in listOf("triptych", "rows", "inset", "bento")) {
            val ts = Templates.everything.filter { it.layout == id }
            assertTrue("$id has no templates", ts.size >= 3)
            var n = 0
            for (t in ts) {
                val p = Templates.build(t, emptyList(), io.github.darkxace01.unroll.model.TripInfo(), "p", t.name, 0, { "i${n++}" })
                assertTrue("${t.id} has no photo slots", p.items.count { it is PhotoItem } >= 4)
            }
        }
        assertTrue(TemplateKits.layouts.size >= 16)
    }

    @Test fun collectionsFollowTheSeason() {
        assertEquals("holidays", Collections.forMonth(12).first().id)
        assertTrue(Collections.forMonth(7).any { it.id == "summer" })
        assertEquals("valentine", Collections.forMonth(2).first().id)
        for (m in 1..12) assertEquals(4, Collections.forMonth(m).size)
    }

    @Test fun everyCollectionHasEnoughTemplates() {
        for (c in Collections.all) {
            val ts = Collections.templates(c, Templates.everything, testVectors)
            assertTrue("${c.id}: ${ts.size}", ts.size >= 6)
        }
    }
}
