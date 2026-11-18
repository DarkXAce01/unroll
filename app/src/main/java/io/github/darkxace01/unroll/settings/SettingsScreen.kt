package io.github.darkxace01.unroll.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darkxace01.unroll.BuildConfig
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.support.SupportLine
import io.github.darkxace01.unroll.ui.ChipRow
import io.github.darkxace01.unroll.ui.SectionLabel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val s by app.prefs.settings.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            SectionLabel("Editing")
            Toggle("Show where Instagram cuts", s.showCuts) { v -> app.prefs.update { it.copy(showCuts = v) } }
            Toggle("Snap to cuts and other photos", s.snapping) { v -> app.prefs.update { it.copy(snapping = v) } }

            SectionLabel("Export")
            Text("JPEG quality", style = MaterialTheme.typography.bodyMedium)
            ChipRow(listOf(90, 95, 100), s.jpegQuality, { "$it" }, { q -> app.prefs.update { it.copy(jpegQuality = q) } }, Modifier.padding(vertical = 4.dp))
            Toggle("Also save one long image", s.alsoPanorama) { v -> app.prefs.update { it.copy(alsoPanorama = v) } }

            if (app.ads.supported || app.tips.supported) {
                SectionLabel("Support")
                if (s.supported > 0) Text("♥ You've supported Unroll ${s.supported} ${if (s.supported == 1) "time" else "times"}. Thank you.", style = MaterialTheme.typography.bodyMedium)
                SupportLine()
            }

            SectionLabel("About")
            Text("Unroll ${BuildConfig.VERSION_NAME} by Grease Pencil Studio", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Every feature is free. Your photos and projects stay on your phone." +
                    if (app.ads.supported) " The only time Unroll goes online is when you choose to watch an ad." else " Unroll has no internet access at all.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (BuildConfig.SHOW_SOURCE_LINK) TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DarkXAce01/unroll")))
            }) { Text("Source code") }
            var credits by remember { mutableStateOf(false) }
            TextButton(onClick = { credits = true }) { Text("Sample photo credits") }
            if (credits) androidx.compose.material3.AlertDialog(
                onDismissRequest = { credits = false },
                confirmButton = { TextButton(onClick = { credits = false }) { Text("Close") } },
                title = { Text("Sample photos") },
                text = {
                    androidx.compose.foundation.lazy.LazyColumn {
                        item {
                            Text("Template previews use these freely licensed photos from Wikimedia Commons. Tap one to see its source.",
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 10.dp))
                        }
                        items(io.github.darkxace01.unroll.render.SamplePhotos.credits.size) { i ->
                            val c = io.github.darkxace01.unroll.render.SamplePhotos.credits[i]
                            Column(Modifier.fillMaxWidth().clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(c.source))) }.padding(vertical = 6.dp)) {
                                Text(c.title.substringBeforeLast('.'), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text("${c.artist.ifBlank { "Unknown" }} · ${c.license}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(value, onChange)
    }
}
