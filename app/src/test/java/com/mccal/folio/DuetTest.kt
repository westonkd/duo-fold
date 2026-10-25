package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Duet, the fold animation, drawn with iPhone Duo's numbers. */
class DuetTest {
    @Test fun `the open screen starts as frosted as iPhone Duo's would be when the panel lights`() {
        assertEquals("starts where the Fold8 lights its inner panel, ~125 degrees", .61f, IPHONE_DUO_UNFOLD_START, 1e-6f)
    }

    @Test fun `Duet is on by default, as the fold animation always was`() {
        assertTrue(decodeLauncherState("{}", legacyRaw = null).foldEffect)
        assertTrue(!decodeLauncherState("""{"foldEffect":false}""", legacyRaw = null).foldEffect)
    }

    @Test fun `a save with the old Duet look, sliders and Use On still loads`() {
        val saved = """{"foldEffect":true,"foldIntensity":.5,"foldSnapshot":true,"duet":{"style":"deep","frost":.25,"direction":"closing"},
            "featureScopes":{"duet":{"COVER":"OFF"}}}"""
        assertTrue(decodeLauncherState(saved, legacyRaw = null).foldEffect)
    }
}
