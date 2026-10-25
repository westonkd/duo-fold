package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins what the Stability and Motion Audit found on 8 Oct 2026 (docs/plan-beta-2-audit-duet-root.md), so a change is deliberate.
 * #256 has since been fixed and its tests pin the fix.
 */
class LayoutIsolationAuditTest {
    /**
     * Issue #256 and #13, fixed 9 Oct 2026: Home used to keep a fixed 124 dp under the dock whenever the rail showed round controls,
     * sized for two at the default icon size, so one Search button squeezed the dock's spacing in a short window. It now keeps the
     * controls' real stack (see [railControlsReserve]). Prints a table of the dock for the windows the reports mention.
     */
    @Test fun `the controls at the bottom of the rail take only the room they use`() {
        val windows = listOf("Fold8 inner" to (932f to 704f), "Fold8 inner upright" to (704f to 932f), "Fold8 cover" to (475f to 751f), "Fold8 cover landscape" to (751f to 475f))
        val rows = mutableListOf<String>()
        for ((name, size) in windows) for (status in listOf(0f, 160f, 300f)) {
            val with = homeGeometry(size.first, size.second, LayoutPreset(), true, statusHeight = status, railControls = 1)
            val without = homeGeometry(size.first, size.second, LayoutPreset(), true, statusHeight = status, railControls = 0)
            rows += "%-22s status %3.0f  dock with search: top %5.1f height %5.1f row %4.1f   without: top %5.1f height %5.1f row %4.1f".format(
                name, status, with.dockTop, with.dockHeight, with.dockRowHeight, without.dockTop, without.dockHeight, without.dockRowHeight)
            assertTrue("$name: the controls never make the dock taller", with.dockHeight <= without.dockHeight + .01f)
            assertTrue("$name: a dock keeps room for four 48 dp targets", with.dockRowHeight >= 48f)
        }
        println("DOCK RESERVE TABLE\n" + rows.joinToString("\n"))
    }

    @Test fun `the reserve is the controls' stack, not a fixed 124 dp`() {
        val control = dockIconSize(66f)
        assertEquals(28f, railControlsReserve(0, 66f), 0f)
        assertEquals(FolioSpace.SNUG + control + 10f, railControlsReserve(1, 66f), .01f)
        assertEquals(FolioSpace.SNUG + 2 * control + FolioSpace.SMALL + 10f, railControlsReserve(2, 66f), .01f)
        assertTrue("one Search button needs far less than the old 124 dp", railControlsReserve(1, 66f) < 70f)
        assertTrue("two large controls need more than 124 dp, which the old reserve let them crowd", railControlsReserve(2, 90f) > 124f)
    }

    @Test fun `Discover's extra control moves the dock only when it has to`() {
        val home = homeGeometry(704f, 932f, LayoutPreset(), true, railControls = 1, homeRailControls = 1)
        val discover = homeGeometry(704f, 932f, LayoutPreset(), true, railControls = 2, homeRailControls = 1)
        assertEquals("a tall window has room for both, so the dock stays put", home.dockTop, discover.dockTop, .01f)
    }
}
