package io.github.darkxace01.unroll.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache

/**
 * Soft focus for backdrops: shrink the photo a lot, then grow it back in steps so the bilinear filter
 * smooths it into a blur. Cheap, works in tests and needs no RenderScript.
 */
object Blur {
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun of(src: Bitmap, region: Rect, amount: Float): Bitmap {
        val key = "${System.identityHashCode(src)}:${src.width}x${src.height}:$region:${(amount * 20).toInt()}"
        cache.get(key)?.let { return it }
        val a = amount.coerceIn(0.05f, 1f)
        // amount 1 → about 12 px across, 0.05 → about 200.
        val small = (200f * (1f - a) + 12f * a).toInt().coerceAtLeast(8)
        val aspect = region.height().toFloat() / region.width().coerceAtLeast(1)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        var cur = Bitmap.createBitmap(small, (small * aspect).toInt().coerceAtLeast(4), Bitmap.Config.ARGB_8888)
        Canvas(cur).drawBitmap(src, region, Rect(0, 0, cur.width, cur.height), paint)
        // Grow twice by 3x: each bilinear pass softens the blocks the shrink left behind.
        repeat(2) {
            val next = Bitmap.createBitmap(cur.width * 3, cur.height * 3, Bitmap.Config.ARGB_8888)
            Canvas(next).drawBitmap(cur, null, Rect(0, 0, next.width, next.height), paint)
            cur = next
        }
        cache.put(key, cur)
        return cur
    }
}
