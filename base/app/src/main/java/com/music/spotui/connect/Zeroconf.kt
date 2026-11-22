package com.music.spotui.connect

import org.json.JSONObject
import java.net.InetAddress

/** What a Spotify Connect device says about itself on its ZeroConf port (`getInfo`). */
data class ZeroconfInfo(
    val deviceId: String,
    val name: String,
    val clientId: String,
    /** How it wants to be logged in: "accesstoken" (a token Spotify issues), "default" (stored credentials), "authorization_code" (Sonos). */
    val tokenType: String,
    val version: String,
    /** The account logged in on it now ("" when none). Never logged. */
    val activeUser: String,
    val brand: String,
    val model: String,
    /** "NOT-LOADED" while its Spotify part is still starting (Sonos after a wake), else "" or "AVAILABLE". */
    val availability: String = "",
    /** Its Diffie-Hellman key; [Zeroconf.INVALID_KEY] while it is still starting. */
    val publicKey: String = "",
    /** Spotify's own error code on the device (0: none), for the log. */
    val spotifyError: Int = 0,
) {
    /** Still starting (its Spotify part not loaded yet). */
    val starting: Boolean get() = availability == "NOT-LOADED" || publicKey == Zeroconf.INVALID_KEY
}

/**
 * Spotify Connect's ZeroConf API, the part that only reads: a speaker advertises
 * `_spotify-connect._tcp` (mDNS) with its port and a `CPath` and answers `getInfo`. The app never
 * logs a speaker in (no `addUser`). Pure functions: the sockets are in [LanDiscovery.call].
 */
object Zeroconf {
    const val SERVICE_TYPE = "_spotify-connect._tcp"
    /** The public key of a device whose Spotify part is not loaded yet ("INVALID" in base64). */
    const val INVALID_KEY = "SU5WQUxJRA=="
    /** The ZeroConf version announced to the device (the one reported working with accesstoken devices). */
    const val VERSION = "2.7.1"

    /** The `CPath` of the service's TXT record as a path ("" and "/" both mean the root). */
    fun path(cpath: String?): String = "/" + (cpath ?: "").trim().trimStart('/')

    fun infoRequest(host: String, port: Int, cpath: String?): ByteArray =
        "GET ${path(cpath)}?action=getInfo&version=$VERSION HTTP/1.1\r\nHost: $host:$port\r\nAccept: */*\r\nConnection: close\r\n\r\n"
            .toByteArray(Charsets.UTF_8)

    /** The device's getInfo answer, or null when it is not one (an error status, not JSON, no device id). */
    fun parseInfo(body: String): ZeroconfInfo? {
        val j = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (j.has("status") && j.optInt("status") != 101) return null
        val id = j.optString("deviceID").ifBlank { return null }
        return ZeroconfInfo(
            deviceId = id,
            name = j.optString("remoteName"),
            clientId = j.optString("clientID"),
            tokenType = j.optString("tokenType"),
            version = j.optString("version"),
            activeUser = j.optString("activeUser"),
            brand = j.optString("brandDisplayName"),
            model = j.optString("modelDisplayName"),
            availability = j.optString("availability"),
            publicKey = j.optString("publicKey"),
            spotifyError = j.optInt("spotifyError", 0),
        )
    }

    /** The status and the body of an HTTP/1.1 answer read to its end (chunked included), or null when it is not one. */
    fun parseResponse(raw: ByteArray): Pair<Int, String>? {
        val end = indexOf(raw, "\r\n\r\n".toByteArray(), 0)
        if (end < 0) return null
        val head = String(raw, 0, end, Charsets.ISO_8859_1).split("\r\n")
        val status = head.firstOrNull()?.split(' ')?.getOrNull(1)?.toIntOrNull() ?: return null
        val chunked = head.any { it.startsWith("Transfer-Encoding:", true) && it.contains("chunked", true) }
        val bytes = if (chunked) dechunk(raw, end + 4) else raw.copyOfRange(end + 4, raw.size)
        return status to String(bytes, Charsets.UTF_8)
    }

    /** [raw] holds a whole answer already (the device may keep the connection open after it, whatever `Connection: close` says). */
    fun complete(raw: ByteArray): Boolean {
        val end = indexOf(raw, "\r\n\r\n".toByteArray(), 0)
        if (end < 0) return false
        val head = String(raw, 0, end, Charsets.ISO_8859_1).split("\r\n")
        if (head.any { it.startsWith("Transfer-Encoding:", true) && it.contains("chunked", true) })
            return indexOf(raw, "\r\n0\r\n".toByteArray(), end) >= 0 || (raw.size >= end + 7 && raw[end + 4] == '0'.code.toByte())
        val length = head.firstNotNullOfOrNull { l -> l.takeIf { it.startsWith("Content-Length:", true) }?.substringAfter(':')?.trim()?.toIntOrNull() } ?: return false
        return raw.size - (end + 4) >= length
    }

    private fun dechunk(raw: ByteArray, from: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var at = from
        while (at < raw.size) {
            val eol = indexOf(raw, "\r\n".toByteArray(), at)
            if (eol < 0) break
            val size = String(raw, at, eol - at, Charsets.ISO_8859_1).substringBefore(';').trim().toIntOrNull(16) ?: break
            if (size == 0) break
            val start = eol + 2
            out.write(raw, start, minOf(size, raw.size - start))
            at = start + size + 2
        }
        return out.toByteArray()
    }

    private fun indexOf(a: ByteArray, pattern: ByteArray, from: Int): Int {
        outer@ for (i in from..a.size - pattern.size) {
            for (k in pattern.indices) if (a[i + k] != pattern[k]) continue@outer
            return i
        }
        return -1
    }

    /**
     * An address on the local network (private IPv4, link-local, IPv6 unique local): the only ones
     * a token is ever sent to, so a service advertising a public address is not offered.
     */
    fun isLan(address: InetAddress): Boolean {
        if (address.isLoopbackAddress) return false
        if (address.isSiteLocalAddress || address.isLinkLocalAddress) return true
        val b = address.address
        return b.size == 16 && (b[0].toInt() and 0xFE) == 0xFC
    }

    /** [info] is one of the account's Connect devices already: by the id Spotify gives it, else by its name. */
    fun inConnectList(info: ZeroconfInfo, ids: Set<String>, names: Set<String>): Boolean =
        info.deviceId in ids || (info.name.isNotBlank() && info.name in names)
}
