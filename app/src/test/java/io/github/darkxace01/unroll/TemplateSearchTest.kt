package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.model.Category
import io.github.darkxace01.unroll.model.TemplateSearch
import io.github.darkxace01.unroll.model.Templates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The real packed word meanings from the app's assets. */
val testVectors: io.github.darkxace01.unroll.model.WordVectors by lazy {
    val dir = java.io.File("src/main/assets/search")
    io.github.darkxace01.unroll.model.WordVectors.read(java.io.File(dir, "vocab.txt").inputStream(), java.io.File(dir, "vectors.bin").inputStream())
}

class TemplateSearchTest {
    private val all = Templates.everything
    private fun s(q: String) = TemplateSearch.search(q, all, testVectors)
    private fun top(q: String, n: Int = 8) = s(q).take(n)

    @Test fun friendsFindsFriendlyTemplates() {
        for (q in listOf("friends", "bestie", "squad", "girls trip", "my besties", "freinds", "frien")) {
            val r = top(q)
            assertTrue("$q found nothing", r.isNotEmpty())
            val good = setOf(Category.Love, Category.Recap, Category.Celebrations, Category.Scribbles, Category.Instant, Category.Travel, Category.Digital, Category.Scrapbook)
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
        assertTrue(top("panorama").all { it.layout == "seam" || it.layout == "pano" || it.name.contains("Seam", true) || it.name.contains("Panorama", true) })
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
        for (q in listOf("asdfgh", "xqzv", "qwerty", "zzzz", "jkjkjk", "hgfdsa", "templates", "for my", "xyzq", "hjkl")) assertEquals(q, emptyList<Any>(), s(q))
    }

    @Test fun wordsNeverListedStillFindTheirLook() {
        // None of these are in the hand-written concepts: their meaning comes from the word vectors.
        assertEquals(Category.Celebrations, top("bachelorette", 1).single().category)
        assertEquals(Category.Recap, top("roommates", 1).single().category)
        assertEquals(Category.Outdoor, top("picnic", 1).single().category)
        assertEquals(Category.Digital, top("anime", 1).single().category)
        assertTrue(s("car").size <= 60)
    }

    @Test fun twoWordsPreferBoth() {
        // No template is about both, so most of the first three should be the beach ones that suit friends.
        val r = top("friends beach", 3)
        val beachy = r.count { it.kit == "beach-days" || it.category == Category.Travel || it.category == Category.Recap || "beach" in it.tags }
        assertTrue(r.map { it.name }.toString(), beachy >= 2)
    }
}

/** Prints what common searches return, to eyeball the ranking: -PtemplateSheets only. */
class TemplateSearchPrint {
    @Test fun print() {
        org.junit.Assume.assumeTrue(System.getProperty("unroll.templateSheets") == "true")
        for (q in listOf("friends beach", "friends", "roommates", "prom", "picnic", "bachelorette", "sleepover", "thanksgiving", "gym", "yoga", "fishing", "graduation", "skate", "ramadan", "brunch", "puppy", "grandma", "paris", "snowboarding", "football", "anime", "gaming", "car", "garden", "rain", "sunset", "wine", "lol", "qwerty", "asdfgh", "frie", "wedd", "par", "lov", "beac", "xyzq"))
            println("$q -> " + TemplateSearch.search(q, Templates.everything, testVectors).take(6).joinToString { it.name } + "  (" + TemplateSearch.search(q, Templates.everything, testVectors).size + ")")
    }
}
