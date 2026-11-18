package io.github.darkxace01.unroll.templates

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import io.github.darkxace01.unroll.render.SamplePhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/** Sample words for previews, suited to the kind of template, so a wedding doesn't say "Lisbon". */
fun previewInfoFor(c: Category) = when (c) {
    Category.Weddings -> TripInfo("Anna & Leo", "14.06.2026", "Lake Como", 5)
    Category.Love -> TripInfo("Us", "Since 2019", "Paris", 5)
    Category.Celebrations -> TripInfo("Happy birthday", "Turning 30", "Home", 5)
    Category.Recap -> TripInfo("November", "Nov 2026", "Lately", 5)
    Category.Fashion -> TripInfo("Lookbook", "Autumn edit", "Milan", 5)
    Category.Portraits -> TripInfo("Mila", "Best friend", "Home", 5)
    Category.Outdoor -> TripInfo("Patagonia", "3–10 March", "Patagonia", 8)
    Category.Editorial -> TripInfo("The weekend", "Issue 04", "Paris", 3)
    else -> TripInfo("Lisbon", "12–16 May", "Lisbon", 5)
}

/**
 * Template previews: built with real sample photos and drawn off the main thread, each worker with
 * its own renderer, then kept in memory so scrolling back is instant.
 */
object Previews {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 7).toInt().coerceAtMost(160 * 1024 * 1024)) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val renderers = ThreadLocal<Renderer>()
    private val built = LruCache<String, Project>(64)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pool = Dispatchers.Default.limitedParallelism(3)

    fun build(t: Template, theme: Theme = t.theme, ratio: Ratio = t.ratio): Project {
        val key = "${t.id}|${theme.name}|${theme.accent}|${ratio.name}"
        built.get(key)?.let { return it }
        return Templates.build(t, emptyList(), previewInfoFor(t.category), "preview", t.name, 0, { java.util.UUID.randomUUID().toString() }, theme = theme, ratio = ratio)
            .also { built.put(key, it) }
    }

    fun slides(t: Template, ratio: Ratio = t.ratio) = build(t, t.theme, ratio).slides

    /** The slide a card opens on: the title slide if it has a photo, else the most photo-filled of the first three. */
    fun cover(t: Template): Int {
        val p = build(t)
        val n = minOf(3, p.slides)
        val areas = (0 until n).map { i ->
            val l = i * p.slideW.toFloat(); val r = l + p.slideW
            p.items.filterIsInstance<io.github.darkxace01.unroll.model.PhotoItem>().sumOf { ph ->
                val b = ph.frame.bounds()
                val w = (minOf(b.right, r) - maxOf(b.left, l)).coerceAtLeast(0f)
                val h = (minOf(b.bottom, p.slideH.toFloat()) - maxOf(b.top, 0f)).coerceAtLeast(0f)
                (w * h).toDouble()
            } / (p.slideW.toDouble() * p.slideH)
        }
        return if (areas[0] >= 0.18) 0 else areas.indices.maxBy { areas[it] }
    }

    /** Slides [from] until [until] of the template side by side, seamless, at [scale]. */
    suspend fun strip(app: UnrollApp, t: Template, theme: Theme, ratio: Ratio, from: Int, until: Int, scale: Float): Bitmap {
        val key = "${t.id}|${theme.name}|${theme.accent}|${ratio.name}|$from|$until|$scale"
        cache.get(key)?.let { return it }
        return withContext(pool) {
            val r = renderers.get() ?: Renderer(app.fonts).also { renderers.set(it) }
            val p = build(t, theme, ratio)
            val end = minOf(until, p.slides)
            val n = (end - from).coerceAtLeast(1)
            val sw = (p.slideW * scale).toInt(); val sh = (p.slideH * scale).toInt()
            val bmp = Bitmap.createBitmap(sw * n, sh, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.scale(scale, scale)
            c.translate(-from * p.slideW.toFloat(), 0f)
            c.clipRect(from * p.slideW.toFloat(), 0f, (from + n) * p.slideW.toFloat(), p.slideH.toFloat())
            r.draw(c, p, { null }, samples = true, sampleOrder = SamplePhotos.order(t.id.hashCode(), SamplePhotos.tagsFor(t.category.name)))
            cache.put(key, bmp)
            bmp
        }
    }

    /** A render scale that gives [px] wide slides, rounded so nearby sizes share the cache. */
    fun scaleFor(px: Float, slideW: Int) = (ceil(px / slideW * 20f) / 20f).coerceIn(0.1f, 1f)
}

