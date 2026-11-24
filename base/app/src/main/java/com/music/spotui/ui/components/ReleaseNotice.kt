package com.music.spotui.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.BuildConfig
import app.myspoti.engine.R
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone
import java.net.HttpURLConnection
import java.net.URL

/** Opens the release page outside the app; a browser missing is not worth a crash. */
private fun openReleasePage(context: Context) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(RELEASES_PAGE))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** The release page a newer build sends people to. */
const val RELEASES_PAGE = "https://github.com/prakha194/Spoon/releases/latest"
private const val LATEST_RELEASE_API = "https://api.github.com/repos/prakha194/Spoon/releases/latest"
private const val NOTICE_PREFS = "release_notice"
private const val KEY_DISMISSED = "dismissed_tag"
private const val KEY_DISMISSED_AT = "dismissed_at"
/** "Not now" hides the card for this long, then it comes back until the app is updated. */
private const val REPEAT_MS = 6 * 60 * 60 * 1000L

/** The latest release's tag ("v1.0.2"), or null when there is no release or no network. */
private fun latestTag(): String? = runCatching {
    val connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8_000
        readTimeout = 8_000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "Melzo-Android")
    }
    try {
        if (connection.responseCode == 200) {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            org.json.JSONObject(body).optString("tag_name").takeIf { it.isNotBlank() }
        } else null
    } finally {
        connection.disconnect()
    }
}.getOrNull()

/** [tag] is a later version than [current]: "1.2.3" and "v1.2.3" compare the same, a -beta suffix is ignored. */
private fun isNewer(tag: String, current: String): Boolean {
    fun parts(s: String) = s.trim().removePrefix("v").removePrefix("V")
        .substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val a = parts(tag)
    val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/**
 * A newer release on GitHub: one card a few seconds after launch, with "Update now" (opens the
 * release page) and "Not now" (that version is not offered again).
 */
@Composable
fun ReleaseNotice() {
    val context = LocalContext.current
    var tag by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        // 4 s after launch, then again every 15 minutes while the app stays open: a release
        // published after the app was started is still offered, without waiting for a relaunch.
        kotlinx.coroutines.delay(4_000)
        while (true) {
            val latest = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { latestTag() }
            if (latest != null && isNewer(latest, BuildConfig.VERSION_NAME)) {
                val prefs = context.getSharedPreferences(NOTICE_PREFS, Context.MODE_PRIVATE)
                val dismissed = prefs.getString(KEY_DISMISSED, null)
                val dismissedAt = prefs.getLong(KEY_DISMISSED_AT, 0L)
                // "Not now" is a pause, not a silence: after REPEAT_MS the card is offered again,
                // and keeps coming back every REPEAT_MS until the app is updated.
                if (dismissed != latest || System.currentTimeMillis() - dismissedAt >= REPEAT_MS) {
                    prefs.edit().putLong("shown_at", System.currentTimeMillis()).apply()
                    tag = latest; return@LaunchedEffect
                }
            }
            kotlinx.coroutines.delay(15 * 60_000L)
        }
    }
    val current = tag ?: return
    val close = {
        context.getSharedPreferences(NOTICE_PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_DISMISSED, current)
            .putLong(KEY_DISMISSED_AT, System.currentTimeMillis())
            .apply()
        tag = null
    }
    val version = current.trim().removePrefix("v").removePrefix("V")
    androidx.compose.ui.window.Dialog(onDismissRequest = close) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(tone(0xFF242424))
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(tone(0xFF181818)),
            )
            Text(
                "A new version of Melzo is available",
                color = AppText, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp),
            )
            Text("Melzo $version", color = AppMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(top = 22.dp).fillMaxWidth().height(48.dp)
                    .clip(RoundedCornerShape(50)).background(AppAccent)
                    .pressable(0.96f) { openReleasePage(context); close() },
            ) {
                Text("Update now", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "Not now", color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp).pressable(0.96f) { close() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            )
        }
    }
}
