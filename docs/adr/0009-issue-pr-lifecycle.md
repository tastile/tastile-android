---
adr_id: 0009
title: Issue / PR lifecycle と GitHub Projects 連動
status: proposed
date: 2026-09-12
deciders: rebuildup, tastile-android maintainers
supersedes: null
superseded_by: null
target_release: release-0-6-0
blocking_issue: tastile/tastile-android#10 (status:blocked)
related_skills:
  - .agents/skills/project-board/SKILL.md
  - .agents/skills/release-branch-workflow/SKILL.md
---

# ADR 0009 — Issue / PR lifecycle と GitHub Projects 連動

## Context

Tastile Android の durable work state は GitHub Issues + Projects で管理するが、
現状 Project 接続が OPEN backlog Issue #10 (status:blocked) であり dependency
graph が branch topology + Issue labels に分散している。release-branch-workflow
Skill と operations/project-board.md は `project-board` Skill 連動を ADR-0009
§7-3 で参照しているが、ADR 自体が不在で参照が破断していた
(`docs/operations/project-board.md` line 65)。

## Decision

Issue / PR lifecycle の正本を ADR-0009 として formalize し、次の invariant を
確定する。

- durable work item の dependency graph は GitHub Issues / Projects を
  canonical dependency SoT とし、branch parent-child relation のみでは
  表現しない。
- Issue body に `priority / size / target_release / area` を必須項目として
  含める。priority は P0-P3、size は XL-XS、target_release は active release
  branch、area は multi-select (dashboard | mobile | account | design-system |
  native | sync | release)。
- PR body に 4 marker (Issue / Target Release / Branch / Execution Generation)
  + Validation + Project fields + Checks + Notes を必須配置する (PULL_REQUEST_TEMPLATE.md
  と整合)。
- Project Status 列は `Backlog → Ready → In Progress → In Review → Done` を
  default とし、`blocked` / `stack-ready` / `integrated` は Project field で
  区別する (Status 列を増やさない)。
- Issue / PR 状態同期は GitHub Projects + workflow automation で enforce し、
  label 手動運用は禁止。
- stacked ticket の Done boundary は ticket changes が target release trunk
  へ land したこと。intermediate predecessor branch への merge だけでは
  Done にしない。

## Consequences

positive:
- dependency SoT が Project に一元化され、branch topology の暗黙依存が消える。
- Issue form の dropdown で `target_release` / `area` を捕捉でき、ADR-0009
  §7-3 "Project 連動を必須" が label 手動運用なしで満たせる。
- stacked ticket の Done boundary が明示され、Issue close / Project Done の
  race を防げる。

negative:
- 現状 GitHub Projects connection (Issue #10 status:blocked) が解決される
  まで ADR-0009 の強制は部分的。blocker 解消まで Issue labels + branch
  topology による暫定運用が継続する。
- Project 連動 automation のメンテ cost が増える。

## Re-evaluation condition

1. Issue #10 (status:blocked) が closed し Projects 接続が live になった時。
2. Project Status 列の default を変更する場合。
3. label / Project field のどちらかに統一する場合。

## Cross-references

- `docs/operations/project-board.md`
- `.agents/skills/project-board/SKILL.md`
- `.agents/skills/release-branch-workflow/SKILL.md`
- `tastile/tastile-android#10` (Projects connection blocker)
- `tastile/tastile-android/.github/PULL_REQUEST_TEMPLATE.md`
- `tastile/tastile-android/.github/ISSUE_TEMPLATE/{feature,chore,bug}.yml`