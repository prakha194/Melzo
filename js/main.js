// Melzo — official website
(function () {
  "use strict";

  // ---- mobile menu ----
  var burger = document.querySelector(".burger");
  var links = document.getElementById("nav-links");
  if (burger && links) {
    burger.addEventListener("click", function () {
      var open = links.classList.toggle("open");
      burger.setAttribute("aria-expanded", open ? "true" : "false");
    });
    links.addEventListener("click", function (e) {
      if (e.target.tagName === "A") links.classList.remove("open");
    });
  }

  // ---- latest APK, straight from the release ----
  var REPO = "prakha194/Melzo";
  var dlBtn = document.getElementById("dl-btn");
  var dlVer = document.getElementById("dl-version");
  var dlNote = document.getElementById("dl-note");

  fetch("https://api.github.com/repos/" + REPO + "/releases/latest", {
    headers: { Accept: "application/vnd.github+json" }
  })
    .then(function (r) {
      if (!r.ok) throw new Error("GitHub answered " + r.status);
      return r.json();
    })
    .then(function (rel) {
      if (!rel || !rel.tag_name) return;
      var apk = (rel.assets || []).filter(function (a) {
        return /\.apk$/i.test(a.name);
      })[0];
      if (dlVer) dlVer.textContent = rel.tag_name;
      if (apk) {
        if (dlBtn) dlBtn.href = apk.browser_download_url;
        if (dlNote) dlNote.textContent = Math.round(apk.size / 1048576) + " MB";
      }
    })
    .catch(function () {
      /* keep the static version and link already in the page */
    });
})();
