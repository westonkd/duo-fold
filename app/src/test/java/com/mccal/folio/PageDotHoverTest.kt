package com.mccal.folio

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageDotHoverTest {
    private val dots = Rect(100f, 500f, 148f, 530f)

    @Test fun `a point over the dots picks the dot under it`() {
        assertEquals(0, pageDotUnder(Offset(101f, 515f), dots, 3, 24f))
        assertEquals(1, pageDotUnder(Offset(124f, 515f), dots, 3, 24f))
        assertEquals(2, pageDotUnder(Offset(147f, 515f), dots, 3, 24f))
    }

    @Test fun `a carried icon a little above or below the dots still counts`() {
        assertEquals(1, pageDotUnder(Offset(124f, 480f), dots, 3, 24f))
        assertEquals(1, pageDotUnder(Offset(124f, 550f), dots, 3, 24f))
    }

    @Test fun `away from the dots, or with none drawn, there is no dot`() {
        assertNull(pageDotUnder(Offset(124f, 400f), dots, 3, 24f))
        assertNull(pageDotUnder(Offset(90f, 515f), dots, 3, 24f))
        assertNull(pageDotUnder(Offset(124f, 515f), Rect.Zero, 3, 24f))
        assertNull(pageDotUnder(Offset(124f, 515f), dots, 0, 24f))
    }
}
