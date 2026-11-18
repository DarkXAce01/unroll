package io.github.darkxace01.unroll.preview

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.model.Project

/**
 * The carousel as Instagram shows it: one slide at a time across the full width, swiped, with the dots.
 * Below, how the first slide sits in the profile grid (3:4 tiles since 2025).
 */
@Composable
fun PreviewScreen(p: Project, onBack: () -> Unit, onExport: () -> Unit) {
    val app = LocalContext.current.app
    val slides by produceState<List<Bitmap>>(emptyList(), p) {
        val cache = HashMap<String, Bitmap?>()
        val out = mutableListOf<Bitmap>()
        for (i in 0 until p.slides) { out += app.exporter.renderSlide(p, i, 0.75f, cache); value = out.toList() }
    }
    val pager = rememberPagerState { p.slides }
    var playing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Preview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { playing = !playing }) { Text(if (playing) "Carousel" else "Play video") }
            TextButton(onClick = onExport) { Text("Export") }
        }
        // A feed post header, so it reads as a real post.
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFDDDDDD)))
            Text("you", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 10.dp))
        }
        if (playing) {
            ScrollPlayer(p)
            Text("How the Reel / scroll video plays. Export saves it as an MP4.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            return@Column
        }
        HorizontalPager(pager, Modifier.fillMaxWidth().aspectRatio(p.slideW.toFloat() / p.slideH)) { i ->
            val b = slides.getOrNull(i)
            Box(Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
                if (b != null) Image(b.asImageBitmap(), "Slide ${i + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            }
        }
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.Center) {
            repeat(p.slides) { i ->
                Box(Modifier.padding(2.dp).size(6.dp).clip(CircleShape).background(if (i == pager.currentPage) Color(0xFF2F6FED) else Color(0xFFCCCCCC)))
            }
        }
        Text("${pager.currentPage + 1} / ${p.slides}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp))
        GridTile(p, slides.firstOrNull())
    }
}

@Composable
private fun GridTile(p: Project, first: Bitmap?) {
    Column(Modifier.padding(12.dp)) {
        Text("On your profile", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            // Profile tiles are 3:4; a 4:5 or 1:1 post is cropped from the middle.
            Box(Modifier.width(100.dp).aspectRatio(3f / 4f).background(Color(0xFFEEEEEE))) {
                if (first != null) Image(first.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            repeat(2) { Box(Modifier.width(100.dp).aspectRatio(3f / 4f).background(Color(0xFFF2F2F2))) }
        }
    }
}
