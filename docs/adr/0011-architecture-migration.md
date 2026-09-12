---
adr_id: 0011
title: tastile-core への execution / replay / projected state migration
status: proposed
date: 2026-09-12
deciders: rebuildup, tastile-android maintainers
supersedes: null
superseded_by: null
target_release: release-0-6-0
related_components:
  - app/src/main/java/app/tastile/android/core
  - app/src/main/java/app/tastile/android/sync
  - app/src/main/java/app/tastile/android/execution
  - ../tastile-core (workspace sibling)
---

# ADR 0011 — tastile-core への execution / replay / projected state migration

## Context

`docs/architecture.md` の Boundaries section は
"Command execution, replay, and projected execution state are moving behind
`tastile-core`." と宣言しているが、ADR としては記録されておらず、
source tree (`app/src/main/java/app/tastile/android/execution`) も
documented layers (ui/data/core/sync/notifications) に含まれていない。
design spec `docs/superpowers/specs/2026-09-07-ui-rebuild-design.md` line 256
が ADR-0011 を直接参照している。

## Decision

次の migration boundary を canonical として確定する。

- `app/src/main/java/app/tastile/android/execution/` は execution / replay /
  projected state の **legacy** 実装。Android 側で UI state projection のみを
  担当し、command execution と replay は `tastile-core` 側に移管する。
- `app/src/main/java/app/tastile/android/core/` は native bridge 層として
  `tastile-core` の NDK artifact (`cargo-ndk` で build された `*.so`) と
  DTO mapping を担当する。execution semantics は持たない。
- `app/src/main/java/app/tastile/android/sync/` は session handoff と event
  synchronization を担当し、`tastile-core` runtime への橋渡しのみ。
- `app/src/main/java/app/tastile/android/domain/` は pure Kotlin の domain
  model + use case layer。Android framework 依存を持たず、テストは
  `./gradlew testDebugUnitTest` で完結する。
- `app/src/main/java/app/tastile/android/di/` は Hilt module 群。
  Application / Activity / ViewModel それぞれの component に binding を
  定義する。
- migration 期間中は Android 側に legacy execution コードを残置するが、
  新規 command execution は `tastile-core` 側にのみ追加する。混在は禁止。

## Consequences

positive:
- ADR-0011 により migration boundary が明示され、design spec / architecture.md
  / source tree の整合が取れる。
- domain / di / execution packages が `docs/architecture.md` Key Source Areas
  に追記され、新規 contributor が層構造を理解しやすくなる。
- legacy execution code の freeze 時期が明確化される。

negative:
- migration 期間中、execution semantics が Android 側と `tastile-core` 側の
  二箇所に存在し、duplication リスクがある。`tastile-core` 側の検証が終わる
  まで legacy を残置する必要がある。
- `tastile-core` 側の API 変更が Android 側に波及するため、cross-repo contract
  check (workspace Skill) の運用 cost が増える。

## Re-evaluation condition

1. `tastile-core` 側で command execution / replay / projected state が
   production-ready になり、legacy `execution/` package の削除を決めた時。
2. cross-repo contract check (workspace) を production gate にした場合。
3. domain / di / execution の layer 責務を再分割する場合。

## Cross-references

- `docs/architecture.md` (Boundaries section, Key Source Areas update)
- `docs/superpowers/specs/2026-09-07-ui-rebuild-design.md` line 256
- `../tastile-core/HARNESS.md`
- `.agents/skills/cross-repo-contract-check/SKILL.md` (workspace)