package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Test

/** StandBy on any phone: the fold's ways only with a hinge, the charger's always. */
class StandByPhonesTest {
    @Test fun `a phone without a fold is offered charging on its side only`() {
        assertEquals(listOf(StandByWay.CHARGING), standByWaysOffered(hinge = false))
    }

    @Test fun `a fold is offered all three, in Settings order`() {
        assertEquals(listOf(StandByWay.HALF_OPEN, StandByWay.CHARGING, StandByWay.TENT), standByWaysOffered(hinge = true))
    }
}
