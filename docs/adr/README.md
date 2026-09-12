# docs/adr/ — Architecture Decision Records

Tastile Android の long-lived な意思決定を記録する。rebuildup/project-init
policy §20 に基づき、設計 / アーキテクチャ / 運用に変更を加える前に ADR を
起票する。各 ADR は frontmatter + 本文で構成する。

## Frontmatter テンプレート (canonical)

```yaml
---
adr_id: "NNNN"             # 4桁連番 (0001 起点)
title: "<subject>"          # 1 行サマリ
status: proposed            # proposed | accepted | deprecated | superseded
date: "YYYY-MM-DD"          # decision date (ISO 8601)
deciders: ["@github-id", ...]
supersedes: null            # supersede 対象 ADR の id (なければ null)
superseded_by: null         # 本 ADR を supersede した ADR の id (なければ null)
target_release: "release-x-y-z"  # 主要な適用対象 release (なければ "-")
related_skills:             # 任意
  - ".agents/skills/<name>/SKILL.md"
related_schemas:            # 任意
  - "<schema path>"
related_components:         # 任意
  - "<component path>"
---
```

## status 遷移

- `proposed` → `accepted` (decision を採用)
- `accepted` → `deprecated` (decision を廃止予定)
- `accepted` → `superseded` (別 ADR が decision を引き継ぎ)
- `superseded` / `deprecated` への遷移時は `superseded_by` / 取消 ADR を
  必ず明記する。

## 一覧

| id | title | status | date | target_release |
| --- | --- | --- | --- | --- |
| [0001](./0001-kotlin-lsp-toolchain.md) | Kotlin LSP を project-local に導入する | accepted | 2026-08-23 | - |
| [0002](./0002-rejected-and-superseded-history.md) | 0002-0005 連番欠落の理由 | accepted | 2026-09-12 | - |
| [0006](./0006-android-ui-skills-vendoring.md) | Android/Compose 向け third-party Skill の vendoring | accepted | 2026-08-25 | - |
| [0007](./0007-release-branch-workflow.md) | release-branch-workflow と ticket lifecycle | accepted | 2026-09-12 | release-0-6-0 |
| [0008](./0008-recovery-checkpoint.md) | structured recovery checkpoint と fencing token | accepted | 2026-09-12 | release-0-6-0 |
| [0009](./0009-issue-pr-lifecycle.md) | Issue / PR lifecycle と GitHub Projects 連動 | proposed | 2026-09-12 | release-0-6-0 |
| [0010](./0010-release-sequencing.md) | post-2026-09-19 release sequencing rule | proposed | 2026-09-12 | release-0-6-0 |
| [0011](./0011-architecture-migration.md) | tastile-core への execution / replay / projected state migration | proposed | 2026-09-12 | release-0-6-0 |

## 連番欠落の扱い (0002-0005)

init 時点 (2026-09-12) で `0002` 〜 `0005` は意図的に空番である
(各 ADR は `0002-rejected-and-superseded-history.md` を参照)。

## 関連

- workspace canonical: `../.agents/skills/project-init/SKILL.md`
- rebuildup/project-init policy §20 (Architecture / design / ADR)