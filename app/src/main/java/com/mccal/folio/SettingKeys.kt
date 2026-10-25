package com.mccal.folio

/** JSON keys in the saved launcher state that the everywhere overlay (another component) also reads. */
internal object SettingKeys {
    const val PREFS = "launcher"
    const val STATE = "state"
    /** Whether Home's window shows Android's wallpaper, saved on its own and at once (see applyWallpaperWindow). */
    const val SYSTEM_WALLPAPER = "systemWallpaperWindow"
    /** Clear Suggestions History: uses at or before this time are ignored, a plain long beside the state so no schema changes. */
    const val SUGGESTIONS_CLEARED_AT = "suggestionsClearedAt"
    const val DOCK = "dock"
    const val LEFT_HANDED = "leftHanded"
    const val DOCK_EVERYWHERE = "dockEverywhere"
    /** Buttons in Every App: a large Back / Home / Recents bar drawn over other apps. */
    const val BUTTON_BAR = "buttonBar"
}
