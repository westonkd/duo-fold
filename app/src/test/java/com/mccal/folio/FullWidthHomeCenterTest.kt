package com.mccal.folio

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Misalignments in Full-Width Home" (Discord, 9 Oct 2026): with the dock at the bottom and no status Side Bar, the page
 * spans the window, so the grid, its columns and the App Library are centered in it, not 8 or 16 dp to one side.
 */
class FullWidthHomeCenterTest {
    // The cover screen with the dock at the bottom and no Side Bar: Full-width Home.
    private fun fullWidth() = homeGeometry(475f, 751f, DockPlacement.BOTTOM, true, statusRail = false)

    @Test fun `the setup really is Full-width Home`() {
        val g = fullWidth()
        assertTrue(g.horizontalDock)
        assertTrue(!g.dockBesideRail)
        assertEquals(475f - 32f, g.gridWidth, .01f)
    }

    @Test fun `a page beside the Side Bar keeps its columns where they were`() {
        val g = homeGeometry(475f, 751f, DockPlacement.SIDE, true)
        assertEquals(0f, g.columnsInset, 0f)
    }

    @Test fun `the App Library has the same edges on both sides when the page spans the window`() {
        for (leftHanded in listOf(false, true)) {
            val edges = libraryEdges(statusInCorner = false, leftHanded = leftHanded, spansWindow = true)
            assertEquals(edges.calculateLeftPadding(LayoutDirection.Ltr), edges.calculateRightPadding(LayoutDirection.Ltr))
            assertEquals(16.dp, edges.calculateLeftPadding(LayoutDirection.Ltr))
        }
        // Beside the Side Bar nothing changes: Home's 16 dp on the screen edge, none against the rail.
        val beside = libraryEdges(statusInCorner = false, leftHanded = false)
        assertEquals(16.dp, beside.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(0.dp, beside.calculateRightPadding(LayoutDirection.Ltr))
    }
}
