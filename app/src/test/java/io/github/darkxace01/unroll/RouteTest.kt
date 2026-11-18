package io.github.darkxace01.unroll

import io.github.darkxace01.unroll.model.Box
import io.github.darkxace01.unroll.model.Frame
import io.github.darkxace01.unroll.model.Point
import io.github.darkxace01.unroll.model.RouteItem
import io.github.darkxace01.unroll.model.RouteMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RouteTest {
    private val zig = listOf(Point(0f, 0f), Point(100f, 100f), Point(200f, 0f), Point(300f, 100f))
    private val route = RouteItem("r", zig)

    @Test fun smoothCurvePassesThroughEveryPoint() {
        val poly = RouteMath.catmullRom(zig, 10)
        for (p in zig) assertTrue(poly.any { abs(it.x - p.x) < 0.01f && abs(it.y - p.y) < 0.01f })
    }

    @Test fun atEndsAndMiddle() {
        val line = listOf(Point(0f, 0f), Point(100f, 0f))
        assertEquals(0f, RouteMath.at(line, 0f).first.x, 0.01f)
        assertEquals(50f, RouteMath.at(line, 0.5f).first.x, 0.01f)
        assertEquals(100f, RouteMath.at(line, 1f).first.x, 0.01f)
        assertEquals(0f, RouteMath.at(line, 0.5f).second, 0.01f)
        assertEquals(180f, RouteMath.at(listOf(Point(100f, 0f), Point(0f, 0f)), 0.5f).second, 0.01f)
    }

    @Test fun partialIsTheRightLength() {
        val poly = RouteMath.sample(route)
        val half = RouteMath.partial(poly, 0.5f)
        assertEquals(RouteMath.length(poly) / 2, RouteMath.length(half), 1f)
        assertEquals(poly, RouteMath.partial(poly, 1f))
    }

    @Test fun fractionAtXGrowsLeftToRight() {
        val poly = RouteMath.sample(route)
        val a = RouteMath.fractionAtX(poly, 50f)
        val b = RouteMath.fractionAtX(poly, 150f)
        val c = RouteMath.fractionAtX(poly, 290f)
        assertTrue(a < b && b < c)
        assertEquals(1f, RouteMath.fractionAtX(poly, 1000f), 0.001f)
    }

    @Test fun simplifyKeepsCorners() {
        val noisy = (0..100).map { Point(it.toFloat(), if (it < 50) 0f else (it - 50).toFloat()) }
        val s = RouteMath.simplify(noisy, 1f)
        assertTrue(s.size in 3..4)
        assertEquals(noisy.first(), s.first()); assertEquals(noisy.last(), s.last())
    }

    @Test fun tapNearTheLineHits() {
        val straight = RouteItem("r", listOf(Point(0f, 0f), Point(100f, 0f)), smooth = false, width = 8f)
        assertTrue(straight.hits(50f, 10f, 8f))
        assertTrue(!straight.hits(50f, 40f, 8f))
    }

    @Test fun frameIsTheBoundsAndMovingMovesPoints() {
        val f = route.frame
        assertEquals(150f, f.cx); assertEquals(300f, f.w)
        val moved = route.withFrame(f.moved(10f, 20f)) as RouteItem
        assertEquals(Point(10f, 20f), moved.points.first())
        val scaled = route.withFrame(Frame(f.cx, f.cy, f.w * 2, f.h)) as RouteItem
        assertEquals(-150f, scaled.points.first().x, 0.01f)
    }

    @Test fun throughVisitsEveryBoxInOrder() {
        val boxes = listOf(Box(500f, 100f, 700f, 300f), Box(100f, 100f, 300f, 300f), Box(900f, 400f, 1100f, 600f))
        val pts = RouteMath.through(boxes, 1000f)
        assertEquals(boxes.size + 2, pts.size)
        val xs = pts.map { it.x }
        assertEquals(xs.sorted(), xs)
        for (p in pts) assertTrue(p.y in 40f..960f)
    }

    @Test fun addAndRemoveBends() {
        val two = RouteItem("r", listOf(Point(0f, 0f), Point(100f, 0f)))
        val three = RouteMath.addPoint(two)
        assertEquals(Point(50f, 0f), three.points[1])
        assertEquals(two.points, RouteMath.removePoint(three, 1).points)
        assertEquals(2, RouteMath.removePoint(two, 0).points.size) // never fewer than 2
    }

    @Test fun stopsAreInOrderAlongTheRoute() {
        val s = RouteMath.stops(route)
        assertEquals(2, s.size)
        assertTrue(s[0].second in 0.1f..0.5f && s[1].second in 0.5f..0.9f)
    }
}
