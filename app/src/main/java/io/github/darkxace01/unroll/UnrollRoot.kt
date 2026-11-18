package io.github.darkxace01.unroll

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darkxace01.unroll.data.NewProject
import io.github.darkxace01.unroll.editor.EditorScreen
import io.github.darkxace01.unroll.editor.EditorViewModel
import io.github.darkxace01.unroll.export.ExportScreen
import io.github.darkxace01.unroll.home.HomeScreen
import io.github.darkxace01.unroll.preview.PreviewScreen
import io.github.darkxace01.unroll.settings.SettingsScreen

private sealed interface Route {
    data object Home : Route
    data class Editor(val id: String) : Route
    data class Preview(val id: String) : Route
    data class Export(val id: String) : Route
    data object Settings : Route
    data object Templates : Route
}

/** A plain back stack: the app is small enough not to need a navigation library. */
@Composable
fun UnrollRoot(shared: List<Uri>, onSharedDone: () -> Unit) {
    val app = LocalContext.current.app
    val stack = remember { mutableStateListOf<Route>(Route.Home) }
    fun go(r: Route) { stack.add(r) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    // The editor's state is shared with preview and export, so they always show the latest edit.
    val editor: EditorViewModel = viewModel()
    val project by editor.project.collectAsStateWithLifecycle()

    LaunchedEffect(shared) {
        if (shared.isNotEmpty()) {
            val p = NewProject.create(app, shared)
            onSharedDone()
            stack.clear(); stack.add(Route.Home); stack.add(Route.Editor(p.id))
        }
    }

    BackHandler(enabled = stack.size > 1) { back() }

    when (val r = stack.last()) {
        Route.Home -> HomeScreen(onOpen = { go(Route.Editor(it)) }, onSettings = { go(Route.Settings) }, onTemplates = { go(Route.Templates) })
        Route.Templates -> io.github.darkxace01.unroll.templates.TemplatesScreen(onBack = ::back, onOpen = { id -> back(); go(Route.Editor(id)) })
        is Route.Editor -> EditorScreen(r.id, onBack = ::back, onPreview = { go(Route.Preview(r.id)) }, onExport = { go(Route.Export(r.id)) }, vm = editor)
        is Route.Preview -> project?.let { PreviewScreen(it, onBack = ::back, onExport = { back(); go(Route.Export(r.id)) }) }
        is Route.Export -> project?.let { ExportScreen(it, onBack = ::back) }
        Route.Settings -> SettingsScreen(onBack = ::back)
    }
}
