# What this build changes

This is a modified build of [Orpheon](https://github.com/kota-zo/Orpheon) by **kota-zo**, which is
free software under the GNU GPL v3. It is **not** the official release, and its author is not
responsible for it. The GPL asks a modified version to say so, and to keep its source available:
both are done here.

Changes, all in the app module:

1. **No update card.** `UpdatePrompt()` is no longer called from `App.kt`, so no "A new update is
   available" popup is drawn and nothing is downloaded or installed from inside the app.
2. **No donation reminder.** `SupportPrompt()` is no longer called: the card that asked for a
   donation a few seconds after launch is gone.
3. **No support entry.** The "Support" row (and its Ko-fi / GitHub Sponsors sheet) is removed from
   the profile sheet in both looks — `HomeScreen.kt` (Classic) and `ImmersiveHomeProfile.kt`
   (Immersive). `SupportSheet` itself is removed from `AboutSheet.kt`.
4. **About, rebuilt.** `AboutSheet.kt` now shows the app icon, the version, the credit to the owner
   (kota-zo, GNU GPL v3, "this APK is a modified build") and two buttons:
   - **Support** → https://github.com/prakha194/
   - **Source code** → https://github.com/kota-zo/Orpheon
   The same credit and links replace the update/diagnostics rows of Settings → App, in both looks
   (`SettingsScreen.kt`, `ImmersiveSettingsScreen.kt`).
5. **No developer popups.** The "Stats for nerds" sheet is no longer reachable from a song's
   options (`SongOptionsSheet.kt`, `ImmersiveSongOptionsSheet.kt`); the diagnostic-report export is
   gone from Settings; the beta log dot and the one-time "new look" popup are disabled in
   `MainActivity.kt`.
6. **New launcher icon** (`drawable-*dpi/ic_launcher_foreground.png`, `drawable/ic_launcher_background.xml`).

Everything else is upstream Orpheon 1.0.0, unchanged.

## Later changes (Spoon build)

7. **Renamed to Spoon** across the app: the launcher label, the About sheet, the welcome text and
   every string that named the app. `applicationId` stays `app.orpheon`, so the app installs over
   the previous build.
8. **About** now credits "Spoon is made by Prashant", with the upstream credit to kota-zo and the
   GNU GPL v3 notice kept on the next line.
9. **New-release notice** (`ui/components/ReleaseNotice.kt`): a few seconds after launch the app
   asks GitHub for the latest release of `prakha194/Spotify` and, if it is newer than the installed
   version, shows one card with **Update now** (opens the release page) and **Not now** (that
   version is not offered again).
10. **Stable signing**: `base/keystore/` now holds a keystore that CI reuses, so each new build
    installs over the previous one instead of asking for an uninstall.
11. The empty Home now says what to do instead of only "Nothing to show yet".
