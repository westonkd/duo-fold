package com.mccal.folio

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusRailInkTest {
    private val whiteText = HomeInk(dark = false, automatic = true)
    private val darkText = HomeInk(dark = true, automatic = true)

    @Test fun `without the pill the status uses Home's own text color`() {
        assertEquals(whiteText, statusRailInk(whiteText, pill = false, frost = .9f, glassLuminance = 1f, wallpaperLuminance = .9f))
        assertEquals(darkText, statusRailInk(darkText, pill = false, frost = .9f, glassLuminance = 0f, wallpaperLuminance = .1f))
    }

    @Test fun `on the pill the text follows how light the frosted pill looks`() {
        assertTrue(statusRailInk(whiteText, pill = true, frost = .5f, glassLuminance = 1f, wallpaperLuminance = .3f).dark)
        assertFalse(statusRailInk(whiteText, pill = true, frost = .05f, glassLuminance = 1f, wallpaperLuminance = .3f).dark)
    }

    @Test fun `a Light or Dark choice for Home text always wins`() {
        val chosen = HomeInk(dark = false, automatic = false)
        assertEquals(chosen, statusRailInk(chosen, pill = true, frost = .8f, glassLuminance = 1f, wallpaperLuminance = .9f))
    }

    @Test fun `the pill is off to start with, also for saves made while it was on by default`() {
        assertFalse(StatusStyle().background)
        assertFalse(StatusStyle.fromJson(JSONObject().put("background", true)).background)
        assertTrue(StatusStyle.fromJson(StatusStyle(background = true).toJson()).background)
    }
}
