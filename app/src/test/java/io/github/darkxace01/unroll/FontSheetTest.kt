package io.github.darkxace01.unroll

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.darkxace01.unroll.model.FontKey
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.TextItem
import io.github.darkxace01.unroll.render.Fonts
import io.github.darkxace01.unroll.render.Renderer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every font draws, and each looks different from the plain one (so none silently fell back). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class)
class FontSheetTest {
    @Test fun everyFontDrawsItsOwnLetters() {
        val renderer = Renderer(Fonts(ApplicationProvider.getApplicationContext()))
        val rowH = 130f
        val items = FontKey.entries.mapIndexed { i, f ->
            TextItem("t$i", Frame(540f, 70f + i * rowH, 1000f, rowH), "${f.label}: Summer in Lisbon", f, size = 64f, color = 0xFF1D1E22)
        }
        val p = Project("p", "p", slides = 1, items = items)
        val h = (FontKey.entries.size * rowH + 40).toInt()
        val bmp = Bitmap.createBitmap(1080, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); c.drawColor(Color.WHITE)
        renderer.draw(c, p.copy(items = emptyList()), { null })
        for (t in items) renderer.drawText(c, p, t)
        File("build/outputs/fonts.png").apply { parentFile?.mkdirs() }.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

        fun row(i: Int) = IntArray(1080 * rowH.toInt()).also { bmp.getPixels(it, 0, 1080, 0, (5 + i * rowH).toInt(), 1080, rowH.toInt()) }
            .map { if (Color.red(it) < 128) 1 else 0 }
        val sans = row(0)
        for (k in listOf(FontKey.Magazine, FontKey.Marker, FontKey.Poster, FontKey.Retro, FontKey.Headline, FontKey.Typewriter)) {
            val r = row(k.ordinal)
            assertTrue("${k.label} drew nothing", r.sum() > 500)
        }
        assertTrue(sans.sum() > 500)
    }
}
