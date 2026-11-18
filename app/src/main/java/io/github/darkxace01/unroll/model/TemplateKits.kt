package io.github.darkxace01.unroll.model

import kotlin.math.ceil

/** How a kit sets its cover title: theme type, or a title sticker. */
sealed interface TitleSpec {
    data class Words(val font: FontRole = FontRole.Head, val size: Float = 140f, val bold: Boolean = false) : TitleSpec
    data class Sticker(val id: String, val text: String? = "{title}", val width: Float = 0.84f) : TitleSpec
}

/**
 * A style family: theme, photo frames, decorations, title treatment and captions. Crossed with every
 * [TemplateLayout] it suits, so 22 kits × 12 layouts give a couple of hundred finished templates, and
 * switching theme or format multiplies that again.
 */
data class Kit(
    val id: String,
    val name: String,
    val category: Category,
    val theme: Theme,
    val styles: List<PhotoStyle>,
    val title: TitleSpec,
    val deco: List<String> = emptyList(),
    val tape: String? = null,
    val caption: String? = null,
    /** Caption as stickers (film subtitles) instead of words. */
    val captionStickers: List<String> = emptyList(),
    val tilt: Float = 0f,
    val pattern: Pattern = Pattern.None,
    val route: Boolean = false,
    /** Layout ids this kit looks right in; null = all. */
    val layouts: Set<String>? = null,
    val tags: String = "",
)

class TemplateLayout(val id: String, val label: String, val sample: Int = 6, val build: KitScope.() -> Unit)

/** Helpers a layout uses to place things in the kit's style. */
class KitScope(val b: TemplateBuilder, val kit: Kit) {
    private var n = 0
    private var d = 0
    private var cap = 0
    val W get() = b.W
    val H get() = b.H
    val remaining get() = b.remaining
    val count get() = b.count
    var slides: Int
        get() = b.slides
        set(v) { b.slides = v }

    private val tilts = floatArrayOf(-1f, 0.6f, -0.4f, 1f, -0.8f, 0.3f)

    /** The next photo, in the kit's next frame style, fitted to that frame's natural shape inside the box. */
    fun photo(l: Float, t: Float, r: Float, bot: Float, rotation: Float? = null, anim: Anim? = Anim(AnimKind.Pop, 0.1f * (n % 3), 0.5f)): PhotoItem? {
        if (b.remaining <= 0) return null
        val style = kit.styles[n % kit.styles.size]
        val box = PhotoGeometry.fitInto(Box(l * W, t * H, r * W, bot * H), PhotoGeometry.naturalAspect(style.frame))
        val rot = rotation ?: (kit.tilt * tilts[n % tilts.size])
        val item = b.photo(box.left / W, box.top / H, box.right / W, box.bottom / H, rot, style, anim)
        if (item != null && kit.tape != null && n % 2 == 0) {
            b.sticker(kit.tape, item.frame.cx / W, (item.frame.top + item.frame.h * 0.02f) / H, item.frame.w * 0.42f, rot - 4f + (n % 3) * 4f)
        }
        n++
        return item
    }

    /** A decoration from the kit at a free spot. */
    fun deco(cx: Float, cy: Float, w: Float = 200f, rotation: Float = 0f) {
        if (kit.deco.isEmpty()) return
        b.sticker(kit.deco[d % kit.deco.size], cx, cy, w, rotation, anim = Anim(AnimKind.Pop, 0.5f, 0.5f))
        d++
    }

    /** The slide's caption ("Day 2", a film subtitle…). */
    fun caption(cx: Float, cy: Float, w: Float, day: Int, rotation: Float = -4f, align: TextAlign = TextAlign.Center) {
        if (kit.captionStickers.isNotEmpty()) {
            b.sticker(kit.captionStickers[cap++ % kit.captionStickers.size], cx, cy, w * W)
            return
        }
        val c = kit.caption ?: return
        b.text(c, cx, cy, w, 84f, FontRole.Hand, ColorRole.Ink, align, rotation = rotation, day = day, anim = Anim(AnimKind.Fade, 0.4f, 0.5f))
    }

