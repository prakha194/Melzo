package com.music.spotui.data.scrobble

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrobblerTest {
    @Test fun aSongCountsAfterHalfItsLength() {
        assertFalse(Scrobbler.counts(durationMs = 200_000, playedMs = 99_000))
        assertTrue(Scrobbler.counts(durationMs = 200_000, playedMs = 100_000))
    }

    @Test fun aLongSongCountsAfterFourMinutes() {
        assertFalse(Scrobbler.counts(durationMs = 20 * 60_000L, playedMs = 239_000))
        assertTrue(Scrobbler.counts(durationMs = 20 * 60_000L, playedMs = 240_000))
    }

    @Test fun aSongUnderThirtySecondsNeverCounts() {
        assertFalse(Scrobbler.counts(durationMs = 29_000, playedMs = 29_000))
    }

    @Test fun anUnknownLengthNeedsFourMinutes() {
        assertFalse(Scrobbler.counts(durationMs = 0, playedMs = 200_000))
        assertTrue(Scrobbler.counts(durationMs = 0, playedMs = 240_000))
    }
}
