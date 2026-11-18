package io.github.darkxace01.unroll.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.darkxace01.unroll.app
import io.github.darkxace01.unroll.data.newId
import io.github.darkxace01.unroll.model.Arrange
import io.github.darkxace01.unroll.model.Arranger
import io.github.darkxace01.unroll.model.Background
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.Grid
import io.github.darkxace01.unroll.model.History
import io.github.darkxace01.unroll.model.Item
import io.github.darkxace01.unroll.model.NewPhoto
import io.github.darkxace01.unroll.model.NormRect
import io.github.darkxace01.unroll.model.PhotoGeometry
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Ratio
import io.github.darkxace01.unroll.model.Seams
import io.github.darkxace01.unroll.model.ShapeItem
import io.github.darkxace01.unroll.model.ArtKind
import io.github.darkxace01.unroll.model.FramePreset
import io.github.darkxace01.unroll.model.StickerItem
import io.github.darkxace01.unroll.model.Stickers
import io.github.darkxace01.unroll.model.ShapeKind
import io.github.darkxace01.unroll.model.TextItem
import io.github.darkxace01.unroll.model.Anim
import io.github.darkxace01.unroll.model.AnimKind
import io.github.darkxace01.unroll.model.Cap
import io.github.darkxace01.unroll.model.ColorRole
import io.github.darkxace01.unroll.model.Dash
import io.github.darkxace01.unroll.model.FontKey
import io.github.darkxace01.unroll.model.FontRole
import io.github.darkxace01.unroll.model.Point
import io.github.darkxace01.unroll.model.RouteItem
import io.github.darkxace01.unroll.model.RouteMath
import io.github.darkxace01.unroll.model.Template
import io.github.darkxace01.unroll.model.Templates
import io.github.darkxace01.unroll.model.Theme
import io.github.darkxace01.unroll.model.Timeline
import io.github.darkxace01.unroll.model.TripInfo
import io.github.darkxace01.unroll.model.Vehicle
import io.github.darkxace01.unroll.model.withAnim
import io.github.darkxace01.unroll.model.withId
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** Something the editor wants to point out: a cut through a face or text, or a photo that will look soft. */
sealed interface Notice {
    val itemId: String
    data class OnCut(override val itemId: String, val cut: Int, val kind: Seams.Kind, val canFix: Boolean) : Notice
    data class Soft(override val itemId: String) : Notice
}

/**
 * One open project. Every edit goes through [preview] (while a finger moves) and [commit] (when it lifts),
 * so undo works per gesture. Saves itself a moment after each change.
 */
