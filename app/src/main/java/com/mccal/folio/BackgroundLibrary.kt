package com.mccal.folio

import android.content.Context
import androidx.core.content.edit

/*
 * The pictures Folio can put behind Home, and which one is chosen.
 *
 * **One picture, from one of two places.** A background is either a photo the user picked or a piece of art from
 * Folio's library, which holds art that shipped inside the app. Nothing here draws anything. Folio draws a background
 * the way it always has, through `drawLauncherBackground`, and this decides which picture that is.
 *
 * **Why art is not a second kind of background.** Before this, Folio's own backgrounds were code that drew a scene,
 * so they needed their own switch, their own settings and their own draw path beside the photo one. Under
 * [DES-2a](../../../../../../docs/standards/design.md) they are pictures instead, which means a piece of art and the
 * user's own photo differ only in where the file came from. So they share the decode, the cache, the scrim, the
 * crop and the parallax, and the picker is one grid rather than two lists that behave differently.
 *
 * **Who owns this.** The choice lives in the `launcher_background` preferences beside the photo that was already
 * there, and `LauncherBackgroundController` remains its only writer (STA-1). That file answers "which picture";
 * `LauncherState.systemWallpaper` answers the different question of whether Folio draws a background at all or lets
 * Android's own wallpaper show through the window. Keeping those two apart is deliberate: one is a choice of
 * picture, the other is a choice of who paints. `docs/standards/state-data.md` Gap 6 has the history, which is that
 * this used to be spread over three places and was about to become four.
 */

/** Where the picture behind Home comes from. */
internal sealed interface BackgroundChoice {
    /** Nothing of Folio's: either Android's wallpaper shows through, or Home is bare. */
    data object None : BackgroundChoice

    /** The one photo the user picked, in [launcherPhotoFile]. */
    data object Photo : BackgroundChoice

    /** A piece of art from the library, by its [Artwork.id]. */
    data class Art(val id: String) : BackgroundChoice

    companion object {
        /**
         * Reads a choice back from what was saved.
         *
         * An id that no longer names anything in the library is [None] rather than an error. Art can leave: a piece
         * that shipped inside the app is retired in a later version. When that happens the background should quietly
         * go away, not crash the launcher on the first frame after an
         * update (STA-6 is about failing visibly for the user's *layout*; a missing picture has nothing to tell
         * them and everything to recover from).
         */
        fun parse(saved: String?, known: (String) -> Boolean): BackgroundChoice = when {
            saved == null || saved == NONE -> None
            saved == PHOTO -> Photo
            saved.startsWith(ART_PREFIX) -> saved.removePrefix(ART_PREFIX)
                .takeIf(known)?.let(::Art) ?: None
            else -> None
        }

        const val NONE = "none"
        const val PHOTO = "photo"
        const val ART_PREFIX = "art:"
    }

    /** How this is written to preferences. Stable across versions: it is saved on the user's phone. */
    fun save(): String = when (this) {
        None -> NONE
        Photo -> PHOTO
        is Art -> ART_PREFIX + id
    }
}

/**
 * A picture with its credit.
 *
 * Every field except [id] is the credit [DES-2b](../../../../../../docs/standards/design.md) requires.
 *
 * The strings are not translated. A painting's title, its painter's name and the name of the museum holding it are
 * the same in every language, and translating "Public domain" into a claim Folio has not checked in that language
 * would be worse than leaving it.
 */
internal data class Artwork(
    val id: String,
    val title: String,
    val artist: String,
    val license: String,
    /** Date and holding collection, as one line: "1785. Yale Center for British Art." Blank when unknown. */
    val detail: String = "",
    /** Where the file came from, for the credit to link to. */
    val source: String = "",
)

