package io.github.darkxace01.unroll.export

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Seams
import io.github.darkxace01.unroll.render.Share
import io.github.darkxace01.unroll.support.SupportLine
import kotlinx.coroutines.launch

/** Saves the slides to the gallery, then hands them to Instagram (or anywhere). */
@Composable
fun ExportScreen(p: Project, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    var uris by remember { mutableStateOf<List<Uri>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var panorama by remember { mutableStateOf<Uri?>(null) }
    val onCuts = remember(p) { Seams.warnings(p).map { it.itemId }.distinct().size }

    LaunchedEffect(p.id) {
        app.exporter.quality = app.prefs.now.jpegQuality
        runCatching { app.exporter.exportSlides(p) { done, total -> progress = done / total.toFloat() } }
            .onSuccess { uris = it }
            .onFailure { error = it.message ?: "Something went wrong saving the slides." }
        if (app.prefs.now.alsoPanorama) panorama = runCatching { app.exporter.exportPanorama(p) }.getOrNull()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val done = uris
            when {
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                done == null -> {
                    Text("Saving ${p.slides} slides…", style = MaterialTheme.typography.titleLarge)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
                else -> {
                    Text("Saved ${done.size} slides", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "In your gallery under Pictures/Unroll, ${p.slideW}×${p.slideH} each, in order. No watermark.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (onCuts > 0) Text(
                        "Heads up: $onCuts ${if (onCuts == 1) "thing sits" else "things sit"} on a cut between slides.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                    )
                    Button(onClick = { Share.toInstagram(context, done) }, modifier = Modifier.fillMaxWidth()) { Text("Post to Instagram") }
                    OutlinedButton(onClick = { Share.anywhere(context, done) }, modifier = Modifier.fillMaxWidth()) { Text("Share somewhere else") }
                    VideoExports(p)
                    if (panorama == null) TextButton(onClick = { scope.launch { panorama = app.exporter.exportPanorama(p) } }) { Text("Also save it as one long image") }
                    else Text("Saved the long image too.", style = MaterialTheme.typography.bodySmall)
                    SupportLine(Modifier.padding(top = 24.dp))
                }
            }
        }
    }
}

/** Reels / scroll video and the animated carousel, made on request (they take a little while). */
@Composable
private fun VideoExports(p: Project) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var reel by remember { mutableStateOf<Uri?>(null) }
    var animated by remember { mutableStateOf<List<Uri>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val moving = remember(p) { io.github.darkxace01.unroll.model.Timeline.animatedSlides(p) }

    Text("Video", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
    val b = busy
    if (b != null) {
        Text(b, style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        return
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    val r = reel
    if (r == null) {
        OutlinedButton(onClick = {
            scope.launch {
                busy = "Making the Reel…"; progress = 0f; error = null
                runCatching { app.video.scroll(p, reel = true) { progress = it } }.onSuccess { reel = it }.onFailure { error = "Couldn't make the video: ${it.message}" }
                busy = null
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Make a Reel (scrolling video)") }
    } else {
        Button(onClick = { Share.toInstagram(context, listOf(r)) }, modifier = Modifier.fillMaxWidth()) { Text("Post the Reel") }
    }
    val a = animated
    if (moving.isEmpty()) {
        Text("Add motion to anything (Motion in the editor) to post an animated carousel.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else if (a == null) {
        OutlinedButton(onClick = {
            scope.launch {
                busy = "Animating ${moving.size} slides…"; progress = 0f; error = null
                runCatching { app.video.animatedSlides(p) { progress = it } }.onSuccess { animated = it }.onFailure { error = "Couldn't make the clips: ${it.message}" }
                busy = null
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Make an animated carousel (${moving.size} moving slides)") }
    } else {
        Button(onClick = { Share.toInstagram(context, a) }, modifier = Modifier.fillMaxWidth()) { Text("Post the animated carousel") }
    }
}
