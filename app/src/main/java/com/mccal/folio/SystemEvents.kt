package com.mccal.folio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * The Charging trigger's moment: the cable going in. Plugged in counts as charging, as it does
 * for the Side Bar and StandBy, so a phone held at a charge limit (Samsung's battery protection) fires it on plug-in,
 * and the battery topping back up at the limit doesn't fire it again. The first reading after Folio starts listening
 * is the state it was already in, not news.
 */
internal fun startsCharging(wasCharging: Boolean?, status: Int, plugged: Int): Boolean = wasCharging == false && isCharging(status, plugged)

/** Listens for system moments (charger, wired headphones, Bluetooth audio) and runs their triggers. */
class SystemEvents private constructor(private val context: Context) {
    private var registered = false
    private var lastCharging: Boolean? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                    if (startsCharging(lastCharging, status, plugged)) FolioActions.onTrigger(c, FolioTrigger.CHARGING)
                    lastCharging = isCharging(status, plugged)
                }
                AudioManager.ACTION_HEADSET_PLUG -> {
                    // The sticky state delivered on registration isn't a new plug-in.
                    if (!isInitialStickyBroadcast && intent.getIntExtra("state", 0) == 1) FolioActions.onTrigger(c, FolioTrigger.HEADPHONES)
                }
            }
        }
    }

    // Bluetooth headphones and speakers come from the audio device list, which carries the device's name and needs no
    // Bluetooth permission. One device can appear as several outputs (media and calls), so each name counts once.
    private val audio = context.getSystemService(AudioManager::class.java)
    private val knownOutputs = mutableSetOf<Int>()
    private var lastAudioName: String? = null
    private var lastAudioAt = 0L
    private val audioCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) {
            added.filter { it.isSink && knownOutputs.add(it.id) }.firstOrNull { it.type in BLUETOOTH_OUTPUTS }?.let { device ->
                val name = device.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                val now = System.currentTimeMillis()
                if (name != null && name == lastAudioName && now - lastAudioAt < 5_000) return
                lastAudioName = name; lastAudioAt = now
                FolioActions.onTrigger(context, FolioTrigger.BLUETOOTH)
            }
        }
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) { removed.forEach { knownOutputs.remove(it.id) } }
    }

    private fun register() {
        if (registered) return
        // Outputs already connected aren't news; only ones added after this count.
        audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapTo(knownOutputs) { it.id }
        audio.registerAudioDeviceCallback(audioCallback, Handler(Looper.getMainLooper()))
        ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
        }, ContextCompat.RECEIVER_NOT_EXPORTED) // all protected system broadcasts
        registered = true
    }

    private fun unregister() {
        if (registered) { runCatching { context.unregisterReceiver(receiver) }; audio.unregisterAudioDeviceCallback(audioCallback) }
        registered = false
        knownOutputs.clear()
        lastCharging = null
    }

    companion object {
        private val BLUETOOTH_OUTPUTS = setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER)
        @android.annotation.SuppressLint("StaticFieldLeak") // holds only the application context // english-only
        private var shared: SystemEvents? = null
        private var users = 0

        /** Reference-counted so Home and the everywhere overlay share one registration. */
        @Synchronized fun acquire(context: Context) {
            if (users++ == 0) shared = SystemEvents(context.applicationContext).also { it.register() }
        }
        @Synchronized fun release() {
            if (users > 0 && --users == 0) { shared?.unregister(); shared = null }
        }
    }

    /** Lifecycle-bound acquire/release for an activity. */
    class Observer(private val context: Context) : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = acquire(context)
        override fun onStop(owner: LifecycleOwner) = release()
    }
}
