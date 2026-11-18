package io.github.darkxace01.unroll.support

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import io.github.darkxace01.unroll.model.NormRect

/** The F-Droid build has no ads. */
@Suppress("UNUSED_PARAMETER")
fun createSupportAds(context: Context): SupportAds = object : SupportAds {
    override val supported = false
    override fun show(activity: Activity, onResult: (AdOutcome) -> Unit) = onResult(AdOutcome.Unavailable)
}

/** ML Kit isn't open source, so the F-Droid build doesn't look for faces (text on cuts is still caught). */
@Suppress("UNUSED_PARAMETER")
fun createFaceFinder(context: Context): FaceFinder = object : FaceFinder {
    override val supported = false
    override suspend fun find(bitmap: Bitmap): List<NormRect> = emptyList()
}
