# ADR-0008: structured recovery checkpoint

- Status: Accepted
- Date: 2026-09-15
- Scope: `tastile-android/`

## Context

model/session context loss、agent crash、sandbox/provider 消失を failure model
とする。native session resume は高速経路に過ぎず、canonical path は fresh
agent が durable project state から reconstruct することである。詳細手順は
`docs/operations/recovery.md`、binding workflow は
`.agents/skills/agent-recovery/SKILL.md`。checkpoint schema は
`.agent-loop/checkpoint.schema.json`、worker result schema は
`.agent-loop/agent-result.schema.json`。

## Decision

### 1. Durable recovery sources (優先順)

1. GitHub Issue / dependency state (canonical SoT)
2. target release branch (`release-x-y-z`)
3. ticket branch / remote commit graph
4. Draft/Ready PR / assignee / reviewer / labels / review / CI state
5. stack predecessor / pinned predecessor SHA
6. committed design / ADR / Skills / docs
7. immutable worker/subagent results
8. structured recovery checkpoint

会話履歴、native session ID、Supervisor local DB、shell history、IDE state は
transient optimization であり唯一の SoT にしない。

### 2. Soft / hard checkpoint

- soft: 同一 host/sandbox 向け。local immutable ref、filesystem snapshot、
  Supervisor journal、native session state。
- hard: sandbox/provider 消失後も復旧できる境界。meaningful code/work state が
  durable remote (canonical remote の commit、remote head identity、Draft PR)
  から到達可能であること。durable ticket では recorded commit が remote で
  到達可能かつ head SHA + Draft PR が追跡できること。release branch は
  zero-diff の間だけ Draft release PR 不要、first difference 後は必須。

全小 edit を remote commit して history を汚す必要はない。RPO/RTO target:

| target | value |
| --- | --- |
| soft RPO | 30s (recent commit の snapshot) |
| hard RPO | 1 commit (mandatory checkpoint.json push) |
| soft RTO | 60s (within session lifetime) |
| hard RTO | 5 min (fresh agent → checkpoint → branch → resume) |

3 sprint 連続で target 違反が観測されたら本 ADR を revise する。

### 3. Checkpoint trigger

meaningful milestone、risky refactor/migration、child spawn 前後、child result
integration 前後、long validation 前後、external side effect 前後、user/external
input 待ち、provider TTL/shutdown 接近、graceful shutdown signal、context limit
接近の前後で checkpoint を検討する。private chain-of-thought は保存しない。

### 4. Execution generation / fencing

Supervisor は task ごとに lease または generation/fencing token を持たせる。
recovery 時に `execution_generation` を進め、worker result へ generation を付与
し、stale generation からの branch integration / external write を拒否する。
heartbeat 消失だけで同一 side effect を再実行しない。同じ ticket branch へ
複数 generation が同時 push することを通常運用にしない。

### 5. Parent loss 後の child 再発見

child lifecycle は Supervisor/control plane が所有する。parent 死亡で safe な
child を即 cancel しない。recovered coordinator は child 再発見 →
generation 確認 → running/completed/failed/orphaned 分類 → completed result を
immutable result として回収 → durable branch child は remote head/Draft PR を
reconcile する。stale child result は自動統合しない。
