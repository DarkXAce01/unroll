package io.github.darkxace01.unroll.data

import android.net.Uri
import io.github.darkxace01.unroll.UnrollApp
import io.github.darkxace01.unroll.model.Arrange
import io.github.darkxace01.unroll.model.Arranger
import io.github.darkxace01.unroll.model.NewPhoto
import io.github.darkxace01.unroll.model.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Starting a carousel from picked (or shared) photos. */
object NewProject {
    suspend fun create(app: UnrollApp, uris: List<Uri>, how: Arrange = Arrange.Auto): Project {
        val photos = uris.mapNotNull { u ->
            app.photos.keep(u)
            app.photos.info(u)?.let { NewPhoto(u.toString(), it.width, it.height, it.takenAt, it.lat, it.lng) }
        }.sortedWith(compareBy(nullsLast()) { it.takenAt })
        val s = app.prefs.now
        val now = System.currentTimeMillis()
        val base = Project(id = newId(), name = name(photos), ratio = s.ratio, created = now)
        val arranged = Arranger.arrange(base, photos, how, s.margin, s.gap) { newId() }
        return app.store.save(arranged)
    }

    suspend fun blank(app: UnrollApp): Project {
        val now = System.currentTimeMillis()
        return app.store.save(Project(id = newId(), name = "Carousel ${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(now))}", ratio = app.prefs.now.ratio, slides = 3, created = now))
    }

    /** "12–16 Nov" from the photos' dates, or today's date. */
    private fun name(photos: List<NewPhoto>): String {
        val dates = photos.mapNotNull { it.takenAt }
        val day = SimpleDateFormat("d MMM", Locale.getDefault())
        if (dates.isEmpty()) return "Carousel ${day.format(Date())}"
        val a = Date(dates.min()); val b = Date(dates.max())
        val sameDay = SimpleDateFormat("yyyyMMdd", Locale.US).let { it.format(a) == it.format(b) }
        if (sameDay) return day.format(a)
        val sameMonth = SimpleDateFormat("yyyyMM", Locale.US).let { it.format(a) == it.format(b) }
        return if (sameMonth) "${SimpleDateFormat("d", Locale.getDefault()).format(a)}–${day.format(b)}" else "${day.format(a)} – ${day.format(b)}"
    }
}
