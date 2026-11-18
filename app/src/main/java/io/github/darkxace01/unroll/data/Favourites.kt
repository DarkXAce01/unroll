package io.github.darkxace01.unroll.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Templates the person has saved with the heart. Kept on the phone only. */
class Favourites(context: Context) {
    private val sp = context.getSharedPreferences("favourites", Context.MODE_PRIVATE)
    private val _ids = MutableStateFlow(sp.getStringSet("templates", emptySet()).orEmpty().toSet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun toggle(id: String) {
        val next = if (id in _ids.value) _ids.value - id else _ids.value + id
        _ids.value = next
        sp.edit().putStringSet("templates", next).apply()
    }
}
