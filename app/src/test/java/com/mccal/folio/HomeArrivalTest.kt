package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeArrivalTest {
    @Test fun `the dock arrives first, then Home from the Side Bar's corner outwards`() {
        val topNearRail = homeArrivalOrder(DOCK_ARRIVAL_LEAD, row = 0, column = GRID_COLUMNS - 1)
        val topFar = homeArrivalOrder(DOCK_ARRIVAL_LEAD, row = 0, column = 0)
        val bottomNearRail = homeArrivalOrder(DOCK_ARRIVAL_LEAD, row = 3, column = GRID_COLUMNS - 1)
        assertEquals(DOCK_ARRIVAL_LEAD, topNearRail)
        assertTrue(topNearRail < topFar)
        assertTrue(topNearRail < bottomNearRail)
        assertTrue((0 until DOCK_ARRIVAL_LEAD).all { it < topNearRail })
    }

    @Test fun `the wave is short however full Home is`() {
        assertEquals(0L, arrivalDelayMs(0))
        assertEquals(ArrivalMotion.STAGGER_MS * 3, arrivalDelayMs(3))
        assertEquals(ArrivalMotion.LONGEST_DELAY_MS, arrivalDelayMs(400))
        assertEquals(0L, arrivalDelayMs(-2))
    }

    @Test fun `icons wait for the start, and ones that show up long after it are simply there`() {
        var now = 1_000L
        val arrival = HomeArrival { now }
        assertTrue(arrival.pending())
        now += 60_000L
        assertTrue("nothing has started yet", arrival.pending())
        arrival.begin()
        assertTrue(arrival.pending())
        now += ArrivalMotion.WINDOW_MS - 1
        assertTrue(arrival.pending())
        now += 1
        assertTrue(!arrival.pending())
        arrival.begin()
        assertTrue("a second begin does not restart it", !arrival.pending())
    }
}
