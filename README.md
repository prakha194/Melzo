# Myspoti

[![Release](https://img.shields.io/github/v/release/Kirisos-Guna/myspoti)](https://github.com/Kirisos-Guna/myspoti/releases/latest)
[![License: GPL-3.0](https://img.shields.io/github/license/Kirisos-Guna/myspoti)](LICENSE)
[![Stars](https://img.shields.io/github/stars/Kirisos-Guna/myspoti?style=social)](https://github.com/Kirisos-Guna/myspoti/stargazers)

**A free, open-source music player for Android, Android TV and Wear OS that uses your own Spotify account.**

No ads added by the app, no tracking, nothing sent to the developer. A free Spotify account is enough.

<p align="center">
  <a href="https://github.com/Kirisos-Guna/myspoti/releases/latest"><b>⬇ Download the latest release</b></a>
</p>

> Myspoti is a fork of [Myspoti / Orpheon](https://github.com/kota-zo/myspoti) by kota-zo and its contributors, released under GPL-3.0. See [Credits](#credits).

---

## How it works

You sign in on Spotify's own login page, so the app never sees your password. Behind the scenes, Spotify's official Web Player plays the music in a hidden view, using your phone's DRM. Myspoti draws its own native screens on top and controls it.

Because playback goes through Spotify's own player, your plays are reported to Spotify just like in a browser. They count for the artists, in your listening history and in your Wrapped.

## Features

**Listening**
- Home, Search, Browse, New releases, Your Library (with folders), and artist, album, playlist and profile pages
- Shuffle, Smart Shuffle, repeat modes, crossfade, sleep timer and a drag-to-reorder queue
- Podcasts and audiobooks with their own player (speed, ±15 s skip, mark as played)
- Spotify Connect, Jam, Bluetooth, lock screen, notification, home-screen widget and Android Auto
- 10-band equalizer, playback speed and "Stats for nerds" for downloaded, local and extension songs

**Lyrics**
- Synced lyrics, word by word or line by line, with translation
- Song meanings from Genius

**Playlists**
- Create, edit, sort, and make playlists public, private or collaborative
- Add songs from an in-playlist search with suggestions
- Share anything as a link or a scannable Spotify Code

**Offline and local**
- Offline downloads through a download-source extension you install (encrypted on your phone)
- Downloaded playlists stay in sync with Spotify
- Plays your own local audio files

**Extensions**
- Sandboxed community scripts: extra lyrics sources, audio and download sources, Home sections and tools
- Before installing, you see every permission an extension asks for and why
- [User guide](docs/extensions/GUIDE.md) · [API for authors](docs/extensions/API.md)

**And more**
- Two looks: Classic and Immersive
- A private listening recap that stays on your phone
- Optional Live Radio from the [Radio Browser](https://www.radio-browser.info) directory
- 69 languages, data saver, battery saver, AMOLED black and accent colours

## Install

1. Download an APK from the [latest release](https://github.com/Kirisos-Guna/myspoti/releases/latest). Android 10 or later is required.
2. Open it. Android asks once to allow installs from your browser or file manager.
3. Tap **Log in with Spotify**, sign in, pick your sound quality, and you're done.

| File | For |
|---|---|
| `Myspoti-<version>-ARMv8.apk` | Most phones from the last ten years (64-bit). **Pick this one if unsure.** |
| `Myspoti-<version>-ARMv7.apk` | Older or entry-level 32-bit phones |
| `Myspoti-<version>-universal.apk` | Both of the above in one larger file |
| `Myspoti-<version>-TV.apk` | Android TV and Google TV |

New versions are offered inside the app: one tap downloads and installs them.

## Privacy

- **Spotify** receives your plays, as with any Spotify client.
- **Lyrics sites, Genius and Google Translate** are queried for lyrics, song meanings and translations.
- **Extensions** contact only the hosts they list, shown to you before you install them.
- **Live Radio**, if you turn it on, queries Radio Browser and each station's own server.
- **Nothing is sent to the developer.**

## FAQ

**Is it safe for my account?**
Myspoti talks to Spotify the same way the Web Player does, and you sign in on Spotify's own page. Ads play as Spotify sends them. See the [disclaimer](#disclaimer).

**Spotify refused to play a song. What can I do?**
Some new or free accounts are refused by Spotify. Go to Settings → **Export a diagnostic report** and attach it to a [GitHub issue](https://github.com/Kirisos-Guna/myspoti/issues). The report contains the app's recent log and your phone model, never your password or login tokens.

**Do downloaded songs count in my Wrapped?**
No. Downloaded songs play from your phone and aren't reported to Spotify. They still count in Myspoti's own recap. Streamed songs count as usual.

**Does Myspoti download Spotify audio?**
No. There's no DRM circumvention. Offline files come only from a download-source extension you choose to install.

## Building from source

The Android project is in [`base/`](base). You need JDK 17+ and the Android SDK (compileSdk 37).

```bash
cd base
./gradlew assembleDebug          # debug build
./gradlew testDebugUnitTest      # unit tests
```

Release APKs (signed with your own key in `base/keystore/keystore.properties`):

```bash
cd base
./gradlew assembleRelease collectApks -PappVersionName=1.0.0 -PappVersionCode=100
```

The APKs end up in `base/app/build/release-apks/`.

### Project layout

| Module | What it does |
|---|---|
| `:api` | Plain Kotlin models and interfaces the UI talks to |
| `:api-spotify` | That API implemented on Spotify's Web Player |
| `:host-android` | The hidden WebView, playback service, media session and Android Auto |
| `:host-cdp` | The same API on desktop Chrome, for testing without a phone |
| `:extensions` / `:extensions-cli` | The extension sandbox and its desktop runner |
| `:app` | The Compose UI (Classic and Immersive looks) |
| `:wear` | The Wear OS app |

More details: [`base/api-spotify/README.md`](base/api-spotify/README.md) and [`docs/api`](docs/api).

## Contributing

Bug reports, ideas and pull requests are welcome in [issues](https://github.com/Kirisos-Guna/myspoti/issues). See [`CONTRIBUTING.md`](CONTRIBUTING.md).

If you enjoy Myspoti, a ⭐ on GitHub helps other people find it.

## Credits

**Myspoti / Orpheon.** This app is a fork of [Myspoti](https://github.com/kota-zo/myspoti) (later renamed Orpheon) by kota-zo and its contributors, under GPL-3.0. Nearly all of the code is their work.

**BitChord.** The Immersive look and the equalizer come from [BitChord](https://github.com/kushagrasinghx/BitChord) by kushagrasinghx and contributors (GPL-3.0-or-later). Each file taken from it keeps its original header.

**Others**
- [Spotui](https://github.com/Spotui/Spotui) and [Metrolist](https://github.com/MetrolistGroup/Metrolist) (GPL-3.0): parts of the UI and the DASH-to-FLAC remux
- [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) (GPL-3.0): music videos
- [Reorderable](https://github.com/Calvin-LL/Reorderable) (Apache-2.0): queue drag and drop
- Lyrics from [LRCLIB](https://lrclib.net), LyricsPlus, lrc.red, Kugou and NetEase; song meanings from [Genius](https://genius.com)
- [Material Icons](https://github.com/google/material-design-icons) (Apache-2.0) and the [Manrope](https://github.com/googlefonts/manrope) font (SIL OFL 1.1)

Full notices: [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

## Disclaimer

Myspoti is not affiliated with, endorsed by or connected to Spotify. Spotify is a trademark of Spotify AB.

Blocking ads goes against Spotify's Terms of Use, and Spotify may suspend accounts that do it. Use this app at your own risk. If you can, support artists with Premium or by buying their music.

## License

[GPL-3.0](LICENSE). If you reuse this code, keep the copyright and license notices, credit the original [Myspoti](https://github.com/kota-zo/myspoti) project, and publish your complete source under GPL-3.0.
