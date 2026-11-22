# Extensions: a guide for users

Orpheon is a player first: extensions add lyrics, sound, downloads and tools. Each one is a small
script, made by a third party, that runs in a sandbox and only does what it asked for when you
installed it. You are responsible for the extensions you install and for having the right to use
their sources.

For extension authors, see [API.md](API.md).

## What kinds there are

| Kind | What it does | Where you see it |
|---|---|---|
| **Audio source** | Gives the audio of the songs you play, from a source of its own (its quality, often lossless). A song no source has audio for is not played. | Settings, Playback: the source order and the extension's quality |
| **Download source** | Lets you download songs, albums, playlists and Liked Songs to play with no connection. Orpheon keeps the files encrypted on your phone. | The download button on songs and lists; Settings, Downloads |
| **Lyrics** | Lyrics from other catalogues, or word by word. | The lyrics screen |
| **Other** | Home or search shelves, player buttons, tools. | Where the extension says, after you install it |

Orpheon ships no audio or download source of its own and hosts no content: what an extension
plays or downloads comes from where its author says, under that author's responsibility and yours.
Use only sources you have the right to use.

## Installing one

1. Open **Settings, Extensions**.
2. Either:
   - **From a repository**: on the **Store** tab, add the repository address the extension's
     author gives you (it ends in `index.json`), then pick the extension in the list. A repository
     keeps its extensions up to date.
   - **From a package**: install the `.zip` file you were given, or paste its address.
3. Read what the extension will do. Every action it asks for is listed with its author's reason,
   and the critical ones are marked. You install it on your own responsibility, and only install
   from people you trust.
4. Some extensions need a sign-in or a setting of their own: their page opens after the install.

An update that asks for more than before waits for your OK and shows only what changes.

## The order of the sources

With several audio sources, Settings, Playback lists them in order: a song is asked to the first,
then the next; when none has it, it is not played. A newly installed extension goes first.
Drag to change the order, or switch one off without removing it.

## When something does not work

| You see | What it usually means | What to try |
|---|---|---|
| The extension says the song is not available or "not reachable from the source", or the song does not play | The extension's source has no copy of that song (or matched none) | Nothing is wrong in Orpheon. Another source may have it. |
| "Not answering for now", a source named on a failed download | The extension's server is down, slow or limits you | Try again later; the app sets a source aside for a while after repeated timeouts. |
| An extension that cannot sign in or connect | Often a VPN, a DNS blocker or an expired session | Turn the VPN off for the sign-in, then sign in again from the extension's page. |
| "No matching recording found" on downloads | The download source found no file of the right length for that song | Try another quality, or another download source. |
| Downloads stop when the app is closed | The phone's battery saver stops the app | Settings, Battery: let Orpheon run in the background. |

Report a problem to the extension's author first: Orpheon cannot change what a source answers.
For Orpheon itself, **Export a diagnostic report** in Settings writes one with personal details
masked, to attach to your report.

## Android Auto

Android Auto lists only apps installed from the Play Store, unless you allow others: in the
Android Auto settings on the phone, tap **Version** about ten times to unlock the developer
settings, then turn on **Unknown sources**. Orpheon then shows in the car's app list.

## Android TV

On a TV, Orpheon opens a short version for the remote: your Library, Liked Songs, Search and the
player. Install the `armeabi-v7a` APK on most TV boxes (the `arm64-v8a` one on recent ones), and
sign in once with the remote. Extensions, downloads and settings stay on the phone version.
