package io.github.darkxace01.unroll.model

/** How a sticker is drawn. Each kind is one drawing routine in render/StickerArt; [StickerDef.param] picks the variant. */
enum class ArtKind {
    Doodle, Marker, DieCut, Bead, BeadWord, Ransom, RansomWord, Tape, Stamp, Postmark, Rubber, Ticket, Label, Clip,
    Patch, Title, Subtitle, Foil, Flower, Flag, Bubble,
}

data class StickerPack(val id: String, val name: String)

/**
 * One sticker in the library. [aspect] is width / height. Stickers with [text] have words you can change
 * (a postmark's town, a title, a label); [colorRole]/[color] are their default colour.
 */
data class StickerDef(
    val id: String,
    val name: String,
    val pack: String,
    val kind: ArtKind,
    val param: String = "",
    val aspect: Float = 1f,
    val text: String? = null,
    val color: Long? = null,
    val colorRole: ColorRole? = null,
    val tags: String = "",
) {
    val editable get() = text != null
}

/**
 * The sticker library: every sticker is drawn by the app itself (no image files), so all of them are
 * sharp at any size and can take the theme's colours. Built from parts, so it is large without being
 * hand-listed: every letter as a bead and as a cut-out, every doodle in four finishes, tapes in every
 * pattern and colourway, and so on.
 */
object Stickers {
    val packs = listOf(
        StickerPack("travel", "Travel"),
        StickerPack("scribbles", "Scribbles"),
        StickerPack("marker", "Marker doodles"),
        StickerPack("diecut", "Sticker sheet"),
        StickerPack("titles", "Titles"),
        StickerPack("subtitles", "Subtitles"),
        StickerPack("tape", "Tape"),
        StickerPack("stamps", "Stamps & postmarks"),
        StickerPack("tickets", "Tickets & tags"),
        StickerPack("labels", "Labels & notes"),
        StickerPack("scrapbook", "Pins & clips"),
        StickerPack("patches", "Patches"),
        StickerPack("beads", "Bead letters"),
        StickerPack("cutout", "Cut-out letters"),
        StickerPack("phrases", "Cut-out phrases"),
        StickerPack("shine", "Stars & shine"),
        StickerPack("flowers", "Flowers"),
        StickerPack("flags", "Flags"),
        StickerPack("bubbles", "Speech bubbles"),
    )

    private val palette = listOf(
        "Red" to 0xFFD9412EL, "Orange" to 0xFFEE8A2BL, "Yellow" to 0xFFF2C230L, "Green" to 0xFF4E9A5BL, "Teal" to 0xFF2A9D9AL,
        "Blue" to 0xFF3B6FD8L, "Navy" to 0xFF233063L, "Purple" to 0xFF7D5BD6L, "Pink" to 0xFFE77BA8L, "Brown" to 0xFF8A5A3BL,
        "Black" to 0xFF1D1E22L, "White" to 0xFFFFFFFFL,
    )

    private val travelDoodles = listOf("plane", "suitcase", "pin", "compass", "globe", "camera", "ticket", "passport", "map", "car", "bike",
        "sailboat", "anchor", "train", "luggage-tag", "palm", "sun", "mountain", "wave", "umbrella", "sunglasses", "shell", "icecream", "cocktail")

    val phrases = listOf(
        "Photo dump", "Wish you were here", "Out of office", "Vacation mode", "Far from home", "Lately", "Weekend", "Recap",
        "Good times", "Hello from", "Postcards", "Day trip", "On the road", "Golden hour", "Sea you soon", "Making memories",
        "Somewhere new", "Off the map", "Little moments", "Best days", "Home sweet home", "Adventure awaits", "Take me back",
        "Not all who wander", "Wanderlust", "Summer diaries", "Travel journal", "Field notes", "The one with", "Core memory",
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
        "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December",
        "Day one", "The end", "To be continued", "Swipe", "Part two", "Highlights", "Bon voyage", "Ciao", "Merci", "Gracias", "Arigato", "Namaste",
        "Happy birthday", "Just married", "Mr & Mrs", "Love you", "Best friends", "Girls trip", "Boys trip", "Family time", "Snow days",
        "Spring", "Summer", "Autumn", "Winter", "Coffee break", "Brunch club", "Date night", "Graduation", "Hello baby", "New year",
        "Cheers", "Bon appétit", "Self care", "Gym era", "Book club", "Road trip", "City break", "Island time", "Holiday", "Hello world", "Merry & bright",
    )

