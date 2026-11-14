package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditModeFitTest {
    private val editTop = JIGGLE_BAR_BOTTOM.value

    private fun HomeGeometry.pageBottom(rows: Int) = contentTop +
        if (splitColumns) maxOf(widgetHeight + 18f + 2f * rowHeight, 3f * rowHeight) else widgetHeight + 18f + rows * rowHeight

    @Test fun `editing a short landscape phone keeps the whole page above the controls`() {
        val editing = homeGeometry(872f, 365f, DockPlacement.AUTOMATIC, true, statusHeight = 100f, editTop = editTop)
        assertTrue(editing.splitColumns)
        assertTrue(editing.contentTop >= editTop)
        assertTrue(editing.pageBottom(BASE_APP_ROWS) <= 365f - 44f + .01f)
    }

    @Test fun `editing Full-Width Home with six rows keeps the last row clear of the bottom dock`() {
        val bottomSpace = 44f
        val resting = homeGeometry(411f, 891f, DockPlacement.BOTTOM, true, statusHeight = 0f, statusRail = false,
            appRows = 6, fillSpace = true, homeBottomSpace = bottomSpace)
        val editing = homeGeometry(411f, 891f, DockPlacement.BOTTOM, true, statusHeight = 0f, statusRail = false,
            appRows = 6, fillSpace = true, editTop = editTop, homeBottomSpace = bottomSpace)
        val dockTop = 891f - bottomSpace - editing.dockBarHeight - 16f
        assertTrue(editing.horizontalDock)
        assertTrue(resting.contentTop < editTop && resting.pageBottom(6) <= dockTop + .01f)
        assertTrue(editing.contentTop >= editTop)
        assertTrue(editing.pageBottom(6) <= dockTop + .01f)
    }

    @Test fun `a page that already sits below the edit bar does not move`() {
        val resting = homeGeometry(411f, 891f, DockPlacement.AUTOMATIC, true, statusHeight = 160f, fillSpace = true)
        assertTrue(resting.contentTop >= editTop)
        assertEquals(resting, homeGeometry(411f, 891f, DockPlacement.AUTOMATIC, true, statusHeight = 160f, fillSpace = true, editTop = editTop))
    }
}
