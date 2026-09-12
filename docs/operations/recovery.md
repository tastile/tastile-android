# Recovery & checkpoint (ADR-0008)

AI agent の conversation / session / sandbox / provider 消失後に fresh agent が
再構成するための canonical procedure。native resume は高速経路に過ぎず、canonical
path は conversation 履歴を推測しない再構成である。

## soft / hard checkpoint

| 種別 | 用途 | 保存先 |
| --- | --- | --- |
| soft | 同一 host / sandbox で同一 agent が再開 | `.tmp/` filesystem snapshot / native session state |
| hard | sandbox / provider 消失後も remote-durable | GitHub Issue / target release branch / ticket branch / PR / commit された checkpoint.json |

commit 境界 recovery なら soft = `.agent-loop/Invoke-PreCommitReview.ps1` の
HEAD snapshot primitive をそのまま使える。`schema_version` を含む checkpoint
object を hard 境界で書く。

## 12 手順 recovery algorithm

canonical reference は
[`/.agents/skills/recover-task/SKILL.md`](../../../.agents/skills/recover-task/SKILL.md)。
fresh agent が次の手順を再帰的に走査する。

1. Issue / target release を特定する (`gh issue list --search`/`view`)。
2. ticket branch の remote commit graph を `git fetch --all` で取得。
3. latest valid checkpoint を `.agent-loop/checkpoint.schema.json` で parse。
4. canonical policy / design / decision refs を確認 (`docs/adr/`、`docs/HARNESS.md`、
   child `AGENTS.md`/`CLAUDE.md`、本ファイル)。
5. active_children を `.codex/agents/*.toml` と `.claude/agents/*.md` の catalog
   から再発見。
6. checkpoint から workspace を再構成 (`git checkout <branch>` + `git reset --hard
   <base_sha>`)。
7. `completed_steps` / `pending_validation` を再評価。
8. `external_side_effects` の actual remote state を `gh` / `git` / `sops` で検証。
9. stale base / conflict を `git fetch` で比較。
10. remaining plan を `next_steps` から再構成。
11. safe な最小 verification (`pwsh -File .agent-loop/gate-root.ps1`) を実行。
12. `execution_generation` を increment、`updated_at` を更新、checkpoint を push。

## child → parent result

`.agent-loop/agent-result.schema.json` (新規) の immutable result を child が返す。
parent は `fencing_token` 不一致の result を reject する。`verdict` は
`pass | fail | blocked | abandoned`。

## recovery drill

weekly cron が `.github/workflows/recovery-drill.yml` で:
- Pester suite (`Invoke-PreCommitReview.Tests.ps1` / `Test-AgentAdapters.ps1` /
  `Test-ReviewSkills.ps1`)
- `.agent-loop/gate-root.ps1`
- synthetic checkpoint round-trip (schema parse + minimum required keys)

を実行する。failure → exit 1。`recovery-drill.yml` cron は月曜早朝 (UTC 03:17) で
contributor push と時間帯が被らないよう調整する。

## RPO / RTO target

初期値:

| target | value |
| --- | --- |
| soft RPO | 30s (recent commit の snapshot) |
| hard RPO | 1 commit (mandatory checkpoint.json push) |
| soft RTO | 60s (within session lifetime) |
| hard RTO | 5 min (fresh agent → checkpoint → branch → resume) |

target 違反が運用上 3 sprint 連続で観測された場合、ADR-0008 を revise する。

## 関連 ADR / 関連 Skill

- [ADR-0008](../../../docs/adr/0008-structured-recovery-checkpoint.md) (root)
- [`.agent-loop/checkpoint.schema.json`](../../../.agent-loop/checkpoint.schema.json)
- [`.agent-loop/agent-result.schema.json`](../../../.agent-loop/agent-result.schema.json)
- [`.agents/skills/recover-task/SKILL.md`](../../../.agents/skills/recover-task/SKILL.md) (root canonical)
- [`.claude/skills/recover-task/SKILL.md`](../../.claude/skills/recover-task/SKILL.md) (child Claude adapter)
- [`.agents/skills/subagent-coordination/SKILL.md`](../../../.agents/skills/subagent-coordination/SKILL.md)
