package io.github.darkxace01.unroll.render

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** What we need to know about a photo before placing it. */
data class PhotoInfo(val uri: Uri, val width: Int, val height: Int, val takenAt: Long?, val lat: Double?, val lng: Double?)

/**
 * Reading photos: their upright size, date and place (EXIF), and bitmaps at the size asked for. Bitmaps
 * for the screen are cached by size; export asks for full size and skips the cache.
 */
class Photos(private val context: Context) {
    private val resolver get() = context.contentResolver

    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 6).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    /** Keeps read access to picker photos across restarts, so saved projects still open. */
    fun keep(uri: Uri) {
        runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    suspend fun info(uri: Uri): PhotoInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            if (opts.outWidth <= 0) return@runCatching null
            val exif = resolver.openInputStream(uri)?.use { ExifInterface(it) }
            val rotated = exif?.rotationDegrees.let { it == 90 || it == 270 }
            val w = if (rotated) opts.outHeight else opts.outWidth
            val h = if (rotated) opts.outWidth else opts.outHeight
            val ll = exif?.latLong
            PhotoInfo(uri, w, h, exif?.let(::takenAt), ll?.get(0), ll?.get(1))
        }.getOrNull()
    }

    private fun takenAt(exif: ExifInterface): Long? {
        val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(ExifInterface.TAG_DATETIME) ?: return null
        return runCatching {
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getDefault() }.parse(raw)?.time
        }.getOrNull()
    }

    /** Cached bitmap at about [maxSide] on its longest side, if already loaded. */
    fun cached(uri: String, maxSide: Int): Bitmap? = cache.get("$uri@$maxSide")

    /**
     * The photo upright, sampled so its longest side is at least [maxSide] (or full size if smaller).
     * Null if the photo is gone (deleted from the phone, or access lost).
     */
    suspend fun load(uri: String, maxSide: Int, useCache: Boolean = true): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$uri@$maxSide"
        if (useCache) cache.get(key)?.let { return@withContext it }
        val bmp = decode(Uri.parse(uri), maxSide) ?: return@withContext null
        if (useCache) cache.put(key, bmp)
        bmp
    }

    private fun decode(uri: Uri, maxSide: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val exif = resolver.openInputStream(uri)?.use { ExifInterface(it) }
        val deg = exif?.rotationDegrees ?: 0
        val flip = exif?.isFlipped ?: false
        if (deg == 0 && !flip) raw
        else {
            val m = Matrix().apply { if (flip) postScale(-1f, 1f); postRotate(deg.toFloat()) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true).also { if (it !== raw) raw.recycle() }
        }
    }.getOrNull()

    fun clear() = cache.evictAll()
}
