package io.github.darkxace01.unroll.data

import android.content.Context
import io.github.darkxace01.unroll.model.Ratio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Settings(
    val showCuts: Boolean = true,
    val snapping: Boolean = true,
    val ratio: Ratio = Ratio.Portrait,
    val margin: Float = 48f,
    val gap: Float = 24f,
    val jpegQuality: Int = 95,
    val alsoPanorama: Boolean = false,
    /** Tips and ads watched, all time. Only used to say thank you. */
    val supported: Int = 0,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _now = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _now.asStateFlow()
    val now get() = _now.value

    private fun read() = Settings(
        showCuts = sp.getBoolean("showCuts", true),
        snapping = sp.getBoolean("snapping", true),
        ratio = runCatching { Ratio.valueOf(sp.getString("ratio", null) ?: "") }.getOrDefault(Ratio.Portrait),
        margin = sp.getFloat("margin", 48f),
        gap = sp.getFloat("gap", 24f),
        jpegQuality = sp.getInt("jpegQuality", 95),
        alsoPanorama = sp.getBoolean("alsoPanorama", false),
        supported = sp.getInt("supported", 0),
    )

    fun update(f: (Settings) -> Settings) {
        val s = f(_now.value)
        sp.edit()
            .putBoolean("showCuts", s.showCuts)
            .putBoolean("snapping", s.snapping)
            .putString("ratio", s.ratio.name)
            .putFloat("margin", s.margin)
            .putFloat("gap", s.gap)
            .putInt("jpegQuality", s.jpegQuality)
            .putBoolean("alsoPanorama", s.alsoPanorama)
            .putInt("supported", s.supported)
            .apply()
        _now.value = s
    }

    fun addSupport(count: Int = 1) = update { it.copy(supported = it.supported + count) }
}
