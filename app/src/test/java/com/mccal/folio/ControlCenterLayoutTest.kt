package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlCenterLayoutTest {
    private val geometry = CcGeometry(cell = 66f, gap = 12f)

    @Test fun `a 2x2 module is two cells and the gap between them`() {
        assertEquals(144f, geometry.module, .001f)
    }

    @Test fun `the four connectivity toggles are spaced evenly, with the same gap at the edges and between them`() {
        assertEquals(geometry.module, geometry.toggle * 2 + geometry.inset * 3, .001f)
    }

    @Test fun `the album art corner is concentric with the module corner`() {
        assertEquals(geometry.moduleCorner, geometry.artCorner + geometry.inset, .001f)
        assertTrue(geometry.artCorner > 0f)
    }

    @Test fun `the Focus toggle sits concentric inside its capsule`() {
        assertEquals(geometry.cell / 2f, geometry.capsuleToggle / 2f + geometry.inset, .001f)
    }

    @Test fun `a light wallpaper keeps the full scrim and a dark one gets a lighter one`() {
        assertEquals(1f, controlCenterDim(.9f), 0f)
        assertEquals(CC_DIM_MIN, controlCenterDim(.1f), 0f)
        assertTrue(controlCenterDim(.5f) > controlCenterDim(.35f))
    }

    @Test fun `the scrim never lets a mid wallpaper show brighter than the backdrop limit`() {
        val luminance = .5f
        val scrim = FolioGlass.scrim.alpha * controlCenterDim(luminance)
        assertTrue(luminance * (1f - scrim) <= CC_BACKDROP_MAX + .001f)
    }

    @Test fun `Control Center's glass is Home's glass, tinted the same way`() {
        val wallpaper = 0xFFC08060.toInt()
        val home = homePalette(LightDuoPalette, wallpaper, .4f, solid = false, inkDark = false)
        assertEquals(tintedGlass(LightDuoPalette.glass, wallpaper, .4f), home.glass)
        assertEquals(LightDuoPalette, homePalette(LightDuoPalette, wallpaper, 0f, solid = false, inkDark = false))
    }

    @Test fun `Reduce Transparency makes the glass dark under white text and light under dark text`() {
        val dark = homePalette(LightDuoPalette, null, 0f, solid = true, inkDark = false)
        val light = homePalette(LightDuoPalette, null, 0f, solid = true, inkDark = true)
        assertEquals(FolioColors.SecondaryBackground, dark.glass)
        assertEquals(FolioColors.LightBackground, light.glass)
    }
}
