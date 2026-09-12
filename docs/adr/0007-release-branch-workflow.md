---
adr_id: 0007
title: release-branch-workflow と ticket lifecycle
status: accepted
date: 2026-09-12
deciders: rebuildup, tastile-android maintainers
supersedes: null
superseded_by: null
target_release: release-0-6-0
related_skills:
  - .agents/skills/release-branch-workflow/SKILL.md (canonical)
  - .agents/skills/project-board/SKILL.md
  - .agents/skills/tastile-precommit-review/SKILL.md
---

# ADR 0007 — release-branch-workflow と ticket lifecycle

## Context

Tastile Android は `tastile/tastile-root` を workspace とし、weekly sprint = 1
target release version で開発する。1 sprint の integration branch は
`release-<major>-<minor>-<patch>` であり、durable ticket branch は Issue 番号
(digits) のみ、stacked ticket は immediate predecessor branch を base とする
運用が Skill および operations doc で宣言されているが、canonical ADR が不在で
各所で参照が破断していた (`docs/operations/release-workflow.md` line 1, 83 /
`docs/operations/project-board.md` line 65 /
`docs/superpowers/specs/2026-09-07-ui-rebuild-design.md` line 53, 256, 308)。

## Decision

`.agents/skills/release-branch-workflow/SKILL.md` を canonical 仕様と確定し、
次の invariant を ADR として formalize する。

- `main` = released / integrated source state。public repository では
  branch protection により direct push / direct web edit / force push /
  deletion を禁止 (B1 で硬化予定)。
- sprint = 1 target release version。release branch は sprint 開始時に `main`
  から作成 (`release-x-y-z`)。
- 1 top-level Issue = 1 durable ticket branch = 1 ticket PR。
  branch 名 canonical regex は `^(?:[0-9]+|release-[0-9]+-[0-9]+-[0-9]+|main)$`。
- independent ticket PR は release branch を base、same-release linear hard
  dependency は immediate predecessor branch を base とする stacked PR を許可。
- durable ticket branch 作成 → first meaningful commit → canonical remote
  publish → remote head SHA 確認 → immediate Draft PR を一つの開始手順とし、
  Draft PR なしで active implementation を継続しない。
- PR body に 4 marker (Issue / Target Release / Branch / Execution Generation)
  を必須配置。merge authorization は user のみ。
- release branch は zero-diff の間だけ Draft release PR 不要。first
  meaningful integrated difference 後は Draft release PR を必須とし、PR
  metadata に release goal / included Issues / breaking changes / migration
  notes / validation results / known limitations を記述する。
- post-merge local tag `v<version>` を gpg / ssh-key 署名付きで打ち、
  `release.yml` が AAB / Play / GitHub Release に流す。

## Consequences

positive:
- canonical ADR により release / ticket lifecycle の参照が単一情報源に収束。
- CI / CODEOWNERS / verify-tastile-change / tastile-precommit-review Skill が
  ADR を anchor として整合性を検証できる。
- merge authorization boundary が明示され、agent / subagent は ready-to-merge
  状態で停止できる。

negative:
- 既存 Sprint 0-6-0 で release-0-6-0 が zero-diff から 1 commit 先 (`aef975b`)
  になっており、first-difference 後の Draft release PR が未作成 (B1 で対応)。
- 既存の stale remote branches (`origin/2026-07-07-android-parity`,
  `origin/codex/verify-release-0-6-0`) は canonical regex を満たさず retire
  が必要 (B1 で対応)。

## Re-evaluation condition

次のいずれかが発生したら再評価し、必要なら supersede:
1. weekly sprint cadence を 1 週間以外に変更する decision。
2. branch protection の head-branch pattern 制限を ruleset / required
   check のみで十分 enforce できないと判断した場合。
3. stacked PR をやめる / nested PR fallback に縮退する場合。

## Cross-references

- `.agents/skills/release-branch-workflow/SKILL.md` (canonical Skill)
- `.agents/skills/project-board/SKILL.md`
- `.agents/skills/tastile-precommit-review/SKILL.md`
- `docs/operations/release-workflow.md`
- `docs/operations/project-board.md`
- `docs/superpowers/specs/2026-09-07-ui-rebuild-design.md`