package io.github.darkxace01.unroll.editor

import io.github.darkxace01.unroll.ui.ToolIcons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Seams
import io.github.darkxace01.unroll.model.ShapeItem
import io.github.darkxace01.unroll.model.TextItem
import io.github.darkxace01.unroll.render.Palette
import io.github.darkxace01.unroll.ui.maxPick
import kotlinx.coroutines.launch

private enum class Sheet { Layout, Background, Adjust, Style, Text, Stickers, Shape, Theme, Route, Motion, Templates, Lines, Frames, AddFrame, Sticker }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(projectId: String, onBack: () -> Unit, onPreview: () -> Unit, onExport: () -> Unit, vm: EditorViewModel = viewModel()) {
    LaunchedEffect(projectId) { vm.open(projectId) }
    val app = LocalContext.current.app
    val project by vm.project.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val cropping by vm.cropping.collectAsStateWithLifecycle()
    val notices by vm.notices.collectAsStateWithLifecycle()
    val canUndo by vm.canUndo.collectAsStateWithLifecycle()
    val canRedo by vm.canRedo.collectAsStateWithLifecycle()
    val settings by app.prefs.settings.collectAsStateWithLifecycle()
    val viewport = remember { Viewport() }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var replacing by remember { mutableStateOf<String?>(null) }
    var drawing by remember { mutableStateOf(false) }
    var routePoint by remember { mutableStateOf<Int?>(null) }
    val screenW = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    val p = project ?: return Box(Modifier.fillMaxSize())
    val item = selected?.let { p.item(it) }
    fun slide() = viewport.centreSlide(p, screenW)

