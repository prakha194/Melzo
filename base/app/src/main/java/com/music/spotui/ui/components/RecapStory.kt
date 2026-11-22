package com.music.spotui.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.myspoti.engine.R
import com.music.spotui.data.local.ListenHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** One page of the recap story. */
private enum class StoryPage { MINUTES, ARTIST, SONGS, ALBUM, SUMMARY }

/** Each page's two background colours (dark enough for white text). */
private val PAGE_COLORS = mapOf(
    StoryPage.MINUTES to (Color(0xFF0E5A3A) to Color(0xFF071A12)),
    StoryPage.ARTIST to (Color(0xFF4B1F7A) to Color(0xFF140A22)),
    StoryPage.SONGS to (Color(0xFF7A2A1F) to Color(0xFF1E0B08)),
    StoryPage.ALBUM to (Color(0xFF1F4E7A) to Color(0xFF08131E)),
    StoryPage.SUMMARY to (Color(0xFF1DB060) to Color(0xFF0A2416)),
)

private const val PAGE_MS = 6_000L

/**
 * The recap as a story: full-screen pages with progress bars, tap the right side for the next,
 * the left for the one before; each moves on by itself after [PAGE_MS]. The last page shares the
 * recap as a poster image ([RecapPoster]). Pages without data (no album yet) are left out.
 */
