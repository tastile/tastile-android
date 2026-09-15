# ADR-0009: GitHub Projects work state

- Status: Accepted
- Date: 2026-09-15
- Scope: `tastile-android/`

## Context

release planning control plane を GitHub Projects と optional Linear profile の
どちらか一方に明示する必要がある。同じ field を二重 canonical にしない。
運用手順は `docs/operations/project-board.md`、binding workflow は
`.agents/skills/github-delivery/SKILL.md`。

## Decision

### 1. Control plane の選択: GitHub Projects

planning control plane は GitHub Projects (Project v2) とする。Linear profile
(`linear-release-control` Skill) は本 repo では不採用とする (ADR-0014)。
durable SoT は GitHub Issues であり、Projects は planning 表示のための補助
board である。dependency metadata の canonical source は Issue dependency
graph であり、Project field ではない。

### 2. Status machine

`Backlog -> Ready -> In Progress -> In Review -> Done`

`blocked` (prerequisite snapshot 未利用)、`stack-ready` (reviewable predecessor
snapshot あり)、`integrated` (target release trunk へ land 済み) の区別は
execution 上必要だが、Status 列自体は増やさない。WIP は実 capacity に合わせ、
`In Progress` は owner あたり 3 ticket を上限目安とする。

### 3. 必須 field

`priority` (P0-P3)、`size` (XS-XL)、`target_version` (`release-x-y-z` 文字列)、
`area` (dashboard/mobile/account/design-system/native/sync/release)、
`execution_generation` (number ≥ 1、ADR-0008 連動)。

### 4. Ticket Done 境界

ticket changes が target release trunk へ land し、required CI/checks green +
blocking review resolved の後に GitHub Issue を明示 close し、Project の
ticket status を Done に更新する。`resolves #<n>` による GitHub built-in 連動は
non-default base では Issue を close しないため、trunk landing 後の明示 close
を運用 rule とする。
