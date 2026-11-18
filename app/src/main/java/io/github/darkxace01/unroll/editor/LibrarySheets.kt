package io.github.darkxace01.unroll.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.model.Background
import io.github.darkxace01.unroll.model.ColorRole
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.FrameLibrary
import io.github.darkxace01.unroll.model.FramePreset
import io.github.darkxace01.unroll.model.Pattern
import io.github.darkxace01.unroll.model.PhotoGeometry
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.ShapeKind
import io.github.darkxace01.unroll.model.StickerDef
import io.github.darkxace01.unroll.model.StickerItem
import io.github.darkxace01.unroll.model.Stickers
import io.github.darkxace01.unroll.model.TripInfo
import io.github.darkxace01.unroll.render.Patterns
import io.github.darkxace01.unroll.ui.ColorRow
import io.github.darkxace01.unroll.ui.SectionLabel

/** One sticker drawn live, the way it will look on the canvas. */
@Composable
fun StickerThumb(def: StickerDef, p: Project, modifier: Modifier = Modifier, color: Long? = null, text: String? = null) {
    val app = LocalContext.current.app
    Canvas(modifier) {
        drawIntoCanvas { cv ->
            val c = cv.nativeCanvas
            val box = minOf(size.width, size.height) * 0.86f
            val (w, h) = if (def.aspect >= 1f) box to box / def.aspect else box * def.aspect to box
            val col = (color ?: def.color ?: def.colorRole?.let { p.theme.color(it) } ?: p.theme.accent).toInt()
            val words = Stickers.fill(text ?: def.text.orEmpty(), TripInfo(title = p.name, place = p.name))
            c.save()
            c.translate(size.width / 2, size.height / 2)
            app.renderer.stickerArt.draw(c, def, w, h, col, words, p.theme)
            c.restore()
        }
    }
}

/** The sticker library: search, packs, and the plain shapes. */
@Composable
fun StickerLibrarySheet(p: Project, onSticker: (StickerDef) -> Unit, onShape: (ShapeKind) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var pack by rememberSaveable { mutableStateOf(Stickers.packs.first().id) }
    val shapes = pack == "shapes"
    val shown = remember(query, pack) { if (query.isNotBlank()) Stickers.search(query) else if (shapes) emptyList() else Stickers.inPack(pack) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
        OutlinedTextField(
            query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(28.dp),
            placeholder = { Text("Search ${Stickers.all.size} stickers") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
        )
        LazyRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Stickers.packs) { k -> FilterChip(pack == k.id && query.isBlank(), { pack = k.id; query = "" }, { Text(k.name) }) }
            item { FilterChip(shapes && query.isBlank(), { pack = "shapes"; query = "" }, { Text("Shapes") }) }
        }
        if (shapes && query.isBlank()) {
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.height(380.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ShapeKind.entries) { k -> OutlinedButton(onClick = { onShape(k) }) { Text(k.label) } }
            }
        } else {
            if (shown.isEmpty()) Text("No stickers match.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
            LazyVerticalGrid(GridCells.Fixed(4), Modifier.height(420.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(shown, key = { it.id }) { d ->
                    StickerThumb(d, p, Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(Color(p.theme.background.toInt())).clickable { onSticker(d) })
                }
            }
        }
    }
}

/** A placed sticker: its words, colour and direction. */
@Composable
fun StickerItemSheet(s: StickerItem, p: Project, onCommit: (StickerItem) -> Unit, onLive: (StickerItem) -> Unit, onDone: () -> Unit) {
    val def = Stickers.byId(s.sticker) ?: return
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        StickerThumb(def, p, Modifier.fillMaxWidth().height(120.dp), color = s.color ?: s.colorRole?.let { p.theme.color(it) }, text = s.text)
        if (def.editable) {
            SectionLabel("Words")
            OutlinedTextField(s.text ?: def.text.orEmpty(), { onLive(s.copy(text = it)) }, Modifier.fillMaxWidth(), minLines = 1, maxLines = 3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDone) { Text("Done") }
                OutlinedButton(onClick = { onCommit(s.copy(text = Stickers.fill(def.text.orEmpty(), TripInfo(title = p.name, place = p.name)))) }) { Text("Reset words") }
            }
        }
        SectionLabel("Colour")
        RoleRow(p.theme, s.colorRole) { onCommit(s.copy(colorRole = it, color = null)) }
        ColorRow(s.color, { onCommit(s.copy(color = it, colorRole = null)) }, Modifier.padding(top = 8.dp))
        OutlinedButton(onClick = { onCommit(s.copy(color = null, colorRole = null)) }, Modifier.padding(top = 8.dp)) { Text("Sticker's own colour") }
        SectionLabel("Direction")
        OutlinedButton(onClick = { onCommit(s.copy(flip = !s.flip)) }) { Text("Flip") }
    }
}

