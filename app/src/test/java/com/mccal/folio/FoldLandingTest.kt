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

    @Test fun `a braced cover lands when it lights, and the late shut reading does not land it again`() {
        val fold = watcher(expanded = true)
        fold.read(180f, 90f)
        fold.switchTo(expanded = false)
        fold.onPanelLit(assumeShut = true)
        assertEquals(listOf(Landing.CLOSED), landings)
        fold.read(0f)
        assertEquals(listOf(Landing.CLOSED), landings)
    }

    @Test fun `after an assumed shut, opening a little and shutting again still lands`() {
        val fold = watcher(expanded = true)
        fold.read(180f, 90f)
        fold.switchTo(expanded = false)
        fold.onPanelLit(assumeShut = true)
        fold.read(0f, 90f, 0f)
        assertEquals(listOf(Landing.CLOSED, Landing.CLOSED), landings)
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

    @Test fun `closing, the cover falls faster and faster and stops dead, like a book shutting`() {
        val fall = PageLanding.SHUT_FALL_MS
        fun lift(ms: Float) = PageLanding.pose(Landing.CLOSED, ms).lift
        assertEquals("it lights already lifted", 1f, lift(0f), 1e-6f)
        val drops = (0..3).map { lift(fall * it / 4f) - lift(fall * (it + 1) / 4f) }
        assertTrue("each quarter of the fall covers more than the last", drops.zipWithNext().all { (a, b) -> b > a })
        assertTrue("the last quarter is the fastest by far", drops.last() > 3f * drops.first())
        val after = (fall.toInt()..PageLanding.duration(Landing.CLOSED).toInt()).map { lift(it.toFloat()) }
        assertTrue("no bounce once it is shut", after.all { it == 0f })
    }

    @Test fun `the crease darkens as the cover closes and lets go after it shuts`() {
        fun shade(ms: Float) = PageLanding.pose(Landing.CLOSED, ms).shade
        val fall = PageLanding.SHUT_FALL_MS
        assertTrue(shade(fall * .25f) < shade(fall * .75f))
        assertEquals("darkest at the moment it shuts", 1f, shade(fall), 1e-4f)
        assertTrue(shade(fall + PageLanding.SHUT_RELEASE_MS / 2f) < .5f)
        assertEquals(PagePose.FLAT, PageLanding.pose(Landing.CLOSED, PageLanding.duration(Landing.CLOSED)))
    }

    @Test fun `opening, the page springs up off the spine quickly and settles slowly, once`() {
        fun lift(ms: Float) = PageLanding.pose(Landing.OPENED, ms).lift
        assertEquals("it starts where the open screen already is: flat", 0f, lift(0f), 1e-6f)
        assertEquals(PageLanding.OPEN_PEAK, lift(PageLanding.OPEN_RISE_MS), 1e-4f)
        assertTrue("up faster than it comes down", PageLanding.OPEN_RISE_MS * 2f < PageLanding.OPEN_SETTLE_MS)
        val settling = (PageLanding.OPEN_RISE_MS.toInt()..PageLanding.duration(Landing.OPENED).toInt()).map { lift(it.toFloat()) }
        assertTrue("no second bounce", settling.zipWithNext().all { (a, b) -> b <= a })
        assertEquals(PagePose.FLAT, PageLanding.pose(Landing.OPENED, PageLanding.duration(Landing.OPENED)))
    }

    @Test fun `opening, the light comes in after the page`() {
        val settle = (PageLanding.OPEN_RISE_MS.toInt()..PageLanding.duration(Landing.OPENED).toInt() step 10)
            .map { PageLanding.pose(Landing.OPENED, it.toFloat()) }
        assertTrue("the crease stays shaded longer than the page stays lifted", settle.all { it.shade >= it.lift / PageLanding.OPEN_PEAK - 1e-4f })
        assertTrue(settle.zipWithNext().all { (a, b) -> b.shade <= a.shade })
    }

    @Test fun `the shut is felt the moment the hinge reads it, before the cover lights, and only once`() {
        var shuts = 0
        val fold = watcher(expanded = true).also { it.onShut = { shuts++ } }
        fold.read(180f, 90f)
        assertEquals(0, shuts)
        fold.read(0f)
        assertEquals(1, shuts)
        fold.switchTo(expanded = false)
        fold.onPanelLit(assumeShut = true)
        assertTrue(fold.shutFelt)
        assertEquals(1, shuts)
    }

    @Test fun `a cover that lights before the hinge reads shut is felt when it does`() {
        var shuts = 0
        val fold = watcher(expanded = true).also { it.onShut = { shuts++ } }
        fold.read(180f, 90f)
        fold.switchTo(expanded = false)
        fold.read(0f)
        assertEquals(1, shuts)
        fold.read(90f, 0f)
        assertEquals("opened a little and shut again", 2, shuts)
    }

    @Test fun `resting shut on the open screen is not felt`() {
        var shuts = 0
        val fold = watcher(expanded = true).also { it.onShut = { shuts++ } }
        fold.read(90f, 0f)
        assertEquals(0, shuts)
    }

    @Test fun `both landings are short and stay within their range`() {
        Landing.values().forEach { kind ->
            val duration = PageLanding.duration(kind)
            assertTrue("$kind under half a second", duration < 500f)
            val poses = (0..duration.toInt()).map { PageLanding.pose(kind, it.toFloat()) }
            assertTrue(poses.all { it.lift in 0f..1f && it.shade in 0f..1f })
        }
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
