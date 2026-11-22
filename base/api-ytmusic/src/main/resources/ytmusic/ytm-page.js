/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Orpheon page script for music.youtube.com (the official web app), injected by the host after each
 * page load (Android WebView or desktop Chrome over CDP). It only tells the host what the web app
 * says about itself (signed in, its client version, language and country), for the app's own API
 * requests. It plays nothing, and touches neither the page's player nor its ads.
 *
 * Page -> host: one JSON string per message, to window.MyspotiHost.postMessage (Android) or
 *   window.__mspHostPost (CDP binding): {type: "ready", page, signedIn, clientVersion, hl, gl}
 *   | {type: "result", id, ok, value | error}
 */
(() => {
  if (window.MyspotiPage && window.MyspotiPage.version) {
    if (window.__mspReannounce) { window.__mspReannounce = false; window.MyspotiPage.announce(); }
    return;
  }
  const gen = window.__mspPageGen;

  const post = (m) => {
    const s = JSON.stringify(m);
    try {
      if (window.MyspotiHost && typeof window.MyspotiHost.postMessage === 'function') window.MyspotiHost.postMessage(s);
      else if (typeof window.__mspHostPost === 'function') window.__mspHostPost(s);
    } catch (e) { /* the host went away */ }
  };

  const cfg = (k) => {
    try { return window.ytcfg && typeof window.ytcfg.get === 'function' ? window.ytcfg.get(k) : undefined; } catch (e) { return undefined; }
  };

  const announce = () => post({
    type: 'ready', page: gen, signedIn: cfg('LOGGED_IN') === true,
    clientVersion: cfg('INNERTUBE_CLIENT_VERSION') || null, hl: cfg('HL') || null, gl: cfg('GL') || null,
  });

  window.MyspotiPage = {
    version: 2,
    announce,
    call: (id, method) => post({ type: 'result', id, ok: false, error: 'unknown method ' + method }),
  };

  // Ready once the app's configuration is there.
  const wait = setInterval(() => { if (cfg('INNERTUBE_CLIENT_VERSION') !== undefined) { clearInterval(wait); announce(); } }, 250);
})();
