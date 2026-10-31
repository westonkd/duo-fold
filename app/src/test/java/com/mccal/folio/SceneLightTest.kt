package com.mccal.folio

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneLightTest {
    private val window = Size(1000f, 2000f)
    private val box = Size(400f, 400f)

    private fun towardFrom(topLeft: Offset, direction: LayoutDirection = LayoutDirection.Ltr): Offset =
        SceneLight.toward(box, SceneLight.source(window, direction) - topLeft)

    @Test fun `the light hangs above the window, nearer the leading edge`() {
        val ltr = SceneLight.source(window, LayoutDirection.Ltr)
        val rtl = SceneLight.source(window, LayoutDirection.Rtl)
        assertTrue(ltr.y < 0f)
        assertTrue(ltr.x in 0f..window.width / 2f)
        assertEquals(window.width - ltr.x, rtl.x, .001f)
    }

    @Test fun `the App Library's light sits inside the window's upper leading corner`() {
        val ltr = SceneLight.Library.at(window, LayoutDirection.Ltr)
        val rtl = SceneLight.Library.at(window, LayoutDirection.Rtl)
        assertTrue(ltr.y in 0f..window.height / 2f)
        assertTrue(ltr.x in 0f..window.width / 2f)
        assertEquals(window.width - ltr.x, rtl.x, .001f)
    }

    @Test fun `two surfaces side by side catch the light at different places`() {
        val left = SceneLight.litEdge(box, towardFrom(Offset(50f, 100f)))
        val right = SceneLight.litEdge(box, towardFrom(Offset(550f, 100f)))
        assertTrue(left.x > right.x + 40f)
    }

    @Test fun `the lit spot slides along the edge as a surface moves past the light`() {
        val turns = (0..6).map {
            val spot = SceneLight.litEdge(box, towardFrom(Offset(-600f + it * 200f, 100f)))
            kotlin.math.atan2(spot.y - box.height / 2f, spot.x - box.width / 2f)
        }
        assertTrue(turns.zipWithNext().all { (a, b) -> b < a })
    }

    @Test fun `the lit spot is on the edge facing the light and the dark side is opposite`() {
        val frame = LitFrame(box, towardFrom(Offset(550f, 1400f)))
        assertEquals(0f, frame.lit.y, .001f)
        assertEquals(box.height, frame.dark.y, .001f)
        assertEquals(box.width, frame.lit.x + frame.dark.x, .001f)
    }

    @Test fun `a surface beside the light is lit on its side`() {
        assertEquals(0f, SceneLight.litEdge(box, towardFrom(Offset(700f, 100f))).x, .001f)
        val far = SceneLight.litEdge(Size(100f, 100f), SceneLight.toward(Size(100f, 100f), Offset(-2000f, 40f)))
        assertEquals(0f, far.x, .001f)
    }

    @Test fun `a long capsule is lit along its whole top edge and dark along its bottom`() {
        val capsule = Size(800f, 100f)
        val frame = LitFrame(capsule, SceneLight.toward(capsule, Offset(150f, -60f)))
        val radians = Math.toRadians(frame.angle.toDouble())
        fun shadeAt(p: Offset): Double = ((p.x - frame.lit.x) / frame.stretch.x * kotlin.math.cos(radians) +
            (p.y - frame.lit.y) / frame.stretch.y * kotlin.math.sin(radians)) / frame.span
        val top = (0..8).map { shadeAt(Offset(it * 100f, 0f)) }
        val bottom = (0..8).map { shadeAt(Offset(it * 100f, capsule.height)) }
        assertEquals(0f, frame.lit.y, .001f)
        assertTrue(bottom.minOrNull()!! > top.maxOrNull()!!)
    }

    @Test fun `round highlights stretch along a long edge, but less than the edge does`() {
        val frame = LitFrame(Size(800f, 100f), SceneLight.UNPLACED)
        assertTrue(frame.oval.x > 1f && frame.oval.x < frame.stretch.x)
        assertEquals(1f, frame.oval.y, .001f)
        assertEquals(1f, LitFrame(Size(300f, 300f), SceneLight.UNPLACED).oval.x, .001f)
    }
}
