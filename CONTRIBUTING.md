# Contributing

## Workflow

1. Keep `tastile-core` cloned next to this repository when working on Android builds.
2. Point `JAVA_HOME` at JDK 17 or 21.
3. Run `./gradlew verify` before pushing.
4. Use `./gradlew assembleDebug` before handing off Android-facing changes.

## Secrets And Local Configuration

- Commit only publishable client configuration.
- Store release signing credentials in `~/.gradle/gradle.properties`.
- Do not commit `local.properties`, keystores, or machine-specific JVM paths.

## Code And Repository Standards

- Prefer small, reviewable commits with clear intent.
- Add or update tests for behavior changes when practical.
- Keep generated outputs out of version control.
- Document operational or architectural decisions under `docs/`.

## Pull Request Checklist

- `./gradlew verify` passes.
- Android build assumptions are documented if they changed.
- New local setup requirements are reflected in `README.md` or `docs/development.md`.
- Secrets and machine-local paths are not introduced.

## Release workflow (ADR-0007)

Active sprint lives on `release-<major>-<minor>-<patch>`. Each ticket works on a
branch named after its GitHub Issue number only (no `feature/` / `fix-*` / worktree).
PR body carries the four required markers (`Issue:`, `Target Release:`, `Branch:`,
`Execution Generation:`) defined in
[`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md) and the
[`release-branch-workflow`](../../.agents/skills/release-branch-workflow/SKILL.md)
Skill.

End-of-sprint release PR (`release-x-y-z -> main`) carries release goal,
included Issues, breaking changes, migration notes, full validation result, and
known limitations per [`.agents/skills/release-branch-workflow/SKILL.md`](../../.agents/skills/release-branch-workflow/SKILL.md).

See [`docs/operations/release-workflow.md`](docs/operations/release-workflow.md)
for the full branch / tag / release procedure, and
[`docs/operations/release-plan.md`](docs/operations/release-plan.md) for the
per-version tag → Play → GitHub Release playbook.

## Project board (ADR-0009)

Durable work item = GitHub Issue, linked to a Project v2 board. Status progresses
`Backlog → Ready → In Progress → In Review → Done`; each transition requires the
mandatory fields (`priority`, `size`, `target_version`, `area`,
`execution_generation`) listed in
[`docs/operations/project-board.md`](docs/operations/project-board.md).

## Recovery (ADR-0008)

Agent context loss, session expiry, sandbox recreation, or handoff to a fresh
agent follows the 12-step procedure in
[`docs/operations/recovery.md`](docs/operations/recovery.md) and the
[`recover-task`](../../.agents/skills/recover-task/SKILL.md) Skill. Reconstruct
from durable remote state (GitHub Issues, branches, schema-validated checkpoints),
not from prior conversation.
