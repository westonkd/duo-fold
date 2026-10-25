package com.mccal.folio

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A phone that had the Soft or Olive app icon kept Folio's default app entry switched off; starting Folio turns it back on. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherEntryResetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val entry get() = ComponentName(context, "$FOLIO_CLASSES.$FOLIO_SETTINGS_ALIAS")

    @Test fun `an entry an old app icon switched off goes back to the manifest's`() {
        context.packageManager.setComponentEnabledSetting(entry, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        restoreLauncherEntry(context)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, context.packageManager.getComponentEnabledSetting(entry))
    }

    @Test fun `only a switched-off entry is touched`() {
        assertFalse(launcherEntryNeedsReset(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT))
        assertFalse(launcherEntryNeedsReset(PackageManager.COMPONENT_ENABLED_STATE_ENABLED))
        assertTrue(launcherEntryNeedsReset(PackageManager.COMPONENT_ENABLED_STATE_DISABLED))
        assertTrue(launcherEntryNeedsReset(PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER))
    }
}
