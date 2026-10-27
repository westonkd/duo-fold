package com.mccal.folio

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The widget gallery lists each app once, however many widgets it has, and shows one app's widgets a page at a time
 * with an Add Widget button, the way iOS does. On the Fold8 805 widgets are installed and 430 come from one pack.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp")
class WidgetGalleryTest {
    @get:Rule val compose = createComposeRule()

    private val personal = AppProfile(0, "Personal", isPersonal = true, isWork = false, quiet = false, unlocked = true, available = true)

    private fun entry(pkg: String, app: String, name: String) = WidgetCatalogEntry(
        AppWidgetProviderInfo().apply {
            provider = ComponentName(pkg, "$pkg.$name")
            AppWidgetProviderInfo::class.java.getField("providerInfo").set(this, android.content.pm.ActivityInfo().apply {
                packageName = pkg; this.name = "$pkg.$name"
                applicationInfo = android.content.pm.ApplicationInfo().apply { packageName = pkg }
            })
        }, name, app, "", 0, "Personal")

    private val entries = (1..10).map { entry("com.pack", "Pack", "W$it") } + listOf(entry("com.clock", "Clock", "Alarm"), entry("com.clock", "Clock", "World"))

    private val added = mutableListOf<Pair<GalleryWidget, WidgetSpan>>()
    private val resizable = GallerySizes(WidgetSpan(2, 2), WIDGET_SIZE_PRESETS)

    private fun show() = compose.setContent {
        VisualWidgetPicker(entries, listOf(personal), personal, {}, {}, hiddenForDrag = false, sizesOf = { resizable },
            cellWidthDp = 80f, rowHeightDp = 90f, onBack = {}, onAdd = { widget, span -> added += widget to span },
            onDragStart = { _, _, _ -> }, onDrag = {}, onDrop = {}, onCancelDrag = {})
    }

    private fun scrollTo(tag: String) = compose.onNodeWithTag("widget-catalog-list", useUnmergedTree = true)
        .performScrollToNode(hasTestTag(tag))

    @Test fun `each app is one row, however many widgets it has`() {
        show()
        scrollTo("widget-app-Pack")
        compose.onNodeWithTag("widget-app-Pack", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodes(hasTestTag("widget-app-Pack"), useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodes(hasTestTag("widget-detail"), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun `choosing an app shows its widgets, and Add Widget adds the one on screen at its size`() {
        show()
        scrollTo("widget-app-Clock")
        compose.onNodeWithTag("widget-app-Clock", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("widget-detail", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Alarm").assertIsDisplayed()
        compose.onNodeWithTag("widget-add", useUnmergedTree = true).performClick()
        compose.waitUntil(2_000) { added.isNotEmpty() }
        val (widget, span) = added.single()
        assertEquals("Alarm", (widget as GalleryWidget.Provider).entry.providerLabel)
        assertEquals(WidgetSpan(2, 2), span)
    }

    @Test fun `a size chosen before adding is the size added`() {
        show()
        scrollTo("widget-app-Clock")
        compose.onNodeWithTag("widget-app-Clock", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Large").performClick()
        compose.onNodeWithTag("widget-add", useUnmergedTree = true).performClick()
        compose.waitUntil(2_000) { added.isNotEmpty() }
        assertEquals(WidgetSpan(4, 4), added.single().second)
    }

    @Test fun `Folio's own widgets are featured above the apps and listed with them`() {
        show()
        compose.onNodeWithTag("widget-featured", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("widget-app-folio", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun `searching narrows the list to apps with a matching widget`() {
        show()
        compose.onNodeWithTag("widget-catalog-search", useUnmergedTree = true).performTextInput("world")
        compose.onAllNodes(hasTestTag("widget-app-Pack"), useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag("widget-app-Clock", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodes(hasTestTag("widget-featured"), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun `Folio's sizes are the ones each card is drawn for`() {
        assertEquals(listOf(WidgetSpan(2, 2)), builtinGallerySizes(CLOCK_WIDGET).options)
        assertEquals(listOf(WidgetSpan(4, 2)), builtinGallerySizes(BIG_CLOCK_WIDGET).options)
        assertEquals(WIDGET_SIZE_PRESETS, builtinGallerySizes(SUGGESTIONS_WIDGET).options)
    }

    @Test fun `replacing a widget starts at its current size when the new one allows it`() {
        assertEquals(WidgetSpan(4, 2), gallerySizeFor(resizable, WidgetSpan(4, 2)))
        assertEquals(WidgetSpan(2, 2), gallerySizeFor(resizable, WidgetSpan(3, 1)))
        assertEquals(WidgetSpan(2, 2), gallerySizeFor(resizable, null))
    }
}
