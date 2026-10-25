package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How many columns Settings shows, on the windows Folio actually meets. The rule is about the window, never the
 * device: 700 dp for two columns.
 */
class SettingsColumnsTest {
    private fun columns(w: Float, h: Float) = settingsColumns(w, h)

    @Test fun `a phone and a cover screen show one page at a time`() {
        assertEquals(1, columns(411f, 891f))          // Pixel 9
        assertEquals(1, columns(475f, 751f))          // Fold8 cover
        assertEquals(1, columns(751f, 475f))          // Fold8 cover, rotated: too short to be regular
    }

    @Test fun `a window too narrow for two readable columns keeps the list over the page`() {
        assertEquals(1, columns(610f, 925f))          // 7.4" rollable
        assertEquals(1, columns(600f, 960f))
    }

    @Test fun `the list sits beside the page in either orientation, which is what iPad Settings does`() {
        assertEquals(2, columns(932f, 704f))          // Fold8 inner
        assertEquals(2, columns(852f, 883f))          // Pixel 9 Pro Fold inner, held upright
        assertEquals(2, columns(800f, 1280f))         // tablet, portrait
        assertEquals(2, columns(1280f, 800f))         // tablet, landscape
        assertEquals(2, columns(1920f, 1080f))        // desktop
    }
}
