package io.github.darkxace01.unroll.ui

import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Plain Material for now; the Grease Pencil Studio look comes in the UI phase. */
@Composable
fun UnrollTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1D1E22),
            onPrimary = Color.White,
            secondary = Color(0xFFD4432A),
            background = Color(0xFFF4F3EF),
            surface = Color(0xFFF4F3EF),
            onBackground = Color(0xFF1D1E22),
            onSurface = Color(0xFF1D1E22),
            onSurfaceVariant = Color(0xFF6A6B70),
            // Every container neutral and warm, so menus, sheets and chips never pick up Material's lavender.
            surfaceVariant = Color(0xFFE9E7E1),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFFAF9F6),
            surfaceContainerHigh = Color(0xFFFAF9F6),
            surfaceContainerHighest = Color(0xFFEDEBE6),
            surfaceTint = Color.Transparent,
            primaryContainer = Color(0xFFE9E7E1),
            onPrimaryContainer = Color(0xFF1D1E22),
            secondaryContainer = Color(0xFFE9E7E1),
            onSecondaryContainer = Color(0xFF1D1E22),
            outline = Color(0xFFC9C7C1),
            outlineVariant = Color(0xFFDEDCD6),
        ),
        content = content,
    )
}

/** Colours offered everywhere a colour is picked. */
val Swatches: List<Long> = listOf(
    0xFFFFFFFF, 0xFFF4F3EF, 0xFFE8E2D6, 0xFFD9CBB0, 0xFFB08D6A, 0xFF6B4F3A,
    0xFF1D1E22, 0xFF55575F, 0xFFA3A5AB, 0xFFD4432A, 0xFFE8875C, 0xFFF2B33D,
    0xFF8DB580, 0xFF3F7D58, 0xFF7FB7C9, 0xFF2F6FED, 0xFF1B2142, 0xFFB78FD6,
    0xFFF1B7C3, 0xFF000000,
)

@Composable
fun ColorRow(selected: Long?, onPick: (Long) -> Unit, modifier: Modifier = Modifier, colors: List<Long> = Swatches) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { c ->
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(c.toInt()))
                    .border(if (c == selected) 3.dp else 1.dp, if (c == selected) Color(0xFF2F6FED) else Color(0x33000000), CircleShape)
                    .clickable { onPick(c) },
            )
        }
    }
}

@Composable
fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, onDone: () -> Unit = {}, format: (Float) -> String = { pct(it, range) }) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(format(value), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, onValueChangeFinished = onDone)
    }
}

private fun pct(v: Float, range: ClosedFloatingPointRange<Float>): String =
    if (range.start < 0f) String.format(Locale.US, "%+d", (v * 100).toInt()) else "${(v * 100 / range.endInclusive.coerceAtLeast(0.0001f)).toInt()}"

@Composable
fun <T> ChipRow(options: List<T>, selected: T?, label: (T) -> String, onPick: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> FilterChip(selected = o == selected, onClick = { onPick(o) }, label = { Text(label(o)) }) }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
}

@Composable
fun Gap(w: Int = 8) = Box(Modifier.width(w.dp))

/** How many photos the system picker may return at once. */
fun maxPick(): Int = if (Build.VERSION.SDK_INT >= 33) minOf(60, MediaStore.getPickImagesMaxLimit()) else 60
