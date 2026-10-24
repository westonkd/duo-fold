package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt

/** The 0.6.9 motion tokens: what they are and how far they overshoot. */
class FolioMotionTest {
    private fun overshootPercent(damping: Float): Double =
        if (damping >= 1f) 0.0 else 100 * exp(-PI * damping / sqrt(1.0 - damping * damping))

    @Test fun `the proposed springs are the ones that were shown and agreed`() {
        assertEquals(.9f to 380f, FolioMotion.Sheet)
        assertEquals(.78f to 650f, FolioMotion.Menu)
        assertEquals(.7f to 480f, FolioMotion.Bounce)
    }

    @Test fun `no spring is livelier than the overshoot rule allows`() {
        // Damping 0.7 is about 4.6% overshoot, the most a settle that starts with the finger's speed should have.
        for ((name, spring) in listOf("Settle" to FolioMotion.Settle, "Quick" to FolioMotion.Quick, "Firm" to FolioMotion.Firm,
            "Sheet" to FolioMotion.Sheet, "Menu" to FolioMotion.Menu, "Bounce" to FolioMotion.Bounce)) {
            assertTrue("$name damps to ${spring.first}", spring.first >= .7f)
            assertTrue("$name overshoots ${overshootPercent(spring.first)}%", overshootPercent(spring.first) <= 4.7)
        }
        assertTrue("a sheet does not visibly overshoot", overshootPercent(FolioMotion.Sheet.first) < .5)
        assertEquals("a dismissal does not overshoot", 0.0, overshootPercent(FolioMotion.Firm.first), 0.0)
    }
}
