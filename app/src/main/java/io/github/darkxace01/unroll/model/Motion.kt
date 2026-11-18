package io.github.darkxace01.unroll.model

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** How an item arrives in a video: when its slide comes into view, after [delay], over [duration] seconds. */
@Serializable
enum class AnimKind(val label: String) {
    None("None"), Fade("Fade"), Rise("Rise"), Drop("Drop"), Pop("Pop"), SlideIn("Slide in"), Spin("Spin"),
    Draw("Draw"), Type("Type"), Float("Float"),
}

@Serializable
data class Anim(val kind: AnimKind = AnimKind.None, val delay: Float = 0f, val duration: Float = 0.6f)

/** What an animation does to an item at one moment. Identity = the item as designed. */
data class Look(
    val alpha: Float = 1f,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    /** How much of a route is drawn / how much of the text has been typed, 0..1. */
    val reveal: Float = 1f,
) {
    companion object { val Still = Look() }
}

object Motion {
    fun easeOut(t: Float) = 1f - (1f - t).pow(3)
    fun easeInOut(t: Float) = if (t < 0.5f) 4 * t * t * t else 1 - (-2 * t + 2).pow(3) / 2
    /** Overshoots a little and settles: for pops. */
    fun back(t: Float): Float { val c = 1.70158f; val x = t - 1; return 1 + (c + 1) * x * x * x + c * x * x }

    /**
     * The look of an animated item [t] seconds after its cue (its slide coming into view). Before the cue
     * plus delay it's in its start state; after the duration it's still (except Float, which keeps drifting).
     * [unit] is a small distance in canvas px for rises and slides.
     */
    fun look(a: Anim?, t: Float, unit: Float = 80f): Look {
        if (a == null || a.kind == AnimKind.None) return Look.Still
        val raw = ((t - a.delay) / a.duration.coerceAtLeast(0.05f)).coerceIn(0f, 1f)
        val e = easeOut(raw)
        return when (a.kind) {
            AnimKind.None -> Look.Still
            AnimKind.Fade -> Look(alpha = e)
            AnimKind.Rise -> Look(alpha = e, dy = (1 - e) * unit)
            AnimKind.Drop -> Look(alpha = e, dy = -(1 - e) * unit)
            AnimKind.Pop -> Look(alpha = minOf(1f, raw * 3), scale = 0.4f + 0.6f * back(raw))
            AnimKind.SlideIn -> Look(alpha = e, dx = (1 - e) * unit * 3)
            AnimKind.Spin -> Look(alpha = e, rotation = (1 - e) * -90f, scale = 0.6f + 0.4f * e)
            AnimKind.Draw -> Look(reveal = easeInOut(raw))
            AnimKind.Type -> Look(reveal = raw)
            AnimKind.Float -> Look(alpha = e, dy = sin(((t - a.delay) * 2 * PI / 3).toFloat()) * unit * 0.15f * e)
        }
    }

    /** Seconds until every animation in the project has finished, measured from its own cue. */
    fun longest(p: Project): Float = p.items.maxOfOrNull { it.anim?.let { a -> if (a.kind == AnimKind.None) 0f else a.delay + a.duration } ?: 0f } ?: 0f
}

/**
 * The camera for a scroll video: it holds on each slide, then glides to the next. Times in seconds.
 */
data class ScrollPlan(val slides: Int, val hold: Float = 1.6f, val move: Float = 0.9f, val tail: Float = 0.6f) {
    val duration get() = slides * hold + (slides - 1) * move + tail

    /** Left edge of the view in slide widths (0 = first slide) at time [t]. */
    fun position(t: Float): Float {
        var time = t.coerceAtLeast(0f)
        for (i in 0 until slides) {
            if (time < hold) return i.toFloat()
            time -= hold
            if (i == slides - 1) return i.toFloat()
            if (time < move) return i + Motion.easeInOut(time / move)
            time -= move
        }
        return (slides - 1).toFloat()
    }

    /** When the camera first shows any of [left]..[right] (in slide widths): an item's cue. */
    fun cue(left: Float): Float {
        // The view [pos, pos+1] reaches left when pos = left - 1 (just entering) — but we cue a little later,
        // when it's a third in, so the animation is seen.
        val target = (left - 0.66f).coerceAtLeast(0f)
        val slide = target.toInt()
        val frac = target - slide
        var t = slide * (hold + move)
        if (frac > 0f) t += hold + inverseEase(frac) * move
        return t
    }

    private fun inverseEase(y: Float): Float {
        var lo = 0f; var hi = 1f
        repeat(24) { val mid = (lo + hi) / 2; if (Motion.easeInOut(mid) < y) lo = mid else hi = mid }
        return (lo + hi) / 2
    }
}
