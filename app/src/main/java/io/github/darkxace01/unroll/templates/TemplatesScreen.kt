package io.github.darkxace01.unroll.templates

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.data.NewProject
import io.github.darkxace01.unroll.model.Ratio
import io.github.darkxace01.unroll.model.Template
import io.github.darkxace01.unroll.model.Templates
import io.github.darkxace01.unroll.model.Theme
import io.github.darkxace01.unroll.model.Themes
import io.github.darkxace01.unroll.ui.maxPick
import kotlinx.coroutines.launch

/** Browse templates, open one to see every slide, remix its colours and format, then use it. */
@Composable
fun TemplatesScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val app = LocalContext.current.app
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf<Template?>(null) }
    var ratio by remember { mutableStateOf<Ratio?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Triple<Template, Theme, Ratio>?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxPick())) { uris ->
        val (t, th, r) = pending ?: return@rememberLauncherForActivityResult
        if (uris.isNotEmpty()) scope.launch {
            busy = true
            onOpen(NewProject.fromTemplate(app, t, uris, th, r).id)
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        val t = open
        if (t == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Templates", style = MaterialTheme.typography.titleLarge)
            }
            TemplateGallery(onPick = { open = it }, onRatio = { ratio = it })
        } else {
            BackHandler { open = null }
            TemplateDetail(
                t, ratio ?: t.ratio, busy,
                onBack = { open = null },
                onOpen = { open = it },
                onPhotos = { th, r -> pending = Triple(t, th, r); pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onEmpty = { th, r -> scope.launch { busy = true; onOpen(NewProject.fromTemplate(app, t, emptyList(), th, r).id) } },
            )
        }
    }
}

@Composable
private fun TemplateDetail(
    t: Template, startRatio: Ratio, busy: Boolean,
    onBack: () -> Unit, onOpen: (Template) -> Unit, onPhotos: (Theme, Ratio) -> Unit, onEmpty: (Theme, Ratio) -> Unit,
) {
    val app = LocalContext.current.app
    val favs by app.favourites.ids.collectAsState()
    var theme by remember(t.id) { mutableStateOf(t.theme) }
    var ratio by remember(t.id) { mutableStateOf(startRatio) }
    val slides = remember(t.id, ratio) { Previews.build(t, theme, ratio).slides }
    val pager = rememberPagerState { slides }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(t.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { app.favourites.toggle(t.id) }) { Icon(if (t.id in favs) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Save") }
        }
        // Every slide, swipeable, the way it will look on Instagram.
        HorizontalPager(pager, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 40.dp), pageSpacing = 12.dp) { i ->
            val img by produceState<Bitmap?>(null, t.id, theme, ratio, i) { value = Previews.strip(app, t, theme, ratio, i, i + 1, 0.42f) }
            Box(Modifier.fillMaxWidth().aspectRatio(ratio.slideW.toFloat() / ratio.slideH).clip(RoundedCornerShape(14.dp)).background(Color(theme.background.toInt()))) {
                img?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
            repeat(slides) { i ->
                Box(Modifier.padding(3.dp).size(if (i == pager.currentPage) 8.dp else 6.dp).clip(CircleShape)
                    .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Text("$slides slides · grows to fit any number of photos · everything stays editable", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

        Text("Colours", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 20.dp, top = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(listOf(t.theme) + Themes.all.filter { it.name != t.theme.name }) { th ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { theme = th }) {
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .border(if (th == theme) 2.dp else 0.dp, if (th == theme) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp)),
                    ) {
                        for (c in listOf(th.background, th.paper, th.ink, th.accent)) Box(Modifier.size(14.dp, 34.dp).background(Color(c.toInt())))
                    }
                    Text(if (th == t.theme) "Original" else th.name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text("Format", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 20.dp, top = 4.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Ratio.entries) { r -> FilterChip(r == ratio, { ratio = r }, { Text("${r.label} ${formatName(r)}") }) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onPhotos(theme, ratio) }, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp)) { Text(if (busy) "Building…" else "Use with my photos") }
            OutlinedButton(onClick = { onEmpty(theme, ratio) }, enabled = !busy, modifier = Modifier.height(52.dp)) { Text("Empty slots") }
        }
        val similar = remember(t.id) {
            Templates.everything.filter { it.id != t.id && ((t.kit != null && it.kit == t.kit) || (t.layout != null && it.layout == t.layout) || it.category == t.category) }
                .sortedBy { if (it.kit == t.kit) 0 else if (it.category == t.category) 1 else 2 }.take(16)
        }
        Text("Similar templates", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 20.dp, top = 8.dp))
        LazyRow(contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(similar, key = { it.id }) { s -> TemplateCard(s, ratio, width = 200.dp) { onOpen(s) } }
        }
    }
}
