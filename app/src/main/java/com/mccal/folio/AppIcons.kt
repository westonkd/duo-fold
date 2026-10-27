package com.mccal.folio

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.asImageBitmap

/** Folio's Kotlin package: class names stay the same in the Folio Dev build, whose app ID ends in ".dev". */
internal const val FOLIO_CLASSES = "com.mccal.folio"

/** Folio's app entry's class name (an activity-alias of MainActivity), so Folio can recognize its own app entry. */
internal const val FOLIO_SETTINGS_ALIAS = "FolioSettingsApp"

/**
 * Whether Folio's app entry was left switched off. Choosing the Soft or Olive app icon, which are gone now, turned the
 * default entry off and theirs on, so a phone that had one has no Folio icon until the default is turned back on.
 */
internal fun launcherEntryNeedsReset(state: Int) =
    state != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED

/** Puts Folio's app entry back to what the manifest says, if an old app icon choice switched it off. */
internal fun restoreLauncherEntry(context: Context) {
    runCatching {
        val entry = ComponentName(context, "$FOLIO_CLASSES.$FOLIO_SETTINGS_ALIAS")
        val pm = context.packageManager
        if (launcherEntryNeedsReset(pm.getComponentEnabledSetting(entry)))
            pm.setComponentEnabledSetting(entry, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP)
    }
}

/**
 * Folio's app icon as Folio draws it in its own screens: the real artwork (never Folio Dev's launcher tint) as an iOS
 * rounded square, drawn from the adaptive layers rather than through the device's icon mask.
 */
internal fun folioIconBitmap(context: android.content.Context, size: Int = 216): androidx.compose.ui.graphics.ImageBitmap? =
    runCatching<androidx.compose.ui.graphics.ImageBitmap> {
        android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = android.graphics.Canvas(bitmap)
            listOf(R.drawable.twofold_icon_background, R.drawable.ic_launcher_foreground).mapNotNull(context::getDrawable).forEach { layer ->
                // Adaptive layers are 108dp with the visible icon in the middle 72dp.
                layer.setBounds(-size / 4, -size / 4, size * 5 / 4, size * 5 / 4); layer.draw(canvas)
            }
        }.asImageBitmap()
    }.getOrNull()
