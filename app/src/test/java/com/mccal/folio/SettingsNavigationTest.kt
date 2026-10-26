package com.mccal.folio

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Settings' Back: what it says, and where it lands. Settings is the list, five pages under it, and the pages General
 * opens. Back returns to the page above, which keeps its scroll, and a page opened again starts at its top.
 */
class SettingsNavigationTest {
    @Before fun forget() { SettingsMemory.overviewScroll = 0 }

    private val listPages = CustomizationPage.entries.filter { it.parent == CustomizationPage.OVERVIEW }
    private val generalPages = CustomizationPage.entries.filter { it.parent == CustomizationPage.GENERAL }

    @Test fun `the list opens exactly five pages`() {
        assertEquals(listOf(CustomizationPage.HOME, CustomizationPage.SEARCH, CustomizationPage.GESTURES, CustomizationPage.FOLD, CustomizationPage.GENERAL), listPages)
    }

    @Test fun `General opens Diagnostics and Credits & Licenses, and they belong under it in the list`() {
        assertEquals(listOf(CustomizationPage.DIAGNOSTICS, CustomizationPage.CREDITS), generalPages)
        for (page in generalPages) assertEquals(CustomizationPage.GENERAL, page.topLevel)
    }

    @Test fun `Back names the page above`() {
        for (page in listPages) assertEquals("$page", R.string.folio, settingsBackLabel(page, columns = 1))
        for (page in generalPages) assertEquals("$page", R.string.general, settingsBackLabel(page, columns = 1))
        assertNull("the list has nowhere to go back to", settingsBackLabel(CustomizationPage.OVERVIEW, columns = 1))
    }

    @Test fun `in two columns the list is on screen, so only General's pages have a Back`() {
        for (page in listPages + CustomizationPage.OVERVIEW) assertNull("$page", settingsBackLabel(page, columns = 2))
        for (page in generalPages) assertEquals("$page", R.string.general, settingsBackLabel(page, columns = 2))
    }

    @Test fun `Back from a page General opened lands where General was left`() = runBlocking {
        val scroll = SettingsScroll(CustomizationPage.GENERAL, 0)
        scroll.state.scrollTo(700)
        scroll.show(CustomizationPage.DIAGNOSTICS).scrollTo(300)
        assertEquals(700, scroll.show(CustomizationPage.GENERAL).value)
    }

    @Test fun `Back lands where the list was left, and a page opened again starts at its top`() = runBlocking {
        val scroll = SettingsScroll(CustomizationPage.OVERVIEW, 0)
        scroll.state.scrollTo(900)                                       // down the list to General
        scroll.show(CustomizationPage.GENERAL).scrollTo(240)             // and down General
        assertEquals(900, scroll.show(CustomizationPage.OVERVIEW).value) // Back
        assertEquals(0, scroll.show(CustomizationPage.GENERAL).value)    // opened anew from the list
    }

    @Test fun `a page reached from another page starts at its top`() = runBlocking {
        val scroll = SettingsScroll(CustomizationPage.HOME, 0)
        scroll.state.scrollTo(500)
        assertEquals(0, scroll.show(CustomizationPage.SEARCH).value)     // from the sidebar, beside the list
        assertEquals(0, scroll.show(CustomizationPage.HOME).value)       // and back to it the same way: not Back
    }
}
