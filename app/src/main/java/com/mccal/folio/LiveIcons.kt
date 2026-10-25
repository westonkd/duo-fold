package com.mccal.folio

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** Which installed apps get a live icon: the phone's calendar apps and clock apps. */
internal object LiveIcons {
    /** Calendar and clock packages, published together so a thread never sees one set without the other. */
    @Volatile private var sets: Pair<Set<String>, Set<String>>? = null

    enum class Kind { CALENDAR, CLOCK }

    fun kind(context: Context, packageName: String): Kind? {
        val (calendar, clock) = sets ?: load(context)
        return when (packageName) {
            in calendar -> Kind.CALENDAR
            in clock -> Kind.CLOCK
            else -> null
        }
    }

    @Synchronized private fun load(context: Context): Pair<Set<String>, Set<String>> {
        sets?.let { return it }
        val pm = context.packageManager
        fun packages(intent: Intent) = runCatching {
            pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
        }.getOrDefault(emptySet())
        val calendar = packages(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)) +
            setOf("com.samsung.android.calendar", "com.google.android.calendar")
        val clock = packages(Intent(AlarmClock.ACTION_SHOW_ALARMS)) +
            setOf("com.sec.android.app.clockpackage", "com.google.android.deskclock")
        return (calendar to clock).also { sets = it }
    }
}

/**
 * One app's own icon: a picture chosen in Edit Icon. Applied when the icon is drawn, never in the saved or cached icon
 * pictures, so changing or clearing it can't leave a stale icon behind.
 */
data class AppIconOverride(val picture: Long = 0L) {
    val isDefault: Boolean get() = picture == 0L

    /** Whether the app has a picture of its own ([picture] is the time it was set, so a new one redraws). */
    val hasPicture: Boolean get() = picture > 0L

    fun toJson(): org.json.JSONObject = org.json.JSONObject().also { o -> if (hasPicture) o.put("picture", picture) }

    companion object {
        fun fromJson(o: org.json.JSONObject?): AppIconOverride =
            AppIconOverride(picture = o?.optLong("picture", 0L)?.coerceAtLeast(0L) ?: 0L)
    }
}

/** [override] for [id] in [map], or no entry at all when it follows the launcher, so the map holds only real choices. */
fun editAppIcon(map: Map<String, AppIconOverride>, id: String, override: AppIconOverride): Map<String, AppIconOverride> =
    if (override.isDefault) map - id else map + (id to override)

fun appIconStylesToJson(map: Map<String, AppIconOverride>): org.json.JSONObject =
    org.json.JSONObject().also { o -> map.forEach { (id, override) -> if (!override.isDefault) o.put(id, override.toJson()) } }

/** Saved pictures back into a map; a damaged entry or one without a picture is dropped. */
fun appIconStylesFromJson(o: org.json.JSONObject?): Map<String, AppIconOverride> =
    o?.keys()?.asSequence()?.mapNotNull { id -> AppIconOverride.fromJson(o.optJSONObject(id)).takeUnless { it.isDefault }?.let { id to it } }?.toMap().orEmpty()

internal val LocalAppIconStyles = androidx.compose.runtime.staticCompositionLocalOf { emptyMap<String, AppIconOverride>() }

enum class BadgeStyle(@androidx.annotation.StringRes val label: Int) { OFF(R.string.off), DOT(R.string.dot), COUNT(R.string.count) }

/** Icon look for the whole launcher, provided from the saved settings. */
internal data class IconLook(val pack: String? = null, val badges: BadgeStyle = BadgeStyle.DOT, val liveIcons: Boolean = true)

/** Unread notification counts per package, for icon badges. */
internal val LocalBadgeCounts = androidx.compose.runtime.compositionLocalOf { emptyMap<String, Int>() }

internal val LocalIconLook = androidx.compose.runtime.staticCompositionLocalOf { IconLook() }
/** Whether most app icons are dark (null until measured), for the live Clock and Calendar icons. */
internal val LocalIconsAreDark = androidx.compose.runtime.staticCompositionLocalOf<Boolean?> { null }

/**
 * Drop-in replacement for an app's icon Image: live Clock and Calendar icons, like iPhone, in Folio's icon shape.
 * [badge] lets the notification badge sit over the corner (Home, dock, folders, App Library).
 */
