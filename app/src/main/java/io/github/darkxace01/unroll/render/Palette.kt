package io.github.darkxace01.unroll.render

import android.graphics.Bitmap
import io.github.darkxace01.unroll.UnrollApp
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Swatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Background colours picked from the project's own photos. */
object Palette {
    suspend fun fromPhotos(app: UnrollApp, p: Project): List<Long> = withContext(Dispatchers.Default) {
        val pixels = mutableListOf<Int>()
        for (ph in p.items.filterIsInstance<PhotoItem>().distinctBy { it.uri }.take(8)) {
            val bmp = app.photos.load(ph.uri, 1600) ?: continue
            val small = Bitmap.createScaledBitmap(bmp, 24, 24, true)
            val arr = IntArray(24 * 24)
            small.getPixels(arr, 0, 24, 0, 0, 24, 24)
            if (small !== bmp) small.recycle()
            pixels += arr.toList()
        }
        Swatch.pick(pixels.toIntArray(), 8)
    }
}
