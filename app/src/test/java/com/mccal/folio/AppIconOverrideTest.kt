package com.mccal.folio

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Per-app icon pictures: only real choices are kept, they survive saving, and old saves load without the old looks. */
class AppIconOverrideTest {
    private val mail = "com.example.mail/.Main"

    @Test fun `an override without a picture is no entry at all`() {
        assertTrue(AppIconOverride().isDefault)
        assertEquals(emptyMap<String, AppIconOverride>(), editAppIcon(emptyMap(), mail, AppIconOverride()))
        val chosen = editAppIcon(emptyMap(), mail, AppIconOverride(picture = 7L))
        assertEquals(mapOf(mail to AppIconOverride(picture = 7L)), chosen)
        assertEquals("removing the picture removes it", emptyMap<String, AppIconOverride>(), editAppIcon(chosen, mail, AppIconOverride()))
    }

    @Test fun `choices round trip through JSON, and damaged ones are dropped`() {
        val map = mapOf(mail to AppIconOverride(picture = 3L), "b" to AppIconOverride(picture = 4L))
        assertEquals(map, appIconStylesFromJson(appIconStylesToJson(map)))
        val damaged = JSONObject("""{"a":{"style":"SPARKLE"},"b":"nope","c":{"picture":5},"d":{}}""")
        assertEquals(mapOf("c" to AppIconOverride(picture = 5L)), appIconStylesFromJson(damaged))
        assertEquals(emptyMap<String, AppIconOverride>(), appIconStylesFromJson(null))
    }

    @Test fun `a save from before has none, an old style or shape is dropped, and a picture is read back`() {
        assertTrue(decodeLauncherState("{}", legacyRaw = null).appIconStyles.isEmpty())
        val saved = JSONObject().put("appIconStyles", JSONObject().put(mail, JSONObject().put("shape", "CIRCLE"))
            .put("b", JSONObject().put("style", "DARK").put("picture", 9L))).toString()
        assertEquals(mapOf("b" to AppIconOverride(picture = 9L)), decodeLauncherState(saved, legacyRaw = null).appIconStyles)
    }

    @Test fun `nothing set writes nothing`() {
        assertFalse(appIconStylesToJson(mapOf(mail to AppIconOverride())).has(mail))
    }

    @Test fun `an app that is only remembered for its picture is still tracked, so uninstalling it clears the picture`() {
        val state = LauncherState(appIconStyles = mapOf(mail to AppIconOverride(picture = 2L)), appNames = mapOf("renamed" to "X"),
            dock = listOf("docked", null, null, null), homeSlots = listOf("homed"))
        assertEquals(setOf(mail, "renamed", "docked", "homed"), state.trackedAppIds().toSet())
    }
}