    /** The cover's title and dates, centred on [cy]. */
    fun title(cx: Float = 0.5f, cy: Float = 0.42f, width: Float = 0.86f) {
        when (val t = kit.title) {
            is TitleSpec.Words -> {
                b.text("{title}", cx, cy, width, t.size, t.font, bold = t.bold, anim = Anim(AnimKind.Rise, 0.1f, 0.6f))
                b.text("{dates}", cx, cy + 0.1f, width * 0.9f, 38f, color = ColorRole.Muted, spacing = 0.15f)
            }
            is TitleSpec.Sticker -> {
                val def = Stickers.byId(t.id)
                val w = t.width * width * W
                b.sticker(t.id, cx, cy, w, text = t.text, color = if (def?.colorRole != null) ColorRole.Ink else null, anim = Anim(AnimKind.Pop, 0.1f, 0.6f))
                val h = w / (def?.aspect ?: 2f)
                b.text("{dates}", cx, cy + h / H / 2 + 0.05f, width * 0.9f, 38f, color = ColorRole.Muted, spacing = 0.15f)
            }
        }
    }

    /** Cover: a hero photo with the title under it. */
    fun heroCover() {
        photo(0.1f, 0.06f, 0.9f, 0.62f, rotation = kit.tilt * -0.5f)
        title(cy = 0.76f)
        deco(0.13f, 0.9f, 180f, -8f)
    }

    /** Cover: just the title, with decorations around it. */
    fun titleCover() {
        title()
        deco(0.2f, 0.16f, 220f, -10f)
        deco(0.8f, 0.8f, 240f, 8f)
    }

    fun slidesFor(n: Int, perSlide: Float, extra: Int) = b.slidesFor(n, perSlide, extra)

    fun finish() {
        b.background = Background(colorRole = ColorRole.Background, pattern = kit.pattern)
        if (kit.route) b.routeThroughPhotos(vehicle = Vehicle.Plane)
    }
}

object TemplateKits {

    /** Grids of tight cells: only for kits whose photos can take any shape (a polaroid can't be a tall strip). */
    private val tightGrids = setOf("triptych", "rows", "bento")