@Composable
fun RecapStory(recap: ListenHistory.Recap, periodLabel: String, onDismiss: () -> Unit) {
    val pages = remember(recap) {
        StoryPage.entries.filter { p ->
            when (p) {
                StoryPage.ARTIST -> recap.topArtists.isNotEmpty()
                StoryPage.SONGS -> recap.topTracks.isNotEmpty()
                StoryPage.ALBUM -> recap.topAlbums.isNotEmpty()
                else -> true
            }
        }
    }
    var index by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var paused by remember { androidx.compose.runtime.mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(index, paused) {
        if (paused) return@LaunchedEffect
        val start = System.currentTimeMillis() - (progress * PAGE_MS).toLong()
        while (true) {
            progress = ((System.currentTimeMillis() - start).toFloat() / PAGE_MS).coerceAtMost(1f)
            if (progress >= 1f) {
                if (index < pages.lastIndex) { index++; progress = 0f } else break
                return@LaunchedEffect
            }
            kotlinx.coroutines.delay(16)
        }
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val page = pages[index]
        val (top, bottom) = PAGE_COLORS.getValue(page)
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))
                .pointerInput(pages.size) {
                    detectTapGestures(
                        onPress = { paused = true; tryAwaitRelease(); paused = false },
                        onTap = { o ->
                            if (o.x > size.width / 3f) {
                                if (index < pages.lastIndex) { index++; progress = 0f }
                            } else if (index > 0) { index--; progress = 0f } else progress = 0f
                        },
                    )
                },
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
                // The progress bars.
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    pages.indices.forEach { i ->
                        val fill = when { i < index -> 1f; i == index -> progress; else -> 0f }
                        Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.3f))) {
                            Box(Modifier.fillMaxWidth(fill).height(3.dp).background(Color.White))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.recap_story_title, periodLabel), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("✕", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
                }
                AnimatedContent(targetState = page, transitionSpec = { fadeIn() togetherWith fadeOut() }, modifier = Modifier.weight(1f), label = "page") { p ->
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) { PageContent(p, recap) }
                }
                if (page == StoryPage.SUMMARY) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50)).background(Color.White)
                            .clickable { scope.launch { RecapPoster.share(context, recap, periodLabel) } },
                    ) { Text(stringResource(R.string.recap_story_share), color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun PageContent(page: StoryPage, recap: ListenHistory.Recap) {
    when (page) {
        StoryPage.MINUTES -> {
            Big(stringResource(R.string.recap_story_minutes_lead))
            Text("%,d".format(recap.minutes), color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black)
            Big(stringResource(R.string.recap_story_minutes_unit))
            Small(stringResource(R.string.recap_listens, recap.listens))
        }
        StoryPage.ARTIST -> {
            val a = recap.topArtists.first()
            Big(stringResource(R.string.recap_story_artist_lead))
            Cover(a.cover, round = true)
            Text(a.name, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black, lineHeight = 44.sp)
            Small(stringResource(R.string.recap_story_artist_minutes, a.minutes))
            recap.topArtists.drop(1).take(4).forEachIndexed { i, r -> Line("${i + 2}", r.name) }
        }
        StoryPage.SONGS -> {
            Big(stringResource(R.string.recap_story_songs_lead))
            recap.topTracks.take(5).forEachIndexed { i, t -> Line("${i + 1}", t.name, t.subtitle) }
        }
        StoryPage.ALBUM -> {
            val a = recap.topAlbums.first()
            Big(stringResource(R.string.recap_story_album_lead))
            Cover(a.cover, round = false)
            Text(a.name, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp)
            Small(a.subtitle)
        }
        StoryPage.SUMMARY -> {
            Big(stringResource(R.string.recap_story_summary_lead))
            Small(stringResource(R.string.recap_minutes, recap.minutes))
            recap.topArtists.firstOrNull()?.let { Line("★", it.name, stringResource(R.string.recap_top_artists)) }
            recap.topTracks.firstOrNull()?.let { Line("♪", it.name, it.subtitle) }
            recap.topAlbums.firstOrNull()?.let { Line("◉", it.name, it.subtitle) }
        }
    }
}

@Composable private fun Big(text: String) =
    Text(text, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp, modifier = Modifier.padding(bottom = 12.dp))

@Composable private fun Small(text: String) =
    Text(text, color = Color.White.copy(alpha = 0.75f), fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))

@Composable
private fun Line(rank: String, title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Text(rank, color = Color.White.copy(alpha = 0.6f), fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.size(36.dp))
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi::class)
@Composable
private fun Cover(url: String, round: Boolean) {
    if (url.isBlank()) return
    com.bumptech.glide.integration.compose.GlideImage(
        model = url, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier.padding(bottom = 16.dp).size(200.dp).clip(if (round) CircleShape else RoundedCornerShape(12.dp)),
    )
}

/** The recap as a 1080×1920 poster, shared through the share sheet (text only: nothing to download). */
object RecapPoster {
    private const val W = 1080
    private const val H = 1920

    suspend fun share(context: Context, recap: ListenHistory.Recap, periodLabel: String) {
        val file = withContext(Dispatchers.IO) {
            runCatching {
                val bmp = render(context, recap, periodLabel)
                val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                File(dir, "myspoti_recap.png").also { f -> f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            }.getOrNull()
        } ?: return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.recap_story_share)).apply {
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    internal fun render(context: Context, recap: ListenHistory.Recap, periodLabel: String): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xFF1DB060.toInt(), 0xFF07140D.toInt(), Shader.TileMode.CLAMP)
        })
        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        fun paint(size: Float, alpha: Int = 255, typeface: Typeface = bold) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE; this.alpha = alpha; textSize = size; this.typeface = typeface
        }
        fun fit(text: String, p: Paint, max: Float = W - 160f): String {
            if (p.measureText(text) <= max) return text
            var t = text
            while (t.length > 1 && p.measureText("$t…") > max) t = t.dropLast(1)
            return "$t…"
        }
        var y = 200f
        val x = 80f
        c.drawText(fit(context.getString(R.string.recap_story_title, periodLabel), paint(44f, 210)), x, y, paint(44f, 210)); y += 170f
        c.drawText("%,d".format(recap.minutes), x, y, paint(170f)); y += 80f
        c.drawText(context.getString(R.string.recap_story_minutes_unit), x, y, paint(52f, 220)); y += 130f
        fun section(title: String, rows: List<ListenHistory.Ranked>) {
            if (rows.isEmpty()) return
            c.drawText(title.uppercase(), x, y, paint(36f, 170)); y += 76f
            rows.take(5).forEachIndexed { i, r ->
                c.drawText("${i + 1}", x, y, paint(48f, 150))
                c.drawText(fit(r.name, paint(50f), W - 260f), x + 80f, y, paint(50f))
                y += 72f
            }
            y += 60f
        }
        section(context.getString(R.string.recap_top_artists), recap.topArtists)
        section(context.getString(R.string.recap_top_tracks), recap.topTracks)
        c.drawText("Myspoti", x, H - 110f, paint(48f))
        return bmp
    }
}
