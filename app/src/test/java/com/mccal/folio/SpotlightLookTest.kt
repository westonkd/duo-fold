package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Test

class SpotlightLookTest {
    @Test fun `under Liquid, Spotlight sits on the App Library's wash with Home's ink`() {
        val dark = HomeInk(dark = true)
        val look = spotlightLook(liquid = true, homeInk = dark, blurred = true)
        assertEquals(dark, look.ink)
        assertEquals(libraryWash(inkIsDark = true, blurred = true), look.backdrop)
    }

    @Test fun `white ink gets the dark wash, as in the App Library`() {
        val look = spotlightLook(liquid = true, homeInk = HomeInk(dark = false), blurred = false)
        assertEquals(libraryWash(inkIsDark = false, blurred = false), look.backdrop)
    }

    @Test fun `without Liquid, Spotlight keeps white text on the dark scrim`() {
        val look = spotlightLook(liquid = false, homeInk = HomeInk(dark = true), blurred = true)
        assertEquals(HomeInk(dark = false), look.ink)
        assertEquals(FolioGlass.scrim, look.backdrop)
    }
}
