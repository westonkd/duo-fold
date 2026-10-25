package com.mccal.folio

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A notification as shown on badges, the Lock Cover and an app's panel. */
data class NotificationItem(val key: String, val packageName: String, val appLabel: String, val icon: Bitmap?,
    val title: String?, val text: String?, val contentIntent: android.app.PendingIntent?)

data class NowPlaying(val packageName: String, val title: String, val subtitle: String?,
    val icon: Bitmap?, val playing: Boolean, val token: android.media.session.MediaSession.Token,
    /** Album art (scaled down), when the app provides it. */
    val art: Bitmap? = null) {
    /** Excluded from equality: a fresh controller object per query must not look like a change. */
    lateinit var controller: MediaController; internal set
}

/**
 * Reads the active media session and the notifications on the phone. Nothing is stored or sent anywhere; Folio simply
 * mirrors what the system already shows in the shade.
 */
class NotificationListener : NotificationListenerService() {
    private var sessions: MediaSessionManager? = null
    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { publish() }
    // All reading happens off the main thread, coalesced so bursts of posts cost one pass.
    private val worker = android.os.HandlerThread("folio-notifications").apply { start() }
    private val workerHandler = android.os.Handler(worker.looper)
    private val publishPass = Runnable { publishNow() }
    private val controllerCallbacks = mutableMapOf<android.media.session.MediaSession.Token, Pair<MediaController, MediaController.Callback>>()

    override fun onListenerConnected() {
        sessions = getSystemService(MediaSessionManager::class.java)
        runCatching { sessions?.addOnActiveSessionsChangedListener(sessionListener, component(this), workerHandler) }
        publish()
    }

    override fun onListenerDisconnected() {
        runCatching { sessions?.removeOnActiveSessionsChangedListener(sessionListener) }
        clearControllerCallbacks()
        mutable.value = null
        notificationsMutable.value = emptyList() // don't keep other apps' content after access is gone
    }

    /** Set before anything is torn down, so a pass already running on the worker cannot register callbacks afterwards. */
    @Volatile private var destroyed = false

    override fun onDestroy() {
        destroyed = true
        workerHandler.removeCallbacksAndMessages(null)
        clearControllerCallbacks()
        worker.quitSafely()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = publish()
    override fun onNotificationRemoved(sbn: StatusBarNotification) = publish()

    private fun publish() {
        workerHandler.removeCallbacks(publishPass)
        workerHandler.postDelayed(publishPass, PUBLISH_COALESCE_MS)
    }

    private fun publishNow() {
        val controllers = runCatching { sessions?.getActiveSessions(component(this)) }.getOrNull().orEmpty()
        watch(controllers) // every pass, so ended sessions are unregistered
        mutable.value = runCatching { currentMedia(controllers) }.getOrNull()
        notificationsMutable.value = runCatching { currentNotifications() }.getOrDefault(emptyList())
    }

    private fun currentNotifications(): List<NotificationItem> = activeNotifications.orEmpty()
        .filter { sbn ->
            val n = sbn.notification
            sbn.packageName != packageName && n.flags and Notification.FLAG_GROUP_SUMMARY == 0 && !isOverflowPlaceholder(sbn) &&
                (n.extras.getCharSequence(Notification.EXTRA_TITLE) != null || n.extras.getCharSequence(Notification.EXTRA_TEXT) != null)
        }
        .sortedByDescending { it.postTime }
        .map { sbn ->
            val extras = sbn.notification.extras
            NotificationItem(sbn.key, sbn.packageName, appLabel(sbn.packageName), appIcon(sbn.packageName),
                extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString(),
                sbn.notification.contentIntent)
        }

    /** Samsung's System UI posts a "1 more notification" stand-in for its own overflow; it isn't a real notification. */
    private fun isOverflowPlaceholder(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName != "com.android.systemui") return false
        val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: return false
        return OVERFLOW_TITLE.matches(title)
    }

