# Development Guide

## Local Setup

1. Clone `tastile-android`.
2. Clone `tastile-core` next to it so the path resolves as `../tastile-core`
   (the build embeds the Rust core via `cargo-ndk`).
3. Set `JAVA_HOME` to JDK 17 or 21.
4. Install the Android SDK + NDK, Rust, and `cargo-ndk`:

   ```bash
   rustup target add aarch64-linux-android armv7-linux-androideabi \
                   i686-linux-android x86_64-linux-android
   cargo install cargo-ndk
   ```

5. Copy `local.properties.example` to `local.properties` and fill in the
   release signing values. See **Release Signing** below.

## Daily Commands

```bash
./gradlew verify                # full quality gate
./gradlew testDebugUnitTest     # unit tests only
./gradlew assembleDebug         # local debug APK
./gradlew bundleRelease         # local release AAB (requires signing config)
```

## Release Signing

Store release signing values in `local.properties` (gitignored) — they are
read into the Gradle build via `findProperty`:

```properties
RELEASE_STORE_FILE=/absolute/path/to/upload-key.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
GOOGLE_WEB_CLIENT_ID=...
```

`bundleRelease` fails fast when these values are missing. The keystore file
itself is **never** committed — only the path is referenced.

## Auth Configuration

`GOOGLE_WEB_CLIENT_ID` is the Cognito Hosted UI Google OAuth client id. It is
the only auth-related buildConfig value the app needs at build time; the
runtime obtains tokens from the web flow. Source of truth:
[`tastile-core/docs/production/secrets-and-deploy.md`](https://github.com/tastile/tastile-core/blob/main/docs/production/secrets-and-deploy.md).

## Release Process (CI)

Production releases are fully automated. See
[`docs/operations/release-plan.md`](./operations/release-plan.md) for the
end-to-end flow. The short version:

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```

GitHub Actions then builds the AAB, uploads it to Google Play (track chosen
via `workflow_dispatch` input), and attaches it to a GitHub Release. No
manual signing, no manual Play Console upload.

## Clean Re-Clone Workflow

When local state becomes questionable:

1. Delete the working directory.
2. Re-clone `tastile-android`.
3. Re-clone `tastile-core` beside it.
4. Restore only local-only files: `local.properties` and the keystore.
5. Run `./gradlew verify`.
