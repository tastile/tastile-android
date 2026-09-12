# Architecture

## Runtime Layers

```text
Jetpack Compose UI
    -> ViewModels
        -> Repositories
            -> Domain (pure Kotlin use cases)
                -> Cognito auth and/or tastile-core runtime bridge
                    -> Native library built from ../tastile-core
```

## Key Source Areas

- `app/src/main/java/app/tastile/android/ui`
  UI screens, state holders, and presentation helpers.
- `app/src/main/java/app/tastile/android/data`
  Auth repository, data models, and repository interfaces.
- `app/src/main/java/app/tastile/android/domain`
  Pure Kotlin domain models and use cases. Android framework 依存を持たず、
  `./gradlew testDebugUnitTest` で完結する unit test 群を担当する。
- `app/src/main/java/app/tastile/android/di`
  Hilt module 群。Application / Activity / ViewModel それぞれの component に
  binding を定義する。
- `app/src/main/java/app/tastile/android/core`
  Native bridge, runtime persistence, and DTO mapping for `tastile-core`.
- `app/src/main/java/app/tastile/android/sync`
  Session handoff and event synchronization into the core runtime.
- `app/src/main/java/app/tastile/android/notifications`
  Alarm scheduling, notification policy, and delivery orchestration.
- `app/src/main/java/app/tastile/android/execution`
  Legacy execution / replay / projected state implementation. ADR-0011 に
  従い `tastile-core` 側へ migration 予定の deprecated layer。

## Boundaries

- Auth and server-backed reads go through Cognito and daemon API.
- Command execution, replay, and projected execution state are moving behind `tastile-core` (see [ADR-0011](./adr/0011-architecture-migration.md)).
- `domain/` package は Android framework 依存を持たず pure Kotlin に閉じる。
  transport / framework 依存は `data/` / `core/` に閉じ込める。
- DI binding は `di/` に集約し、ViewModel / Repository から直接生成しない。
- Keep these boundaries explicit until the migration is complete. Avoid mixing UI logic directly with transport details.

## Build Assumptions

- JVM-only verification does not require `tastile-core`.
- Android artifact builds that invoke `cargo-ndk` require the sibling Rust repository at `../tastile-core`.
- `./gradlew testDebugUnitTest` は JVM のみで完結し、domain / di layer の unit
  test を含む。instrumented test (`:app:connectedDebugAndroidTest`) は別途
  device / emulator を要求する。
- Guard tasks `verifyDesignSystemImports` / `verifyNoEmbeddedServerSecrets` /
  `verifySkillAdapterDrift` が `./gradlew verify` 経由で発火し、CI の fail-fast
  gate として機能する。

## Cross-references

- [ADR-0001](./adr/0001-kotlin-lsp-toolchain.md) — Kotlin LSP を project-local に導入
- [ADR-0006](./adr/0006-android-ui-skills-vendoring.md) — Compose Skills の vendoring
- [ADR-0007](./adr/0007-release-branch-workflow.md) — release / ticket lifecycle
- [ADR-0008](./adr/0008-recovery-checkpoint.md) — structured recovery checkpoint
- [ADR-0009](./adr/0009-issue-pr-lifecycle.md) — Issue / PR lifecycle
- [ADR-0010](./adr/0010-release-sequencing.md) — release sequencing rule
- [ADR-0011](./adr/0011-architecture-migration.md) — tastile-core への migration boundary
- `../.agents/skills/release-branch-workflow/SKILL.md` (workspace canonical)
