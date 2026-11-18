package io.github.darkxace01.unroll.render

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import io.github.darkxace01.unroll.model.PhotoGeometry
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Seams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Turns a project into Instagram-ready files: one JPEG per slide at exactly 1080 wide, saved to
 * Pictures/Unroll/<project>. Slides are drawn one at a time, loading only the photos on that slide, so
 * twenty slides fit in a small phone's memory.
 */
class Exporter(private val context: Context, private val photos: Photos, private val renderer: Renderer) {

    /** JPEG quality. Instagram re-encodes anyway; 95 gives it the cleanest start without huge files. */
    var quality = 95

    /** How big the whole photo must be decoded so its visible part is sharp at its frame size. */
    fun neededSide(item: PhotoItem): Int {
        val v = PhotoGeometry.visible(item)
        val k = maxOf(item.frame.w / ((v.right - v.left) * item.srcW), item.frame.h / ((v.bottom - v.top) * item.srcH)).coerceAtMost(1f)
        return ceil(maxOf(item.srcW, item.srcH) * k).toInt().coerceAtLeast(64)
    }

    /** One slide as a bitmap at full size (or scaled by [scale] for thumbnails and the preview). */
    suspend fun renderSlide(p: Project, index: Int, scale: Float = 1f, cache: MutableMap<String, Bitmap?> = HashMap()): Bitmap = withContext(Dispatchers.Default) {
        val slide = Seams.slide(p, index)
        val onSlide = p.items.filterIsInstance<PhotoItem>().filter { it.frame.bounds().overlaps(slide) }
        for (ph in onSlide) {
            val key = "${ph.uri}@${neededScaled(ph, scale)}"
            if (key !in cache) cache[key] = photos.load(ph.uri, neededScaled(ph, scale), useCache = scale < 1f)
        }
        val bmp = Bitmap.createBitmap((p.slideW * scale).toInt(), (p.slideH * scale).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(scale, scale)
        c.translate(-slide.left, 0f)
        renderer.draw(c, p, { uri -> onSlide.firstOrNull { it.uri == uri }?.let { cache["$uri@${neededScaled(it, scale)}"] } })
        bmp
    }

    private fun neededScaled(ph: PhotoItem, scale: Float) = (neededSide(ph) * scale).toInt().coerceAtLeast(64)

    /** The whole canvas as one long image, scaled down if it would be over ~24 megapixels. */
    suspend fun renderPanorama(p: Project): Bitmap = withContext(Dispatchers.Default) {
        val px = p.width.toFloat() * p.height
        val scale = minOf(1f, sqrt(24_000_000f / px))
        val bmp = Bitmap.createBitmap((p.width * scale).toInt(), (p.height * scale).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(scale, scale)
        val loaded = HashMap<String, Bitmap?>()
        for (ph in p.items.filterIsInstance<PhotoItem>()) {
            if (ph.uri !in loaded) loaded[ph.uri] = photos.load(ph.uri, (neededSide(ph) * scale).toInt().coerceAtLeast(64), useCache = false)
        }
        renderer.draw(c, p, { loaded[it] })
        bmp
    }

    /** Saves every slide (or just [only]) to the gallery and returns their URIs in order. */
    suspend fun exportSlides(p: Project, only: Collection<Int>? = null, onProgress: (Int, Int) -> Unit = { _, _ -> }): List<Uri> = withContext(Dispatchers.IO) {
        val indices = (0 until p.slides).filter { only == null || it in only }
        val out = mutableListOf<Uri>()
        val cache = HashMap<String, Bitmap?>()
        indices.forEachIndexed { n, i ->
            onProgress(n, indices.size)
            // Keep only photos that reach into this slide or later ones.
            val bmp = renderSlide(p, i, 1f, cache)
            out += save(bmp, "${fileBase(p)} ${(i + 1).toString().padStart(2, '0')}", folder(p))
            bmp.recycle()
            val ahead = Seams.slide(p, i + 1)
            val keep = p.items.filterIsInstance<PhotoItem>().filter { it.frame.bounds().right > ahead.left }.map { "${it.uri}@${neededSide(it)}" }.toSet()
            cache.keys.retainAll(keep)
        }
        onProgress(indices.size, indices.size)
        out
    }

    suspend fun exportPanorama(p: Project): Uri = withContext(Dispatchers.IO) {
        val bmp = renderPanorama(p)
        save(bmp, "${fileBase(p)} panorama", folder(p)).also { bmp.recycle() }
    }

    private fun fileBase(p: Project) = p.name.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim().ifEmpty { "Unroll" }

    private fun folder(p: Project) = "${Environment.DIRECTORY_PICTURES}/Unroll/${fileBase(p)}"

    private fun save(bmp: Bitmap, name: String, folder: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, folder)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: error("Couldn't create $name in the gallery")
        resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }
}

/** Handing the slides to Instagram (or anything else). No Instagram login or API: just Android sharing. */
object Share {
    const val INSTAGRAM = "com.instagram.android"

    fun has(context: Context, pkg: String) = runCatching {
        context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)); true
    }.getOrDefault(false)

    /** The slides in order, as one share. Instagram opens its carousel composer with them. */
    fun intent(uris: List<Uri>, pkg: String? = null): Intent {
        val i = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        i.type = if (uris.size > 0) "image/jpeg" else "image/*"
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (pkg != null) i.setPackage(pkg)
        return i
    }

    fun toInstagram(context: Context, uris: List<Uri>) {
        val direct = if (has(context, INSTAGRAM)) intent(uris, INSTAGRAM) else null
        val launched = direct != null && runCatching { context.startActivity(direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
        if (!launched) anywhere(context, uris)
    }

    fun anywhere(context: Context, uris: List<Uri>) {
        context.startActivity(Intent.createChooser(intent(uris), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
