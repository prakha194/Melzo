/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone

private const val PREFS = "support_prompt"
private const val KEY_FIRST_SEEN = "first_seen"
private const val KEY_LAST_SHOWN = "last_shown"

private const val DAY_MS = 24 * 60 * 60 * 1000L

/** Asked at most this often: one a week. */
private const val REPEAT_MS = 7 * DAY_MS

/** A release notice on screen a moment ago: the ask waits for another launch. */
private const val YIELD_TO_UPDATE_MS = 5 * 60 * 1000L

/**
 * "Support the developer", once a week: a short card that asks the user to keep Melzo going.
 * Shown once soon after the install, then once a week; never right after a release notice, and
 * "No thanks" puts it away for another week.
 */
@Composable
fun SupportPrompt() {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(8_000)
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val firstSeen = prefs.getLong(KEY_FIRST_SEEN, 0L)
        if (firstSeen == 0L) prefs.edit().putLong(KEY_FIRST_SEEN, now).apply()
        // Shown once right after the install, then once a week.
        if (now - prefs.getLong(KEY_LAST_SHOWN, 0L) < REPEAT_MS) return@LaunchedEffect
        val noticeAt = context.getSharedPreferences("release_notice", Context.MODE_PRIVATE).getLong("shown_at", 0L)
        if (now - noticeAt < YIELD_TO_UPDATE_MS) return@LaunchedEffect
        show = true
    }
    if (!show) return
    val close = {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_SHOWN, System.currentTimeMillis()).apply()
        show = false
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = close) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(tone(0xFF242424))
                .padding(horizontal = 24.dp, vertical = 26.dp),
        ) {
            Text(
                "Support Melzo",
                color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            )
            Text(
                "Support the developer to keep Melzo running, and to keep it free of ads.",
                color = AppMuted, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(46.dp)
                    .clip(RoundedCornerShape(50)).background(AppAccent).pressable(0.96f) {
                        close()
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(COFFEE_URL),
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
            ) { Text("Buy me a coffee", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Text(
                "No thanks",
                color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp).pressable(0.96f) { close() }.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}
