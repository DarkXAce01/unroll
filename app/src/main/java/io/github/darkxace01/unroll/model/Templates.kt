package io.github.darkxace01.unroll.model

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Words the templates fill in: from the photos (dates, count) or typed by the person. */
data class TripInfo(
    val title: String = "Our trip",
    val dates: String = "",
    val place: String = "",
    val days: Int = 0,
)

enum class Category(val label: String) {
    Travel("Travel"), Scrapbook("Scrapbook"), Recap("Photo dump"), Instant("Instant"), Film("Film"), Paper("Paper collage"),
    Minimal("Clean"), Grids("Grids"), Stationery("Stationery"), Scribbles("Scribbles"), Digital("Digital"), Editorial("Editorial"),
    Bold("Bold"), Celebrations("Celebrations"), Love("Love"), Weddings("Weddings"), Portraits("Portraits"), Outdoor("Outdoor"),
    Fashion("Fashion"), Story("Stories"),
}

/**
 * A template is a recipe, not a fixed picture: given any number of photos it builds a whole carousel, so
 * it stretches from 3 photos to 40. Everything it makes is ordinary items (photos, text, shapes, routes)
 * tagged with theme roles, so afterwards every part can be moved, restyled or deleted, and switching the
 * theme recolours it all.
 */
class Template(
    val id: String,
    val name: String,
    val category: Category,
    val theme: Theme,
    val ratio: Ratio = Ratio.Portrait,
    /** How many photos to show in the gallery preview. */
    val sample: Int = 6,
    /** Extra words for search. */
    val tags: String = "",
    /** The style family (kits) and layout it was made from, for "similar templates". */
    val kit: String? = null,
    val layout: String? = null,
    val build: TemplateBuilder.() -> Unit,
) {
    fun matches(q: String): Boolean {
        val hay = "$name ${category.label} $tags ${theme.name}".lowercase()
        return q.lowercase().split(' ').filter { it.isNotBlank() }.all { it in hay }
    }
}

/**
 * Coordinates in templates: x in slide widths from the canvas's left (1.5 = middle of slide 2), y as a
 * fraction of the height. That keeps every template correct at any shape.
 */
