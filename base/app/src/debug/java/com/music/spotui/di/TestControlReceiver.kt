package com.music.spotui.di

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject

/**
 * Debug builds only: the playback test (tools/playback_test.py) sends the app's own
 * commands, as a tap would, with
 * `adb shell am broadcast -a app.myspoti.TEST -p app.orpheon.beta.dev --es cmd <cmd> ...`.
 * cmd: play (--es track <uri> --es context <uri>), resume, pause, next, previous, seek (--el ms <n>).
 * cmd: download (--es id <song id> --es title T --es artist A --ei ms <duration>), playdownloaded (--es id).
 * cmd: screenon (--ez on false to release) holds the screen on for a long run: One UI does not
 * honour the huge `screen_off_timeout` set over adb, the window flag does.
 * cmd: crossfade (--ei secs <0..12>) sets the crossfade between songs, as the Settings slider does.
 * cmd: extlist logs the installed extensions; extuninstall (--es id <extension id>) removes one
 * through the app's own uninstall path, as its page's Uninstall button does.
 */
class TestControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cmd = intent.getStringExtra("cmd").orEmpty()
        Log.i("MspPlayer", "test command $cmd")
        when (cmd) {
            "play" -> SongPlayer.playSong(intent.getStringExtra("track").orEmpty(), contextUri = intent.getStringExtra("context"))
            "resume" -> SongPlayer.play()
            "pause" -> SongPlayer.pause()
            "next" -> SongPlayer.next()
            // The player's own "next" button and the mini player's swipe.
            "swipenext" -> SongPlayer.swipeNext()
            "previous" -> SongPlayer.previous()
            "crossfade" -> com.music.spotui.data.preferences.CrossfadePref.set(context, intent.getIntExtra("secs", 0))
            "seek" -> SongPlayer.seekTo(intent.getLongExtra("ms", 0L))
            "screenon" -> context.getSharedPreferences("myspoti_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("keep_screen_on", intent.getBooleanExtra("on", true)).apply()
            // Debug builds: the same report the Settings row writes, so an unattended run can check
            // that the export really produces a readable file (and where it lands).
            "diagnose" -> runCatching {
                val file = app.myspoti.host.android.Logbook.export(context)
                Log.i("MspPlayer", "diagnostic report: ${file.absolutePath} (${file.length()} bytes)")
            }.onFailure { Log.i("MspPlayer", "diagnostic report failed: $it") }
            "extlist" -> com.music.spotui.extensions.Extensions.registry.value.extensions
                .forEach { Log.i("MspPlayer", "extension ${it.id} ${it.version} enabled=${it.enabled} source=${it.source}") }
            "extuninstall" -> app.myspoti.host.android.MyspotiRuntime.scope.launch {
                val id = intent.getStringExtra("id").orEmpty()
                if (com.music.spotui.extensions.Extensions.registry.value.find(id) == null) Log.i("MspPlayer", "extension $id: not installed")
                else { com.music.spotui.extensions.Extensions.uninstall(id); Log.i("MspPlayer", "extension $id: uninstalled") }
            }
            "top" -> app.myspoti.host.android.MyspotiRuntime.scope.launch {
                app.myspoti.api.TimeRange.entries.forEach { r ->
                    runCatching { app.myspoti.host.android.MyspotiRuntime.api.users.top(r, 50) }
                        .onSuccess { Log.i("MspPlayer", "top $r -> artists=${it.artists.size}/${it.artistTotal} tracks=${it.tracks.size}/${it.trackTotal}") }
                        .onFailure { Log.i("MspPlayer", "top $r failed: $it") }
                }
            }
            // Debug: downloads one song (--es id <song id> --es title T --es artist A --ei ms <duration>)
            // through the download-source extensions, as the download button does.
            "download" -> com.music.spotui.data.offline.OfflineDownloader.enqueue(listOf(com.music.spotui.data.entity.SongsModel(
                0, intent.getStringExtra("title").orEmpty(), "", intent.getStringExtra("artist").orEmpty(), "", "",
                spotifyTrackId = intent.getStringExtra("id").orEmpty(), durationMs = intent.getIntExtra("ms", 0),
            )))
            // Debug: plays the downloaded songs in the local player (--es id <song id> to start there).
            "playdownloaded" -> {
                val all = com.music.spotui.data.offline.OfflineStore.tracks.values.toList()
                val at = all.indexOfFirst { it.trackId == intent.getStringExtra("id") }.coerceAtLeast(0)
                Log.i("MspPlayer", "downloaded: ${all.size} song(s)")
                if (all.isNotEmpty()) LocalPlayer.play(all.map(com.music.spotui.data.offline.OfflineStore::toLocalTrack), at, context = "test")
            }
            // Debug: how many local audio files the app would list (Local Files check without the UI).
            "localfiles" -> Thread {
                val n = runCatching { kotlinx.coroutines.runBlocking { com.music.spotui.data.local.LocalMusic.tracks(context, refresh = true).size } }
                Log.i("MspPlayer", "local files: ${n.getOrElse { "failed: $it" }} track(s)")
            }.start()
        }
    }
}

/** A JSON value's structure without its values: object keys, array sizes and the first element's shape. */
private fun shape(e: kotlinx.serialization.json.JsonElement?, depth: Int = 0): String = when {
    e == null -> "null"
    depth > 4 -> "..."
    e is kotlinx.serialization.json.JsonObject -> e.entries.joinToString(",", "{", "}") { (k, v) -> "$k:${shape(v, depth + 1)}" }
    e is kotlinx.serialization.json.JsonArray -> "[${e.size}]" + (e.firstOrNull()?.let { shape(it, depth + 1) } ?: "")
    e is kotlinx.serialization.json.JsonNull -> "null"
    else -> (e as kotlinx.serialization.json.JsonPrimitive).let { if (it.isString) "s" else "n" }
}
