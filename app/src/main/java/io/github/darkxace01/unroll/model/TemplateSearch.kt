package io.github.darkxace01.unroll.model

/**
 * Search by meaning, fully offline: "friends", "bestie" and "girls trip" all find the photo dumps, party
 * and doodle templates even though none of them say "friends". Each [Concept] links everyday words
 * (slang and typos included) to the categories and style kits that suit them. Words that mean nothing
 * here (gibberish) find nothing, rather than a random guess.
 */
object TemplateSearch {

    class Concept(
        val words: List<String>,
        val cats: Map<Category, Float> = emptyMap(),
        val kits: Map<String, Float> = emptyMap(),
        val layouts: Map<String, Float> = emptyMap(),
    )

    private fun c(words: String, cats: Map<Category, Float> = emptyMap(), kits: Map<String, Float> = emptyMap(), layouts: Map<String, Float> = emptyMap()) =
        Concept(words.split(',').map { it.trim().replace(" ", "") }.filter { it.isNotEmpty() }, cats, kits, layouts)

    private val C = Category.entries.associateBy { it }

    val concepts: List<Concept> = listOf(
        c("gym, workout, workouts, fitness, fit, training, sport, sports, football, soccer, basketball, tennis, running, run, marathon, race, yoga, pilates, cycling, team, match, game, athlete, progress, transformation",
            mapOf(Category.Bold to 1f, Category.Recap to 0.6f, Category.Minimal to 0.55f, Category.Grids to 0.55f, Category.Editorial to 0.5f),
            mapOf("admit-one" to 0.6f, "grid-lines" to 0.6f, "photo-dump" to 0.5f)),
        c("friends, friend, friendship, bestie, besties, bff, bffs, squad, crew, gang, homies, homie, buddies, buddy, mates, pals, pal, girls, boys, girlstrip, boystrip, group, hangout, hanging out, nightout, sleepover, reunion, together",
            mapOf(Category.Recap to 1f, Category.Celebrations to 0.85f, Category.Scribbles to 0.8f, Category.Instant to 0.75f, Category.Digital to 0.6f, Category.Scrapbook to 0.6f, Category.Film to 0.4f),
            mapOf("photo-dump" to 1f, "party" to 0.85f, "scribbles" to 0.8f, "instant-pastel" to 0.75f, "screen-time" to 0.6f)),
        c("love, lovers, couple, couples, boyfriend, girlfriend, bf, gf, partner, anniversary, valentine, valentines, date, datenight, romance, romantic, us, husband, wife, crush, soulmate, relationship, kiss, hearts",
            mapOf(Category.Love to 1f, Category.Weddings to 0.7f, Category.Instant to 0.55f, Category.Portraits to 0.4f),
            mapOf("love-notes" to 1f, "instant-pastel" to 0.6f, "wedding" to 0.65f)),
        c("wedding, weddings, bride, groom, bridal, engaged, engagement, marriage, married, proposal, propose, honeymoon, ceremony, vows, bridesmaid, reception",
            mapOf(Category.Weddings to 1f, Category.Love to 0.6f, Category.Editorial to 0.3f), mapOf("wedding" to 1f, "love-notes" to 0.5f)),
        c("birthday, bday, party, parties, celebrate, celebration, cake, turning, graduation, grad, newyear, nye, christmas, xmas, holidays, halloween, eid, diwali, festive, anniversary, congrats, babyshower, prom, homecoming, dance, formal, thanksgiving, easter, hanukkah, ramadan, kwanzaa, bachelorette, bachelor, housewarming, quinceanera, carnival, pride",
            mapOf(Category.Celebrations to 1f, Category.Recap to 0.45f, Category.Bold to 0.35f), mapOf("party" to 1f, "photo-dump" to 0.4f)),
        c("travel, travels, traveling, travelling, trip, trips, vacation, vacay, holiday, abroad, journey, roadtrip, flight, plane, airport, europe, asia, paris, italy, japan, city, citybreak, explore, exploring, wanderlust, backpacking, interrail, tour, itinerary, passport, abroad",
            mapOf(Category.Travel to 1f, Category.Stationery to 0.65f, Category.Story to 0.5f, Category.Outdoor to 0.45f, Category.Film to 0.3f),
            mapOf("polaroid-travel" to 1f, "postcards" to 0.85f, "stamps" to 0.8f, "beach-days" to 0.7f)),
        c("beach, beaches, summer, sea, seaside, ocean, sand, island, tropical, pool, surf, surfing, swim, swimming, sunny, sun, coast, bikini, palm, sunset, sunsets, sunrise, goldenhour",
            mapOf(Category.Travel to 0.6f, Category.Outdoor to 0.45f), mapOf("beach-days" to 1f, "polaroid-travel" to 0.45f, "postcards" to 0.4f)),
        c("hike, hiking, camping, camp, mountain, mountains, nature, forest, woods, outdoors, outdoor, adventure, trail, lake, park, nationalpark, ski, skiing, snow, winter, autumn, fall, garden, flowers, plants",
            mapOf(Category.Outdoor to 1f, Category.Paper to 0.55f, Category.Travel to 0.45f), mapOf("out-there" to 1f, "torn-journal" to 0.6f)),
        c("dump, photodump, recap, month, monthly, week, weekly, weekend, lately, random, randoms, year, yearinreview, highlights, memories, memory, summary, everyday, life, dailylife, moments, camera roll, cameraroll",
            mapOf(Category.Recap to 1f, Category.Scrapbook to 0.55f, Category.Instant to 0.45f, Category.Grids to 0.4f), mapOf("photo-dump" to 1f)),
        c("fashion, outfit, outfits, ootd, style, styling, look, looks, lookbook, fit, fits, clothes, clothing, model, modeling, streetstyle, shopping, haul, makeup, beauty, vogue",
            mapOf(Category.Fashion to 1f, Category.Editorial to 0.8f, Category.Minimal to 0.45f), mapOf("editorial" to 1f, "clean-white" to 0.4f)),
        c("selfie, selfies, portrait, portraits, me, myself, headshot, face, faces, people, photoshoot, shoot, model, profile",
            mapOf(Category.Portraits to 1f, Category.Fashion to 0.5f, Category.Instant to 0.4f), mapOf("portraits" to 1f)),
        c("family, fam, mom, mum, dad, parents, kids, children, siblings, sister, brother, grandma, grandpa, cousins, home",
            mapOf(Category.Portraits to 0.7f, Category.Scrapbook to 0.75f, Category.Instant to 0.7f, Category.Celebrations to 0.5f, Category.Recap to 0.5f),
            mapOf("instant-pastel" to 0.6f, "portraits" to 0.6f)),
        c("baby, babies, newborn, pregnancy, pregnant, toddler, firstyear, milestone, milestones",
            mapOf(Category.Instant to 0.8f, Category.Portraits to 0.7f, Category.Love to 0.5f), mapOf("instant-pastel" to 0.9f, "soft-round" to 0.6f)),
        c("dog, dogs, puppy, puppies, cat, cats, kitten, kitty, pet, pets, doggo",
            mapOf(Category.Portraits to 0.7f, Category.Scribbles to 0.75f, Category.Instant to 0.65f), mapOf("scribbles" to 0.7f, "portraits" to 0.6f)),
        c("food, foodie, restaurant, restaurants, brunch, breakfast, lunch, dinner, coffee, cafe, recipe, recipes, cooking, baking, eat, eats, drinks, cocktails",
            mapOf(Category.Stationery to 0.65f, Category.Recap to 0.6f, Category.Minimal to 0.55f, Category.Editorial to 0.5f, Category.Grids to 0.5f),
            mapOf("notebook" to 0.8f, "clean-white" to 0.6f, "grid-lines" to 0.5f)),
        c("school, college, uni, university, study, studying, student, class, classes, notes, campus, exam, semester",
            mapOf(Category.Stationery to 0.8f, Category.Scribbles to 0.6f), mapOf("notebook" to 1f, "scribbles" to 0.5f)),
        c("film, analog, analogue, vintage, retro, 35mm, filmcamera, disposable, cinema, movie, movies, noir, blackandwhite, bw, grain, nostalgia, nostalgic, y2k, 90s, 80s",
            mapOf(Category.Film to 1f, Category.Bold to 0.6f, Category.Instant to 0.45f), mapOf("film-noir" to 0.9f, "contact-sheet" to 0.9f, "admit-one" to 0.7f)),
        c("minimal, minimalist, clean, simple, plain, white, neutral, elegant, classy, modern, sleek",
            mapOf(Category.Minimal to 1f, Category.Grids to 0.7f, Category.Editorial to 0.5f), mapOf("clean-white" to 1f, "soft-round" to 0.8f, "grid-lines" to 0.7f)),
        c("business, brand, branding, product, products, shop, store, portfolio, launch, work, job, announcement, announce, tips, tutorial, howto, infographic, smallbusiness, sale",
            mapOf(Category.Editorial to 0.85f, Category.Minimal to 0.75f, Category.Grids to 0.7f, Category.Bold to 0.6f), mapOf("editorial" to 0.7f, "clean-white" to 0.6f)),
        c("art, artsy, drawing, doodle, doodles, sketch, creative, journal, journaling, scrapbook, scrapbooking, collage, diary, handmade, crafty, crafts, stickers",
            mapOf(Category.Scrapbook to 1f, Category.Scribbles to 0.9f, Category.Paper to 0.9f, Category.Stationery to 0.7f), mapOf("scribbles" to 0.8f, "torn-journal" to 0.8f)),
        c("cute, kawaii, pastel, pink, girly, soft, sweet, aesthetic, dreamy, coquette",
            mapOf(Category.Instant to 0.9f, Category.Love to 0.6f, Category.Minimal to 0.4f), mapOf("instant-pastel" to 1f, "soft-round" to 0.8f, "love-notes" to 0.6f)),
        c("dark, moody, night, nights, nightlife, black, edgy, grunge, gothic",
            mapOf(Category.Film to 0.9f, Category.Bold to 0.5f), mapOf("film-noir" to 1f)),
        c("concert, concerts, music, festival, festivals, gig, gigs, show, tour, rave, band",
            mapOf(Category.Celebrations to 0.7f, Category.Bold to 0.8f, Category.Film to 0.5f, Category.Recap to 0.5f), mapOf("admit-one" to 1f, "party" to 0.6f)),
        c("bold, loud, colorful, colourful, bright, fun, playful, vibrant, pop",
            mapOf(Category.Bold to 1f, Category.Celebrations to 0.6f, Category.Digital to 0.5f)),
        c("phone, screenshot, screenshots, app, apps, chat, texts, tweet, meme, memes, online, digital, lol, lmao, funny, jokes, humor, gaming, games, gamer, videogames, anime, manga, cartoon, comics, internet",
            mapOf(Category.Digital to 1f), mapOf("screen-time" to 1f)),
        c("story, stories, reel, reels, vertical, tiktok, fullscreen",
            mapOf(Category.Story to 1f)),
        c("seamless, panorama, panoramic, continuous, wide, landscape, swipe, flow",
            layouts = mapOf("seam" to 1f, "hero" to 0.8f)),
        c("grid, grids, collage, tiles, mosaic, many, lots",
            mapOf(Category.Grids to 1f), layouts = mapOf("grid" to 0.9f, "mosaic" to 0.9f)),
        c("postcard, postcards, letter, letters, mail, stamp, stamps, postage",
            mapOf(Category.Stationery to 0.9f), mapOf("postcards" to 1f, "stamps" to 1f)),
        c("polaroid, polaroids, instax, instant, instants",
            mapOf(Category.Instant to 1f), mapOf("polaroid-travel" to 1f, "instant-pastel" to 1f)),
        c("magazine, editorial, cover, article, issue, newspaper, zine",
            mapOf(Category.Editorial to 1f, Category.Fashion to 0.6f), mapOf("editorial" to 0.8f)),
        c("ticket, tickets, admit, cinema, receipt, boardingpass",
            mapOf(Category.Bold to 0.7f), mapOf("admit-one" to 1f, "polaroid-travel" to 0.4f)),
    )

