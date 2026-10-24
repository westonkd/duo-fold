package com.mccal.folio

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Flipbook's effects, on its own page. Choosing one turns Flipbook on; the Enabled switch above turns it off.
 */
@Composable
internal fun FlipbookEffects(state: LauncherState, onEffect: (PageEffect) -> Unit) {
    SettingsCard(stringResource(R.string.page_effects)) {
        // The card draws the line between its rows itself.
        for (effect in PageEffect.CHOICES) {
            IosCheckRow(stringResource(effect.label), state.pageEffect == effect, { onEffect(effect) },
                "page-effect-${effect.name.lowercase()}")
        }
        CardNote(stringResource(R.string.home_pages_turn_in_3d_as_you_swipe_off_w))
    }
}
