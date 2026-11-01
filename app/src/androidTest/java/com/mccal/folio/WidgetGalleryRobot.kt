package com.mccal.folio

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput

internal fun appLabelOf(context: Context, provider: AppWidgetProviderInfo): String =
    widgetCatalog(context, listOf(provider), AppProfile(0, "Personal", isPersonal = true, isWork = false, quiet = false,
        unlocked = true, available = true)).single().appLabel

internal fun SemanticsNodeInteractionsProvider.searchGallery(text: String) {
    if (onAllNodesWithTag("widget-detail-back").fetchSemanticsNodes().isNotEmpty()) onNodeWithTag("widget-detail-back").performClick()
    onNodeWithTag("widget-catalog-search").performTextClearance()
    onNodeWithTag("widget-catalog-search").performTextInput(text)
}

internal fun SemanticsNodeInteractionsProvider.openGalleryApp(appLabel: String) {
    val row = "widget-app-$appLabel"
    onNodeWithTag("widget-catalog-list", useUnmergedTree = true).performScrollToNode(hasTestTag(row))
    onNodeWithTag(row, useUnmergedTree = true).performClick()
    onNodeWithTag("widget-detail", useUnmergedTree = true).assertIsDisplayed()
}

internal fun SemanticsNodeInteractionsProvider.showInGallery(context: Context, provider: AppWidgetProviderInfo) {
    searchGallery(provider.loadLabel(context.packageManager).toString())
    openGalleryApp(appLabelOf(context, provider))
}

internal fun SemanticsNodeInteractionsProvider.addShownWidget() {
    onNodeWithTag("widget-add").assertIsEnabled().performClick()
}

internal fun SemanticsNodeInteractionsProvider.closeGallery() {
    onAllNodesWithContentDescription("Close").onFirst().performClick()
}
