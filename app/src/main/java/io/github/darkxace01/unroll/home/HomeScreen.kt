package io.github.darkxace01.unroll.home

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.data.NewProject
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.ui.maxPick
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(onOpen: (String) -> Unit, onSettings: () -> Unit, onTemplates: () -> Unit) {
    val app = LocalContext.current.app
    val scope = rememberCoroutineScope()
    val projects by app.store.projects.collectAsStateWithLifecycle()
    var busy by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxPick())) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            busy = true
            val p = NewProject.create(app, uris)
            busy = false
            onOpen(p.id)
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp)) {
            Text("Unroll", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).padding(top = 4.dp))
            TextButton(onClick = onSettings) { Text("Settings") }
        }
        Text(
            "Seamless carousels for Instagram. Free, no watermark.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy) {
                Text(if (busy) "Laying out…" else "New carousel")
            }
            OutlinedButton(onClick = onTemplates) { Text("Templates") }
            OutlinedButton(onClick = { scope.launch { onOpen(NewProject.blank(app).id) } }) { Text("Blank") }
        }
        if (projects.isEmpty()) {
            Text(
                "Pick photos from a trip, a day out, anything. Unroll lays them across slides that flow into each other when you swipe.",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        LazyVerticalGrid(
            GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(projects, key = { it.id }) { p ->
                var menu by remember { mutableStateOf(false) }
                Column(Modifier.combinedClickable(onClick = { onOpen(p.id) }, onLongClick = { menu = true })) {
                    Thumb(p)
                    Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                    Text("${p.slides} slides · ${p.ratio.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text("Duplicate") }, onClick = { menu = false; scope.launch { app.store.duplicate(p.id) } })
                        DropdownMenuItem({ Text("Delete") }, onClick = { menu = false; scope.launch { app.store.delete(p.id) } })
                    }
                }
            }
        }
    }
}

/** The first slide, small. Re-rendered when the project changes. */
@Composable
private fun Thumb(p: Project) {
    val app = LocalContext.current.app
    val bmp by produceState<Bitmap?>(null, p.updated) { value = runCatching { app.exporter.renderSlide(p, 0, 0.3f) }.getOrNull() }
    Box(Modifier.fillMaxWidth().aspectRatio(p.slideW.toFloat() / p.slideH).background(Color(0xFFE6E4DE))) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}
