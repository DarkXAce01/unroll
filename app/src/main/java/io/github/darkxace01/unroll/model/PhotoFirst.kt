package io.github.darkxace01.unroll.model

import kotlin.math.ceil

/**
 * Photo-first templates: the photos are the design. No big titles, no cartoon stickers; structure comes
 * from where the photos sit (torn seams, prints over a blurred copy, a measured grid), with small,
 * quiet type. Each one is shown in the gallery with one real photo shoot, so the preview looks like a
 * finished post rather than a layout.
 */
object PhotoFirst {

    /** One bundled photo shoot (samples tagged with [id]) and the words that suit it. */
    data class Shoot(
        val id: String,
        val label: String,
        val category: Category,
        /** Photos in the shoot, i.e. how many the preview shows. */
        val n: Int,
        /** A small caption in the shoot's voice ("slow days in the van"). */
        val caption: String,
        /** Where, for a location tag. */
        val place: String,
        /** A short handwritten line. */
        val script: String,
        /** Single words for word cells. */
        val words: List<String>,
        val tags: String,
        /** A deep colour from the photos, for a coloured backdrop. */
        val deep: Long,
        val designs: List<String>,
    )

    val shoots = listOf(
        Shoot("van_couple", "Van life", Category.Travel, 7, "slow days in the van", "Pacific coast", "nowhere to be",
            listOf("sunset", "us", "the van", "salt air"), "road trip van camper couple sunset travel summer", 0xFF2F4A57,
            listOf("torn", "blur", "words", "pano")),
        Shoot("van_solo", "Retro van", Category.Fashion, 5, "a blue van and a long weekend", "out of town", "weekend mode",
            listOf("chrome", "blue", "weekend"), "van retro vintage car fashion road trip", 0xFF26475A,
            listOf("editorial", "clip", "mono")),
        Shoot("city_couple", "City date", Category.Love, 6, "a whole day in the city", "downtown", "just us",
            listOf("you", "me", "the city", "later"), "couple city date love street", 0xFF5A3B33,
            listOf("prints", "words", "editorial")),
        Shoot("wedding_dance", "First dance", Category.Weddings, 3, "the first dance", "the old town", "forever starts here",
            listOf("vows", "us", "always"), "wedding dance bride groom city", 0xFF3B3A36,
            listOf("mono", "blur")),
        Shoot("winter_magic", "Winter tale", Category.Portraits, 8, "the first snow this year", "the woods", "snow day",
            listOf("cold", "quiet", "snow", "december"), "winter snow portrait christmas holidays cold", 0xFF27384F,
            listOf("blur", "clip", "torn")),
        Shoot("xmas_lights", "Christmas lights", Category.Celebrations, 5, "lights on every street", "the market", "merry everything",
            listOf("lights", "cocoa", "joy"), "christmas lights holidays winter market festive", 0xFF5B1A1A,
            listOf("prints", "clip", "words")),
        Shoot("snow_friends", "Snow day", Category.Celebrations, 5, "snow day with the girls", "the park", "best kind of cold",
            listOf("friends", "snow", "laughing"), "friends snow winter fun girls", 0xFF3D4B5C,
            listOf("prints", "torn", "pano")),
        Shoot("golden_beach", "Golden hour", Category.Outdoor, 7, "chasing the last light", "the coast", "golden hour",
            listOf("light", "sea", "slow", "warm"), "golden hour sunset beach coast summer", 0xFF6B4423,
            listOf("torn", "editorial", "blur", "pano")),
        Shoot("golden_field", "Summer field", Category.Portraits, 5, "long grass, longer evenings", "the meadow", "summer, slowly",
            listOf("field", "june", "light"), "summer field golden portrait meadow", 0xFF5E5130,
            listOf("clip", "words", "mono")),
        Shoot("pebble", "Grey coast", Category.Editorial, 6, "the sea in november", "the pebble beach", "breathe out",
            listOf("grey", "sea", "alone", "wind"), "beach sea moody grey coast calm solo autumn", 0xFF33393F,
            listOf("editorial", "mono", "torn", "blur")),
        Shoot("seychelles", "Island days", Category.Travel, 8, "island time", "La Digue", "out of office",
            listOf("turquoise", "granite", "salt"), "beach island tropical travel holiday summer sea", 0xFF1E5961,
            listOf("pano", "torn", "prints", "blur")),
        Shoot("wedding_studio", "The wedding", Category.Weddings, 8, "the day we said yes", "the hall", "just married",
            listOf("yes", "vows", "us", "forever"), "wedding bride groom dress ceremony", 0xFF4A4238,
            listOf("editorial", "words", "clip", "mono")),
        Shoot("couple_field", "Meadow vows", Category.Love, 7, "barefoot in the meadow", "the countryside", "you and me",
            listOf("you", "me", "always"), "couple love meadow golden wedding engagement", 0xFF5B4B2E,
            listOf("blur", "torn", "prints")),
        Shoot("couple_road", "Lakeside", Category.Love, 6, "the long way round", "the lake", "my favourite person",
            listOf("lake", "us", "road"), "couple love lake road engagement", 0xFF2D4C55,
            listOf("pano", "editorial", "words")),
        Shoot("beach_couple", "Shoreline", Category.Love, 5, "barefoot at dusk", "the shore", "sunset with you",
            listOf("dusk", "waves", "us"), "couple beach sunset love romantic", 0xFF4A3B52,
            listOf("blur", "mono", "clip")),
        Shoot("rome", "Rome", Category.Travel, 4, "three days in Rome", "Roma", "ciao, Roma",
            listOf("ciao", "pasta", "sun"), "rome italy city travel europe landmarks", 0xFF6B3A1E,
            listOf("words", "pano", "prints")),
        Shoot("rain_city", "Rainy city", Category.Fashion, 8, "a rainy afternoon in the old town", "old town", "rain looks good on you",
            listOf("rain", "black", "streets"), "rain city fashion street umbrella moody", 0xFF2A2A2C,
            listOf("mono", "editorial", "torn", "clip")),
        Shoot("autumn_hat", "Autumn", Category.Portraits, 8, "hello, autumn", "the old park", "cosy season",
            listOf("leaves", "october", "hat", "cosy"), "autumn fall portrait hat leaves cosy october", 0xFF5A3A1E,
            listOf("prints", "blur", "words", "torn")),
    )

