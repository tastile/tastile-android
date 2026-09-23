# Development Guide

## Local Setup

1. Clone `tastile-android`.
2. Clone `tastile-core` next to it so the path resolves as `../tastile-core`.
3. Set `JAVA_HOME` to JDK 17 or 21.
4. Ensure Android SDK, NDK, Rust, and `cargo-ndk` are installed.

## Daily Commands

```bash
infisical --domain=https://secrets.rebuildup.dev run --env=dev --path=/ -- ./gradlew verify
infisical --domain=https://secrets.rebuildup.dev run --env=dev --path=/ -- ./gradlew testDebugUnitTest
infisical --domain=https://secrets.rebuildup.dev run --env=dev --path=/ -- ./gradlew assembleDebug
```

## Release Signing

Release signing values and the keystore are stored in the dedicated Infisical project. GitHub Actions fetches them with OIDC for releases; do not copy them to local Gradle properties. Release tasks fail fast when the required Infisical environment variables are missing.

## AWS Cognito Configuration

The repository uses AWS Cognito for authentication. Cognito configuration is set via BuildConfig fields.
Keep them aligned with the shared backend/web configuration.

## Clean Re-Clone Workflow

When local state becomes questionable:

1. Delete the working directory.
2. Re-clone `tastile-android`.
3. Re-clone `tastile-core` beside it.
4. Restore only local-only files such as `local.properties` and user-level Gradle secrets.
5. Run `./gradlew verify`.
