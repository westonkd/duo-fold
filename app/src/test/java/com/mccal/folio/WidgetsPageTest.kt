package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetsPageTest {
    @Test fun `opening the phone lands on the widgets page beside Home 1`() {
        assertEquals(0, pagerHomeAfterSwitch(logicalPage = 3, firstHome = 0, unfolding = true))
    }

    @Test fun `folding keeps the Home page and the widgets page is a page of its own on the cover`() {
        assertEquals(2, pagerHomeAfterSwitch(logicalPage = 2, firstHome = 1, unfolding = false))
        assertEquals(-1, pagerHomeAfterSwitch(logicalPage = -1, firstHome = 1, unfolding = false))
        assertEquals(0, pagerHomeAfterSwitch(logicalPage = -1, firstHome = 0, unfolding = false))
    }

    @Test fun `the dots and dock step aside only as the widgets page comes in`() {
        assertEquals(0f, widgetsPageShown(0f), 0f)
        assertEquals(.5f, widgetsPageShown(-.5f), 0f)
        assertEquals(1f, widgetsPageShown(-1f), 0f)
        assertEquals(0f, widgetsPageShown(1.5f), 0f)
    }

    @Test fun `Home pressed from an app comes back to the widgets page it was left on`() {
        assertEquals(-1, homePressPage(alreadyInFront = false, somethingOpen = false, currentPage = -1, homePages = 3, lastHomePage = 1))
        assertEquals(0, homePressPage(alreadyInFront = true, somethingOpen = false, currentPage = -1, homePages = 3, lastHomePage = 1))
    }
}
