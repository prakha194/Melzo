# How to get the APK from this source

Everything is already changed. Only the build is left, and it has to run on a machine with
enough CPU: the app module is ~90,000 lines of Jetpack Compose, and the build needs about an hour
of solid compile time on one core.

## Option A — GitHub Actions (no software to install, ~15 minutes)

1. On GitHub, create a new repository, e.g. `orpheon-mod`. Private is fine.
2. Upload this folder's contents to it (drag the files into the repo page, or use
   "Add file → Upload files"). Keep the `.github/workflows/build-apk.yml` file — it is what runs
   the build.
3. Open the **Actions** tab. "Build APK" starts by itself (or press **Run workflow**).
4. Wait ~15 minutes. Open the finished run, download the **orpheon-apk** artifact, unzip it — you
   get `Orpheon-1.0.1.apk`.
5. Copy it to your phone and install it.

## Option B — Android Studio on your computer

1. Open the `base` folder (not the outer folder) as a project in Android Studio.
2. Let Gradle sync and download the SDK parts it asks for.
3. Build → Generate Signed Bundle / APK → APK → create or pick a keystore → **release**.
4. The APK lands in `base/app/build/outputs/apk/release/`.

## Command line equivalent (same as the workflow)

```bash
cd base
./gradlew :app:assembleRelease \
  -PappVersionName=1.0.1 -PappVersionCode=201 \
  -Pabis=arm64-v8a,armeabi-v7a
```

A stable release build refuses to run without a signing key, so either let the workflow generate
one (it does) or add `base/keystore/keystore.properties` + `base/keystore/release.jks`.

## Installing it

- The app id is `app.orpheon`, the same as the official Orpheon, but this build is signed with a
  **different key**. Android will refuse to install it over the official app: uninstall the
  official Orpheon first (that deletes its data), then install this one.
- Android 10 or newer.

## What was changed

See `MODIFICATIONS.md` in this folder. Orpheon is by **kota-zo**, GNU GPL v3, and this is a
modified build, not the official release — the About screen says so, as the licence asks.
