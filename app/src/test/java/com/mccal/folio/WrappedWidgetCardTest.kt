package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Test

class WrappedWidgetCardTest {
    @Test fun `a full-width card a little shorter than its slot grows to the slot's height`() {
        assertEquals(CardStretch(width = false, height = true), wrappedCardStretch(171, 190, 171, 163, wrapsWidth = false, wrapsHeight = true))
    }

    @Test fun `a full-height card a little narrower than its slot grows to the slot's width`() {
        assertEquals(CardStretch(width = true, height = false), wrappedCardStretch(360, 190, 300, 190, wrapsWidth = true, wrapsHeight = false))
    }

    @Test fun `a pill floating in a tall slot keeps its size`() {
        assertEquals(CardStretch.NONE, wrappedCardStretch(360, 190, 360, 56, wrapsWidth = false, wrapsHeight = true))
    }

    @Test fun `a card that doesn't reach either edge keeps its size`() {
        assertEquals(CardStretch.NONE, wrappedCardStretch(171, 190, 160, 170, wrapsWidth = true, wrapsHeight = true))
    }

    @Test fun `a card sized by the provider keeps its size`() {
        assertEquals(CardStretch.NONE, wrappedCardStretch(171, 190, 171, 163, wrapsWidth = false, wrapsHeight = false))
    }

    @Test fun `a card that already fills its slot needs no stretch`() {
        assertEquals(CardStretch.NONE, wrappedCardStretch(171, 190, 171, 190, wrapsWidth = true, wrapsHeight = true))
    }
}
