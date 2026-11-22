package com.music.spotui.connect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * While the device picker is open, the Spotify Connect devices of the network are looked for, so a
 * Sonos the music is moved to can be told to play ([SonosLocal]). Nothing is logged in from here:
 * a device joins the account through Spotify's own apps.
 */
@Composable
fun LanDiscoveryWhileOpen() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        LanDiscovery.start(context)
        onDispose { LanDiscovery.stop() }
    }
}
