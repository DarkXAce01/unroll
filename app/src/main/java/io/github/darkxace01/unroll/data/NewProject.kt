package io.github.darkxace01.unroll.data

import android.net.Uri
import io.github.darkxace01.unroll.UnrollApp
import io.github.darkxace01.unroll.model.Arrange
import io.github.darkxace01.unroll.model.Arranger
import io.github.darkxace01.unroll.model.NewPhoto
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Template
import io.github.darkxace01.unroll.model.Templates
import io.github.darkxace01.unroll.model.TripInfo
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    /** A carousel from a template, filled with [uris] (or empty slots to tap and fill when there are none). */
    suspend fun fromTemplate(app: UnrollApp, t: Template, uris: List<Uri>): Project {
        val photos = read(app, uris)
        val now = System.currentTimeMillis()
        val info = tripInfo(app, photos)
        val p = Templates.build(t, photos, info, newId(), name(photos).takeIf { photos.isNotEmpty() } ?: t.name, now, ::newId)
        return app.store.save(p)
    }

    private suspend fun read(app: UnrollApp, uris: List<Uri>) = uris.mapNotNull { u ->
        app.photos.keep(u)
        app.photos.info(u)?.let { NewPhoto(u.toString(), it.width, it.height, it.takenAt, it.lat, it.lng) }
    }.sortedWith(compareBy(nullsLast()) { it.takenAt })

    /** Dates, number of days and (where the phone can tell) the place, from the photos themselves. */
    suspend fun tripInfo(app: UnrollApp, photos: List<NewPhoto>, title: String? = null): TripInfo {
        val dates = photos.mapNotNull { it.takenAt }
        val days = if (dates.isEmpty()) 0 else ((dates.max() - dates.min()) / 86_400_000L).toInt() + 1
        val place = photos.firstOrNull { it.lat != null }?.let { placeName(app, it.lat!!, it.lng!!) }.orEmpty()
        return TripInfo(title = title ?: place.ifEmpty { "Our trip" }, dates = if (dates.isEmpty()) "" else name(photos), place = place, days = days)
    }

    /** The town for a spot, using the phone's own geocoder. Empty if it can't (offline, no service). */
    @Suppress("DEPRECATION")
    private suspend fun placeName(app: UnrollApp, lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext ""
        withTimeoutOrNull(3000) {
            runCatching {
                val a = Geocoder(app, Locale.getDefault()).getFromLocation(lat, lng, 1)?.firstOrNull()
                a?.locality ?: a?.subAdminArea ?: a?.adminArea ?: a?.countryName
            }.getOrNull()
        }.orEmpty()
    }

    suspend fun blank(app: UnrollApp): Project {
        val now = System.currentTimeMillis()
        return app.store.save(Project(id = newId(), name = "Carousel ${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(now))}", ratio = app.prefs.now.ratio, slides = 3, created = now))
    }

    /** "12–16 Nov" from the photos' dates, or today's date. */
    fun name(photos: List<NewPhoto>): String {
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
