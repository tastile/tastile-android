---
name: project-board
description: tastile-android の GitHub Issue / Project board 操作で ADR-0009 必須 field を適用するときに使用する。
---

canonical Skill は `../.agents/skills/github-delivery/SKILL.md` である。
発火時にその全文を読み、binding workflow として実行する。この adapter に手順を複製しない。

adapter 自身は `../docs/adr/0009-github-projects-work-state.md`、
`../docs/operations/project-board.md` を参照する。
tastile-android の `area` field は `dashboard | mobile | account | design-system | native | sync | release`
の multi-select を使う。
