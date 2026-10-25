package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconDepthTest {
    private class Recorder {
        val points = mutableListOf<Pair<Float, Float>>()
        val curves = mutableListOf<FloatArray>()
        fun move(x: Float, y: Float) { points += x to y }
        fun line(x: Float, y: Float) { points += x to y }
        fun cubic(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) { curves += floatArrayOf(x1, y1, x2, y2, x3, y3); points += x3 to y3 }
    }

    private fun trace(w: Float, h: Float, r: Float) = Recorder().also { ContinuousCorners.trace(w, h, r, it::move, it::line, it::cubic) }

    @Test fun `the outline closes where it started and stays inside the box`() {
        val path = trace(100f, 100f, 22.37f)
        assertEquals(path.points.first().first, path.points.last().first, .001f)
        assertEquals(path.points.first().second, path.points.last().second, .001f)
        assertTrue(path.points.all { (x, y) -> x in 0f..100f && y in 0f..100f })
        assertEquals(12, path.curves.size)
    }

    @Test fun `each corner eases in over about one and a half radii, like iOS`() {
        val r = 22.37f
        val path = trace(100f, 100f, r)
        assertEquals(ContinuousCorners.SPAN * r, path.points.first().first, .001f)
        assertEquals(100f - ContinuousCorners.SPAN * r, path.points[1].first, .001f)
    }

    @Test fun `the corner is as deep as a round one at forty-five degrees`() {
        val r = 20f
        val curves = trace(100f, 100f, r).curves
        val start = curves[0]; val middle = curves[1]
        val x = (start[4] + 3 * middle[0] + 3 * middle[2] + middle[4]) / 8f
        val y = (start[5] + 3 * middle[1] + 3 * middle[3] + middle[5]) / 8f
        assertEquals(.2969f * r, y, .01f)
        assertEquals(.2969f * r, 100f - x, .01f)
    }

    @Test fun `the curves meet without a visible kink`() {
        val curves = trace(100f, 100f, 22.37f).curves
        for (i in 0 until 2) {
            val a = curves[i]; val b = curves[i + 1]
            val inX = a[4] - a[2]; val inY = a[5] - a[3]
            val outX = b[0] - a[4]; val outY = b[1] - a[5]
            val sine = (inX * outY - inY * outX) / (kotlin.math.hypot(inX, inY) * kotlin.math.hypot(outX, outY))
            assertTrue(kotlin.math.abs(sine) < .05f)
        }
    }

    @Test fun `a radius too big for the box is capped so the corners never cross`() {
        assertEquals(50f / ContinuousCorners.SPAN, ContinuousCorners.radius(100f, 100f, 60f), .001f)
        assertEquals(10f, ContinuousCorners.radius(100f, 100f, 10f), 0f)
    }

    @Test fun `lighting goes only on icons whose art reaches the edges`() {
        assertTrue(fillsEdges(144, 144) { _, _ -> 255 })
        assertFalse(fillsEdges(144, 144) { x, y -> if (x < 10 || y < 10 || x > 133 || y > 133) 0 else 255 })
        assertFalse(fillsEdges(4, 4) { _, _ -> 255 })
    }

    @Test fun `a widget gets the rim only when it fills its box`() {
        fun roundedRect(radius: Int) = { x: Int, y: Int ->
            val cx = x.coerceIn(radius, 96 - radius); val cy = y.coerceIn(radius, 64 - radius)
            if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= radius * radius) 255 else 0
        }
        val circle = { x: Int, y: Int -> if ((x - 48) * (x - 48) + (y - 48) * (y - 48) <= 47 * 47) 255 else 0 }
        assertTrue(fillsBox(96, 64, roundedRect(12)))
        assertFalse(fillsBox(96, 96, circle))
        assertFalse(fillsBox(96, 64) { _, _ -> 40 })
        assertFalse(fillsBox(4, 4) { _, _ -> 255 })
    }

    @Test fun `the rim is brightest on the side facing the light`() {
        assertEquals(IconLight.RIM_LEAD, iconLighting(null).rimLead.alpha, 1f / 255f)
        assertTrue(iconLighting(null).rimLead.alpha > iconLighting(null).rimTrail.alpha)
    }

    @Test fun `the wallpaper tints the icon only faintly, and not at all without its colors`() {
        assertNull(iconLighting(null).wash)
        val wash = iconLighting(0xFFC08060.toInt()).wash!!
        assertTrue(wash.red > .9f && wash.green > .9f && wash.blue > .9f)
        assertTrue(wash.blue < 1f)
    }
}
