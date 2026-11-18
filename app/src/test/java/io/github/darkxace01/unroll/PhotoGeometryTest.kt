package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.model.Crop
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.NormRect
import io.github.darkxace01.unroll.model.PhotoGeometry
import io.github.darkxace01.unroll.model.PhotoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoGeometryTest {
    @Test fun landscapeInSquareFrameCropsSides() {
        val v = PhotoGeometry.visible(4000, 2000, 500f, 500f, Crop())
        assertEquals(0.25f, v.left, 1e-4f)
        assertEquals(0.75f, v.right, 1e-4f)
        assertEquals(0f, v.top, 1e-4f)
        assertEquals(1f, v.bottom, 1e-4f)
    }

    @Test fun panSlidesToTheEdge() {
        val v = PhotoGeometry.visible(4000, 2000, 500f, 500f, Crop(panX = 1f))
        assertEquals(0.5f, v.left, 1e-4f)
        assertEquals(1f, v.right, 1e-4f)
        val l = PhotoGeometry.visible(4000, 2000, 500f, 500f, Crop(panX = -1f))
        assertEquals(0f, l.left, 1e-4f)
    }

    @Test fun zoomShrinksWindow() {
        val v = PhotoGeometry.visible(1000, 1000, 500f, 500f, Crop(zoom = 2f))
        assertEquals(0.5f, v.right - v.left, 1e-4f)
        assertEquals(0.5f, (v.left + v.right) / 2, 1e-4f)
    }

    @Test fun regionMapsOntoCanvas() {
        // Square photo filling a square frame at (100..600, 0..500): photo centre is frame centre.
        val item = PhotoItem("a", Frame(350f, 250f, 500f, 500f), "u", 1000, 1000)
        val box = PhotoGeometry.regionOnCanvas(item, NormRect(0.4f, 0.4f, 0.6f, 0.6f))!!
        assertEquals(300f, box.left, 0.01f)
        assertEquals(400f, box.right, 0.01f)
    }

    @Test fun croppedOutRegionIsNull() {
        val item = PhotoItem("a", Frame(250f, 250f, 500f, 500f), "u", 4000, 2000)
        assertNull(PhotoGeometry.regionOnCanvas(item, NormRect(0.0f, 0.2f, 0.1f, 0.4f)))
        assertNotNull(PhotoGeometry.regionOnCanvas(item, NormRect(0.4f, 0.2f, 0.6f, 0.4f)))
    }

    @Test fun flippedRegionMirrors() {
        val item = PhotoItem("a", Frame(250f, 250f, 500f, 500f), "u", 1000, 1000, crop = Crop(flipX = true))
        val box = PhotoGeometry.regionOnCanvas(item, NormRect(0f, 0f, 0.2f, 0.2f))!!
        assertEquals(400f, box.left, 0.01f)
    }

    @Test fun sharpnessFlagsSmallPhotos() {
        val big = PhotoItem("a", Frame(540f, 675f, 1080f, 1350f), "u", 4000, 5000)
        val tiny = big.copy(srcW = 400, srcH = 500)
        assertTrue(PhotoGeometry.sharpness(big) > 1f)
        assertTrue(PhotoGeometry.sharpness(tiny) < 0.6f)
    }

    @Test fun fitKeepsAspect() {
        val (w, h) = PhotoGeometry.fit(2f, 1000f, 1000f)
        assertEquals(1000f, w); assertEquals(500f, h)
        val (w2, h2) = PhotoGeometry.fit(0.5f, 1000f, 1000f)
        assertEquals(500f, w2); assertEquals(1000f, h2)
    }
}