@OptIn(FlowPreview::class)
class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.app
    private val faceCache = HashMap<String, List<NormRect>>()

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _selected = MutableStateFlow<String?>(null)
    val selected: StateFlow<String?> = _selected.asStateFlow()

    /** The photo being re-cropped inside its frame (double-tap), or null. */
    private val _cropping = MutableStateFlow<String?>(null)
    val cropping: StateFlow<String?> = _cropping.asStateFlow()

    private val _notices = MutableStateFlow<List<Notice>>(emptyList())
    val notices: StateFlow<List<Notice>> = _notices.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    private var history: History? = null
    private var seed = 0
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            _project.filterNotNull().debounce(400).collect { app.store.save(it) }
        }
    }

    fun open(id: String) {
        if (_project.value?.id == id) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (app.store.projects.value.isEmpty()) app.store.load()
            val p = app.store.get(id) ?: return@launch
            history = History(p)
            set(p)
            p.items.filterIsInstance<PhotoItem>().forEach { lookForFaces(it.uri) }
        }
    }

    private fun set(p: Project) {
        val merged = mergeFaces(p)
        _project.value = merged
        _notices.value = notices(merged)
        _canUndo.value = history?.canUndo == true
        _canRedo.value = history?.canRedo == true
        if (_selected.value != null && merged.item(_selected.value!!) == null) _selected.value = null
    }

    /** Faces are found in the background and kept outside history, so undo never forgets them. */
    private fun mergeFaces(p: Project): Project {
        if (faceCache.isEmpty()) return p
        var changed = false
        val items = p.items.map { if (it is PhotoItem && it.faces == null && it.uri in faceCache) { changed = true; it.copy(faces = faceCache[it.uri]) } else it }
        return if (changed) p.copy(items = items) else p
    }

    private fun notices(p: Project): List<Notice> {
        val cuts = Seams.warnings(p).distinctBy { it.itemId to it.cut }.map { w ->
            Notice.OnCut(w.itemId, w.cut, w.kind, (Seams.nudge(p, w.itemId) ?: 0f) != 0f)
        }
        val soft = p.items.filterIsInstance<PhotoItem>().filter { !it.isEmpty && PhotoGeometry.sharpness(it) < 0.6f }.map { Notice.Soft(it.id) }
        return cuts + soft
    }

    private fun lookForFaces(uri: String) {
        if (!app.faces.supported || uri in faceCache) return
        viewModelScope.launch {
            val bmp = app.photos.load(uri, 960) ?: return@launch
            faceCache[uri] = app.faces.find(bmp)
            _project.value?.let { p ->
                history?.let { h -> h.replaceCommitted(mergeFaces(h.committed)) }
                set(p)
            }
        }
    }

    /** While a finger moves: show it, don't record it. */
    fun preview(p: Project) = set(p)

    /** A finished change: one undo step. */
    fun commit(p: Project) {
        history?.commit(p)
        set(p)
    }

    /** Commits whatever [preview] showed last. */
    fun endGesture() { _project.value?.let(::commit) }

    fun edit(f: (Project) -> Project) { _project.value?.let { commit(f(it)) } }

    fun undo() { history?.undo()?.let(::set) }
    fun redo() { history?.redo()?.let(::set) }

    fun select(id: String?) {
        if (id != _cropping.value) _cropping.value = null
        _selected.value = id
    }

    fun crop(id: String?) {
        _cropping.value = id
        if (id != null) _selected.value = id
    }

    fun updateItem(item: Item) = edit { it.replace(item) }
    fun previewItem(item: Item) { _project.value?.let { preview(it.replace(item)) } }

    fun delete(id: String) { edit { it.remove(id) }; select(null) }
    fun front(id: String) = edit { it.bringToFront(id) }
    fun back(id: String) = edit { it.sendToBack(id) }

    fun duplicate(id: String) {
        val p = _project.value ?: return
        val item = p.item(id) ?: return
        val nid = newId()
        val copy = item.withId(nid).let { it.withFrame(it.frame.moved(48f, 48f)) }
        commit(p.add(copy))
        select(nid)
    }

    fun fixCut(id: String) = edit { Seams.fix(it, id) }

    fun fixAllCuts() = edit { p -> Seams.warnings(p).map { it.itemId }.distinct().fold(p) { acc, id -> Seams.fix(acc, id) } }

    // Photos

    private suspend fun read(uris: List<Uri>): List<NewPhoto> = uris.mapNotNull { u ->
        app.photos.keep(u)
        app.photos.info(u)?.let { NewPhoto(u.toString(), it.width, it.height, it.takenAt, it.lat, it.lng, faceCache[u.toString()]) }
    }

    /** Adds photos: onto [slide] if one, otherwise one per new slide at the end. */
    fun addPhotos(uris: List<Uri>, slide: Int?) = viewModelScope.launch {
        val photos = read(uris)
        val p = _project.value ?: return@launch
        val next = if (slide != null && photos.size == 1) Arranger.place(p, slide, photos[0], newId())
        else Arranger.append(p, photos, app.prefs.now.margin) { newId() }
        commit(next)
        photos.forEach { lookForFaces(it.uri) }
    }

    /** Swaps the photo in a frame for another, keeping the frame and style. */
    fun replacePhoto(id: String, uri: Uri) = viewModelScope.launch {
        val ph = read(listOf(uri)).firstOrNull() ?: return@launch
        val p = _project.value ?: return@launch
        val old = p.item(id) as? PhotoItem ?: return@launch
        commit(p.replace(old.copy(uri = ph.uri, srcW = ph.w, srcH = ph.h, crop = io.github.darkxace01.unroll.model.Crop(), faces = ph.faces, takenAt = ph.takenAt, lat = ph.lat, lng = ph.lng)))
        lookForFaces(ph.uri)
    }

    fun arrange(how: Arrange, shuffle: Boolean = false) {
        val p = _project.value ?: return
        if (shuffle) seed++
        val photos = Arranger.photosOf(p).map { it.copy(faces = it.faces ?: faceCache[it.uri]) }
        val ids = p.items.filterIsInstance<PhotoItem>().sortedBy { it.frame.left }.map { it.id }
        val s = app.prefs.now
        commit(Arranger.arrange(p, photos, how, s.margin, s.gap, seed) { ids.getOrElse(it) { newId() } })
    }

    fun grid(slide: Int, grid: Grid) {
        val s = app.prefs.now
        edit { Arranger.gridSlide(it, slide, grid, s.margin, s.gap) }
    }

    // Canvas

    fun ratio(r: Ratio) { edit { it.withRatio(r) }; app.prefs.update { it.copy(ratio = r) } }
    fun background(b: Background) = edit { it.copy(background = b) }
    fun previewBackground(b: Background) { _project.value?.let { preview(it.copy(background = b)) } }
    fun insertSlide(after: Int) = edit { it.insertSlide(after) }
    fun removeSlide(i: Int) = edit { it.removeSlide(i) }
    fun moveSlide(from: Int, to: Int) = edit { it.moveSlide(from, to) }
    fun rename(name: String) = edit { it.copy(name = name.trim().ifEmpty { it.name }) }

    // Text & stickers

    fun addText(slide: Int, text: String = "Your words"): String {
        val p = _project.value ?: return ""
        val id = newId()
        val base = TextItem(id, Frame(0f, 0f, p.slideW * 0.8f, 100f), text, size = 84f, colorRole = ColorRole.Ink, fontRole = FontRole.Head)
        val w = (app.renderer.textWidth(base, p) + app.renderer.pad(base) * 2 + 8).coerceIn(160f, p.slideW * 0.9f)
        val sized = base.copy(frame = base.frame.copy(w = w))
        val h = app.renderer.textHeight(sized, p)
        commit(p.add(sized.copy(frame = Frame(slide * p.slideW + p.slideW / 2f, p.slideH / 2f, w, h))))
        select(id)
        return id
    }

    /** Applies a text change and resizes the box's height to the text. */
    fun updateText(t: TextItem, live: Boolean = false) {
        val h = app.renderer.textHeight(t, _project.value)
        val fitted = t.copy(frame = t.frame.copy(h = h))
        if (live) previewItem(fitted) else updateItem(fitted)
    }

    fun addShape(slide: Int, kind: ShapeKind) {
        val p = _project.value ?: return
        val id = newId()
        val (w, h) = when (kind) {
            ShapeKind.Line, ShapeKind.Arrow -> 400f to 40f
            ShapeKind.Tape -> 260f to 70f
            ShapeKind.Plane -> 240f to 200f
            ShapeKind.Pin -> 120f to 170f
            else -> 220f to 220f
        }
        commit(p.add(ShapeItem(id, Frame(slide * p.slideW + p.slideW / 2f, p.slideH / 2f, w, h), kind, colorRole = if (kind == ShapeKind.Tape) ColorRole.Paper else ColorRole.Accent)))
        select(id)
    }

    /** A sticker from the library, centred on the slide in view, sized for its kind. */
    fun addSticker(slide: Int, def: io.github.darkxace01.unroll.model.StickerDef) {
        val p = _project.value ?: return
        val id = newId()
        val wide = def.kind in setOf(ArtKind.Title, ArtKind.RansomWord, ArtKind.BeadWord, ArtKind.Subtitle) || def.aspect >= 2f
        val w = when {
            def.kind == ArtKind.Subtitle -> p.slideW * 0.85f
            wide -> p.slideW * 0.62f
            def.aspect < 1f -> 300f * def.aspect
            else -> 280f
        }
        val text = def.text?.let { Stickers.fill(it, TripInfo(title = p.name, place = p.name)) }
        commit(p.add(StickerItem(id, Frame(slide * p.slideW + p.slideW / 2f, p.slideH / 2f, w, w / def.aspect), def.id, text = text, anim = Anim(AnimKind.Pop, 0.3f, 0.5f))))
        select(id)
    }

    /** An empty photo slot in a frame; tapping it picks the photo. */
    fun addFrame(slide: Int, f: FramePreset) {
        val p = _project.value ?: return
        val id = newId()
        val a = PhotoGeometry.naturalAspect(f.kind) ?: 0.8f
        var w = p.slideW * 0.62f
        if (w / a > p.slideH * 0.7f) w = p.slideH * 0.7f * a
        val h = w / a
        commit(p.add(PhotoItem(id, Frame(slide * p.slideW + p.slideW / 2f, p.slideH / 2f, w, h), "", w.toInt(), h.toInt(), style = f.style(io.github.darkxace01.unroll.model.PhotoStyle(shadow = 12f)))))
        select(id)
    }

    /** Puts a frame on a photo, reshaping it to the frame's natural shape (keeping its area). */
    fun framePhoto(item: PhotoItem, f: FramePreset) {
        val a = PhotoGeometry.naturalAspect(f.kind)
        val fr = item.frame
        val frame = if (a == null) fr else { val area = fr.w * fr.h; val h = kotlin.math.sqrt(area / a); fr.copy(w = h * a, h = h) }
        updateItem(item.copy(frame = frame, style = f.style(item.style)))
    }

    // Templates & themes

    /** Rebuilds the carousel from a template, keeping its photos (in order) and the shape. */
    fun applyTemplate(t: Template, info: TripInfo) {
        val p = _project.value ?: return
        val photos = Arranger.photosOf(p).filter { it.uri.isNotEmpty() }.map { it.copy(faces = it.faces ?: faceCache[it.uri]) }
        val built = Templates.build(t, photos, info, p.id, p.name, p.created, ::newId, theme = p.theme.takeIf { p.template != null } ?: t.theme)
        commit(built.copy(updated = p.updated))
        select(null)
    }

    fun theme(t: Theme) = edit { it.copy(theme = t) }

    /** Head and body fonts of the theme. */
    fun fonts(head: FontKey, body: FontKey) = edit { it.copy(theme = it.theme.copy(head = head, body = body)) }

    fun themeColor(role: ColorRole, color: Long) = edit { p ->
        val t = p.theme
        p.copy(theme = when (role) {
            ColorRole.Background -> t.copy(background = color)
            ColorRole.Paper -> t.copy(paper = color)
            ColorRole.Ink -> t.copy(ink = color)
            ColorRole.Accent -> t.copy(accent = color)
            ColorRole.Muted -> t.copy(muted = color)
        })
    }

    // Routes

    /** A route weaving through every photo, under them, with a plane on it. */
    fun connectPhotos() {
        val p = _project.value ?: return
        val boxes = p.items.filterIsInstance<PhotoItem>().map { it.frame.bounds() }
        if (boxes.isEmpty()) return
        val id = newId()
        commit(p.addBottom(RouteItem(id, RouteMath.through(boxes, p.height.toFloat()), stops = true, vehicle = Vehicle.Plane, start = Cap.Ring, anim = Anim(AnimKind.Draw, 0f, 1.6f))))
        select(id)
    }

    /** A straight-ish route across the slide in view, to bend into shape. */
    fun addRoute(slide: Int) {
        val p = _project.value ?: return
        val x = slide * p.slideW.toFloat()
        val id = newId()
        val pts = listOf(Point(x + p.slideW * 0.15f, p.slideH * 0.7f), Point(x + p.slideW * 0.5f, p.slideH * 0.55f), Point(x + p.slideW * 0.85f, p.slideH * 0.7f))
        commit(p.add(RouteItem(id, pts, vehicle = Vehicle.Plane, anim = Anim(AnimKind.Draw, 0f, 1.6f))))
        select(id)
    }

    /** A freehand line from the draw tool. */
    fun addDrawing(points: List<Point>, style: RouteItem?) {
        val p = _project.value ?: return
        val simple = RouteMath.simplify(points, 6f)
        if (simple.size < 2) return
        val base = style ?: RouteItem("", emptyList(), dash = Dash.Solid, start = Cap.None, end = Cap.None, width = 10f, colorRole = ColorRole.Accent)
        commit(p.add(base.copy(id = newId(), points = simple, smooth = true, vehicle = Vehicle.None, anim = Anim(AnimKind.Draw, 0f, 1.0f))))
    }

    fun routeAddPoint(id: String) { (_project.value?.item(id) as? RouteItem)?.let { updateItem(RouteMath.addPoint(it)) } }
    fun routeRemovePoint(id: String, index: Int) { (_project.value?.item(id) as? RouteItem)?.let { updateItem(RouteMath.removePoint(it, index)) } }

    // Motion

    fun animate(id: String, a: Anim?) = edit { it.animate(id, a) }
    fun previewAnim(id: String, a: Anim?) { _project.value?.let { preview(it.animate(id, a)) } }
    fun autoAnimate() = edit { Timeline.autoAnimate(it) }
    fun stillAll() = edit { p -> p.copy(items = p.items.map { it.withAnim(null) }) }
}
