---
name: release-branch-workflow
description: Tastile Android で sprint planning、GitHub Issue 作成、PR 開始、release 統合を行う直前に使用する。
---

canonical Skill は `../.agents/skills/github-delivery/SKILL.md`
である。発火時にその全文を読み、binding workflow として実行する。この adapter に手順を複製しない。

adapter 自身は `../docs/adr/0007-release-branch-and-ticket-workflow.md`、
`../docs/adr/0009-github-projects-work-state.md`、
`../docs/adr/0012-release-merge-authorization.md`、
`../docs/operations/release-workflow.md` を参照する。
tastile-android local の override が必要になったときのみ本ファイルへ追記する。
