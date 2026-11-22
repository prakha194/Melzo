/*
 * Adapted from BitChord (https://github.com/kushagrasinghx/BitChord), GPL-3.0-or-later,
 * app/src/main/java/com/music/bitchord/playback/LoudnessProcessor.kt at acf219a.
 * Modified for Myspoti, 2026-11.
 *
 * Changes: BitChord's per-player gain, its glide and its peak limiter (instant attack, smooth
 * release) are kept; the gain comes from the song's own measured loudness (AutoMix's analysis)
 * instead of YouTube's figure, the blend trim and gapless-boundary hand-over are left out, and the
 * stage is a media3 audio processor (16-bit and float PCM) like Myspoti's equalizer.
 */
package com.music.spotui.di

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * "Even out volume": the local player's songs (extensions, downloads, the phone's files) brought to
 * one level. Spotify's own songs are levelled by Spotify's player and are not touched.
 */
object Leveller {
    private const val PREFS = "leveller"
    private const val KEY = "on"

    /** On or off (Settings > Playback). Read on the audio thread. */
    @Volatile var on: Boolean = false
        private set

    /** The level every song is brought to, in dB of [app.myspoti.automix.TrackAnalysis.loudnessDb]. */
    const val TARGET_DB = -14.0
    /** Never more than this much louder (quiet songs are raised, but not into noise). */
    const val MAX_BOOST_DB = 6.0
    /** Never more than this much quieter. */
    const val MAX_CUT_DB = 12.0

    fun init(context: android.content.Context) {
        on = context.getSharedPreferences(PREFS, 0).getBoolean(KEY, false)
    }

    fun set(context: android.content.Context, value: Boolean) {
        on = value
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(KEY, value).apply()
        LocalPlayer.levellerChanged()
    }

    /** The gain for a song of measured loudness [loudnessDb]: what brings it to [TARGET_DB], within the limits. */
    fun gainDbFor(loudnessDb: Double): Float =
        (TARGET_DB - loudnessDb).coerceIn(-MAX_CUT_DB, MAX_BOOST_DB).toFloat()

    /** The gain for the song at [uri], or 0 dB while it is not measured yet (or levelling is off). */
    fun gainDb(uri: Uri?): Float {
        if (!on || uri == null) return 0f
        val a = runCatching { com.music.spotui.automix.AutoMix.analysis(uri) }.getOrNull() ?: return 0f
        return gainDbFor(a.loudnessDb)
    }
}

/**
 * One player's levelling stage, last in its audio chain (after the equalizer and the spatial
 * stage). Each player has its own, so the two songs of a crossfade or a mix are each levelled for
 * themselves. The song is set by [LocalPlayer] ([uri]); its gain is read at every buffer, so a
 * measurement that arrives after the song started is glided in, not stepped.
 *
 * At 0 dB with nothing to limit it copies its input as it came (a lossless stream stays bit for
 * bit). A raised song is kept under full scale by a peak limiter.
 */
@UnstableApi
class LoudnessProcessor(private val gainFor: (Uri?) -> Float = Leveller::gainDb) : BaseAudioProcessor() {
    /** The song this player is playing now. Set from the main thread. */
    @Volatile var uri: Uri? = null
        set(value) { if (field != value) { field = value; snap = true } }

    private var channels = 0
    private var float = false
    private var currentGain = 1f
    private var reduction = 1f
    private var glideCoef = 1f
    private var releaseCoef = 1f
    @Volatile private var snap = true

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val enc = inputAudioFormat.encoding
        if ((enc != C.ENCODING_PCM_16BIT && enc != C.ENCODING_PCM_FLOAT) || inputAudioFormat.channelCount < 1 || inputAudioFormat.sampleRate <= 0)
            return AudioProcessor.AudioFormat.NOT_SET
        channels = inputAudioFormat.channelCount
        float = enc == C.ENCODING_PCM_FLOAT
        glideCoef = coefficient(GLIDE_SECONDS, inputAudioFormat.sampleRate)
        releaseCoef = coefficient(RELEASE_SECONDS, inputAudioFormat.sampleRate)
        snap = true
        return inputAudioFormat
    }

    override fun onFlush() { reduction = 1f; snap = true }

    override fun onReset() { channels = 0; reduction = 1f; snap = true }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val target = 10f.pow(gainFor(uri) / 20f)
        if (snap) { currentGain = target; snap = false }
        // Parked: no gain and nothing being limited. The exact copy.
        if (target == 1f && abs(currentGain - 1f) < SETTLED && reduction >= PARK_REDUCTION) {
            currentGain = 1f; reduction = 1f
            val out = replaceOutputBuffer(inputBuffer.remaining())
            out.put(inputBuffer)
            out.flip()
            return
        }
        val frameBytes = (if (float) 4 else 2) * channels
        val frames = inputBuffer.remaining() / frameBytes
        if (frames == 0) return
        val out = replaceOutputBuffer(frames * frameBytes)
        inputBuffer.order(ByteOrder.nativeOrder())
        val frame = FloatArray(channels)
        var gain = currentGain
        var gr = reduction
        repeat(frames) {
            gain += (target - gain) * glideCoef
            var peak = 0f
            for (c in 0 until channels) {
                val s = (if (float) inputBuffer.float else inputBuffer.short * (1f / 32768f)) * gain
                frame[c] = s
                if (abs(s) > peak) peak = abs(s)
            }
            // Down at once to what keeps this frame under the ceiling, then back up smoothly.
            val wanted = if (peak > LIMIT) LIMIT / peak else 1f
            gr = if (wanted < gr) wanted else gr + (wanted - gr) * releaseCoef
            for (c in 0 until channels) {
                val s = frame[c] * gr
                if (float) out.putFloat(s) else out.putShort((s * 32768f).roundToInt().coerceIn(-32768, 32767).toShort())
            }
        }
        currentGain = gain
        reduction = gr
        out.flip()
    }

    private fun coefficient(seconds: Double, rate: Int): Float =
        if (rate <= 0) 1f else (1.0 - exp(-1.0 / (seconds * rate))).toFloat()

    private companion object {
        /** Just under full scale. */
        const val LIMIT = 0.985f
        /** Gain changes glide over about this long: no zipper noise. */
        const val GLIDE_SECONDS = 0.04
        /** How quickly the limiter lets go after a peak. */
        const val RELEASE_SECONDS = 0.08
        const val SETTLED = 1e-4f
        /** Little enough reduction left (about 0.25 dB) to drop it when parking. */
        const val PARK_REDUCTION = 0.97f
    }
}
