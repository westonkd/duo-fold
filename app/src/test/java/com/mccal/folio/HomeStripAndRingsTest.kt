package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStripAndRingsTest {
    @Test fun `an older save with the Search button on or off becomes the matching choice`() {
        assertEquals(HomeStrip.SEARCH, HomeStrip.read(null, legacySearchPill = true))
        assertEquals(HomeStrip.DOTS, HomeStrip.read(null, legacySearchPill = false))
    }

    @Test fun `a saved choice wins over the old switch, and an unknown one falls back to it`() {
        assertEquals(HomeStrip.NOTHING, HomeStrip.read("nothing", legacySearchPill = false))
        assertEquals(HomeStrip.DOTS, HomeStrip.read("dots", legacySearchPill = true))
        assertEquals(HomeStrip.SEARCH, HomeStrip.read("something from the future", legacySearchPill = true))
    }

    @Test fun `only the Search button counts as the old switch being on`() {
        assertTrue(LauncherState(homeStrip = HomeStrip.SEARCH).searchPill)
        assertFalse(LauncherState(homeStrip = HomeStrip.DOTS).searchPill)
        assertFalse(LauncherState(homeStrip = HomeStrip.NOTHING).searchPill)
    }

    @Test fun `the strip shows the dots away from rest, whatever was chosen`() {
        HomeStrip.entries.forEach { assertEquals(StripView.DOTS, stripView(it, idle = false)) }
    }

    @Test fun `at rest the strip shows what was chosen`() {
        assertEquals(StripView.PILL, stripView(HomeStrip.SEARCH, idle = true))
        assertEquals(StripView.DOTS, stripView(HomeStrip.DOTS, idle = true))
        assertEquals(StripView.EMPTY, stripView(HomeStrip.NOTHING, idle = true))
    }

    @Test fun `the strong ring is thicker with a clearer track, and Bold text turns it on`() {
        assertTrue(RingLook.STRONG.strokeScale > RingLook.NORMAL.strokeScale)
        assertTrue(RingLook.STRONG.trackAlpha > RingLook.NORMAL.trackAlpha)
        assertEquals(RingLook.NORMAL, RingLook.of(fontWeightAdjustment = 0))
        assertEquals(RingLook.STRONG, RingLook.of(fontWeightAdjustment = 300))
        assertEquals("an undefined adjustment is not Bold text", RingLook.NORMAL, RingLook.of(fontWeightAdjustment = Int.MAX_VALUE))
        assertEquals("nor is a negative one", RingLook.NORMAL, RingLook.of(fontWeightAdjustment = -100))
    }

    @Test fun `the ring still fits inside its box when strong`() {
        // Radius .44 of the width plus half the stroke must stay inside .5.
        assertTrue(.44f + .06f * RingLook.STRONG.strokeScale / 2f < .5f)
    }
}
