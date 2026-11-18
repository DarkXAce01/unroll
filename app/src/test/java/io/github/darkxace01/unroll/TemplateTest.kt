package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.data.ProjectJson
import io.github.darkxace01.unroll.model.ColorRole
import io.github.darkxace01.unroll.model.MAX_SLIDES
import io.github.darkxace01.unroll.model.NewPhoto
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Templates
import io.github.darkxace01.unroll.model.TextItem
import io.github.darkxace01.unroll.model.Themes
import io.github.darkxace01.unroll.model.TripInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateTest {
    private var n = 0
    private fun id() = "i${n++}"
    private fun photos(k: Int) = (0 until k).map { NewPhoto("content://p/$it", if (it % 3 == 0) 3000 else 4000, if (it % 3 == 0) 4000 else 3000) }
    private val info = TripInfo("Lisbon", "12–16 May", "Lisbon", 5)

    @Test fun thereAreEnoughTemplatesAndIdsAreUnique() {
        assertTrue(Templates.all.size >= 15)
        assertEquals(Templates.all.size, Templates.all.map { it.id }.toSet().size)
    }

    @Test fun everyTemplateStretchesToAnyNumberOfPhotos() {
        for (t in Templates.all) for (k in listOf(0, 1, 3, 8, 25)) {
            val p = Templates.build(t, photos(k), info, "p", "x", 0, ::id)
            assertTrue("${t.id} with $k: ${p.slides} slides", p.slides in 1..MAX_SLIDES)
            val placed = p.items.filterIsInstance<PhotoItem>()
            val filled = placed.filterNot { it.isEmpty }
            // Every photo is used unless the carousel hit the 20-slide limit.
            if (p.slides < MAX_SLIDES) assertEquals("${t.id} with $k", k, filled.size)
            if (k == 0) assertTrue("${t.id} has slots", placed.isNotEmpty() && placed.all { it.isEmpty })
            assertEquals("${t.id}: unique ids", p.items.size, p.items.map { it.id }.toSet().size)
            // Nothing lands off the canvas.
            for (it in p.items) {
                val b = it.frame.bounds()
                assertTrue("${t.id} ${it.id} at ${b}", b.cx >= -10f && b.cx <= p.width + 10f && b.cy >= -10f && b.cy <= p.height + 10f)
            }
        }
    }

    @Test fun tokensAreFilledIn() {
        for (t in Templates.all) {
            val p = Templates.build(t, photos(4), info, "p", "x", 0, ::id)
            for (txt in p.items.filterIsInstance<TextItem>()) assertFalse("${t.id}: ${txt.text}", txt.text.contains('{'))
        }
    }

    @Test fun everyTemplateSurvivesSaving() {
        for (t in Templates.all) {
            val p = Templates.build(t, photos(5), info, "p", "x", 0, ::id)
            assertEquals(t.id, ProjectJson.decode(ProjectJson.encode(p)), p)
        }
    }

    @Test fun switchingThemeRecoloursRoleItems() {
        val p = Templates.build(Templates.all.first(), photos(3), info, "p", "x", 0, ::id)
        val text = p.items.filterIsInstance<TextItem>().first { it.colorRole == ColorRole.Ink }
        assertEquals(Themes.Paper.ink, p.color(text.color, text.colorRole))
        val q = p.copy(theme = Themes.Midnight)
        assertEquals(Themes.Midnight.ink, q.color(text.color, text.colorRole))
        assertEquals(Themes.Midnight.head, q.font(text.font, text.fontRole).takeIf { text.fontRole == io.github.darkxace01.unroll.model.FontRole.Head } ?: Themes.Midnight.head)
    }

    @Test fun slotsListsEmptyOnes() {
        val p = Templates.build(Templates.all.first(), emptyList(), info, "p", "x", 0, ::id)
        assertEquals(p.items.filterIsInstance<PhotoItem>().size, p.slots().size)
    }
}
