package io.github.darkxace01.unroll.data

import io.github.darkxace01.unroll.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Projects as JSON files, one per project. Small, readable, easy to share later. */
object ProjectJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        classDiscriminator = "type"
    }

    fun encode(p: Project): String = json.encodeToString(Project.serializer(), p)
    fun decode(s: String): Project = json.decodeFromString(Project.serializer(), s)
}

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

class ProjectStore(private val dir: File) {
    private val _projects = MutableStateFlow<List<Project>>(emptyList())

    /** Every project, most recently changed first. */
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    init {
        dir.mkdirs()
    }

    suspend fun load() = withContext(Dispatchers.IO) {
        _projects.value = dir.listFiles { f -> f.extension == "json" }.orEmpty()
            .mapNotNull { f -> runCatching { ProjectJson.decode(f.readText()) }.getOrNull() }
            .sortedByDescending { it.updated }
    }

    fun get(id: String) = _projects.value.firstOrNull { it.id == id }

    /** Saves atomically (write to a temp file, then rename) so a crash mid-save never loses a project. */
    suspend fun save(p: Project) = withContext(Dispatchers.IO) {
        val stamped = p.copy(updated = System.currentTimeMillis())
        val tmp = File(dir, "${p.id}.json.tmp")
        tmp.writeText(ProjectJson.encode(stamped))
        tmp.renameTo(File(dir, "${p.id}.json"))
        _projects.value = (listOf(stamped) + _projects.value.filterNot { it.id == p.id }).sortedByDescending { it.updated }
        stamped
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        File(dir, "$id.json").delete()
        _projects.value = _projects.value.filterNot { it.id == id }
    }

    suspend fun duplicate(id: String): Project? {
        val p = get(id) ?: return null
        val now = System.currentTimeMillis()
        return save(p.copy(id = newId(), name = "${p.name} copy", created = now))
    }
}
