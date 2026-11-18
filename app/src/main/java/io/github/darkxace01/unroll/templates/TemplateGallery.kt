package io.github.darkxace01.unroll.templates

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.model.Category
import io.github.darkxace01.unroll.model.Template
import io.github.darkxace01.unroll.model.Templates
import io.github.darkxace01.unroll.model.TripInfo
import io.github.darkxace01.unroll.ui.ChipRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Every template, each shown as a strip of its first slides. Tap one to use it. */
@Composable
fun TemplateGallery(onPick: (Template) -> Unit, modifier: Modifier = Modifier, maxHeight: Dp = Dp.Unspecified) {
    var category by remember { mutableStateOf<Category?>(null) }
    val shown = Templates.all.filter { category == null || it.category == category }
    LazyVerticalGrid(
        GridCells.Fixed(1),
        modifier.then(if (maxHeight != Dp.Unspecified) Modifier.heightIn(max = maxHeight) else Modifier),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ChipRow(listOf<Category?>(null) + Category.entries, category, { it?.label ?: "All" }, { category = it })
        }
        items(shown, key = { it.id }) { t -> Card(t) { onPick(t) } }
    }
}

@Composable
private fun Card(t: Template, onClick: () -> Unit) {
    val app = LocalContext.current.app
    val strip by produceState<Bitmap?>(null, t.id) {
        value = withContext(Dispatchers.Default) {
            val p = Templates.build(t, emptyList(), TripInfo("Lisbon", "12–16 May", "Lisbon", 5), "preview", t.name, 0, { java.util.UUID.randomUUID().toString() })
            val n = minOf(3, p.slides)
            val scale = 0.16f
            val bmp = Bitmap.createBitmap((p.slideW * n * scale).toInt(), (p.slideH * scale).toInt(), Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.scale(scale, scale)
            app.renderer.draw(c, p, { null })
            bmp
        }
    }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(if (t.ratio.slideH > 1500) 1.7f else 3f * t.ratio.slideW / t.ratio.slideH)
                .clip(RoundedCornerShape(10.dp)).background(Color(t.theme.background.toInt())),
        ) {
            strip?.let { Image(it.asImageBitmap(), t.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        Text(t.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
        Text("${t.category.label} · ${t.ratio.label} · ${t.theme.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
