/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.data.api

import app.myspoti.api.AlbumRef
import app.myspoti.api.ArtistRef
import app.myspoti.api.Image
import app.myspoti.api.Page
import app.myspoti.api.Playlist
import app.myspoti.api.Track
import app.myspoti.host.android.MyspotiRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * The signed-in Spotify account's own library, read on the Web API with the web player's token
 * ([token]): its liked songs, its playlists and their songs, and what it played lately. Read only:
 * nothing is changed on the account, and nothing plays from it.
 */
class SpotifyAccountLibrary(private val token: suspend (Boolean) -> String?) {
    private val base = "https://api.spotify.com/v1"

    /** One GET on the Web API; a 401 asks for a fresh token once. */
    private suspend fun get(path: String, retried: Boolean = false): JSONObject? = withContext(Dispatchers.IO) {
        val bearer = token(retried) ?: return@withContext null
        val request = Request.Builder().url(if (path.startsWith("http")) path else base + path)
            .header("Authorization", "Bearer $bearer")
            .header("Accept", "application/json")
            .build()
        val answer = runCatching {
            MyspotiRuntime.apiHttp.newCall(request).execute().use { r -> r.code to r.body.string() }
        }.getOrNull() ?: return@withContext null
        when {
            answer.first == 200 -> runCatching { JSONObject(answer.second) }.getOrNull()
            answer.first == 401 && !retried -> get(path, retried = true)
            else -> null
        }
    }

    private fun images(a: JSONArray?): List<Image> =
        (0 until (a?.length() ?: 0)).mapNotNull { i ->
            a?.optJSONObject(i)?.let { o ->
                Image(o.optString("url"), o.optInt("width").takeIf { it > 0 }, o.optInt("height").takeIf { it > 0 })
            }
        }

    /** One track of the API, as the app's own [Track]. */
    private fun trackOf(o: JSONObject): Track {
        val album = o.optJSONObject("album")
        val artists = o.optJSONArray("artists")
        return Track(
            uri = o.optString("uri").takeIf { it.isNotBlank() } ?: ("spotify:track:" + o.optString("id")),
            name = o.optString("name"),
            durationMs = o.optLong("duration_ms"),
            artists = (0 until (artists?.length() ?: 0)).mapNotNull { i ->
                artists?.optJSONObject(i)?.let { ArtistRef(it.optString("uri"), it.optString("name")) }
            },
            album = album?.let { AlbumRef(it.optString("uri"), it.optString("name"), images(it.optJSONArray("images"))) },
            explicit = o.optBoolean("explicit"),
            playable = o.optBoolean("is_playable", true),
        )
    }

    private fun pageOf(o: JSONObject?, offset: Int, map: (JSONObject) -> Track?): Page<Track> {
        if (o == null) return Page(emptyList(), offset, 0, null)
        val items = o.optJSONArray("items") ?: JSONArray()
        val tracks = (0 until items.length()).mapNotNull { i -> items.optJSONObject(i)?.let(map) }
        val next = o.optString("next").takeIf { it.isNotBlank() && it != "null" }?.let { offset + tracks.size }
        return Page(tracks, offset, o.optInt("total"), next)
    }

    suspend fun likedTracks(offset: Int, limit: Int): Page<Track> {
        val o = get("/me/tracks?limit=$limit&offset=$offset") ?: return Page(emptyList(), offset, 0, null)
        return pageOf(o, offset) { row -> row.optJSONObject("track")?.let(::trackOf) }
    }

    suspend fun playlists(): List<Playlist> {
        val o = get("/me/playlists?limit=50") ?: return emptyList()
        val items = o.optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i ->
            items.optJSONObject(i)?.let { p ->
                Playlist(
                    uri = p.optString("uri").takeIf { it.isNotBlank() } ?: ("spotify:playlist:" + p.optString("id")),
                    name = p.optString("name"),
                    description = p.optString("description").takeIf { it.isNotBlank() },
                    owner = p.optJSONObject("owner")?.optString("display_name")?.takeIf { it.isNotBlank() },
                    images = images(p.optJSONArray("images")),
                    trackCount = p.optJSONObject("items")?.optInt("total")
                        ?: p.optJSONObject("tracks")?.optInt("total") ?: 0,
                )
            }
        }
    }

    /** One playlist's details, for its page. */
    suspend fun playlist(uri: String): Playlist? {
        val id = uri.substringAfterLast(':')
        val p = get("/playlists/$id") ?: return null
        return Playlist(
            uri = p.optString("uri").takeIf { it.isNotBlank() } ?: uri,
            name = p.optString("name"),
            description = p.optString("description").takeIf { it.isNotBlank() },
            owner = p.optJSONObject("owner")?.optString("display_name")?.takeIf { it.isNotBlank() },
            images = images(p.optJSONArray("images")),
            trackCount = p.optJSONObject("tracks")?.optInt("total") ?: 0,
            followers = p.optJSONObject("followers")?.optLong("total"),
        )
    }

    suspend fun playlistTracks(uri: String, offset: Int, limit: Int): Page<Track> {
        val id = uri.substringAfterLast(':')
        val o = get("/playlists/$id/tracks?limit=$limit&offset=$offset")
            ?: return Page(emptyList(), offset, 0, null)
        return pageOf(o, offset) { row ->
            row.optJSONObject("track")?.let { t -> trackOf(t).copy(addedAt = row.optString("added_at").takeIf { it.isNotBlank() }) }
        }
    }

    suspend fun history(limit: Int): List<Track> {
        val o = get("/me/player/recently-played?limit=$limit") ?: return emptyList()
        val items = o.optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i ->
            items.optJSONObject(i)?.optJSONObject("track")?.let(::trackOf)
        }
    }
}
