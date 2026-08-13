---
name: tastile-precommit-review
description: Use when independently reviewing a Tastile Android change immediately before an agent-initiated commit.
---

canonical Skill は `../.agents/skills/tastile-precommit-review/SKILL.md` である。発火時にその全文を読み、
binding workflow として実行する。この adapter に手順を複製しない。

adapter 自身は `docs/HARNESS.md` と `../docs/HARNESS.md`、`../.agent-loop/gate-root.ps1`、
`../.agent-loop/repositories.json` など root 正本へ `..` で遡って参照する。子 repo 単体で commit する場合は、
root の独立 review gate (`.agent-loop/Invoke-PreCommitReview.ps1`) を経由する。
