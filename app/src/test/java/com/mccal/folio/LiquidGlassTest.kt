package com.mccal.folio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassTest {
    private val glass = LightDuoPalette.glass
    private val outline = GlassLook().outline

    @Test fun `Liquid draws only when chosen, its gate is open and Reduce Transparency is off`() {
        assertTrue(usesLiquidGlass(GlassMaterial.LIQUID, gateOpen = true, solid = false))
        assertFalse(usesLiquidGlass(GlassMaterial.CLASSIC, gateOpen = true, solid = false))
        assertFalse(usesLiquidGlass(GlassMaterial.LIQUID, gateOpen = false, solid = false))
        assertFalse(usesLiquidGlass(GlassMaterial.LIQUID, gateOpen = true, solid = true))
    }

    @Test fun `the body keeps the surface's frost and thins toward the far corner`() {
        val spec = liquidGlassSpec(glass, .3f, GlassRole.PANEL, outline)
        assertEquals(.3f, spec.bodyStart.alpha, .01f)
        assertTrue(spec.bodyEnd.alpha < spec.bodyStart.alpha)
    }

    @Test fun `the body leans toward the wallpaper's colors`() {
        val red = Color.Red.toArgb()
        val blue = Color.Blue.toArgb()
        val spec = liquidGlassSpec(glass, .3f, GlassRole.PANEL, outline, primary = red, secondary = blue)
        assertTrue(spec.bodyStart.red > spec.bodyStart.blue)
        assertTrue(spec.bodyEnd.blue > spec.bodyEnd.red)
        assertEquals(liquidGlassSpec(glass, .3f, GlassRole.PANEL, outline).bodyStart, liquidGlassSpec(glass, .3f, GlassRole.PANEL, outline, null, null).bodyStart)
    }

    @Test fun `raised glass is lit from the top left and a field is lit from below`() {
        val panel = liquidGlassSpec(glass, .3f, GlassRole.PANEL, outline)
        assertTrue(panel.rimLead.alpha > panel.rimTrail.alpha)
        assertTrue(panel.rimSide.alpha < panel.rimTrail.alpha)
        val field = liquidGlassSpec(glass, .12f, GlassRole.FIELD, outline)
        assertTrue(field.rimTrail.alpha > field.rimLead.alpha)
        assertEquals(0f, field.light, 0f)
    }

    @Test fun `the Outline slider scales the rim and at zero removes it`() {
        val none = liquidGlassSpec(glass, .3f, GlassRole.CAPSULE, 0f)
        assertEquals(0f, none.rimLead.alpha, 0f)
        assertEquals(0f, none.rimSide.alpha, 0f)
        assertEquals(0f, none.rimTrail.alpha, 0f)
        val strong = liquidGlassSpec(glass, .3f, GlassRole.CAPSULE, .5f)
        assertTrue(strong.rimLead.alpha > liquidGlassSpec(glass, .3f, GlassRole.CAPSULE, outline).rimLead.alpha)
        assertTrue(strong.rimLead.alpha <= 1f)
    }

    @Test fun `only floating glass casts a shadow`() {
        GlassRole.entries.forEach { role ->
            val lift = liquidGlassSpec(glass, .3f, role, outline).lift
            if (role == GlassRole.FLOATING) assertTrue(lift > 0f) else assertEquals(0f, lift, 0f)
        }
    }

    @Test fun `a save from before Liquid is Classic, and a saved choice is read back`() {
        assertEquals(GlassMaterial.CLASSIC, decodeLauncherState("{}", legacyRaw = null).glassMaterial)
        assertEquals(GlassMaterial.LIQUID, decodeLauncherState("""{"glassMaterial":"LIQUID"}""", legacyRaw = null).glassMaterial)
        assertEquals(GlassMaterial.CLASSIC, decodeLauncherState("""{"glassMaterial":"MOLTEN"}""", legacyRaw = null).glassMaterial)
    }

    @Test fun `Reduce Transparency keeps the chosen material so turning it off brings Liquid back`() {
        assertEquals(GlassMaterial.LIQUID, LauncherState(glassMaterial = GlassMaterial.LIQUID).withSolidGlass().glassMaterial)
    }

    @Test fun `the library backdrop follows the swipe and is gone a page away`() {
        assertEquals(1f, libraryShown(3f, 3), 0f)
        assertEquals(.25f, libraryShown(2.25f, 3), .001f)
        assertEquals(0f, libraryShown(2f, 3), 0f)
        assertEquals(0f, libraryShown(0f, 3), 0f)
    }

    @Test fun `the window blur moves in a few steps rather than every frame`() {
        assertEquals(0, libraryBlurPx(0f, 100))
        assertEquals(100, libraryBlurPx(1f, 100))
        assertEquals(libraryBlurPx(.51f, 100), libraryBlurPx(.54f, 100))
        val distinct = (0..100).map { libraryBlurPx(it / 100f, 100) }.toSet()
        assertEquals(LIBRARY_BLUR_STEPS + 1, distinct.size)
    }

    @Test fun `the wash backs the library's text and is stronger without a real blur`() {
        assertEquals(Color.White.red, libraryWash(inkIsDark = true, blurred = true).red, 0f)
        assertEquals(Color.Black.red, libraryWash(inkIsDark = false, blurred = true).red, 0f)
        assertTrue(libraryWash(false, blurred = false).alpha > libraryWash(false, blurred = true).alpha)
    }
}
