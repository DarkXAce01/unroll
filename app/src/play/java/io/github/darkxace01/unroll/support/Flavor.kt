package io.github.darkxace01.unroll.support

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import io.github.darkxace01.unroll.BuildConfig
import io.github.darkxace01.unroll.model.NormRect
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

fun createSupportAds(context: Context): SupportAds = RewardedSupport(context.applicationContext)

/**
 * The ads SDK isn't even started until someone taps "support with an ad", so people who never tap it
 * never talk to an ad server. Ads are requested non-personalised and rated for general audiences.
 */
private class RewardedSupport(private val app: Context) : SupportAds {
    override val supported = true
    private var started = false

    override fun show(activity: Activity, onResult: (AdOutcome) -> Unit) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                if (consent.canRequestAds()) load(activity, onResult) else onResult(AdOutcome.Unavailable)
            }
        }, { onResult(AdOutcome.Unavailable) })
    }

    private fun load(activity: Activity, onResult: (AdOutcome) -> Unit) {
        if (!started) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder().setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G).build(),
            )
            MobileAds.initialize(app)
            started = true
        }
        val request = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply { putString("npa", "1") })
            .build()
        RewardedAd.load(activity, BuildConfig.REWARDED_AD_UNIT, request, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) = onResult(AdOutcome.Unavailable)

            override fun onAdLoaded(ad: RewardedAd) {
                var earned = false
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() = onResult(if (earned) AdOutcome.Thanked else AdOutcome.Closed)
                    override fun onAdFailedToShowFullScreenContent(e: AdError) = onResult(AdOutcome.Unavailable)
                }
                ad.show(activity) { earned = true }
            }
        })
    }
}

fun createFaceFinder(context: Context): FaceFinder = MlKitFaces

/** ML Kit's bundled face model: runs on the phone, the photo never leaves it. */
private object MlKitFaces : FaceFinder {
    override val supported = true
    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.05f)
                .build(),
        )
    }

    override suspend fun find(bitmap: Bitmap): List<NormRect> = suspendCancellableCoroutine { cont ->
        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { faces ->
                val w = bitmap.width.toFloat()
                val h = bitmap.height.toFloat()
                cont.resume(faces.map { f ->
                    val b = f.boundingBox
                    NormRect((b.left / w).coerceIn(0f, 1f), (b.top / h).coerceIn(0f, 1f), (b.right / w).coerceIn(0f, 1f), (b.bottom / h).coerceIn(0f, 1f))
                })
            }
            .addOnFailureListener { cont.resume(emptyList()) }
    }
}
