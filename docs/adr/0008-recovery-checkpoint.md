---
adr_id: 0008
title: structured recovery checkpoint と fencing token
status: accepted
date: 2026-09-12
deciders: rebuildup, tastile-android maintainers
supersedes: null
superseded_by: null
target_release: release-0-6-0
related_schemas:
  - .agent-loop/checkpoint.schema.json (workspace canonical)
  - .agent-loop/agent-result.schema.json (workspace canonical)
related_skills:
  - .agents/skills/recover-task/SKILL.md (canonical)
---

# ADR 0008 — structured recovery checkpoint と fencing token

## Context

AI agent の session / context 消失後に fresh agent が前タスクを引き継ぐため、
rebuildup/project-init policy §16 で定義される recovery algorithm が必要。
Tastile workspace は checkpoint schema を
`/home/basic/work/tastile/.agent-loop/checkpoint.schema.json` (draft 2020-12)
で canonical 化しているが、operations / recovery / Skill / Issue / design-spec
から ADR-0008 への参照が破断していた
(`docs/operations/recovery.md` line 1, 72 /
`docs/operations/project-board.md` line 29 /
`docs/superpowers/specs/2026-09-07-ui-rebuild-design.md`)。

## Decision

recovery algorithm の正本を ADR-0008 として formalize し、次の invariant を
確定する。

- checkpoint は JSON Schema draft 2020-12 に従い、
  `execution_generation` (integer ≥ 1) と `fencing_token` (minLength 16) を必須
  field として保持する。
- parent → child transfer は immutable snapshot (`/home/basic/work/tastile/.agent-loop/checkpoints/<id>.json`)
  で接続し、child → parent は immutable result (agent-result schema) で接続する。
- recovery 12-step algorithm を canonical Skill
  (`.agents/skills/recover-task/SKILL.md`) に集約し、adapter / doc / operations
  は canonical への参照のみを保持する。
- per-ticket durable checkpoint を recovery boundary とし、stale base /
  predecessor / conflicting integration を recovery step で reject する。
- external side-effect (`gh_release` / `gh_pr_comment` / `gh_issue_state` /
  `git_push` / `play_console_upload` / `sops_decrypt` / `github_project_field` /
  `custom`) は `external_side_effects[]` array に記録し、idempotency_key で
  replay 時に dedupe する。
- execution_generation は checkpoint write 時に increment し、stale generation
  からの branch integration / external write を reject する。

## Consequences

positive:
- ADR / Skill / schema の三層が canonical pointer で参照され、recovery の
  単一情報源が確定する。
- fencing token により split-brain recovery を防ぎ、stale continuation を
  防止できる。
- external side-effect journal により irreversible / destructive operation
  の retry が idempotent になる。

negative:
- execution_generation producer / fencing_token verifier / side-effect journal
  writer-reader code が現状 documentation のみで未実装 (B5 で対応予定)。
- per-repo `.agent-loop/` mirror が tastile-android 配下には未作成
  (現状 workspace root に集約)。policy §8 の per-repo  boundary と完全な
  整合は別 ticket で判断。

## Re-evaluation condition

1. checkpoint schema の required field を変更する場合 (例: execution_generation
   の semantic を counting から timestamp-based に変更)。
2. external side-effect の kind enum を拡張 / 縮小する場合。
3. per-repo `.agent-loop/` mirror の decision を確定する場合。

## Cross-references

- `docs/operations/recovery.md`
- `docs/operations/project-board.md`
- `.agents/skills/recover-task/SKILL.md`
- `/home/basic/work/tastile/.agent-loop/checkpoint.schema.json`
- `/home/basic/work/tastile/.agent-loop/agent-result.schema.json`
- `/home/basic/work/tastile/docs/agent-orchestration.md` §6 / §7 / §8