    // Quiet palettes. Head = a magazine serif for small captions, Hand = a pen for the odd script line.
    private val White = Theme("Gallery white", 0xFFFFFFFF, 0xFFFFFFFF, 0xFF161616, 0xFF161616, 0xFF8E8E8E, FontKey.Magazine, FontKey.Sans, FontKey.Marker)
    private val Stone = Theme("Stone", 0xFFE6E4E0, 0xFFFFFFFF, 0xFF1E1E1E, 0xFF1E1E1E, 0xFF8A8782, FontKey.Magazine, FontKey.Sans, FontKey.Marker)
    private val Ink = Theme("Ink", 0xFF0C0C0C, 0xFFFFFFFF, 0xFFF2F2F2, 0xFFF2F2F2, 0xFF8A8A8A, FontKey.Magazine, FontKey.Sans, FontKey.Marker)
    private fun deep(sh: Shoot) = Theme("Deep", sh.deep, 0xFFFFFFFF, 0xFFF4EFE8, 0xFFF4EFE8, 0xFFC9BFB4, FontKey.Magazine, FontKey.Sans, FontKey.Marker)

    private val print = PhotoStyle(borderWidth = 14f, borderColor = 0xFFFFFFFF, shadow = 16f)
    private val bigPrint = PhotoStyle(borderWidth = 20f, borderColor = 0xFFFFFFFF, shadow = 22f)
    private val mono = Adjust(filter = Filter.Mono, contrast = 0.12f)
    private val fade = Anim(AnimKind.Fade, 0f, 0.7f)
    private val rise = Anim(AnimKind.Rise, 0.15f, 0.6f)

