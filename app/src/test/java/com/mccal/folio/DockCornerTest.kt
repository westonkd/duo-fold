package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockCornerTest {
    @Test fun `the dock's corner is the icon's corner plus the gap around it`() {
        val icon = 48f
        val gap = 10f
        assertEquals(icon * ICON_CORNER_FRACTION + gap, dockCornerRadius(icon + 2 * gap, icon), .001f)
        assertTrue(dockCornerRadius(icon + 2 * gap, icon) < (icon + 2 * gap) / 2f)
    }

    @Test fun `the corner never passes half the dock's thickness`() {
        assertEquals(20f, dockCornerRadius(40f, 100f), .001f)
    }
}
