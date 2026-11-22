package com.music.spotui.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.di.LocalPlayer
import com.music.spotui.di.SongPlayer
import com.music.spotui.extensions.ExtensionAudio

/**
 * The player's quality badge (after BitChord's SourceQualityBadge): "HI-RES", "LOSSLESS" or the
 * lossy codec, from what Myspoti's own player plays (the extension's announcement, else the
 * decoded format, else a download's file type). Nothing for Spotify's songs: their line says it.
 */
object QualityTier {
    private val LOSSLESS = setOf("flac", "alac", "wav", "pcm", "aiff", "ape", "wv", "dsd", "dsf", "dff")

    /** The badge's text for [codec] (a name or a mime type), [bitDepth] and [sampleRate] (Hz); null when nothing is known. */
    fun label(codec: String?, bitDepth: Int?, sampleRate: Int?, bitrate: Int? = null): String? {
        val c = codec?.lowercase()?.substringAfterLast('/')?.substringBefore(';')?.removePrefix("x-")?.trim()?.ifBlank { null } ?: return null
        val lossless = LOSSLESS.any { c.startsWith(it) } || c == "raw"
        if (!lossless) {
            val name = when {
                c.startsWith("mp4a") || c.startsWith("aac") || c == "mp4" -> "AAC"
                c.startsWith("mpeg") || c.startsWith("mp3") -> "MP3"
                c.startsWith("opus") -> "OPUS"
                c.startsWith("vorbis") || c.startsWith("ogg") -> "OGG"
                else -> c.uppercase().take(6)
            }
            return bitrate?.takeIf { it > 0 }?.let { "$name ${it / 1000}" } ?: name
        }
        val hiRes = (bitDepth ?: 0) > 16 || (sampleRate ?: 0) > 48_000
        val spec = if (bitDepth != null && sampleRate != null) " ${bitDepth}/${khz(sampleRate)}" else ""
        return (if (hiRes) "HI-RES" else "LOSSLESS") + spec
    }

    private fun khz(hz: Int) = if (hz % 1000 == 0) "${hz / 1000}" else "%.1f".format(java.util.Locale.ROOT, hz / 1000.0)
}

/** The badge for the song playing, or nothing. A tap does what the source line's tap does ([onClick]). */
@Composable
fun QualityBadge(tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val uri = SongPlayer.nowPlaying.value.uri
    val answers = ExtensionAudio.answers.intValue
    val local = LocalPlayer.active.value
    val label by produceState<String?>(null, uri, answers, local) {
        value = null
        if (!local) return@produceState
        // The decoded format is known a moment after the song starts: looked at a few times.
        repeat(6) { i ->
            if (i > 0) kotlinx.coroutines.delay(1_000)
            val t = LocalPlayer.current() ?: return@repeat
            val file = com.music.spotui.data.offline.OfflineStore.byLocalId(t.id)?.takeIf { t.uri.scheme == "file" }
            val stream = t.uri.takeIf { it.scheme == ExtensionAudio.SCHEME }?.let { ExtensionAudio.streamOf(it.lastPathSegment.orEmpty()) }
            val f = LocalPlayer.audioFormat()
            val found = QualityTier.label(
                codec = stream?.codec ?: f?.sampleMimeType ?: file?.file?.substringAfterLast('.', ""),
                bitDepth = stream?.bitDepth,
                sampleRate = stream?.sampleRate ?: f?.sampleRate?.takeIf { it > 0 },
                bitrate = f?.bitrate?.takeIf { it > 0 },
            )
            if (found != null) { value = found; if (f != null) return@produceState }
        }
    }
    val text = label ?: return
    Text(
        text, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, maxLines = 1,
        modifier = modifier
            .border(1.dp, tint.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}
