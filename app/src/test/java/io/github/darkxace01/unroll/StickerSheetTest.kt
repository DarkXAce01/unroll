package io.github.darkxace01.unroll

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.darkxace01.unroll.model.ColorRole
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.FrameKind
import io.github.darkxace01.unroll.model.Pattern
import io.github.darkxace01.unroll.model.PhotoGeometry
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.PhotoStyle
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Background
import io.github.darkxace01.unroll.model.Stickers
import io.github.darkxace01.unroll.model.Themes
import io.github.darkxace01.unroll.render.Fonts
import io.github.darkxace01.unroll.render.Patterns
import io.github.darkxace01.unroll.render.Renderer
import io.github.darkxace01.unroll.render.Scenes
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Contact sheets of every sticker pack, every frame, background texture and sample scene, written to
 * app/build/outputs/library to look at. Off by default: ./gradlew testPlayDebugUnitTest -PtemplateSheets
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class)
class StickerSheetTest {
    private val renderer = Renderer(Fonts(ApplicationProvider.getApplicationContext()))
    private val out = File("build/outputs/library").apply { mkdirs() }
    private val theme = Themes.Paper

    private fun save(b: Bitmap, name: String) = File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }

    @Test fun stickers() {
        assumeTrue(System.getProperty("unroll.templateSheets") == "true")
        for (pack in Stickers.packs) {
            val all = Stickers.inPack(pack.id)
            // Big packs: a sample spread across the pack.
            val defs = if (all.size > 48) all.filterIndexed { i, _ -> i % (all.size / 48 + 1) == 0 }.take(48) else all
            val cell = 220; val cols = 8
            val rows = (defs.size + cols - 1) / cols
            val bmp = Bitmap.createBitmap(cell * cols, cell * rows.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(0xFFF4F1EA.toInt())
            defs.forEachIndexed { i, d ->
                val box = cell * 0.82f
                val (w, h) = if (d.aspect >= 1f) box to box / d.aspect else box * d.aspect to box
                c.save()
                c.translate((i % cols) * cell + cell / 2f, (i / cols) * cell + cell / 2f)
                val color = (d.color ?: d.colorRole?.let { theme.color(it) } ?: theme.accent).toInt()
                renderer.stickerArt.draw(c, d, w, h, color, Stickers.fill(d.text.orEmpty(), io.github.darkxace01.unroll.model.TripInfo("Lisbon", "12–16 May", "Lisbon")), theme)
                c.restore()
            }
            save(bmp, "stickers-${pack.id}")
        }
    }

    @Test fun frames() {
        assumeTrue(System.getProperty("unroll.templateSheets") == "true")
        val kinds = FrameKind.entries
        val cell = 300; val cols = 6
        val rows = (kinds.size + cols - 1) / cols
        val p = Project("f", "Lisbon", slides = 1, items = emptyList(), theme = theme, background = Background(colorRole = ColorRole.Background))
        val bmp = Bitmap.createBitmap(cell * cols, cell * rows, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(0xFFE9E4DA.toInt())
        kinds.forEachIndexed { i, k ->
            val a = PhotoGeometry.naturalAspect(k) ?: 0.8f
            val box = cell * 0.8f
            val (w, h) = if (a >= 1f) box to box / a else box * a to box
            val item = PhotoItem("x$i", Frame((i % cols) * cell + cell / 2f, (i / cols) * cell + cell / 2f, w, h), "", 100, 100, style = PhotoStyle(frame = k, shadow = 10f))
            renderer.drawPhoto(c, p, item, null, sample = i)
        }
        save(bmp, "frames")
    }

    @Test fun texturesAndScenes() {
        assumeTrue(System.getProperty("unroll.templateSheets") == "true")
        val cell = 300
        val ps = Pattern.entries
        val bmp = Bitmap.createBitmap(cell * 7, cell * 2 + cell * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        ps.forEachIndexed { i, pt ->
            c.save(); c.translate((i % 7) * cell.toFloat(), (i / 7) * cell.toFloat()); c.clipRect(0, 0, cell - 6, cell - 6)
            c.drawColor(theme.background.toInt()); Patterns.draw(c, pt, cell.toFloat(), cell.toFloat(), theme.ink.toInt(), theme.accent.toInt())
            c.drawText(pt.label, 10f, 30f, Paint().apply { textSize = 24f }); c.restore()
        }
        for (i in 0 until Scenes.COUNT) Scenes.draw(c, RectF((i % 5) * cell * 1.4f, cell * 2f + (i / 5) * cell, (i % 5) * cell * 1.4f + cell * 1.35f, cell * 2f + (i / 5) * cell + cell - 6f), i)
        save(bmp, "textures-scenes")
    }
}