@Composable
internal fun AppIcon(app: AppEntry, contentDescription: String?, modifier: Modifier = Modifier, badge: Boolean = false,
    badgePlacement: BadgePlacement = BadgePlacement.DEFAULT) {
    val context = LocalContext.current
    val look = LocalIconLook.current
    val override = LocalAppIconStyles.current[app.id]
    val kind = remember(app.component.packageName, look.liveIcons) { if (look.liveIcons) LiveIcons.kind(context, app.component.packageName) else null }
    // Pack lookup: null until it finishes, then the pack's icon or none. An icon pack's own Clock or Calendar icon wins
    // over the live one, so a pack keeps one consistent look; live icons fill in where the pack has nothing.
    val packLookup by androidx.compose.runtime.produceState<PackLookup?>(if (look.pack == null) PackLookup(null) else null, look.pack, app.id) {
        value = PackLookup(look.pack?.let { pack ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { IconPacks.icon(context, pack, app.component, 192) }
        })
    }
    val packIcon = packLookup?.icon
    // Your own picture for this app wins over every other look. Until it loads, the last one drawn at this stamp is used, so it doesn't flash.
    val stamp = override?.picture ?: 0L
    val picture by androidx.compose.runtime.produceState(if (stamp > 0L) AppIconPictures.cached(app.id, stamp) else null, app.id, stamp) {
        value = if (stamp > 0L) AppIconPictures.load(context, app.id, stamp) else null
    }
    val liveKind = kind.takeIf { packLookup != null && packIcon == null && picture == null }
    // A dark system icon theme (like iDark through Theme Park) gets the dark live icon, so Clock and Calendar match the
    // icons around them. It follows the other icons on Home (an icon theme often leaves Calendar and Clock alone),
    // falling back to this app's own icon before those have been measured.
    val iconsAreDark = LocalIconsAreDark.current
    val darkSource = iconsAreDark ?: remember(app.icon) { kind != null && isDarkIcon(app.icon) }
    val palette = if (darkSource) LivePalette.Dark else LivePalette.Light
    val badgeCount = if (!badge || look.badges == BadgeStyle.OFF) 0 else LocalBadgeCounts.current[app.component.packageName] ?: 0
    val wallpaper = LocalWallpaperTone.current.primary
    val lighting = remember(wallpaper) { iconLighting(wallpaper) }
    fun Modifier.lit(fills: Boolean = true): Modifier = if (fills) iconLighting(IconContinuousShape, lighting) else this
    Box(modifier.semantics { contentDescription?.let { this.contentDescription = it } }) {
        val fill = Modifier.fillMaxSize().clip(IconContinuousShape)
        when {
            picture != null -> { val shown = remember(picture) { picture!!.asImageBitmap() }; Image(shown, null, fill.lit()) }
            liveKind == LiveIcons.Kind.CALENDAR -> CalendarIcon(fill.lit(), palette)
            liveKind == LiveIcons.Kind.CLOCK -> ClockIcon(fill.lit(), palette)
            else -> {
                val source = packIcon ?: app.icon
                val bitmap = remember(source) { source.asImageBitmap() }
                val fills = remember(source) { runCatching { fillsEdges(source.width, source.height) { x, y -> android.graphics.Color.alpha(source.getPixel(x, y)) } }.getOrDefault(false) }
                Image(bitmap, null, fill.lit(fills))
            }
        }
        if (badgeCount > 0) IconBadge(badgeCount, look.badges, BadgeRed, badgePlacement)
        // Updating: the icon dims under an iOS-style progress ring until the installer finishes.
        installProgressFor(app.component.packageName)?.let { progress -> InstallRing(progress, fill) }
    }
}

/** iOS download progress over an icon: a dark veil with a white pie filling clockwise inside a thin ring. */
@Composable
internal fun InstallRing(progress: Float, modifier: Modifier) {
    val shown by androidx.compose.animation.core.animateFloatAsState(progress.coerceIn(0f, 1f), label = "install progress")
    val installingText = androidx.compose.ui.res.stringResource(R.string.installing_percent, (progress * 100).toInt())
    Canvas(modifier.semantics { contentDescription = installingText }) {
        drawRect(Color.Black.copy(alpha = .45f))
        val r = size.minDimension * .22f
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(Color.White.copy(alpha = .9f), r, c, style = androidx.compose.ui.graphics.drawscope.Stroke(r * .14f))
        drawArc(Color.White, -90f, 360f * shown, true, Offset(c.x - r * .78f, c.y - r * .78f), androidx.compose.ui.geometry.Size(r * 1.56f, r * 1.56f))
    }
}

