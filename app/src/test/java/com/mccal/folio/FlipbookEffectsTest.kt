package com.mccal.folio

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Flipbook's page lists its effects. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class FlipbookEffectsTest {
    @get:Rule val compose = createComposeRule()

    private fun show(state: LauncherState, onEffect: (PageEffect) -> Unit = {}) =
        compose.setContent { Column { FlipbookEffects(state, onEffect) } }

    @Test fun `the built-in in use is selected`() {
        val picked = mutableListOf<Any>()
        show(LauncherState(pageEffect = PageEffect.CAROUSEL), { picked += it })
        compose.onNodeWithTag("page-effect-cube").assertIsNotSelected()
        compose.onNodeWithTag("page-effect-carousel").assertIsSelected()
        // Tapping the effect already in use changes nothing, so nothing is saved again.
        compose.onNodeWithTag("page-effect-carousel").performClick()
        assertEquals(emptyList<Any>(), picked)
    }

    // Cube's pair beside it, then Carousel's: Inside Cube is Cube from inside, Stack steps back like Carousel without turning.
    @Test fun `Flipbook offers its four effects in pairs, and choosing Stack reports it`() {
        assertEquals(listOf(PageEffect.CUBE, PageEffect.INSIDE_CUBE, PageEffect.CAROUSEL, PageEffect.STACK), PageEffect.CHOICES)
        val picked = mutableListOf<Any>()
        show(LauncherState(pageEffect = PageEffect.CUBE), { picked += it })
        compose.onNodeWithTag("page-effect-inside_cube").assertIsNotSelected()
        compose.onNodeWithText("Inside Cube").assertExists()
        compose.onNodeWithTag("page-effect-stack").performClick()
        assertEquals(listOf<Any>(PageEffect.STACK), picked)
    }

    // Both tweaks it re-creates ideas from are named on its page.
    @Test fun `Flipbook credits Barrel and Cylinder`() {
        val flipbook = TweakFeatures.first { it.id == "pageEffects" }
        assertEquals("Barrel by Aaron Ash, Cylinder by Reed Weichler", flipbook.inspiredBy)
    }

    @Test fun `with Flipbook off nothing is selected`() {
        show(LauncherState(pageEffect = PageEffect.NONE))
        compose.onNodeWithTag("page-effect-cube").assertIsNotSelected()
        compose.onNodeWithTag("page-effect-carousel").assertIsNotSelected()
    }
}