    /** Words that carry no meaning in a template search ("templates for my trip" means "trip"). */
    private val stop = setOf(
        "a", "an", "the", "for", "my", "our", "your", "with", "and", "of", "to", "in", "on", "at", "some", "any", "ideas", "idea",
        "template", "templates", "post", "posts", "carousel", "carousels", "instagram", "insta", "ig", "pics", "pic", "photos",
        "photo", "pictures", "picture", "slides", "slide", "about", "from", "this", "that", "i", "we", "best", "nice", "good", "cool",
        "new", "style", "styles", "theme", "themes", "vibe", "vibes",
    )

    private val lexicon: Map<String, List<Int>> by lazy {
        val m = HashMap<String, MutableList<Int>>()
        concepts.forEachIndexed { i, c -> c.words.forEach { m.getOrPut(it) { mutableListOf() } += i } }
        m
    }

    fun tokens(q: String): List<String> = q.lowercase()
        .replace(Regex("[^\\p{L}\\p{N}&]+"), " ")
        .split(' ').filter { it.isNotBlank() }

    /** Words for one template from its own name, category, kit and tags (for literal matches like "noir"). */
    private fun ownWords(t: Template) = tokens("${t.name} ${t.category.label} ${t.category.name} ${t.tags} ${t.theme.name} ${t.layout ?: ""}").toSet()

