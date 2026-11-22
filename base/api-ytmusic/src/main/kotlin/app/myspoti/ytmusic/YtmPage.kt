/* SPDX-License-Identifier: GPL-3.0-only */
package app.myspoti.ytmusic

import app.myspoti.web.PageBridge
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * The official web app in the hidden page, as its page script (`ytm-page.js`) describes it: signed
 * in or not, its client version, language and country (for the API's requests). Nothing plays there.
 */
class YtmPage(val bridge: PageBridge) {
    /** The page says the account is signed in (its own configuration), null before the page is up. */
    val signedIn: Boolean? get() = bridge.info.value?.let { (it["signedIn"] as? JsonPrimitive)?.booleanOrNull }

    /** The web app's version and language as the page says them (for the API's requests). */
    fun pageInfo(key: String): String? = (bridge.info.value?.get(key) as? JsonPrimitive)?.contentOrNull

    companion object {
        /** The page script, bundled as a resource of this module. */
        val SCRIPT: String by lazy {
            YtmPage::class.java.getResourceAsStream("/ytmusic/ytm-page.js")!!.bufferedReader().use { it.readText() }
        }
    }
}
