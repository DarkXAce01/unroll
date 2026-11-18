package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.model.Category
import io.github.darkxace01.unroll.model.TemplateSearch
import io.github.darkxace01.unroll.model.Templates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateSearchTest {
    private val all = Templates.everything
    private fun s(q: String) = TemplateSearch.search(q, all)
    private fun top(q: String, n: Int = 8) = s(q).take(n)

    @Test fun friendsFindsFriendlyTemplates() {
        for (q in listOf("friends", "bestie", "squad", "girls trip", "my besties", "freinds", "frien")) {
            val r = top(q)
            assertTrue("$q found nothing", r.isNotEmpty())
            val good = setOf(Category.Recap, Category.Celebrations, Category.Scribbles, Category.Instant, Category.Travel, Category.Digital, Category.Scrapbook)
            assertTrue("$q top: ${r.map { it.name }}", r.all { it.category in good })
        }
    }

    @Test fun meaningsLandInTheRightPlace() {
        assertEquals(Category.Weddings, top("bride", 1).single().category)
        assertEquals(Category.Celebrations, top("bday", 1).single().category)
        assertEquals(Category.Love, top("boyfriend", 1).single().category)
        assertEquals(Category.Fashion, top("ootd", 1).single().category)
        assertEquals(Category.Film, top("analog", 1).single().category)
        assertTrue(top("vacay").all { it.category == Category.Travel || it.category == Category.Stationery })
        assertTrue(top("photo dump", 3).all { it.category == Category.Recap })
        assertTrue(top("hiking").first().category == Category.Outdoor)
        assertTrue(top("panorama").all { it.layout == "seam" || it.name.contains("Seam", true) || it.name.contains("Panorama", true) })
    }

    @Test fun typosAndPluralsStillWork() {
        assertEquals(Category.Weddings, top("weddding", 1).single().category)
        assertEquals(Category.Celebrations, top("birthdya", 1).single().category)
        assertTrue(top("beaches").isNotEmpty())
    }

    @Test fun literalNamesStillWork() {
        assertTrue(top("noir").any { it.kit == "film-noir" })
        assertTrue(top("polaroid").isNotEmpty())
    }

    @Test fun gibberishFindsNothing() {
        for (q in listOf("asdfgh", "xqzv", "qwerty", "zzzz", "jkjkjk", "hgfdsa", "templates", "for my", "bla", "lol")) assertEquals(q, emptyList<Any>(), s(q))
    }

    @Test fun twoWordsPreferBoth() {
        val r = top("friends beach", 3)
        assertTrue(r.map { it.name }.toString(), r.all { it.kit == "beach-days" || it.category == Category.Travel || it.category == Category.Recap })
    }
}

/** Prints what common searches return, to eyeball the ranking: -PtemplateSheets only. */
class TemplateSearchPrint {
    @Test fun print() {
        org.junit.Assume.assumeTrue(System.getProperty("unroll.templateSheets") == "true")
        for (q in listOf("friends", "girls trip", "bday", "couple", "graduation", "dog", "coffee", "summer", "christmas", "concert", "family", "aesthetic", "business", "freinds", "bla", "lol"))
            println("$q -> " + TemplateSearch.search(q, Templates.everything).take(6).joinToString { it.name } + "  (" + TemplateSearch.search(q, Templates.everything).size + ")")
    }
}
