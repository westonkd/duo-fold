package com.mccal.folio

import androidx.compose.ui.graphics.Color

/**
 * Folio's shared colors: Apple's dark-appearance system colors, named once so screens and themes all point at the
 * same values. Use these instead of writing hex colors in a screen.
 */
internal object FolioColors {
    /**
     * The palette as ARGB numbers, for the places that hold a color as a value rather than a `Color`: a tweak's
     * tint, a saved layout's icon tint. One source of truth, so a number and a color can't drift.
     */
    object Value {
        const val Blue = 0xFF0A84FFL
        const val BlueOnDark = 0xFF6CB4FFL
        const val Red = 0xFFFF453AL
        const val RedLight = 0xFFFF3B30L
        const val RedOnDark = 0xFFFF8A80L
        const val RedSoft = 0xFFFF6961L
        const val Green = 0xFF30D158L
        const val GreenLight = 0xFF34C759L
        const val Orange = 0xFFFF9F0AL
        const val Yellow = 0xFFFFD60AL
        const val Indigo = 0xFF5E5CE6L
        const val Purple = 0xFFBF5AF2L
        const val Pink = 0xFFFF375FL
        const val Cyan = 0xFF64D2FFL
        const val CyanLight = 0xFF32ADE6L
        const val Teal = 0xFF30B0C7L
        const val Gray = 0xFF8E8E93L
        const val Warning = 0xFFFFB340L
        const val SecondaryBackground = 0xFF1C1C1EL
        const val SheetSurface = 0xFF2C2C2EL
        const val MenuSurface = 0xFF3A3A3CL
        const val LightBackground = 0xFFF2F2F7L
    }

    val Blue = Color(Value.Blue)
    /** Blue for text and links on a dark surface, where [Blue] is too dark to read (6.19:1 on a sheet). */
    val BlueOnDark = Color(Value.BlueOnDark)
    val Red = Color(Value.Red)
    /** The light-appearance red (destructive text on light surfaces, badges). */
    val RedLight = Color(Value.RedLight)
    /** Destructive text on dark menus and sheets: [Red] measures 3.33 on a menu's grey, this one 4.97 (A11Y-9). */
    val RedOnDark = Color(Value.RedOnDark)
    /** A softer red, for a state that is wrong rather than destructive: a source that failed, a paused profile. */
    val RedSoft = Color(Value.RedSoft)
    val Green = Color(Value.Green)
    /** The light-appearance green (iOS systemGreen): switch tracks and confirmations on light surfaces. */
    val GreenLight = Color(Value.GreenLight)
    val Orange = Color(Value.Orange)
    val Yellow = Color(Value.Yellow)
    val Indigo = Color(Value.Indigo)
    val Purple = Color(Value.Purple)
    val Pink = Color(Value.Pink)
    val Cyan = Color(Value.Cyan)
    /** The light-appearance cyan (iOS systemCyan). */
    val CyanLight = Color(Value.CyanLight)
    /** iOS systemTeal. Folio's accent teal is its own color ([FolioAccents.Teal]); this is the system one. */
    val Teal = Color(Value.Teal)
    val Gray = Color(Value.Gray)
    /**
     * Amber for something that needs attention but isn't destructive: a package Safe Mode turned off, an unsigned
     * file, a setting that is about to change. Status colors never follow the accent (see [LocalAccent]).
     */
    val Warning = Color(Value.Warning)
    /** Grouped background and cards in dark appearance. */
    val SecondaryBackground = Color(Value.SecondaryBackground)
    /** The grey a sheet, an alert and a grouped card sit on. */
    val SheetSurface = Color(Value.SheetSurface)
    /** The grey a pop-up menu sits on, a step lighter than a sheet. */
    val MenuSurface = Color(Value.MenuSurface)
    /** iOS's grouped background in light appearance, for the few Folio surfaces that follow the system theme. */
    val LightBackground = Color(Value.LightBackground)
    /** The darkest container in the sheet scheme, under [SecondaryBackground]. */
    val SurfaceLowest = Color(0xFF141416)
    /** The container glass cards are built on ([FolioGlass.card]), and the sheet scheme's `surfaceContainer`. */
    val SurfaceCard = Color(0xFF242428)
    /** Caption and helper text in the sheet scheme (Material's `onSurfaceVariant`). */
    val LabelVariant = Color(0xFFA1A1A6)
    /** The sheet scheme's strong outline. */
    val Outline = Color(0xFF545458)
    /** Hairline dividers in the sheet scheme. */
    val OutlineVariant = Color(0xFF38383A)
    /**
     * A switch's track when off. It is close to the sheet it sits on (1.3:1), so the thumb's position, not the track,
     * carries the state.
     */
    val TrackOff = Color(0xFF39393D)
    /** The sliding thumb of a segmented control; white text on it is 5.9:1. */
    val SegmentThumb = Color(0xFF636366)
    /** What floating menus and action sheets are drawn on, at 80 to 97% depending on the blur behind. */
    val PopoverSurface = Color(0xFF2A2A2E)
    /** The outline and handle of a widget that can't be dropped where it is: softer than [Red], which is for destructive actions. */
    val InvalidDrop = Color(0xFFFF6B6B)
    /** The bezel drawn around a phone in the Customization previews. */
    val PreviewBezel = Color(0xFF0B0B0C)
    /** Supporting text on dark glass and sheets. */
    val SecondaryLabel = Color.White.copy(alpha = .62f)
}

/**
 * The color Folio uses for the thing you can act on: buttons, switches, selection, links. Two roles, because one
 * color can't do both jobs on a dark surface: [fill] sits behind white text, [ink] is the accent as text.
 */
internal data class FolioAccent(val fill: Color, val ink: Color)

