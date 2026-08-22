# Android CI toolchain alignment

## Goal

Make the GitHub Actions Android SDK setup match the Android application build
configuration so the handoff quality gate can be reproduced outside a local
developer machine.

## Scope

1. Install the NDK version declared by `app/build.gradle.kts` in both verify
   and release workflows.
2. Preinstall the declared compile SDK and the target SDK build tools in the
   release workflow.
3. Keep release signing and Play publication unchanged; those remain gated by
   repository secrets and an explicit release trigger.

## Verification

- Run `./gradlew verify --no-daemon` in the WSLC Android image with test-only
  configuration values.
- Review the workflow values against `compileSdk` and `ndkVersion`.
