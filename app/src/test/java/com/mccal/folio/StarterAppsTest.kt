package com.mccal.folio

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StarterAppsTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    private fun install(pkg: String, filter: IntentFilter) {
        val component = ComponentName(pkg, "$pkg.Main")
        shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(component)
            addIntentFilterForActivity(component, filter)
        }
    }

    @Test fun `a phone from another maker fills the dock with its own phone and music apps`() {
        install("com.oem.dialer", IntentFilter(Intent.ACTION_DIAL).apply { addCategory(Intent.CATEGORY_DEFAULT) })
        install("com.oem.music", IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) })
        val starter = StarterApps.resolve(context)
        assertTrue(starter.dock[0].toString(), "com.oem.dialer" in starter.dock[0])
        assertTrue(starter.dock[3].toString(), "com.oem.music" in starter.dock[3])
    }

    @Test fun `known Samsung and Google apps still come first in every slot`() {
        install("com.oem.calendar", IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) })
        val starter = StarterApps.resolve(context)
        StarterApps.KNOWN.dock.zip(starter.dock).forEach { (known, found) -> assertEquals(known, found.take(known.size)) }
        StarterApps.KNOWN.pins.zip(starter.pins).forEach { (known, found) -> assertEquals(known, found.take(known.size)) }
        assertEquals(listOf("com.samsung.android.calendar", "com.google.android.calendar", "com.oem.calendar"), starter.pins[0])
    }
}
