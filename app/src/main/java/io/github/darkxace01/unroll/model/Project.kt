package io.github.darkxace01.unroll.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Instagram's post shapes, in the exact pixels Instagram keeps (1080 wide). Canvas units are these
 * pixels, so the canvas is [slideW] × slides wide and exporting is just slicing it.
 */
@Serializable
enum class Ratio(val slideW: Int, val slideH: Int, val label: String) {
    Portrait(1080, 1350, "4:5"),
    Square(1080, 1080, "1:1"),
    Tall(1080, 1440, "3:4"),
    Story(1080, 1920, "9:16"),
}

const val MAX_SLIDES = 20

/** A rectangle in canvas units, rotated [rotation] degrees about its centre. */
@Serializable
data class Frame(
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val rotation: Float = 0f,
) {
    val left get() = cx - w / 2
    val right get() = cx + w / 2
    val top get() = cy - h / 2
    val bottom get() = cy + h / 2

    fun moved(dx: Float, dy: Float) = copy(cx = cx + dx, cy = cy + dy)

    /** The four corners after rotation, clockwise from top-left. */
    fun corners(): List<Point> {
        val r = Math.toRadians(rotation.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        return listOf(-w / 2 to -h / 2, w / 2 to -h / 2, w / 2 to h / 2, -w / 2 to h / 2).map { (x, y) ->
            Point(cx + x * c - y * s, cy + x * s + y * c)
        }
    }

    /** Axis-aligned box around the rotated frame. */
    fun bounds(): Box {
        if (rotation % 360f == 0f) return Box(left, top, right, bottom)
        val cs = corners()
        return Box(cs.minOf { it.x }, cs.minOf { it.y }, cs.maxOf { it.x }, cs.maxOf { it.y })
    }

    /** Whether a canvas point falls inside the rotated frame. */
    fun contains(x: Float, y: Float): Boolean {
        val local = toLocal(x, y)
        return abs(local.x) <= w / 2 && abs(local.y) <= h / 2
    }

    /** A canvas point in the frame's own unrotated coordinates, (0,0) at its centre. */
    fun toLocal(x: Float, y: Float): Point {
        val r = Math.toRadians(-rotation.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        val dx = x - cx
        val dy = y - cy
        return Point(dx * c - dy * s, dx * s + dy * c)
    }

    /** A point in the frame's own coordinates, back on the canvas. */
    fun toCanvas(lx: Float, ly: Float): Point {
        val r = Math.toRadians(rotation.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        return Point(cx + lx * c - ly * s, cy + lx * s + ly * c)
    }
}

@Serializable
data class Point(val x: Float, val y: Float)

@Serializable
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    val cx get() = (left + right) / 2
    val cy get() = (top + bottom) / 2
    fun overlaps(o: Box) = left < o.right && o.left < right && top < o.bottom && o.top < bottom
}

/** A region of a photo in 0..1 of its (upright) width and height. Used for faces. */
@Serializable
data class NormRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/**
 * How the photo sits in its frame. The photo always fills the frame (cover); [zoom] ≥ 1 zooms in
 * further, and [panX]/[panY] in -1..1 slide it within whatever overflows (0 = centred).
 */
@Serializable
data class Crop(val zoom: Float = 1f, val panX: Float = 0f, val panY: Float = 0f, val flipX: Boolean = false)

/** Non-destructive photo adjustments, each -1..1 with 0 meaning untouched. */
@Serializable
data class Adjust(
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val fade: Float = 0f,
    val vignette: Float = 0f,
    val filter: Filter = Filter.None,
    /** 0..1, how strongly [filter] applies. */
    val filterStrength: Float = 1f,
) {
    val isIdentity get() = this == Adjust()
}

@Serializable
enum class Filter(val label: String) {
    None("Original"), Warm("Warm"), Cool("Cool"), Golden("Golden"), Film("Film"), Fade("Faded"),
    Vivid("Vivid"), Mono("Mono"), Noir("Noir"), Sepia("Sepia"), Teal("Teal & orange"), Dusk("Dusk"), Matte("Matte"),
}

/** Colours are ARGB packed in a Long so they serialise cleanly. */
@Serializable
data class PhotoStyle(
    val cornerRadius: Float = 0f,
    val borderWidth: Float = 0f,
    val borderColor: Long = 0xFFFFFFFF,
    val shadow: Float = 0f,
    /** 0..1. */
    val opacity: Float = 1f,
)

@Serializable
enum class TextAlign { Start, Center, End }

@Serializable
enum class FontKey(val label: String) {
    Sans("Sans"), Serif("Serif"), Mono("Mono"), Condensed("Condensed"), Display("Display"),
    Elegant("Elegant"), ElegantItalic("Elegant italic"), Hand("Handwritten"), Pixel("Pixel"), Grotesk("Grotesk"),
}

@Serializable
sealed interface Item {
    val id: String
    val frame: Frame
    fun withFrame(f: Frame): Item
}

@Serializable
@SerialName("photo")
data class PhotoItem(
    override val id: String,
    override val frame: Frame,
    /** content:// URI from the photo picker (persisted permission) or MediaStore. */
    val uri: String,
    /** Upright pixel size of the source (EXIF rotation already applied). */
    val srcW: Int,
    val srcH: Int,
    val crop: Crop = Crop(),
    val adjust: Adjust = Adjust(),
    val style: PhotoStyle = PhotoStyle(),
    /** Faces found on the photo, if it's been looked at. Null = not looked yet. */
    val faces: List<NormRect>? = null,
    /** EXIF date (epoch ms) and place, for labels and trip mode. */
    val takenAt: Long? = null,
    val lat: Double? = null,
    val lng: Double? = null,
) : Item {
    override fun withFrame(f: Frame) = copy(frame = f)
    val aspect get() = srcW.toFloat() / srcH.coerceAtLeast(1)
}

@Serializable
@SerialName("text")
data class TextItem(
    override val id: String,
    override val frame: Frame,
    val text: String,
    val font: FontKey = FontKey.Sans,
    /** Text size in canvas px. */
    val size: Float = 72f,
    val color: Long = 0xFFFFFFFF,
    val align: TextAlign = TextAlign.Center,
    val bold: Boolean = false,
    val letterSpacing: Float = 0f,
    val lineHeight: Float = 1.15f,
    val outlineWidth: Float = 0f,
    val outlineColor: Long = 0xFF000000,
    val shadow: Float = 0f,
    /** A rounded box behind the text, or null. */
    val pillColor: Long? = null,
) : Item {
    override fun withFrame(f: Frame) = copy(frame = f)
}

@Serializable
@SerialName("shape")
data class ShapeItem(
    override val id: String,
    override val frame: Frame,
    val kind: ShapeKind,
    val color: Long = 0xFFFFFFFF,
    val stroke: Float = 0f,
) : Item {
    override fun withFrame(f: Frame) = copy(frame = f)
}

@Serializable
enum class ShapeKind(val label: String) { Rect("Box"), Circle("Circle"), Line("Line"), Arrow("Arrow"), Heart("Heart"), Star("Star"), Pin("Pin"), Plane("Plane"), Tape("Tape") }

@Serializable
enum class BackgroundKind { Solid, Gradient }

@Serializable
data class Background(
    val kind: BackgroundKind = BackgroundKind.Solid,
    val color: Long = 0xFFFFFFFF,
    val color2: Long = 0xFFE8E2D6,
    /** Gradient direction in degrees; 0 = left to right across the whole canvas. */
    val angle: Float = 0f,
)

@Serializable
data class Project(
    val id: String,
    val name: String,
    val ratio: Ratio = Ratio.Portrait,
    val slides: Int = 3,
    val background: Background = Background(),
    /** Bottom to top. */
    val items: List<Item> = emptyList(),
    val created: Long = 0,
    val updated: Long = 0,
) {
    val slideW get() = ratio.slideW
    val slideH get() = ratio.slideH
    val width get() = slideW * slides
    val height get() = slideH

    fun item(id: String) = items.firstOrNull { it.id == id }

    fun replace(item: Item) = copy(items = items.map { if (it.id == item.id) item else it })

    fun remove(id: String) = copy(items = items.filterNot { it.id == id })

    fun add(item: Item) = copy(items = items + item)

    fun bringToFront(id: String) = item(id)?.let { copy(items = items.filterNot { i -> i.id == id } + it) } ?: this

    fun sendToBack(id: String) = item(id)?.let { copy(items = listOf(it) + items.filterNot { i -> i.id == id }) } ?: this

    /** Topmost item under a canvas point. */
    fun hit(x: Float, y: Float): Item? = items.lastOrNull { it.frame.contains(x, y) }

    /** The slide a canvas x falls in. */
    fun slideAt(x: Float) = (x / slideW).toInt().coerceIn(0, slides - 1)

    /**
     * Changes the shape of the slides. Items keep their place relative to the slide they were on, scaled
     * by height, so a layout made for 4:5 still looks right at 1:1.
     */
    fun withRatio(r: Ratio): Project {
        if (r == ratio) return this
        val sy = r.slideH.toFloat() / slideH
        val sx = r.slideW.toFloat() / slideW
        val s = minOf(sx, sy)
        return copy(ratio = r, items = items.map { item ->
            val f = item.frame
            val slide = slideAt(f.cx)
            val inSlideX = f.cx - slide * slideW
            item.withFrame(f.copy(cx = slide * r.slideW + inSlideX * sx, cy = f.cy * sy, w = f.w * s, h = f.h * s))
        }.map { if (it is TextItem) it.copy(size = it.size * s) else it })
    }

    /** Adds an empty slide after [index] (−1 = at the start), pushing everything after it right. */
    fun insertSlide(index: Int): Project {
        if (slides >= MAX_SLIDES) return this
        val at = (index + 1) * slideW.toFloat()
        return copy(slides = slides + 1, items = items.map { if (it.frame.cx >= at) it.withFrame(it.frame.moved(slideW.toFloat(), 0f)) else it })
    }

    /** Removes slide [index]. Items centred on it go with it; later ones shift left. */
    fun removeSlide(index: Int): Project {
        if (slides <= 1) return this
        val from = index * slideW.toFloat()
        val to = from + slideW
        return copy(
            slides = slides - 1,
            items = items.filterNot { it.frame.cx in from..<to }.map { if (it.frame.cx >= to) it.withFrame(it.frame.moved(-slideW.toFloat(), 0f)) else it },
        )
    }

    /** Swaps two whole slides, with everything centred on them. */
    fun moveSlide(from: Int, to: Int): Project {
        if (from == to || from !in 0 until slides || to !in 0 until slides) return this
        val order = (0 until slides).toMutableList().apply { add(to, removeAt(from)) }
        val newIndexOf = IntArray(slides).also { order.forEachIndexed { newIdx, old -> it[old] = newIdx } }
        return copy(items = items.map { item ->
            val old = slideAt(item.frame.cx)
            item.withFrame(item.frame.moved((newIndexOf[old] - old) * slideW.toFloat(), 0f))
        })
    }
}
