# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Canonical Contract

This is the `tastile-android` child repository of the Tastile workspace. The workspace contract is `../AGENTS.md` — read it first, then this repo's `README.md` (orientation) and `docs/architecture.md` (layer breakdown) before any non-trivial work.

Project-local Skills live in `.agents/skills/`. Claude Code skill adapter mirrors the canonical skills at `.claude/skills/`. Agent-specific settings and hooks inherit from the workspace root; this repo does not redefine them. Do not duplicate workspace-wide rules here.

## Agent Skills (UI 必須セット, vendored upstream)

15 件の third-party Skill を `.agents/upstream-skills/<repo>/` に `git subtree --squash` で
取り込み済み。Canonical pointer は `.agents/skills/<name>/SKILL.md`、Claude Code adapter
は `.claude/skills/<name>/SKILL.md` (pointer を指すだけ)。`description:` frontmatter は
upstream verbatim なので trigger は upstream と一致する。更新は `git subtree pull`。
詳細: `docs/adr/0006-android-ui-skills-vendoring.md`。

- adaptive, edge-to-edge, navigation-3, testing-setup, styles, android-cli
  (from [android/skills](https://github.com/android/skills) @ `aaca635061a4`, Apache-2.0)
- material-3
  (from [hamen/material-3-skill](https://github.com/hamen/material-3-skill) @ `14385f2bf380`, Apache-2.0)
- compose-state-and-effects, compose-performance, compose-component-design,
  compose-animations, compose-focus-navigation, compose-ui-testing-patterns
  (from [chrisbanes/skills](https://github.com/chrisbanes/skills) @ `948acbbd6c44`, Apache-2.0)
- compose-agent, jetpack-compose-audit
  (from [hamen/compose_skill](https://github.com/hamen/compose_skill) @ `f815c31d6cc1`, Apache-2.0)

## Agent Skills (delivery / orchestration, project-local)

並行 agent 駆動の delivery / orchestration / governance 用に 16 件を
project-local canonical (`.agents/skills/<name>/SKILL.md` + `.claude/skills/`
adapter) として保持する。通常 task では必要な Skill だけを読む。

- `github-delivery`, `parallel-orchestration`, `agent-recovery`,
  `quality-gate`, `sandbox-runtime`, `security-maintenance`,
  `engineering-decisions`, `onboarding`
  (from local agent config `~/.config/opencode/skills/`, vendored 2026-09-15)
- `agent-delivery-estimation`, `correctness-assurance`, `policy-evaluation`,
  `design-refinement`, `writing-discipline`, `interaction-discipline`,
  `linear-release-control`, `worktree-workflow`
  (project-local authored, 2026-09-15 — no upstream; reconcile against task
  prompt §10 on next init)
- `buildconfig-guard-check`, `design-system-imports-check`,
  `openapi-spec-refresh`, `tastile-precommit-review` (repo-specific guards)

## Build and Verify

All commands run from this repo root. JDK 17 or 21, Android SDK with API 35, NDK, and the Rust toolchain with `cargo-ndk` are required.

| Goal | Command |
| --- | --- |
| Full verification suite (default pre-push gate) | `./gradlew verify` |
| JVM unit tests only (no release keystore needed) | `./gradlew testDebugUnitTest` |
| Single unit test class | `./gradlew testDebugUnitTest --tests "app.tastile.android.<package>.<ClassName>"` |
| Single unit test method | `./gradlew testDebugUnitTest --tests "app.tastile.android.<package>.<ClassName>.<methodName>"` |
| Debug APK + native libs (needs `../tastile-core`) | `./gradlew assembleDebug` |
| Release build (fails fast without signing props) | `./gradlew bundleRelease` |
| Instrumented tests (Hilt + Espresso) | `./gradlew connectedDebugAndroidTest` |
| Lint only | `./gradlew lintDebug` |

`./gradlew verify` depends on `:app:check`, which itself depends on the project guard tasks `verifyDesignSystemImports` and `verifyNoEmbeddedServerSecrets` registered in `app/build.gradle.kts`.

## Build-Time Hard Requirements

These guards fail the build rather than silently degrading — they exist to prevent environment drift from shipping to users.

- `gradle.projectsEvaluated` in `app/build.gradle.kts` requires every `BuildConfig.*` field listed in `app/build.gradle.kts` (Cognito client/region/hosted-ui/redirect/web-auth base, `TASTILE_CORE_URL`, `GOOGLE_WEB_CLIENT_ID`) to be non-blank. Set them in `gradle.properties` (CI), `~/.gradle/gradle.properties` (local dev), or `-PKEY=value`.
- Release tasks (`assembleRelease`, `bundleRelease`) fail fast if `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` are not provided via the same paths. Never commit keystore or `google-services.json`.
- `verifyDesignSystemImports`: direct `androidx.compose.material3.*` imports are forbidden in `app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account}/` unless the immediately preceding non-blank line is `// m2-allow:`. M3 unified screens must go through the design system.
- `verifyNoEmbeddedServerSecrets`: rejects `TASTILE_WEB_BRIDGE_SECRET` / `x-tastile-web-bridge-secret` from Android sources and the build script. Server-only bridge credentials must not enter Android artifacts.
- The lint block in `app/build.gradle.kts` must not add `disable +=`. Every lint rule surfaces; unaddressable rules go in a tracking doc with a hard BLOCKED rationale.
- Native artifact builds require `../tastile-core` to be a sibling checkout. If missing, the build fails with an explicit message rather than a cargo error cascade.

## Architecture (Quick Map)

Compose UI → ViewModels → Repositories → Cognito auth and/or `tastile-core` native bridge.

- `ui/` — Compose screens, state holders, presentation helpers
- `data/` — Auth repository, data models, repository interfaces
- `domain/` — Domain layer (audit baseline recommends, treat as required)
- `core/` — Native bridge, runtime persistence, DTO mapping for `tastile-core`
- `sync/` — Session handoff and event synchronization into the core runtime
- `notifications/` — Alarm scheduling, notification policy, delivery
- `di/` — Hilt modules
- `execution/` — Execution state projection

Auth and server-backed reads go through Cognito + daemon API. Command execution, replay, and projected execution state are moving behind `tastile-core`; keep that boundary explicit until migration completes.

## Toolchain

Authoritative versions live in `build.gradle.kts` (plugins) and `app/build.gradle.kts` (deps + sdk). The table below is a snapshot for orientation; if it disagrees with the build script, the build script wins.

- AGP 9.4.0, Kotlin 2.2.10, Compose Compiler plugin 2.2.10, Hilt plugin 2.60.1 / Hilt 2.60.1, KSP 2.3.11
- Compose BOM 2026.08.00, Navigation Compose 2.10.0
- `minSdk` 26, `targetSdk` 35, `compileSdk` 37, `versionCode` 33, `versionName` 0.4.0
- `kotlinx-datetime` is pinned at 0.6.1 and `kotlinx-coroutines-test` at 1.11.0 — bumping either surfaces an `ExperimentalTime` opt-in requirement. See `docs/plans/`.
- Compose Compiler Reports land in `app/build/compose-reports/` and `app/build/compose-metrics/`; baseline at `docs/superpowers/m3/before-reports/`.

## IDE-agnostic Kotlin tooling (kotlin-lsp)

Android Studio 以外の editor を使う contributor のため、JetBrains 公式の
Kotlin Language Server (`Kotlin/kotlin-lsp`) を **alpha status** の範囲で
project-local に vendor 可能にしている。Build には影響しない。

- Bootstrap: `./scripts/install-kotlin-lsp.ps1` (Windows) / `./scripts/install-kotlin-lsp.sh` (wslc / POSIX)
- Launcher:  `./scripts/kotlin-lsp-launcher.ps1` / `./scripts/kotlin-lsp-launcher.sh`
- Pin manifest: `scripts/kotlin-lsp-release.json` (version + URL + SHA-256)
- Decision: `docs/adr/0001-kotlin-lsp-toolchain.md`

JDK 25 を別途用意する必要がある。優先順: `$KOTLIN_LSP_JAVA_HOME` → `.tools/jdk-25/`
→ 標準 install 配下。`.tools/` は gitignored。Editor pointer
(例: `.vscode/settings.json` の `kotlin.lsp.executable`) は contributor 各自
が設定し、リポジトリには commit しない。

## WSLC Dev Container

`.wslc/` holds the Windows + WSL Container definitions; the version is auto-extracted from `app/build.gradle.kts`.

- Build: `.wslc/wslc-build.ps1` (add `-NoCache` to bust caches)
- Dev shell: `.wslc/wslc-dev.ps1` (add `-DeviceIp <ip>` for wireless ADB)
- ADB inside container: `wslc exec tastile-android-dev adb devices`

## Working Rules

- Work on the active `release-x-y-z` sprint branch or a ticket branch named after the GitHub Issue number only (`<issue-number>`). Do not commit directly to `main`, and do not create `feature/*`, `fix-*`, `hotfix-*`, or `wip-*` branches. See ADR-0007 and `docs/operations/release-workflow.md`.
- Source code, identifiers, code comments, and Git/GitHub messages are English. Internal development docs are Japanese.
- Do not write new Python scripts in this repo. Use Kotlin, shell, or PowerShell as appropriate.
- Search with `rg` / `rg --files`; prefer semantic navigation via the Kotlin language tooling already in `.tools/`.
- Never commit: `local.properties`, `google-services.json`, keystores, `.env*` with real values, generated `app/src/main/jniLibs/`, or anything in `reference/`, `.build-logs/`, `.tools/`.
- `reference/` clones are read-only; they must not become implicit build or runtime dependencies.
- Before claiming "PASS / DONE / GREEN / ready to ship", run `./gradlew verify` from a clean state. If your change is in `:app` source, also run the unit-test target to catch regression coverage gaps.

## Related Workspace Siblings

- `../tastile-core/` — Rust core, produces Android native libs via `cargo-ndk`. Required for artifact builds.
- `../tastile-web/` — Next.js sibling; shares Cognito config values with this repo.
- `../AGENTS.md` — workspace contract. Read it before any cross-repo change.

## Workflow (release sprint, projects, recovery)

The child repo follows the workflow defined by `../AGENTS.md` (workspace
contract) and the child-local ADRs referenced below. Delivery Skills have
their canonical form at `.agents/skills/<name>/SKILL.md` in this repo and a
Claude Code adapter under `.claude/skills/<name>/SKILL.md`. Three adapters
(`project-board`, `recover-task`, `release-branch-workflow`) keep their stable
names and point at the child-local canonical Skills listed below.

| Topic                | ADR (this repo)                              | Skill (repo canonical)                                  | Adapter (this repo)                                 |
| -------------------- | -------------------------------------------- | ------------------------------------------------------- | --------------------------------------------------- |
| Release sprint / PR  | `docs/adr/0007-release-branch-and-ticket-workflow.md` | `.agents/skills/github-delivery/SKILL.md`        | `.claude/skills/release-branch-workflow/SKILL.md`   |
| Recovery / checkpoint| `docs/adr/0008-structured-recovery-checkpoint.md`     | `.agents/skills/agent-recovery/SKILL.md`         | `.claude/skills/recover-task/SKILL.md`              |
| Project work state   | `docs/adr/0009-github-projects-work-state.md`         | `.agents/skills/github-delivery/SKILL.md`        | `.claude/skills/project-board/SKILL.md`             |
| Release authorization| `docs/adr/0012-release-merge-authorization.md`        | `.agents/skills/github-delivery/SKILL.md`        | (same adapters as above)                            |
| Linear profile       | `docs/adr/0014-linear-profile-non-adoption.md`        | `.agents/skills/linear-release-control/SKILL.md` | `.claude/skills/linear-release-control/SKILL.md`    |
| Estimation           | (policy in `agent-delivery-estimation` Skill)         | `.agents/skills/agent-delivery-estimation/SKILL.md` | `.claude/skills/agent-delivery-estimation/SKILL.md` |

`tastile-android` does not redefine workspace `docs/adr/00NN-*`; its local
`docs/adr/0001-kotlin-lsp-toolchain.md`,
`docs/adr/0006-android-ui-skills-vendoring.md`,
`docs/adr/0007-release-branch-and-ticket-workflow.md`,
`docs/adr/0008-structured-recovery-checkpoint.md`,
`docs/adr/0009-github-projects-work-state.md`,
`docs/adr/0012-release-merge-authorization.md`, and
`docs/adr/0014-linear-profile-non-adoption.md` remain authoritative on
their subjects. Full delivery Skill index: `.agents/skills/` (canonical) —
see also `parallel-orchestration`, `sandbox-runtime`, `quality-gate`,
`engineering-decisions`, `security-maintenance`, `onboarding`,
`correctness-assurance`, `policy-evaluation`, `design-refinement`,
`writing-discipline`, `interaction-discipline`, `worktree-workflow`.
