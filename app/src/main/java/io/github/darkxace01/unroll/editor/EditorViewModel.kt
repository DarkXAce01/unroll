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
import io.github.darkxace01.unroll.model.ShapeKind
import io.github.darkxace01.unroll.model.TextItem
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
        val soft = p.items.filterIsInstance<PhotoItem>().filter { PhotoGeometry.sharpness(it) < 0.6f }.map { Notice.Soft(it.id) }
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
        val f = item.frame.moved(48f, 48f)
        val copy: Item = when (item) {
            is PhotoItem -> item.copy(id = nid, frame = f)
            is TextItem -> item.copy(id = nid, frame = f)
            is ShapeItem -> item.copy(id = nid, frame = f)
        }
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
        val base = TextItem(id, Frame(0f, 0f, p.slideW * 0.8f, 100f), text, size = 84f, color = 0xFF1D1E22)
        val w = (app.renderer.textWidth(base) + app.renderer.pad(base) * 2 + 8).coerceIn(160f, p.slideW * 0.9f)
        val sized = base.copy(frame = base.frame.copy(w = w))
        val h = app.renderer.textHeight(sized)
        commit(p.add(sized.copy(frame = Frame(slide * p.slideW + p.slideW / 2f, p.slideH / 2f, w, h))))
        select(id)
        return id
    }

    /** Applies a text change and resizes the box's height to the text. */
    fun updateText(t: TextItem, live: Boolean = false) {
        val h = app.renderer.textHeight(t)
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
        commit(p.add(ShapeItem(id, Frame(slide * p.slideW + p.slideW / 2f, p.slideH / 2f, w, h), kind, color = if (kind == ShapeKind.Tape) 0xFFEFE3C2 else 0xFF1D1E22)))
        select(id)
    }
}
