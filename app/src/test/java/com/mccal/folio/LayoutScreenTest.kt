package com.mccal.folio

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which saved layout a window gets: the cover's, or the inner screen's, which the upright inner screen shares. */
class LayoutScreenTest {
    private val sizes = org.json.JSONObject(javaClass.getResource("/screen-matrix.json")!!.readText()).getJSONArray("devices").let { list ->
        (0 until list.length()).flatMap { i ->
            val d = list.getJSONObject(i); val w = d.getDouble("width").toFloat(); val h = d.getDouble("height").toFloat()
            listOf(w to h, h to w, maxOf(w, h) / 2f to minOf(w, h))
        }
    } + listOf(649f to 700f, 650f to 559f, 650f to 560f, 700f to 700f, 704f to 932f, 932f to 704f)

    @Test fun `every window gets the layout it always did`() {
        val state = LauncherState(compact = LayoutPreset(DockPlacement.SIDE), expanded = LayoutPreset(DockPlacement.BOTTOM))
        for ((w, h) in sizes) for (scale in listOf(1f, 1.2f)) {
            val before = if (w * scale >= EXPANDED_HOME_MIN_WIDTH_DP && h * scale >= HOME_REGULAR_MIN_HEIGHT_DP) state.expanded else state.compact
            assertEquals("$w x $h at $scale", before, state.presetFor(layoutScreenFor(w, h, scale)))
        }
    }

    @Test fun `an upright inner screen is the one taller than wide, among the expanded windows`() {
        assertEquals(LayoutScreen.INNER_UPRIGHT, layoutScreenFor(704f, 932f))
        assertEquals(LayoutScreen.INNER, layoutScreenFor(932f, 704f))
        assertEquals(LayoutScreen.INNER, layoutScreenFor(700f, 700f))
        assertEquals("the cover stays the cover however it is held", LayoutScreen.COVER, layoutScreenFor(475f, 751f))
        assertEquals(LayoutScreen.COVER, layoutScreenFor(649f, 700f))
    }

    @Test fun `the upright inner screen shares the inner layout, and changing it changes the inner one`() {
        val state = LauncherState().withPreset(LayoutScreen.INNER_UPRIGHT, LayoutPreset(DockPlacement.SIDE))
        assertEquals(LayoutPreset(DockPlacement.SIDE), state.expanded)
        assertEquals(state.expanded, state.presetFor(LayoutScreen.INNER_UPRIGHT))
        assertEquals(LayoutPreset(), state.compact)
    }

    @Test fun `an old separate upright layout is read past`() {
        val saved = JSONObject().put("expanded", JSONObject().put("iconSize", 60.0).put("dockPlacement", "SIDE"))
            .put("portrait", JSONObject().put("iconSize", 44.0).put("dockPlacement", "BOTTOM"))
        val state = decodeLauncherState(saved.toString(), legacyRaw = null)
        assertEquals(LayoutPreset(DockPlacement.SIDE), state.expanded)
        assertEquals(state.expanded, state.presetFor(LayoutScreen.INNER_UPRIGHT))
    }
}
