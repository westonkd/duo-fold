package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackdropBlurTest {
    @Test fun `the blur behind an overlay grows in steps with the overlay and is off when it is closed`() {
        assertEquals(0, backdropBlurStep(0f))
        assertEquals(BACKDROP_BLUR_STEPS, backdropBlurStep(1f))
        assertEquals(BACKDROP_BLUR_STEPS, backdropBlurStep(1.04f))
        val steps = (0..20).map { backdropBlurStep(it / 20f) }
        assertTrue(steps.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(steps.toSet().size > 4)
    }

    @Test fun `the sticky letter chip is dark under white text and light under dark text`() {
        fun luminance(c: androidx.compose.ui.graphics.Color) = .2126f * c.red + .7152f * c.green + .0722f * c.blue
        val wallpaper = 0xFF8A6A5C.toInt()
        assertTrue(luminance(libraryLetterChip(wallpaper, inkIsDark = false)) < .3f)
        assertTrue(luminance(libraryLetterChip(wallpaper, inkIsDark = true)) > .6f)
        assertEquals(1f, libraryLetterChip(null, inkIsDark = false).alpha, .001f)
    }
}
