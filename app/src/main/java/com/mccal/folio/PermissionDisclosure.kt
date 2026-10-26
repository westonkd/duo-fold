package com.mccal.folio

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal const val PRIVACY_POLICY_URL = "https://github.com/westonkd/duo-fold/blob/main/PRIVACY.md"

internal data class FolioPrompt(
    val tag: String,
    val title: String,
    val message: String,
    val confirm: String,
    val dismiss: String,
    val onConfirm: () -> Unit,
    val neutral: String? = null,
    val onNeutral: () -> Unit = {},
)

internal object FolioPrompts {
    val current = mutableStateOf<FolioPrompt?>(null)
    fun show(prompt: FolioPrompt) { current.value = prompt }
    fun showing(tag: String) = current.value?.tag == tag
    fun close() { current.value = null }
}

@Composable
internal fun FolioPromptHost() {
    val prompt = FolioPrompts.current.value ?: return
    AlertDialog(onDismissRequest = FolioPrompts::close, modifier = Modifier.testTag("prompt-${prompt.tag}"), width = 320.dp,
        title = { Text(prompt.title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(prompt.message, textAlign = TextAlign.Start) } },
        confirmButton = { TextButton(onClick = { FolioPrompts.close(); prompt.onConfirm() }, Modifier.testTag("prompt-confirm")) { Text(prompt.confirm) } },
        dismissButton = { TextButton(onClick = FolioPrompts::close, Modifier.testTag("prompt-dismiss")) { Text(prompt.dismiss) } },
        neutralButton = prompt.neutral?.let { label -> { TextButton(onClick = { FolioPrompts.close(); prompt.onNeutral() }) { Text(label) } } })
}

internal object PermissionDisclosure {
    fun notificationAccess(context: Context) =
        confirm(context, "notification-access", R.string.notification_access_disclosure_title, R.string.notification_access_disclosure) {
            listOf(NotificationListener.accessSettingsIntent(context), Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

    fun usageAccess(context: Context) =
        confirm(context, "usage-access", R.string.usage_access_disclosure_title, R.string.usage_access_disclosure) {
            listOf(Suggestions.usageAccessIntent(context), Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

    private fun confirm(context: Context, tag: String, title: Int, message: Int, settings: () -> List<Intent>) {
        FolioPrompts.show(FolioPrompt(tag, context.getString(title), context.getString(message),
            confirm = context.getString(R.string.agree), dismiss = context.getString(R.string.not_now),
            onConfirm = { openFirstAvailable(context, settings()) }))
    }

    private fun openFirstAvailable(context: Context, intents: List<Intent>) {
        intents.any { intent ->
            runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        }
    }
}

internal object GesturesSetupRequest {
    @Volatile private var pending = false
    fun request() { pending = true }
    fun consume(): Boolean = pending.also { pending = false }
}
