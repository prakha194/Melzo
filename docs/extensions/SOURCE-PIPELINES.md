# Source racing (experimental)

Feature-detect exact strings in `myspoti.features`; never infer these from an app version.

The app does not decrypt media for an extension. A download file or audio answer that carries a
`transform` is refused.

## `sources.race`

```js
myspoti.provide('download-source', function (query) {
  if (!query.source) {
    return myspoti.sources.race('download-source', query, ['one', 'two']);
  }
  return resolveOnly(query.source, query);
});
```

Also supports `audio-source`. One to four named lanes run independent copies of the same
extension script, receiving `query.source`. They share the extension's own storage/settings,
but never a Rhino scope. Top-level extension code must therefore be idempotent. Do not do
pairing or other UI work in a lane. No nested races; source workers cannot
open sign-in windows. Network requests retain host checks and a shared parent HTTP quota;
notifications share the parent's quota. Root hook time limits include all lane work.

The first non-empty parseable answer wins; a CD/hi-res download cannot win with a lossy file.
A failed/empty lane does not win. Remaining workers are interrupted and closed. Transport
cancellation remains best-effort and bounded by request timeouts. This selects a descriptor;
normal file validation still decides whether the audio is the requested recording. A hi-res
preference with CD fallback remains a preference; implement strict quality in each resolver
if required. Old hosts must keep a sequential fallback instead of calling this API.
