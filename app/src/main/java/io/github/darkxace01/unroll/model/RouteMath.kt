package io.github.darkxace01.unroll.model

import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Route geometry in plain Kotlin: smoothing (Catmull-Rom), the drawn length, a point and heading at any
 * fraction of the way (for the vehicle and the "draw itself" animation), tap distance, and simplifying a
 * finger's freehand line into a few points.
 */
object RouteMath {

    /** The route as a dense polyline: smoothed through its points, or straight between them. */
    fun sample(r: RouteItem, perSegment: Int = 16): List<Point> = if (r.smooth) catmullRom(r.points, perSegment) else r.points

    fun catmullRom(p: List<Point>, perSegment: Int = 16): List<Point> {
        if (p.size < 3) return p
        val out = ArrayList<Point>(p.size * perSegment)
        for (i in 0 until p.size - 1) {
            val p0 = p[maxOf(i - 1, 0)]
            val p1 = p[i]
            val p2 = p[i + 1]
            val p3 = p[minOf(i + 2, p.size - 1)]
            for (s in 0 until perSegment) {
                val t = s / perSegment.toFloat()
                val t2 = t * t
                val t3 = t2 * t
                fun c(a: Float, b: Float, cc: Float, d: Float) =
                    0.5f * ((2 * b) + (-a + cc) * t + (2 * a - 5 * b + 4 * cc - d) * t2 + (-a + 3 * b - 3 * cc + d) * t3)
                out += Point(c(p0.x, p1.x, p2.x, p3.x), c(p0.y, p1.y, p2.y, p3.y))
            }
        }
        out += p.last()
        return out
    }

    fun length(poly: List<Point>): Float {
        var len = 0f
        for (i in 1 until poly.size) len += hypot(poly[i].x - poly[i - 1].x, poly[i].y - poly[i - 1].y)
        return len
    }

    /** Position and heading (degrees, 0 = pointing right) at fraction [t] of the way along. */
    fun at(poly: List<Point>, t: Float): Pair<Point, Float> {
        if (poly.isEmpty()) return Point(0f, 0f) to 0f
        if (poly.size == 1) return poly[0] to 0f
        val target = length(poly) * t.coerceIn(0f, 1f)
        var walked = 0f
        for (i in 1 until poly.size) {
            val a = poly[i - 1]; val b = poly[i]
            val seg = hypot(b.x - a.x, b.y - a.y)
            if (walked + seg >= target || i == poly.size - 1) {
                val k = if (seg > 0f) ((target - walked) / seg).coerceIn(0f, 1f) else 0f
                val angle = Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat()
                return Point(a.x + (b.x - a.x) * k, a.y + (b.y - a.y) * k) to angle
            }
            walked += seg
        }
        return poly.last() to 0f
    }

    /** The first [t] of the route, for drawing it partway. */
    fun partial(poly: List<Point>, t: Float): List<Point> {
        if (t >= 1f || poly.size < 2) return poly
        if (t <= 0f) return poly.take(1)
        val target = length(poly) * t
        val out = mutableListOf(poly[0])
        var walked = 0f
        for (i in 1 until poly.size) {
            val a = poly[i - 1]; val b = poly[i]
            val seg = hypot(b.x - a.x, b.y - a.y)
            if (walked + seg >= target) {
                val k = if (seg > 0f) (target - walked) / seg else 0f
                out += Point(a.x + (b.x - a.x) * k, a.y + (b.y - a.y) * k)
                return out
            }
            out += b
            walked += seg
        }
        return out
    }

    fun distanceToPolyline(poly: List<Point>, x: Float, y: Float): Float {
        if (poly.isEmpty()) return Float.MAX_VALUE
        if (poly.size == 1) return hypot(poly[0].x - x, poly[0].y - y)
        var best = Float.MAX_VALUE
        for (i in 1 until poly.size) best = minOf(best, distanceToSegment(poly[i - 1], poly[i], x, y))
        return best
    }