@Composable
private fun CalendarIcon(modifier: Modifier, palette: LivePalette) {
    val tick by rememberMinuteTick()
    val today = remember(tick) { LocalDate.now() }
    BoxWithConstraints(modifier.clip(IconContinuousShape).background(palette.calendarBackground), contentAlignment = Alignment.Center) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val small = with(density) { (maxWidth * .17f).toSp() }
        val big = with(density) { (maxWidth * .46f).toSp() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                color = palette.accent, fontSize = small, fontWeight = FontWeight.SemiBold, lineHeight = small * 1.15f)
            Text(today.dayOfMonth.toString(), color = palette.calendarNumber, fontSize = big,
                fontWeight = FontWeight.Light, lineHeight = big * 1.08f)
        }
    }
}

@Composable
private fun ClockIcon(modifier: Modifier, palette: LivePalette) {
    // The tick is read while drawing, so a second redraws the hands without recomposing the icon.
    val tick = rememberSecondTick()
    Box(modifier.clip(IconContinuousShape).background(palette.clockBackground)) {
        Canvas(Modifier.fillMaxSize()) {
            tick.value // subscribes this draw to the second tick
            val now = java.time.LocalTime.now()
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension * .42f
            drawCircle(palette.clockFace, r, c)
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0 - 90).toFloat()
                val inner = if (i % 3 == 0) r * .78f else r * .84f
                drawLine(palette.clockHands.copy(alpha = if (i % 3 == 0) 1f else .6f),
                    Offset(c.x + inner * cos(a), c.y + inner * sin(a)), Offset(c.x + r * .93f * cos(a), c.y + r * .93f * sin(a)),
                    strokeWidth = r * (if (i % 3 == 0) .05f else .03f), cap = StrokeCap.Round)
            }
            fun hand(fraction: Float, length: Float, width: Float, color: Color, tail: Float = .12f) {
                val a = (fraction * 2 * Math.PI - Math.PI / 2).toFloat()
                drawLine(color, Offset(c.x - r * tail * cos(a), c.y - r * tail * sin(a)),
                    Offset(c.x + r * length * cos(a), c.y + r * length * sin(a)), strokeWidth = r * width, cap = StrokeCap.Round)
            }
            val seconds = now.second.toFloat()
            val minutes = now.minute + seconds / 60f
            val hours = (now.hour % 12) + minutes / 60f
            hand(hours / 12f, .5f, .085f, palette.clockHands)
            hand(minutes / 60f, .74f, .06f, palette.clockHands)
            hand(seconds / 60f, .82f, .025f, palette.secondHand, tail = .2f)
            drawCircle(palette.secondHand, r * .06f, c)
        }
    }
}

private class PackLookup(val icon: android.graphics.Bitmap?)

/** Whether an icon is mostly dark: average luminance of its visible pixels, sampled on a coarse grid. */
internal fun isDarkIcon(bitmap: android.graphics.Bitmap): Boolean = runCatching {
    val step = maxOf(1, bitmap.width / 24)
    var sum = 0.0; var count = 0
    for (y in 0 until bitmap.height step step) for (x in 0 until bitmap.width step step) {
        val c = bitmap.getPixel(x, y)
        if (android.graphics.Color.alpha(c) < 200) continue
        sum += (.2126 * android.graphics.Color.red(c) + .7152 * android.graphics.Color.green(c) + .0722 * android.graphics.Color.blue(c)) / 255.0
        count++
    }
    count > 0 && sum / count < .4
}.getOrDefault(false)

/**
 * Live Clock and Calendar colors, like iOS 18: [Light] is the classic light Calendar and Clock, [Dark] the dark-mode
 * versions.
 */
internal data class LivePalette(val calendarBackground: Color, val calendarNumber: Color, val accent: Color,
    val clockBackground: Color, val clockFace: Color, val clockHands: Color, val secondHand: Color) {
    companion object {
        val Light = LivePalette(Color.White, Color.Black, FolioColors.RedLight, Color.Black, Color.White, Color.Black, FolioColors.Orange)
        val Dark = LivePalette(FolioColors.SecondaryBackground, Color.White, FolioColors.Red, FolioColors.SecondaryBackground,
            FolioColors.SheetSurface, Color.White, FolioColors.Orange)
    }
}