    /**
     * A paused session that nobody comes back to: some players (Spotify among them) keep their session alive long
     * after the app is gone, and Folio was left showing a track whose buttons did nothing. Playing music is
     * always shown; a pause is held for a while, then let go. Reported on r/GalaxyFold, 18 Sep 2026.
     */
    private fun stillWorthShowing(controller: MediaController): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        val since = pausedSince.getOrPut(controller.packageName) { now }
        return now - since < PAUSED_KEEP_MS
    }

    private fun currentMedia(controllers: List<MediaController>): NowPlaying? {
        // A session that has gone away takes its pause clock with it, so a player that comes back paused gets its
        // fifteen minutes again instead of being judged on a pause from an hour ago — and the map can't grow forever.
        pausedSince.keys.retainAll(controllers.map { it.packageName }.toSet())
        // Anything playing is not forgotten, so its pause clock starts again from zero next time it stops.
        controllers.filter { playbackIsLive(it.playbackState?.state) }
            .forEach { pausedSince.remove(it.packageName) }
        val order = controllers.filter { playbackIsLive(it.playbackState?.state) } +
            controllers.filter { it.playbackState?.state == PlaybackState.STATE_PAUSED && stillWorthShowing(it) }
        // One session without a title used to hide a perfectly good one behind it, so each is tried in turn.
        for (active in order) {
            val meta = active.metadata ?: continue
            val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: continue
            val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
            return NowPlaying(active.packageName, title, artist,
                appIcon(active.packageName), playbackIsLive(active.playbackState?.state), active.sessionToken,
                art = albumArt(active.packageName, title, artist, meta))
                .also { it.controller = active }
        }
        return null
    }

    /** Cached per track so each playback update reuses one small bitmap (stable equality, no re-scaling). */
    private fun albumArt(pkg: String, title: String, artist: String?, meta: MediaMetadata): Bitmap? {
        val key = "$pkg|$title|$artist"
        artCache.get(key)?.let { return it }
        val source = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON) ?: return null
        val scaled = runCatching {
            val max = 256
            if (source.width <= max && source.height <= max) source
            else Bitmap.createScaledBitmap(source, max, (max * source.height / source.width.coerceAtLeast(1)).coerceAtLeast(1), true)
        }.getOrNull() ?: return null
        artCache.put(key, scaled)
        return scaled
    }

    private fun watch(controllers: List<MediaController>) {
        val tokens = controllers.map { it.sessionToken }.toSet()
        // The worker thread changes this map and the main thread clears it when access goes, so both hold the same lock.
        synchronized(controllerCallbacks) {
            if (destroyed) return
            controllerCallbacks.keys.filter { it !in tokens }.forEach { token ->
                controllerCallbacks.remove(token)?.let { (controller, callback) -> controller.unregisterCallback(callback) }
            }
            controllers.filter { it.sessionToken !in controllerCallbacks }.forEach { controller ->
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                    override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                }
                controller.registerCallback(callback, workerHandler)
                controllerCallbacks[controller.sessionToken] = controller to callback
            }
        }
    }

    private fun clearControllerCallbacks() = synchronized(controllerCallbacks) {
        controllerCallbacks.values.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        controllerCallbacks.clear()
    }

    private fun appIcon(pkg: String): Bitmap? = iconCache.get(pkg) ?: loadIcon(pkg)?.also { iconCache.put(pkg, it) }
    private fun loadIcon(pkg: String): Bitmap? = run {
        runCatching { packageManager.getApplicationIcon(pkg).toBitmap(96, 96) }.getOrNull()
    }

    private fun appLabel(pkg: String): String = labelCache.get(pkg) ?: runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg).also { labelCache.put(pkg, it) }

    companion object {
        private val notificationsMutable = MutableStateFlow<List<NotificationItem>>(emptyList())
        val notifications: StateFlow<List<NotificationItem>> = notificationsMutable.asStateFlow()

        fun openNotification(context: Context, item: NotificationItem) {
            val sent = sendAllowingLaunch(context, item.contentIntent)
            if (!sent) context.packageManager.getLaunchIntentForPackage(item.packageName)
                ?.let { runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
        }
        private val mutable = MutableStateFlow<NowPlaying?>(null)
        val media: StateFlow<NowPlaying?> = mutable.asStateFlow()
        private const val PUBLISH_COALESCE_MS = 120L
        private val iconCache = android.util.LruCache<String, Bitmap>(64)
        private val artCache = android.util.LruCache<String, Bitmap>(8)
        /** When each app's playback was first seen paused, so a forgotten session doesn't stay up forever. */
        private val pausedSince = mutableMapOf<String, Long>()
        /** How long a paused track stays up: long enough to come back to, short enough not to be clutter. */
        private const val PAUSED_KEEP_MS = 15 * 60 * 1000L

        /**
         * Playback that is running, not stopped: buffering and connecting count, because a track that pauses to load
         * would otherwise drop out and reappear a second later, once on every skip.
         */
        internal fun playbackIsLive(state: Int?) = state == PlaybackState.STATE_PLAYING ||
            state == PlaybackState.STATE_BUFFERING || state == PlaybackState.STATE_CONNECTING
        private val OVERFLOW_TITLE = Regex("^\\d+ more notifications?$", RegexOption.IGNORE_CASE)
        private val labelCache = android.util.LruCache<String, String>(128)

        /** Sends another app's PendingIntent so it may open even though Folio's window isn't in front (Android 14+ rule). */
        private fun sendAllowingLaunch(context: Context, intent: android.app.PendingIntent?): Boolean = intent != null && runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                val options = android.app.ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED).toBundle()
                intent.send(context, 0, null, null, null, null, options)
            } else intent.send()
            true
        }.getOrDefault(false)

        fun component(context: Context) = ComponentName(context, NotificationListener::class.java)

        fun hasAccess(context: Context): Boolean =
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.contains(component(context).flattenToString()) == true

        fun accessSettingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component(context).flattenToString())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
