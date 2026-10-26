package com.mccal.folio

import android.content.Context
import android.content.Intent

internal const val PRIVACY_POLICY_URL = "https://github.com/westonkd/duo-fold/blob/main/PRIVACY.md"

internal object PermissionDisclosure {
    fun notificationAccess(context: Context) =
        confirm(context, R.string.notification_access_disclosure_title, R.string.notification_access_disclosure) {
            NotificationListener.accessSettingsIntent(context)
        }

    fun usageAccess(context: Context) =
        confirm(context, R.string.usage_access_disclosure_title, R.string.usage_access_disclosure) {
            Suggestions.usageAccessIntent(context)
        }

    private fun confirm(context: Context, title: Int, message: Int, settings: () -> Intent) {
        android.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.not_now, null)
            .setPositiveButton(R.string.agree) { _, _ ->
                runCatching { context.startActivity(settings().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            .show()
    }
}

internal object GesturesSetupRequest {
    @Volatile private var pending = false
    fun request() { pending = true }
    fun consume(): Boolean = pending.also { pending = false }
}
