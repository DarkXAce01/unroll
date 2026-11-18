package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.model.Box
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.NormRect
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Seams
import io.github.darkxace01.unroll.model.TextItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeamsTest {
    private val base = Project("p", "p", slides = 3)

    @Test fun cutsBetweenSlides() {
        assertEquals(listOf(1080f, 2160f), Seams.cuts(base))
        assertEquals(listOf(1), Seams.crossed(base, Box(1000f, 0f, 1100f, 10f)))
        assertEquals(listOf(1, 2), Seams.crossed(base, Box(1000f, 0f, 2200f, 10f)))
        assertTrue(Seams.crossed(base, Box(10f, 0f, 1070f, 10f)).isEmpty())
    }

    /** A square photo centred on the first cut, with a face in its middle. */
    private fun faceOnCut(): Project = base.add(
        PhotoItem("a", Frame(1080f, 675f, 800f, 800f), "u", 1000, 1000, faces = listOf(NormRect(0.45f, 0.3f, 0.55f, 0.45f))),
    )

    @Test fun faceOnCutIsWarned() {
        val w = Seams.warnings(faceOnCut())
        assertEquals(1, w.size)
        assertEquals(Seams.Kind.Face, w[0].kind)
        assertEquals(1, w[0].cut)
    }

    @Test fun nudgeClearsTheFaceWithTheSmallestMove() {
        val p = faceOnCut()
        val dx = Seams.nudge(p, "a")
        assertNotNull(dx)
        val fixed = Seams.fix(p, "a")
        assertTrue(Seams.warnings(fixed).isEmpty())
        // The face is 80 wide around the cut: about 40 + margin each way, not half a slide.
        assertTrue(kotlin.math.abs(dx!!) < 100f)
    }

    @Test fun hiddenFacesDontCount() {
        val p = faceOnCut().add(PhotoItem("cover", Frame(1080f, 675f, 400f, 400f), "v", 100, 100))
        assertTrue(Seams.warnings(p).none { it.itemId == "a" })
    }

    @Test fun textOnCutIsWarned() {
        val p = base.add(TextItem("t", Frame(1080f, 200f, 400f, 100f), "Hello"))
        assertEquals(Seams.Kind.Text, Seams.warnings(p).single().kind)
        assertTrue(Seams.warnings(Seams.fix(p, "t")).isEmpty())
    }

    @Test fun noFixWhenTooWide() {
        // Text wider than a slide can't clear every cut.
        val p = base.add(TextItem("t", Frame(1620f, 200f, 2000f, 100f), "Long"))
        assertNull(Seams.nudge(p, "t"))
        assertEquals(p, Seams.fix(p, "t"))
    }
}
