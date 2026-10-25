package com.mccal.folio

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Messaging helpers: opening a conversation in OpenBubbles or BlueBubbles (iMessage on Android).
 * Folio never talks to those apps' servers; it only uses what Android already exposes.
 */
internal object Messaging {
    const val OPENBUBBLES = "com.openbubbles.messaging"
    const val BLUEBUBBLES = "com.bluebubbles.messaging"

    /** Messaging apps Folio can open a conversation in, besides the default texting app. */
    val iMessageApps = listOf(OPENBUBBLES to "OpenBubbles", BLUEBUBBLES to "BlueBubbles")

    fun installed(context: Context, pkg: String) = runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess

    /**
     * Opens a conversation with [address] (phone number or email). [appPackage] null uses the default
     * texting app; OpenBubbles/BlueBubbles get an `imessage:` link, or simply open if they don't take it.
     */
    fun conversationIntent(context: Context, appPackage: String?, address: String): Intent? {
        val pm = context.packageManager
        if (appPackage == null) return Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", address, null))
        val link = Intent(Intent.ACTION_VIEW, Uri.parse("imessage:${Uri.encode(address, "+@.")}")).setPackage(appPackage)
        return if (link.resolveActivity(pm) != null) link else pm.getLaunchIntentForPackage(appPackage)
    }
}
