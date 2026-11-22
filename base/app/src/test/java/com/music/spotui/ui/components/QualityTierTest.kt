package com.music.spotui.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QualityTierTest {
    @Test fun cdQualityFlacIsLossless() = assertEquals("LOSSLESS 16/44.1", QualityTier.label("flac", 16, 44_100))
    @Test fun deepOrFastFlacIsHiRes() {
        assertEquals("HI-RES 24/96", QualityTier.label("audio/flac", 24, 96_000))
        assertEquals("HI-RES", QualityTier.label("flac", null, 192_000))
    }
    @Test fun lossyShowsCodecAndBitrate() {
        assertEquals("AAC 256", QualityTier.label("audio/mp4a-latm", null, 44_100, 256_000))
        assertEquals("MP3", QualityTier.label("audio/mpeg", null, null))
        assertEquals("OPUS 160", QualityTier.label("opus", null, 48_000, 160_000))
    }
    @Test fun nothingKnownIsNoBadge() = assertNull(QualityTier.label(null, 24, 96_000))
}