internal object BackgroundLibrary {
    /**
     * Art that shipped inside the app.
     *
     * Two woodblock prints, both by an artist who died in 1858, both from a library that publishes its scans with no
     * restriction. They are stored under `assets/wallpapers` rather than `res/`, so no density qualifier applies to
     * them and the file is the same one on every phone, and they are cropped off their paper mounts and sized to
     * 2448 px wide, which is the widest window Folio runs in. That width is the point: a center crop never has to
     * scale them up.
     *
     * The credit is written out here rather than read from a file because these ship with the app, so there is no
     * later moment at which it could be missing, and a constant is the one form of it that cannot go stale or fail
     * to parse.
     *
     * Each credit line is marked `// english-only`, which is why it is not in `strings.xml`. A painting's title, its
     * painter's name and the museum holding it are the same in every language, and "Public domain" translated into a
     * language nobody checked would be Folio making a legal claim it has not verified. The words around them in the
     * picker are translated as usual; these four fields are the work's own name for itself.
     */
    fun builtIn(): List<Artwork> = BUILT_IN

    private val BUILT_IN = listOf(
        Artwork(
            id = "night-view-of-saruwaka-machi",
            title = "Night View of Saruwaka-machi", // english-only
            artist = "Utagawa Hiroshige", // english-only
            license = "Public domain", // english-only
            detail = "1856. Library of Congress.", // english-only
            source = "https://www.loc.gov/pictures/item/2008660961/",
        ),
        Artwork(
            id = "naruto-whirlpools",
            title = "Naruto Whirlpools", // english-only
            artist = "Utagawa Hiroshige", // english-only
            license = "Public domain", // english-only
            detail = "1855. National Library of New Zealand.", // english-only
            source = "https://natlib.govt.nz/records/22811404",
        ),
    )

    /** The art inside the app, by id. Read by the test that holds each piece to its credit and its picture. */
    val builtInIds: List<String> get() = BUILT_IN.map { it.id }

    fun isBuiltIn(id: String) = BUILT_IN.any { it.id == id }

    /** Where a built-in piece lives inside the app. */
    fun assetPath(id: String) = "wallpapers/$id.webp"

    /**
     * The largest picture Folio decodes as it is: a little over the art it ships (2448 x 3796), and well under the size
     * Android refuses to draw.
     */
    const val MAX_ART_EDGE = 4096
    const val MAX_ART_PIXELS = 12_000_000

    /** The power-of-two step that brings a picture inside the limits when it is decoded. */
    fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > MAX_ART_EDGE ||
            (width.toLong() / sample) * (height / sample) > MAX_ART_PIXELS
        ) sample *= 2
        return sample
    }
}

/** The key holding [BackgroundChoice], in the same preferences the picked photo already used. */
private const val BACKGROUND_CHOICE = "backgroundChoice"

/**
 * Which picture is behind Home.
 *
 * **The migration.** Before this key existed, the only background was a photo and a single boolean said whether it
 * was showing. A phone updating into this version has that boolean and no choice, so the boolean is read once and
 * written forward as [BackgroundChoice.Photo] (STA-5). The old key is left alone rather than deleted, so a downgrade
 * to the previous version still finds the photo it expects.
 */
internal fun backgroundChoice(context: Context): BackgroundChoice {
    val prefs = launcherBackgroundPreferences(context)
    val saved = prefs.getString(BACKGROUND_CHOICE, null)
    if (saved == null) {
        val hadPhoto = prefs.getBoolean("photoEnabled", false) && launcherPhotoFile(context).isFile
        val migrated = if (hadPhoto) BackgroundChoice.Photo else BackgroundChoice.None
        prefs.edit { putString(BACKGROUND_CHOICE, migrated.save()) }
        return migrated
    }
    return BackgroundChoice.parse(saved, BackgroundLibrary::isBuiltIn)
}

internal fun setBackgroundChoice(context: Context, choice: BackgroundChoice) {
    launcherBackgroundPreferences(context).edit {
        putString(BACKGROUND_CHOICE, choice.save())
        // The old boolean is kept in step so the wallpaper service, which reads it directly, agrees with Home.
        putBoolean("photoEnabled", choice is BackgroundChoice.Photo)
    }
}
