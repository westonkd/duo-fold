package com.mccal.folio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A background too big for Android to draw is decoded at a fraction of its size instead. */
class BackgroundSampleSizeTest {
    @Test fun `a picture is decoded inside the limits`() {
        assertEquals(1, BackgroundLibrary.sampleSize(2448, 3796))
        for ((w, h) in listOf(6000 to 6000, 20000 to 20000, 1000 to 9000, 5000 to 3000)) {
            val sample = BackgroundLibrary.sampleSize(w, h)
            assertTrue("$w x $h at 1/$sample", maxOf(w, h) / sample <= BackgroundLibrary.MAX_ART_EDGE &&
                (w.toLong() / sample) * (h / sample) <= BackgroundLibrary.MAX_ART_PIXELS)
        }
    }
}
