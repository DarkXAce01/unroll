package io.github.darkxace01.unroll.preview

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.photoUris
import io.github.darkxace01.unroll.model.ScrollPlan
import io.github.darkxace01.unroll.model.Timeline

/**
 * Plays the scroll video live, with the same renderer and timeline as the exported MP4, looping.
 */
@Composable
fun ScrollPlayer(p: Project, modifier: Modifier = Modifier) {
    val app = LocalContext.current.app
    val bitmaps = remember { mutableStateMapOf<String, Bitmap>() }
    val plan = remember(p.slides) { ScrollPlan(p.slides) }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(p.id) {
        for (u in p.photoUris()) app.photos.load(u, 1600)?.let { bitmaps[u] = it }
    }
    LaunchedEffect(plan) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { now -> t = ((now - start) / 1e9f) % (plan.duration + 0.8f) }
    }
    Canvas(modifier.fillMaxWidth().aspectRatio(p.slideW.toFloat() / p.slideH)) {
        val scale = size.width / p.slideW
        val x = plan.position(t) * p.slideW
        val look = Timeline.scrollLook(p, plan, t)
        drawIntoCanvas { cc ->
            val c = cc.nativeCanvas
            c.save()
            c.scale(scale, scale)
            c.clipRect(0f, 0f, p.slideW.toFloat(), p.slideH.toFloat())
            c.translate(-x, 0f)
            app.renderer.draw(c, p, { bitmaps[it] }, look = look)
            c.restore()
        }
    }
}
