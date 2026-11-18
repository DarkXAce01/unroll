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
import io.github.darkxace01.unroll.model.TemplateKits
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
        io.github.darkxace01.unroll.render.SamplePhotos.init(androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().assets)
        // One sheet per style kit (each row a layout), plus one per hand-made template, all with painted sample photos.
        val only = System.getProperty("sheets.only")?.takeIf { it.isNotEmpty() }
        val photoFirst = io.github.darkxace01.unroll.model.PhotoFirst.templates.groupBy { it.shoot }.values.toList()
        val groups = (photoFirst + Templates.all.map { listOf(it) } + TemplateKits.kits.map { k -> Templates.everything.filter { it.kit == k.id } })
            .filter { only == null || it.first().id.startsWith(only) || it.first().kit?.startsWith(only) == true }
        for (group in groups) {
            val scale = 0.2f
            val gap = 10
            val rows = group.map { t -> Templates.build(t, emptyList(), io.github.darkxace01.unroll.templates.previewInfoFor(t.category), "p", t.name, 0, { "i${n++}" }) }
            val first = rows.first()
            val sw = (first.slideW * scale).toInt(); val sh = (first.slideH * scale).toInt()
            val bmp = Bitmap.createBitmap((sw + gap) * 5, (sh + gap) * rows.size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(0xFF333333.toInt())
            rows.forEachIndexed { r, p ->
                for (s in 0 until minOf(5, p.slides)) {
                    c.save()
                    c.translate(s * (sw + gap).toFloat(), r * (sh + gap).toFloat())
                    c.clipRect(0f, 0f, sw.toFloat(), sh.toFloat())
                    c.scale(scale, scale)
                    c.translate(-s * p.slideW.toFloat(), 0f)
                    val t = group[r]
                    renderer.draw(c, p, { null }, samples = true,
                        sampleOrder = io.github.darkxace01.unroll.render.SamplePhotos.order(t.id.hashCode(), io.github.darkxace01.unroll.render.SamplePhotos.tagsFor(t.category.name), t.shoot, wideFirst = t.layout == "pano"))
                    c.restore()
                }
            }
            val name = group.first().shoot?.let { "pf-$it" } ?: group.first().kit ?: group.first().id
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        @Suppress("UNUSED_VARIABLE") val unused = bmps
    }
}
