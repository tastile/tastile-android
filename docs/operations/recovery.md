# Recovery & checkpoint (ADR-0008)

AI agent の conversation / session / sandbox / provider 消失後に fresh agent が
再構成するための canonical procedure。native resume は高速経路に過ぎず、canonical
path は conversation 履歴を推測しない再構成である。

## soft / hard checkpoint

| 種別 | 用途 | 保存先 |
| --- | --- | --- |
| soft | 同一 host / sandbox で同一 agent が再開 | `.tmp/` filesystem snapshot / native session state |
| hard | sandbox / provider 消失後も remote-durable | GitHub Issue / target release branch / ticket branch / PR / commit された checkpoint.json |

commit 境界 recovery なら soft 境界として HEAD snapshot を使う。
`schema_version` を含む checkpoint object を hard 境界で書く。

## 12 手順 recovery algorithm

canonical reference は
[`.agents/skills/agent-recovery/SKILL.md`](../../.agents/skills/agent-recovery/SKILL.md)。
fresh agent が次の手順を再帰的に走査する。

1. Issue / target release を特定する (`gh issue list --search`/`view`)。
2. ticket branch の remote commit graph を `git fetch --all` で取得。
3. latest valid checkpoint を `.agent-loop/checkpoint.schema.json` で parse。
4. canonical policy / design / decision refs を確認 (`docs/adr/`、
   child `AGENTS.md`/`CLAUDE.md`、本ファイル)。
5. active_children を checkpoint の `active_children` と
   `gh pr list --head <branch>` / Supervisor 状態から再発見
   (agent catalog は将来拡張。現状は checkpoint + `gh` が正本)。
6. checkpoint から workspace を再構成 (`git checkout <branch>` + `git reset --hard
   <base_sha>`)。
7. `completed_steps` / `pending_validation` を再評価。
8. `external_side_effects` の actual remote state を `gh` / `git` / `sops` で検証。
9. stale base / conflict を `git fetch` で比較。
10. remaining plan を `next_steps` から再構成。
11. safe な最小 verification (`git status` による state 確認 +
    該当する `./gradlew` gate) を実行。
12. `execution_generation` を increment、`updated_at` を更新、checkpoint を push。

## child → parent result

`.agent-loop/agent-result.schema.json` (新規) の immutable result を child が返す。
parent は `fencing_token` 不一致の result を reject する。`verdict` は
`pass | fail | blocked | abandoned`。

## recovery drill

weekly cron が `.github/workflows/recovery-drill.yml` で synthetic checkpoint
round-trip (`.agent-loop/checkpoint.schema.json` /
`.agent-loop/agent-result.schema.json` の parse + required keys 検証) を実行する。
failure → exit 1。`recovery-drill.yml` cron は月曜早朝 (UTC 03:17) で
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

- [ADR-0008](../adr/0008-structured-recovery-checkpoint.md)
- [`.agent-loop/checkpoint.schema.json`](../../.agent-loop/checkpoint.schema.json)
- [`.agent-loop/agent-result.schema.json`](../../.agent-loop/agent-result.schema.json)
- [`.agents/skills/agent-recovery/SKILL.md`](../../.agents/skills/agent-recovery/SKILL.md)
- [`.claude/skills/recover-task/SKILL.md`](../../.claude/skills/recover-task/SKILL.md) (Claude adapter、workspace-root canonical への pointer)
- [`.agents/skills/parallel-orchestration/SKILL.md`](../../.agents/skills/parallel-orchestration/SKILL.md)
