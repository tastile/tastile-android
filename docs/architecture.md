# Architecture

## Runtime Layers

```text
Jetpack Compose UI
    -> ViewModels
        -> Repositories
            -> AWS / tastile-core runtime bridge
                -> Native library built from ../tastile-core
```

## Key Source Areas

- `app/src/main/java/app/tastile/android/ui`
  UI screens, state holders, and presentation helpers.
- `app/src/main/java/app/tastile/android/data`
  Data models, repository interfaces, and AWS / tastile-core bridge.
- `app/src/main/java/app/tastile/android/core`
  Native bridge, runtime persistence, and DTO mapping for `tastile-core`.
- `app/src/main/java/app/tastile/android/sync`
  Session handoff and event synchronization into the core runtime.
- `app/src/main/java/app/tastile/android/notifications`
  Alarm scheduling, notification policy, and delivery orchestration.

## Boundaries

- Auth and server-backed reads go through AWS (Cognito + the `tastile-core` API).
- Command execution, replay, and projected execution state are moving behind `tastile-core`.
- Keep these boundaries explicit until the migration is complete. Avoid mixing UI logic directly with transport details.

## Build Assumptions

- JVM-only verification does not require `tastile-core`.
- Android artifact builds that invoke `cargo-ndk` require the sibling Rust repository at `../tastile-core`.
