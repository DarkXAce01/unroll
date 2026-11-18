package io.github.darkxace01.unroll

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.darkxace01.unroll.model.Adjust
import io.github.darkxace01.unroll.model.Background
import io.github.darkxace01.unroll.model.Filter
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.PhotoStyle
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.Ratio
import io.github.darkxace01.unroll.model.TextItem
import io.github.darkxace01.unroll.render.Fonts
import io.github.darkxace01.unroll.render.Renderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Real Android drawing (Robolectric's native graphics). The core promise: slide N exported on its own is
 * exactly that strip of the whole canvas, so the picture lines up when someone swipes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class)
class RenderTest {
    private val renderer = Renderer(Fonts(ApplicationProvider.getApplicationContext()))

    /** A photo with a horizontal colour ramp, so any misalignment shows up as a colour jump. */
    private val ramp: Bitmap = Bitmap.createBitmap(1200, 400, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until width) for (y in 0 until height) setPixel(x, y, Color.rgb(x * 255 / width, 255 - x * 255 / width, (y * 255 / height)))
    }

    private val project = Project(
        "p", "p", Ratio.Square, slides = 3, background = Background(color = 0xFFFFFFFF),
        items = listOf(
            PhotoItem("a", Frame(1620f, 540f, 3000f, 1000f), "ramp", 1200, 400),
            TextItem("t", Frame(1080f, 900f, 600f, 120f), "Across the cut", size = 72f, color = 0xFF000000),
        ),
    )

    private fun render(p: Project, w: Int, h: Int, dx: Float): Bitmap {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.translate(-dx, 0f)
        renderer.draw(c, p, { if (it == "ramp") ramp else null })
        return b
    }

    private fun close(a: Int, b: Int) =
        abs(Color.red(a) - Color.red(b)) <= 2 && abs(Color.green(a) - Color.green(b)) <= 2 && abs(Color.blue(a) - Color.blue(b)) <= 2

    @Test fun slidesAreExactStripsOfTheCanvas() {
        val whole = render(project, project.width, project.height, 0f)
        for (s in 0 until project.slides) {
            val slide = render(project, project.slideW, project.slideH, s * project.slideW.toFloat())
            for (y in listOf(100, 540, 900)) for (x in listOf(0, 1, 540, 1078, 1079)) {
                val a = slide.getPixel(x, y)
                val b = whole.getPixel(s * project.slideW + x, y)
                assertTrue("slide $s at $x,$y: ${Integer.toHexString(a)} vs ${Integer.toHexString(b)}", close(a, b))
            }
        }
    }

    @Test fun pictureFlowsAcrossTheCut() {
        val left = render(project, 1080, 1080, 0f)
        val right = render(project, 1080, 1080, 1080f)
        // Last column of slide 1 and first of slide 2 are neighbours in the ramp: nearly the same colour.
        val a = left.getPixel(1079, 540)
        val b = right.getPixel(0, 540)
        assertTrue(abs(Color.red(a) - Color.red(b)) <= 2)
        assertTrue(Color.red(a) in 60..200) // and it is the photo, not the background
    }

    @Test fun backgroundShowsOutsidePhotos() {
        val slide = render(project, 1080, 1080, 0f)
        assertEquals(Color.WHITE, slide.getPixel(10, 10))
    }

    @Test fun monoFilterGreysThePhoto() {
        val p = project.replace((project.item("a") as PhotoItem).copy(adjust = Adjust(filter = Filter.Mono)))
        val px = render(p, 1080, 1080, 1080f).getPixel(540, 300)
        assertTrue(abs(Color.red(px) - Color.green(px)) <= 2 && abs(Color.green(px) - Color.blue(px)) <= 2)
    }

    @Test fun borderIsDrawnInside() {
        val p = project.replace((project.item("a") as PhotoItem).copy(style = PhotoStyle(borderWidth = 20f, borderColor = 0xFFFF0000)))
        val slide = render(p, 1080, 1080, 1080f)
        // Photo top edge is at y = 40; the border runs 40..60.
        assertEquals(Color.RED, slide.getPixel(540, 50))
    }

    @Test fun routeIsDrawnInItsRoleColour() {
        val p = Project("p", "p", Ratio.Square, slides = 1, theme = io.github.darkxace01.unroll.model.Themes.Paper, items = listOf(
            io.github.darkxace01.unroll.model.RouteItem("r", listOf(io.github.darkxace01.unroll.model.Point(100f, 540f), io.github.darkxace01.unroll.model.Point(980f, 540f)),
                smooth = false, width = 20f, dash = io.github.darkxace01.unroll.model.Dash.Solid, start = io.github.darkxace01.unroll.model.Cap.None, end = io.github.darkxace01.unroll.model.Cap.None,
                colorRole = io.github.darkxace01.unroll.model.ColorRole.Accent),
        ))
        val px = render(p, 1080, 1080, 0f).getPixel(540, 540)
        assertEquals(io.github.darkxace01.unroll.model.Themes.Paper.accent.toInt(), px)
        // Half drawn: the far end is still background.
        val b = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
        renderer.draw(Canvas(b), p, { null }, look = { io.github.darkxace01.unroll.model.Look(reveal = 0.3f) })
        assertEquals(Color.WHITE, b.getPixel(900, 540))
        assertEquals(io.github.darkxace01.unroll.model.Themes.Paper.accent.toInt(), b.getPixel(200, 540))
    }

    @Test fun polaroidLeavesAPaperBorder() {
        val item = (project.item("a") as PhotoItem).copy(frame = Frame(540f, 540f, 600f, 700f), style = PhotoStyle(frame = io.github.darkxace01.unroll.model.FrameKind.Polaroid))
        val p = project.copy(slides = 1, items = listOf(item))
        val bmp = render(p, 1080, 1080, 0f)
        // Bottom strip of the polaroid is paper, the middle is the photo.
        // Paper (with a faint sheen, so within a few levels).
        val got = bmp.getPixel(540, 540 + 350 - 40); val want = p.theme.paper.toInt()
        for (sh in listOf(16, 8, 0)) assertTrue("paper", kotlin.math.abs(((got shr sh) and 0xFF) - ((want shr sh) and 0xFF)) <= 10)
        assertTrue(bmp.getPixel(540, 400) != p.theme.paper.toInt())
    }

    @Test fun textWrapsToItsBox() {
        val t = TextItem("t", Frame(0f, 0f, 300f, 10f), "A long caption that has to wrap", size = 60f)
        assertTrue(renderer.layout(t).lineCount > 1)
        assertTrue(renderer.textHeight(t) > 120f)
    }
}
