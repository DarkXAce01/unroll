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
import io.github.darkxace01.unroll.model.photoUris
import io.github.darkxace01.unroll.render.Fonts
import io.github.darkxace01.unroll.render.Renderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Text filled with a photo: the photo shows inside the letters and nowhere else. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class)
class TextFillTest {
    @Test fun photoShowsOnlyThroughLetters() {
        val renderer = Renderer(Fonts(ApplicationProvider.getApplicationContext()))
        // A photo that is red on the left half and blue on the right.
        val photo = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888).also {
            val c = Canvas(it); c.drawColor(Color.RED); c.clipRect(200, 0, 400, 200); c.drawColor(Color.BLUE)
        }
        val t = TextItem("t", Frame(540f, 675f, 1000f, 400f), "MMMM", FontKey.Sans, size = 300f, bold = true, color = 0xFF00FF00, fill = "photo")
        val p = Project("p", "p", slides = 1, items = listOf(t))
        val h = renderer.textHeight(t, p)
        val q = p.copy(items = listOf(t.copy(frame = t.frame.copy(h = h))))
        val bmp = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        renderer.draw(Canvas(bmp), q, { if (it == "photo") photo else null })
        File("build/outputs/textfill.png").apply { parentFile?.mkdirs() }.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val colours = HashSet<Int>()
        for (x in 0 until 1080 step 3) for (y in 500 until 850 step 3) colours += bmp.getPixel(x, y)
        // Photo colours inside the letters (left red, right blue), never the plain text colour.
        assert(colours.any { Color.red(it) > 200 && Color.blue(it) < 60 }) { "no red from the photo" }
        assert(colours.any { Color.blue(it) > 200 && Color.red(it) < 60 }) { "no blue from the photo" }
        assert(colours.none { Color.green(it) > 200 && Color.red(it) < 60 && Color.blue(it) < 60 }) { "plain colour leaked" }
        // Outside the text box the background is untouched.
        assertEquals(bmp.getPixel(20, 20), bmp.getPixel(20, 1300))
        assertNotEquals(Color.RED, bmp.getPixel(20, 20))
        assertEquals(listOf("photo"), q.photoUris())
    }
}
