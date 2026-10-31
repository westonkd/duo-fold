package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusRingTest {
    @Test fun `without signal dots the ring is whole and fills from the top`() {
        assertEquals(RingArc(-90f, 360f), statusRingArc(0))
        assertTrue(statusRingDotAngles(0).isEmpty())
    }

    @Test fun `the dots sit in the ring's bottom opening, centred and evenly spaced like the arc ends`() {
        val arc = statusRingArc(STATUS_RING_DOTS)
        val angles = statusRingDotAngles(STATUS_RING_DOTS)
        val gap = 360f - arc.sweep
        assertEquals(90f, angles.average().toFloat(), .001f)
        assertEquals(arc.start - STATUS_RING_DOT_PITCH, angles.first(), .001f)
        assertEquals(arc.start + arc.sweep - 360f + STATUS_RING_DOT_PITCH, angles.last(), .001f)
        assertEquals(STATUS_RING_DOT_PITCH * (STATUS_RING_DOTS + 1), gap, .001f)
    }

    @Test fun `the first dot is on the left, so signal lights left to right`() {
        val angles = statusRingDotAngles(STATUS_RING_DOTS)
        assertTrue(kotlin.math.cos(Math.toRadians(angles.first().toDouble())) < 0)
        assertTrue(kotlin.math.cos(Math.toRadians(angles.last().toDouble())) > 0)
    }

    @Test fun `the ring refills from empty only when a charger connects`() {
        assertEquals(0f, ringFillStart(wasCharging = false, charging = true, reduceMotion = false))
        assertEquals(null, ringFillStart(wasCharging = true, charging = true, reduceMotion = false))
        assertEquals(null, ringFillStart(wasCharging = true, charging = false, reduceMotion = false))
        assertEquals(null, ringFillStart(wasCharging = false, charging = true, reduceMotion = true))
    }

    @Test fun `the bell rings only when Silent turns on, and never with Reduce Motion`() {
        assertTrue(bellShouldRing(wasSilent = false, silent = true, reduceMotion = false))
        assertTrue(!bellShouldRing(wasSilent = true, silent = true, reduceMotion = false))
        assertTrue(!bellShouldRing(wasSilent = true, silent = false, reduceMotion = false))
        assertTrue(!bellShouldRing(wasSilent = false, silent = true, reduceMotion = true))
    }
}
