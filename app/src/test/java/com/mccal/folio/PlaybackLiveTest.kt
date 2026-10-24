package com.mccal.folio

import android.media.session.PlaybackState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What counts as playing for Now Playing. A track that stops to load is still a track you're listening to: treating
 * only STATE_PLAYING as live made the media card disappear and come back on every skip over a slow connection.
 */
class PlaybackLiveTest {
    @Test fun `playing, buffering and connecting are all live`() {
        assertTrue(NotificationListener.playbackIsLive(PlaybackState.STATE_PLAYING))
        assertTrue(NotificationListener.playbackIsLive(PlaybackState.STATE_BUFFERING))
        assertTrue(NotificationListener.playbackIsLive(PlaybackState.STATE_CONNECTING))
    }

    @Test fun `a pause, a stop or no state at all is not live`() {
        assertFalse(NotificationListener.playbackIsLive(PlaybackState.STATE_PAUSED))
        assertFalse(NotificationListener.playbackIsLive(PlaybackState.STATE_STOPPED))
        assertFalse(NotificationListener.playbackIsLive(PlaybackState.STATE_NONE))
        assertFalse(NotificationListener.playbackIsLive(PlaybackState.STATE_ERROR))
        assertFalse(NotificationListener.playbackIsLive(null))
    }
}
