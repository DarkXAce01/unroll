package io.github.darkxace01.unroll.model

import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Word meanings as vectors (GloVe, packed by scripts/pack_word_vectors.py): words used in similar
 * places sit close together, so "roommates" lands near "friends" without anyone listing it. Most
 * frequent words first. Words not in here (keyboard mash) have no meaning and find nothing.
 */
class WordVectors(val words: List<String>, private val dim: Int, private val data: FloatArray) {
    private val index = HashMap<String, Int>(words.size * 2).apply { words.forEachIndexed { i, w -> put(w, i) } }

    fun has(w: String) = w in index
    fun rank(w: String) = index[w] ?: Int.MAX_VALUE

    /** Cosine similarity of two known words (vectors are stored at length 1), or 0. */
    fun sim(a: String, b: String): Float {
        val i = index[a] ?: return 0f
        val j = index[b] ?: return 0f
        if (i == j) return 1f
        var s = 0f
        val oi = i * dim; val oj = j * dim
        for (k in 0 until dim) s += data[oi + k] * data[oj + k]
        return s
    }

    /** Known words starting with [prefix], most frequent first. */
    fun completions(prefix: String, max: Int = 6): List<String> {
        val out = ArrayList<String>(max)
        for (w in words) {
            if (w.length > prefix.length && w.startsWith(prefix)) { out += w; if (out.size == max) break }
        }
        return out
    }

    /** The most frequent known word one typo away (two for long words), or null. */
    fun correct(w: String): String? {
        var best: String? = null
        var bestRank = Int.MAX_VALUE
        for ((i, v) in words.withIndex()) {
            if (i >= bestRank) break
            if (v[0] != w[0] && (v.length < 2 || w.length < 2 || v[0] != w[1] || v[1] != w[0])) continue
            if (TemplateSearch.close(w, v)) { best = v; bestRank = i }
        }
        return best
    }

    companion object {
        fun read(vocab: InputStream, vectors: InputStream): WordVectors {
            val words = vocab.bufferedReader().readLines().filter { it.isNotBlank() }
            val bytes = DataInputStream(vectors.buffered()).use { it.readBytes() }
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val count = buf.int
            val dim = buf.int
            require(count == words.size) { "vocab and vectors disagree: ${words.size} vs $count" }
            val data = FloatArray(count * dim)
            for (i in 0 until count) {
                val scale = buf.float
                for (k in 0 until dim) data[i * dim + k] = buf.get() * scale
            }
            return WordVectors(words, dim, data)
        }
    }
}
