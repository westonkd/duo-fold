package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How many rows Home lands on.
 *
 * The sizes are the window Home actually gets, logged on a Galaxy Z Fold8: the cover screen is 475 x 751 dp, but
 * Android's bars and gesture area take about 56 dp of it, so Home is laid out in 695. Measuring against the physical
 * screen instead says five rows fit when four do, which is how this test came to exist.
 */
class AutomaticRowsTest {
    private val cover = 475f to 695f   // 751 dp of screen, less ~56 dp of system insets
    private val inner = 932f to 648f

    /** The same call Home makes: the search pill's space, and Folio as the Home app. */
    private fun fit(size: Pair<Float, Float>, labelHeight: Float): Int =
        homeGeometry(size.first, size.second, LayoutPreset(), labels = true, statusHeight = 0f, labelHeight = labelHeight,
            inLibrary = false, homeBottomSpace = 44f, railControls = 0, classScale = 1f,
            appRows = BASE_APP_ROWS, foldAtCenter = false, fillSpace = true).fitAppRows

    // A widget fills two rows, so the cover fits a fifth row with the space under labels tightened; the inner screen
    // fits four, and Home takes the smaller.
    @Test fun `the Fold8 fits four rows at the default icon size`() {
        assertEquals(5, fit(cover, 20f))
        assertEquals(4, fit(inner, 20f))
        assertEquals(4, effectiveHomeRows(fitCompact = fit(cover, 20f), fitExpanded = fit(inner, 20f)))
    }

    @Test fun `a screen Folio hasn't measured doesn't drag the count down`() {
        assertEquals(5, effectiveHomeRows(fitCompact = 5, fitExpanded = 0))
        assertEquals(BASE_APP_ROWS, effectiveHomeRows(fitCompact = 0, fitExpanded = 0))
    }

    @Test fun `the upright inner screen counts as part of the inner screen`() {
        assertEquals(4, LauncherState(homeFitCompact = 6, homeFitExpanded = 6, homeFitUpright = 4).homeAppRows)
        assertEquals(6, LauncherState(homeFitCompact = 6, homeFitExpanded = 6).homeAppRows)
    }
}
