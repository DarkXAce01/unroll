package io.github.darkxace01.unroll.model

/**
 * Hand-named shelves of templates for moments in the year ("Holiday season" in December, "Summer trips"
 * in July) plus a few that are always good. Each one is a search by meaning, so new templates join the
 * right shelves by themselves.
 */
data class TemplateCollection(
    val id: String,
    val title: String,
    val line: String,
    val query: String,
    /** Months (1-12) when it's in season; empty = all year. */
    val months: Set<Int> = emptySet(),
)

object Collections {
    val all = listOf(
        TemplateCollection("holidays", "Holiday season", "Lights, family, too much food", "christmas party family winter", setOf(11, 12)),
        TemplateCollection("year", "Your year", "Twelve months in one swipe", "recap year memories", setOf(12, 1)),
        TemplateCollection("valentine", "Valentine's", "For the two of you", "love couple valentine", setOf(1, 2)),
        TemplateCollection("spring", "Spring days", "Blossom, picnics, long walks", "garden flowers picnic nature", setOf(3, 4, 5)),
        TemplateCollection("weddings", "Wedding season", "Vows, dances, the whole day", "wedding", setOf(5, 6, 7, 8, 9)),
        TemplateCollection("summer", "Summer trips", "Sea, sun, somewhere new", "summer beach travel", setOf(6, 7, 8)),
        TemplateCollection("school", "Back to school", "New year, new notebook", "school notes", setOf(8, 9)),
        TemplateCollection("autumn", "Cosy & spooky", "Leaves, films, Halloween", "halloween autumn film", setOf(10, 11)),
        TemplateCollection("friends", "Friends", "Nights out and group chats", "friends party"),
        TemplateCollection("travel", "Trips", "Routes, tickets, postcards", "travel trip"),
        TemplateCollection("clean", "Clean & simple", "Let the photos talk", "minimal clean"),
        TemplateCollection("film", "Analog", "Grain, frames, contact sheets", "film vintage"),
    )

    /** What to show in [month]: the season's shelves first, then the always-good ones; four in all. */
    fun forMonth(month: Int, max: Int = 4): List<TemplateCollection> {
        val (seasonal, always) = all.partition { it.months.isNotEmpty() }
        return (seasonal.filter { month in it.months } + always).take(max)
    }

    fun byId(id: String) = all.firstOrNull { it.id == id }

    fun templates(c: TemplateCollection, from: List<Template>, v: WordVectors? = TemplateSearch.vectors): List<Template> =
        TemplateSearch.search(c.query + " ", from, v)
}
