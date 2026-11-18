package io.github.darkxace01.unroll.model

/**
 * Which part of a photo shows inside its frame. The photo covers the frame (like CSS object-fit:
 * cover), then [Crop.zoom] and [Crop.pan] pick the window. Shared by the screen, export and seam checks
 * so they always agree.
 */
object PhotoGeometry {

    /** The visible window of the source, in 0..1 of its width and height. */
    fun visible(item: PhotoItem): NormRect = visible(item.srcW, item.srcH, item.frame.w, item.frame.h, item.crop)

    fun visible(srcW: Int, srcH: Int, frameW: Float, frameH: Float, crop: Crop): NormRect {
        val sa = srcW.toFloat() / srcH.coerceAtLeast(1)
        val fa = frameW / frameH.coerceAtLeast(1f)
        val zoom = crop.zoom.coerceAtLeast(1f)
        var vw = 1f
        var vh = 1f
        if (sa > fa) vw = fa / sa else vh = sa / fa
        vw /= zoom
        vh /= zoom
        val cx = 0.5f + crop.panX.coerceIn(-1f, 1f) * (1f - vw) / 2f
        val cy = 0.5f + crop.panY.coerceIn(-1f, 1f) * (1f - vh) / 2f
        return NormRect(cx - vw / 2, cy - vh / 2, cx + vw / 2, cy + vh / 2)
    }

    /** A point on the photo (0..1), where it lands on the canvas. */
    fun photoToCanvas(item: PhotoItem, nx: Float, ny: Float): Point {
        val v = visible(item)
        var fx = (nx - v.left) / (v.right - v.left)
        val fy = (ny - v.top) / (v.bottom - v.top)
        if (item.crop.flipX) fx = 1f - fx
        val f = item.frame
        return f.toCanvas((fx - 0.5f) * f.w, (fy - 0.5f) * f.h)
    }

    /**
     * A region of the photo as a box on the canvas, clipped to the frame (a face half out of the frame is
     * only the visible half). Null when it's cropped out entirely.
     */
    fun regionOnCanvas(item: PhotoItem, r: NormRect): Box? {
        val v = visible(item)
        val l = maxOf(r.left, v.left)
        val t = maxOf(r.top, v.top)
        val rr = minOf(r.right, v.right)
        val b = minOf(r.bottom, v.bottom)
        if (rr <= l || b <= t) return null
        val pts = listOf(photoToCanvas(item, l, t), photoToCanvas(item, rr, t), photoToCanvas(item, rr, b), photoToCanvas(item, l, b))
        return Box(pts.minOf { it.x }, pts.minOf { it.y }, pts.maxOf { it.x }, pts.maxOf { it.y })
    }

    /**
     * How many source pixels land on each canvas pixel, along the narrower side. Below ~0.66 the photo is
     * being stretched and will look soft once Instagram shows it at 1080.
     */
    fun sharpness(item: PhotoItem): Float {
        val v = visible(item)
        val pxW = (v.right - v.left) * item.srcW
        val pxH = (v.bottom - v.top) * item.srcH
        return minOf(pxW / item.frame.w.coerceAtLeast(1f), pxH / item.frame.h.coerceAtLeast(1f))
    }

    /** The frame size that shows the whole photo at its own shape, fitting inside [maxW] × [maxH]. */
    fun fit(aspect: Float, maxW: Float, maxH: Float): Pair<Float, Float> =
        if (aspect > maxW / maxH) maxW to maxW / aspect else maxH * aspect to maxH
}