    val layouts = listOf(
        TemplateLayout("pairs", "Pairs") {
            slides = slidesFor(count - 1, 2f, 1)
            heroCover()
            var s = 1
            while (remaining > 0 && s < slides) {
                photo(s + 0.07f, 0.06f, s + 0.62f, 0.5f)
                caption(s + 0.8f, 0.28f, 0.34f, s)
                val last = s == slides - 1
                photo(s + 0.36f, 0.5f, s + if (last) 0.95f else 1.04f, 0.94f)
                deco(s + 0.2f, 0.76f, 170f, -6f)
                s++
            }
        },
        TemplateLayout("grid", "Grid", sample = 9) {
            slides = slidesFor(count - 1, 4f, 1)
            heroCover()
            for (s in 1 until slides) {
                val m = 0.06f; val g = 0.025f
                val mx = s + 0.5f
                photo(s + m, 0.05f, mx - g / 2, 0.475f, 0f); photo(mx + g / 2, 0.05f, s + 1 - m, 0.475f, 0f)
                photo(s + m, 0.475f + g, mx - g / 2, 0.88f, 0f); photo(mx + g / 2, 0.475f + g, s + 1 - m, 0.88f, 0f)
                caption(s + 0.5f, 0.94f, 0.6f, s, rotation = 0f)
            }
        },
        TemplateLayout("trio", "Big & two") {
            slides = slidesFor(count - 1, 3f, 1)
            heroCover()
            for (s in 1 until slides) {
                photo(s + 0.06f, 0.05f, s + 0.94f, 0.55f, 0f)
                photo(s + 0.06f, 0.58f, s + 0.485f, 0.95f)
                photo(s + 0.515f, 0.58f, s + 0.94f, 0.95f)
            }
        },
        TemplateLayout("solo", "One by one", sample = 5) {
            slides = slidesFor(count, 1f, 1)
            titleCover()
            for (s in 1 until slides) {
                photo(s + 0.1f, 0.07f, s + 0.9f, 0.78f)
                caption(s + 0.5f, 0.87f, 0.7f, s, rotation = -2f)
                if (s % 2 == 0) deco(s + 0.86f, 0.9f, 150f, 10f)
            }
        },
        TemplateLayout("seam", "Across the cuts") {
            slides = slidesFor(count, 2f, 1)
            titleCover()
            for (s in 1 until slides) {
                // One photo straddling the cut, so swiping reveals the rest of it.
                photo(s - 0.3f, 0.12f, s + 0.3f, 0.6f)
                photo(s + 0.26f, 0.6f, s + 0.74f, 0.94f)
                caption(s + 0.62f, 0.2f, 0.32f, s)
            }
        },
        TemplateLayout("scatter", "Scattered", sample = 9) {
            slides = slidesFor(count, 2.5f, 1)
            titleCover()
            val step = (slides - 1.1f) / count.coerceAtLeast(1)
            var i = 0
            while (remaining > 0) {
                val x = 0.85f + i * step
                val t = if (i % 2 == 0) 0.07f else 0.43f
                photo(x, t, x + 0.6f, t + 0.5f, rotation = listOf(-7f, 5f, -3f, 8f, -5f, 3f)[i % 6])
                if (i % 3 == 2) deco(x + 0.3f, if (t < 0.3f) 0.85f else 0.15f, 160f)
                i++
            }
        },
        TemplateLayout("strip", "Contact strip", sample = 7) {
            val frameW = 0.62f
            slides = ceil(0.4f + count * (frameW + 0.04f) + 0.3f).toInt().coerceIn(1, MAX_SLIDES)
            title(cy = 0.12f, cx = 0.5f, width = 0.5f)
            var x = 0.4f
            var k = 1
            while (remaining > 0) {
                photo(x, 0.33f, x + frameW, 0.78f, 0f, anim = Anim(AnimKind.SlideIn, 0f, 0.6f))
                caption(x + frameW / 2, 0.84f, frameW, k, rotation = 0f)
                x += frameW + 0.04f
                k++
            }
            deco(0.15f, 0.92f, 160f)
        },
        TemplateLayout("hero", "Panorama opener") {
            slides = 3 + ceil((count - 1).coerceAtLeast(0) / 2f).toInt()
            titleCover()
            // A wide photo across two whole slides.
            photo(1.05f, 0.1f, 2.95f, 0.7f, 0f, anim = Anim(AnimKind.Fade, 0f, 0.8f))
            caption(2.0f, 0.82f, 0.8f, 1, rotation = 0f)
            var s = 3
            while (remaining > 0 && s < slides) {
                photo(s + 0.06f, 0.06f, s + 0.94f, 0.48f)
                photo(s + 0.06f, 0.52f, s + 0.94f, 0.94f)
                s++
            }
        },
        TemplateLayout("stack", "Pile of prints") {
            slides = slidesFor(count - 1, 3f, 1)
            heroCover()
            for (s in 1 until slides) {
                photo(s + 0.08f, 0.06f, s + 0.64f, 0.48f, -6f)
                photo(s + 0.36f, 0.28f, s + 0.92f, 0.7f, 5f)
                photo(s + 0.1f, 0.52f, s + 0.66f, 0.94f, -2f)
                deco(s + 0.82f, 0.86f, 160f, 8f)
            }
        },
        TemplateLayout("column", "Tall & notes", sample = 5) {
            slides = slidesFor(count, 1f, 1)
            titleCover()
            for (s in 1 until slides) {
                photo(s + 0.06f, 0.06f, s + 0.6f, 0.94f, 0f)
                b.text("{nn}", s + 0.8f, 0.18f, 0.32f, 120f, FontRole.Head, ColorRole.Accent, bold = true, day = s)
                caption(s + 0.8f, 0.42f, 0.34f, s, rotation = 0f)
                deco(s + 0.8f, 0.72f, 190f, 6f)
            }
        },
        TemplateLayout("mosaic", "Seamless mosaic", sample = 8) {
            val laid = Layouts.auto((0 until count).map { LayoutPhoto("$it", b.photos.getOrNull(it)?.aspect ?: 1.33f) }, b.ratio, 40f, 20f)
            slides = laid.slides + 1
            titleCover()
            for (i in 0 until count) {
                val f = laid.frames["$i"] ?: continue
                photo(1f + f.left / W, f.top / H, 1f + f.right / W, f.bottom / H, 0f, anim = Anim(AnimKind.Fade, (i % 3) * 0.15f, 0.6f))
            }
        },
        TemplateLayout("triptych", "Three tall", sample = 7) {
            slides = slidesFor(count - 1, 3f, 1)
            heroCover()
            for (s in 1 until slides) {
                val w = (0.88f - 0.04f) / 3
                for (k in 0 until 3) photo(s + 0.06f + k * (w + 0.02f), 0.12f, s + 0.06f + k * (w + 0.02f) + w, 0.78f, 0f)
                caption(s + 0.5f, 0.88f, 0.6f, s, rotation = 0f)
            }
        },
        TemplateLayout("rows", "Three wide", sample = 7) {
            slides = slidesFor(count - 1, 3f, 1)
            heroCover()
            for (s in 1 until slides) {
                for (k in 0 until 3) photo(s + 0.07f, 0.05f + k * 0.3f, s + 0.93f, 0.05f + k * 0.3f + 0.28f, 0f)
                caption(s + 0.5f, 0.965f, 0.5f, s, rotation = 0f)
            }
        },
        TemplateLayout("inset", "Photo in photo") {
            slides = slidesFor(count - 1, 2f, 1)
            heroCover()
            for (s in 1 until slides) {
                // One photo fills the slide; a second, in the kit's frame, sits in its corner.
                b.photo(s.toFloat(), 0f, s + 1f, 1f, 0f, PhotoStyle(), Anim(AnimKind.Fade, 0f, 0.6f))
                val right = s % 2 == 1
                photo(if (right) s + 0.46f else s + 0.08f, 0.5f, if (right) s + 0.93f else s + 0.55f, 0.93f, if (right) 4f else -4f)
            }
        },
        TemplateLayout("bento", "Bento", sample = 9) {
            slides = slidesFor(count - 1, 4f, 1)
            heroCover()
            for (s in 1 until slides) {
                val m = 0.06f; val g = 0.02f
                photo(s + m, 0.05f, s + 0.6f, 0.62f, 0f)
                photo(s + 0.6f + g, 0.05f, s + 1 - m, 0.335f - g / 2, 0f)
                photo(s + 0.6f + g, 0.335f + g / 2, s + 1 - m, 0.62f, 0f)
                photo(s + m, 0.62f + g, s + 1 - m, 0.93f, 0f)
            }
        },
        TemplateLayout("diary", "Diary pages", sample = 5) {
            slides = slidesFor(count, 1f, 1)
            heroCover()
            val notes = listOf("labels/sticky-yellow", "labels/torn-note-pink", "labels/lined-white", "labels/sticky-blue")
            for (s in 1 until slides) {
                photo(s + 0.08f, 0.07f, s + 0.8f, 0.62f)
                val head = (kit.caption ?: "Day {day}").replace("{day}", "$s").replace("{nn}", "$s".padStart(2, '0'))
                b.sticker(notes[s % notes.size], s + 0.66f, 0.79f, 0.42f * W, if (s % 2 == 0) 4f else -3f, text = "$head\nwe were here")
                deco(s + 0.2f, 0.82f, 170f, -8f)
            }
        },
    )