    private val titleStyles = listOf("stack", "spaced", "serif-script", "outline", "laurel", "boxed", "script-line", "magazine", "badge-circle", "date-slash")

    private val subtitles = listOf(
        "And just like that, we were somewhere else.", "The light here moves slower.", "We didn't plan this part.",
        "Salt in our hair, sun on our skin.", "Every street had a story.", "Lost track of the days, happily.",
        "Some places stay with you.", "We kept saying \"one more photo\".", "The best seat in the house.", "Nowhere to be. Nowhere better.",
        "It rained, and we didn't care.", "Postcard weather.", "Remember this.", "Then the sun went down.", "Coffee first, plans later.",
        "Golden hour, every hour.", "We'll be back.", "Small town, big sky.", "The view was worth every step.", "Home is a feeling.",
    )

    val all: List<StickerDef> by lazy { build() }
    private val index by lazy { all.associateBy { it.id } }
    fun byId(id: String) = index[id]
    fun inPack(pack: String) = all.filter { it.pack == pack }

    /** Words in name, tags, pack and text all count; every word of the query must match. */
    fun search(q: String): List<StickerDef> {
        val words = q.lowercase().split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        return all.filter { d ->
            val hay = "${d.name} ${d.tags} ${d.pack} ${d.text.orEmpty()} ${packs.firstOrNull { it.id == d.pack }?.name.orEmpty()}".lowercase()
            words.all { it in hay }
        }
    }

    private fun label(k: String) = k.replace('-', ' ').replaceFirstChar { it.uppercase() }

