package io.github.darkxace01.unroll.support

import android.app.Activity
import android.graphics.Bitmap
import io.github.darkxace01.unroll.model.NormRect

/**
 * "Support with an ad": a rewarded ad that only ever plays when someone taps the button for it. Nothing
 * else in the app shows ads, and watching one unlocks nothing (the reward is a thank-you).
 */
interface SupportAds {
    /** False in builds without ads (F-Droid); the app hides every way in. */
    val supported: Boolean

    /** Asks for consent where the law needs it, loads one ad and plays it. */
    fun show(activity: Activity, onResult: (AdOutcome) -> Unit)
}

enum class AdOutcome { Thanked, Closed, Unavailable }

/** Finds faces on a photo so the app can warn before a cut runs through one. */
interface FaceFinder {
    val supported: Boolean

    /** Faces as regions of [bitmap] in 0..1. Empty when there are none (or the build can't look). */
    suspend fun find(bitmap: Bitmap): List<NormRect>
}