    private val names = mapOf(
        "torn" to "Torn edges", "blur" to "Soft frame", "editorial" to "Editorial", "prints" to "Loose prints",
        "words" to "Word grid", "mono" to "Black & white", "pano" to "Wide open", "clip" to "Clipped print",
    )
    private val designTags = mapOf(
        "torn" to "torn paper seamless rip full bleed", "blur" to "blur frame border clean simple", "editorial" to "editorial minimal clean white magazine",
        "prints" to "prints collage photos scattered", "words" to "grid words minimal", "mono" to "black and white monochrome noir",
        "pano" to "panorama wide seamless location", "clip" to "print clip simple colour",
    )

    /** Shoots whose photos are bundled (assets/samples, tagged with the shoot). The rest wait for theirs. */
    val ready = setOf("van_couple", "van_solo", "city_couple", "wedding_dance", "winter_magic")

    val templates: List<Template> by lazy {
        shoots.filter { it.id in ready }.flatMap { sh -> sh.designs.map { d -> make(sh, d) } }
    }

    private fun make(sh: Shoot, d: String): Template {
        val theme = when (d) {
            "editorial", "words" -> White
            "prints" -> Stone
            "mono" -> Ink
            "clip" -> deep(sh)
            else -> White
        }
        return Template(
            id = "pf-$d-${sh.id}", name = "${names[d]} · ${sh.label}", category = sh.category, theme = theme,
            sample = sh.n, tags = "${sh.tags} ${designTags[d]} ${sh.caption}", kit = "pf-$d", layout = d, shoot = sh.id,
        ) {
            when (d) {
                "torn" -> torn(sh)
                "blur" -> blur(sh)
                "editorial" -> editorial(sh)
                "prints" -> prints(sh)
                "words" -> words(sh)
                "mono" -> monochrome(sh)
                "pano" -> pano(sh)
                else -> clip(sh)
            }
        }
    }

    /** A small caption: magazine serif, spaced capitals. */
    private fun TemplateBuilder.caption(s: String, cx: Float, cy: Float, color: ColorRole = ColorRole.Ink, size: Float = 30f, w: Float = 0.8f, align: TextAlign = TextAlign.Center) =
        text(s.uppercase(), cx, cy, w, size, FontRole.Head, color, align, spacing = 0.28f, anim = rise)

    /** A tiny location tag in a soft pill. */
    private fun TemplateBuilder.place(s: String, cx: Float, cy: Float) =
        text("⌖ $s", cx, cy, 0.4f, 26f, FontRole.Body, ColorRole.Ink, pill = ColorRole.Paper, anim = rise)

    /**
     * Full-bleed photos that overlap a little, each torn along its left edge over the one before, so
     * the seams land mid-slide; every third photo is a white print laid over the last seam.
     */
    private fun TemplateBuilder.torn(sh: Shoot) {
        val step = 0.8f
        val full = count - count / 3
        slides = Math.round((full - 1) * step + 1f).coerceIn(1, MAX_SLIDES)
        var x = 0f
        var i = 0
        var seams = 0
        var placed = 0
        while (remaining > 0) {
            if (i % 3 == 2 && x > 0f) {
                // A print across the seam just made.
                val up = seams % 2 == 0
                val t = if (up) 0.1f else 0.52f
                photo(x - 0.22f, t, x + 0.22f, t + 0.36f, if (up) 2.5f else -2f, print, anim = Anim(AnimKind.Pop, 0.3f, 0.5f))
                seams++
            } else {
                val tear = if (x == 0f) 0 else Tear.LEFT
                // The last one runs to the end of the carousel, so no slide is left half empty.
                val right = if (placed == full - 1) maxOf(slides.toFloat(), x + 0.6f) else x + 1f
                photo(x, 0f, right, 1f, style = PhotoStyle(tear = tear), anim = fade)
                x += step
                placed++
            }
            i++
        }
        place(sh.place, 0.5f, 0.92f)
    }

