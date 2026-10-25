package com.mccal.folio

import android.app.Application

class DuoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        restoreLauncherEntry(this)
    }

    /**
     * Give the background picture back when Home is not on screen.
     *
     * A launcher wants to stay alive, and the surest way to be chosen for death is to be the process holding the
     * most memory. The decoded background is the largest single thing Folio keeps: several megabytes today, and more
     * once it is stored at a size that does not have to be upscaled on the inner screen. Held forever, it counts
     * against Folio every time Android goes looking for something to kill, including while the user is in another
     * app and Folio is drawing nothing at all.
     *
     * **Only once the UI is hidden**, which is what [TRIM_MEMORY_UI_HIDDEN] means and why every level at or above it
     * qualifies. Dropping it while Home is visible would be worse than keeping it: the next frame asks for the
     * background again, decodes the same file, and arrives back where it started having flashed an empty Home on the
     * way. So the tighter foreground levels are deliberately not handled here.
     *
     * Nothing is recycled, only let go of. A canvas or a RenderThread frame may still be drawing this bitmap, and
     * [LauncherBackgroundCache] leaves collection to the garbage collector for that reason.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) LauncherBackgroundCache.changed(null)
    }
}