class TemplateBuilder(
    val ratio: Ratio,
    val theme: Theme,
    val photos: List<NewPhoto>,
    val info: TripInfo,
    private val newId: () -> String,
    sample: Int,
) {
    val W = ratio.slideW.toFloat()
    val H = ratio.slideH.toFloat()
    /** Photos to lay out: the real ones, or [sample] empty slots for a preview / an empty start. */
    val count: Int = if (photos.isEmpty()) sample else photos.size
    var slides = 1
        set(v) { field = v.coerceIn(1, MAX_SLIDES) }
    var background = Background(colorRole = ColorRole.Background)
    val items = mutableListOf<Item>()
    private var next = 0

    val remaining get() = count - next

    fun id() = newId()

    /** The aspect of the next photo (1.33 for an empty slot), to pick a layout that suits it. */
    fun nextAspect(): Float = photos.getOrNull(next)?.aspect ?: 1.33f

    fun fill(s: String, day: Int = 0) = s
        .replace("{title}", info.title)
        .replace("{dates}", info.dates)
        .replace("{place}", info.place.ifEmpty { info.title })
        .replace("{count}", count.toString())
        .replace("{days}", max(info.days, 1).toString())
        .replace("{day}", day.toString())
        .replace("{nn}", day.toString().padStart(2, '0'))

    /** Places the next photo (or an empty slot) in the box [l]..[r] (slide widths) × [t]..[b] (height). */
    fun photo(l: Float, t: Float, r: Float, b: Float, rotation: Float = 0f, style: PhotoStyle = PhotoStyle(), anim: Anim? = null): PhotoItem? {
        if (next >= count) return null
        // Past the last slide (the 20-slide limit): the rest of the photos don't fit.
        if (l >= slides - 0.05f) { next = count; return null }
        val frame = Frame((l + r) / 2 * W, (t + b) / 2 * H, (r - l) * W, (b - t) * H, rotation)
        val ph = photos.getOrNull(next)
        next++
        val item = if (ph == null) PhotoItem(newId(), frame, "", frame.w.toInt().coerceAtLeast(1), frame.h.toInt().coerceAtLeast(1), style = style, anim = anim)
        else PhotoItem(newId(), frame, ph.uri, ph.w, ph.h, style = style, faces = ph.faces, takenAt = ph.takenAt, lat = ph.lat, lng = ph.lng, anim = anim)
        items += item
        return item
    }

    fun text(
        s: String, cx: Float, cy: Float, w: Float, size: Float,
        font: FontRole = FontRole.Body, color: ColorRole = ColorRole.Ink, align: TextAlign = TextAlign.Center,
        bold: Boolean = false, spacing: Float = 0f, pill: ColorRole? = null, rotation: Float = 0f, day: Int = 0,
        lineHeight: Float = 1.1f, anim: Anim? = null,
    ): TextItem {
        val str = fill(s, day)
        val lines = str.count { it == '\n' } + 1
        val pad = if (pill != null) size * 0.45f else size * 0.1f
        val h = size * lineHeight * lines + pad * 2
        val t = TextItem(
            newId(), Frame(cx * W, cy * H, w * W, h, rotation), str, size = size, align = align, bold = bold, letterSpacing = spacing,
            lineHeight = lineHeight, colorRole = color, fontRole = font, pillRole = pill, anim = anim,
        )
        items += t
        return t
    }

    fun shape(kind: ShapeKind, cx: Float, cy: Float, w: Float, h: Float, color: ColorRole = ColorRole.Accent, rotation: Float = 0f, stroke: Float = 0f, anim: Anim? = null): ShapeItem {
        val s = ShapeItem(newId(), Frame(cx * W, cy * H, w, h, rotation), kind, stroke = stroke, colorRole = color, anim = anim)
        items += s
        return s
    }

    /** A sticker [w] canvas px wide (height from its shape), centred at ([cx] slides, [cy] of the height). */
    fun sticker(id: String, cx: Float, cy: Float, w: Float, rotation: Float = 0f, text: String? = null, color: ColorRole? = null, anim: Anim? = null): StickerItem? {
        val def = Stickers.byId(id) ?: return null
        val s = StickerItem(newId(), Frame(cx * W, cy * H, w, w / def.aspect, rotation), id, text = text?.let { fill(it) } ?: def.text?.let { fill(it) }, colorRole = color, anim = anim)
        items += s
        return s
    }

    fun route(points: List<Pair<Float, Float>>, dash: Dash = Dash.Dashed, width: Float = 7f, color: ColorRole = ColorRole.Ink, start: Cap = Cap.Dot, end: Cap = Cap.Pin, stops: Boolean = false, vehicle: Vehicle = Vehicle.None, under: Boolean = true, anim: Anim? = Anim(AnimKind.Draw, 0f, 1.6f)): RouteItem {
        val r = RouteItem(newId(), points.map { (x, y) -> Point(x * W, y * H) }, width = width, colorRole = color, dash = dash, start = start, end = end, stops = stops, vehicle = vehicle, anim = anim)
        if (under) items.add(0, r) else items += r
        return r
    }

    /** A route weaving through every photo placed so far, under them. */
    fun routeThroughPhotos(dash: Dash = Dash.Dashed, width: Float = 7f, color: ColorRole = ColorRole.Ink, vehicle: Vehicle = Vehicle.Plane, stops: Boolean = true): RouteItem? {
        val boxes = items.filterIsInstance<PhotoItem>().map { it.frame.bounds() }
        if (boxes.isEmpty()) return null
        val pts = RouteMath.through(boxes, H)
        val r = RouteItem(newId(), pts, width = width, colorRole = color, dash = dash, stops = stops, vehicle = vehicle, start = Cap.Ring, end = Cap.Pin, anim = Anim(AnimKind.Draw, 0f, 1.6f))
        items.add(0, r)
        return r
    }

    /** Slides needed for [n] items at [perSlide] each, after [extra] fixed slides. */
    fun slidesFor(n: Int, perSlide: Float, extra: Int = 0) = (extra + ceil(n / perSlide).toInt()).coerceIn(1, MAX_SLIDES)

    fun project(id: String, name: String, templateId: String, now: Long) = Project(
        id = id, name = name, ratio = ratio, slides = slides, background = background,
        // Anything a template put past the last slide (captions of photos that didn't fit) goes.
        items = items.filter { it.frame.bounds().left < slides * W - 1f },
        created = now, updated = now, theme = theme, template = templateId,
    )
}

