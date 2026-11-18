package io.github.darkxace01.unroll.model

import kotlin.math.abs

/**
 * Picks a few distinct, usable colours from photo pixels: buckets colours coarsely, takes the most common
 * buckets that differ enough from each other, and adds a soft light tint of the top one (a typical
 * carousel background). Plain Kotlin on ARGB ints so it's tested without Android.
 */
object Swatch {
    fun pick(argb: IntArray, count: Int): List<Long> {
        if (argb.isEmpty()) return emptyList()
        val buckets = HashMap<Int, IntArray>() // key -> [n, r, g, b]
        for (c in argb) {
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
            val key = (r shr 5 shl 6) or (g shr 5 shl 3) or (b shr 5)
            val acc = buckets.getOrPut(key) { IntArray(4) }
            acc[0]++; acc[1] += r; acc[2] += g; acc[3] += b
        }
        val ranked = buckets.values.sortedByDescending { it[0] }.map { Triple(it[1] / it[0], it[2] / it[0], it[3] / it[0]) }
        val chosen = mutableListOf<Triple<Int, Int, Int>>()
        for (c in ranked) {
            if (chosen.none { o -> abs(o.first - c.first) + abs(o.second - c.second) + abs(o.third - c.third) < 90 }) chosen += c
            if (chosen.size >= count - 1) break
        }
        val top = chosen.firstOrNull()
        val tint = top?.let { Triple(mix(it.first), mix(it.second), mix(it.third)) }
        return (listOfNotNull(tint) + chosen).map { (r, g, b) -> 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong() }
    }

    /** 85% of the way to white. */
    private fun mix(v: Int) = v + ((255 - v) * 0.85f).toInt()
}
