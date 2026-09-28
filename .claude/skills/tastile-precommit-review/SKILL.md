---
name: tastile-precommit-review
description: Use when independently reviewing a Tastile Android change immediately before an agent-initiated commit.
---

canonical Skill は `../.agents/skills/tastile-precommit-review/SKILL.md` である。発火時にその全文を読み、
binding workflow として実行する。この adapter に手順を複製しない。

adapter 自身は `../docs/adr/0008-structured-recovery-checkpoint.md`、
`../docs/operations/recovery.md` を参照する。子 repo 単体で commit する場合は、
本 repo の branch 命名 rule (ADR-0007) と `./gradlew verify` gate に従う。
