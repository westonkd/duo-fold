package com.mccal.folio

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.conflate

/**
 * With Android's wallpaper behind Home, tell the wallpaper which page is showing so it can shift a little
 * as pages change (the parallax iPhone and most launchers have). Draws nothing.
 *
 * **Why a still picture is moved less than the pages it follows.** A live wallpaper draws itself as wide as it
 * likes, so it can afford the full travel. A still one is a fixed image the system has cropped to the screen, and
 * there is no spare width to slide: asking for the whole travel across two pages slides it twice as far as the
 * picture can go, which the system absorbs by squashing the movement into whatever margin it happens to have.
 * AOSP's answer, in `WallpaperOffsetInterpolator`, is [MIN_PARALLAX_SPAN]: spread a still wallpaper's parallax over
 * at least four pages however few there really are, and use the true count only when the wallpaper is live. Folio
 * asked for the true count either way, so on a two page Home it moved Android's wallpaper twice as far as Android's
 * own launcher would.
 */
@Composable
internal fun SystemWallpaperParallax(pager: androidx.compose.foundation.pager.PagerState) {
    val view = androidx.compose.ui.platform.LocalView.current
    val manager = androidx.compose.runtime.remember(view) { android.app.WallpaperManager.getInstance(view.context) }
    androidx.compose.runtime.LaunchedEffect(pager, view) {
        // getWallpaperInfo() is a binder call, so it is read once here rather than on every page of every swipe.
        // A wallpaper swapped while Home is open keeps the old span until Home is next composed, which costs a
        // slightly wrong drift and nothing else.
        val live = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            runCatching { manager.wallpaperInfo }.getOrNull() != null
        }
        // The offset call is a binder round-trip: keep it off the UI thread and drop stale positions.
        androidx.compose.runtime.snapshotFlow { pager.currentPage + pager.currentPageOffsetFraction to pager.pageCount }
            .conflate()
            .collect { (position, count) ->
                val token = view.windowToken ?: return@collect
                val pages = if (live) count else maxOf(MIN_PARALLAX_SPAN, count)
                val steps = (pages - 1).coerceAtLeast(1)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    runCatching {
                        // yStep is 1f, not 0f: a wallpaper that works out its vertical pixel offset from the step
                        // divides by it, and Folio was handing it a zero to divide by while still asking for the
                        // middle of the vertical range below. AOSP passes 1f here for the same pair of values.
                        manager.setWallpaperOffsetSteps(1f / steps, 1f)
                        manager.setWallpaperOffsets(token, (position / steps).coerceIn(0f, 1f), .5f)
                    }
                }
            }
    }
}

/**
 * The fewest pages a still wallpaper's parallax is spread over, matching `MIN_PARALLAX_PAGE_SPAN` in AOSP's
 * `WallpaperOffsetInterpolator`. See [SystemWallpaperParallax] for why a still picture gets less travel.
 */
private const val MIN_PARALLAX_SPAN = 4

/**
 * The activity behind a context. Compose inside a sheet or dialog hands out a wrapper rather than the activity, so
 * `context as? Activity` there is null and whatever it guarded silently never happens.
 */
internal tailrec fun android.content.Context.asActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.asActivity()
    else -> null
}

/** Whether this window actually shows Android's wallpaper behind it. */
internal val android.app.Activity.showsWallpaper: Boolean
    get() = window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER != 0

internal fun forgetFolioBackground(context: android.content.Context) {
    val prefs = context.getSharedPreferences("launcher_background", android.content.Context.MODE_PRIVATE)
    prefs.getString("previewGrantUri", null)?.let { uri ->
        runCatching {
            context.contentResolver.releasePersistableUriPermission(android.net.Uri.parse(uri),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.filesDir.listFiles { file -> file.name.startsWith("launcher-background.jpg") }?.forEach(java.io.File::delete)
    context.deleteSharedPreferences("launcher_background")
    context.getSharedPreferences(SettingKeys.PREFS, 0).edit().remove("systemWallpaperWindow").apply()
}
