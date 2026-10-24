package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRowTargetTest {
    @Test fun aTitleInBothListsIsOneSearchResult() {
        val results = SettingsEntries.map { it.first to it.third }
        assertEquals("Search lists a setting twice: ${results.groupBy { it }.filterValues { it.size > 1 }.keys}", results.distinct().size, results.size)
    }

    @Test fun anEntryWithNoKeywordsIsARow() = assertTrue(isRowEntry(R.string.text_color, null))

    @Test fun aTopicWithKeywordsThatIsNotARowOpensItsPageAtTheTop() {
        assertFalse(isRowEntry(R.string.settings_background_wallpaper, R.string.settings_keywords_background_wallpaper))
    }
}
