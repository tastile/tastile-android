# Architecture

## Runtime Layers

```text
Jetpack Compose UI
    -> ViewModels
        -> Repositories
            -> Supabase client and/or tastile-core runtime bridge
                -> Native library built from ../tastile-core
```

## Key Source Areas

- `app/src/main/java/app/tastile/android/ui`
  UI screens, state holders, and presentation helpers.
- `app/src/main/java/app/tastile/android/data`
  Supabase client factory, data models, and repository interfaces.
- `app/src/main/java/app/tastile/android/core`
  Native bridge, runtime persistence, and DTO mapping for `tastile-core`.
- `app/src/main/java/app/tastile/android/sync`
  Session handoff and event synchronization into the core runtime.
- `app/src/main/java/app/tastile/android/notifications`
  Alarm scheduling, notification policy, and delivery orchestration.

## Boundaries

- Auth and some server-backed reads still go through Supabase.
- Command execution, replay, and projected execution state are moving behind `tastile-core`.
- Keep these boundaries explicit until the migration is complete. Avoid mixing UI logic directly with transport details.

## Build Assumptions

- JVM-only verification does not require `tastile-core`.
- Android artifact builds that invoke `cargo-ndk` require the sibling Rust repository at `../tastile-core`.
