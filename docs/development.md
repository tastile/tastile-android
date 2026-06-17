# Development Guide

## Local Setup

1. Clone `tastile-android`.
2. Clone `tastile-core` next to it so the path resolves as `../tastile-core`.
3. Set `JAVA_HOME` to JDK 17 or 21.
4. Ensure Android SDK, NDK, Rust, and `cargo-ndk` are installed.

## Daily Commands

```bash
./gradlew verify
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Release Signing

Store release signing values in `~/.gradle/gradle.properties`:

```properties
RELEASE_STORE_FILE=/absolute/path/to/upload-key.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

Release tasks fail fast when these values are missing.

## AWS Cognito Configuration

`GOOGLE_WEB_CLIENT_ID` (Cognito Hosted UI, Google OAuth federated identity) lives in `local.properties`
and is read into `gradle.properties` at build time. Keep it aligned with the AWS-only connection model
documented in `tastile-desktop/CLAUDE.md`.

## Clean Re-Clone Workflow

When local state becomes questionable:

1. Delete the working directory.
2. Re-clone `tastile-android`.
3. Re-clone `tastile-core` beside it.
4. Restore only local-only files such as `local.properties` and user-level Gradle secrets.
5. Run `./gradlew verify`.
