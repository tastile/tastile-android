---
adr_id: 0002
title: 0002-0005 連番欠落の理由
status: accepted
date: 2026-09-12
deciders: rebuildup, tastile-android maintainers
supersedes: null
superseded_by: null
target_release: "-"
---

# ADR 0002 — 0002-0005 連番欠落の理由

## Context

2026-09-12 時点で `docs/adr/` には `0001-kotlin-lsp-toolchain.md` と
`0006-android-ui-skills-vendoring.md` のみが存在し、`0002` 〜 `0005` の
連番が空いている。これは init 時の numbering drift であり、ADR を
連番で参照する operations / design-spec からの reference が破断する原因の
一つとなっていた。

## Decision

`0002` 〜 `0005` は意図的に空番とし、本 ADR (`0002`) でその理由を canonical に
明記する。

- 0003, 0004, 0005: 当時の意思決定が口頭 / commit message のみで ADR 化されず
  rejected / superseded history として残っていない。今後の ADR 起票で
  連番が埋まる際、本 ADR に superseded chain を追記する必要はない (該当
  decision は rejected で参照不要)。
- 0002: 本 ADR として accepted。0002 自体を連番欠落理由の canonical として
  以降の ADR 起票者が参照できるようにする。

## Consequences

positive:
- 連番欠落が意図的であることが ADR として永続化される。
- 新規 ADR 起票者が連番割り当て時に本 ADR を参照し、空番の意図を理解できる。
- README の一覧表で 0002-0005 が "意図的に空" と表示される。

negative:
- 連番が詰まる形 (0002 の次は 0006) になり、新規 ADR は 0007 以降に
  採番される。採番管理は maintainer 責任。

## Re-evaluation condition

1. rejected / superseded な過去の意思決定を retrospective に formalize
   する必要が出た場合、空番 0003-0005 を埋める retrospective ADR を起票する
   可能性がある。
2. ADR 採番ルールを連番以外 (semver / hash / 日付) に変更する場合。

## Cross-references

- `docs/adr/README.md` (一覧表)
- `docs/adr/0001-kotlin-lsp-toolchain.md` (Accepted 2026-08-23)
- `docs/adr/0006-android-ui-skills-vendoring.md` (Accepted 2026-08-25)
- `docs/adr/0007-release-branch-workflow.md` (Accepted 2026-09-12)