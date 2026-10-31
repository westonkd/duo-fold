package com.mccal.folio

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LightTiltTest {
    private fun TiltFollower.hold(x: Float, y: Float, samples: Int): Offset {
        var shift = Offset.Zero
        repeat(samples) { shift = follow(x, y) }
        return shift
    }

    @Test fun `a phone held still leaves the light where it is`() {
        val follower = TiltFollower()
        assertEquals(Offset.Zero, follower.follow(.1f, .8f))
        assertEquals(0f, follower.hold(.1f, .8f, 50).getDistance(), .0001f)
    }

    @Test fun `tilting moves the light, then it drifts back while the phone stays tilted`() {
        val follower = TiltFollower()
        follower.hold(0f, .8f, 50)
        val moved = follower.hold(-.15f, .7f, 10)
        assertTrue(moved.x > .1f)
        assertTrue(moved.y < -.05f)
        val later = follower.hold(-.15f, .7f, 400)
        assertTrue(later.getDistance() < moved.getDistance() / 10f)
    }

    @Test fun `a hard tilt stops at the limit`() {
        val follower = TiltFollower()
        follower.hold(0f, .8f, 50)
        val shift = follower.hold(-1f, -.2f, 8)
        assertEquals(LightTiltMotion.LIMIT, shift.x, .0001f)
        assertEquals(-LightTiltMotion.LIMIT, shift.y, .0001f)
    }

    @Test fun `small wobbles don't redraw anything`() {
        val tilt = LightTilt()
        tilt.follow(Offset(LightTiltMotion.STEP / 2f, 0f))
        assertEquals(Offset.Zero, tilt.shift)
        tilt.follow(Offset(LightTiltMotion.STEP * 2f, 0f))
        assertEquals(LightTiltMotion.STEP * 2f, tilt.shift.x, .0001f)
    }
}