    /** Each photo as a white-bordered print over a big, soft copy of itself, with a quiet caption. */
    private fun TemplateBuilder.blur(sh: Shoot) {
        slides = count.coerceAtLeast(1)
        var s = 0
        while (remaining > 0) {
            val p = photo(s + 0.13f, 0.1f, s + 0.87f, 0.8f, 0f, bigPrint, anim = Anim(AnimKind.Rise, 0.1f, 0.6f)) ?: break
            twin(p, s.toFloat(), 0f, s + 1f, 1f)
            val line = if (s == 0) sh.caption else sh.words[(s - 1) % sh.words.size]
            caption(line, s + 0.5f, 0.895f, ColorRole.Paper, 32f)
            s++
        }
    }

    /**
     * White space and measured placement: a full-bleed opener with a small caption in its middle, then
     * a large and a small photo per slide, mirrored slide to slide, numbered like a magazine.
     */
    private fun TemplateBuilder.editorial(sh: Shoot) {
        slides = 1 + ceil((count - 1) / 2f).toInt()
        photo(0f, 0f, 1f, 1f, anim = fade)
        caption(sh.caption, 0.5f, 0.5f, ColorRole.Paper, 34f)
        var s = 1
        while (remaining > 0) {
            val flip = s % 2 == 0
            if (remaining == 1) {
                // One left: give it the slide.
                photo(s + 0.1f, 0.1f, s + 0.9f, 0.84f, anim = fade)
                text("No. {nn}", s + 0.25f, 0.875f, 0.3f, 26f, FontRole.Body, ColorRole.Muted, TextAlign.Start, spacing = 0.2f, day = s)
            } else if (!flip) {
                photo(s + 0.08f, 0.06f, s + 0.8f, 0.6f, anim = fade)
                photo(s + 0.46f, 0.64f, s + 0.92f, 0.94f, anim = Anim(AnimKind.Fade, 0.2f, 0.6f))
                text("No. {nn}", s + 0.23f, 0.66f, 0.3f, 26f, FontRole.Body, ColorRole.Muted, TextAlign.Start, spacing = 0.2f, day = s)
            } else {
                photo(s + 0.08f, 0.06f, s + 0.54f, 0.38f, anim = Anim(AnimKind.Fade, 0.2f, 0.6f))
                photo(s + 0.2f, 0.42f, s + 0.92f, 0.94f, anim = fade)
                text("No. {nn}", s + 0.77f, 0.36f, 0.3f, 26f, FontRole.Body, ColorRole.Muted, TextAlign.End, spacing = 0.2f, day = s)
            }
            s++
        }
        caption(sh.place, (slides - 1) + 0.5f, 0.965f, ColorRole.Muted, 22f)
    }

    /** White prints on warm grey, overlapping and crossing the cuts, with one handwritten line. */
    private fun TemplateBuilder.prints(sh: Shoot) {
        slides = ceil(0.16f + (count - 1) * 0.4f + 0.5f).toInt().coerceIn(1, MAX_SLIDES)
        text(sh.script, 0.5f, 0.1f, 0.9f, 96f, FontRole.Hand, ColorRole.Ink, rotation = -3f, anim = Anim(AnimKind.Type, 0.1f, 1f))
        // A loose two-row rhythm: up, down, up... stepping 0.4 of a slide, so prints sit across the cuts.
        var k = 0
        while (remaining > 0) {
            val x = 0.08f + k * 0.4f
            if (x > slides - 0.3f) break
            val up = k % 2 == 0
            val t = if (up) 0.22f else 0.52f
            val rot = listOf(-3f, 2f, -1.5f, 3f, -2.5f)[k % 5]
            photo(x, t, x + 0.5f, t + 0.42f, rot, print, anim = Anim(AnimKind.Pop, 0.1f * (k % 3), 0.5f))
            k++
        }
    }

