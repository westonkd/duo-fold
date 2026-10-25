package com.mccal.folio

import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

/**
 * How much to scale dp by to judge a size class at the phone's own screen density. Developer options' "Smallest
 * width" and Display size change how many dp a screen reports, but a phone-sized screen should still get the phone
 * layout: a Galaxy Z Fold8 cover set to 600dp was getting the unfolded one (bottom dock, wide margins).
 */
internal val Configuration.classScale: Float
    get() = classScale(densityDpi, DisplayMetrics.DENSITY_DEVICE_STABLE)

/** Regular size (the unfolded screen, tablets), judged at the phone's own density. */
internal fun Configuration.fitsRegularHomeLayout(): Boolean = fitsRegularHomeLayout(screenWidthDp.toFloat(), screenHeightDp.toFloat(), classScale)

/**
 * How many columns Settings shows at once.
 *
 * 1 is the phone: one page at a time, and above 600 dp the list opens over it from the sidebar button. 2 is iPad
 * Settings: the list beside the page, in either orientation, once there's room for two readable columns.
 */
internal fun settingsColumns(
    widthDp: Float,
    heightDp: Float,
    classScale: Float = 1f,
    keyboardDp: Float = 0f,
): Int = if (!fitsRegularHomeLayout(widthDp, sizeClassHeightDp(heightDp, keyboardDp), classScale) || widthDp < 700f) 1 else 2

/**
 * The height a size class is judged by while the keyboard is up. A keyboard covers a window; it doesn't make it a
 * smaller one, so [heightDp] (what's left to draw in) plus [keyboardDp] (what the keyboard covers) is the window
 * the layout is owed. Judged on what's left instead, the unfolded screen looks like a phone's the moment you tap a
 * text field, and the pane holding that field is taken away with the layout it belonged to (#117).
 */
internal fun sizeClassHeightDp(heightDp: Float, keyboardDp: Float): Float = heightDp + keyboardDp

/**
 * The keyboard height to give [sizeClassHeightDp] inside FolioSheet's full-screen page, which is where every sheet
 * that measures its own box is drawn. That page pads its content column by navigationBarsPadding() and only then by
 * WindowInsets.ime, and windowInsetsPadding subtracts what an earlier one already consumed, so the IME padding takes
 * off just the part of the keyboard past the navigation bar.
 *
 * Reading the raw WindowInsets.ime here would add the navigation bar back a second time and make the reconstructed
 * window taller than the real one. A window whose content height sits just under [HOME_REGULAR_MIN_HEIGHT_DP] would
 * then turn regular while a field has focus: #117's bug again, pointing the other way.
 */
@Composable
internal fun keyboardDpOverSheet(): Float = with(LocalDensity.current) {
    WindowInsets.ime.exclude(WindowInsets.navigationBars).getBottom(this).toDp().value
}

/**
 * Whether Settings keeps its list beside the page rather than pushing pages over it. The keyboard is measured out, as
 * in [settingsColumns].
 */
internal fun settingsSplits(widthDp: Float, heightDp: Float, classScale: Float = 1f, keyboardDp: Float = 0f): Boolean =
    fitsRegularHomeLayout(widthDp, sizeClassHeightDp(heightDp, keyboardDp), classScale)
