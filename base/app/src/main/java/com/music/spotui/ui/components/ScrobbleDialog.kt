package com.music.spotui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import com.music.spotui.data.scrobble.Scrobbler
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone
import kotlinx.coroutines.launch

/** What the Scrobbling row shows under its title: who it sends to, or that it is off. */
@Composable
fun scrobbleSummary(): String {
    val names = listOfNotNull(Scrobbler.lastFmUser?.let { "Last.fm ($it)" }, Scrobbler.listenBrainzUser?.let { "ListenBrainz ($it)" })
    return if (names.isEmpty()) stringResource(R.string.scrobble_off) else names.joinToString(", ")
}

/** Scrobbling settings: Last.fm (signed in on its own page), ListenBrainz (a user token), Spotify's songs or not. */
@Composable
fun ScrobbleDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lbToken by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var waitingForLastFm by remember { mutableStateOf(false) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(tone(0xFF1E1E1E))
                .border(1.dp, AppText.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState()).padding(24.dp),
        ) {
            Text(stringResource(R.string.scrobble_title), color = AppText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.scrobble_detail), color = AppMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))

            // Last.fm
            Heading("Last.fm")
            val fmUser = Scrobbler.lastFmUser
            when {
                !Scrobbler.lastFmAvailable -> Note(stringResource(R.string.scrobble_lastfm_unavailable))
                fmUser != null -> {
                    Note(stringResource(R.string.scrobble_connected_as, fmUser))
                    Action(stringResource(R.string.scrobble_disconnect)) { Scrobbler.disconnectLastFm() }
                }
                waitingForLastFm -> {
                    Note(stringResource(R.string.scrobble_lastfm_approve))
                    Action(stringResource(R.string.scrobble_lastfm_done)) {
                        scope.launch {
                            val name = Scrobbler.finishLastFm()
                            if (name != null) waitingForLastFm = false
                            message = if (name == null) context.getString(R.string.scrobble_lastfm_not_yet) else null
                        }
                    }
                }
                else -> Action(stringResource(R.string.scrobble_connect)) {
                    scope.launch {
                        val url = Scrobbler.lastFmAuthUrl()
                        if (url == null) { message = context.getString(R.string.scrobble_failed); return@launch }
                        waitingForLastFm = true
                        runCatching {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }
            }

            // ListenBrainz
            Heading("ListenBrainz")
            val lbUser = Scrobbler.listenBrainzUser
            if (lbUser != null) {
                Note(stringResource(R.string.scrobble_connected_as, lbUser))
                Action(stringResource(R.string.scrobble_disconnect)) { Scrobbler.disconnectListenBrainz() }
            } else {
                Note(stringResource(R.string.scrobble_lb_token_hint))
                OutlinedTextField(
                    value = lbToken, onValueChange = { lbToken = it }, singleLine = true,
                    label = { Text(stringResource(R.string.scrobble_lb_token)) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = AppText, unfocusedTextColor = AppText),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Action(stringResource(R.string.scrobble_connect)) {
                    scope.launch {
                        val name = Scrobbler.connectListenBrainz(lbToken)
                        message = if (name == null) context.getString(R.string.scrobble_lb_refused) else null
                        if (name != null) lbToken = ""
                    }
                }
            }

            // Spotify's songs
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.scrobble_include_spotify), color = AppText, fontSize = 15.sp)
                    Text(stringResource(R.string.scrobble_include_spotify_detail), color = AppMuted, fontSize = 12.sp)
                }
                Switch(checked = Scrobbler.includeSpotify, onCheckedChange = Scrobbler::includeSpotifySongs)
            }

            message?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp)) }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp))
                    .background(AppText.copy(alpha = 0.08f)).clickable(onClick = onDismiss),
            ) { Text(stringResource(R.string.scrobble_close), color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun Heading(text: String) =
    Text(text, color = AppText, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp))

@Composable
private fun Note(text: String) = Text(text, color = AppMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))

@Composable
private fun Action(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(top = 10.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(50))
            .border(1.dp, AppText.copy(alpha = 0.5f), RoundedCornerShape(50)).clickable(onClick = onClick),
    ) { Text(label, color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}
