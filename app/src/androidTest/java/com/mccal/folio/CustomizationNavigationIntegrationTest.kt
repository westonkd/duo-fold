package com.mccal.folio

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CustomizationNavigationIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun model() = ViewModelProvider(compose.activity)[LauncherModel::class.java]
    private fun ready() {
        compose.waitUntil(15_000) { !model().state.value.loading }
        compose.onNodeWithContentDescription("Home page 1").performClick()
        compose.waitForIdle()
    }


    @Test fun systemBackReturnsFromSubpageToOverviewThenHome() {
        ready()
        compose.onNodeWithTag("settings").assertDoesNotExist()
        compose.openHomeCustomization()
        compose.onNodeWithTag("customization-home").performClick()
        compose.onNodeWithTag("customization-back").assertExists()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("customization-general").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        compose.onNodeWithTag("customization-general").assertIsDisplayed()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("search").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        compose.onNodeWithTag("customization-general").assertDoesNotExist()
    }

    @Test fun emptySpaceWallpaperOpensWallpaperControlsWithoutChangingLayoutOrBindings() {
        ready()
        val before = model().state.value.layout
        val idsBefore = model().state.value.widgetPlacements.map { it.slot to it.id }
        val blocked = before.widgetPlacements.flatMapTo(mutableSetOf()) { it.coveredIndices() }
        val empty = (0 until HOME_CELLS).first { before.slotAt(it) == null && it !in blocked }

        compose.onNodeWithTag("home-cell-$empty").performSemanticsAction(SemanticsActions.OnLongClick)
        compose.onNodeWithTag("empty-space-wallpaper").performClick()
        compose.onNodeWithTag("background-change-system").assertIsDisplayed()
        assertEquals(before, model().state.value.layout)
        assertEquals(idsBefore, model().state.value.widgetPlacements.map { it.slot to it.id })
    }

    @org.junit.Ignore("The app options sheet with a Move subpage was replaced by the iOS context menu (Edit Home Screen starts jiggle mode).")
    @Test fun appMoveSubpageBackDoesNotChangePlacement() {
        ready()
        val before = model().state.value.layout
        val appId = (0 until HOME_CELLS).mapNotNull(before::slotAt)
            .first { id -> model().state.value.apps.any { it.id == id } }
        val index = requireNotNull(before.indexOfShortcut(appId))

        val start = compose.onNodeWithTag("home-cell-$index").fetchSemanticsNode().boundsInRoot.center
        compose.onNodeWithTag("launcher-root").performTouchInput { down(start); advanceEventTime(700); up() }
        compose.onNodeWithText("Move on Home").performClick()
        compose.onNodeWithText("Move to first position").assertIsDisplayed()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitForIdle()
        compose.onNodeWithText("Move on Home").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close app options").performClick()
        assertEquals(before, model().state.value.layout)
    }
}