    private fun build(): List<StickerDef> = buildList {
        // Travel: doodles in a full-colour die-cut finish, plus place-aware stickers.
        for (k in travelDoodles) add(StickerDef("travel/$k", label(k), "travel", ArtKind.DieCut, k, colorRole = ColorRole.Accent, tags = "travel trip $k"))
        add(StickerDef("travel/boarding", "Boarding pass", "travel", ArtKind.Ticket, "boarding", 2.2f, text = "{place}", tags = "flight plane ticket"))
        add(StickerDef("travel/postmark", "Postmark", "travel", ArtKind.Postmark, "round", 1f, text = "{place}", colorRole = ColorRole.Ink, tags = "post mail"))
        add(StickerDef("travel/arrived", "Arrived stamp", "travel", ArtKind.Rubber, "rect", 1.6f, text = "Arrived", colorRole = ColorRole.Accent, tags = "passport visa"))
        add(StickerDef("travel/tag", "Luggage tag", "travel", ArtKind.Ticket, "luggage", 0.55f, text = "{place}", tags = "bag"))
        add(StickerDef("travel/hello", "Hello from", "travel", ArtKind.Title, "serif-script", 1.8f, text = "Hello from {place}", colorRole = ColorRole.Ink, tags = "greetings"))

        // Every doodle as chalk/ink scribble, marker and die-cut sticker.
        for (k in Doodles.paths.keys) {
            add(StickerDef("scribbles/$k", label(k), "scribbles", ArtKind.Doodle, k, colorRole = ColorRole.Ink, tags = "doodle chalk hand drawn $k"))
            add(StickerDef("marker/$k", label(k), "marker", ArtKind.Marker, k, colorRole = ColorRole.Accent, tags = "doodle colour marker $k"))
            add(StickerDef("diecut/$k", label(k), "diecut", ArtKind.DieCut, k, colorRole = ColorRole.Accent, tags = "sticker cute $k"))
        }

        // Titles: each phrase in a default style; every style also offered on a few key phrases.
        phrases.forEachIndexed { i, ph ->
            val style = titleStyles[i % titleStyles.size]
            add(StickerDef("titles/${ph.lowercase().replace(' ', '-')}", ph, "titles", ArtKind.Title, style, aspect = 1.8f, text = ph, colorRole = ColorRole.Ink, tags = "title words heading"))
        }
        for (s in titleStyles) for (ph in listOf("Photo dump", "{place}", "{title}")) {
            add(StickerDef("titles/$s-${ph.filter { it.isLetter() }.lowercase()}", "${label(s)}: ${ph.trim('{', '}')}", "titles", ArtKind.Title, s, 1.8f, text = ph, colorRole = ColorRole.Ink, tags = "title $s"))
        }
        subtitles.forEachIndexed { i, s ->
            add(StickerDef("subtitles/$i", s.take(28), "subtitles", ArtKind.Subtitle, if (i % 2 == 0) "yellow" else "white", aspect = 6f, text = s, tags = "subtitle film caption movie"))
        }

        // Tape: every pattern in every colourway.
        val tapes = listOf("solid", "stripes", "dots", "grid", "gingham", "hearts", "stars", "zigzag", "wave", "checker")
        for (t in tapes) for ((n, c) in palette) add(StickerDef("tape/$t-${n.lowercase()}", "${label(t)} tape, ${n.lowercase()}", "tape", ArtKind.Tape, t, 3.6f, color = c, tags = "washi tape $t ${n.lowercase()}"))
        add(StickerDef("tape/masking", "Masking tape", "tape", ArtKind.Tape, "masking", 3.6f, color = 0xFFE8DCC0, tags = "masking kraft"))
        add(StickerDef("tape/kraft", "Kraft tape", "tape", ArtKind.Tape, "kraft", 3.6f, color = 0xFFC69C6D, tags = "kraft brown"))
        add(StickerDef("tape/clear", "Clear tape", "tape", ArtKind.Tape, "clear", 3.6f, color = 0x66FFFFFF, tags = "clear sellotape"))
        add(StickerDef("tape/words", "Words tape", "tape", ArtKind.Tape, "words", 4.5f, text = "handle with love", color = 0xFFF2E6C8, tags = "text"))

        // Stamps: a doodle inside a perforated stamp, a value and a word.
        val stampArt = listOf("palm", "mountain", "sailboat", "sun", "flower", "plane", "shell", "bird", "butterfly", "lemon", "cherry", "tulip",
            "compass", "fish", "house", "moon", "cactus", "mushroom", "daisy", "anchor", "train", "coffee", "heart", "star")
        stampArt.forEachIndexed { i, k ->
            val (n, c) = palette[i % 10]
            add(StickerDef("stamps/$k", "${label(k)} stamp", "stamps", ArtKind.Stamp, k, 0.8f, text = "${(i % 9 + 1) * 10}c", color = c, tags = "stamp post ${n.lowercase()} $k"))
        }
        for ((i, shape) in listOf("round", "wavy", "oval", "box").withIndex()) {
            add(StickerDef("stamps/postmark-$shape", "Postmark (${shape})", "stamps", ArtKind.Postmark, shape, if (shape == "wavy") 2.4f else 1f, text = "{place}", colorRole = ColorRole.Ink, tags = "postmark mail ${i}"))
        }
        val rubber = listOf("Arrived", "Departed", "Approved", "Visa", "Immigration", "Entry", "Exit", "Fragile", "Air mail", "Par avion", "First class", "Received")
        for ((i, w) in rubber.withIndex()) for (shape in listOf("rect", "oval")) {
            add(StickerDef("stamps/rubber-${w.lowercase().replace(' ', '-')}-$shape", "$w ($shape)", "stamps", ArtKind.Rubber, shape, if (shape == "rect") 1.7f else 1.5f, text = w,
                color = listOf(0xFFC2362BL, 0xFF2E5AA8L, 0xFF2F7A4AL, 0xFF6B3FA0L)[i % 4], tags = "passport rubber ink stamp"))
        }

        // Tickets & tags
        val tickets = listOf("boarding" to 2.2f, "train" to 2.2f, "admit" to 2.4f, "luggage" to 0.55f, "receipt" to 0.5f, "stub" to 1.8f, "boardingpass-vertical" to 0.5f, "barcode" to 2.6f)
        for ((t, a) in tickets) for ((n, c) in listOf("Cream" to 0xFFF4ECD8L, "White" to 0xFFFFFFFFL, "Pink" to 0xFFF6D3DCL, "Sky" to 0xFFD5E7F5L)) {
            add(StickerDef("tickets/$t-${n.lowercase()}", "${label(t)} (${n.lowercase()})", "tickets", ArtKind.Ticket, t, a, text = if (t == "barcode") "Unroll" else "{place}", color = c, tags = "ticket tag $t"))
        }

        // Labels & notes
        val labels = listOf("dymo" to 4f, "price" to 1.6f, "kraft-tag" to 0.6f, "index" to 1.5f, "typewriter" to 5f, "masking-label" to 4f, "sticky" to 1f, "torn-note" to 1.3f, "lined" to 0.8f, "date-tab" to 2.4f)
        for ((l, a) in labels) for ((n, c) in palette.filter { it.first in listOf("Yellow", "Pink", "Blue", "Green", "Black", "White") }) {
            val text = when (l) { "dymo" -> "SUMMER '26"; "price" -> "priceless"; "typewriter" -> "we were here."; "date-tab" -> "{dates}"; "sticky", "torn-note", "lined", "index" -> "don't forget\nthis day"; else -> "{place}" }
            add(StickerDef("labels/$l-${n.lowercase()}", "${label(l)} (${n.lowercase()})", "labels", ArtKind.Label, l, a, text = text, color = c, tags = "label note paper $l"))
        }

        // Pins & clips, in a dozen colours each.
        val clips = listOf("pushpin" to 0.8f, "paperclip" to 0.4f, "binder" to 1f, "bulldog" to 1.2f, "safety" to 3f, "staple" to 3f, "thumbtack" to 1f, "heart-clip" to 0.5f)
        for ((k, a) in clips) for ((n, c) in palette) {
            if (k == "staple" && n != "Black" && n != "White") continue
            add(StickerDef("scrapbook/$k-${n.lowercase()}", "${label(k)} (${n.lowercase()})", "scrapbook", ArtKind.Clip, k, a, color = c, tags = "pin clip scrapbook $k ${n.lowercase()}"))
        }

        // Embroidered patches
        val patches = listOf("circle", "shield", "banner", "cloud", "star", "heart", "rect")
        val patchWords = listOf("Vacation mode", "Out of office", "{place}", "Wish you were here", "Adventure club", "Sun & sea", "Road trip", "Holiday", "Ciao bella", "Born to travel")
        patchWords.forEachIndexed { i, w ->
            for (j in 0 until 2) {
                val shape = patches[(i + j * 3) % patches.size]
                val (n, c) = palette[(i * 2 + j) % 10]
                add(StickerDef("patches/${i}-$shape", "$w patch", "patches", ArtKind.Patch, shape, if (shape == "banner") 2.4f else if (shape == "rect") 1.6f else 1f, text = w, color = c, tags = "embroidered patch ${n.lowercase()}"))
            }
        }

        // Beads: every letter and number, in four bead styles; plus whole words.
        val glyphs = ('A'..'Z').map { it.toString() } + ('0'..'9').map { it.toString() } + listOf("♥", "★", "☺", "&", "!", "?")
        for (style in listOf("white", "black", "pastel", "gold")) for (g in glyphs) {
            add(StickerDef("beads/$style-${g.code()}", "Bead $g ($style)", "beads", ArtKind.Bead, style, 1f, text = g, tags = "bead letter $g $style"))
        }
        for ((style, w) in listOf("white" to "XOXO", "pastel" to "BESTIES", "black" to "{place}", "white" to "HOLIDAY", "gold" to "LOVE", "pastel" to "SUMMER",
                "white" to "BFF", "pastel" to "MAMA", "black" to "PAPA", "pastel" to "HAPPY", "white" to "BABY", "gold" to "SUN", "black" to "MOOD", "white" to "{title}")) {
            add(StickerDef("beads/word-${style}-${w.filter { it.isLetter() }.lowercase()}", "Bead word: ${w.trim('{', '}')}", "beads", ArtKind.BeadWord, style, 4f, text = w, tags = "bead word bracelet"))
        }

        // Cut-out letters: each letter in eight looks; phrases spelled out ransom-style.
        for (look in 0 until 8) for (g in ('A'..'Z').map { it.toString() } + ('0'..'9').map { it.toString() } + listOf("!", "?", "&", "#")) {
            add(StickerDef("cutout/$look-${g.code()}", "Cut-out $g", "cutout", ArtKind.Ransom, look.toString(), 0.8f, text = g, tags = "letter magazine ransom $g"))
        }
        for (ph in phrases) add(StickerDef("phrases/${ph.lowercase().replace(' ', '-')}", ph, "phrases", ArtKind.RansomWord, "", 3f, text = ph, tags = "phrase word magazine ransom"))

        // Shine: foil stars, hearts, sparkles in metallic finishes.
        val foils = listOf("star", "heart", "sparkle", "sparkles", "burst", "moon", "crown", "flower", "bolt", "cloud", "leaf", "lips", "fish", "blob")
        for (k in foils) for (metal in listOf("gold", "silver", "rose", "holo")) add(StickerDef("shine/$k-$metal", "${label(metal)} $k", "shine", ArtKind.Foil, "$k:$metal", tags = "foil shiny metallic $metal $k"))

        // Flowers drawn petal by petal.
        val flowers = listOf("daisy", "sunflower", "cosmos", "poppy", "blossom", "aster", "buttercup", "cornflower", "camellia", "marigold", "lavender", "leaf-sprig")
        for (f in flowers) add(StickerDef("flowers/$f", label(f), "flowers", ArtKind.Flower, f, if (f == "lavender" || f == "leaf-sprig") 0.5f else 1f, tags = "flower plant $f"))

        // Flags (simplified).
        for (f in Flags.all) add(StickerDef("flags/${f.code}", f.name, "flags", ArtKind.Flag, f.code, 1.5f, tags = "flag country ${f.name.lowercase()}"))

        // Speech bubbles & thought clouds.
        val bubbles = listOf("round", "cloud", "rect", "shout", "heart")
        for (b in bubbles) for (w in listOf("Hi!", "Wow", "Same", "Take me back", "Obsessed", "Yes!", "Ooh", "Mood", "Us", "Ha!", "Love it")) add(StickerDef("bubbles/$b-${w.lowercase().replace(' ', '-')}", "$w (${b})", "bubbles", ArtKind.Bubble, b, 1.3f, text = w, colorRole = ColorRole.Paper, tags = "speech bubble talk"))
    }

