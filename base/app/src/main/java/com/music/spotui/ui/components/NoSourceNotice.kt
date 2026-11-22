package com.music.spotui.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.myspoti.engine.R
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone

/** A song or an episode that no source (file, download, radio, extension) plays: the user is told. */
object NoSource {
    /** The item last refused, with its title; null once shown. */
    var refused by mutableStateOf<Pair<String, String>?>(null)

    /** [uri] had no source. */
    fun tell(uri: String, title: String?) { refused = uri to title.orEmpty() }
}

/** The message for [NoSource]. Drawn by the app's root over both looks. */
@Composable
fun NoSourceNotice() {
    val context = LocalContext.current
    val host = remember { SnackbarHostState() }
    val refused = NoSource.refused
    LaunchedEffect(refused) {
        val (_, title) = refused ?: return@LaunchedEffect
        host.showSnackbar(
            message = if (title.isBlank()) context.getString(R.string.no_source_message_untitled) else context.getString(R.string.no_source_message, title),
            duration = SnackbarDuration.Long,
        )
        NoSource.refused = null
    }
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 140.dp, start = 8.dp, end = 8.dp), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(host) { data ->
            Snackbar(snackbarData = data, containerColor = tone(0xFF282828), contentColor = AppText, actionContentColor = AppAccent)
        }
    }
}
