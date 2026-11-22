package com.music.spotui.connect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class ZeroconfTest {
    private val sonosInfo = """{"status":101,"statusString":"OK","spotifyError":0,"version":"2.7.1","deviceID":"abc123","remoteName":"Kitchen",
        "activeUser":"","brandDisplayName":"Sonos","modelDisplayName":"One","clientID":"client9","tokenType":"authorization_code"}"""

    @Test fun pathAlwaysStartsWithOneSlash() {
        assertEquals("/", Zeroconf.path(null))
        assertEquals("/", Zeroconf.path(""))
        assertEquals("/spotifyzc", Zeroconf.path("/spotifyzc"))
        assertEquals("/zc", Zeroconf.path(" zc "))
    }

    @Test fun getInfoIsParsed() {
        val i = Zeroconf.parseInfo(sonosInfo)!!
        assertEquals("abc123", i.deviceId)
        assertEquals("Kitchen", i.name)
        assertEquals("client9", i.clientId)
        assertEquals("authorization_code", i.tokenType)
    }

    @Test fun anAnswerWithoutIdsOrWithAnErrorIsNotAnInfo() {
        assertNull(Zeroconf.parseInfo("""{"status":102,"statusString":"ERROR","deviceID":"x"}"""))
        assertNull(Zeroconf.parseInfo("""{"status":101}"""))
        assertNull(Zeroconf.parseInfo("not json"))
    }

    @Test fun aDeviceStillStartingIsSeen() {
        val i = Zeroconf.parseInfo("""{"status":101,"deviceID":"d","clientID":"c","tokenType":"authorization_code","availability":"NOT-LOADED","publicKey":"SU5WQUxJRA==","spotifyError":0}""")!!
        assertEquals("NOT-LOADED", i.availability)
        assertEquals(Zeroconf.INVALID_KEY, i.publicKey)
        assertTrue(i.starting)
        assertFalse(i.copy(availability = "", publicKey = "abc").starting)
        assertTrue(i.copy(availability = "").starting)
        assertEquals(0, Zeroconf.parseInfo(sonosInfo)!!.spotifyError)
    }

    @Test fun responsesAreReadWithAndWithoutChunks() {
        val plain = "HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhello".toByteArray()
        assertEquals(200 to "hello", Zeroconf.parseResponse(plain))
        val chunked = "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nhel\r\n2\r\nlo\r\n0\r\n\r\n".toByteArray()
        assertEquals(200 to "hello", Zeroconf.parseResponse(chunked))
        assertNull(Zeroconf.parseResponse("garbage".toByteArray()))
    }

    @Test fun anAnswerIsCompleteOnceItsLengthArrived() {
        assertFalse(Zeroconf.complete("HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhel".toByteArray()))
        assertTrue(Zeroconf.complete("HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhello".toByteArray()))
        assertFalse(Zeroconf.complete("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nhel\r\n".toByteArray()))
        assertTrue(Zeroconf.complete("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nhel\r\n0\r\n\r\n".toByteArray()))
    }

    @Test fun onlyLocalAddressesAreTrusted() {
        for (a in listOf("192.168.1.20", "10.0.0.7", "172.16.4.2", "169.254.3.3", "fd12:3456::1")) assertTrue(a, Zeroconf.isLan(InetAddress.getByName(a)))
        for (a in listOf("8.8.8.8", "127.0.0.1", "2001:4860:4860::8888")) assertFalse(a, Zeroconf.isLan(InetAddress.getByName(a)))
    }

    @Test fun aDeviceIsKnownByIdOrByName() {
        val i = Zeroconf.parseInfo(sonosInfo)!!
        assertTrue(Zeroconf.inConnectList(i, setOf("abc123"), emptySet()))
        assertTrue(Zeroconf.inConnectList(i, emptySet(), setOf("Kitchen")))
        assertFalse(Zeroconf.inConnectList(i, setOf("zzz"), setOf("Bedroom")))
    }
}
