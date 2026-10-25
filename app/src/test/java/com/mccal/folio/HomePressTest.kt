package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Test

class HomePressTest {
    @Test fun `already in front on page 3 goes to the first page`() {
        assertEquals(0, homePressPage(alreadyInFront = true, somethingOpen = false, currentPage = 3, homePages = 5, lastHomePage = 3))
    }

    @Test fun `already in front on the App Library goes to the first page`() {
        assertEquals(0, homePressPage(true, false, currentPage = 5, homePages = 5, lastHomePage = 2))
    }

    @Test fun `coming back from an app stays on page 3`() {
        assertEquals(3, homePressPage(alreadyInFront = false, somethingOpen = false, currentPage = 3, homePages = 5, lastHomePage = 1))
    }

    @Test fun `from an app while on the App Library returns to the last Home page`() {
        assertEquals(2, homePressPage(false, false, currentPage = 5, homePages = 5, lastHomePage = 2))
    }

    @Test fun `with a sheet or edit mode open the press only closes it`() {
        assertEquals(3, homePressPage(alreadyInFront = true, somethingOpen = true, currentPage = 3, homePages = 5, lastHomePage = 3))
    }

    @Test fun `no Home pages is page 0`() {
        assertEquals(0, homePressPage(true, false, currentPage = 0, homePages = 0, lastHomePage = 0))
    }
}
