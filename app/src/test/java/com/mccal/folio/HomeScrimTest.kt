package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The legibility gradient over Home's background: that it is strongest where Home's text is, that it is subtle enough
 * to leave a pale wallpaper looking like itself, that it stands down under dark text, and that it is free while a
 * page moves.
 */
class HomeScrimTest {
    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "CHANGELOG.md").exists() }
    private fun source(name: String) = File(root, "app/src/main/java/com/mccal/folio/$name").readText()

    /** WCAG relative luminance of an sRGB colour, each channel 0..1. */
    private fun luminance(r: Float, g: Float, b: Float): Double {
        fun lin(c: Float) = if (c <= .04045f) c / 12.92 else Math.pow((c + .055) / 1.055, 2.4)
        return .2126 * lin(r) + .7152 * lin(g) + .0722 * lin(b)
    }

    /** Contrast of white against an sRGB colour that a black scrim at [alpha] has been laid over. */
    private fun whiteOver(hex: Int, alpha: Float): Double {
        fun channel(shift: Int) = (hex shr shift and 0xFF) / 255f * (1f - alpha)
        return 1.05 / (luminance(channel(16), channel(8), channel(0)) + .05)
    }

    /**
     * A pale sand, about as light as a wallpaper gets before Automatic should pick dark text: the lit paper of a
     * Hiroshige print Folio once shipped, kept as the hard case the scrim has to work over.
     */
    private val paleSand = 0xDFC69B

    @Test fun `turning it off draws nothing at all`() {
        assertTrue(!HomeScrim.of(on = false, darkText = false).draws)
        assertEquals(HomeScrim.None, HomeScrim.of(on = false, darkText = false))
    }

    @Test fun `dark text on Home means no scrim, because a dark scrim would take its contrast away`() {
        assertTrue(!HomeScrim.of(on = true, darkText = true).draws)
    }

    @Test fun `on by default, over a light wallpaper, it draws at both ends`() {
        assertTrue(LauncherState().homeScrim)
        val scrim = HomeScrim.of(on = true, darkText = false)
        assertEquals(HomeScrim.TOP, scrim.top, 1e-6f)
        assertEquals(HomeScrim.BOTTOM, scrim.bottom, 1e-6f)
        assertTrue("the bottom carries the page dots and the two round controls, so it is the stronger end",
            scrim.bottom > scrim.top)
    }

    @Test fun `the two bands leave the middle of the wallpaper alone`() {
        assertTrue("the bands must not meet, let alone overlap: ${HomeScrim.BAND}", HomeScrim.BAND * 2f < 1f)
        // What can actually be seen is smaller than the band: the falloff is under a tenth of the peak by four
        // fifths of the way in, so the grid's own rows keep their wallpaper.
        assertTrue(HomeScrim.alphaAt(1f, .8f) < .1f)
        assertEquals(0f, HomeScrim.alphaAt(1f, 1f), 1e-6f)
    }

    @Test fun `it is strongest at the rows Home really draws its weakest text on`() {
        // A Fold8 cover window, 2376 px tall. Fractions, so the same holds on any window.
        val h = 2376f
        fun topAt(y: Float) = HomeScrim.alphaAt(HomeScrim.TOP, y / (h * HomeScrim.BAND))
        fun bottomAt(fromBottom: Float) = HomeScrim.alphaAt(HomeScrim.BOTTOM, fromBottom / (h * HomeScrim.BAND))

        // The page dots and the two round controls beside them sit in the last 6% of the window, with no shadow and no
        // glass: they keep at least three quarters of the peak.
        assertTrue(bottomAt(h * .06f) > HomeScrim.BOTTOM * .75f)
        // The dock bar, about a fifth of the way up, still gets something.
        assertTrue(bottomAt(h * .19f) > HomeScrim.BOTTOM * .1f)
        // The clock and date, in the first tenth: most of the top's strength.
        assertTrue(topAt(h * .1f) > HomeScrim.TOP * .6f)
        // And the grid's middle rows, which already have a shadow per glyph, get nothing at all.
        assertEquals(0f, topAt(h * .5f), 0f)
        assertEquals(0f, bottomAt(h * .5f), 0f)
    }

    @Test fun `the falloff is smooth, starts at the peak and never brightens on the way out`() {
        val peak = HomeScrim.BOTTOM
        val ramp = List(21) { HomeScrim.alphaAt(peak, it / 20f) }
        assertEquals(peak, ramp.first(), 1e-6f)
        assertEquals(0f, ramp.last(), 1e-6f)
        ramp.zipWithNext { a, b -> assertTrue("the scrim must only ever fade out: $ramp", b <= a + 1e-6f) }
        // No visible edge at either end of the band: the curve leaves and arrives flat.
        assertTrue(ramp[1] > peak * .97f)
        assertTrue(ramp[ramp.size - 2] < peak * .03f)
        // The drawn stops are the same curve, black only, so the scrim cannot cast a colour.
        val stops = HomeScrim.ramp(peak)
        assertEquals(HomeScrim.STOPS, stops.size)
        stops.forEachIndexed { i, colour ->
            assertEquals(0f, colour.red, 0f); assertEquals(0f, colour.green, 0f); assertEquals(0f, colour.blue, 0f)
            // A colour holds its alpha to eight bits, so it can be a 255th out and no more.
            assertEquals(HomeScrim.alphaAt(peak, i / (HomeScrim.STOPS - 1f)), colour.alpha, 1f / 255f)
        }
    }

    @Test fun `white icons, dots and large text reach 3 to 1 over a pale wallpaper (A11Y-9)`() {
        // Without the scrim white text on Folio's own palest sand is hopeless, which is why this exists.
        assertTrue("white on bare lit paper: ${whiteOver(paleSand, 0f)}", whiteOver(paleSand, 0f) < 2.0)
        assertTrue("top edge: ${whiteOver(paleSand, HomeScrim.TOP)}", whiteOver(paleSand, HomeScrim.TOP) >= 3.0)
        assertTrue("bottom edge: ${whiteOver(paleSand, HomeScrim.BOTTOM)}", whiteOver(paleSand, HomeScrim.BOTTOM) >= 3.0)
    }

    @Test fun `it stops well short of a dark filter`() {
        // Not darker at its peak than a .3 black over the whole screen, so it stays a dim rather than a filter.
        assertTrue(HomeScrim.TOP < .3f)
        assertTrue(HomeScrim.BOTTOM <= .32f)
        // A11Y-9's 4.5:1 for body-size white text over that same sand is out of a scrim's reach: it would take about
        // .43, which is a filter. That case is Automatic dark text's, not the scrim's, and is why dark ink wins above.
        assertTrue("a subtle scrim cannot get body text to 4.5:1", whiteOver(paleSand, HomeScrim.BOTTOM) < 4.5)
        assertTrue("and .43 is what it would take", whiteOver(paleSand, .43f) >= 4.5)
    }

    @Test fun `the scrim costs no blur, no render effect and no layer of its own`() {
        val scrim = source("HomeScrim.kt")
        // Comments out: they name these very things to say the scrim does not use them.
        val code = scrim.lines().filterNot { it.trim().let { l -> l.startsWith("*") || l.startsWith("//") || l.startsWith("/*") } }
            .joinToString("\n")
        for (costly in listOf("RenderEffect", "blur", "CompositingStrategy", "graphicsLayer")) {
            assertTrue("$costly would cost a frame what this must not", costly !in code)
        }
        // The brushes are built once per size, so a swipe over Android's wallpaper does not allocate per frame.
        assertTrue("drawWithCache" in code)
    }

    @Test fun `Home composes the scrim with the text colour, and draws it over Android's wallpaper`() {
        val screen = source("LauncherScreen.kt")
        assertTrue("the scrim has to take the resolved ink",
            "HomeScrim.of(state.homeScrim, homeInk.dark)" in screen)
        assertTrue("Android's wallpaper is the system's to draw, so Home draws the bands over it",
            "if (scrim.draws) Box(Modifier.fillMaxSize().homeScrim(scrim))" in screen)
    }

    @Test fun `the setting is saved under its own key, and a phone that never had it gets it on`() {
        val model = source("LauncherModel.kt")
        assertTrue("""put("homeScrim", s.homeScrim)""" in model)
        assertTrue("""homeScrim = j.optBoolean("homeScrim", true)""" in model)
        val sheet = source("CustomizationSheet.kt")
        val row = sheet.lines().first { "home-scrim-switch" in it }
        assertTrue("the switch reads and writes the setting: $row",
            "state.homeScrim" in row && "model::setHomeScrim" in row)
        assertTrue("it belongs in the Text on Home card, beside the colour it exists for",
            sheet.indexOf("R.string.text_on_home)") < sheet.indexOf("home-scrim-switch"))
    }
}
