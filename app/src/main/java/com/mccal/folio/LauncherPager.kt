package com.mccal.folio

import androidx.compose.foundation.pager.PagerState

internal class LauncherPager(val state: PagerState, val firstHome: Int) {
    val currentPage get() = state.currentPage - firstHome
    val settledPage get() = state.settledPage - firstHome
    val currentPageOffsetFraction get() = state.currentPageOffsetFraction
    val position get() = state.currentPage + state.currentPageOffsetFraction - firstHome
    val isScrollInProgress get() = state.isScrollInProgress
    val firstPage get() = -firstHome
    private fun physical(page: Int) = (page + firstHome).coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))
    fun requestScrollToPage(page: Int) = state.requestScrollToPage(physical(page))
    suspend fun scrollToPage(page: Int) = state.scrollToPage(physical(page))
    suspend fun glideToPage(page: Int) = state.animateScrollToPage(physical(page), animationSpec = FolioMotion.spring(FolioMotion.Page))
}

internal fun widgetsPageShown(position: Float): Float = (-position).coerceIn(0f, 1f)

internal fun pagerHomeAfterSwitch(logicalPage: Int, firstHome: Int, unfolding: Boolean): Int =
    if (unfolding) 0 else logicalPage.coerceAtLeast(-firstHome)
