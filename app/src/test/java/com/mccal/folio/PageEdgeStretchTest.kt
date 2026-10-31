package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageEdgeStretchTest {
    @Test fun `only the first and last pages stretch`() {
        assertEquals(-.25f, edgeOvershoot(-.25f, 4), .0001f)
        assertEquals(.5f, edgeOvershoot(3.5f, 4), .0001f)
        assertEquals(0f, edgeOvershoot(1.5f, 4), .0001f)
        assertEquals(0f, edgeOvershoot(0f, 4), .0001f)
        assertEquals(0f, edgeOvershoot(3f, 4), .0001f)
        assertEquals(0f, edgeOvershoot(-1f, 0), .0001f)
    }

    @Test fun `the stretch follows the finger at first and then resists`() {
        val width = 1000f
        val small = rubberBand(10f, width)
        assertEquals(10f * RUBBER_BAND, small, .2f)
        val pulls = listOf(50f, 200f, 600f, 2000f, 10000f).map { rubberBand(it, width) }
        pulls.zipWithNext().forEach { (a, b) -> assertTrue("$a then $b", b > a) }
        pulls.forEach { assertTrue("$it stays under the page width", it < width) }
        assertTrue("half a page of finger moves far less than half a page", rubberBand(500f, width) < 250f)
    }

    @Test fun `the stretch keeps the finger's direction`() {
        assertEquals(-rubberBand(300f, 1000f), rubberBand(-300f, 1000f), .0001f)
        assertEquals(0f, rubberBand(0f, 1000f), 0f)
        assertEquals(0f, rubberBand(100f, 0f), 0f)
    }

    @Test fun `pulling a whole page past the end moves Home only a little`() {
        val stretch = PageEdgeStretch()
        val stride = 1248f
        stretch.pull(requested = -1f, count = 4, stride = stride)
        assertTrue("${stretch.offset}", stretch.offset > 0f && stretch.offset < stride * EDGE_REACH && stretch.offset < stride / 4)
        stretch.pull(requested = 4f, count = 4, stride = stride)
        assertTrue("${stretch.offset}", stretch.offset < 0f)
        stretch.pull(requested = 2f, count = 4, stride = stride)
        assertEquals(0f, stretch.offset, 0f)
    }
}
