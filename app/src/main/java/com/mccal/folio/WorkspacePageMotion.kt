package com.mccal.folio

import kotlin.math.abs

/**
 * Maps the native full-width pager position to the scroll distance shown by the
 * expanded workspace. Home pages advance by one pane while the pages outside
 * Home still enter and leave across the full pager width.
 */
internal data class WorkspacePageMotion(
    val homePages: Int,
    val pageWidth: Float,
    val homeStride: Float,
) {
    init {
        require(homePages > 0)
        require(pageWidth.isFinite() && pageWidth > 0f)
        require(homeStride.isFinite() && homeStride > 0f)
    }

    private val lastHome = homePages - 1
    private val lastHomeOffset = lastHome * homeStride

    /** Visual scroll offset for a (possibly fractional) pager position. */
    fun offset(position: Float): Float = when {
        position < 0f -> position * pageWidth
        position <= lastHome -> position * homeStride
        else -> lastHomeOffset + (position - lastHome) * pageWidth
    }

    /** Pager position for a visual scroll offset. */
    fun position(offset: Float): Float = when {
        offset < 0f -> offset / pageWidth
        offset <= lastHomeOffset && lastHome > 0 -> offset / homeStride
        else -> lastHome + (offset - lastHomeOffset) / pageWidth
    }

    fun positionAfterVisualDelta(position: Float, delta: Float): Float =
        position(offset(position) + delta)

    fun stride(fromPosition: Int, towardPosition: Int): Float =
        abs(offset(towardPosition.toFloat()) - offset(fromPosition.toFloat()))
}
