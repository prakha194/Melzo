/* SPDX-License-Identifier: GPL-3.0-only */
package app.myspoti.ytmusic

import app.myspoti.web.PageBridge
import app.myspoti.web.PageHost
import app.myspoti.web.SessionCookies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient

/**
 * YouTube Music as the user's account provider, on one hidden page ([host], with [cookies], the
 * session the user signed in to in a WebView of the official site): the page and its script, the
 * web app's API on that session, the library read and the metadata.
 */
class YtmAccount(
    val host: PageHost,
    private val cookies: SessionCookies,
    http: OkHttpClient,
    scope: CoroutineScope,
    language: () -> String = { "en" },
    country: () -> String = { "US" },
) {
    val bridge = PageBridge(host, YtmPage.SCRIPT, scope)
    val page = YtmPage(bridge)
    val api = InnerTube(
        http, cookies = { cookies.header(Ytm.ORIGIN) },
        clientVersion = { page.pageInfo("clientVersion") ?: InnerTube.DEFAULT_CLIENT_VERSION },
        language = { page.pageInfo("hl") ?: language() }, country = { page.pageInfo("gl") ?: country() },
    )
    val library = YtmLibrary(api)
    val metadata = YtmMetadata(api)

    private val _signedIn = MutableStateFlow(false)
    /** The session holds a signed-in account (its auth cookie), as last read ([refreshSignIn]). */
    val signedIn: StateFlow<Boolean> = _signedIn

    /** Reads the session's cookies again: true when an account is signed in. */
    suspend fun refreshSignIn(): Boolean = Ytm.signedIn(cookies.header(Ytm.ORIGIN)).also { _signedIn.value = it }

    /** Loads the web app's home in the hidden page (nothing plays there). */
    suspend fun open() = host.navigate(Ytm.HOME_URL)

    /** Signs out: the session's cookies are deleted, the page is loaded again signed out. */
    suspend fun signOut() {
        cookies.clear()
        _signedIn.value = false
        host.navigate(Ytm.HOME_URL)
    }
}