/** One rendered slide (or run of slides) of a template, sized to fill its box. */
@Composable
fun SlideImage(t: Template, theme: Theme, ratio: Ratio, from: Int, until: Int, modifier: Modifier = Modifier) {
    val app = LocalContext.current.app
    BoxWithConstraints(modifier.background(Color(theme.background.toInt()))) {
        val px = with(LocalDensity.current) { maxWidth.toPx() } / (until - from)
        val scale = Previews.scaleFor(px, ratio.slideW)
        val img by produceState<Bitmap?>(null, t.id, theme, ratio, from, until, scale) { value = Previews.strip(app, t, theme, ratio, from, until, scale) }
        img?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

@Composable
fun Dots(count: Int, current: Int, modifier: Modifier = Modifier, light: Boolean = true) {
    if (count < 2) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(minOf(count, 8)) { i ->
            val on = i == minOf(current, 7)
            val c = if (light) Color.White.copy(alpha = if (on) 1f else 0.55f)
            else if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
            Box(Modifier.size(if (on) 6.dp else 5.dp).clip(CircleShape).background(c))
        }
    }
}

@Composable
private fun Heart(t: Template, modifier: Modifier) {
    val app = LocalContext.current.app
    val favs by app.favourites.ids.collectAsState()
    Box(
        modifier.size(32.dp).clip(CircleShape).background(Color(0x40000000)).clickable { app.favourites.toggle(t.id) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (t.id in favs) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Save", tint = Color.White, modifier = Modifier.size(17.dp))
    }
}

/** A template card: its cover slide, swipe to see the rest; name and size underneath. */
@Composable
fun TemplateCard(t: Template, modifier: Modifier = Modifier, swipe: Boolean = true, onClick: () -> Unit) {
    val r = t.ratio
    val slides = remember(t.id) { Previews.slides(t) }
    val cover = remember(t.id) { Previews.cover(t) }
    val pager = rememberPagerState(initialPage = if (swipe) cover else 0) { if (swipe) slides else 1 }
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(r.slideW.toFloat() / r.slideH)
                .shadow(3.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        ) {
            HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 0) { page ->
                val i = if (swipe) page else cover
                SlideImage(t, t.theme, r, i, i + 1, Modifier.fillMaxSize())
            }
            if (swipe) Dots(slides, pager.currentPage, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
            Heart(t, Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Text(t.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
        Text("$slides slides · ${t.sample}+ photos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 2.dp))
    }
}

/** The big banner: a whole seamless carousel at a glance. */
@Composable
private fun FeaturedPager(featured: List<Template>, onPick: (Template) -> Unit) {
    val pager = rememberPagerState { featured.size }
    Column {
        HorizontalPager(pager, contentPadding = PaddingValues(end = 28.dp), pageSpacing = 12.dp) { i ->
            val t = featured[i]
            val shown = minOf(3, Previews.slides(t))
            Column(Modifier.clickable { onPick(t) }) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(shown * t.ratio.slideW.toFloat() / t.ratio.slideH)
                        .shadow(4.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)),
                ) {
                    SlideImage(t, t.theme, t.ratio, 0, shown, Modifier.fillMaxSize())
                    // Faint hairlines where Instagram cuts, so it reads as a carousel.
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        for (k in 1 until shown) {
                            val x = size.width * k / shown
                            drawLine(Color(0x40FFFFFF), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1.dp.toPx())
                        }
                    }
                }
                Row(Modifier.padding(top = 10.dp, start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name, style = MaterialTheme.typography.titleSmall)
                        Text("${Previews.slides(t)} slides · seamless", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Dots(featured.size, pager.currentPage, Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp), light = false)
    }
}

/** Hand-picked for the banner; the ones that show off seamless carousels best. */
private val featuredIds = listOf("panorama-title", "postcard-route", "k-polaroid-travel-scatter", "k-film-noir-strip", "scrapbook", "k-wedding-hero", "k-beach-days-seam")

/** Every template, mixed so neighbours differ: hand-made first, then one of each kit in turn. */
private val forYou: List<Template> by lazy {
    val (hand, kits) = Templates.everything.partition { it.kit == null }
    val byKit = kits.groupBy { it.kit }.values.map { it.toMutableList() }
    buildList {
        addAll(hand)
        var round = 0
        while (byKit.any { it.isNotEmpty() }) {
            for (k in byKit) if (k.isNotEmpty()) add(k.removeAt((round * 5) % k.size))
            round++
        }
    }
}

/**
 * The template shop as one calm vertical feed. A single header row holds search, saved and the
 * categories; "For you" opens with a featured banner, then every template two by two.
 */
@Composable
fun TemplateGallery(onPick: (Template) -> Unit, modifier: Modifier = Modifier, height: Dp = Dp.Unspecified) {
    val app = LocalContext.current.app
    val favs by app.favourites.ids.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    var saved by rememberSaveable { mutableStateOf(false) }
    val all = Templates.everything
    val shown = when {
        searching && query.isNotBlank() -> all.filter { it.matches(query) }
        saved -> all.filter { it.id in favs }
        category != null -> all.filter { it.category == category }
        else -> forYou
    }
    val home = !saved && category == null && !(searching && query.isNotBlank())
    val grid = rememberLazyStaggeredGridState()
    LaunchedEffect(category, saved, query) { grid.scrollToItem(0) }

    Column(modifier.then(if (height != Dp.Unspecified) Modifier.height(height) else Modifier)) {
        AnimatedContent(searching, label = "search") { s ->
            if (s) SearchRow(query, { query = it }, onClose = { searching = false; query = "" }, all.size)
            else LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp),
            ) {
                item {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)).clickable { searching = true; saved = false },
                        contentAlignment = Alignment.Center) { Icon(Icons.Filled.Search, "Search", Modifier.size(20.dp)) }
                }
                item {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(if (saved) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                        .clickable { saved = !saved; if (saved) category = null }, contentAlignment = Alignment.Center) {
                        Icon(if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Saved", Modifier.size(19.dp),
                            tint = if (saved) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface)
                    }
                }
                item { Pill("For you", !saved && category == null) { category = null; saved = false } }
                items(Category.entries.filter { c -> all.any { it.category == c } }) { c ->
                    Pill(c.label, !saved && category == c) { category = c; saved = false }
                }
            }
        }
        LazyVerticalStaggeredGrid(
            StaggeredGridCells.Fixed(2), Modifier.fillMaxWidth().weight(1f), state = grid,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalItemSpacing = 20.dp, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (home) {
                val featured = featuredIds.mapNotNull { Templates.byId(it) }
                item(span = StaggeredGridItemSpan.FullLine, key = "featured") {
                    FeaturedPager(featured, onPick)
                }
                item(span = StaggeredGridItemSpan.FullLine, key = "all") {
                    Text("All ${all.size} templates", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
                }
            } else if (shown.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        if (saved) "Tap the heart on any template to keep it here." else "Nothing matches. Try another word.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
            items(shown, key = { it.id }) { t -> TemplateCard(t) { onPick(t) } }
        }
    }
}

@Composable
private fun Pill(label: String, on: Boolean, onClick: () -> Unit) = FilterChip(
    on, onClick, { Text(label) }, shape = CircleShape,
    colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.onSurface, selectedLabelColor = MaterialTheme.colorScheme.surface,
    ),
    border = if (on) null else FilterChipDefaults.filterChipBorder(true, false),
)

@Composable
private fun SearchRow(query: String, onQuery: (String) -> Unit, onClose: () -> Unit, total: Int) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(44.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)).padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            if (query.isEmpty()) Text("Search $total templates", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                query, onQuery, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface), modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close search") }
    }
}

fun formatName(r: Ratio) = when (r) { Ratio.Portrait -> "Portrait"; Ratio.Square -> "Square"; Ratio.Tall -> "Tall"; Ratio.Story -> "Story" }
