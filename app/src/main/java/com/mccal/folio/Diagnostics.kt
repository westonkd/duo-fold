package com.mccal.folio

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What happened before a problem, kept only on the phone (shared only if you choose to): Android's own record of why
 * Folio stopped (freezes, native crashes, being closed for memory), a note when the phone restarted while Folio was on
 * screen, and a short trail of recent events. No notifications, app names you haven't chosen, or anything you typed.
 */
internal object Diagnostics {
    private const val PREFS = "diagnostics"
    private const val LAST_EXIT = "lastExitTimestamp"
    private const val BOOT_COUNT = "bootCount"
    private const val LAST_SEEN = "lastSeenWallTime"
    private const val VISIBLE = "visible"
    private const val TRAIL_FILE = "trail.txt"
    private const val TRAIL_SIZE = 40
    /** A restart within this long of Folio last being on screen is worth a note. */
    private const val RESTART_WINDOW_MS = 2 * 60_000L
    private const val TRACE_LIMIT = 48_000

    private val trail = ArrayDeque<String>()
    private val time = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    /** Adds one line to the trail (short, no personal content). */
    @Synchronized fun event(what: String) {
        lastCaught = null // any other line ends a run of repeats
        trail.addLast("${LocalDateTime.now().format(time)}  $what")
        while (trail.size > TRAIL_SIZE) trail.removeFirst()
    }

    @Synchronized fun trailText(): String = trail.joinToString("\n")

    private var lastCaught: String? = null
    private var caughtRepeats = 0

    /**
     * Notes a failure Folio carried on from, in the trail: where it happened and the exception's type. Never its
     * message, which can hold a file path, a package or a name. The same failure again straight after is counted on
     * its line instead of pushing the rest of the trail out.
     *
     * Cancellation is rethrown, not noted: it's a coroutine being stopped on purpose, and swallowing it (as a bare
     * `runCatching` does) leaves work running that was meant to stop.
     */
    @Synchronized fun caught(where: String, error: Throwable) {
        if (error is CancellationException) throw error
        val what = "$where failed: ${error.javaClass.simpleName}"
        if (what == lastCaught && trail.isNotEmpty()) {
            caughtRepeats++
            trail.removeLast()
            trail.addLast("${LocalDateTime.now().format(time)}  $what (×${caughtRepeats + 1})")
        } else {
            event(what)
            lastCaught = what; caughtRepeats = 0
        }
    }

    /** Saves the trail and a heartbeat, so the next start can tell what came before a freeze or restart. */
    fun checkpoint(context: Context, visible: Boolean) {
        // The text is made here, where the trail is; the file write and the settings read happen on a thread of
        // their own, so a slow flash write never lands on a frame (the fold pauses and resumes Home).
        val trail = runCatching { trailText() }.getOrNull() ?: return
        val now = System.currentTimeMillis()
        val app = context.applicationContext
        checkpointWriter.execute {
            runCatching {
                File(CrashLog.dir(app), TRAIL_FILE).writeText(trail)
                app.getSharedPreferences(PREFS, 0).edit().putLong(LAST_SEEN, now)
                    .putBoolean(VISIBLE, visible).putInt(BOOT_COUNT, bootCount(app)).apply()
            }
        }
    }

