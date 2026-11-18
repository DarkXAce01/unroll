package io.github.darkxace01.unroll.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.darkxace01.unroll.model.Anim
import io.github.darkxace01.unroll.model.AnimKind
import io.github.darkxace01.unroll.model.Cap
import io.github.darkxace01.unroll.model.ColorRole
import io.github.darkxace01.unroll.model.Dash
import io.github.darkxace01.unroll.model.FontKey
import io.github.darkxace01.unroll.model.Item
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.RouteItem
import io.github.darkxace01.unroll.model.Theme
import io.github.darkxace01.unroll.model.Themes
import io.github.darkxace01.unroll.model.Vehicle
import io.github.darkxace01.unroll.ui.ChipRow
import io.github.darkxace01.unroll.ui.ColorRow
import io.github.darkxace01.unroll.ui.SectionLabel
import io.github.darkxace01.unroll.ui.SliderRow

@Composable
private fun Body(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) { content() }
}

/** Theme colours as chips: picking one ties the item to the theme, so it follows theme changes. */
@Composable
fun RoleRow(theme: Theme, selected: ColorRole?, onPick: (ColorRole) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorRole.entries.forEach { r ->
            FilterChip(
                selected = r == selected, onClick = { onPick(r) },
                label = { Text(r.label) },
                leadingIcon = { Box(Modifier.size(16.dp).clip(CircleShape).background(Color(theme.color(r).toInt())).border(1.dp, Color(0x33000000), CircleShape)) },
            )
        }
    }
}

/** A theme as a little strip of its colours. */
@Composable
private fun ThemeSwatch(t: Theme, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(76.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.clip(RoundedCornerShape(10.dp)).border(if (selected) 3.dp else 1.dp, if (selected) Color(0xFF2F6FED) else Color(0x22000000), RoundedCornerShape(10.dp)),
        ) {
            listOf(t.background, t.paper, t.ink, t.accent).forEach { c -> Box(Modifier.width(17.dp).height(44.dp).background(Color(c.toInt()))) }
        }
        Text(t.name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun ThemeSheet(p: Project, onTheme: (Theme) -> Unit, onColor: (ColorRole, Long) -> Unit, onFonts: (FontKey, FontKey) -> Unit) = Body {
    SectionLabel("Theme")
    Text("Everything tied to the theme changes together.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Themes.all.forEach { t -> ThemeSwatch(t, t.name == p.theme.name, onClick = { onTheme(t.copy()) }) }
    }
    ColorRole.entries.forEach { r ->
        SectionLabel(r.label)
        ColorRow(p.theme.color(r), { onColor(r, it) })
    }
    SectionLabel("Headline font")
    ChipRow(FontKey.entries, p.theme.head, { it.label }, { onFonts(it, p.theme.body) })
    SectionLabel("Body font")
    ChipRow(FontKey.entries, p.theme.body, { it.label }, { onFonts(p.theme.head, it) })
}

@Composable
fun RouteSheet(r: RouteItem, p: Project, selectedPoint: Int?, onPreview: (RouteItem) -> Unit, onDone: () -> Unit, onCommit: (RouteItem) -> Unit, onAddPoint: () -> Unit, onRemovePoint: (Int) -> Unit) = Body {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = onAddPoint) { Text("Add a bend") }
        OutlinedButton(onClick = { selectedPoint?.let(onRemovePoint) }, enabled = selectedPoint != null && r.points.size > 2) { Text("Remove bend") }
    }
    Text("Drag the dots on the route to bend it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    SectionLabel("Line")
    ChipRow(Dash.entries, r.dash, { it.label }, { onCommit(r.copy(dash = it)) })
    SliderRow("Thickness", r.width, 2f..40f, { onPreview(r.copy(width = it)) }, onDone)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Smooth curve", Modifier.weight(1f)); Switch(r.smooth, { onCommit(r.copy(smooth = it)) })
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Stops on the way", Modifier.weight(1f)); Switch(r.stops, { onCommit(r.copy(stops = it)) })
    }
    SectionLabel("Colour")
    RoleRow(p.theme, r.colorRole) { onCommit(r.copy(colorRole = it)) }
    ColorRow(if (r.colorRole == null) r.color else null, { onCommit(r.copy(color = it, colorRole = null)) }, Modifier.padding(top = 8.dp))
    SectionLabel("Start")
    ChipRow(Cap.entries, r.start, { it.label }, { onCommit(r.copy(start = it)) })
    SectionLabel("End")
    ChipRow(Cap.entries, r.end, { it.label }, { onCommit(r.copy(end = it)) })
    SectionLabel("On the route")
    ChipRow(Vehicle.entries, r.vehicle, { it.label }, { onCommit(r.copy(vehicle = it)) })
    if (r.vehicle != Vehicle.None) {
        SliderRow("Where", r.vehicleAt, 0f..1f, { onPreview(r.copy(vehicleAt = it)) }, onDone)
        SliderRow("Size", r.vehicleSize, 30f..240f, { onPreview(r.copy(vehicleSize = it)) }, onDone)
    }
}

@Composable
fun MotionSheet(item: Item, onPreview: (Anim?) -> Unit, onDone: () -> Unit, onCommit: (Anim?) -> Unit, onAll: () -> Unit, onNone: () -> Unit) = Body {
    val a = item.anim ?: Anim()
    SectionLabel("How it arrives in videos")
    Text("Plays in Reels videos and animated carousels. Photos you post as images stay still.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val kinds = if (item is RouteItem) listOf(AnimKind.None, AnimKind.Draw, AnimKind.Fade) else AnimKind.entries.filter { it != AnimKind.Draw && (it != AnimKind.Type || item is io.github.darkxace01.unroll.model.TextItem) }
    ChipRow(kinds, a.kind, { it.label }, { onCommit(a.copy(kind = it)) }, Modifier.padding(vertical = 8.dp))
    if (a.kind != AnimKind.None) {
        SliderRow("Wait", a.delay, 0f..3f, { onPreview(a.copy(delay = it)) }, onDone) { "%.1fs".format(it) }
        SliderRow("Length", a.duration, 0.2f..3f, { onPreview(a.copy(duration = it)) }, onDone) { "%.1fs".format(it) }
    }
    SectionLabel("Everything")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onAll) { Text("Animate everything") }
        OutlinedButton(onClick = onNone) { Text("Keep everything still") }
    }
}
