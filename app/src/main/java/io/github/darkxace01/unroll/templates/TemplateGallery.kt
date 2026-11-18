package io.github.darkxace01.unroll.templates

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.darkxace01.unroll.UnrollApp
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.model.Category
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Ratio
import io.github.darkxace01.unroll.model.Template
import io.github.darkxace01.unroll.model.Templates
import io.github.darkxace01.unroll.model.Theme
import io.github.darkxace01.unroll.model.TripInfo
import io.github.darkxace01.unroll.render.Renderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

/** Sample words for previews. */
val previewInfo = TripInfo("Lisbon", "12–16 May", "Lisbon", 5)

/**
 * Template previews: built with painted sample photos and drawn off the main thread, each worker with
 * its own renderer, then kept in memory so scrolling back is instant.
 */
object Previews {
    private val cache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val renderers = ThreadLocal<Renderer>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pool = Dispatchers.Default.limitedParallelism(2)

    fun build(t: Template, theme: Theme = t.theme, ratio: Ratio = t.ratio): Project =
        Templates.build(t, emptyList(), previewInfo, "preview", t.name, 0, { java.util.UUID.randomUUID().toString() }, theme = theme, ratio = ratio)

    /** Slides [from] until [until] of the template side by side (with a hairline gap), at [scale]. */
    suspend fun strip(app: UnrollApp, t: Template, theme: Theme, ratio: Ratio, from: Int, until: Int, scale: Float): Bitmap {
        val key = "${t.id}|${theme.name}|${theme.accent}|${ratio.name}|$from|$until|$scale"
        cache.get(key)?.let { return it }
        return withContext(pool) {
            val r = renderers.get() ?: Renderer(app.fonts).also { renderers.set(it) }
            val p = build(t, theme, ratio)
            val end = minOf(until, p.slides)
            val n = (end - from).coerceAtLeast(1)
            val sw = (p.slideW * scale).toInt(); val sh = (p.slideH * scale).toInt()
            val gap = if (n > 1) (4 * scale / 0.2f).toInt().coerceAtLeast(2) else 0
            val bmp = Bitmap.createBitmap(sw * n + gap * (n - 1), sh, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            for (i in 0 until n) {
                c.save()
                c.translate(i * (sw + gap).toFloat(), 0f)
                c.clipRect(0, 0, sw, sh)
                c.scale(scale, scale)
                c.translate(-(from + i) * p.slideW.toFloat(), 0f)
                r.draw(c, p, { null }, samples = true)
                c.restore()
            }
            cache.put(key, bmp)
            bmp
        }
    }
}

/** A template card: its first two slides, name, size and a heart. */
@Composable
fun TemplateCard(t: Template, ratio: Ratio?, modifier: Modifier = Modifier, width: Dp? = null, onClick: () -> Unit) {
    val app = LocalContext.current.app
    val favs by app.favourites.ids.collectAsState()
    val r = ratio ?: t.ratio
    val img by produceState<Bitmap?>(null, t.id, r) { value = Previews.strip(app, t, t.theme, r, 0, 2, 0.14f) }
    val slides = remember(t.id, r) { Previews.build(t, t.theme, r).slides }
    Column(modifier.then(if (width != null) Modifier.width(width) else Modifier).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f * r.slideW / r.slideH + 0.02f)
                .clip(RoundedCornerShape(12.dp)).background(Color(t.theme.background.toInt())),
        ) {
            img?.let { Image(it.asImageBitmap(), t.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(30.dp).clip(CircleShape).background(Color(0x66000000))
                    .clickable { app.favourites.toggle(t.id) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (t.id in favs) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Save", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Text(t.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text("$slides slides · ${t.sample}+ photos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The template shop: search, format, saved, categories. With nothing chosen it shows a shelf per
 * category; choosing narrows to a grid.
 */
@Composable
fun TemplateGallery(onPick: (Template) -> Unit, modifier: Modifier = Modifier, maxHeight: Dp = Dp.Unspecified, onRatio: ((Ratio?) -> Unit)? = null) {
    val app = LocalContext.current.app
    val favs by app.favourites.ids.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    var saved by rememberSaveable { mutableStateOf(false) }
    var ratio by rememberSaveable { mutableStateOf<Ratio?>(null) }
    val all = Templates.everything
    val filtered = all.filter {
        (category == null || it.category == category) && (!saved || it.id in favs) && (query.isBlank() || it.matches(query))
    }
    val shelves = category == null && !saved && query.isBlank()
    LazyVerticalGrid(
        GridCells.Fixed(2),
        modifier.then(if (maxHeight != Dp.Unspecified) Modifier.heightIn(max = maxHeight) else Modifier),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Search ${all.size} templates: beach, film, wedding…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                shape = RoundedCornerShape(28.dp),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(saved, { saved = !saved }, { Text("♥ Saved") }) }
                items(listOf<Ratio?>(null) + Ratio.entries) { r ->
                    FilterChip(ratio == r, { ratio = r; onRatio?.invoke(r) }, { Text(r?.let { "${it.label} ${formatName(it)}" } ?: "Any format") })
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(category == null, { category = null }, { Text("All") }) }
                items(Category.entries.filter { c -> all.any { it.category == c } }) { c -> FilterChip(category == c, { category = if (category == c) null else c }, { Text(c.label) }) }
            }
        }
        if (shelves) {
            for (c in Category.entries) {
                val inCat = all.filter { it.category == c }
                if (inCat.isEmpty()) continue
                item(span = { GridItemSpan(maxLineSpan) }, key = "shelf-${c.name}") {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { category = c }) { Text("See all ${inCat.size}") }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(inCat.take(12), key = { it.id }) { t -> TemplateCard(t, ratio, width = 220.dp) { onPick(t) } }
                        }
                    }
                }
            }
        } else {
            if (filtered.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(if (saved) "Tap the heart on any template to keep it here." else "Nothing matches. Try another word.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(filtered, key = { it.id }) { t -> TemplateCard(t, ratio) { onPick(t) } }
        }
    }
}

fun formatName(r: Ratio) = when (r) { Ratio.Portrait -> "Portrait"; Ratio.Square -> "Square"; Ratio.Tall -> "Tall"; Ratio.Story -> "Story" }