internal object FolioAccents {
    /** Folio's app icon, which ships in teal (#173D43 to #2E5E66), and the soft icon's mint for text. */
    val Teal = FolioAccent(fill = Color(0xFF2A6A70), ink = Color(0xFF6DB7B4))
}

/** The accent in force. */
internal val LocalAccent = androidx.compose.runtime.staticCompositionLocalOf { FolioAccents.Teal }

/**
 * Folio's spacing scale, in dp. These are the gaps Folio already uses; a size off the scale needs a reason in a
 * comment beside it. See docs/standards/design.md (DES-6).
 */
internal object FolioSpace {
    const val HAIR = 2
    const val TINY = 4
    const val SNUG = 6
    const val SMALL = 8
    /** A list row's vertical inset, and the gap either side of a chevron. */
    const val COMPACT = 10
    const val MEDIUM = 12
    /** A sidebar or library row's horizontal inset: 16 crowds a 44 dp icon beside it. */
    const val COMFY = 14
    const val LARGE = 16
    const val XL = 20
    const val XXL = 24
}

/** Folio's corner radii, in dp. Inner corners are concentric: outer radius minus the padding between them (DES-8). */
internal object FolioRadius {
    /** Small controls: chips, search fields, inline buttons. */
    const val CONTROL = 10
    /** Menus, alerts, form sheets and cards. */
    const val CARD = 14
    /** Groups inside a sheet. */
    const val GROUP = 16
    /** Grouped settings cards. */
    const val GROUPED_CARD = 20
    /** Panels and large surfaces. */
    const val PANEL = 24
    /** The top corners of a bottom sheet. */
    const val SHEET_TOP = 28
    /** List rows, search fields and small cards that sit on a sheet. */
    const val ROW = 12
    /** Floating menus and action sheets. */
    const val FLOATING = 18
    /** The dock and the side rail: a capsule. */
    const val RAIL = 30
    /** A segmented control's track. */
    const val SEGMENT = 9
    /** A segmented control's thumb: the track's radius minus its 2 dp inset, so the corners stay concentric (DES-8). */
    const val SEGMENT_THUMB = 7
}

/**
 * Folio's type scale, in sp, following iOS's text styles. Home app labels come from [LabelSize] instead, because they
 * follow the user's icon size.
 */
internal object FolioType {
    const val TITLE = 28
    const val BODY = 17
    const val SUBHEAD = 15
    const val FOOTNOTE = 13
    /** Dense rows, chip labels (SemiBold when selected) and secondary controls. */
    const val COMPACT = 14
    /** iOS Callout: list values and supporting lines a step above [SUBHEAD]. */
    const val CALLOUT = 16
    /** iOS Title 3: sheet and card headings. */
    const val TITLE3 = 20
    /** iOS Caption 2: timestamps and the smallest supporting text. Home app labels use [LabelSize] instead. */
    const val CAPTION = 11
    /** Uppercase group labels, SemiBold with .4sp tracking. */
    const val GROUP_LABEL = 12
}

/**
 * The smallest a finger's target may be, in dp (A11Y-1). What a control *draws* can be smaller: put the drawn thing
 * inside a box this size rather than growing the thing itself, so a 32 dp circle still takes a 48 dp tap.
 */
internal object FolioTouch {
    const val MIN = 48
}

/** Row heights, in dp: iOS's list metrics, and never below the 48dp touch target (A11Y-1). */
internal object FolioRow {
    const val ACTION = 48
    const val NAV = 52
}

/**
 * Folio's shared springs as (damping, stiffness) pairs. Always run them through [MotionSpeed.spring] so they follow
 * Settings › Gestures › Animation Speed; Reduce Motion is handled by the callers.
 */
internal object FolioMotion {
    /** How long the Gauge takes to sweep to a new battery level: long enough to read as movement, short enough to ignore. */
    const val GAUGE_MS = 650

    /** Sheets and panels settling into place. */
    val Settle = .86f to 420f
    /** Quick responses to a touch (buttons, toggles, closing). */
    val Quick = .8f to 700f
    /** Firm, no-bounce snaps (dismissals). */
    val Firm = 1f to 700f
    /** A panel, island or overlay appearing or resizing: a little overshoot, unhurried. */
    val Appear = .78f to 400f
    /** Small controls answering a finger (switch thumbs, segments, a pressed card, a released pull): quick and a touch bouncy. */
    val Control = .75f to 1500f
    /** A value following another with no overshoot (gauges, progress). */
    val Snap = 1f to 1500f

    // The 0.6.9 motion pass (docs/research-motion-0-6-9.md). Damping never goes below 0.7: that is about 4.6% overshoot,
    // the most a settle that starts with the finger's speed should have, and Apple calls more than that exaggerated.
    /** Full pages and panels sliding in from an edge: soft, no visible overshoot, about 0.32 s. */
    val Sheet = .9f to 380f
    /** Menus opening from the pressed object: a hint of life, about 0.25 s. Close them on [Firm]. */
    val Menu = .78f to 650f
    /** A settle that starts with the finger's speed (a flick, a drop): visibly passes the line and eases back. Never for a tap. */
    val Bounce = .7f to 480f

    fun <T> spring(pair: Pair<Float, Float>, visibilityThreshold: T? = null) = MotionSpeed.spring(pair.first, pair.second, visibilityThreshold)

    /** Reduce Motion's stand-in for anything that would slide or grow in: a fade where it lands, this long. */
    const val FADE_MS = 150

    /**
     * Reduce Motion keeps motion that shows a state (a switch thumb, a segment) but takes out the overshoot: the same
     * stiffness at damping 1. Under Android's Remove animations Compose ends it at once anyway.
     */
    fun calm(pair: Pair<Float, Float>, reduce: Boolean) = if (reduce) 1f to pair.second else pair
}
