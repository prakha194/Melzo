package app.myspoti.extensions

/** Spotify's servers: never open to extensions, whatever their manifest lists (network or media hosts). */
object SpotifyHosts {
    private val DOMAINS = listOf("spotify.com", "spotifycdn.com", "scdn.co", "spotify.net", "spotify.link", "spotilocal.com")

    fun isSpotify(host: String): Boolean {
        val h = host.lowercase().trimEnd('.')
        return DOMAINS.any { h == it || h.endsWith(".$it") }
    }
}
