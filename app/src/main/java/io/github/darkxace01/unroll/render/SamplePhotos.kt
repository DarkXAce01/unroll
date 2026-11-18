package io.github.darkxace01.unroll.render

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One bundled sample photo and who took it (all freely licensed, credited in Settings). */
@Serializable
data class SampleCredit(
    val file: String, val tag: String, val w: Int, val h: Int, val title: String, val artist: String, val license: String, val source: String,
    /** Photos from one shoot share this, so a template's preview can show one real day. */
    val shoot: String = "",
)

/**
 * Real photos for template previews, so a template looks the way it will with your own pictures.
 * Decoded at half size (previews never need more) and kept in a small cache.
 */
object SamplePhotos {
    @Volatile private var assets: AssetManager? = null
    @Volatile var credits: List<SampleCredit> = emptyList(); private set

    private val cache = object : LruCache<Int, Bitmap>(28 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: Bitmap) = value.byteCount
    }

    fun init(a: AssetManager) {
        if (assets != null) return
        assets = a
        credits = runCatching {
            a.open("samples/credits.json").use { Json { ignoreUnknownKeys = true }.decodeFromString<List<SampleCredit>>(it.readBytes().decodeToString()) }
        }.getOrDefault(emptyList())
    }

    val size get() = credits.size

    fun bitmap(i: Int): Bitmap? {
        val a = assets ?: return null
        if (credits.isEmpty()) return null
        val k = Math.floorMod(i, credits.size)
        cache.get(k)?.let { return it }
        val bmp = runCatching {
            a.open("samples/${credits[k].file}").use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 2 }) }
        }.getOrNull() ?: return null
        cache.put(k, bmp)
        return bmp
    }

    /**
     * A stable order of photos for one template: photos that suit [tags] first, shuffled by [seed] so
     * neighbouring templates don't all show the same beach.
     */
    fun order(seed: Int, tags: Set<String>, shoot: String? = null, wideFirst: Boolean = false): IntArray {
        if (credits.isEmpty()) return IntArray(0)
        if (shoot != null) {
            val own = credits.indices.filter { credits[it].shoot == shoot }
            if (own.isNotEmpty()) {
                // A layout that opens across two slides starts on the shoot's widest photo.
                val lead = if (wideFirst) own.maxBy { credits[it].w.toFloat() / credits[it].h } else null
                return (listOfNotNull(lead) + own.filter { it != lead }).toIntArray()
            }
        }
        val rnd = java.util.Random(seed.toLong())
        val idx = credits.indices.shuffled(rnd)
        val (fit, rest) = idx.partition { credits[it].tag in tags }
        return (fit + rest).toIntArray()
    }

    /** Which kinds of photo suit a template category. */
    fun tagsFor(category: String): Set<String> = when (category) {
        "Travel", "Story", "Recap", "Film", "Instant" -> setOf("beach", "city", "sunset", "nature", "night")
        "Outdoor" -> setOf("nature", "beach", "sunset", "flowers")
        "Weddings" -> setOf("wedding", "flowers", "sunset")
        "Love" -> setOf("flowers", "sunset", "wedding")
        "Celebrations" -> setOf("party", "night", "food")
        "Scrapbook", "Stationery", "Scribbles", "Paper" -> setOf("food", "flowers", "pets", "city")
        "Fashion" -> setOf("style", "city", "night")
        "Editorial" -> setOf("city", "style", "flowers")
        "Portraits" -> setOf("pets", "flowers", "style")
        "Bold", "Digital" -> setOf("night", "city", "food", "party")
        else -> setOf("city", "nature", "food", "flowers")
    }
}
