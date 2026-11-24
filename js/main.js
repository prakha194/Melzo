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

  // ---- screenshot slider: swipe, arrows, dots, autoplay ----
  var track = document.getElementById("shots-track");
  if (track) {
    var slides = Array.prototype.slice.call(track.querySelectorAll(".slide"));
    var dotsBox = document.getElementById("shots-dots");
    var slider = document.getElementById("shots-slider");
    var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    var timer = null;
    var paused = false;

    function step() {
      if (slides.length < 2) return slides[0] ? slides[0].offsetWidth : 0;
      return slides[1].offsetLeft - slides[0].offsetLeft;
    }

    function current() {
      return Math.round(track.scrollLeft / step()) || 0;
    }

    function goTo(i) {
      var n = slides.length;
      var target = ((i % n) + n) % n;
      track.scrollTo({ left: target * step(), behavior: reduce ? "auto" : "smooth" });
    }

    if (dotsBox) {
      slides.forEach(function (_, i) {
        var b = document.createElement("button");
        b.type = "button";
        b.className = "dot";
        b.setAttribute("aria-label", "Screenshot " + (i + 1));
        b.addEventListener("click", function () {
          goTo(i);
          restart();
        });
        dotsBox.appendChild(b);
      });
    }

    function paintDots() {
      if (!dotsBox) return;
      var i = current();
      Array.prototype.forEach.call(dotsBox.children, function (d, k) {
        d.classList.toggle("on", k === i);
      });
    }

    var scrollTimer = null;
    track.addEventListener("scroll", function () {
      clearTimeout(scrollTimer);
      scrollTimer = setTimeout(paintDots, 90);
    }, { passive: true });

    var prev = slider && slider.querySelector(".slider-prev");
    var next = slider && slider.querySelector(".slider-next");
    if (prev) prev.addEventListener("click", function () { goTo(current() - 1); restart(); });
    if (next) next.addEventListener("click", function () { goTo(current() + 1); restart(); });

    track.addEventListener("keydown", function (e) {
      if (e.key === "ArrowRight") { e.preventDefault(); goTo(current() + 1); restart(); }
      if (e.key === "ArrowLeft") { e.preventDefault(); goTo(current() - 1); restart(); }
    });

    function start() {
      if (reduce || timer) return;
      timer = setInterval(function () {
        if (paused || document.hidden) return;
        goTo(current() + 1);
      }, 4200);
    }
    function stop() {
      clearInterval(timer);
      timer = null;
    }
    function restart() {
      stop();
      start();
    }

    slider && slider.addEventListener("pointerenter", function () { paused = true; });
    slider && slider.addEventListener("pointerleave", function () { paused = false; });
    track.addEventListener("focusin", function () { paused = true; });
    track.addEventListener("focusout", function () { paused = false; });

    paintDots();
    start();
  }

  // ---- copy the extension address ----
  Array.prototype.forEach.call(document.querySelectorAll(".copy-btn"), function (btn) {
    btn.addEventListener("click", function () {
      var text = btn.getAttribute("data-copy") || "";
      var done = function () {
        var old = btn.textContent;
        btn.textContent = "Copied";
        btn.classList.add("copied");
        setTimeout(function () {
          btn.textContent = old;
          btn.classList.remove("copied");
        }, 1600);
      };
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(done, fallback);
      } else {
        fallback();
      }
      function fallback() {
        var ta = document.createElement("textarea");
        ta.value = text;
        ta.setAttribute("readonly", "");
        ta.style.position = "absolute";
        ta.style.left = "-9999px";
        document.body.appendChild(ta);
        ta.select();
        try { document.execCommand("copy"); done(); } catch (e) { /* leave the text visible */ }
        document.body.removeChild(ta);
      }
    });
  });
})();
