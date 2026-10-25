package com.mccal.folio

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FoldLandingTest {
    private val landings = mutableListOf<Landing>()
    private var clock = 0L

    private fun watcher(expanded: Boolean) = FoldWatcher(ApplicationProvider.getApplicationContext()).also {
        it.stayAwake = false
        it.expanded = expanded
        it.onLanded = { kind -> landings += kind }
    }

    private fun FoldWatcher.read(vararg degrees: Float) {
        degrees.forEach { angle -> clock += 200_000_000L; onAngle(angle, clock) }
    }

    private fun FoldWatcher.switchTo(expanded: Boolean) {
        this.expanded = expanded
        onDisplaySwitched(0L)
    }

    @Test fun `unfolding lands once, when the hinge reaches flat after the panel lights`() {
        val fold = watcher(expanded = false)
        fold.read(0f, 90f)
        fold.switchTo(expanded = true)
        fold.onPanelLit()
        assertEquals("nothing yet: the hinge is still on its way", emptyList<Landing>(), landings)
        fold.read(180f)
        assertEquals(listOf(Landing.OPENED), landings)
    }

    @Test fun `flat before the inner panel lights lands as soon as it does`() {
        val fold = watcher(expanded = false)
        fold.read(0f, 90f)
        fold.switchTo(expanded = true)
        fold.read(180f)
        assertEquals("not while the panel is still dark", emptyList<Landing>(), landings)
        fold.onPanelLit()
        assertEquals(listOf(Landing.OPENED), landings)
    }

    @Test fun `closing lands on the cover`() {
        val fold = watcher(expanded = true)
        fold.read(180f, 90f, 0f)
        fold.switchTo(expanded = false)
        fold.onPanelLit()
        assertEquals(listOf(Landing.CLOSED), landings)
    }

    @Test fun `a cover that lights before the hinge is shut lands when it shuts`() {
        val fold = watcher(expanded = true)
        fold.read(180f, 90f)
        fold.switchTo(expanded = false)
        fold.onPanelLit()
        assertEquals(emptyList<Landing>(), landings)
        fold.read(0f)
        assertEquals(listOf(Landing.CLOSED), landings)
    }

    @Test fun `half open and then flat lands, like a book laid open`() {
        val fold = watcher(expanded = true)
        fold.read(180f, 90f)
        assertEquals(emptyList<Landing>(), landings)
        fold.read(180f)
        assertEquals(listOf(Landing.OPENED), landings)
    }

    @Test fun `the first reading after starting is not a landing`() {
        watcher(expanded = true).read(180f)
        watcher(expanded = false).read(0f)
        assertEquals(emptyList<Landing>(), landings)
    }

    @Test fun `opening the cover a little and shutting it again lands`() {
        val fold = watcher(expanded = false)
        fold.read(0f, 90f, 0f)
        assertEquals(listOf(Landing.CLOSED), landings)
    }

    @Test fun `with no hinge readings the screen lands when its panel lights`() {
        val fold = watcher(expanded = false)
        fold.switchTo(expanded = true)
        fold.onPanelLit()
        fold.switchTo(expanded = false)
        fold.onPanelLit()
        assertEquals(listOf(Landing.OPENED, Landing.CLOSED), landings)
    }

    @Test fun `the page bounces up from flat, lower each time, and comes to rest`() {
        val first = PageLanding.FIRST_BOUNCE_MS
        assertEquals(0f, PageLanding.lift(0f), 1e-6f)
        assertEquals("the first bounce peaks half way through it", 1f, PageLanding.lift(first / 2f), 1e-4f)
        assertEquals("and touches down again", 0f, PageLanding.lift(first), 1e-4f)
        val second = PageLanding.lift(first + first * PageLanding.RESTITUTION / 2f)
        assertEquals(PageLanding.RESTITUTION * PageLanding.RESTITUTION, second, 1e-4f)
        assertEquals(0f, PageLanding.lift(PageLanding.DURATION_MS), 1e-6f)
        val samples = (0..PageLanding.DURATION_MS.toInt()).map { PageLanding.lift(it.toFloat()) }
        assertTrue("never below flat or past the peak", samples.all { it in 0f..1f })
        assertTrue("short, under half a second", PageLanding.DURATION_MS < 500f)
    }

    @Test fun `the cover turns about its hinge edge in every rotation`() {
        fun page(rotation: Int) = landingPage(Landing.CLOSED, rotation, null, 100f, 200f)
        assertEquals(LandingPage(false, 0f, true), page(android.view.Surface.ROTATION_0))
        assertEquals(LandingPage(true, 200f, false), page(android.view.Surface.ROTATION_90))
        assertEquals(LandingPage(false, 100f, false), page(android.view.Surface.ROTATION_180))
        assertEquals(LandingPage(true, 0f, true), page(android.view.Surface.ROTATION_270))
    }

    @Test fun `the open screen turns its moving half about the hinge`() {
        assertEquals(LandingPage(false, 1224f, false), landingPage(Landing.OPENED, android.view.Surface.ROTATION_0, null, 2448f, 1848f))
    }
}
