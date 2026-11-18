package io.github.darkxace01.unroll.model

/**
 * Photo adjustments as a 4×5 colour matrix (Android's ColorMatrix layout, offsets in 0..255), built in
 * plain Kotlin so it's tested on the computer. The same matrix is used on screen and in export.
 */
object ColorMath {
    private val IDENTITY = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    fun identity() = IDENTITY.copyOf()

    /** a then b (b ∘ a). */
    fun concat(b: FloatArray, a: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                out[r * 5 + c] = (0 until 4).sumOf { k -> (b[r * 5 + k] * a[k * 5 + c]).toDouble() }.toFloat()
            }
            out[r * 5 + 4] = (0 until 4).sumOf { k -> (b[r * 5 + k] * a[k * 5 + 4]).toDouble() }.toFloat() + b[r * 5 + 4]
        }
        return out
    }

    fun scale(r: Float, g: Float, b: Float) = floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    fun offset(r: Float, g: Float, b: Float) = floatArrayOf(
        1f, 0f, 0f, 0f, r,
        0f, 1f, 0f, 0f, g,
        0f, 0f, 1f, 0f, b,
        0f, 0f, 0f, 1f, 0f,
    )

    /** s = 1 keeps colour, 0 is grey (Rec. 709 luma). */
    fun saturation(s: Float): FloatArray {
        val lr = 0.2126f; val lg = 0.7152f; val lb = 0.0722f
        val i = 1 - s
        return floatArrayOf(
            lr * i + s, lg * i, lb * i, 0f, 0f,
            lr * i, lg * i + s, lb * i, 0f, 0f,
            lr * i, lg * i, lb * i + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /** Contrast around mid-grey. c = 1 unchanged. */
    fun contrast(c: Float): FloatArray {
        val t = 127.5f * (1 - c)
        return floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /** Lifts blacks (and softens whites a touch), the "matte" look. */
    fun fade(f: Float): FloatArray {
        val k = 1 - 0.25f * f
        val t = 255 * 0.18f * f
        return floatArrayOf(
            k, 0f, 0f, 0f, t,
            0f, k, 0f, 0f, t,
            0f, 0f, k, 0f, t,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    private fun lerp(a: FloatArray, b: FloatArray, t: Float) = FloatArray(20) { a[it] + (b[it] - a[it]) * t }

    fun filter(f: Filter): FloatArray = when (f) {
        Filter.None -> identity()
        Filter.Warm -> concat(offset(12f, 4f, -10f), scale(1.06f, 1.0f, 0.92f))
        Filter.Cool -> concat(offset(-8f, 0f, 12f), scale(0.94f, 1.0f, 1.08f))
        Filter.Golden -> concat(saturation(1.1f), concat(offset(18f, 8f, -16f), contrast(1.05f)))
        Filter.Film -> concat(fade(0.45f), concat(saturation(0.85f), offset(6f, 2f, -4f)))
        Filter.Fade -> concat(fade(0.8f), saturation(0.75f))
        Filter.Vivid -> concat(saturation(1.35f), contrast(1.12f))
        Filter.Mono -> saturation(0f)
        Filter.Noir -> concat(contrast(1.35f), saturation(0f))
        Filter.Sepia -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        Filter.Teal -> concat(contrast(1.08f), concat(offset(10f, -2f, 6f), floatArrayOf(
            1.08f, 0f, -0.04f, 0f, 0f,
            0f, 1.0f, 0.02f, 0f, 0f,
            -0.06f, 0.06f, 1.04f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )))
        Filter.Dusk -> concat(offset(10f, -4f, 14f), concat(saturation(0.9f), contrast(1.04f)))
        Filter.Matte -> concat(fade(0.6f), contrast(0.95f))
    }

    /** The full matrix for an [Adjust]: filter first, then the sliders. */
    fun matrix(a: Adjust): FloatArray {
        var m = if (a.filter == Filter.None) identity() else lerp(identity(), filter(a.filter), a.filterStrength.coerceIn(0f, 1f))
        if (a.exposure != 0f) {
            // ±1 stop.
            val e = Math.pow(2.0, a.exposure.toDouble()).toFloat()
            m = concat(scale(e, e, e), m)
        }
        if (a.contrast != 0f) m = concat(contrast(1 + a.contrast * 0.5f), m)
        if (a.saturation != 0f) m = concat(saturation(1 + a.saturation), m)
        if (a.warmth != 0f) m = concat(offset(20f * a.warmth, 4f * a.warmth, -20f * a.warmth), m)
        if (a.tint != 0f) m = concat(offset(10f * a.tint, -14f * a.tint, 10f * a.tint), m)
        if (a.fade != 0f) m = concat(fade(a.fade.coerceAtLeast(0f)), m)
        return m
    }

    /** Applies [m] to an sRGB colour (0..255 each), clamped. For tests and colour pickers. */
    fun apply(m: FloatArray, r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        fun row(i: Int) = (m[i * 5] * r + m[i * 5 + 1] * g + m[i * 5 + 2] * b + m[i * 5 + 4]).coerceIn(0f, 255f)
        return Triple(row(0), row(1), row(2))
    }
}
