package com.mccal.folio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassTest {
    private val glass = LightDuoPalette.glass

    @Test fun `the body keeps the surface's frost and thins toward the far corner`() {
        val spec = liquidGlassSpec(glass, .3f, GlassRole.PANEL)
        assertEquals(.3f, spec.bodyStart.alpha, .01f)
        assertTrue(spec.bodyEnd.alpha < spec.bodyStart.alpha)
    }

    @Test fun `the body leans toward the wallpaper's colors`() {
        val red = Color.Red.toArgb()
        val blue = Color.Blue.toArgb()
        val spec = liquidGlassSpec(glass, .3f, GlassRole.PANEL, primary = red, secondary = blue)
        assertTrue(spec.bodyStart.red > spec.bodyStart.blue)
        assertTrue(spec.bodyEnd.blue > spec.bodyEnd.red)
        assertEquals(liquidGlassSpec(glass, .3f, GlassRole.PANEL).bodyStart, liquidGlassSpec(glass, .3f, GlassRole.PANEL, null, null).bodyStart)
    }

    @Test fun `raised glass is lit from the top left and a field is lit from below`() {
        val panel = liquidGlassSpec(glass, .3f, GlassRole.PANEL)
        assertTrue(panel.rimLead.alpha > panel.rimTrail.alpha)
        assertTrue(panel.rimSide.alpha < panel.rimTrail.alpha)
        val field = liquidGlassSpec(glass, .12f, GlassRole.FIELD)
        assertTrue(field.rimTrail.alpha > field.rimLead.alpha)
        assertEquals(0f, field.light, 0f)
    }

    @Test fun `only floating glass casts a shadow`() {
        GlassRole.entries.forEach { role ->
            val lift = liquidGlassSpec(glass, .3f, role).lift
            if (role == GlassRole.FLOATING) assertTrue(lift > 0f) else assertEquals(0f, lift, 0f)
        }
    }

    @Test fun `a save with the old glass settings still loads`() {
        val old = """{"glassMaterial":"CLASSIC","reduceTransparency":true,"widgetGlass":.55,"glassOutline":0,"tintedGlass":false,"glassTint":.9,"folderBackground":"SOLID"}"""
        assertEquals(LauncherState().homeScrim, decodeLauncherState(old, legacyRaw = null).homeScrim)
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

    @Test fun `Spotlight's glass is Home's glass, tinted the same way`() {
        val wallpaper = 0xFFC08060.toInt()
        val home = homePalette(LightDuoPalette, wallpaper)
        assertEquals(tintedGlass(LightDuoPalette.glass, wallpaper, GLASS_TINT), home.glass)
        assertEquals(LightDuoPalette, homePalette(LightDuoPalette, null))
    }
}
