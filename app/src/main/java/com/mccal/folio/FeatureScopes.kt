package com.mccal.folio

import androidx.compose.material.icons.rounded.*

/** Where a tweak can be overridden (Choicy-style scopes). */
enum class FolioScreen(@androidx.annotation.StringRes val label: Int) { COVER(R.string.cover_screen), INNER(R.string.inner_screen) }

/** DEFAULT inherits the tweak's main switch; ON/OFF force it on that screen. */
enum class ScopeValue(@androidx.annotation.StringRes val label: Int) { DEFAULT(R.string.default_choice), ON(R.string.on), OFF(R.string.off) }

internal object FeatureScopes {
    fun value(scopes: Map<String, Map<String, String>>, id: String, screen: FolioScreen): ScopeValue =
        scopes[id]?.get(screen.name)?.let { name -> ScopeValue.entries.firstOrNull { it.name == name } } ?: ScopeValue.DEFAULT

    /** Whether a feature is on for [screen]: an override wins, otherwise the main switch. */
    fun on(scopes: Map<String, Map<String, String>>, id: String, global: Boolean, screen: FolioScreen): Boolean =
        when (value(scopes, id, screen)) { ScopeValue.ON -> true; ScopeValue.OFF -> false; ScopeValue.DEFAULT -> global }

    fun set(scopes: Map<String, Map<String, String>>, id: String, screen: FolioScreen, value: ScopeValue): Map<String, Map<String, String>> {
        val current = scopes[id].orEmpty()
        val next = if (value == ScopeValue.DEFAULT) current - screen.name else current + (screen.name to value.name)
        return if (next.isEmpty()) scopes - id else scopes + (id to next)
    }
}

internal fun screenFor(wide: Boolean) = if (wide) FolioScreen.INNER else FolioScreen.COVER

/** A tweak-inspired feature: its page in Settings › Tweaks, with a main switch and per-screen overrides. */
internal data class TweakFeature(
    val id: String, val name: String, val inspiredBy: String,
    /** A resource, not a string: this is the one line of a tweak a person reads, so it is translated. The name and
     *  what it is after are proper nouns and stay as they are. */
    @androidx.annotation.StringRes val description: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Long,
    val get: (LauncherState) -> Boolean, val set: (LauncherModel, Boolean) -> Unit, val default: Boolean,
    /** More words Settings search finds it by, for a tweak people know by what it does ("Page Effects"). */
    @androidx.annotation.StringRes val keywords: Int? = null,
    /** The credit line when this tweak includes someone's code, in place of "re-created from scratch". */
    @androidx.annotation.StringRes val credit: Int? = null,
)

internal val TweakFeatures = listOf(
    TweakFeature("appPanels", "Cabinet", "Velox by Phillip Tennen", // english-only
        R.string.tweak_cabinet_detail,
        androidx.compose.material.icons.Icons.Rounded.Widgets, FolioColors.Value.Blue, { it.appPanels }, { m, v -> m.setAppPanels(v) }, true),
    TweakFeature("dockMagnify", "Harborline", "Harbor by Evan Swick", // english-only
        R.string.tweak_harborline_detail,
        androidx.compose.material.icons.Icons.Rounded.Add, FolioColors.Value.Indigo, { it.dockMagnify }, { m, v -> m.setDockMagnify(v) }, false),
    // The switch is on or off, and which effect it is lives on Flipbook's own page. Turning it on brings back the last
    // one chosen; the cube the first time, since that is the one Barrel was known for.
    TweakFeature("pageEffects", "Flipbook", "Barrel by Aaron Ash, Cylinder by Reed Weichler", // english-only
        R.string.tweak_flipbook_detail,
        androidx.compose.material.icons.Icons.Rounded.AutoStories, FolioColors.Value.Teal,
        { it.pageEffect != PageEffect.NONE },
        { m, v -> m.setPageEffectOn(v) }, false, keywords = R.string.page_effects),
    // Folio's fold animation. It was built in, so it defaults on.
    TweakFeature(DUET_ID, "Duet", "Duo Fold Live by joeconsorti", // english-only
        R.string.tweak_duet_detail,
        androidx.compose.material.icons.Icons.Rounded.MenuBook, FolioColors.Value.Indigo,
        { it.foldEffect }, { m, v -> m.setFoldEffect(v) }, true, credit = R.string.tweak_duet_credit,
        keywords = R.string.settings_keywords_fold_animation),
)

internal const val DUET_ID = "duet"
