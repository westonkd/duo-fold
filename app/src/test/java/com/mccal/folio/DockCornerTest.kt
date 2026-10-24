package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockCornerTest {
    @Test fun `the dock's corner is the icon's corner plus the gap around it`() {
        val icon = 48f
        val gap = 10f
        assertEquals(icon * ICON_CORNER_FRACTION + gap, dockCornerRadius(icon + 2 * gap, icon, IconShape.SQUIRCLE), .001f)
        assertTrue(dockCornerRadius(icon + 2 * gap, icon, IconShape.DEFAULT) < (icon + 2 * gap) / 2f)
    }

    @Test fun `round icons keep the capsule`() {
        assertEquals(34f, dockCornerRadius(68f, 48f, IconShape.CIRCLE), 0f)
    }

    @Test fun `the corner never passes half the dock's thickness`() {
        assertEquals(20f, dockCornerRadius(40f, 100f, IconShape.ROUNDED), .001f)
    }
}
