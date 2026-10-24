package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Test

class SpotlightLookTest {
    @Test fun `Spotlight sits on the App Library's wash with Home's ink`() {
        val dark = HomeInk(dark = true)
        val look = spotlightLook(homeInk = dark, blurred = true)
        assertEquals(dark, look.ink)
        assertEquals(libraryWash(inkIsDark = true, blurred = true), look.backdrop)
    }

    @Test fun `white ink gets the dark wash, as in the App Library`() {
        val look = spotlightLook(homeInk = HomeInk(dark = false), blurred = false)
        assertEquals(libraryWash(inkIsDark = false, blurred = false), look.backdrop)
    }
}