internal val BadgeRed = FolioColors.RedLight

/** iOS-style badge: sits over the icon's top-right corner (or where [placement] says), a dot or a pill that widens for 2+ digits. */
@Composable
internal fun androidx.compose.foundation.layout.BoxScope.IconBadge(count: Int, style: BadgeStyle, color: Color,
    placement: BadgePlacement = BadgePlacement.DEFAULT) {
    BoxWithConstraints(Modifier.matchParentSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val h = if (style == BadgeStyle.COUNT) maxWidth * .34f else maxWidth * .22f
        val pill = androidx.compose.foundation.shape.CircleShape
        val readable = if (color.luminance() > .62f) Color.Black.copy(alpha = .85f) else Color.White
        Box(Modifier.align(placement.corner.alignment).offset(placement.offsetX(maxWidth.value, h.value).dp, placement.offsetY(h.value).dp)
            .heightIn(min = h).widthIn(min = h)
            .background(color, pill)
            .padding(horizontal = if (style == BadgeStyle.COUNT && count > 9) h * .22f else 0.dp),
            contentAlignment = Alignment.Center) {
            if (style == BadgeStyle.COUNT) {
                val size = with(density) { (h * .6f).toSp() }
                // The count rolls when it changes.
                RollingCount(count, { if (it > 99) "99+" else it.toString() }, readable, size,
                    androidx.compose.ui.text.TextStyle(lineHeight = size,
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                            androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                            androidx.compose.ui.text.style.LineHeightStyle.Trim.Both)))
            }
        }
    }
}

/**
 * Clear Badge (in an app's long-press menu): hides the badge for the notifications showing now, without dismissing
 * them. A notification posted later brings the badge back, like iOS. Kept in memory only.
 */
internal object BadgeClears {
    /**
     * Keys of the notifications that were showing when Clear Badge was tapped. An app updating one of those (a download
     * progressing, music, a summary) keeps the badge cleared; a new notification brings it back.
     */
    val cleared = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())

    fun clear(packageName: String, items: List<NotificationItem>) {
        val keys = items.filter { it.packageName == packageName }.map { it.key }
        if (keys.isNotEmpty()) cleared.value = cleared.value + keys
    }

    fun counts(items: List<NotificationItem>, cleared: Set<String>): Map<String, Int> =
        items.filter { it.key !in cleared }.groupingBy { it.packageName }.eachCount()
}

/**
 * Clear Badges When Opened (Settings › App Icons): an app's badge goes quiet the moment you open the app, instead of
 * waiting for the app itself to clear it. Opening an app remembers the count that was showing as seen, and the badge
 * stays away while the app is still showing that many notifications or fewer. A new notification takes the count past
 * the seen one, so the badge comes straight back.
 *
 * The seen counts live in [LauncherState.badgesSeen], so they survive a restart; nothing is dismissed, and Folio's
 * notification list is left exactly as the phone reports it.
 *
 * Both functions take the seen counts rather than reading a setting, so the caller turns them off by passing an empty
 * map: with the switch off nothing is hidden, written, or paid for.
 */
internal object BadgesWhenOpened {
    /** [counts] with the apps whose badge has already been seen left out. */
    fun visible(counts: Map<String, Int>, seen: Map<String, Int>): Map<String, Int> =
        if (seen.isEmpty()) counts else counts.filterNot { (packageName, count) -> count <= (seen[packageName] ?: 0) }

    /**
     * The seen counts brought down to what each app is showing now, dropping apps with nothing left. Without this, an
     * app whose notifications are swiped away elsewhere (five seen, two left) would hide its next three as well.
     */
    fun trimmed(counts: Map<String, Int>, seen: Map<String, Int>): Map<String, Int> =
        seen.mapNotNull { (packageName, wasSeen) ->
            (counts[packageName] ?: 0).takeIf { it > 0 }?.let { packageName to minOf(wasSeen, it) }
        }.toMap()
}

/** Most icons dark? Samples up to 40 app icons (skipping Clock and Calendar themselves); null if there aren't enough. */
internal fun iconsMostlyDark(icons: List<android.graphics.Bitmap>): Boolean? {
    val sample = icons.take(40)
    if (sample.size < 4) return null
    return sample.count(::isDarkIcon) * 2 > sample.size
}
