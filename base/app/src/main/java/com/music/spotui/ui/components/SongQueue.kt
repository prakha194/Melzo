package com.music.spotui.ui.components

import android.content.Context
import android.widget.Toast
import app.myspoti.engine.R
import com.music.spotui.data.catalog.CatalogRuntime
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.SongPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Add to queue" and "Play next", for every place that offers them (the song menus, the
 * swipe on a row). A song of another provider (a search's while Spotify is not first in the
 * metadata order) has no Spotify id: it is first found on Spotify, as a tap on it plays it,
 * instead of the swipe doing nothing and the menu rows being greyed out.
 */
object SongQueue {
    /** Whether [song] can be queued at all: a song with an id, or a catalog provider's.  */
    fun canQueue(song: SongsModel): Boolean =
        song.spotifyTrackId.isNotBlank() || song.url.startsWith(CatalogRuntime.PLAY_PREFIX)

    /** Queues [song] (after the song playing when [next]) and says so. A catalog song goes by its catalog key. */
    fun add(context: Context, song: SongsModel, next: Boolean = false) {
        val id = song.spotifyTrackId.ifBlank {
            if (song.url.startsWith(CatalogRuntime.PLAY_PREFIX)) CatalogRuntime.keyOf(song.url.removePrefix(CatalogRuntime.PLAY_PREFIX)).orEmpty() else ""
        }
        if (id.isNotBlank()) queue(context.applicationContext, id, next)
    }

    private fun queue(context: Context, spotifyTrackId: String, next: Boolean) {
        if (next) SongPlayer.playNext(spotifyTrackId) else SongPlayer.addToQueue(spotifyTrackId)
        val said = if (next) R.string.lbl_play_next else R.string.lbl_queued
        Toast.makeText(context, said, Toast.LENGTH_SHORT).show()
    }
}
