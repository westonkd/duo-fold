package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The wallpaper's tone as Android shares it, and what Diagnostics reports about it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WallpaperToneTest {
    private val blue = 0xFF1E5AA8.toInt()
    private val orange = 0xFFE07A1F.toInt()
    private val green = 0xFF2E8B57.toInt()

    @Test fun `when Android shares no colors the tone has none and does not make any up`() {
        val none: android.app.WallpaperColors? = null
        val tone = none.tone()
        assertNull(tone.primary); assertNull(tone.secondary)
    }

    @Test fun `Android's colors become the tone`() {
        val colors = android.app.WallpaperColors(android.graphics.Color.valueOf(blue), android.graphics.Color.valueOf(orange),
            android.graphics.Color.valueOf(green))
        assertEquals(WallpaperTone(primary = blue, secondary = orange), colors.tone())
    }

    @Test fun `Diagnostics says what Android returned for the wallpaper colors`() {
        val text = Diagnostics.wallpaperReportText(home = listOf(blue, orange, green), lock = null)
        assertTrue(text, text.contains("Android's colors, home: #1E5AA8 #E07A1F #2E8B57"))
        assertTrue(text, text.contains("Android's colors, lock: none"))
    }

    @Test fun `Diagnostics lists the system palette and the theme color source`() {
        val palette = SystemPalette(0xFF6750A4.toInt(), 0xFF625B71.toInt(), 0xFF7D5260.toInt())
        val text = Diagnostics.wallpaperReportText(null, null, palette.colors, "home_wallpaper")
        assertTrue(text, text.contains("system palette (accent 1, 2, 3 at 500): #6750A4 #625B71 #7D5260, theme color source: home_wallpaper"))
    }

    @Test fun `the theme color source is read from AOSP's setting`() {
        assertEquals("home_wallpaper", Diagnostics.themeColorSource("""{"android.theme.customization.color_source":"home_wallpaper","android.theme.customization.system_palette":"B1611C"}"""))
        assertEquals("preset", Diagnostics.themeColorSource("""{ "android.theme.customization.color_source" : "preset" }"""))
        assertNull(Diagnostics.themeColorSource("""{"android.theme.customization.system_palette":"B1611C"}"""))
        assertNull(Diagnostics.themeColorSource(null))
    }
}
