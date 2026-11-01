package com.mccal.folio

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * iOS-style legibility for things drawn straight on the wallpaper (labels, status, page dots, cards' text):
 * dark ink over light wallpapers, white ink over dark ones.
 */
@Immutable
internal data class HomeInk(val dark: Boolean, /** Chosen from the wallpaper (Automatic) rather than set by hand. */ val automatic: Boolean = false) {
    val primary get() = if (dark) FolioColors.SecondaryBackground else Color.White
    val secondary get() = primary.copy(alpha = if (dark) .65f else .75f)
    val faint get() = primary.copy(alpha = if (dark) .3f else .4f)
    /** Soft shadow for labels: dark under white text, light under dark text. */
    val labelShadow get() = if (dark) Shadow(Color.White.copy(alpha = .45f), Offset(0f, 1f), 3f)
        else Shadow(Color.Black.copy(alpha = .55f), Offset(0f, 1f), 3f)
}

internal val LocalHomeInk = staticCompositionLocalOf { HomeInk(dark = false) }

/** "AUTO", "LIGHT" (white text) or "DARK" (dark text). */
internal fun homeInkFor(setting: String, wallpaperPrefersDarkText: Boolean) = HomeInk(when (setting) {
    "DARK" -> true
    "LIGHT" -> false
    else -> wallpaperPrefersDarkText
}, automatic = setting != "DARK" && setting != "LIGHT")

internal fun WallpaperColors?.prefersDarkText(): Boolean =
    this != null && colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0

/**
 * What Folio knows about Android's wallpaper behind Home, without reading its pixels: the colors Android shares for it,
 * all null when it shares none.
 */
@Immutable
internal data class WallpaperTone(val prefersDarkText: Boolean = false, val primary: Int? = null, val secondary: Int? = null) {
    val luminance: Float get() = primary?.let { Color(it).luminance() } ?: if (prefersDarkText) .75f else .25f
}

internal val LocalWallpaperTone = staticCompositionLocalOf { WallpaperTone() }

internal fun WallpaperColors?.tone() = WallpaperTone(prefersDarkText(),
    this?.primaryColor?.toArgb(), this?.secondaryColor?.toArgb())

/**
 * The middle shade (500, tone 50) of the three accent ranges of Android's system palette (API 31+). Android, or One UI
 * when its Color palette is on, makes these from the wallpaper or from a color the user picked; an app can't always tell which.
 * The middle shade keeps each range's hue without the lightening or darkening of the light and dark theme roles.
 */
@Immutable
internal data class SystemPalette(val accent1: Int, val accent2: Int, val accent3: Int) {
    val colors get() = listOf(accent1, accent2, accent3)
}

internal fun systemPalette(context: android.content.Context): SystemPalette? = runCatching {
    SystemPalette(context.getColor(android.R.color.system_accent1_500), context.getColor(android.R.color.system_accent2_500),
        context.getColor(android.R.color.system_accent3_500))
}.getOrNull()

/** Android's colors for the home screen's wallpaper, or a tone with no colors when it shares none (or the call fails). */
internal fun WallpaperManager.androidWallpaperTone(): WallpaperTone =
    runCatching { getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull().tone()

/** The wallpaper's tone: the system's own colors and hints (no permission needed, updates when the wallpaper changes). */
@Composable
internal fun rememberWallpaperTone(): WallpaperTone {
    val context = LocalContext.current
    val manager = remember(context) { WallpaperManager.getInstance(context) }
    // Read once where it is composed, so the first frame already has the colors; later reads go off the main thread.
    var tone by remember { mutableStateOf(manager.androidWallpaperTone()) }
    val scope = rememberCoroutineScope()
    val refresh = { scope.launch { tone = withContext(Dispatchers.IO) { manager.androidWallpaperTone() } }; Unit }
    DisposableEffect(manager) {
        // Any change reads the home screen's colors again rather than trusting the ones handed over: One UI reports
        // `which` with its own display bits added (5 is home on the main screen), and the colors in a callback can
        // be the lock screen's or the other screen's.
        val listener = WallpaperManager.OnColorsChangedListener { _, _ -> refresh() }
        runCatching { manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper())) }
        onDispose { runCatching { manager.removeOnColorsChangedListener(listener) } }
    }
    // The listener only hears changes. Android can have no colors yet right after boot or while a live wallpaper
    // starts, so Home asks again each time it comes back rather than keeping a first empty answer for good.
    androidx.lifecycle.compose.LifecycleResumeEffect(manager) { refresh(); onPauseOrDispose { } }
    return tone
}

/** Blends [base] toward the wallpaper's main color, keeping [base]'s alpha (iOS-style tinted material). */
internal fun tintedGlass(base: Color, wallpaper: Int?, amount: Float = .28f): Color {
    if (wallpaper == null) return base
    val w = Color(wallpaper)
    return Color(base.red + (w.red - base.red) * amount, base.green + (w.green - base.green) * amount,
        base.blue + (w.blue - base.blue) * amount, base.alpha)
}

internal fun homePalette(base: DuoPalette, wallpaper: Int?): DuoPalette =
    base.copy(glass = tintedGlass(base.glass, wallpaper, GLASS_TINT))