    private fun String.code() = if (this[0].isLetterOrDigit()) this else "x${this[0].code}"

    /** Sticker text with the trip filled in. */
    fun fill(text: String, info: TripInfo?): String = if (info == null) text else text
        .replace("{place}", info.place.ifEmpty { info.title })
        .replace("{title}", info.title)
        .replace("{dates}", info.dates)
}

/** Simplified flags: stripes, a cross, a disc. Enough to read at sticker size. */
object Flags {
    enum class Layout { H, V, Cross, Disc, Nordic, Saltire, Union, Stars, Diamond, Crescent, Star }
    data class Flag(val code: String, val name: String, val layout: Layout, val colors: List<Long>)

    val all = listOf(
        Flag("fr", "France", Layout.V, listOf(0xFF0055A4, 0xFFFFFFFF, 0xFFEF4135)),
        Flag("it", "Italy", Layout.V, listOf(0xFF009246, 0xFFFFFFFF, 0xFFCE2B37)),
        Flag("ie", "Ireland", Layout.V, listOf(0xFF169B62, 0xFFFFFFFF, 0xFFFF883E)),
        Flag("be", "Belgium", Layout.V, listOf(0xFF000000, 0xFFFAE042, 0xFFED2939)),
        Flag("ro", "Romania", Layout.V, listOf(0xFF002B7F, 0xFFFCD116, 0xFFCE1126)),
        Flag("ng", "Nigeria", Layout.V, listOf(0xFF008751, 0xFFFFFFFF, 0xFF008751)),
        Flag("pe", "Peru", Layout.V, listOf(0xFFD91023, 0xFFFFFFFF, 0xFFD91023)),
        Flag("de", "Germany", Layout.H, listOf(0xFF000000, 0xFFDD0000, 0xFFFFCE00)),
        Flag("nl", "Netherlands", Layout.H, listOf(0xFFAE1C28, 0xFFFFFFFF, 0xFF21468B)),
        Flag("at", "Austria", Layout.H, listOf(0xFFED2939, 0xFFFFFFFF, 0xFFED2939)),
        Flag("ru", "Russia", Layout.H, listOf(0xFFFFFFFF, 0xFF0039A6, 0xFFD52B1E)),
        Flag("hu", "Hungary", Layout.H, listOf(0xFFCD2A3E, 0xFFFFFFFF, 0xFF436F4D)),
        Flag("bg", "Bulgaria", Layout.H, listOf(0xFFFFFFFF, 0xFF00966E, 0xFFD62612)),
        Flag("ee", "Estonia", Layout.H, listOf(0xFF0072CE, 0xFF000000, 0xFFFFFFFF)),
        Flag("lt", "Lithuania", Layout.H, listOf(0xFFFDB913, 0xFF006A44, 0xFFC1272D)),
        Flag("pl", "Poland", Layout.H, listOf(0xFFFFFFFF, 0xFFDC143C)),
        Flag("ua", "Ukraine", Layout.H, listOf(0xFF0057B7, 0xFFFFD700)),
        Flag("id", "Indonesia", Layout.H, listOf(0xFFCE1126, 0xFFFFFFFF)),
        Flag("mc", "Monaco", Layout.H, listOf(0xFFCE1126, 0xFFFFFFFF)),
        Flag("co", "Colombia", Layout.H, listOf(0xFFFCD116, 0xFFFCD116, 0xFF003893, 0xFFCE1126)),
        Flag("th", "Thailand", Layout.H, listOf(0xFFA51931, 0xFFF4F5F8, 0xFF2D2A4A, 0xFF2D2A4A, 0xFFF4F5F8, 0xFFA51931)),
        Flag("ar", "Argentina", Layout.Disc, listOf(0xFF74ACDF, 0xFFFFFFFF, 0xFF74ACDF, 0xFFF6B40E)),
        Flag("in", "India", Layout.Disc, listOf(0xFFFF9933, 0xFFFFFFFF, 0xFF138808, 0xFF000080)),
        Flag("jp", "Japan", Layout.Disc, listOf(0xFFFFFFFF, 0xFFBC002D)),
        Flag("bd", "Bangladesh", Layout.Disc, listOf(0xFF006A4E, 0xFFF42A41)),
        Flag("kr", "South Korea", Layout.Disc, listOf(0xFFFFFFFF, 0xFFCD2E3A, 0xFF0047A0)),
        Flag("ch", "Switzerland", Layout.Cross, listOf(0xFFDA291C, 0xFFFFFFFF)),
        Flag("gb-eng", "England", Layout.Cross, listOf(0xFFFFFFFF, 0xFFCE1124)),
        Flag("ge", "Georgia", Layout.Cross, listOf(0xFFFFFFFF, 0xFFFF0000)),
        Flag("se", "Sweden", Layout.Nordic, listOf(0xFF006AA7, 0xFFFECC00)),
        Flag("dk", "Denmark", Layout.Nordic, listOf(0xFFC8102E, 0xFFFFFFFF)),
        Flag("no", "Norway", Layout.Nordic, listOf(0xFFBA0C2F, 0xFFFFFFFF, 0xFF00205B)),
        Flag("fi", "Finland", Layout.Nordic, listOf(0xFFFFFFFF, 0xFF002F6C)),
        Flag("is", "Iceland", Layout.Nordic, listOf(0xFF02529C, 0xFFFFFFFF, 0xFFDC1E35)),
        Flag("gb-sct", "Scotland", Layout.Saltire, listOf(0xFF005EB8, 0xFFFFFFFF)),
        Flag("jm", "Jamaica", Layout.Saltire, listOf(0xFF009B3A, 0xFFFED100, 0xFF000000)),
        Flag("gb", "United Kingdom", Layout.Union, listOf(0xFF012169, 0xFFFFFFFF, 0xFFC8102E)),
        Flag("au", "Australia", Layout.Union, listOf(0xFF012169, 0xFFFFFFFF, 0xFFE4002B)),
        Flag("nz", "New Zealand", Layout.Union, listOf(0xFF012169, 0xFFFFFFFF, 0xFFC8102E)),
        Flag("us", "United States", Layout.Stars, listOf(0xFFB22234, 0xFFFFFFFF, 0xFF3C3B6E)),
        Flag("my", "Malaysia", Layout.Stars, listOf(0xFFCC0001, 0xFFFFFFFF, 0xFF010066)),
        Flag("br", "Brazil", Layout.Diamond, listOf(0xFF009C3B, 0xFFFFDF00, 0xFF002776)),
        Flag("tr", "Türkiye", Layout.Crescent, listOf(0xFFE30A17, 0xFFFFFFFF)),
        Flag("tn", "Tunisia", Layout.Crescent, listOf(0xFFE70013, 0xFFFFFFFF)),
        Flag("vn", "Vietnam", Layout.Star, listOf(0xFFDA251D, 0xFFFFFF00)),
        Flag("ma", "Morocco", Layout.Star, listOf(0xFFC1272D, 0xFF006233)),
        Flag("gr", "Greece", Layout.H, listOf(0xFF0D5EAF, 0xFFFFFFFF, 0xFF0D5EAF, 0xFFFFFFFF, 0xFF0D5EAF, 0xFFFFFFFF, 0xFF0D5EAF, 0xFFFFFFFF, 0xFF0D5EAF)),
        Flag("es", "Spain", Layout.H, listOf(0xFFAA151B, 0xFFF1BF00, 0xFFF1BF00, 0xFFAA151B)),
        Flag("mx", "Mexico", Layout.V, listOf(0xFF006847, 0xFFFFFFFF, 0xFFCE1126)),
        Flag("pt", "Portugal", Layout.V, listOf(0xFF006600, 0xFFFF0000, 0xFFFF0000)),
        Flag("ca", "Canada", Layout.V, listOf(0xFFD80621, 0xFFFFFFFF, 0xFFD80621)),
        Flag("eg", "Egypt", Layout.H, listOf(0xFFCE1126, 0xFFFFFFFF, 0xFF000000)),
        Flag("ae", "UAE", Layout.H, listOf(0xFF00732F, 0xFFFFFFFF, 0xFF000000)),
        Flag("cl", "Chile", Layout.H, listOf(0xFFFFFFFF, 0xFFD52B1E)),
        Flag("cz", "Czechia", Layout.H, listOf(0xFFFFFFFF, 0xFFD7141A)),
        Flag("sg", "Singapore", Layout.H, listOf(0xFFEF3340, 0xFFFFFFFF)),
        Flag("lk", "Sri Lanka", Layout.V, listOf(0xFF00534E, 0xFFEB7400, 0xFFFFBE29, 0xFFFFBE29, 0xFFFFBE29)),
        Flag("np", "Nepal", Layout.Star, listOf(0xFFDC143C, 0xFFFFFFFF)),
        Flag("ph", "Philippines", Layout.H, listOf(0xFF0038A8, 0xFFCE1126)),
        Flag("za", "South Africa", Layout.H, listOf(0xFFE03C31, 0xFF007749, 0xFF001489)),
    )
}
