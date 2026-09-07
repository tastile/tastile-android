---
name: recover-task
description: tastile-android の AI agent context 消失時、または fresh agent が前タスクを引き継ぐときに使用する。
---

canonical Skill は workspace root の `../../.agents/skills/recover-task/SKILL.md` である。
発火時にその全文を読み、binding workflow として実行する。この adapter に手順を複製しない。

adapter 自身は root の `../../docs/HARNESS.md` §15、
`../../docs/adr/0008-structured-recovery-checkpoint.md`、
`../../.agent-loop/checkpoint.schema.json`、`../../.agent-loop/agent-result.schema.json`
を参照する。
