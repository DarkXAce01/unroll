package io.github.darkxace01.unroll.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.darkxace01.unroll.R
import io.github.darkxace01.unroll.data.Settings
import io.github.darkxace01.unroll.model.Adjust
import io.github.darkxace01.unroll.model.Arrange
import io.github.darkxace01.unroll.model.Background
import io.github.darkxace01.unroll.model.BackgroundKind
import io.github.darkxace01.unroll.model.Filter
import io.github.darkxace01.unroll.model.FontKey
import io.github.darkxace01.unroll.model.Grid
import io.github.darkxace01.unroll.model.MAX_SLIDES
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Ratio
import io.github.darkxace01.unroll.model.ShapeItem
import io.github.darkxace01.unroll.model.ShapeKind
import io.github.darkxace01.unroll.model.TextAlign
import io.github.darkxace01.unroll.model.TextItem
import io.github.darkxace01.unroll.ui.ChipRow
import io.github.darkxace01.unroll.ui.ColorRow
import io.github.darkxace01.unroll.ui.SectionLabel
import io.github.darkxace01.unroll.ui.SliderRow

@Composable
private fun SheetBody(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) { content() }
}

@Composable
fun LayoutSheet(
    p: Project,
    slide: Int,
    settings: Settings,
    onArrange: (Arrange, Boolean) -> Unit,
    onGrid: (Grid) -> Unit,
    onSpacing: (margin: Float, gap: Float) -> Unit,
    onRatio: (Ratio) -> Unit,
    onInsert: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) = SheetBody {
    SectionLabel("Arrange all photos")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = { onArrange(Arrange.Auto, false) }, label = { Text("Seamless") })
        AssistChip(onClick = { onArrange(Arrange.Auto, true) }, label = { Text("Shuffle") })
        AssistChip(onClick = { onArrange(Arrange.OnePerSlide, false) }, label = { Text("One per slide") })
        AssistChip(onClick = { onArrange(Arrange.Panorama, false) }, label = { Text("Panorama") })
    }
    var margin by remember { mutableStateOf(settings.margin) }
    var gap by remember { mutableStateOf(settings.gap) }
    SliderRow("Edge", margin, 0f..160f, { margin = it }, { onSpacing(margin, gap) }) { "${it.toInt()}" }
    SliderRow("Between photos", gap, 0f..120f, { gap = it }, { onSpacing(margin, gap) }) { "${it.toInt()}" }

    SectionLabel("Slide ${slide + 1}: grid")
    ChipRow(Grid.entries, null, { it.label }, onGrid)

    SectionLabel("Shape")
    ChipRow(Ratio.entries, p.ratio, { it.label }, onRatio)

    SectionLabel("Slides (${p.slides} of $MAX_SLIDES)")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onInsert(slide) }, enabled = p.slides < MAX_SLIDES) { Text("Add after ${slide + 1}") }
        OutlinedButton(onClick = { onRemove(slide) }, enabled = p.slides > 1) { Text("Remove ${slide + 1}") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { onMove(slide, slide - 1) }, enabled = slide > 0) { Text("Move left") }
        TextButton(onClick = { onMove(slide, slide + 1) }, enabled = slide < p.slides - 1) { Text("Move right") }
        TextButton(onClick = { onInsert(-1) }, enabled = p.slides < MAX_SLIDES) { Text("Add at start") }
    }
}

@Composable
fun BackgroundSheet(bg: Background, photoColors: List<Long>, onPreview: (Background) -> Unit, onDone: (Background) -> Unit) = SheetBody {
    SectionLabel("Background")
    ChipRow(BackgroundKind.entries, bg.kind, { if (it == BackgroundKind.Solid) "Colour" else "Gradient" }, { onDone(bg.copy(kind = it)) })
    if (photoColors.isNotEmpty()) {
        SectionLabel("From your photos")
        ColorRow(bg.color, { onDone(bg.copy(color = it)) }, colors = photoColors)
    }
    SectionLabel(if (bg.kind == BackgroundKind.Gradient) "From" else "Colour")
    ColorRow(bg.color, { onDone(bg.copy(color = it)) })
    if (bg.kind == BackgroundKind.Gradient) {
        SectionLabel("To")
        ColorRow(bg.color2, { onDone(bg.copy(color2 = it)) })
        SliderRow("Direction", bg.angle, 0f..360f, { onPreview(bg.copy(angle = it)) }, { onDone(bg) }) { "${it.toInt()}°" }
    }
}

