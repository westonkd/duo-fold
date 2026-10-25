package com.mccal.folio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Where the 21 Oct regrouping put things (docs/audits/2026-10-20-settings-redundancy.md): the sheet's source is the spec. */
class SettingsRegroupTest {
    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "CHANGELOG.md").exists() }
    private val sheet = File(root, "app/src/main/java/com/mccal/folio/CustomizationSheet.kt").readText()

    private fun between(start: String, end: String): String {
        val a = sheet.indexOf(start); require(a >= 0) { "missing: $start" }
        val b = sheet.indexOf(end, a + start.length); require(b >= 0) { "missing: $end" }
        return sheet.substring(a, b)
    }

    @Test fun theDockCardHoldsItsPlacement() {
        val card = between("SettingsCard(stringResource(R.string.dock)) {", "SheetGroupLabel(stringResource(R.string.widgets_page_number")
        assertTrue(card.contains("IosMenuRow(stringResource(R.string.placement)") && card.contains("dock-placement"))
        assertTrue(sheet.contains("R.string.placement to CustomizationPage.HOME"))
    }

    @Test fun searchForSideBarLandsOnTheStatusPageWhereItsCardIs() {
        assertTrue(between("CustomizationPage.STATUS -> {", "CustomizationPage.FOLD -> {").contains("SettingsCard(stringResource(R.string.side_rail)) {"))
        assertTrue(sheet.contains("R.string.side_rail to CustomizationPage.STATUS"))
        assertFalse(sheet.contains("R.string.side_rail to CustomizationPage.HOME"))
    }
}
