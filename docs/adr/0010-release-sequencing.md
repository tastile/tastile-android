---
adr_id: 0010
title: post-2026-09-19 release sequencing rule
status: proposed
date: 2026-09-12
deciders: rebuildup, tastile-android maintainers
supersedes: null
superseded_by: null
target_release: release-0-6-0
followup_issue: tastile/tastile-android#24
---

# ADR 0010 — post-2026-09-19 release sequencing rule

## Context

Sprint 2026-09-19 (Release 0.6.0) 中に、B09 (post-release sequencing
formalize) が意思決定され、ADR 化が必要とされた。design spec
`docs/superpowers/specs/2026-09-07-ui-rebuild-design.md` line 63 が ADR-0010
を直接参照しているが ADR ファイルが不在で参照が破断していた。

本 ADR は Issue #24 (B09) の followup として、2026-09-19 sprint の release 後
 sequencing rule を formalize する。

## Decision

次の sequencing rule を canonical として確定する。

1. weekly sprint cadence は 1 週間を維持し、sprint 開始時に `main` から
   `release-x-y-z` を作成する (ADR-0007 §Decision 整合)。
2. release-x-y-z への integration は target release branch が Draft release PR
   を持った後にのみ行う。first-difference 後は Draft release PR 必須。
3. durable ticket branch は Issue 番号 (digits) のみ。stacked ticket は
   immediate predecessor branch を base とし、target release trunk landing を
   Done boundary とする。
4. emergency patch 等で release scope / date を 1 週間以外に広げる場合、
   patch release branch から release PR を使用し `main` を直接変更しない。
6. zero-diff release branch は Draft release PR 不変条件の例外。first
   meaningful integrated difference が入った直後に Draft release PR を作成する。

## Consequences

positive:
- B09 由来の sequencing decision が ADR として永続化され、referencing docs
  (design spec, operations) が破断しない。
- Issue #24 (B09 followup) が正式に closed できる。
- weekly cadence / patch / emergency release が boundary で明確に分離される。

negative:
- ADR を変更する場合、design spec / operations への reference link も同時
  更新する必要がある。

## Re-evaluation condition

1. weekly sprint cadence を変更する場合。
2. emergency patch の発動条件を緩和 / 強化する場合。
3. ADR-0007 / ADR-0009 と矛盾する rule を追加する場合。

## Cross-references

- `tastile/tastile-android#24` (B09 followup)
- `docs/superpowers/specs/2026-09-07-ui-rebuild-design.md` line 63
- ADR-0007 (release-branch-workflow)
- ADR-0009 (Issue / PR lifecycle)