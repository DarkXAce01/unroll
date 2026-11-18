package io.github.darkxace01.unroll.model

/**
 * Which part of a photo shows inside its frame. The photo covers the frame (like CSS object-fit:
 * cover), then [Crop.zoom] and [Crop.pan] pick the window. Shared by the screen, export and seam checks
 * so they always agree.
 */
object PhotoGeometry {

    /**
     * Where the photo itself sits inside the item, in the item's own coordinates (centre = 0,0). Mounts
     * like a polaroid take a border; the rest show the photo edge to edge.
     */
    fun inner(item: PhotoItem): Box = inner(item.frame.w, item.frame.h, item.style.frame)

    fun inner(w: Float, h: Float, kind: FrameKind): Box {
        val m = minOf(w, h)
        val (l, t, r, b) = when (kind) {
            FrameKind.Polaroid -> listOf(0.055f * w, 0.055f * w, 0.055f * w, 0.2f * w)
            FrameKind.Film -> listOf(0.03f * w, 0.13f * h, 0.03f * w, 0.13f * h)
            FrameKind.Negative -> listOf(0.025f * w, 0.15f * h, 0.025f * w, 0.15f * h)
            FrameKind.Stamp -> listOf(0.075f * m, 0.075f * m, 0.075f * m, 0.075f * m)
            FrameKind.Instax -> listOf(0.075f * w, 0.08f * h, 0.075f * w, 0.2f * h)
            FrameKind.Wide -> listOf(0.042f * w, 0.08f * h, 0.042f * w, 0.19f * h)
            FrameKind.Square -> listOf(0.07f * w, 0.08f * h, 0.07f * w, 0.2f * h)
            FrameKind.Slide -> listOf(0.17f * m, 0.17f * m, 0.17f * m, 0.17f * m)
            FrameKind.Deckle -> listOf(0.065f * m, 0.065f * m, 0.065f * m, 0.065f * m)
            FrameKind.Postcard, FrameKind.Corners -> listOf(0.045f * m, 0.045f * m, 0.045f * m, 0.045f * m)
            FrameKind.Notebook -> listOf(0.15f * w, 0.09f * h, 0.08f * w, 0.24f * h)
            FrameKind.Phone -> listOf(0.045f * w, 0.03f * h, 0.045f * w, 0.03f * h)
            FrameKind.Camera -> listOf(0.06f * w, 0.13f * h, 0.32f * w, 0.13f * h)
            FrameKind.Browser -> { val bar = minOf(h * 0.13f, w * 0.1f); listOf(0.012f * m, bar, 0.012f * m, 0.012f * m) }
            FrameKind.Player -> { val art = minOf(w * 0.84f, h * 0.62f); listOf((w - art) / 2, (w - art) / 2, (w - art) / 2, h - (w - art) / 2 - art) }
            FrameKind.Message -> listOf(0.03f * m, 0.03f * m, 0.03f * m, 0.03f * m + 0.07f * h)
            FrameKind.Popup -> listOf(0.06f * w, 0.17f * h, 0.06f * w, 0.19f * h)
            FrameKind.Lace -> listOf(0.11f * m, 0.11f * m, 0.11f * m, 0.11f * m)
            FrameKind.Window -> listOf(0.07f * m, 0.07f * m, 0.07f * m, 0.07f * m)
            FrameKind.Ticket -> listOf(0.045f * w, 0.1f * h, 0.3f * w, 0.1f * h)
            else -> listOf(0f, 0f, 0f, 0f)
        }
        return Box(-w / 2 + l, -h / 2 + t, w / 2 - r, h / 2 - b)
    }

    /** Frames drawn on a mount (paper, a device, a ticket) with the photo set inside; the rest clip the photo to a shape. */
    fun mounted(kind: FrameKind) = kind in MOUNTED

    private val MOUNTED = setOf(
        FrameKind.Polaroid, FrameKind.Film, FrameKind.Negative, FrameKind.Stamp, FrameKind.Instax, FrameKind.Wide, FrameKind.Square,
        FrameKind.Slide, FrameKind.Deckle, FrameKind.Postcard, FrameKind.Corners, FrameKind.Notebook, FrameKind.Phone, FrameKind.Camera,
        FrameKind.Browser, FrameKind.Player, FrameKind.Message, FrameKind.Popup, FrameKind.Lace, FrameKind.Window, FrameKind.Ticket,
    )

    /** The shape a frame looks right at (width / height), or null when any shape works. */
    fun naturalAspect(kind: FrameKind): Float? = when (kind) {
        FrameKind.Polaroid -> 0.84f
        FrameKind.Instax -> 0.63f
        FrameKind.Wide -> 1.26f
        FrameKind.Square -> 0.84f
        FrameKind.Slide, FrameKind.Circle, FrameKind.Heart -> 1f
        FrameKind.Negative -> 1.4f
        FrameKind.Postcard -> 1.45f
        FrameKind.Notebook -> 0.76f
        FrameKind.Phone -> 0.49f
        FrameKind.Camera -> 1.5f
        FrameKind.Browser -> 1.2f
        FrameKind.Player -> 0.66f
        FrameKind.Popup -> 0.8f
        FrameKind.Lace, FrameKind.Oval -> 0.78f
        FrameKind.Ticket -> 2.1f
        FrameKind.Stamp -> 0.82f
        else -> null
    }

    /** The largest box of [aspect] centred in [box] (for fitting a frame of a set shape into a layout slot). */
    fun fitInto(box: Box, aspect: Float?): Box {
        if (aspect == null || box.width <= 0f || box.height <= 0f) return box
        val ba = box.width / box.height
        return if (ba > aspect) { val w = box.height * aspect; Box(box.cx - w / 2, box.top, box.cx + w / 2, box.bottom) }
        else { val h = box.width / aspect; Box(box.left, box.cy - h / 2, box.right, box.cy + h / 2) }
    }

    /** The visible window of the source, in 0..1 of its width and height. */
    fun visible(item: PhotoItem): NormRect {
        val i = inner(item)
        return visible(item.srcW, item.srcH, i.width, i.height, item.crop)
    }

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
        val i = inner(item)
        return item.frame.toCanvas(i.left + fx * i.width, i.top + fy * i.height)
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
        val i = inner(item)
        return minOf(pxW / i.width.coerceAtLeast(1f), pxH / i.height.coerceAtLeast(1f))
    }

    /** The frame size that shows the whole photo at its own shape, fitting inside [maxW] × [maxH]. */
    fun fit(aspect: Float, maxW: Float, maxH: Float): Pair<Float, Float> =
        if (aspect > maxW / maxH) maxW to maxW / aspect else maxH * aspect to maxH
}
