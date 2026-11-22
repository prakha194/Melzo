package app.myspoti.host.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticDigestTest {
    @Test fun `tokens, accounts, e-mails and addresses are masked`() {
        val line = """registration: Authorization=Bearer BQDx1y2z.abc-DEF user spotify:user:ytdgg4jsixgf6n3nd44dpz3j7 """ +
            """mail jane.doe+x@example.co.uk {"username":"jane","displayName":"Jane D","name":"Halo"} """ +
            """device 62623a152cc9c682fe53263500ad6201eee30a9c audio?__token__=exp=1792855998~hmac=4fc1 """ +
            """from 85.12.34.56 and 2a01:e0a:1f2:3c40::1 UA Chrome/153.0.0.0 track spotify:track:76hCWqC3QR1e6ny32DYoAO"""
        val masked = DiagnosticDigest.mask(line)
        assertFalse(masked, "BQDx1y2z" in masked)
        assertFalse(masked, "ytdgg4" in masked)
        assertFalse(masked, "jane.doe" in masked || "\"jane\"" in masked || "Jane D" in masked)
        assertFalse(masked, "62623a152cc9c682fe53263500ad6201eee30a9c" in masked)
        assertFalse(masked, "1792855998" in masked)
        assertFalse(masked, "85.12.34.56" in masked || "e0a:1f2" in masked)
        // What the reader needs stays: the song, the browser version, the address families.
        assertTrue(masked, "\"name\":\"Halo\"" in masked)
        assertTrue(masked, "spotify:track:76hCWqC3QR1e6ny32DYoAO" in masked)
        assertTrue(masked, "Chrome/153.0.0.0" in masked)
        assertTrue(masked, "85.x.x.x" in masked && "2a01:…" in masked)
    }

    @Test fun `identical lines in a row are kept once with their count`() {
        val out = DiagnosticDigest.digest(listOf(
            "10:00:00.000 page state: \"Halo\" paused=false buffering=false",
            "10:00:00.100 page state: \"Halo\" paused=false buffering=false",
            "10:00:00.200 page state: \"Halo\" paused=false buffering=false",
            "10:00:01.000 call #8 player.resume {}",
        ))
        assertEquals(listOf(
            "10:00:00.000 page state: \"Halo\" paused=false buffering=false (×3)",
            "10:00:01.000 call #8 player.resume {}",
        ), out.trim().lines())
    }

    @Test fun `polling and look-ups go with their answers, failures stay`() {
        val out = DiagnosticDigest.digest(listOf(
            "10:00:00.000 call #4 player.state {}",
            "10:00:00.010 call #5 service.call {\"service\":\"ConnectDevicesAPI\",\"method\":\"getDevices\",\"args\":[]}",
            "10:00:00.020 call #4 player.state ok in 7 ms: null",
            "10:00:00.030 call #5 service.call ok in 4 ms: [{\"id\":\"x\"}]",
            "10:00:00.040 console WARNING: The resource https://encore.scdn.co/fonts/a.woff2 was preloaded using link preload but not used",
            "10:00:00.050 page net: {\"type\":\"net\",\"url\":\"https://apresolve.spotify.com/\",\"status\":200}",
            "10:00:00.060 page net: {\"type\":\"net\",\"url\":\"https://apresolve.spotify.com/\",\"status\":503}",
            "10:00:00.070 call #6 player.queue FAILED after 20000 ms: timeout",
            "10:00:00.080 relay POST /connect-state/v1/player/command/from/…/to/self -> 402 in 300 ms",
        )).trim().lines()
        assertEquals(out.joinToString("\n"), 3, out.size)
        assertTrue(out[0].contains("\"status\":503"))
        assertTrue(out[1].contains("FAILED"))
        assertTrue(out[2].contains("-> 402"))
    }

    @Test fun `each kind is capped, the cut is counted, and the newest lines fit the size`() {
        val lines = (1..300).map { "10:00:${(it % 60).toString().padStart(2, '0')}.000 relay PUT /track-playback/v1/devices/…/state -> 200 in $it ms" }
        val out = DiagnosticDigest.digest(lines, perKind = 50)
        assertEquals(50, out.lines().count { it.startsWith("10:") })
        assertTrue(out.takeLast(300), out.contains("250 × relay PUT"))
        val small = DiagnosticDigest.digest((1..400).map { "10:00:00.000 event $it ${"x".repeat(100)}" }, perKind = 1000, maxBytes = 5_000)
        assertTrue("size ${small.length}", small.length <= 5_100)
        assertTrue("the newest line is kept", small.contains("event 400 "))
        assertTrue("the cut is said", small.startsWith("("))
    }
}
