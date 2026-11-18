package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.model.Arrange
import io.github.darkxace01.unroll.model.Arranger
import io.github.darkxace01.unroll.model.Grid
import io.github.darkxace01.unroll.model.LayoutPhoto
import io.github.darkxace01.unroll.model.Layouts
import io.github.darkxace01.unroll.model.MAX_SLIDES
import io.github.darkxace01.unroll.model.NewPhoto
import io.github.darkxace01.unroll.model.NormRect
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Ratio
import io.github.darkxace01.unroll.model.TextItem
import io.github.darkxace01.unroll.model.Frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTest {
    private val ratio = Ratio.Portrait

    @Test fun gridCellsStayInsideTheSlideWithGaps() {
        val p = Project("p", "p", slides = 3)
        val frames = Layouts.grid(p, 1, Grid.Four, margin = 40f, gap = 20f)
        assertEquals(4, frames.size)
        for (f in frames) {
            assertTrue(f.left >= 1080f + 40f - 0.01f)
            assertTrue(f.right <= 2160f - 40f + 0.01f)
            assertTrue(f.top >= 39.99f && f.bottom <= 1350f - 39.99f)
        }
        // Gap between the two top cells.
        assertEquals(20f, frames[1].left - frames[0].right, 0.01f)
    }

    @Test fun panoramaSlidesFromShape() {
        // 4:5 slide = 0.8 wide per unit height; a 3.2:1 panorama fills 4 slides.
        assertEquals(4, Layouts.panoramaSlides(3.2f, ratio))
        assertEquals(1, Layouts.panoramaSlides(0.5f, ratio))
        assertEquals(MAX_SLIDES, Layouts.panoramaSlides(100f, ratio))
    }

    private fun photos(vararg aspects: Float) = aspects.mapIndexed { i, a -> LayoutPhoto("$i", a) }

    @Test fun autoFillsWholeSlidesExactly() {
        val r = Layouts.auto(photos(1.5f, 1.5f, 0.75f, 1.33f, 0.66f, 1.5f, 1.5f, 1f), ratio, margin = 48f, gap = 24f)
        val right = r.frames.values.maxOf { it.right }
        val left = r.frames.values.minOf { it.left }
        assertEquals(48f, left, 0.5f)
        assertEquals(r.slides * 1080f - 48f, right, 0.5f)
        assertEquals(8, r.frames.size)
        for (f in r.frames.values) assertTrue(f.top >= 47.9f && f.bottom <= 1350f - 47.9f)
    }

    @Test fun autoNeverOverlaps() {
        val r = Layouts.auto(photos(1.5f, 1.5f, 0.75f, 1.33f, 0.66f, 1.5f, 1.5f, 1f, 1.77f, 0.56f), ratio, seed = 3)
        val boxes = r.frames.values.map { it.bounds() }
        for (i in boxes.indices) for (j in i + 1 until boxes.size) assertTrue("$i overlaps $j", !boxes[i].overlaps(boxes[j]))
    }

    @Test fun autoIsDeterministicPerSeed() {
        val ph = photos(1.5f, 1.5f, 1.5f, 1.5f, 0.8f, 1.5f)
        assertEquals(Layouts.auto(ph, ratio, seed = 2), Layouts.auto(ph, ratio, seed = 2))
    }

    @Test fun autoAvoidsFacesOnCutsWhenItCan() {
        val face = listOf(NormRect(0.4f, 0.2f, 0.6f, 0.5f))
        val ph = (0 until 9).map { LayoutPhoto("$it", if (it % 3 == 0) 0.75f else 1.5f, face) }
        val best = Layouts.auto(ph, ratio)
        val single = Layouts.auto(ph.map { it.copy(faces = emptyList()) }, ratio)
        assertTrue(Layouts.faceHits(best, ph, ratio) <= Layouts.faceHits(single, ph, ratio))
    }

    private fun newPhotos(n: Int) = (0 until n).map { NewPhoto("u$it", 4000, 3000) }

    @Test fun arrangeKeepsTextAndReplacesPhotos() {
        val base = Project("p", "p", slides = 3, items = listOf(
            PhotoItem("old", Frame(10f, 10f, 10f, 10f), "x", 1, 1),
            TextItem("t", Frame(540f, 100f, 300f, 80f), "Title"),
        ))
        val out = Arranger.arrange(base, newPhotos(5), Arrange.Auto, 48f, 24f) { "n$it" }
        assertTrue(out.item("old") == null)
        assertTrue(out.item("t") != null)
        assertEquals(5, out.items.filterIsInstance<PhotoItem>().size)
        // Text stays on top.
        assertEquals("t", out.items.last().id)
    }

    @Test fun onePerSlide() {
        val out = Arranger.arrange(Project("p", "p"), newPhotos(4), Arrange.OnePerSlide, 0f, 0f) { "n$it" }
        assertEquals(4, out.slides)
        out.items.forEachIndexed { i, it -> assertEquals(i, out.slideAt(it.frame.cx)) }
    }

    @Test fun panoramaCoversCanvas() {
        val out = Arranger.arrange(Project("p", "p"), listOf(NewPhoto("pano", 12000, 3000)), Arrange.Panorama, 0f, 0f) { "n$it" }
        assertEquals(5, out.slides)
        val f = out.items.single().frame
        assertEquals(out.width.toFloat(), f.w)
        assertEquals(out.height.toFloat(), f.h)
    }

    @Test fun appendAddsSlidesUpToTheLimit() {
        val p = Project("p", "p", slides = 18)
        val out = Arranger.append(p, newPhotos(5), 48f) { "n$it" }
        assertEquals(MAX_SLIDES, out.slides)
        assertEquals(2, out.items.size)
    }

    @Test fun gridSlideUsesPhotosOnThatSlide() {
        var p = Project("p", "p", slides = 2)
        p = Arranger.place(p, 0, NewPhoto("a", 100, 100), "a")
        p = Arranger.place(p, 0, NewPhoto("b", 100, 100), "b")
        p = Arranger.place(p, 1, NewPhoto("c", 100, 100), "c")
        val out = Arranger.gridSlide(p, 0, Grid.TwoCols, 40f, 20f)
        val a = out.item("a")!!.frame; val b = out.item("b")!!.frame
        assertEquals(1350f - 80f, a.h, 0.01f)
        assertTrue(a.right <= b.left)
        assertEquals(p.item("c"), out.item("c"))
    }
}