    var pickOnto by remember { mutableStateOf<Int?>(null) }
    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxPick())) { uris ->
        if (uris.isNotEmpty()) vm.addPhotos(uris, pickOnto)
    }
    val pickOne = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = replacing
        if (uri != null && target != null) vm.replacePhoto(target, uri)
        replacing = null
    }
    var filling by remember { mutableStateOf<String?>(null) }
    val pickFill = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = filling
        if (uri != null && target != null) vm.fillText(target, uri)
        filling = null
    }

    BackHandler(enabled = cropping != null || selected != null || drawing) {
        if (drawing) drawing = false else if (cropping != null) vm.crop(null) else vm.select(null)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        // Top bar
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(ToolIcons.Back, "Back") }
            Text(
                p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { renaming = true }.padding(horizontal = 6.dp, vertical = 8.dp),
            )
            IconButton(onClick = vm::undo, enabled = canUndo) { Icon(ToolIcons.Undo, "Undo") }
            IconButton(onClick = vm::redo, enabled = canRedo) { Icon(ToolIcons.Redo, "Redo") }
            IconButton(onClick = onPreview) { Icon(ToolIcons.Play, "Preview") }
            Button(onClick = onExport, contentPadding = PaddingValues(horizontal = 18.dp), modifier = Modifier.padding(start = 4.dp, end = 8.dp).height(40.dp)) { Text("Export") }
        }

        CanvasView(
            project = p,
            selectedId = selected,
            croppingId = cropping,
            notices = notices,
            showCuts = settings.showCuts,
            snapping = settings.snapping,
            viewport = viewport,
            onSelect = vm::select,
            onCrop = vm::crop,
            onPreview = vm::preview,
            onCommit = vm::endGesture,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            drawing = drawing,
            onDrawn = { pts -> vm.addDrawing(pts, null) },
            onEmptySlot = { id ->
                replacing = id
                pickOne.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onPoint = { routePoint = it },
        )

        NoticeBar(notices, onFix = vm::fixCut, onFixAll = vm::fixAllCuts, onShow = vm::select)

        if (drawing) {
            Hint("Draw with your finger: across slides, around photos, anything. Each line can be styled and animated after.") { drawing = false }
        }
        if (cropping != null) {
            Hint("Drag and pinch to move the photo in its frame. Tap outside when done.") { vm.crop(null) }
        }

        // Tools: for the selected item, or for the whole carousel. Icons with words; housekeeping last.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item != null) {
                Tool("Done", ToolIcons.Done, strong = true) { vm.select(null); vm.crop(null) }
                ToolGap()
            }
            when (item) {
                is PhotoItem -> {
                    Tool("Crop", ToolIcons.Crop, on = cropping == item.id) { vm.crop(if (cropping == item.id) null else item.id) }
                    Tool("Adjust", ToolIcons.Adjust) { sheet = Sheet.Adjust }
                    Tool("Frame", ToolIcons.Frame) { sheet = Sheet.Frames }
                    Tool("Style", ToolIcons.Style) { sheet = Sheet.Style }
                    Tool("Replace", ToolIcons.Replace) { replacing = item.id; pickOne.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    Tool("Motion", ToolIcons.Motion) { sheet = Sheet.Motion }
                    Housekeeping(vm, item.id)
                }
                is TextItem -> {
                    Tool("Edit", ToolIcons.Edit) { sheet = Sheet.Text }
                    Tool("Motion", ToolIcons.Motion) { sheet = Sheet.Motion }
                    Housekeeping(vm, item.id)
                }
                is ShapeItem -> {
                    Tool("Colour", ToolIcons.Colour) { sheet = Sheet.Shape }
                    Tool("Motion", ToolIcons.Motion) { sheet = Sheet.Motion }
                    Housekeeping(vm, item.id)
                }
                is io.github.darkxace01.unroll.model.StickerItem -> {
                    if (io.github.darkxace01.unroll.model.Stickers.byId(item.sticker)?.editable == true) Tool("Words", ToolIcons.Edit) { sheet = Sheet.Sticker }
                    Tool("Colour", ToolIcons.Colour) { sheet = Sheet.Sticker }
                    Tool("Flip", ToolIcons.Flip) { vm.updateItem(item.copy(flip = !item.flip)) }
                    Tool("Motion", ToolIcons.Motion) { sheet = Sheet.Motion }
                    Housekeeping(vm, item.id)
                }
                is io.github.darkxace01.unroll.model.RouteItem -> {
                    Tool("Style", ToolIcons.Style) { sheet = Sheet.Route }
                    Tool("Add bend", ToolIcons.Bend) { vm.routeAddPoint(item.id) }
                    Tool("Motion", ToolIcons.Motion) { sheet = Sheet.Motion }
                    Housekeeping(vm, item.id)
                }
                null -> {
                    Tool("Templates", ToolIcons.Templates) { sheet = Sheet.Templates }
                    Tool("Photos", ToolIcons.Photos) { pickOnto = null; pickMany.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    Tool("Photo here", ToolIcons.PhotoHere) { pickOnto = slide(); pickMany.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    Tool("Text", ToolIcons.Text) { vm.addText(slide()); sheet = Sheet.Text }
                    Tool("Stickers", ToolIcons.Stickers) { sheet = Sheet.Stickers }
                    Tool("Frames", ToolIcons.Frames) { sheet = Sheet.AddFrame }
                    Tool("Lines", ToolIcons.Routes) { sheet = Sheet.Lines }
                    Tool("Theme", ToolIcons.Theme) { sheet = Sheet.Theme }
                    Tool("Background", ToolIcons.Background) { sheet = Sheet.Background }
                    Tool("Layout", ToolIcons.Layout) { sheet = Sheet.Layout }
                    Tool("Motion", ToolIcons.Motion) { sheet = Sheet.Motion }
                }
            }
        }
    }

    if (sheet != null) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = sheet in setOf(Sheet.Text, Sheet.Stickers, Sheet.Frames, Sheet.AddFrame, Sheet.Sticker, Sheet.Templates))
        ModalBottomSheet(onDismissRequest = { vm.endGesture(); sheet = null }, sheetState = state, scrimColor = Color.Transparent) {
            val cur = selected?.let { p.item(it) }
            when (sheet) {
                Sheet.Layout -> LayoutSheet(
                    p, slide(), settings,
                    onArrange = { how, shuffle -> vm.arrange(how, shuffle) },
                    onGrid = { vm.grid(slide(), it) },
                    onSpacing = { m, g -> app.prefs.update { it.copy(margin = m, gap = g) }; vm.arrange(io.github.darkxace01.unroll.model.Arrange.Auto) },
                    onRatio = vm::ratio,
                    onInsert = vm::insertSlide,
                    onRemove = vm::removeSlide,
                    onMove = vm::moveSlide,
                )
                Sheet.Background -> {
                    var colors by remember { mutableStateOf<List<Long>>(emptyList()) }
                    LaunchedEffect(p.id) { colors = Palette.fromPhotos(app, p) }
                    Text("Texture", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 6.dp))
                    Box(Modifier.padding(horizontal = 20.dp)) { PatternRow(p.background, p) { pt -> vm.background(p.background.copy(pattern = pt)) } }
                    BackgroundSheet(p.background, p.theme, colors, onPreview = vm::previewBackground, onDone = vm::background)
                }
                Sheet.Adjust -> (cur as? PhotoItem)?.let { AdjustSheet(it, { n -> vm.previewItem(n) }, vm::endGesture, { n -> vm.updateItem(n) }) }
                Sheet.Style -> (cur as? PhotoItem)?.let {
                    PhotoStyleSheet(it, p.theme, { n -> vm.previewItem(n) }, vm::endGesture, { n -> vm.updateItem(n) }, onReplace = {
                        replacing = it.id
                        pickOne.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                }
                Sheet.Text -> (cur as? TextItem)?.let {
                    TextSheet(
                        it, p.theme, onLive = { n -> vm.updateText(n, live = true) }, onCommit = { n -> vm.updateText(n) }, onDone = vm::endGesture,
                        onPickFill = { filling = it.id; pickFill.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    )
                }
                Sheet.Stickers -> StickerLibrarySheet(p, onSticker = { d -> vm.addSticker(slide(), d); sheet = null }, onShape = { k -> vm.addShape(slide(), k); sheet = Sheet.Shape })
                Sheet.Sticker -> (cur as? io.github.darkxace01.unroll.model.StickerItem)?.let {
                    StickerItemSheet(it, p, onCommit = { n -> vm.updateItem(n) }, onLive = { n -> vm.previewItem(n) }, onDone = vm::endGesture)
                }
                Sheet.Frames -> (cur as? PhotoItem)?.let { ph ->
                    FrameLibrarySheet(p, ph.style, onPick = { f -> vm.framePhoto(ph, f) }) {
                        TextButton(onClick = { sheet = Sheet.Style }) { Text("Border, shadow, corners…") }
                    }
                }
                Sheet.AddFrame -> FrameLibrarySheet(p, null, onPick = { f -> vm.addFrame(slide(), f); sheet = null })
                Sheet.Shape -> (cur as? ShapeItem)?.let { ShapeSheet(it, p.theme, { n -> vm.previewItem(n) }, vm::endGesture, { n -> vm.updateItem(n) }) }
                Sheet.Theme -> ThemeSheet(p, onTheme = vm::theme, onColor = vm::themeColor, onFonts = vm::fonts)
                Sheet.Route -> (cur as? io.github.darkxace01.unroll.model.RouteItem)?.let {
                    RouteSheet(it, p, routePoint, { n -> vm.previewItem(n) }, vm::endGesture, { n -> vm.updateItem(n) },
                        onAddPoint = { vm.routeAddPoint(it.id) }, onRemovePoint = { i -> vm.routeRemovePoint(it.id, i); routePoint = null })
                }
                Sheet.Motion -> {
                    if (cur != null) MotionSheet(cur, { a -> vm.previewAnim(cur.id, a) }, vm::endGesture, { a -> vm.animate(cur.id, a) }, onAll = vm::autoAnimate, onNone = vm::stillAll)
                    else MotionAll(onAll = { vm.autoAnimate(); sheet = null }, onNone = { vm.stillAll(); sheet = null }, onPreview = { sheet = null; onPreview() })
                }
                Sheet.Templates -> {
                    Text(
                        "Rebuilds this carousel in the template's style, keeping your photos.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    io.github.darkxace01.unroll.templates.TemplateGallery(onPick = { t ->
                        scope.launch {
                            val photos = io.github.darkxace01.unroll.model.Arranger.photosOf(p).filter { it.uri.isNotEmpty() }
                            vm.applyTemplate(t, io.github.darkxace01.unroll.data.NewProject.tripInfo(app, photos))
                            sheet = null
                        }
                    }, height = 640.dp)
                }
                Sheet.Lines -> LinesSheet(
                    onConnect = { vm.connectPhotos(); sheet = Sheet.Route },
                    onRoute = { vm.addRoute(slide()); sheet = Sheet.Route },
                    onDraw = { drawing = true; vm.select(null); sheet = null },
                )
                null -> Unit
            }
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(p.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Name") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.rename(name); renaming = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LinesSheet(onConnect: () -> Unit, onRoute: () -> Unit, onDraw: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Routes & lines", style = MaterialTheme.typography.titleMedium)
        androidx.compose.material3.OutlinedButton(onClick = onConnect, modifier = Modifier.fillMaxWidth()) { Text("Route through my photos") }
        androidx.compose.material3.OutlinedButton(onClick = onRoute, modifier = Modifier.fillMaxWidth()) { Text("A route on this slide") }
        androidx.compose.material3.OutlinedButton(onClick = onDraw, modifier = Modifier.fillMaxWidth()) { Text("Draw freehand") }
        Text("Routes draw themselves in videos: in a Reel the line leads from slide to slide, and in an animated carousel each slide continues it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MotionAll(onAll: () -> Unit, onNone: () -> Unit, onPreview: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Motion", style = MaterialTheme.typography.titleMedium)
        Text("Select anything to choose how it arrives. Or set the whole carousel at once:", style = MaterialTheme.typography.bodySmall)
        androidx.compose.material3.OutlinedButton(onClick = onAll, modifier = Modifier.fillMaxWidth()) { Text("Animate everything") }
        androidx.compose.material3.OutlinedButton(onClick = onNone, modifier = Modifier.fillMaxWidth()) { Text("Keep everything still") }
        androidx.compose.material3.Button(onClick = onPreview, modifier = Modifier.fillMaxWidth()) { Text("Watch it play") }
    }
}