    private val own = HashMap<String, Set<String>>()

    /** How well one query word fits one template, 0..1. */
    private fun fit(t: Template, hits: Map<Int, Float>, word: String): Float {
        var best = 0f
        val words = own.getOrPut(t.id) { ownWords(t) }
        if (word in words) best = 1f
        else if (word.length >= 3 && words.any { it.startsWith(word) }) best = 0.9f
        else if (word.length >= 5 && words.any { it.length >= 4 && close(word, it) }) best = 0.8f
        for ((ci, w) in hits) {
            val c = concepts[ci]
            val a = maxOf(c.cats[t.category] ?: 0f, t.kit?.let { c.kits[it] } ?: 0f, t.layout?.let { c.layouts[it] } ?: 0f)
            best = maxOf(best, a * w)
        }
        return best
    }

    /** Word meanings, loaded once at start; without them search falls back to the concept words alone. */
    @Volatile var vectors: WordVectors? = null

    /** Similarity below this is chance; at [SURE] or above a word means the same thing for search. */
    private const val CHANCE = 0.42f
    private const val SURE = 0.72f

    /** Which concepts a query word means, with how sure we are (typos and half-typed words count less). */
    fun meaning(word: String, v: WordVectors? = vectors): Map<Int, Float> {
        lexicon[word]?.let { l -> return l.associateWith { 1f } }
        val out = HashMap<Int, Float>()
        fun add(c: Int, s: Float) { if (s > (out[c] ?: 0f)) out[c] = s }
        val known = v != null && v.has(word)
        if (known) {
            // Real meaning: how close this word sits to each concept's own words.
            concepts.forEachIndexed { ci, c ->
                var best = 0f
                for (cw in c.words) { val x = v!!.sim(word, cw); if (x > best) best = x }
                if (best >= CHANCE) add(ci, (0.45f + 0.55f * (best - CHANCE) / (SURE - CHANCE)).coerceAtMost(0.97f))
            }
        }
        if (out.isEmpty() && !known) {
            // Not a word we know: a typo of one ("freinds"), or an ending ("weddings").
            for ((w, cs) in lexicon) {
                val sc = when {
                    word.length >= 4 && w.length >= 4 && close(word, w) -> 0.8f
                    word.length >= 5 && word.startsWith(w) && w.length >= 4 && word.length - w.length <= 3 -> 0.85f
                    else -> 0f
                }
                if (sc > 0f) cs.forEach { add(it, sc) }
            }
            if (out.isEmpty() && v != null && word.length >= 4) v.correct(word)?.let { fixed ->
                meaning(fixed, v).forEach { (c, sc) -> add(c, sc * 0.9f) }
            }
        }
        return out
    }

