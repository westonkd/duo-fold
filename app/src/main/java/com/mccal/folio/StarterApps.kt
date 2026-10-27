package com.mccal.folio

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager

internal data class StarterApps(val dock: List<List<String>>, val pins: List<List<String>>) {
    private class Slot(val known: List<String>, val onThisPhone: SystemHandlers.() -> List<String> = { emptyList() }) {
        constructor(vararg known: String, onThisPhone: SystemHandlers.() -> List<String> = { emptyList() }) : this(known.toList(), onThisPhone)
    }

    companion object {
        private val DOCK = listOf(
            Slot("com.samsung.android.dialer", "com.google.android.dialer") { defaultDialer() + of(Intent(Intent.ACTION_DIAL)) },
            Slot("com.android.chrome", "com.sec.android.app.sbrowser") {
                of(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)) + app(Intent.CATEGORY_APP_BROWSER)
            },
            Slot("com.google.android.apps.messaging", "com.samsung.android.messaging") { defaultSms() + app(Intent.CATEGORY_APP_MESSAGING) },
            Slot("com.spotify.music", "com.google.android.apps.youtube.music") { app(Intent.CATEGORY_APP_MUSIC) },
        )

        private val PINS = listOf(
            Slot("com.samsung.android.calendar", "com.google.android.calendar") { app(Intent.CATEGORY_APP_CALENDAR) },
            Slot("com.sec.android.app.camera", "com.android.camera2", "com.google.android.GoogleCamera") { of(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) },
            Slot("com.sec.android.gallery3d", "com.google.android.apps.photos") { app(Intent.CATEGORY_APP_GALLERY) },
            Slot("com.sec.android.app.clockpackage", "com.google.android.deskclock") { of(Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
            Slot("com.google.android.gm", "com.samsung.android.email.provider") { app(Intent.CATEGORY_APP_EMAIL) },
            Slot("com.google.android.apps.maps") { app(Intent.CATEGORY_APP_MAPS) },
            Slot("com.samsung.android.app.notes", "com.google.android.keep"),
            Slot("com.sec.android.app.myfiles", "com.google.android.documentsui") { app(Intent.CATEGORY_APP_FILES) },
            Slot("com.android.settings") { of(Intent(Settings.ACTION_SETTINGS)) },
            Slot("com.sec.android.app.popupcalculator", "com.google.android.calculator") { app(Intent.CATEGORY_APP_CALCULATOR) },
            Slot("com.android.vending"),
            Slot("com.google.android.youtube"),
            Slot("com.samsung.android.app.contacts", "com.google.android.contacts") { app(Intent.CATEGORY_APP_CONTACTS) },
            Slot("com.google.android.apps.docs"),
            Slot("com.google.android.apps.walletnfcrel"),
            Slot("com.sec.android.app.shealth") { app(Intent.CATEGORY_APP_FITNESS) },
        )

        val KNOWN = StarterApps(DOCK.map(Slot::known), PINS.map(Slot::known))

        fun resolve(context: Context): StarterApps {
            val handlers = SystemHandlers(context)
            fun List<Slot>.candidates() = map { slot -> (slot.known + slot.onThisPhone(handlers)).distinct() }
            return StarterApps(DOCK.candidates(), PINS.candidates())
        }
    }
}

internal class SystemHandlers(private val context: Context) {
    private val pm = context.packageManager

    fun of(intent: Intent): List<String> = runCatching {
        val preferred = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        listOfNotNull(preferred?.takeIf { it != "android" }) +
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo.packageName }
    }.getOrDefault(emptyList())

    fun app(category: String): List<String> = runCatching {
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0).map { it.activityInfo.packageName }
    }.getOrDefault(emptyList())

    fun defaultDialer(): List<String> =
        listOfNotNull(runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull())

    fun defaultSms(): List<String> = listOfNotNull(runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull())
}