    fun distanceToSegment(a: Point, b: Point, x: Float, y: Float): Float {
        val dx = b.x - a.x; val dy = b.y - a.y
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0f) 0f else (((x - a.x) * dx + (y - a.y) * dy) / len2).coerceIn(0f, 1f)
        return hypot(a.x + t * dx - x, a.y + t * dy - y)
    }

    /** Ramer–Douglas–Peucker: keeps the shape of a finger-drawn line with far fewer points. */
    fun simplify(p: List<Point>, epsilon: Float): List<Point> {
        if (p.size < 3) return p
        var maxD = 0f
        var idx = 0
        for (i in 1 until p.size - 1) {
            val d = distanceToSegment(p.first(), p.last(), p[i].x, p[i].y)
            if (d > maxD) { maxD = d; idx = i }
        }
        if (maxD <= epsilon) return listOf(p.first(), p.last())
        val left = simplify(p.subList(0, idx + 1), epsilon)
        val right = simplify(p.subList(idx, p.size), epsilon)
        return left.dropLast(1) + right
    }

    /**
     * A route that visits each box (photos, left to right), weaving a little above and below their
     * middles so it reads as a journey rather than a ruler line. It runs under the photos, so its dashes
     * show in the gaps, tying one photo to the next. Starts and ends a little outside the first and last.
     */
    fun through(boxes: List<Box>, canvasH: Float, lead: Float = 120f): List<Point> {
        if (boxes.isEmpty()) return emptyList()
        val sorted = boxes.sortedBy { it.cx }
        val lo = canvasH * 0.06f
        val hi = canvasH * 0.94f
        val pts = mutableListOf<Point>()
        val first = sorted.first()
        pts += Point(first.left - lead, (first.cy + first.height * 0.3f).coerceIn(lo, hi))
        sorted.forEachIndexed { i, b ->
            val k = if (i % 2 == 0) 0.18f else -0.18f
            pts += Point(b.cx, (b.cy + b.height * k).coerceIn(lo, hi))
        }
        val last = sorted.last()
        pts += Point(last.right + lead, pts.last().y)
        return pts
    }

    /** How far along the route (0..1) it first reaches canvas x [x], walking from its start. */
    fun fractionAtX(poly: List<Point>, x: Float): Float {
        if (poly.size < 2) return 1f
        val total = length(poly).coerceAtLeast(0.001f)
        val goingRight = poly.last().x >= poly.first().x
        var walked = 0f
        for (i in 1 until poly.size) {
            val a = poly[i - 1]; val b = poly[i]
            val seg = hypot(b.x - a.x, b.y - a.y)
            val crosses = if (goingRight) b.x >= x else b.x <= x
            if (crosses) {
                val k = if (b.x != a.x) ((x - a.x) / (b.x - a.x)).coerceIn(0f, 1f) else 1f
                return ((walked + seg * k) / total).coerceIn(0f, 1f)
            }
            walked += seg
        }
        return 1f
    }

    /** The in-between points (not the ends) and how far along the route each sits, 0..1. */
    fun stops(r: RouteItem, perSegment: Int = 16): List<Pair<Point, Float>> {
        if (r.points.size < 3) return emptyList()
        val poly = sample(r, perSegment)
        val total = length(poly).coerceAtLeast(0.001f)
        val step = if (r.smooth) perSegment else 1
        return (1 until r.points.size - 1).map { i -> r.points[i] to length(poly.subList(0, i * step + 1)) / total }
    }

    /** Moves point [index] of a route. */
    fun movePoint(r: RouteItem, index: Int, to: Point) = r.copy(points = r.points.mapIndexed { i, p -> if (i == index) to else p })

    /** Adds a point in the middle of the longest straight stretch. */
    fun addPoint(r: RouteItem): RouteItem {
        if (r.points.size < 2) return r
        var best = 1
        var bestLen = -1f
        for (i in 1 until r.points.size) {
            val l = hypot(r.points[i].x - r.points[i - 1].x, r.points[i].y - r.points[i - 1].y)
            if (l > bestLen) { bestLen = l; best = i }
        }
        val a = r.points[best - 1]; val b = r.points[best]
        return r.copy(points = r.points.toMutableList().apply { add(best, Point((a.x + b.x) / 2, (a.y + b.y) / 2)) })
    }

    fun removePoint(r: RouteItem, index: Int): RouteItem =
        if (r.points.size <= 2 || index !in r.points.indices) r else r.copy(points = r.points.filterIndexed { i, _ -> i != index })
}