@Composable
fun AdjustSheet(item: PhotoItem, onPreview: (PhotoItem) -> Unit, onDone: () -> Unit, onCommit: (PhotoItem) -> Unit) = SheetBody {
    val a = item.adjust
    fun set(n: Adjust) = onPreview(item.copy(adjust = n))
    SectionLabel("Filter")
    ChipRow(Filter.entries, a.filter, { it.label }, { onCommit(item.copy(adjust = a.copy(filter = it, filterStrength = 1f))) })
    if (a.filter != Filter.None) SliderRow("Strength", a.filterStrength, 0f..1f, { set(a.copy(filterStrength = it)) }, onDone)
    SectionLabel("Light & colour")
    SliderRow("Exposure", a.exposure, -1f..1f, { set(a.copy(exposure = it)) }, onDone)
    SliderRow("Contrast", a.contrast, -1f..1f, { set(a.copy(contrast = it)) }, onDone)
    SliderRow("Saturation", a.saturation, -1f..1f, { set(a.copy(saturation = it)) }, onDone)
    SliderRow("Warmth", a.warmth, -1f..1f, { set(a.copy(warmth = it)) }, onDone)
    SliderRow("Tint", a.tint, -1f..1f, { set(a.copy(tint = it)) }, onDone)
    SliderRow("Fade", a.fade, 0f..1f, { set(a.copy(fade = it)) }, onDone)
    SliderRow("Vignette", a.vignette, 0f..1f, { set(a.copy(vignette = it)) }, onDone)
    TextButton(onClick = { onCommit(item.copy(adjust = Adjust())) }, enabled = !a.isIdentity) { Text("Reset") }
}

@Composable
fun PhotoStyleSheet(item: PhotoItem, onPreview: (PhotoItem) -> Unit, onDone: () -> Unit, onCommit: (PhotoItem) -> Unit, onReplace: () -> Unit) = SheetBody {
    val s = item.style
    SectionLabel("Frame")
    SliderRow("Rounded corners", s.cornerRadius, 0f..300f, { onPreview(item.copy(style = s.copy(cornerRadius = it))) }, onDone) { "${it.toInt()}" }
    SliderRow("Border", s.borderWidth, 0f..60f, { onPreview(item.copy(style = s.copy(borderWidth = it))) }, onDone) { "${it.toInt()}" }
    if (s.borderWidth > 0f) ColorRow(s.borderColor, { onCommit(item.copy(style = s.copy(borderColor = it))) })
    SliderRow("Shadow", s.shadow, 0f..80f, { onPreview(item.copy(style = s.copy(shadow = it))) }, onDone) { "${it.toInt()}" }
    SliderRow("Opacity", s.opacity, 0.05f..1f, { onPreview(item.copy(style = s.copy(opacity = it))) }, onDone)
    SectionLabel("Photo")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            // Frame takes the photo's own shape, same area.
            val f = item.frame
            val area = f.w * f.h
            val h = kotlin.math.sqrt(area / item.aspect)
            onCommit(item.copy(frame = f.copy(w = h * item.aspect, h = h), crop = item.crop.copy(zoom = 1f, panX = 0f, panY = 0f)))
        }) { Text("Fit photo shape") }
        OutlinedButton(onClick = { onCommit(item.copy(crop = item.crop.copy(flipX = !item.crop.flipX))) }) { Text("Flip") }
        OutlinedButton(onClick = onReplace) { Text("Replace") }
    }
}

