# Contributing to Orpheon

Thanks for helping. Bug reports (with a diagnostic report from Settings), fixes and translations are all welcome.

## Build and test

Requirements: JDK 17+ and the Android SDK (compileSdk 37). The Android project is in `base/`.

```bash
cd base
./gradlew assembleDebug
./gradlew :app:testDebugUnitTest :extensions:test :catalog:test
```

Put your SDK path in `base/local.properties` (`sdk.dir=...`).

## Changes

- Keep a change focused, and write it like the code around it. Texts in the repository (code, comments, strings in `values/`, docs) are in English; translations go in the matching `values-xx/` folder.
- Add or update a unit test when you fix a bug or change a rule.
- Commit messages are one plain sentence in English that says what changes for the user (or what was wrong), not a list of files.

## Rules

- **Extensions API:** an extension call that is accepted today must never become an error. New fields and calls are fine; removing, renaming or tightening an existing one is not.
- **No circumvention:** no contribution may bypass DRM or a service's protections (licences, attestation, rate limits), or add an audio download or decryption of protected content.
- **License:** Myspoti is GPL-3.0. Your contribution is released under the same license. If you reuse this code in another project, keep the notices, credit Myspoti (https://github.com/kota-zo/myspoti) and publish your complete source under GPL-3.0; closing the source or changing the license is a violation.
