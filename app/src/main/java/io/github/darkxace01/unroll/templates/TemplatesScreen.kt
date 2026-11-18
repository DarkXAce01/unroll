package io.github.darkxace01.unroll.templates

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.data.NewProject
import io.github.darkxace01.unroll.model.Template
import io.github.darkxace01.unroll.ui.maxPick
import kotlinx.coroutines.launch

/** Pick a template, then the photos for it (or start with empty slots to tap and fill). */
@Composable
fun TemplatesScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val app = LocalContext.current.app
    val scope = rememberCoroutineScope()
    var chosen by remember { mutableStateOf<Template?>(null) }
    var busy by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxPick())) { uris ->
        val t = chosen ?: return@rememberLauncherForActivityResult
        if (uris.isNotEmpty()) scope.launch {
            busy = true
            onOpen(NewProject.fromTemplate(app, t, uris).id)
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(if (busy) "Building…" else "Templates", style = MaterialTheme.typography.titleLarge)
        }
        TemplateGallery(onPick = { chosen = it })
    }
    val t = chosen
    if (t != null && !busy) {
        AlertDialog(
            onDismissRequest = { chosen = null },
            title = { Text(t.name) },
            text = { Text("Pick your photos and the template stretches to fit them, any number. Everything stays editable, and the theme can be switched any time.") },
            confirmButton = { TextButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Pick photos") } },
            dismissButton = {
                TextButton(onClick = { scope.launch { busy = true; onOpen(NewProject.fromTemplate(app, t, emptyList()).id) } }) { Text("Empty slots") }
            },
        )
    }
}
