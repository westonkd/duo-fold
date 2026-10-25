package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Turning Full-Width Home off puts back exactly what turning it on replaced, not a guess. */
class FullWidthRestoreTest {
    @Test fun `off puts back the dock placement and the Side Bar that were there`() {
        val mine = LauncherState(coverDock = DockPlacement.SIDE, verticalStatus = true)
        val on = mine.withFullWidthHome(LayoutScreen.COVER, true)
        assertEquals(DockPlacement.BOTTOM, on.coverDock)
        assertFalse(on.verticalStatus)
        val off = on.withFullWidthHome(LayoutScreen.COVER, false)
        assertEquals("not Automatic: the Side Bar it was", DockPlacement.SIDE, off.coverDock)
        assertTrue(off.verticalStatus)
        assertEquals("nothing is left remembered", emptyMap<String, String>(), off.fullWidthRestore)
        assertEquals(mine, off)
    }

    @Test fun `a Side Bar that was already off stays off`() {
        val mine = LauncherState(verticalStatus = false)
        val back = mine.withFullWidthHome(LayoutScreen.COVER, true).withFullWidthHome(LayoutScreen.COVER, false)
        assertFalse(back.verticalStatus)
        assertEquals(mine.coverDock, back.coverDock)
    }

    @Test fun `with two screens in it, the Side Bar waits for the last one`() {
        val mine = LauncherState()
        val both = mine.withFullWidthHome(LayoutScreen.COVER, true).withFullWidthHome(LayoutScreen.INNER, true)
        val oneOff = both.withFullWidthHome(LayoutScreen.COVER, false)
        assertFalse("the inner screen is still full width", oneOff.verticalStatus)
        assertEquals(DockPlacement.BOTTOM, oneOff.innerDock)
        val allOff = oneOff.withFullWidthHome(LayoutScreen.INNER, false)
        assertTrue(allOff.verticalStatus)
        assertEquals(mine, allOff)
    }

    @Test fun `off with nothing remembered goes to the automatic dock and the Side Bar`() {
        val odd = LauncherState(coverDock = DockPlacement.BOTTOM, verticalStatus = false)
        val off = odd.withFullWidthHome(LayoutScreen.COVER, false)
        assertEquals(DockPlacement.AUTOMATIC, off.coverDock)
        assertTrue(off.verticalStatus)
    }

    @Test fun `what it remembers survives a save, and an older save has nothing`() {
        assertTrue(decodeLauncherState("{}", legacyRaw = null).fullWidthRestore.isEmpty())
        val saved = """{"fullWidthRestore":{"COVER":"SIDE","status":"true"}}"""
        assertEquals(mapOf("COVER" to "SIDE", "status" to "true"), decodeLauncherState(saved, legacyRaw = null).fullWidthRestore)
    }

    @Test fun `a bottom dock that was already there comes back as the bottom dock`() {
        val mine = LauncherState(coverDock = DockPlacement.BOTTOM, verticalStatus = true)
        val on = mine.withFullWidthHome(LayoutScreen.COVER, true)
        assertEquals("BOTTOM", on.fullWidthRestore["COVER"])
        assertEquals(mine, on.withFullWidthHome(LayoutScreen.COVER, false))
    }

    @Test fun `an old separate upright layout in Full-Width Home is merged back, and the Side Bar returns when nothing else is in it`() {
        val upright = """{"verticalStatus":false,"fullWidthRestore":{"INNER_UPRIGHT":"AUTOMATIC","status":"true"}}"""
        val merged = decodeLauncherState(upright, legacyRaw = null)
        assertTrue(merged.verticalStatus)
        assertTrue(merged.fullWidthRestore.isEmpty())
        // With the cover still in it, the Side Bar stays away.
        val both = """{"verticalStatus":false,"fullWidthRestore":{"COVER":"SIDE","INNER_UPRIGHT":"AUTOMATIC","status":"true"}}"""
        assertFalse(decodeLauncherState(both, legacyRaw = null).verticalStatus)
        val cover = LauncherState().withFullWidthHome(LayoutScreen.COVER, true)
        assertEquals(cover, cover.withoutFullWidthEntry(LayoutScreen.INNER))
    }
}