    /** The last word while it's still being typed: the word itself if it is one, plus likely endings. */
    private fun typed(word: String, v: WordVectors?): Map<Int, Float> {
        val out = HashMap(meaning(word, v))
        if (out.isNotEmpty() && v != null && v.has(word)) return out
        val ends = (v?.completions(word, 3) ?: emptyList()) + lexicon.keys.filter { it.length > word.length && it.startsWith(word) }
        for (w in ends.distinct()) meaning(w, v).forEach { (c, sc) -> if (sc * 0.9f > (out[c] ?: 0f)) out[c] = sc * 0.9f }
        return out
    }

    /** Edit distance at most 1 (2 for long words), with a swap of neighbours counting as one. */
    fun close(a: String, b: String): Boolean {
        val max = if (minOf(a.length, b.length) >= 8) 2 else 1
        if (kotlin.math.abs(a.length - b.length) > max) return false
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            var rowMin = Int.MAX_VALUE
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = minOf(v, d[i - 2][j - 2] + 1)
                d[i][j] = v
                rowMin = minOf(rowMin, v)
            }
            if (rowMin > max) return false
        }
        return d[a.length][b.length] <= max
    }

    /**
     * Templates for [q], best first; empty when no word in it means anything here. A template must fit
     * every meaningful word a little, so "friends beach" prefers templates good for both.
     */
    fun search(q: String, all: List<Template>, v: WordVectors? = vectors): List<Template> {
        val raw = tokens(q)
        // Still typing the last word (no space after it yet) and it's short of a word: complete it.
        val typing = q.isNotEmpty() && !q.last().isWhitespace()
        // Two-word phrases first ("photo dump", "girls trip", "road trip").
        val words = mutableListOf<String>()
        var i = 0
        while (i < raw.size) {
            val pair = if (i + 1 < raw.size) raw[i] + raw[i + 1] else null
            if (pair != null && pair in lexicon) { words += pair; i += 2 } else { words += raw[i]; i++ }
        }
        val meaningful = words.filter { it !in stop }
        if (meaningful.isEmpty()) return emptyList()
        val last = words.lastOrNull()
        val parsed = meaningful.map { w ->
            w to (if (typing && w == last && w.length >= 3 && lexicon[w] == null && true) typed(w, v) else meaning(w, v))
        }
        val scored = all.mapNotNull { t ->
            var total = 0f
            var matched = 0
            for ((w, hits) in parsed) {
                val f = fit(t, hits, w)
                if (f >= 0.5f) { total += f; matched++ }
            }
            if (matched == 0) null else t to total * matched / parsed.size
        }
        if (scored.isEmpty()) return emptyList()
        val top = scored.maxOf { it.second }
        // Keep the clear matches only: anything far below the best result is noise.
        // Mix styles: each further template from the same kit counts a little less, so the first screen
        // shows several looks instead of six layouts of one.
        val seen = HashMap<String, Int>()
        return scored.filter { it.second >= top * 0.62f }.sortedByDescending { it.second }.take(60)
            .map { (t, sc) -> val k = t.kit ?: t.id; val n = seen.merge(k, 1, Int::plus)!! - 1; t to sc * Math.pow(0.9, n.toDouble()).toFloat() }
            .sortedByDescending { it.second }.map { it.first }
    }
}