    private val checkpointWriter = java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "folio-checkpoint").apply { isDaemon = true } }

    private fun bootCount(context: Context) =
        runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrDefault(-1)

    /** Called once when Folio starts: turns anything Android recorded since last time into local reports. */
    fun onStart(context: Context) {
        runCatching { recordExits(context) }
        runCatching { recordRestart(context) }
        event("Folio started")
    }

    private fun previousTrail(context: Context) =
        runCatching { File(CrashLog.dir(context), TRAIL_FILE).readText() }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun recordExits(context: Context) {
        val am = context.getSystemService(ActivityManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, 0)
        val seen = prefs.getLong(LAST_EXIT, 0L)
        val exits = am.getHistoricalProcessExitReasons(context.packageName, 0, 5).filter { it.timestamp > seen }
        if (exits.isEmpty()) return
        prefs.edit().putLong(LAST_EXIT, exits.maxOf { it.timestamp }).apply()
        // Java crashes are already written by the crash handler, with their stack trace.
        exits.filter { it.reason in WORTH_REPORTING }.forEach { exit ->
            // Only a freeze's trace, which is text. A native crash's record is a binary tombstone that can hold pieces
            // of whatever Folio had in memory: unreadable, and not something to put in a report someone emails.
            val trace = if (exit.reason == ApplicationExitInfo.REASON_ANR)
                runCatching { exit.traceInputStream?.bufferedReader()?.use { it.readText().take(TRACE_LIMIT) } }.getOrNull() else null
            CrashLog.save(context, "exit", buildString {
                appendLine("Folio ${label(exit.reason)}")
                appendLine("When: ${format(exit.timestamp)}")
                appendLine(CrashLog.environment(context))
                appendLine("Reason: ${exit.description ?: label(exit.reason)} (status ${exit.status}, importance ${exit.importance})")
                appendLine("Memory: ${exit.pss / 1024} MB PSS, ${exit.rss / 1024} MB RSS")
                previousTrail(context)?.let { appendLine(); appendLine("Before it:"); appendLine(it) }
                trace?.let { appendLine(); appendLine("Android's trace:"); append(it) }
            })
        }
    }

    private fun recordRestart(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val lastBoot = prefs.getInt(BOOT_COUNT, -1)
        val boot = bootCount(context)
        if (lastBoot < 0 || boot < 0 || boot == lastBoot) return
        val bootedAt = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
        val lastSeen = prefs.getLong(LAST_SEEN, 0L)
        // Only when Folio was on screen shortly before the phone went down: that's the kind of restart a stuck
        // screen leads to. Ordinary restarts hours later aren't recorded.
        if (!prefs.getBoolean(VISIBLE, false) || lastSeen <= 0 || bootedAt - lastSeen !in 0..RESTART_WINDOW_MS) return
        CrashLog.save(context, "restart", buildString {
            appendLine("Phone restarted while Folio was on screen")
            appendLine("Folio last seen: ${format(lastSeen)}; phone started again: ${format(bootedAt)}")
            appendLine(CrashLog.environment(context))
            appendLine("Android doesn't tell apps why a phone restarted. If you restarted it because Folio stopped responding, please say so in your report.")
            previousTrail(context)?.let { appendLine(); appendLine("Before it:"); append(it) }
        })
    }

    private fun format(epochMs: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault())
        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    private val WORTH_REPORTING = setOf(ApplicationExitInfo.REASON_ANR, ApplicationExitInfo.REASON_CRASH_NATIVE,
        ApplicationExitInfo.REASON_LOW_MEMORY, ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE, ApplicationExitInfo.REASON_SIGNALED)

    internal fun label(reason: Int) = when (reason) {
        ApplicationExitInfo.REASON_ANR -> "stopped responding (freeze)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "native crash"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "was closed by Android to free memory"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "was closed for using too many resources"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "failed to start"
        ApplicationExitInfo.REASON_SIGNALED -> "was stopped by the system"
        else -> "stopped (reason $reason)"
    }

    /** Screen and setup facts that explain most layout and drawing bugs (no personal data). */
    fun screenSummary(context: Context): String {
        val config = context.resources.configuration
        val metrics = context.resources.displayMetrics
        val state = runCatching {
            org.json.JSONObject(context.getSharedPreferences(SettingKeys.PREFS, 0).getString(SettingKeys.STATE, "{}") ?: "{}")
        }.getOrNull()
        val scale = runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f)
        return listOf(
            "${if (config.fitsRegularHomeLayout()) "unfolded" else "folded"} ${config.screenWidthDp}×${config.screenHeightDp} dp",
            "smallest width ${config.smallestScreenWidthDp} dp",
            "density ${metrics.densityDpi} (default ${android.util.DisplayMetrics.DENSITY_DEVICE_STABLE})",
            "font ${config.fontScale}×",
            "animations ${scale}×",
            "wallpaper ${if (state?.optBoolean("systemWallpaper", false) == true) "Android" else "Folio"}",
            "fold effect ${if (state?.optBoolean("foldEffect", true) != false) "on" else "off"}" +
                " (Duet ${state?.optJSONObject("duet")?.optString("style")?.ifBlank { null } ?: "duo"}," +
                " plays ${state?.optJSONObject("duet")?.optString("direction")?.ifBlank { null } ?: "both"})",
            "page effect ${state?.optString("pageEffect")?.ifBlank { PageEffect.NONE.name } ?: "?"}",
            "safe mode ${if (SafeMode.active) "on" else "off"}",
        ).joinToString(", ")
    }

    /**
     * Every sensor that could be the hinge, and what Folio learned about it: some foldables report only a few fixed
     * positions (the Fold8's public sensor gives 0, 90 and 180), which decides how Duet can follow the fold. The idea
     * of a copyable sensor report is from marcoazeem/duo-open (MIT); this is Folio's own listing, no code from it.
     */
    fun hingeReport(context: Context): String = buildString {
        appendLine("Hinge sensors:")
        val sensors = context.getSystemService(android.hardware.SensorManager::class.java)?.getSensorList(android.hardware.Sensor.TYPE_ALL).orEmpty()
        val hinge = Regex("hinge|angle|fold|posture|flip", RegexOption.IGNORE_CASE)
        val found = sensors.filter { it.type == android.hardware.Sensor.TYPE_HINGE_ANGLE || hinge.containsMatchIn(it.name) || hinge.containsMatchIn(it.stringType) }
        if (found.isEmpty()) appendLine("  (none)")
        found.forEach { s ->
            appendLine("  ${s.name} · ${s.vendor} · ${s.stringType} (type ${s.type})" +
                " · range ${s.maximumRange} · resolution ${s.resolution} · min delay ${s.minDelay} µs" +
                " · ${if (s.isWakeUpSensor) "wake-up" else "non-wake-up"}" +
                (if (s.type == android.hardware.Sensor.TYPE_HINGE_ANGLE && s.resolution >= 45f) " · steps only" else ""))
        }
        val learned = context.getSharedPreferences("folio", 0).getString("fold_hinge_capability", null)
        append("Folio has seen the hinge as: ${learned?.lowercase() ?: "not moved yet"}")
    }

    /**
     * The camera cutout of each screen as Android reports it (#61): the rects Folio wraps the island around, and the
     * bounds of the cutout's own outline, which on a punch hole is the lens. A rect much taller than its outline is
     * what makes the island too tall at the camera.
     */
    fun cutoutReport(context: Context): String = buildString {
        appendLine("Camera cutouts:")
        val displays = context.getSystemService(android.hardware.display.DisplayManager::class.java)?.displays.orEmpty()
        if (displays.isEmpty()) appendLine("  (no displays)")
        displays.forEach { d ->
            val cutout = runCatching { d.cutout }.getOrNull()
            val size = android.graphics.Point().also { @Suppress("DEPRECATION") d.getRealSize(it) }
            append("  display ${d.displayId} ${size.x}×${size.y}: ")
            if (cutout == null) { appendLine("none"); return@forEach }
            append("rects ${cutout.boundingRects.joinToString { it.toShortString() }}")
            append(", safe insets ${cutout.safeInsetLeft} ${cutout.safeInsetTop} ${cutout.safeInsetRight} ${cutout.safeInsetBottom}")
            val bounds = android.graphics.RectF()
            cutout.cutoutPath?.computeBounds(bounds, true)
            append(", outline ${if (bounds.isEmpty) "none" else "[${bounds.left.toInt()},${bounds.top.toInt()}][${bounds.right.toInt()},${bounds.bottom.toInt()}]"}")
            appendLine()
        }
    }.trimEnd()

    /**
     * What Android shares about its wallpaper, and which background Folio puts behind Home (#353). With Android's
     * wallpaper the Big Clock's colors come from the home colors here, then from the system palette when Android shares
     * none; "none" means neither gave Folio any colors.
     */
    fun wallpaperReport(context: Context): String {
        val manager = runCatching { android.app.WallpaperManager.getInstance(context) }.getOrNull()
        fun colors(which: Int) = manager?.let { m -> runCatching { m.getWallpaperColors(which) }.getOrNull() }
            ?.let { c -> listOfNotNull(c.primaryColor.toArgb(), c.secondaryColor?.toArgb(), c.tertiaryColor?.toArgb()) }
        // AOSP's record of where the theme colors come from (a readable setting); One UI may keep its own instead.
        val themeSetting = runCatching { android.provider.Settings.Secure.getString(context.contentResolver, "theme_customization_overlay_packages") }.getOrNull()
        return wallpaperReportText(usesSystemWallpaper(context), runCatching { launcherBackgroundEnabled(context) }.getOrDefault(false),
            colors(android.app.WallpaperManager.FLAG_SYSTEM), colors(android.app.WallpaperManager.FLAG_LOCK),
            systemPalette(context)?.colors, themeColorSource(themeSetting))
    }

    /** The `color_source` in AOSP's theme setting ("home_wallpaper", "lock_wallpaper" or "preset"), or null when not set. */
    internal fun themeColorSource(setting: String?): String? =
        setting?.let { Regex(""""android\.theme\.customization\.color_source"\s*:\s*"([^"]*)"""").find(it)?.groupValues?.get(1) }

    internal fun wallpaperReportText(systemWallpaper: Boolean, folioPictureSaved: Boolean, home: List<Int>?, lock: List<Int>?,
        palette: List<Int>? = null, themeColorSource: String? = null): String = buildString {
        fun text(colors: List<Int>?) = colors?.takeIf { it.isNotEmpty() }?.joinToString(" ") { String.format(java.util.Locale.ROOT, "#%06X", it and 0xFFFFFF) } ?: "none"
        val clockSource = WallpaperTone(primary = home?.firstOrNull(), fromAndroid = systemWallpaper,
            palette = palette?.takeIf { it.size >= 3 }?.let { SystemPalette(it[0], it[1], it[2]) }).colorSource()
        appendLine("Wallpaper:")
        appendLine("  behind Home: ${if (systemWallpaper) "Android's wallpaper" else "Folio's background"}" +
            " (a Folio picture is ${if (folioPictureSaved) "saved" else "not saved"})")
        appendLine("  Android's colors, home: ${text(home)}")
        appendLine("  Android's colors, lock: ${text(lock)}")
        appendLine("  system palette (accent 1, 2, 3 at 500): ${text(palette)}, theme color source: ${themeColorSource ?: "not set"}")
        append("  Big Clock colors: " + if (!systemWallpaper) "from Folio's background" else when (clockSource) {
            WallpaperColorSource.ANDROID_COLORS -> "from Android's wallpaper colors"
            WallpaperColorSource.SYSTEM_PALETTE -> "from the system palette"
            WallpaperColorSource.NONE -> "none"
        })
    }

    fun buildDisplay(): String = "${Build.DISPLAY} (${Build.HARDWARE}, ${Build.SOC_MODEL})"

    /**
     * Everything useful for a bug report in one text: phone and screen, the latest reports, the recent trail and
     * Folio's own log lines (apps can only read their own). Built on demand, shown to you before it goes anywhere.
     */
    fun bundle(context: Context): String = buildString {
        appendLine("Folio diagnostics (${format(System.currentTimeMillis())})")
        appendLine(CrashLog.environment(context))
        appendLine()
        appendLine(hingeReport(context))
        appendLine()
        appendLine(cutoutReport(context))
        appendLine()
        appendLine(wallpaperReport(context))
        appendLine()
        appendLine("Recent events:")
        appendLine(trailText().ifBlank { "(none)" })
        CrashLog.reports(context).take(3).forEach { appendLine(); appendLine("---- ${it.name}"); appendLine(it.readText().take(12_000)) }
        appendLine()
        appendLine("---- Folio log")
        append(ownLog().takeLast(20_000))
    }.take(60_000)

    private fun ownLog(): String = runCatching {
        val process = ProcessBuilder("logcat", "-d", "-t", "400", "-v", "time", "--pid", android.os.Process.myPid().toString())
            .redirectErrorStream(true).start()
        process.inputStream.bufferedReader().use { it.readText() }.also { process.destroy() }
    }.getOrDefault("(log unavailable)")

    /**
     * The bundle, built where it can take its time. It runs logcat and reads report files, and doing that on the
     * main thread from a tap is what froze Folio on a slow phone, usually just when it was already misbehaving.
     */
    suspend fun bundleOffMain(context: Context): String = withContext(Dispatchers.IO) { bundle(context) }

    /**
     * The report as a file, handed to whatever app the person picks, so it arrives as one readable file instead of
     * pages of pasted text.
     */
    private suspend fun reportIntent(context: Context): Intent = withContext(Dispatchers.IO) {
        // Old reports are cleared, but not one an app may still be reading: only those more than ten minutes old,
        // and every report gets a name of its own, so two in the same minute don't overwrite each other.
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val now = System.currentTimeMillis()
        dir.listFiles()?.filter { now - it.lastModified() > 10 * 60_000 }?.forEach { it.delete() }
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"))
        val file = File(dir, "folio-report-$stamp-${(1000..9999).random()}.txt").apply { writeText(bundle(context)) }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.reports", file)
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // The chooser only passes read access on if the file is also in clipData.
        send.clipData = ClipData.newRawUri("", uri)
        Intent.createChooser(send, context.getString(R.string.share_diagnostics))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Opens the report in the share sheet. Returns whether something opened. */
    suspend fun send(context: Context): Boolean = runCatching { context.startActivity(reportIntent(context)) }.isSuccess

    suspend fun copy(context: Context) {
        val text = bundleOffMain(context)
        context.getSystemService(android.content.ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("Folio diagnostics", text))
    }

    fun backgroundSet(choice: BackgroundChoice) = event("Background set: ${choice.save()}")

    /** Home asked for its background and got nothing back from the decoder. */
    fun backgroundUnreadable(choice: BackgroundChoice, width: Int = 0, height: Int = 0) =
        event("Background ${choice.save()} couldn't be decoded" + if (width > 0) " (${width}x$height)" else "")

    /** The background was bigger than Folio draws and was decoded smaller. Expected never to happen. */
    fun backgroundSampled(choice: BackgroundChoice, width: Int, height: Int, sample: Int) =
        event("Background ${choice.save()} is ${width}x$height, decoded at 1/$sample")

    /** A pin request turned away. [why] is one of [PinTrust]'s reasons; the package is the one the request named. */
    fun pinRefused(packageName: String?, why: String) = event("Pin request from ${packageName ?: "an unknown app"} refused: $why")
}

/**
 * [runCatching] that leaves a note: a failure goes in the Diagnostics trail as [where] plus its type, and
 * cancellation is rethrown rather than swallowed. For the places Folio recovers from a failure the user would
 * otherwise never hear about; an expected miss (an optional file that isn't there) stays a plain `runCatching`.
 */
internal inline fun <T> caught(where: String, block: () -> T): Result<T> =
    runCatching(block).onFailure { Diagnostics.caught(where, it) }

/**
 * A named section in a system trace, for Perfetto and Macrobenchmark's TraceSectionMetric. When nothing is tracing
 * it costs a flag check. Android records an app's own sections from a debuggable or profileable build, which is why
 * the fast build is profileable.
 */
internal inline fun <T> traced(section: String, block: () -> T): T {
    androidx.tracing.Trace.beginSection(section)
    try { return block() } finally { androidx.tracing.Trace.endSection() }
}

/**
 * What Folio can reach on this phone, by the tiers in ADR 0008 (docs/adr/0008-platform-architecture.md): A0 needs nothing, A1 is
 * notification access, A2 is Folio's accessibility service.
 */
internal enum class CapabilityTier(val code: String, val risk: OperationRisk) {
    /** Home and folders: every change has an Undo. */
    STANDARD("A0", OperationRisk.REVERSIBLE),
    /** Reads notifications; replying and dismissing one are changes that stay. */
    NOTIFICATIONS("A1", OperationRisk.STATEFUL),
    /** Global gestures and the shade over other apps: they end when you let go. */
    ACCESSIBILITY("A2", OperationRisk.REVERSIBLE),
}

internal data class CapabilityRow(val tier: CapabilityTier, val on: Boolean) {
    /** The most that Folio does with this tier. */
    val risk get() = tier.risk
}

internal object Capabilities {
    /** The rows for this phone: Standard is always on; the other two follow what the person has allowed. */
    fun rows(notificationAccess: Boolean, accessibilityConnected: Boolean): List<CapabilityRow> = listOf(
        CapabilityRow(CapabilityTier.STANDARD, true),
        CapabilityRow(CapabilityTier.NOTIFICATIONS, notificationAccess),
        CapabilityRow(CapabilityTier.ACCESSIBILITY, accessibilityConnected),
    )

    /** Which tiers still need the person's permission, in the order to offer them. */
    fun needsPermission(rows: List<CapabilityRow>) = rows.filter { !it.on }.map { it.tier }
}

/** One line of Folio's own activity trail, read back for the Inspector: when, what, and whether it was a failure Folio carried on from. */
internal data class InspectorEntry(val time: String, val text: String, val failed: Boolean)

internal object Inspector {
    private val line = Regex("""^(\d{2}:\d{2}:\d{2}\.\d{3})\s{2}(.*)$""")

    /** The trail (see [Diagnostics.trailText]) newest first. A line that doesn't look like a trail line is kept as it is, with no time. */
    fun entries(trail: String): List<InspectorEntry> = trail.lineSequence().filter { it.isNotBlank() }.map { raw ->
        line.matchEntire(raw)?.let { m -> InspectorEntry(m.groupValues[1], m.groupValues[2], isFailure(m.groupValues[2])) }
            ?: InspectorEntry("", raw.trim(), isFailure(raw))
    }.toList().asReversed()

    /** [Diagnostics.caught] writes "<where> failed: <Type>"; that is what marks a failure. */
    fun isFailure(text: String) = " failed: " in text || text.startsWith("failed")

    fun filter(entries: List<InspectorEntry>, failedOnly: Boolean) = if (failedOnly) entries.filter { it.failed } else entries
}
