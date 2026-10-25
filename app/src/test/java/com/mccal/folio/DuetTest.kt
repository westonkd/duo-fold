package com.mccal.folio

import com.mccal.folio.duet.DuetStyles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Duet, the fold animation as a tweak, drawn with iPhone Duo's numbers. */
class DuetTest {
    @Test fun `the look is iPhone Duo, with Apple's numbers`() {
        with(DuetStyles.IPHONE) {
            // chuspeeism/iphone-duo: 72 px at the edge (Folio's 4.5% of the width), 2x shade, straight-on projection, full
            // tilt, and a hard edge at the hinge because the camera half never moves.
            assertEquals(listOf(1f, 1f, 1f, 0f, 0f), listOf(frost, darkening, perspective, blurFloor, softEdge))
            assertTrue(classic)
            assertEquals("starts where the Fold8 lights its inner panel, ~125 degrees", .61f, startAt, 1e-6f)
        }
    }

    @Test fun `Reduce Motion keeps only the shade`() {
        with(DuetStyles.reducedMotion(DuetStyles.IPHONE)) {
            assertEquals(listOf(0f, 1f, 0f, 0f, 0f), listOf(frost, darkening, perspective, blurFloor, softEdge))
            assertTrue(!classic)
        }
    }

    @Test fun `Duet is a tweak, on by default as the fold animation always was`() {
        val duet = TweakFeatures.single { it.id == DUET_ID }
        assertTrue(duet.default)
        assertTrue(decodeLauncherState("{}", legacyRaw = null).foldEffect)
        assertTrue(!decodeLauncherState("""{"foldEffect":false}""", legacyRaw = null).foldEffect)
    }

    @Test fun `Use On turns Duet off for one screen, and Back from Fold & Displays returns there`() {
        val scopes = FeatureScopes.set(emptyMap(), DUET_ID, FolioScreen.COVER, ScopeValue.OFF)
        assertTrue(!FeatureScopes.on(scopes, DUET_ID, true, FolioScreen.COVER))
        assertTrue(FeatureScopes.on(scopes, DUET_ID, true, FolioScreen.INNER))
        assertEquals(CustomizationPage.FOLD, CustomizationPage.FOLD_TWEAK.parent)
        val main = java.io.File("src/main/java/com/mccal/folio/MainActivity.kt").readText()
        assertTrue("the fold animation should ask Use On", "FeatureScopes.on(state.featureScopes, DUET_ID" in main)
    }

    @Test fun `a save with the old Duet look and sliders still loads`() {
        val saved = """{"foldEffect":true,"foldIntensity":.5,"foldSnapshot":true,"duet":{"style":"deep","frost":.25,"direction":"closing"}}"""
        assertTrue(decodeLauncherState(saved, legacyRaw = null).foldEffect)
    }
}