    private val polaroid = PhotoStyle(frame = FrameKind.Polaroid, shadow = 14f)
    private fun preset(id: String, shadow: Float = 14f) = (FrameLibrary.byId(id) ?: FrameLibrary.all.first()).style(PhotoStyle(shadow = shadow))

    /** Kits with a lot of personality: still here, shown after the calmer ones. */
    val playful = setOf("party", "instant-pastel", "love-notes", "screen-time", "photo-dump", "stamps", "admit-one", "scribbles", "postcards", "notebook", "beach-days")

    val kits = listOf(
        Kit("polaroid-travel", "Polaroid travel", Category.Travel, Themes.Paper, listOf(polaroid), TitleSpec.Sticker("titles/serif-script-title"),
            listOf("travel/plane", "travel/pin", "scribbles/arrow", "travel/postmark", "travel/compass"), tape = "tape/masking", caption = "Day {day}", tilt = 4f, pattern = Pattern.Paper, route = true, tags = "trip holiday vacation"),
        Kit("instant-pastel", "Instant pastel", Category.Instant, Themes.Blush, listOf(preset("instax-pink"), preset("instax-white"), preset("instax-sky")), TitleSpec.Sticker("titles/script-line-title"),
            listOf("shine/heart-rose", "shine/sparkles-gold"), tape = "tape/hearts-pink", caption = "♡ {nn}", tilt = 5f, tags = "instax cute pastel"),
        Kit("film-noir", "Film noir", Category.Film, Themes.Noir, listOf(PhotoStyle(frame = FrameKind.Film)), TitleSpec.Sticker("titles/spaced-title"),
            captionStickers = (0 until 20).map { "subtitles/$it" }, layouts = setOf("strip", "solo", "pairs", "column", "seam", "hero", "triptych", "inset"), tags = "cinema movie subtitles dark"),
        Kit("contact-sheet", "Contact sheet", Category.Film, Themes.Sand, listOf(preset("negative")), TitleSpec.Sticker("titles/boxed-title"),
            listOf("scribbles/circle", "scribbles/check", "scribbles/star"), caption = "{nn}A", pattern = Pattern.Paper, layouts = setOf("strip", "grid", "solo", "trio", "pairs", "rows"), tags = "negative analog 35mm"),
        Kit("stamps", "Stamp collector", Category.Stationery, Themes.Retro, listOf(PhotoStyle(frame = FrameKind.Stamp, shadow = 8f)), TitleSpec.Sticker("titles/badge-circle-place", "{place}", 0.6f),
            listOf("stamps/postmark-round", "stamps/rubber-par-avion-oval", "stamps/palm", "travel/boarding"), caption = "{nn}c", tilt = 4f, pattern = Pattern.Paper, route = true, tags = "postage mail post"),
        Kit("torn-journal", "Torn journal", Category.Paper, Themes.Forest, listOf(PhotoStyle(frame = FrameKind.Torn, shadow = 16f), preset("deckle-cream")), TitleSpec.Words(FontRole.Hand, 150f),
            listOf("flowers/leaf-sprig", "flowers/daisy", "labels/kraft-tag-yellow", "scribbles/leaf"), tape = "tape/kraft", caption = "day {day}", tilt = 3f, pattern = Pattern.Kraft, tags = "collage paper nature"),
        Kit("clean-white", "Clean white", Category.Minimal, Themes.Paper.copy(name = "White", background = 0xFFFFFFFF), listOf(PhotoStyle()), TitleSpec.Words(FontRole.Head, 120f),
            caption = "{nn}", layouts = setOf("pairs", "grid", "trio", "solo", "seam", "strip", "hero", "column", "mosaic", "triptych", "rows", "bento", "inset"), tags = "minimal simple white"),
        Kit("soft-round", "Soft rounded", Category.Minimal, Themes.Lilac, listOf(PhotoStyle(cornerRadius = 44f)), TitleSpec.Sticker("titles/stack-title"),
            listOf("shine/sparkle-holo"), layouts = setOf("pairs", "grid", "trio", "solo", "seam", "hero", "column", "mosaic", "triptych", "bento", "inset"), tags = "rounded soft minimal"),
        Kit("scribbles", "Scribbles", Category.Scribbles, Themes.Paper.copy(name = "Notebook", background = 0xFFFFFFFF), listOf(preset("scribble"), PhotoStyle()), TitleSpec.Words(FontRole.Hand, 170f),
            listOf("scribbles/arrow", "scribbles/heart", "scribbles/sparkles", "scribbles/swirl", "scribbles/smile", "scribbles/star", "scribbles/underline"), caption = "{nn} ↓", tilt = 2f, pattern = Pattern.Grid, tags = "doodle hand drawn chalk"),
        Kit("screen-time", "Screen time", Category.Digital, Themes.Ocean, listOf(preset("phone-black"), preset("browser-light"), preset("player-light"), preset("message-blue"), preset("popup-light"), preset("camera-black")),
            TitleSpec.Sticker("titles/outline-title"), listOf("diecut/sparkle", "bubbles/round-hi!"),
            layouts = setOf("solo", "pairs", "column", "strip", "scatter", "diary"), tags = "phone screenshot app ui"),
        Kit("party", "Party time", Category.Celebrations, Themes.Sunset.copy(background = 0xFFEDE6DE), listOf(preset("instax-white"), preset("square-cream")), TitleSpec.Sticker("titles/script-line-title"),
            listOf("shine/star-gold", "shine/sparkles-gold"), tilt = 4f, tags = "birthday party celebrate"),
        Kit("wedding", "Wedding day", Category.Weddings, Themes.Sand, listOf(PhotoStyle(frame = FrameKind.Arch), preset("lace-white"), PhotoStyle(frame = FrameKind.Oval)), TitleSpec.Sticker("titles/laurel-title"),
            listOf("flowers/blossom", "flowers/leaf-sprig", "shine/heart-gold"), pattern = Pattern.Linen, layouts = setOf("solo", "pairs", "column", "trio", "hero", "seam", "inset", "triptych"), tags = "wedding engagement elegant"),
        Kit("out-there", "Out there", Category.Outdoor, Themes.Forest, listOf(preset("deckle-white")), TitleSpec.Sticker("titles/stack-title"),
            listOf("marker/mountain", "marker/pine", "marker/sun", "marker/compass", "diecut/mushroom"), caption = "day {day}", pattern = Pattern.Paper, route = true, tags = "hiking camping nature outdoors"),
        Kit("editorial", "Editorial", Category.Fashion, Themes.Mono, listOf(PhotoStyle()), TitleSpec.Sticker("titles/magazine-title"),
            listOf("phrases/lately", "labels/dymo-black"), caption = "{nn}", layouts = setOf("column", "solo", "trio", "hero", "grid", "mosaic", "inset", "triptych", "bento"), tags = "fashion magazine vogue outfit"),
        Kit("beach-days", "Beach days", Category.Travel, Themes.Ocean, listOf(preset("wide-white"), preset("polaroid-white")), TitleSpec.Sticker("titles/script-line-title"),
            listOf("diecut/shell", "diecut/palm"), tape = "tape/stripes-teal", caption = "day {day}", tilt = 4f, tags = "summer sea sand"),
        Kit("photo-dump", "Photo dump", Category.Recap, Themes.Citrus, listOf(preset("corners-white"), PhotoStyle(cornerRadius = 24f), preset("taped")), TitleSpec.Sticker("phrases/photo-dump", null, 0.9f),
            listOf("phrases/lately", "phrases/weekend", "phrases/recap"), tilt = 3f, tags = "dump recap month lately"),
        Kit("postcards", "Postcards home", Category.Stationery, Themes.Sunset, listOf(preset("postcard-white")), TitleSpec.Sticker("travel/hello", "Hello from {place}"),
            listOf("stamps/postmark-wavy", "stamps/sun", "tickets/stub-cream"), tilt = 3f, pattern = Pattern.Paper, route = true, tags = "postcard greetings"),
        Kit("notebook", "Notebook", Category.Stationery, Themes.Paper, listOf(preset("notebook-white")), TitleSpec.Words(FontRole.Hand, 160f),
            listOf("labels/sticky-yellow", "scrapbook/paperclip-red", "labels/typewriter-white", "scribbles/underline"), caption = "note {nn}", tilt = 2f, pattern = Pattern.Lines, tags = "school journal notes"),
        Kit("portraits", "Portraits", Category.Portraits, Themes.Blush, listOf(PhotoStyle(frame = FrameKind.Arch), PhotoStyle(frame = FrameKind.Oval)), TitleSpec.Sticker("titles/serif-script-title"),
            listOf("shine/sparkle-gold"), layouts = setOf("solo", "pairs", "column", "trio", "seam", "inset", "triptych"), tags = "portrait people face"),
        Kit("love-notes", "Love notes", Category.Love, Themes.Blush, listOf(PhotoStyle(frame = FrameKind.Heart, shadow = 10f), preset("polaroid-pink")), TitleSpec.Sticker("titles/script-line-title"),
            listOf("shine/heart-rose", "scribbles/heart"), caption = "xo", tilt = 4f, tags = "valentine couple love"),
        Kit("admit-one", "Admit one", Category.Bold, Themes.Retro, listOf(preset("ticket-cream"), preset("slide-white")), TitleSpec.Sticker("titles/boxed-title"),
            listOf("tickets/admit-cream", "stamps/rubber-approved-rect", "scribbles/star"), tilt = 3f, pattern = Pattern.Halftone, caption = "№ {nn}", tags = "retro vintage ticket"),
        Kit("grid-lines", "Grid lines", Category.Grids, Themes.Mono.copy(name = "Paper white", background = 0xFFF7F7F5), listOf(PhotoStyle()), TitleSpec.Words(FontRole.Head, 130f, bold = true),
            layouts = setOf("grid", "trio", "mosaic", "solo", "rows", "triptych", "bento"), caption = "{nn}", tags = "grid tidy clean"),
    )

    val generated: List<Template> by lazy {
        kits.flatMap { kit ->
            layouts.filter { (kit.layouts == null || it.id in kit.layouts) && (it.id !in tightGrids || kit.styles.all { s -> PhotoGeometry.naturalAspect(s.frame) == null }) }.map { lay ->
                Template("k-${kit.id}-${lay.id}", "${kit.name} · ${lay.label}", kit.category, kit.theme, sample = lay.sample, tags = "${kit.tags} ${lay.label}", kit = kit.id, layout = lay.id) {
                    val scope = KitScope(this, kit)
                    lay.build(scope)
                    scope.finish()
                }
            }
        }
    }
}