/** A frame drawn on a painted sample photo. */
@Composable
fun FrameThumb(f: FramePreset, p: Project, modifier: Modifier = Modifier, index: Int = 0) {
    val app = LocalContext.current.app
    Canvas(modifier) {
        drawIntoCanvas { cv ->
            val a = PhotoGeometry.naturalAspect(f.kind) ?: 0.8f
            val box = minOf(size.width, size.height) * 0.8f
            val (w, h) = if (a >= 1f) box to box / a else box * a to box
            // Thumbnails are small: draw at 3x and scale down so borders keep their proportions.
            val k = 3f
            val c = cv.nativeCanvas
            c.save(); c.scale(1 / k, 1 / k)
            val item = PhotoItem("t", Frame(size.width / 2 * k, size.height / 2 * k, w * k, h * k), "", 100, 100, style = f.style(io.github.darkxace01.unroll.model.PhotoStyle(shadow = 10f)))
            app.renderer.drawPhoto(c, p, item, null, sample = index)
            c.restore()
        }
    }
}

/** Every frame, by shelf. [onPick] puts it on the selected photo or adds a new framed slot. */
@Composable
fun FrameLibrarySheet(p: Project, current: io.github.darkxace01.unroll.model.PhotoStyle?, onPick: (FramePreset) -> Unit, footer: @Composable () -> Unit = {}) {
    var group by rememberSaveable { mutableStateOf(FrameLibrary.groups.first()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
        LazyRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FrameLibrary.groups) { g -> FilterChip(group == g, { group = g }, { Text(g) }) }
        }
        val shown = FrameLibrary.all.filter { it.group == group }
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.height(400.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.id }) { f ->
                val on = current != null && current.frame == f.kind && (f.mount == null) == (current.mountRole == ColorRole.Paper) && (f.mount == null || f.mount == current.mountColor)
                Column(Modifier.clickable { onPick(f) }) {
                    FrameThumb(
                        f, p,
                        Modifier.fillMaxWidth().aspectRatio(1f)
                            .border(if (on) 3.dp else 0.dp, if (on) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(13.dp))
                            .padding(if (on) 5.dp else 0.dp).clip(RoundedCornerShape(10.dp)).background(Color(p.theme.background.toInt())),
                        FrameLibrary.all.indexOf(f),
                    )
                    Text(f.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp))
                }
            }
        }
        footer()
    }
}

/** Background textures as swatches. */
@Composable
fun PatternRow(bg: Background, p: Project, onPick: (Pattern) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Pattern.entries) { pt ->
            Column(Modifier.clickable { onPick(pt) }) {
                val on = bg.pattern == pt
                Canvas(Modifier.height(60.dp).aspectRatio(1f)
                    .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else Color(0x22000000), RoundedCornerShape(11.dp))
                    .padding(if (on) 5.dp else 1.dp).clip(RoundedCornerShape(8.dp))) {
                    drawIntoCanvas { cv ->
                        val c = cv.nativeCanvas
                        c.drawColor(p.color(bg.color, bg.colorRole).toInt())
                        c.save(); c.scale(0.4f, 0.4f)
                        Patterns.draw(c, pt, size.width / 0.4f, size.height / 0.4f, p.theme.ink.toInt(), p.theme.accent.toInt())
                        c.restore()
                    }
                }
                Text(pt.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
