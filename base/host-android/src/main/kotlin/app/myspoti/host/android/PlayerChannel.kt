package app.myspoti.host.android

import android.webkit.JavascriptInterface

/**
 * A hidden page's channel to the app ([HiddenPageHost]), where the interface object is
 * reachable from every frame. A message is only taken when it starts with the page's secret:
 * a random token, new at each page start ([newPage]), that [shim] hands to the main frame's
 * script only. The shim replaces `window.<name>` in the top document by a wrapper that keeps the
 * raw interface and the token in its closure, so the scripts of the app go on calling
 * `postMessage(json)` unchanged; a frame of another origin cannot read the top document's
 * closure and its messages carry no valid token. Refused messages are dropped and counted in the log.
 */
internal class PlayerChannel(private val name: String, private val onMessage: (String) -> Unit) {
    @Volatile private var token = ""
    private val refused = java.util.concurrent.atomic.AtomicInteger()

    /** A fresh secret for the page that starts now; the previous one stops working. */
    fun newPage() {
        token = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    }

    /** The script that installs the wrapper in the top document with the current secret; harmless twice. */
    fun shim(): String = """
        (() => {
          if (window !== window.top) return;
          const raw = window.$name;
          if (!raw || raw.__msp) return;
          const wrapper = { __msp: true, postMessage: (m) => raw.postMessage('$token.' + m) };
          try { Object.defineProperty(window, '$name', { value: wrapper }); } catch (e) { window.$name = wrapper; }
        })();
    """.trimIndent()

    @JavascriptInterface
    fun postMessage(message: String) {
        val t = token
        if (t.isEmpty() || message.length <= t.length || !message.startsWith(t) || message[t.length] != '.') {
            val n = refused.incrementAndGet()
            Logbook.logEvery("channel-refused-$name", 60_000, "$name message without the page's secret ignored ($n so far)")
            return
        }
        onMessage(message.substring(t.length + 1))
    }
}
