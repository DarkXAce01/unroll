package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.data.ProjectJson
import io.github.darkxace01.unroll.model.Adjust
import io.github.darkxace01.unroll.model.Background
import io.github.darkxace01.unroll.model.BackgroundKind
import io.github.darkxace01.unroll.model.ColorMath
import io.github.darkxace01.unroll.model.Filter
import io.github.darkxace01.unroll.model.FontKey
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.History
import io.github.darkxace01.unroll.model.NormRect
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Ratio
import io.github.darkxace01.unroll.model.ShapeItem
import io.github.darkxace01.unroll.model.ShapeKind
import io.github.darkxace01.unroll.model.Snap
import io.github.darkxace01.unroll.model.Swatch
import io.github.darkxace01.unroll.model.TextItem
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {
    @Test fun identityLeavesColour() {
        assertArrayEquals(ColorMath.identity(), ColorMath.matrix(Adjust()), 1e-5f)
    }

    @Test fun exposureBrightens() {
        val (r, _, _) = ColorMath.apply(ColorMath.matrix(Adjust(exposure = 1f)), 100f, 100f, 100f)
        assertEquals(200f, r, 0.5f)
    }

    @Test fun monoMakesGrey() {
        val (r, g, b) = ColorMath.apply(ColorMath.matrix(Adjust(filter = Filter.Mono)), 200f, 50f, 30f)
        assertEquals(r, g, 0.5f); assertEquals(g, b, 0.5f)
    }

    @Test fun warmthWarms() {
        val (r, _, b) = ColorMath.apply(ColorMath.matrix(Adjust(warmth = 1f)), 128f, 128f, 128f)
        assertTrue(r > 128f && b < 128f)
    }

    @Test fun halfStrengthFilterIsBetween() {
        val full = ColorMath.apply(ColorMath.matrix(Adjust(filter = Filter.Sepia)), 30f, 90f, 200f)
        val half = ColorMath.apply(ColorMath.matrix(Adjust(filter = Filter.Sepia, filterStrength = 0.5f)), 30f, 90f, 200f)
        assertEquals((30f + full.first) / 2, half.first, 0.5f)
    }

    @Test fun concatOrder() {
        // Offset then scale ≠ scale then offset.
        val a = ColorMath.concat(ColorMath.scale(2f, 2f, 2f), ColorMath.offset(10f, 10f, 10f))
        assertEquals(40f, ColorMath.apply(a, 10f, 0f, 0f).first, 0.01f)
    }
}

class HistoryTest {
    private val p0 = Project("p", "a")

    @Test fun undoRedo() {
        val h = History(p0)
        h.commit(p0.copy(name = "b"))
        h.commit(p0.copy(name = "c"))
        assertEquals("b", h.undo()!!.name)
        assertEquals("a", h.undo()!!.name)
        assertNull(h.undo())
        assertEquals("b", h.redo()!!.name)
        h.commit(p0.copy(name = "d"))
        assertFalse(h.canRedo)
    }

    @Test fun sameStateIsNotAStep() {
        val h = History(p0)
        h.commit(p0)
        assertFalse(h.canUndo)
    }

    @Test fun limitDropsOldest() {
        val h = History(p0, limit = 3)
        repeat(10) { h.commit(p0.copy(name = "$it")) }
        var n = 0
        while (h.undo() != null) n++
        assertEquals(3, n)
    }
}

class SnapTest {
    private val p = Project("p", "p", slides = 3)

    @Test fun edgeSnapsToCut() {
        val r = Snap.snap(p, Frame(1080f - 200f + 6f, 400f, 400f, 300f), null, threshold = 10f)
        assertEquals(1080f, r.frame.right, 0.01f)
        assertTrue(r.guides.any { it.vertical && it.at == 1080f })
    }

    @Test fun farAwayDoesNotSnap() {
        val f = Frame(700f, 333f, 100f, 100f)
        assertEquals(f, Snap.snap(p, f, null, threshold = 5f).frame)
    }

    @Test fun rotationSnaps() {
        assertEquals(0f, Snap.rotation(2f))
        assertEquals(90f, Snap.rotation(88f))
        assertEquals(0f, Snap.rotation(-3f))
        assertEquals(45f, Snap.rotation(45f))
    }
}

class JsonTest {
    @Test fun roundTripsEveryKindOfItem() {
        val p = Project(
            "p", "Udaipur", Ratio.Tall, 4,
            Background(BackgroundKind.Gradient, 0xFF112233, 0xFFAABBCC, 45f),
            listOf(
                PhotoItem("a", Frame(1f, 2f, 3f, 4f, 5f), "content://x/1", 4000, 3000, adjust = Adjust(exposure = 0.2f, filter = Filter.Film), faces = listOf(NormRect(0.1f, 0.2f, 0.3f, 0.4f)), takenAt = 123L, lat = 24.5, lng = 73.7),
                TextItem("t", Frame(1f, 2f, 3f, 4f), "Day 1", FontKey.Hand, pillColor = 0xFFFFFFFF),
                ShapeItem("s", Frame(1f, 2f, 3f, 4f), ShapeKind.Heart),
            ),
            created = 1, updated = 2,
        )
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
    }

    @Test fun ignoresUnknownFields() {
        val s = ProjectJson.encode(Project("p", "x")).replaceFirst("{", "{\"future\":1,")
        assertEquals("x", ProjectJson.decode(s).name)
    }
}

class SwatchTest {
    @Test fun picksDistinctDominantColours() {
        val red = 0xFFD03020.toInt(); val blue = 0xFF2040C0.toInt(); val nearRed = 0xFFD43424.toInt()
        val px = IntArray(100) { if (it < 60) red else if (it < 70) nearRed else blue }
        val out = Swatch.pick(px, 4)
        // Light tint + red + blue; near-red merges with red.
        assertEquals(3, out.size)
        assertTrue(out[0] and 0xFF0000 > 0xE00000) // tint is light
    }

    @Test fun emptyIsEmpty() = assertTrue(Swatch.pick(IntArray(0), 4).isEmpty())
}