private val fontFamilies = mapOf(
    FontKey.Sans to FontFamily.SansSerif,
    FontKey.Serif to FontFamily.Serif,
    FontKey.Mono to FontFamily.Monospace,
    FontKey.Condensed to FontFamily(Font(R.font.archivo_condensed)),
    FontKey.Display to FontFamily(Font(R.font.anybody)),
    FontKey.Elegant to FontFamily(Font(R.font.instrument_serif)),
    FontKey.ElegantItalic to FontFamily(Font(R.font.instrument_serif_italic)),
    FontKey.Hand to FontFamily(Font(R.font.reenie_beanie)),
    FontKey.Pixel to FontFamily(Font(R.font.vt323)),
    FontKey.Grotesk to FontFamily(Font(R.font.bricolage)),
)

@Composable
fun TextSheet(t: TextItem, onLive: (TextItem) -> Unit, onCommit: (TextItem) -> Unit, onDone: () -> Unit) = SheetBody {
    OutlinedTextField(
        value = t.text,
        onValueChange = { onLive(t.copy(text = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Text") },
        minLines = 1,
        maxLines = 5,
    )
    SectionLabel("Font")
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 200.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(FontKey.entries) { f ->
                FilterChip(selected = t.font == f, onClick = { onCommit(t.copy(font = f)) }, label = { Text(f.label, fontFamily = fontFamilies[f]) })
            }
        }
    }
    SliderRow("Size", t.size, 24f..400f, { onLive(t.copy(size = it)) }, onDone) { "${it.toInt()}" }
    SectionLabel("Colour")
    ColorRow(t.color, { onCommit(t.copy(color = it)) })
    SectionLabel("Style")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChipRow(TextAlign.entries, t.align, { it.name.replace("Start", "Left").replace("End", "Right") }, { onCommit(t.copy(align = it)) })
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Bold", Modifier.weight(1f)); Switch(t.bold, { onCommit(t.copy(bold = it)) })
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Label behind", Modifier.weight(1f)); Switch(t.pillColor != null, { onCommit(t.copy(pillColor = if (it) 0xFFFFFFFF else null)) })
    }
    if (t.pillColor != null) ColorRow(t.pillColor, { onCommit(t.copy(pillColor = it)) })
    SliderRow("Letter spacing", t.letterSpacing, -0.1f..0.5f, { onLive(t.copy(letterSpacing = it)) }, onDone) { "%.2f".format(it) }
    SliderRow("Line height", t.lineHeight, 0.7f..2f, { onLive(t.copy(lineHeight = it)) }, onDone) { "%.2f".format(it) }
    SliderRow("Outline", t.outlineWidth, 0f..12f, { onLive(t.copy(outlineWidth = it)) }, onDone) { "${it.toInt()}" }
    if (t.outlineWidth > 0f) ColorRow(t.outlineColor, { onCommit(t.copy(outlineColor = it)) })
    SliderRow("Shadow", t.shadow, 0f..40f, { onLive(t.copy(shadow = it)) }, onDone) { "${it.toInt()}" }
}

@Composable
fun StickerSheet(onPick: (ShapeKind) -> Unit) = SheetBody {
    SectionLabel("Shapes & stickers")
    LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 300.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ShapeKind.entries) { k -> OutlinedButton(onClick = { onPick(k) }) { Text(k.label) } }
    }
}

@Composable
fun ShapeSheet(s: ShapeItem, onPreview: (ShapeItem) -> Unit, onDone: () -> Unit, onCommit: (ShapeItem) -> Unit) = SheetBody {
    SectionLabel("Colour")
    ColorRow(s.color, { onCommit(s.copy(color = it)) })
    if (s.kind == ShapeKind.Rect || s.kind == ShapeKind.Circle || s.kind == ShapeKind.Heart || s.kind == ShapeKind.Star) {
        SliderRow("Outline only", s.stroke, 0f..40f, { onPreview(s.copy(stroke = it)) }, onDone) { if (it == 0f) "Filled" else "${it.toInt()}" }
    }
}
