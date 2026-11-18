package io.github.darkxace01.unroll.ui

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Unroll's own line icons: one 24-unit grid, 1.8 stroke, round ends, so every tool reads as one set.
 * Each is a few SVG path strings; tinted by whatever draws them.
 */
object ToolIcons {
    private fun icon(name: String, vararg d: String, filled: Set<Int> = emptySet()): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            d.forEachIndexed { i, p ->
                if (i in filled) addPath(addPathNodes(p), fill = SolidColor(androidx.compose.ui.graphics.Color.Black))
                else addPath(
                    addPathNodes(p), stroke = SolidColor(androidx.compose.ui.graphics.Color.Black), strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Back by lazy { icon("back", "M15 5l-7 7 7 7") }
    val Undo by lazy { icon("undo", "M9 14L4 9l5-5", "M4 9h10a6 6 0 0 1 0 12h-3") }
    val Redo by lazy { icon("redo", "M15 14l5-5-5-5", "M20 9H10a6 6 0 0 0 0 12h3") }
    val Play by lazy { icon("play", "M8 5.5v13l10.5-6.5z") }
    val Done by lazy { icon("done", "M5 12.5l4.5 4.5L19 7.5") }
    val Close by lazy { icon("close", "M6 6l12 12", "M18 6L6 18") }

    // Carousel tools
    val Templates by lazy { icon("templates", "M4 4h7v9H4z", "M13 4h7v5h-7z", "M13 11h7v9h-7z", "M4 15h7v5H4z") }
    val Theme by lazy { icon("theme", "M12 3a9 9 0 1 0 0 18c1.2 0 1.8-.8 1.8-1.7 0-1-.9-1.4-.9-2.4 0-1 .8-1.6 1.8-1.6H17a4 4 0 0 0 4-4C21 6.6 17 3 12 3z", "M7.5 11.5h.01", "M10 7.5h.01", "M15 7.5h.01") }
    val Photos by lazy { icon("photos", "M3.5 5.5h17v13h-17z", "M3.5 15.5l5-5 4 4 2.5-2.5 5.5 5.5", "M15.5 9h.01") }
    val PhotoHere by lazy { icon("photo-here", "M3.5 7.5h12v11h-12z", "M3.5 15.5l3.5-3.5 3 3 1.5-1.5 4 4", "M19 3v6", "M16 6h6") }
    val Text by lazy { icon("text", "M5 6V4.5h14V6", "M12 4.5v15", "M9 19.5h6") }
    val Stickers by lazy { icon("stickers", "M20 13V5.5A1.5 1.5 0 0 0 18.5 4h-13A1.5 1.5 0 0 0 4 5.5v13A1.5 1.5 0 0 0 5.5 20H13z", "M13 20v-5.5a1.5 1.5 0 0 1 1.5-1.5H20", "M9 9.5h.01", "M14 9.5h.01", "M8.5 14c.8.8 1.8 1.2 3 1.2") }
    val Frames by lazy { icon("frames", "M5 3.5h14v17H5z", "M8 6.5h8v9H8z") }
    val Routes by lazy { icon("routes", "M5 19c3 0 3-6 7-6s4-6 7-6", "M5 19h.01", "M19 7h.01", "M17 4.5l2 2.5-2.5 2") }
    val Motion by lazy { icon("motion", "M3 12h4", "M5 8h5", "M5 16h5", "M13 6.5a5.5 5.5 0 1 1 0 11 5.5 5.5 0 0 1 0-11z") }
    val Layout by lazy { icon("layout", "M3.5 5h17v14h-17z", "M9.5 5v14", "M15 5v14") }
    val Background by lazy { icon("background", "M4 4h16v16H4z", "M4 15l5-5 11 10", "M4 9.5l5.5-5.5", "M9 20l11-11") }

    // Item tools
    val PhotoInside by lazy { icon("photo-inside", "M3.5 4.5h17v15h-17z", "M3.5 13l4-4 4 4", "M12 11.5h7v6.5h-7z") }
    val Crop by lazy { icon("crop", "M6 2.5V18h15.5", "M2.5 6H18v15.5") }
    val Adjust by lazy { icon("adjust", "M4 7h10", "M18 7h2", "M4 17h4", "M12 17h8", "M16 4.5v5", "M10 14.5v5") }
    val Frame by lazy { icon("frame", "M5 3.5h14v17H5z", "M8 6.5h8v9H8z") }
    val Style by lazy { icon("style", "M4 20c3 0 4.5-1.5 4.5-4a2.5 2.5 0 0 1 5 0", "M10 14L19.5 4.5a1.5 1.5 0 0 1 2 2L12 16") }
    val Replace by lazy { icon("replace", "M4 9a8 8 0 0 1 14-3l2 2", "M20 3v5h-5", "M20 15a8 8 0 0 1-14 3l-2-2", "M4 21v-5h5") }
    val Front by lazy { icon("front", "M8 8h12v12H8z", "M4 16V4h12") }
    val Back2 by lazy { icon("to-back", "M4 4h12v12H4z", "M20 8v12H8") }
    val Copy by lazy { icon("copy", "M8 8h12v12H8z", "M16 8V4H4v12h4") }
    val Delete by lazy { icon("delete", "M4 7h16", "M10 11v6", "M14 11v6", "M6 7l1 13h10l1-13", "M9 7V4h6v3") }
    val Edit by lazy { icon("edit", "M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4") }
    val Colour by lazy { icon("colour", "M12 3.5c3.5 4 6 7 6 10a6 6 0 0 1-12 0c0-3 2.5-6 6-10z") }
    val Flip by lazy { icon("flip", "M12 3v18", "M8 7L3 17h5z", "M16 7l5 10h-5z") }
    val Bend by lazy { icon("bend", "M4 18c4-12 12-12 16 0", "M12 9h.01") }

    // Export
    val Download by lazy { icon("download", "M12 4v11", "M7 10l5 5 5-5", "M5 20h14") }
    val ShareOut by lazy { icon("share", "M12 15V4", "M7.5 8.5L12 4l4.5 4.5", "M5 13v6.5h14V13") }
    val Instagram by lazy { icon("instagram", "M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4z", "M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8z", "M17.3 6.7h.01") }
    val Note by lazy { icon("note", "M9 18V5l10-2v13", "M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0z", "M19 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0z") }
    val Video by lazy { icon("video", "M3.5 6.5h12v11h-12z", "M15.5 10.5l5-3v9l-5-3") }
    val Long by lazy { icon("long", "M2.5 8h19v8h-19z", "M8.5 8v8", "M15 8v8") }
}
