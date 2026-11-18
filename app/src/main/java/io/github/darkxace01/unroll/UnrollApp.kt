package io.github.darkxace01.unroll

import android.app.Application
import android.content.Context
import io.github.darkxace01.unroll.data.Prefs
import io.github.darkxace01.unroll.data.ProjectStore
import io.github.darkxace01.unroll.render.Exporter
import io.github.darkxace01.unroll.render.Fonts
import io.github.darkxace01.unroll.render.Photos
import io.github.darkxace01.unroll.render.Renderer
import io.github.darkxace01.unroll.support.FaceFinder
import io.github.darkxace01.unroll.support.SupportAds
import io.github.darkxace01.unroll.support.TipJar
import io.github.darkxace01.unroll.support.createFaceFinder
import io.github.darkxace01.unroll.support.createSupportAds
import io.github.darkxace01.unroll.support.createTipJar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class UnrollApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val prefs by lazy { Prefs(this) }
    val store by lazy { ProjectStore(File(filesDir, "projects")) }
    val photos by lazy { Photos(this) }
    val fonts by lazy { Fonts(this) }
    val renderer by lazy { Renderer(fonts) }
    val exporter by lazy { Exporter(this, photos, renderer) }
    val faces: FaceFinder by lazy { createFaceFinder(this) }
    val ads: SupportAds by lazy { createSupportAds(this) }
    val tips: TipJar by lazy { createTipJar(this, scope) { _, count -> prefs.addSupport(count) } }

    override fun onCreate() {
        super.onCreate()
        scope.launch { store.load() }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND) photos.clear()
    }
}

val Context.app get() = applicationContext as UnrollApp
