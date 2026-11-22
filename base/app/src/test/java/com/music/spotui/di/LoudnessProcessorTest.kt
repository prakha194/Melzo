package com.music.spotui.di

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class LoudnessProcessorTest {
    private fun run(gainDb: Float, samples: FloatArray): FloatArray {
        val p = LoudnessProcessor { gainDb }
        p.configure(AudioProcessor.AudioFormat(44_100, 1, C.ENCODING_PCM_FLOAT))
        p.flush()
        val input = ByteBuffer.allocateDirect(samples.size * 4).order(ByteOrder.nativeOrder())
        samples.forEach(input::putFloat)
        input.flip()
        p.queueInput(input)
        val out = p.output.order(ByteOrder.nativeOrder())
        return FloatArray(out.remaining() / 4) { out.float }
    }

    @Test fun quietSongIsRaisedLoudSongIsLowered() {
        assertEquals(6f, Leveller.gainDbFor(-24.0), 0.001f) // capped boost
        assertEquals(4f, Leveller.gainDbFor(-18.0), 0.001f)
        assertEquals(-6f, Leveller.gainDbFor(-8.0), 0.001f)
        assertEquals(-12f, Leveller.gainDbFor(10.0), 0.001f) // capped cut
    }

    @Test fun zeroGainIsAnExactCopy() {
        val input = FloatArray(512) { (it % 7) * 0.1f - 0.3f }
        assertTrue(input.contentEquals(run(0f, input)))
    }

    @Test fun gainIsAppliedFromTheFirstSample() {
        // -6 dB halves the level.
        val out = run(-6.0206f, FloatArray(256) { 0.5f })
        out.forEach { assertEquals(0.25f, it, 0.001f) }
    }

    @Test fun raisedPeaksNeverPassFullScale() {
        val out = run(6f, FloatArray(4_096) { if (it % 2 == 0) 0.9f else -0.9f })
        out.forEach { assertTrue("sample $it over full scale", kotlin.math.abs(it) <= 0.986f) }
    }
}