    /** A two-by-three grid per slide: four photos and two white cells holding one small word each. */
    private fun TemplateBuilder.words(sh: Shoot) {
        slides = ceil(count / 4f).toInt().coerceAtLeast(1)
        var w = 0
        for (s in 0 until slides) {
            // Which two of the six cells are words: alternates so the words zigzag across the carousel.
            val wordCells = if (s % 2 == 0) setOf(1, 4) else setOf(0, 5)
            for (cell in 0 until 6) {
                val col = cell % 2; val row = cell / 2
                val l = s + col * 0.5f; val t = row / 3f
                // Out of photos: the rest of the slide is words too, rather than blank cells.
                if (cell in wordCells || remaining == 0) {
                    val word = sh.words[w++ % sh.words.size]
                    text(word, l + 0.25f, t + 1 / 6f, 0.4f, 30f, FontRole.Head, ColorRole.Ink, anim = rise)
                } else if (remaining > 0) photo(l, t, l + 0.5f, t + 1 / 3f, anim = fade)
            }
        }
    }

    /** Black, white and grain: alternating a full-bleed photo and a framed one with a caption. */
    private fun TemplateBuilder.monochrome(sh: Shoot) {
        slides = count.coerceAtLeast(1)
        var s = 0
        while (remaining > 0) {
            val p = if (s % 2 == 0) photo(s.toFloat(), 0f, s + 1f, 1f, anim = fade)
            else photo(s + 0.12f, 0.1f, s + 0.88f, 0.8f, anim = fade)
            p?.let { items[items.indexOf(it)] = it.copy(adjust = mono) }
            if (s == 0) caption(sh.caption, 0.5f, 0.9f, ColorRole.Paper, 30f)
            else if (s % 2 == 1) caption(sh.words[(s / 2) % sh.words.size], s + 0.5f, 0.89f, ColorRole.Ink, 26f)
            s++
        }
    }

    /** The first photo opens across two slides with a location tag; the rest full-bleed, some with a print inside. */
    private fun TemplateBuilder.pano(sh: Shoot) {
        val rest = (count - 1).coerceAtLeast(0)
        slides = 2 + ceil(rest / 2f).toInt()
        photo(0f, 0f, 2f, 1f, anim = fade)
        place(sh.place, 0.5f, 0.92f)
        caption(sh.caption, 1.5f, 0.12f, ColorRole.Paper, 30f)
        var s = 2
        while (remaining > 0) {
            photo(s.toFloat(), 0f, s + 1f, 1f, anim = fade)
            val left = s % 2 == 0
            photo(if (left) s + 0.08f else s + 0.5f, 0.56f, if (left) s + 0.5f else s + 0.92f, 0.93f, if (left) -2f else 2f, print, anim = Anim(AnimKind.Pop, 0.3f, 0.5f))
            s++
        }
    }

    /** One print per slide, clipped, on a deep colour taken from the shoot; numbered underneath. */
    private fun TemplateBuilder.clip(sh: Shoot) {
        slides = count.coerceAtLeast(1)
        var s = 0
        while (remaining > 0) {
            val rot = if (s % 2 == 0) -1.5f else 1.5f
            photo(s + 0.16f, 0.12f, s + 0.84f, 0.8f, rot, bigPrint, anim = Anim(AnimKind.Rise, 0.1f, 0.6f)) ?: break
            sticker("scrapbook/binder-black", s + 0.5f, 0.1f, 110f)
            if (s == 0) caption(sh.caption, 0.5f, 0.9f, ColorRole.Ink, 28f)
            else text("No. {nn}", s + 0.5f, 0.9f, 0.4f, 24f, FontRole.Body, ColorRole.Muted, spacing = 0.2f, day = s + 1)
            s++
        }
    }
}
