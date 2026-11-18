package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.model.Box
import io.github.darkxace01.unroll.model.Doodles
import io.github.darkxace01.unroll.model.FrameKind
import io.github.darkxace01.unroll.model.FrameLibrary
import io.github.darkxace01.unroll.model.PhotoGeometry
import io.github.darkxace01.unroll.model.Stickers
import io.github.darkxace01.unroll.model.TripInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The sticker and frame libraries: big, well-formed, and every frame leaves room for its photo. */
class LibraryTest {
    @Test fun stickerLibraryIsBigAndIdsAreUnique() {
        assertTrue("${Stickers.all.size} stickers", Stickers.all.size >= 1500)
        assertEquals(Stickers.all.size, Stickers.all.map { it.id }.toSet().size)
        for (p in Stickers.packs) assertTrue(p.id, Stickers.inPack(p.id).isNotEmpty())
        for (d in Stickers.all) {
            assertTrue(d.id, Stickers.packs.any { it.id == d.pack })
            assertTrue(d.id, d.aspect in 0.2f..8f)
        }
    }

    @Test fun doodlePathsUseOnlySupportedCommands() {
        val ok = Regex("^[MLHVCQTAZ0-9 .,-]+$")
        for ((k, d) in Doodles.paths) {
            assertTrue("$k: $d", ok.matches(d))
            assertTrue(k, d.startsWith("M"))
        }
        assertTrue(Doodles.paths.size >= 70)
    }

    @Test fun searchMatchesEveryWord() {
        assertTrue(Stickers.search("tape pink").all { "pink" in (it.name + it.tags).lowercase() })
        assertTrue(Stickers.search("plane").isNotEmpty())
        assertTrue(Stickers.search("").isEmpty())
        assertTrue(Stickers.search("bead A").any { it.text == "A" })
    }

    @Test fun placeholdersFillFromTheTrip() {
        val info = TripInfo("Summer", "1–5 Jun", "Lisbon")
        assertEquals("Hello from Lisbon", Stickers.fill("Hello from {place}", info))
        assertEquals("Summer 1–5 Jun", Stickers.fill("{title} {dates}", info))
        assertEquals("Hello from Summer", Stickers.fill("Hello from {place}", TripInfo("Summer")))
    }

    @Test fun everyFrameLeavesRoomForThePhoto() {
        for (k in FrameKind.entries) for ((w, h) in listOf(800f to 1000f, 1000f to 800f, 600f to 600f)) {
            val i = PhotoGeometry.inner(w, h, k)
            assertTrue("$k ${w}x$h: $i", i.width > w * 0.3f && i.height > h * 0.3f)
            assertTrue("$k inside", i.left >= -w / 2 && i.right <= w / 2 && i.top >= -h / 2 && i.bottom <= h / 2)
        }
    }

    @Test fun frameLibraryIdsAreUniqueAndGrouped() {
        assertTrue(FrameLibrary.all.size >= 80)
        assertEquals(FrameLibrary.all.size, FrameLibrary.all.map { it.id }.toSet().size)
        for (f in FrameLibrary.all) assertTrue(f.id, f.group in FrameLibrary.groups)
        for (g in FrameLibrary.groups) assertTrue(g, FrameLibrary.all.any { it.group == g })
    }

    @Test fun fitIntoKeepsTheShapeInsideTheBox() {
        val b = PhotoGeometry.fitInto(Box(0f, 0f, 1000f, 500f), 0.5f)
        assertEquals(250f, b.width, 0.01f); assertEquals(500f, b.height, 0.01f); assertEquals(500f, b.cx, 0.01f)
        val t = PhotoGeometry.fitInto(Box(0f, 0f, 400f, 1000f), 2f)
        assertEquals(400f, t.width, 0.01f); assertEquals(200f, t.height, 0.01f); assertEquals(500f, t.cy, 0.01f)
        assertEquals(Box(0f, 0f, 1f, 1f), PhotoGeometry.fitInto(Box(0f, 0f, 1f, 1f), null))
    }
}
