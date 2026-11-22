/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.di

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.myspoti.host.android.HiddenPageHost
import app.myspoti.host.android.MyspotiRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * Spotify as the user's account, on one hidden page: the official site the user signs in to in a
 * WebView ([host], its own WebView profile), and the bearer token the web player issues for that
 * session, which the Web API is then read with.
 *
 * Nothing plays here: the token is used for the account's own library (liked songs, playlists) and
 * its metadata. Spotify audio needs a playback engine this build does not have.
 */
object SpotifyRuntime {
    const val ORIGIN = "https://open.spotify.com"

    /** Spotify's own sign-in page, back on the player once done. */
    const val SIGN_IN_URL = "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com%2F"

    private const val PROFILE = "spotify"
    private const val TOKEN_URL = "$ORIGIN/get_access_token?reason=transport&productType=web_player"
    private const val WEB_PLAYER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36"

    /** The sites whose cookies hold the session (all deleted at sign-out). */
    val SESSION_URLS = listOf(ORIGIN, "https://accounts.spotify.com", "https://www.spotify.com")

    private var made = false

    val host: HiddenPageHost by lazy {
        made = true
        HiddenPageHost(Graph.app, PROFILE, ORIGIN, SESSION_URLS, null)
    }

    /** Signed in, as the session's cookies said last (for the screens). */
    var signedIn by mutableStateOf(false)
        private set

    val chosen: Boolean get() = com.music.spotui.data.preferences.AccountProviderPref.id == app.myspoti.api.account.AccountProviders.SPOTIFY

    /** The sign-in page came back to the player: the flow is over (signed in, or left signed out). */
    fun signInDone(url: String?): Boolean = url != null && (url == ORIGIN || url.startsWith("$ORIGIN/"))

    /** The cookie the session is kept in (`sp_dc`): the account is signed in. */
    fun signedIn(cookieHeader: String?): Boolean = cookieHeader?.contains("sp_dc=") == true

    /** Reads the session's cookies again: true when an account is signed in. */
    suspend fun refreshSignIn(): Boolean {
        val ok = runCatching { signedIn(host.header(ORIGIN)) }.getOrDefault(false)
        signedIn = ok
        if (!ok) cached = null
        return ok
    }

    /** Chosen: the session is read, the page freed when it is not. */
    fun startIfChosen() {
        if (!chosen) { if (made) { host.unload(); signedIn = false; cached = null }; return }
        MyspotiRuntime.scope.launch {
            val ok = runCatching { refreshSignIn() }.getOrDefault(false)
            app.myspoti.host.android.Logbook.log("spotify: ${if (ok) "signed in" else "not signed in"}")
        }
    }

    /** The sign-in WebView came back to Spotify ([ok]: an account is signed in). */
    suspend fun signInFinished(ok: Boolean) {
        host.flush()
        refreshSignIn()
        app.myspoti.host.android.Logbook.log("spotify: sign-in ${if (ok) "done" else "left signed out"}")
    }

    /** The sign-in dialog's callback (not a coroutine): the session is read in the background. */
    fun signInFinishedAsync(ok: Boolean) {
        MyspotiRuntime.scope.launch { signInFinished(ok) }
    }

    /** Sign-out from a click (not a coroutine): the cookies go in the background. */
    fun signOutAsync() { MyspotiRuntime.scope.launch { signOut() } }

    /** Signs out of Spotify on this phone: the session's cookies are deleted. */
    suspend fun signOut() {
        runCatching { host.clear() }
        signedIn = false
        cached = null
        host.unload()
    }

    @Volatile private var cached: Pair<String, Long>? = null

    /**
     * The web player's access token for this session, read from the cookie the sign-in left. Kept
     * until a minute before it expires; [force] asks again whatever was kept.
     */
    suspend fun token(force: Boolean = false): String? = withContext(Dispatchers.IO) {
        cached?.let { (t, expiresAt) -> if (!force && System.currentTimeMillis() < expiresAt - 60_000) return@withContext t }
        val cookie = runCatching { host.header(ORIGIN) }.getOrNull() ?: return@withContext null
        if (!cookie.contains("sp_dc=")) return@withContext null
        val request = Request.Builder().url(TOKEN_URL)
            .header("Cookie", cookie)
            .header("User-Agent", WEB_PLAYER_UA)
            .header("Accept", "application/json")
            .header("App-Platform", "WebPlayer")
            .build()
        runCatching {
            MyspotiRuntime.apiHttp.newCall(request).execute().use { r ->
                if (!r.isSuccessful) return@runCatching null
                val body = r.body.string()
                val json = JSONObject(body)
                val token = json.optString("accessToken").takeIf { it.isNotBlank() } ?: return@runCatching null
                val expiresAt = json.optLong("expiresAt").takeIf { it > 0 }
                    ?: (System.currentTimeMillis() + 30 * 60_000L)
                cached = token to expiresAt
                token
            }
        }.getOrNull()
    }

    /** Forget the page entirely (the app is closing). */
    fun release() { if (made) host.unload() }
}
