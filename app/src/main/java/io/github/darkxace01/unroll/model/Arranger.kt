package io.github.darkxace01.unroll.model

/** A photo ready to be placed: what [PhotoItem] needs apart from where it goes. */
data class NewPhoto(
    val uri: String,
    val w: Int,
    val h: Int,
    val takenAt: Long? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val faces: List<NormRect>? = null,
) {
    val aspect get() = w.toFloat() / h.coerceAtLeast(1)
}

enum class Arrange(val label: String) {
    Auto("Seamless"),
    OnePerSlide("One per slide"),
    Panorama("Panorama"),
}

/** Building and re-arranging whole projects. Plain Kotlin, so every arrangement is tested. */
object Arranger {

    private fun photoItem(id: String, ph: NewPhoto, frame: Frame) =
        PhotoItem(id, frame, ph.uri, ph.w, ph.h, faces = ph.faces, takenAt = ph.takenAt, lat = ph.lat, lng = ph.lng)

    /**
     * Lays [photos] out in a project. Text and stickers already there stay where they are; existing photos
     * are replaced by the new arrangement of [photos]. Ids come from [ids] so tests can predict them.
     */
    fun arrange(base: Project, photos: List<NewPhoto>, how: Arrange, margin: Float, gap: Float, seed: Int = 0, ids: (Int) -> String): Project {
        val ratio = base.ratio
        val keep = base.items.filter { it !is PhotoItem }
        if (photos.isEmpty()) return base.copy(items = keep)
        return when (how) {
            Arrange.Auto -> {
                val r = Layouts.auto(photos.mapIndexed { i, p -> LayoutPhoto(i.toString(), p.aspect, p.faces.orEmpty()) }, ratio, margin, gap, seed)
                val items = photos.mapIndexedNotNull { i, p -> r.frames[i.toString()]?.let { photoItem(ids(i), p, it) } }
                base.copy(slides = r.slides, items = items + keep)
            }
            Arrange.OnePerSlide -> {
                val n = photos.size.coerceAtMost(MAX_SLIDES)
                val frames = Layouts.onePerSlide(n, ratio, margin)
                base.copy(slides = n, items = photos.take(n).mapIndexed { i, p -> photoItem(ids(i), p, frames[i]) } + keep)
            }
            Arrange.Panorama -> {
                // The first photo across the whole canvas; any others go one per slide after it would be
                // confusing, so a panorama is one photo.
                val p = photos.first()
                val slides = Layouts.panoramaSlides(p.aspect, ratio)
                val sized = base.copy(slides = slides)
                sized.copy(items = listOf(photoItem(ids(0), p, Layouts.panorama(sized))) + keep)
            }
        }
    }

    /** Fills one slide with a grid, using the photos already on it (in reading order), then [extra]. */
    fun gridSlide(p: Project, slide: Int, grid: Grid, margin: Float, gap: Float): Project {
        val area = Seams.slide(p, slide)
        val onSlide = p.items.filterIsInstance<PhotoItem>().filter { it.frame.cx >= area.left && it.frame.cx < area.right }
            .sortedWith(compareBy({ it.frame.top.toInt() / 50 }, { it.frame.left }))
        val frames = Layouts.grid(p, slide, grid, margin, gap)
        var out = p
        onSlide.forEachIndexed { i, ph ->
            out = if (i < frames.size) out.replace(ph.copy(frame = frames[i], crop = Crop())) else out
        }
        return out
    }

    /** New photos added to a project that already has some: one per new slide at the end, inside margins. */
    fun append(p: Project, photos: List<NewPhoto>, margin: Float, ids: (Int) -> String): Project {
        val room = MAX_SLIDES - p.slides
        val adding = photos.take(room.coerceAtLeast(0))
        if (adding.isEmpty()) return p
        val start = p.slides
        val items = adding.mapIndexed { i, ph ->
            val slide = start + i
            val (w, h) = PhotoGeometry.fit(ph.aspect, p.slideW - 2 * margin, p.slideH - 2 * margin)
            photoItem(ids(i), ph, Frame(slide * p.slideW + p.slideW / 2f, p.slideH / 2f, w, h))
        }
        return p.copy(slides = p.slides + adding.size, items = p.items + items)
    }

    /** One photo dropped onto a slide at its own shape, about two-thirds of the slide. */
    fun place(p: Project, slide: Int, ph: NewPhoto, id: String): Project {
        val (w, h) = PhotoGeometry.fit(ph.aspect, p.slideW * 0.66f, p.slideH * 0.66f)
        return p.add(photoItem(id, ph, Frame(slide * p.slideW + p.slideW / 2f, p.slideH / 2f, w, h)))
    }

    /** Re-applies margin/gap to a project's auto layout by re-running it on the same photos. */
    fun photosOf(p: Project): List<NewPhoto> = p.items.filterIsInstance<PhotoItem>()
        .sortedBy { it.frame.left }
        .map { NewPhoto(it.uri, it.srcW, it.srcH, it.takenAt, it.lat, it.lng, it.faces) }
}