object Templates {

    fun build(t: Template, photos: List<NewPhoto>, info: TripInfo, id: String, name: String, now: Long, newId: () -> String, theme: Theme = t.theme, ratio: Ratio? = null): Project {
        val b = TemplateBuilder(ratio ?: t.ratio, theme, photos, info, newId, t.sample)
        t.build(b)
        return b.project(id, name, t.id, now)
    }

    fun byId(id: String?) = everything.firstOrNull { it.id == id }

    /** The hand-made templates plus every kit × layout. */
    val everything: List<Template> by lazy { all + TemplateKits.generated }

    private val rise = Anim(AnimKind.Rise, 0.1f, 0.6f)
    private val pop = Anim(AnimKind.Pop, 0.25f, 0.5f)
    private val fade = Anim(AnimKind.Fade, 0f, 0.6f)
    private val type = Anim(AnimKind.Type, 0.1f, 1.0f)
    private fun later(a: Anim, by: Float) = a.copy(delay = a.delay + by)

    private val polaroid = PhotoStyle(frame = FrameKind.Polaroid, shadow = 14f)

    val all: List<Template> = listOf(

        Template("postcard-route", "Postcard route", Category.Travel, Themes.Paper) {
            val bodySlides = ceil(count / 2f).toInt()
            slides = 1 + bodySlides + 1
            text("{title}", 0.5f, 0.17f, 0.86f, 150f, FontRole.Head, anim = rise)
            text("{dates}", 0.5f, 0.3f, 0.8f, 40f, color = ColorRole.Muted, spacing = 0.12f, anim = later(rise, 0.2f))
            photo(0.16f, 0.4f, 0.84f, 0.93f, -3f, polaroid, anim = pop)
            var s = 1
            while (remaining > 0 && s < slides - 1) {
                photo(s + 0.06f, 0.08f, s + 0.56f, 0.5f, 3f, polaroid, anim = pop)
                text("Day {day}", s + 0.75f, 0.2f, 0.4f, 74f, FontRole.Hand, rotation = -6f, day = s, anim = later(fade, 0.4f))
                if (remaining > 0) photo(s + 0.42f, 0.5f, s + 0.95f, 0.94f, -4f, polaroid, anim = later(pop, 0.2f))
                s++
            }
            text("Until next time", (slides - 1) + 0.5f, 0.45f, 0.85f, 110f, FontRole.Hand, anim = type)
            shape(ShapeKind.Heart, (slides - 1) + 0.5f, 0.6f, 90f, 80f, anim = later(pop, 0.8f))
            routeThroughPhotos(vehicle = Vehicle.Plane)
        },

        Template("film-roll", "Film roll", Category.Bold, Themes.Noir, sample = 7) {
            // One continuous strip of film running across every slide.
            val frameW = 0.66f
            val total = 0.34f + count * frameW + 0.34f
            slides = ceil(total).toInt()
            text("{title}", 0.5f, 0.12f, 0.9f, 104f, FontRole.Head, color = ColorRole.Ink, bold = true, anim = rise)
            text("{dates}", 0.5f, 0.2f, 0.9f, 34f, color = ColorRole.Muted, spacing = 0.2f)
            var x = 0.34f
            var n = 1
            while (remaining > 0) {
                photo(x, 0.3f, x + frameW, 0.78f, 0f, PhotoStyle(frame = FrameKind.Film), anim = Anim(AnimKind.SlideIn, 0f, 0.7f))
                text("{nn} ▸", x + 0.1f, 0.83f, 0.25f, 30f, FontRole.Body, ColorRole.Accent, TextAlign.Start, day = n)
                x += frameW
                n++
            }
        },

        Template("scrapbook", "Scrapbook", Category.Scrapbook, Themes.Sand) {
            slides = slidesFor(count, 2f, 1)
            text("{title}", 0.5f, 0.3f, 0.85f, 130f, FontRole.Head, anim = rise)
            text("{dates}", 0.5f, 0.42f, 0.8f, 46f, FontRole.Hand, ColorRole.Accent)
            shape(ShapeKind.Sun, 0.5f, 0.66f, 200f, 200f, ColorRole.Accent, anim = Anim(AnimKind.Spin, 0.2f, 0.8f))
            shape(ShapeKind.Wave, 0.5f, 0.82f, 520f, 60f, ColorRole.Muted)
            var s = 1
            while (remaining > 0) {
                val rot = if (s % 2 == 0) 4f else -3f
                val style = PhotoStyle(borderWidth = 16f, borderRole = ColorRole.Paper, shadow = 10f)
                photo(s + 0.07f, 0.07f, s + 0.66f, 0.52f, rot, style, anim = pop)?.let {
                    shape(ShapeKind.Tape, s + 0.36f, 0.075f, 220f, 60f, ColorRole.Paper, rotation = -rot * 2)
                }
                text("Day {day}", s + 0.8f, 0.32f, 0.36f, 80f, FontRole.Hand, rotation = 8f, day = s)
                // The second photo leans over the cut into the next slide.
                photo(s + 0.38f, 0.5f, s + 1.04f, 0.93f, -rot, style, anim = later(pop, 0.2f))?.let {
                    shape(ShapeKind.Tape, s + 1.0f, 0.53f, 200f, 56f, ColorRole.Paper, rotation = 40f)
                }
                text("little moments", s + 0.22f, 0.7f, 0.36f, 54f, FontRole.Hand, ColorRole.Muted, rotation = -5f)
                s++
            }
        },

        Template("gallery", "Gallery", Category.Minimal, Themes.Paper.copy(name = "Gallery", background = 0xFFFFFFFF)) {
            slides = slidesFor(count, 1f, 1)
            text("{title}", 0.5f, 0.45f, 0.8f, 96f, FontRole.Head, anim = fade)
            shape(ShapeKind.Line, 0.5f, 0.53f, 120f, 6f, ColorRole.Ink)
            text("{dates}", 0.5f, 0.59f, 0.8f, 32f, color = ColorRole.Muted, spacing = 0.15f)
            var s = 1
            while (remaining > 0) {
                val a = nextAspect()
                val maxW = 0.8f; val maxH = 0.7f * H / W
                val (w, h) = PhotoGeometry.fit(a, maxW, maxH)
                val top = 0.44f - h * W / H / 2
                photo(s + 0.5f - w / 2, top, s + 0.5f + w / 2, top + h * W / H, anim = fade)
                text("{nn} / ${count.toString().padStart(2, '0')}", s + 0.5f, 0.9f, 0.5f, 30f, color = ColorRole.Muted, spacing = 0.2f, day = s)
                s++
            }
        },

        Template("magazine", "Magazine", Category.Editorial, Themes.Paper) {
            slides = slidesFor(count - 1, 2f, 1)
            photo(0f, 0f, 1f, 1f, anim = fade)
            text("{title}", 0.5f, 0.12f, 0.95f, 170f, FontRole.Head, ColorRole.Paper, anim = Anim(AnimKind.Drop, 0.1f, 0.7f))
            text("THE TRAVEL ISSUE · {dates}", 0.5f, 0.22f, 0.9f, 30f, color = ColorRole.Paper, spacing = 0.2f)
            text("Swipe →", 0.88f, 0.94f, 0.3f, 34f, FontRole.Body, ColorRole.Paper, TextAlign.End)
            var s = 1
            while (remaining > 0) {
                photo(s + 0f, 0.06f, s + 0.62f, 0.66f, anim = fade)
                text("Day\n{nn}", s + 0.81f, 0.18f, 0.32f, 96f, FontRole.Head, align = TextAlign.Start, day = s, lineHeight = 0.95f, anim = rise)
                text("“The best views come after the hardest climbs.”", s + 0.8f, 0.48f, 0.34f, 38f, FontRole.Head, ColorRole.Accent, TextAlign.Start)
                photo(s + 0.38f, 0.7f, s + 0.96f, 0.95f, anim = later(fade, 0.3f))
                text("page {nn}", s + 0.17f, 0.88f, 0.3f, 26f, color = ColorRole.Muted, align = TextAlign.Start, day = s + 1)
                s++
            }
        },

        Template("panorama-title", "Seamless + title", Category.Travel, Themes.Midnight, sample = 8) {
            val laid = Layouts.auto((0 until count).map { LayoutPhoto("$it", photos.getOrNull(it)?.aspect ?: 1.33f) }, ratio, 40f, 20f)
            slides = laid.slides + 1
            text("{title}", 0.5f, 0.38f, 0.86f, 140f, FontRole.Head, anim = rise)
            text("{dates}", 0.5f, 0.5f, 0.8f, 40f, color = ColorRole.Accent, spacing = 0.15f)
            text("swipe", 0.5f, 0.85f, 0.4f, 34f, color = ColorRole.Muted, spacing = 0.3f, anim = Anim(AnimKind.Float, 0.5f, 0.8f))
            for (i in 0 until count) {
                val f = laid.frames["$i"] ?: continue
                photo(1f + f.left / W, f.top / H, 1f + f.right / W, f.bottom / H, style = PhotoStyle(cornerRadius = 18f), anim = later(fade, (i % 3) * 0.15f))
            }
            route(listOf(0.3f to 0.7f, 0.62f to 0.66f, 0.98f to 0.72f), Dash.Dotted, 8f, ColorRole.Accent, Cap.Ring, Cap.Arrow, under = false)
        },

        Template("day-by-day", "Day by day", Category.Travel, Themes.Ocean) {
            slides = slidesFor(count, 2f, 1)
            text("{title}", 0.5f, 0.35f, 0.86f, 140f, FontRole.Head, bold = true, anim = rise)
            text("{count} photos · {days} days", 0.5f, 0.47f, 0.8f, 40f, color = ColorRole.Accent)
            shape(ShapeKind.Compass, 0.5f, 0.7f, 200f, 200f, ColorRole.Ink, anim = Anim(AnimKind.Spin, 0.3f, 1f))
            var s = 1
            while (remaining > 0) {
                text("Day {day}", s + 0.08f, 0.08f, 0.6f, 84f, FontRole.Head, align = TextAlign.Start, bold = true, day = s, anim = rise)
                photo(s + 0.07f, 0.16f, s + 0.93f, 0.56f, style = PhotoStyle(cornerRadius = 28f), anim = later(fade, 0.1f))
                photo(s + 0.07f, 0.59f, s + 0.93f, 0.95f, style = PhotoStyle(cornerRadius = 28f), anim = later(fade, 0.3f))
                s++
            }
        },

        Template("burst", "Collage burst", Category.Scrapbook, Themes.Blush, sample = 9) {
            slides = slidesFor(count, 2.5f, 1)
            text("{title}", 0.5f, 0.4f, 0.86f, 150f, FontRole.Head, anim = rise)
            text("{dates}", 0.5f, 0.52f, 0.8f, 40f, color = ColorRole.Muted)
            shape(ShapeKind.Sparkle, 0.2f, 0.25f, 90f, 90f, anim = pop)
            shape(ShapeKind.Sparkle, 0.82f, 0.68f, 60f, 60f, anim = later(pop, 0.2f))
            // Photos tumble along the strip, overlapping each other and the cuts.
            val rots = floatArrayOf(-7f, 5f, -3f, 8f, -5f, 3f)
            var i = 0
            val step = (slides - 1.1f) / count.coerceAtLeast(1)
            while (remaining > 0) {
                val x = 0.85f + i * step
                val up = i % 2 == 0
                val w = 0.62f
                val t = if (up) 0.08f else 0.42f
                photo(x, t, x + w, t + 0.5f, rots[i % rots.size], PhotoStyle(borderWidth = 14f, borderRole = ColorRole.Paper, shadow = 18f), anim = pop)
                i++
            }
        },

        Template("stamps", "Stamp collection", Category.Scrapbook, Themes.Retro) {
            slides = slidesFor(count, 2f, 1)
            text("Greetings\nfrom", 0.5f, 0.22f, 0.8f, 90f, FontRole.Body, ColorRole.Accent, lineHeight = 0.95f)
            text("{place}", 0.5f, 0.42f, 0.9f, 150f, FontRole.Head, bold = true, anim = pop)
            text("{dates}", 0.5f, 0.55f, 0.8f, 40f, color = ColorRole.Muted)
            shape(ShapeKind.Compass, 0.5f, 0.75f, 180f, 180f, ColorRole.Ink)
            var s = 1
            while (remaining > 0) {
                val stamp = PhotoStyle(frame = FrameKind.Stamp, shadow = 8f)
                photo(s + 0.1f, 0.08f, s + 0.62f, 0.5f, -4f, stamp, anim = pop)
                photo(s + 0.42f, 0.5f, s + 0.94f, 0.92f, 3f, stamp, anim = later(pop, 0.2f))
                shape(ShapeKind.Circle, s + 0.78f, 0.25f, 170f, 170f, ColorRole.Accent, stroke = 6f)
                text("{nn}c", s + 0.78f, 0.25f, 0.2f, 48f, FontRole.Head, ColorRole.Accent, day = s * 10)
                s++
            }
            routeThroughPhotos(Dash.Dotted, 8f, ColorRole.Accent, Vehicle.Plane)
        },

        Template("boarding-pass", "Boarding pass", Category.Travel, Themes.Sunset) {
            slides = slidesFor(count, 2f, 1)
            shape(ShapeKind.Ticket, 0.5f, 0.5f, 0.9f * W, 0.42f * H, ColorRole.Paper, anim = Anim(AnimKind.Rise, 0f, 0.6f))
            text("BOARDING PASS", 0.38f, 0.33f, 0.6f, 30f, FontRole.Body, ColorRole.Muted, TextAlign.Start, spacing = 0.25f)
            text("HOME", 0.18f, 0.43f, 0.3f, 80f, FontRole.Head, bold = true)
            text("{place}", 0.6f, 0.43f, 0.4f, 80f, FontRole.Head, ColorRole.Accent, bold = true)
            shape(ShapeKind.Plane, 0.39f, 0.43f, 90f, 70f, ColorRole.Ink)
            text("DATE  {dates}\nSEAT  7F · GATE A1", 0.36f, 0.6f, 0.55f, 32f, FontRole.Body, align = TextAlign.Start, spacing = 0.1f)
            text("{title}", 0.5f, 0.15f, 0.9f, 96f, FontRole.Head, anim = rise)
            var s = 1
            while (remaining > 0) {
                photo(s + 0.06f, 0.1f, s + 0.6f, 0.52f, 2f, polaroid, anim = pop)
                photo(s + 0.42f, 0.5f, s + 0.95f, 0.92f, -3f, polaroid, anim = later(pop, 0.2f))
                s++
            }
            route(listOf(0.6f to 0.7f, 0.95f to 0.88f, 1.2f to 0.6f), Dash.Dashed, 7f, ColorRole.Ink, Cap.Dot, Cap.None, vehicle = Vehicle.Plane, under = false)
        },

        Template("recap", "Big numbers", Category.Bold, Themes.Citrus, sample = 8) {
            slides = slidesFor(count, 3f, 1)
            text("{count}", 0.5f, 0.3f, 0.9f, 360f, FontRole.Head, ColorRole.Accent, bold = true, anim = pop)
            text("photos", 0.5f, 0.47f, 0.8f, 60f, FontRole.Head)
            text("{days} days · {dates}", 0.5f, 0.58f, 0.86f, 40f, color = ColorRole.Muted)
            text("{title}", 0.5f, 0.82f, 0.86f, 70f, FontRole.Head, bold = true, pill = ColorRole.Paper, anim = rise)
            var s = 1
            while (remaining > 0) {
                photo(s + 0.05f, 0.05f, s + 0.95f, 0.5f, style = PhotoStyle(cornerRadius = 40f), anim = fade)
                photo(s + 0.05f, 0.53f, s + 0.48f, 0.95f, style = PhotoStyle(cornerRadius = 40f), anim = later(fade, 0.15f))
                photo(s + 0.52f, 0.53f, s + 0.95f, 0.95f, style = PhotoStyle(cornerRadius = 40f), anim = later(fade, 0.3f))
                s++
            }
        },

        Template("arches", "Arches", Category.Editorial, Themes.Sand) {
            slides = slidesFor(count, 2f, 1)
            photo(0.18f, 0.12f, 0.82f, 0.7f, style = PhotoStyle(frame = FrameKind.Arch), anim = Anim(AnimKind.Rise, 0f, 0.8f))
            text("{title}", 0.5f, 0.79f, 0.9f, 120f, FontRole.Head, anim = later(rise, 0.3f))
            text("{dates}", 0.5f, 0.89f, 0.8f, 36f, color = ColorRole.Muted, spacing = 0.15f)
            var s = 1
            while (remaining > 0) {
                photo(s + 0.07f, 0.1f, s + 0.5f, 0.68f, style = PhotoStyle(frame = FrameKind.Arch), anim = rise)
                photo(s + 0.55f, 0.3f, s + 0.98f, 0.88f, style = PhotoStyle(frame = FrameKind.Arch), anim = later(rise, 0.2f))
                text("chapter {day}", s + 0.28f, 0.77f, 0.4f, 46f, FontRole.Head, ColorRole.Accent, day = s)
                shape(ShapeKind.Sun, s + 0.78f, 0.15f, 120f, 120f, ColorRole.Accent)
                s++
            }
        },

        Template("bold-type", "Bold type", Category.Bold, Themes.Noir, sample = 6) {
            slides = slidesFor(count, 1.5f, 0)
            // One giant word running behind every slide.
            val word = fill("{title}").uppercase()
            items += TextItem(id(), Frame(slides * W / 2, H * 0.5f, slides * W * 0.98f, H * 0.36f), word, size = H * 0.3f, colorRole = ColorRole.Accent, fontRole = FontRole.Head, bold = true, lineHeight = 0.9f, anim = fade)
            // Photos above and below the word, never over its middle, so it stays readable.
            var i = 0
            val step = slides.toFloat() / count
            while (remaining > 0) {
                val x = 0.06f + i * step
                val top = if (i % 2 == 0) 0.05f else 0.67f
                photo(x, top, x + 0.6f, top + 0.28f, style = PhotoStyle(shadow = 24f), anim = Anim(AnimKind.Rise, 0.2f + (i % 2) * 0.15f, 0.6f))
                i++
            }
            text("{dates}", 0.5f, 0.95f, 0.9f, 30f, color = ColorRole.Muted, spacing = 0.3f)
        },

        Template("torn-paper", "Torn paper", Category.Scrapbook, Themes.Forest) {
            slides = slidesFor(count, 2f, 1)
            text("{title}", 0.5f, 0.38f, 0.86f, 140f, FontRole.Head, anim = rise)
            text("{dates}", 0.5f, 0.5f, 0.86f, 44f, FontRole.Hand, ColorRole.Accent)
            shape(ShapeKind.Blob, 0.15f, 0.85f, 360f, 300f, ColorRole.Muted)
            shape(ShapeKind.Blob, 0.92f, 0.1f, 300f, 260f, ColorRole.Accent)
            var s = 1
            while (remaining > 0) {
                val torn = PhotoStyle(frame = FrameKind.Torn, shadow = 16f)
                photo(s + 0.05f, 0.06f, s + 0.7f, 0.5f, -2f, torn, anim = Anim(AnimKind.SlideIn, 0f, 0.6f))
                photo(s + 0.35f, 0.5f, s + 1.02f, 0.94f, 2f, torn, anim = Anim(AnimKind.SlideIn, 0.2f, 0.6f))
                text("day {day}", s + 0.84f, 0.28f, 0.3f, 60f, FontRole.Hand, rotation = 6f, day = s)
                s++
            }
        },

        Template("story-route", "Route stories", Category.Story, Themes.Midnight, Ratio.Story, sample = 5) {
            slides = slidesFor(count, 1f, 1)
            text("{title}", 0.5f, 0.3f, 0.86f, 150f, FontRole.Head, anim = rise)
            text("{dates}", 0.5f, 0.4f, 0.8f, 44f, color = ColorRole.Accent, spacing = 0.15f)
            text("tap through →", 0.5f, 0.88f, 0.6f, 36f, color = ColorRole.Muted, anim = Anim(AnimKind.Float, 0.4f, 0.8f))
            var s = 1
            val pts = mutableListOf(0.5f to 0.6f)
            while (remaining > 0) {
                val up = s % 2 == 0
                val cy = if (up) 0.36f else 0.5f
                photo(s + 0.14f, cy - 0.2f, s + 0.86f, cy + 0.2f, style = PhotoStyle(frame = FrameKind.Circle, borderWidth = 14f, borderRole = ColorRole.Paper), anim = pop)
                text("stop {day}", s + 0.5f, cy + 0.27f, 0.8f, 64f, FontRole.Head, day = s, anim = later(rise, 0.3f))
                pts += (s + 0.5f) to (cy + 0.34f)
                s++
            }
            route(pts, Dash.Dotted, 10f, ColorRole.Accent, Cap.Ring, Cap.Pin, stops = true, vehicle = Vehicle.Plane)
        },

        Template("circles", "Circle moments", Category.Minimal, Themes.Lilac, sample = 7) {
            slides = slidesFor(count, 2f, 1)
            text("{title}", 0.5f, 0.42f, 0.86f, 140f, FontRole.Head, anim = rise)
            text("{dates}", 0.5f, 0.53f, 0.8f, 40f, color = ColorRole.Muted)
            var i = 0
            val step = (slides - 1f) / count.coerceAtLeast(1)
            while (remaining > 0) {
                val cx = 1f + step * (i + 0.5f)
                val cy = if (i % 2 == 0) 0.32f else 0.68f
                val r = min(0.38f, step * 0.9f).coerceAtLeast(0.28f)
                photo(cx - r, cy - r * W / H, cx + r, cy + r * W / H, style = PhotoStyle(frame = FrameKind.Circle), anim = pop)
                if (i % 3 == 1) shape(ShapeKind.Sparkle, cx + r * 0.9f, cy - r * W / H * 0.8f, 70f, 70f)
                i++
            }
            routeThroughPhotos(Dash.Dotted, 9f, ColorRole.Accent, Vehicle.Heart, stops = false)
        },
    )
}
