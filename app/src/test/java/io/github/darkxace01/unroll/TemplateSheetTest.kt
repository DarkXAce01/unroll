package io.github.darkxace01.unroll

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.darkxace01.unroll.model.NewPhoto
import io.github.darkxace01.unroll.model.Templates
import io.github.darkxace01.unroll.model.TripInfo
import io.github.darkxace01.unroll.render.Fonts
import io.github.darkxace01.unroll.render.Renderer
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders every template with stand-in photos to app/build/outputs/templates, to look at the designs.
 * Off by default: ./gradlew testPlayDebugUnitTest -PtemplateSheets
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class)
class TemplateSheetTest {
    @Test fun renderAll() {
        assumeTrue(System.getProperty("unroll.templateSheets") == "true")
        val renderer = Renderer(Fonts(ApplicationProvider.getApplicationContext()))
        val out = File("build/outputs/templates").apply { mkdirs() }
        // Stand-in photos: soft gradients, landscape and portrait, so the layouts read.
        val hues = intArrayOf(0xFF6C9BC2.toInt(), 0xFFE0A06A.toInt(), 0xFF7FB38A.toInt(), 0xFFC97B8E.toInt(), 0xFF8C7BC9.toInt(), 0xFFD9C36A.toInt())
        val photos = (0 until 7).map { NewPhoto("p$it", if (it % 3 == 1) 900 else 1200, if (it % 3 == 1) 1200 else 900) }
        val bmps = photos.mapIndexed { i, ph ->
            ph.uri to Bitmap.createBitmap(ph.w, ph.h, Bitmap.Config.ARGB_8888).also { b ->
                val c = Canvas(b)
                c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, ph.w.toFloat(), ph.h.toFloat(), hues[i % hues.size], Color.WHITE, Shader.TileMode.CLAMP) })
                c.drawCircle(ph.w * 0.5f, ph.h * 0.45f, ph.h * 0.12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000 })
            }
        }.toMap()
        var n = 0
        for (t in Templates.all) {
            val p = Templates.build(t, photos, TripInfo("Lisbon", "12–16 May", "Lisbon", 5), "p", t.name, 0, { "i${n++}" })
            val k = minOf(4, p.slides)
            val scale = 0.3f
            val gap = 12
            val bmp = Bitmap.createBitmap(((p.slideW * scale).toInt() + gap) * k, (p.slideH * scale).toInt(), Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(0xFF333333.toInt())
            for (s in 0 until k) {
                c.save()
                c.translate(s * ((p.slideW * scale).toInt() + gap).toFloat(), 0f)
                c.clipRect(0f, 0f, p.slideW * scale, p.slideH * scale)
                c.scale(scale, scale)
                c.translate(-s * p.slideW.toFloat(), 0f)
                renderer.draw(c, p, { bmps[it] })
                c.restore()
            }
            File(out, "${t.id}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