@Composable
private fun Tool(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, on: Boolean = false, strong: Boolean = false, onClick: () -> Unit) {
    val fg = if (strong) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.widthIn(min = 62.dp).clip(RoundedCornerShape(14.dp))
            .background(if (strong) MaterialTheme.colorScheme.onSurface else if (on) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = fg)
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ToolGap() = Box(Modifier.padding(horizontal = 6.dp).size(1.dp, 34.dp).background(MaterialTheme.colorScheme.outlineVariant))

/** Order, copy and delete: the same four at the end of every item's tools. */
@Composable
private fun Housekeeping(vm: EditorViewModel, id: String) {
    ToolGap()
    Tool("Forward", ToolIcons.Front) { vm.front(id) }
    Tool("Backward", ToolIcons.Back2) { vm.back(id) }
    Tool("Copy", ToolIcons.Copy) { vm.duplicate(id) }
    Tool("Delete", ToolIcons.Delete) { vm.delete(id) }
}

@Composable
private fun Hint(text: String, onDone: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        TextButton(onClick = onDone) { Text("Done") }
    }
}

@Composable
private fun NoticeBar(notices: List<Notice>, onFix: (String) -> Unit, onFixAll: () -> Unit, onShow: (String) -> Unit) {
    if (notices.isEmpty()) return
    val cuts = notices.filterIsInstance<Notice.OnCut>()
    val soft = notices.filterIsInstance<Notice.Soft>()
    Surface(color = Color(0xFFFBE9E4), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            cuts.distinctBy { it.itemId }.take(2).forEach { n ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val what = if (n.kind == Seams.Kind.Face) "A face" else "Text"
                    Text("$what sits on cut ${n.cut}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onShow(n.itemId) }) { Text("Show") }
                    if (n.canFix) TextButton(onClick = { onFix(n.itemId) }) { Text("Fix") }
                }
            }
            if (cuts.distinctBy { it.itemId }.size > 2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${cuts.distinctBy { it.itemId }.size - 2} more on cuts", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onFixAll) { Text("Fix all") }
                }
            }
            if (soft.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (soft.size == 1) "1 photo is small for its frame and may look soft" else "${soft.size} photos are small for their frames and may look soft",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onShow(soft.first().itemId) }) { Text("Show") }
                }
            }
        }
    }
}
