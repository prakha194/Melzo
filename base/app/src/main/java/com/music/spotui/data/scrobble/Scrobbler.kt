package com.music.spotui.data.scrobble

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.myspoti.engine.BuildConfig
import com.music.spotui.data.local.ListenHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Scrobbling: what plays in Myspoti sent to Last.fm and/or ListenBrainz, off by default.
 *
 * A song counts once it has played half its length or 4 minutes, whichever comes first, and only
 * songs longer than 30 seconds (Last.fm's rule, used for both). "Now playing" is sent as a song
 * starts. A scrobble that cannot be sent (offline, a server error) is kept and sent with the next
 * one, up to [MAX_PENDING].
 *
 * Last.fm needs the app's own API key and secret ([BuildConfig.LASTFM_API_KEY]); the user signs in
 * on Last.fm's page (the app never sees a password). ListenBrainz needs the user's token only.
 */
object Scrobbler {
    private const val PREFS = "scrobbling"
    private const val MAX_PENDING = 500
    private const val MIN_SONG_MS = 30_000L
    private const val ENOUGH_MS = 4 * 60_000L
    private const val LASTFM = "https://ws.audioscrobbler.com/2.0/"
    private const val LISTENBRAINZ = "https://api.listenbrainz.org/1/"

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sending = Mutex()
    private val http by lazy { OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build() }

    /** Last.fm can be used in this build (its key and secret were given at build time). */
    val lastFmAvailable: Boolean get() = BuildConfig.LASTFM_API_KEY.isNotBlank() && BuildConfig.LASTFM_SECRET.isNotBlank()

    /** The signed-in Last.fm user, or null. */
    var lastFmUser by mutableStateOf<String?>(null)
        private set
    /** The ListenBrainz user the saved token belongs to, or null. */
    var listenBrainzUser by mutableStateOf<String?>(null)
        private set
    /** Spotify's own songs are scrobbled too (off: only downloads, local files and extension songs). */
    var includeSpotify by mutableStateOf(true)
        private set

    private var lastFmKey: String? = null
    private var lbToken: String? = null
    /** The Last.fm web-auth token waiting for the user's approval in the browser. */
    private var pendingAuthToken: String? = null

    val on: Boolean get() = lastFmKey != null || lbToken != null

    fun init(context: Context) {
        app = context.applicationContext
        val p = prefs()
        lastFmKey = p.getString("lastfm_key", null)
        lastFmUser = p.getString("lastfm_user", null)
        lbToken = p.getString("lb_token", null)
        listenBrainzUser = p.getString("lb_user", null)
        includeSpotify = p.getBoolean("include_spotify", true)
    }

    private fun prefs() = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun includeSpotifySongs(value: Boolean) {
        includeSpotify = value
        prefs().edit().putBoolean("include_spotify", value).apply()
    }

    // ── Plays ──

    /** A song started (the player's [source] plays it). */
    fun nowPlaying(title: String, artist: String, album: String, durationMs: Long, source: ListenHistory.Source) {
        if (!on || !wanted(source) || title.isBlank() || artist.isBlank()) return
        scope.launch {
            runCatching { lastFmKey?.let { sk -> lastFm("track.updateNowPlaying", sk, track(title, artist, album, durationMs)) } }
            runCatching { lbToken?.let { listenBrainz("playing_now", JSONArray().put(JSONObject().put("track_metadata", meta(title, artist, album, durationMs)))) } }
        }
    }

    /** A song ended or was left after [playedMs] of listening; it started at [startedAtMs] (wall clock). */
    fun finished(title: String, artist: String, album: String, durationMs: Long, playedMs: Long, startedAtMs: Long, source: ListenHistory.Source) {
        if (!on || !wanted(source) || title.isBlank() || artist.isBlank()) return
        if (!counts(durationMs, playedMs)) return
        val s = JSONObject().put("title", title).put("artist", artist).put("album", album).put("ms", durationMs).put("at", startedAtMs / 1000)
        scope.launch { sending.withLock { queue(s); flush() } }
    }

    /** Last.fm's rule: a song over 30 s, played for half its length or 4 minutes. */
    internal fun counts(durationMs: Long, playedMs: Long): Boolean {
        if (durationMs in 1 until MIN_SONG_MS) return false
        val needed = if (durationMs > 0) minOf(durationMs / 2, ENOUGH_MS) else ENOUGH_MS
        return playedMs >= needed
    }

    private fun wanted(source: ListenHistory.Source) = includeSpotify || source != ListenHistory.Source.SPOTIFY

    private fun queue(s: JSONObject) {
        val list = pending()
        list.put(s)
        while (list.length() > MAX_PENDING) list.remove(0)
        prefs().edit().putString("pending", list.toString()).apply()
    }

    private fun pending(): JSONArray = runCatching { JSONArray(prefs().getString("pending", "[]")) }.getOrDefault(JSONArray())

    /** Sends what is waiting; what fails stays for the next time. Holds [sending]. */
    private fun flush() {
        val list = pending()
        if (list.length() == 0) return
        val left = JSONArray()
        // Last.fm takes up to 50 scrobbles a request, ListenBrainz many: both sent in batches of 50.
        for (start in 0 until list.length() step 50) {
            val batch = (start until minOf(list.length(), start + 50)).map { list.getJSONObject(it) }
            val fmOk = lastFmKey?.let { sk -> runCatching { scrobbleLastFm(sk, batch) }.getOrDefault(false) } ?: true
            val lbOk = lbToken?.let { runCatching { scrobbleListenBrainz(batch) }.getOrDefault(false) } ?: true
            // Kept only for a service that failed; a resend to one that took it would show twice there.
            if (!fmOk || !lbOk) batch.forEach { left.put(it.put("fm", !fmOk).put("lb", !lbOk)) }
        }
        prefs().edit().putString("pending", left.toString()).apply()
    }

    private fun scrobbleLastFm(sk: String, batch: List<JSONObject>): Boolean {
        val items = batch.filter { it.optBoolean("fm", true) }
        if (items.isEmpty()) return true
        val params = sortedMapOf<String, String>()
        items.forEachIndexed { i, s ->
            params["artist[$i]"] = s.getString("artist"); params["track[$i]"] = s.getString("title")
            params["timestamp[$i]"] = s.getLong("at").toString()
            s.optString("album").takeIf { it.isNotBlank() }?.let { params["album[$i]"] = it }
            s.optLong("ms").takeIf { it > 0 }?.let { params["duration[$i]"] = (it / 1000).toString() }
        }
        return lastFm("track.scrobble", sk, params) != null
    }

    private fun scrobbleListenBrainz(batch: List<JSONObject>): Boolean {
        val items = batch.filter { it.optBoolean("lb", true) }
        if (items.isEmpty()) return true
        val payload = JSONArray()
        items.forEach { s -> payload.put(JSONObject().put("listened_at", s.getLong("at"))
            .put("track_metadata", meta(s.getString("title"), s.getString("artist"), s.optString("album"), s.optLong("ms")))) }
        return listenBrainz(if (items.size == 1) "single" else "import", payload)
    }

    private fun track(title: String, artist: String, album: String, durationMs: Long) = sortedMapOf("artist" to artist, "track" to title).apply {
        if (album.isNotBlank()) put("album", album)
        if (durationMs > 0) put("duration", (durationMs / 1000).toString())
    }

    private fun meta(title: String, artist: String, album: String, durationMs: Long) = JSONObject()
        .put("artist_name", artist).put("track_name", title).apply { if (album.isNotBlank()) put("release_name", album) }
        .put("additional_info", JSONObject().put("submission_client", "Myspoti").put("submission_client_version", BuildConfig.VERSION_NAME)
            .apply { if (durationMs > 0) put("duration_ms", durationMs) })

    // ── ListenBrainz ──

    private fun listenBrainz(type: String, payload: JSONArray): Boolean {
        val token = lbToken ?: return false
        val body = JSONObject().put("listen_type", type).put("payload", payload).toString()
        val req = Request.Builder().url(LISTENBRAINZ + "submit-listens").header("Authorization", "Token $token")
            .post(body.toRequestBody("application/json".toMediaType())).build()
        return http.newCall(req).execute().use { it.isSuccessful }
    }

    /** Checks [token] with ListenBrainz and keeps it when valid; the user name, or null when refused. */
    suspend fun connectListenBrainz(token: String): String? = withContext(Dispatchers.IO) {
        val t = token.trim().takeIf { it.isNotEmpty() } ?: return@withContext null
        val req = Request.Builder().url(LISTENBRAINZ + "validate-token").header("Authorization", "Token $t").build()
        val user = runCatching {
            http.newCall(req).execute().use { r -> JSONObject(r.body.string()).takeIf { it.optBoolean("valid") }?.optString("user_name") }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@withContext null
        lbToken = t; listenBrainzUser = user
        prefs().edit().putString("lb_token", t).putString("lb_user", user).apply()
        user
    }

    fun disconnectListenBrainz() {
        lbToken = null; listenBrainzUser = null
        prefs().edit().remove("lb_token").remove("lb_user").apply()
    }

    // ── Last.fm ──

    /** Step 1: a token from Last.fm, and the page where the user approves it (opened by the caller). */
    suspend fun lastFmAuthUrl(): String? = withContext(Dispatchers.IO) {
        if (!lastFmAvailable) return@withContext null
        val token = lastFm("auth.getToken", null, sortedMapOf())?.optString("token")?.takeIf { it.isNotBlank() } ?: return@withContext null
        pendingAuthToken = token
        "https://www.last.fm/api/auth/?api_key=${BuildConfig.LASTFM_API_KEY}&token=$token"
    }

    /** Step 2, after the user approved in the browser: the session. The user name, or null when not approved (yet). */
    suspend fun finishLastFm(): String? = withContext(Dispatchers.IO) {
        val token = pendingAuthToken ?: return@withContext null
        val session = lastFm("auth.getSession", null, sortedMapOf("token" to token))?.optJSONObject("session") ?: return@withContext null
        val key = session.optString("key").takeIf { it.isNotBlank() } ?: return@withContext null
        val name = session.optString("name")
        pendingAuthToken = null
        lastFmKey = key; lastFmUser = name
        prefs().edit().putString("lastfm_key", key).putString("lastfm_user", name).apply()
        name
    }

    fun disconnectLastFm() {
        lastFmKey = null; lastFmUser = null; pendingAuthToken = null
        prefs().edit().remove("lastfm_key").remove("lastfm_user").apply()
    }

    /** A signed Last.fm call (POST, JSON answer); null on any error. */
    private fun lastFm(method: String, sk: String?, params: java.util.SortedMap<String, String>): JSONObject? {
        if (!lastFmAvailable) return null
        val all = java.util.TreeMap(params).apply {
            put("method", method); put("api_key", BuildConfig.LASTFM_API_KEY); sk?.let { put("sk", it) }
        }
        val sig = md5(all.entries.joinToString("") { it.key + it.value } + BuildConfig.LASTFM_SECRET)
        val form = FormBody.Builder().apply { all.forEach { (k, v) -> add(k, v) }; add("api_sig", sig); add("format", "json") }.build()
        val req = Request.Builder().url(LASTFM).post(form).build()
        return http.newCall(req).execute().use { r ->
            val o = runCatching { JSONObject(r.body.string()) }.getOrNull()
            if (!r.isSuccessful || o == null || o.has("error")) {
                // 9: the session was revoked on Last.fm's side; signed out here too.
                if (o?.optInt("error") == 9) disconnectLastFm()
                null
            } else o
        }
    }

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
