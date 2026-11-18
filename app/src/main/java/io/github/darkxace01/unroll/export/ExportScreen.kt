package io.github.darkxace01.unroll.export

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Seams
import io.github.darkxace01.unroll.render.Share
import io.github.darkxace01.unroll.support.SupportLine
import io.github.darkxace01.unroll.ui.ToolIcons
import kotlinx.coroutines.launch

/** Sizes on offer: Instagram's own, then two sharper ones for printing or other apps. */
private enum class Size(val scale: Int, val label: String) { Standard(1, "Instagram"), High(2, "High"), Max(3, "Max") }

/**
 * See the post the way Instagram will show it, pick a size, then send it: Instagram first, TikTok
 * when it's on the phone, the gallery, or anywhere. Slides are saved only when you choose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(p: Project, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()
    var size by remember { mutableStateOf(Size.Standard) }
    val saved = remember { mutableStateMapOf<Size, List<Uri>>() }
    var progress by remember { mutableFloatStateOf(-1f) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedNote by remember { mutableStateOf<String?>(null) }
    var more by remember { mutableStateOf(false) }
    val onCuts = remember(p) { Seams.warnings(p).map { it.itemId }.distinct().size }
    val tiktok = remember { Share.tiktok(context) }

    /** Saves at the chosen size if not saved yet, then does [then] with the files. */
    fun withSlides(then: (List<Uri>) -> Unit) {
        saved[size]?.let { then(it); return }
        val s = size
        scope.launch {
            error = null; progress = 0f
            app.exporter.quality = app.prefs.now.jpegQuality
            runCatching { app.exporter.exportSlides(p, scale = s.scale.toFloat()) { done, total -> progress = done / total.toFloat() } }
                .onSuccess {
                    saved[s] = it; savedNote = "Saved ${it.size} slides to Pictures/Unroll"; then(it)
                    if (app.prefs.now.alsoPanorama && saved.size == 1) runCatching { app.exporter.exportPanorama(p) }
                }
                .onFailure { error = it.message ?: "Something went wrong saving the slides." }
            progress = -1f
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(ToolIcons.Back, "Back") }
            Text("Export", style = MaterialTheme.typography.titleMedium)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            InstagramPost(p)
            if (onCuts > 0) Text(
                "$onCuts ${if (onCuts == 1) "thing sits" else "things sit"} right on a cut between slides. Fine if you meant it.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Text("Size", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 10.dp))
            Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (s in Size.entries) {
                    val on = s == size
                    val w = p.slideW * s.scale; val h = p.slideH * s.scale
                    val mb = w.toLong() * h * 0.28 / 1_000_000 * p.slides
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                            .clickable { size = s }.padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val fg = if (on) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                        Text("${s.scale}×  ${s.label}", style = MaterialTheme.typography.labelLarge, color = fg)
                        Text("$w × $h", style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.75f))
                        Text("≈ ${if (mb < 10) "%.1f".format(mb) else mb.toInt().toString()} MB in all", style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.75f))
                    }
                }
            }
            if (size != Size.Standard) Text(
                "Instagram shrinks everything to 1080 wide, so 1× looks the same there. Bigger sizes are for printing or other apps.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            SupportLine(Modifier.padding(horizontal = 24.dp, vertical = 20.dp))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (progress >= 0f) {
                Text("Saving ${p.slides} slides…", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            } else savedNote?.let { Text("✓ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Button(onClick = { withSlides { Share.toInstagram(context, it) } }, enabled = progress < 0f, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(ToolIcons.Instagram, null, Modifier.size(20.dp)); Text("  Post to Instagram")
            }
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Dest("Save", ToolIcons.Download, progress < 0f) { withSlides { } }
                if (tiktok != null) Dest("TikTok", ToolIcons.Note, progress < 0f) { withSlides { Share.toApp(context, it, tiktok) } }
                Dest("Share", ToolIcons.ShareOut, progress < 0f) { withSlides { Share.anywhere(context, it) } }
                Dest("Video & more", ToolIcons.Video, true) { more = true }
            }
        }
    }

    if (more) ModalBottomSheet(onDismissRequest = { more = false }) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VideoExports(p)
            LongImage(p)
        }
    }
}

@Composable
private fun Dest(label: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The carousel inside an Instagram-like post frame: swipe it the way your followers will. */
@Composable
private fun InstagramPost(p: Project) {
    val app = LocalContext.current.app
    val pager = rememberPagerState { p.slides }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFF2A65A), Color(0xFFD4432A), Color(0xFF9B4DCA)))).padding(2.dp)) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.surface).padding(2.dp)) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)))
                }
            }
            Text("you", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 10.dp).weight(1f))
            Text("${pager.currentPage + 1}/${p.slides}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalPager(pager, Modifier.fillMaxWidth().aspectRatio(p.slideW.toFloat() / p.slideH), beyondViewportPageCount = 1) { i ->
            val bmp by produceState<Bitmap?>(null, p, i) { value = runCatching { app.exporter.renderSlide(p, i, 0.6f) }.getOrNull() }
            Box(Modifier.fillMaxSize().background(Color(p.theme.background.toInt()))) {
                bmp?.let { Image(it.asImageBitmap(), "Slide ${i + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
            repeat(minOf(p.slides, 10)) { i ->
                val on = i == pager.currentPage
                Box(Modifier.padding(horizontal = 2.dp).size(if (on) 6.dp else 5.dp).clip(CircleShape)
                    .background(if (on) Color(0xFF3897F0) else MaterialTheme.colorScheme.outlineVariant))
            }
        }
    }
}

@Composable
private fun LongImage(p: Project) {
    val app = LocalContext.current.app
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(0) }
    Text("One long image", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    when (state) {
        0 -> OutlinedButton(onClick = { state = 1; scope.launch { runCatching { app.exporter.exportPanorama(p) }; state = 2 } }, modifier = Modifier.fillMaxWidth()) {
            Icon(ToolIcons.Long, null, Modifier.size(18.dp)); Text("  Save the whole carousel as one picture")
        }
        1 -> LinearProgressIndicator(Modifier.fillMaxWidth())
        else -> Text("✓ Saved to Pictures/Unroll", style = MaterialTheme.typography.bodySmall)
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

    Text("Video", style = MaterialTheme.typography.titleSmall)
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
        }, modifier = Modifier.fillMaxWidth()) { Text("Make a Reel that scrolls across the carousel") }
    } else {
        Button(onClick = { Share.toInstagram(context, listOf(r)) }, modifier = Modifier.fillMaxWidth()) { Text("Post the Reel") }
        TextButton(onClick = { Share.anywhere(context, listOf(r)) }) { Text("Share it somewhere else") }
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
