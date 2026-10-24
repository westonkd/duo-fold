package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PressFeedbackTest {
    @Test fun `an icon is full size and fully opaque at rest`() {
        assertEquals(1f, PressFeedback.scale(pressed = false, lifted = false, reduceMotion = false), 0f)
        assertEquals(1f, PressFeedback.alpha(pressed = false, lifted = false), 0f)
        assertEquals(0f, PressFeedback.shadowDp(lifted = false, reduceMotion = false), 0f)
    }

    @Test fun `a finger dips and dims it a little`() {
        assertEquals(PressFeedback.DIP, PressFeedback.scale(true, false, false), 0f)
        assertEquals(PressFeedback.DIP_ALPHA, PressFeedback.alpha(true, false), 0f)
        assertTrue(PressFeedback.DIP in .85f..0.95f && PressFeedback.DIP_ALPHA in .85f..1f)
    }

    @Test fun `an open menu lifts the icon over everything else, with a shadow and no dim`() {
        assertEquals(PressFeedback.LIFT, PressFeedback.scale(pressed = true, lifted = true, reduceMotion = false), 0f)
        assertEquals(1f, PressFeedback.alpha(pressed = true, lifted = true), 0f)
        assertTrue(PressFeedback.shadowDp(true, false) > 0f)
        assertTrue(PressFeedback.LIFT > 1f && PressFeedback.LIFT <= 1.1f)
    }

    @Test fun `Reduce Motion keeps the dim and drops the scale and the shadow`() {
        assertEquals(1f, PressFeedback.scale(true, false, true), 0f)
        assertEquals(1f, PressFeedback.scale(true, true, true), 0f)
        assertEquals(PressFeedback.DIP_ALPHA, PressFeedback.alpha(true, false), 0f)
        assertEquals(0f, PressFeedback.shadowDp(true, true), 0f)
    }

    @Test fun `the hold after a tap is short, so a failed launch never leaves an icon dipped`() {
        assertTrue(PressFeedback.HOLD_MS in 300L..1_000L)
    }
}
