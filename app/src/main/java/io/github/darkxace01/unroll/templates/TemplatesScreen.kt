package io.github.darkxace01.unroll.templates

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Templates", style = MaterialTheme.typography.titleLarge)
            }
            TemplateGallery(onPick = { open = it }, modifier = Modifier.weight(1f))
        } else {
            BackHandler { open = null }
            TemplateDetail(
                t, busy,
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
    t: Template, busy: Boolean,
    onBack: () -> Unit, onOpen: (Template) -> Unit, onPhotos: (Theme, Ratio) -> Unit, onEmpty: (Theme, Ratio) -> Unit,
) {
    val app = LocalContext.current.app
    val favs by app.favourites.ids.collectAsState()
    var theme by remember(t.id) { mutableStateOf(t.theme) }
    var ratio by remember(t.id) { mutableStateOf(t.ratio) }
    val slides = remember(t.id, theme, ratio) { Previews.build(t, theme, ratio).slides }
    val pager = rememberPagerState { slides }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(t.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = { app.favourites.toggle(t.id) }) { Icon(if (t.id in favs) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Save") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Every slide, swipeable, the way it will look on Instagram; the next one peeks in.
            HorizontalPager(pager, Modifier.fillMaxWidth().padding(top = 4.dp), contentPadding = PaddingValues(start = 24.dp, end = 56.dp), pageSpacing = 10.dp) { i ->
                Box(Modifier.fillMaxWidth().aspectRatio(ratio.slideW.toFloat() / ratio.slideH).shadow(6.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))) {
                    SlideImage(t, theme, ratio, i, i + 1, Modifier.fillMaxSize())
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${pager.currentPage + 1} / $slides  ·  ${t.sample}+ photos  ·  grows to fit yours", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Dots(slides, pager.currentPage, light = false)
            }

            Label("Colours")
            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(listOf(t.theme) + Themes.all.filter { it.name != t.theme.name }) { th ->
                    val on = th == theme
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { theme = th }) {
                        Box(
                            Modifier.size(48.dp).border(if (on) 2.dp else 0.dp, if (on) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                .padding(if (on) 4.dp else 0.dp).clip(CircleShape).border(1.dp, Color(0x1A000000), CircleShape),
                        ) {
                            Row(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f).fillMaxSize().background(Color(th.background.toInt())))
                                Column(Modifier.weight(1f).fillMaxSize()) {
                                    Box(Modifier.weight(1f).fillMaxWidth().background(Color(th.accent.toInt())))
                                    Box(Modifier.weight(1f).fillMaxWidth().background(Color(th.ink.toInt())))
                                }
                            }
                        }
                        Text(if (th == t.theme) "Original" else th.name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            Label("Format")
            Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in Ratio.entries) {
                    val on = r == ratio
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                            .clickable { ratio = r }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val fg = if (on) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                        Box(Modifier.height(22.dp).aspectRatio(r.slideW.toFloat() / r.slideH).border(1.5.dp, fg, RoundedCornerShape(3.dp)))
                        Text(formatName(r), style = MaterialTheme.typography.labelSmall, color = fg, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            val similar = remember(t.id) {
                Templates.everything.filter { it.id != t.id && ((t.kit != null && it.kit == t.kit) || (t.layout != null && it.layout == t.layout) || it.category == t.category) }
                    .sortedBy { if (it.kit == t.kit) 0 else if (it.category == t.category) 1 else 2 }.take(10)
            }
            Label("More like this")
            LazyRow(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(similar, key = { it.id }) { s -> TemplateCard(s, Modifier.size(width = 150.dp, height = 240.dp), swipe = false) { onOpen(s) } }
            }
        }
        // The one thing to do here, always in reach.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onEmpty(theme, ratio) }, enabled = !busy) { Text("Empty slots") }
            Button(onClick = { onPhotos(theme, ratio) }, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp).padding(start = 8.dp)) {
                Text(if (busy) "Building…" else "Use with my photos")
            }
        }
    }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 10.dp))
