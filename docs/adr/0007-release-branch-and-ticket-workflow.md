# ADR-0007: release branch と ticket 駆動 workflow

- Status: Accepted
- Date: 2026-09-15
- Scope: `tastile-android/`

## Context

`sprint = 1 target release version` を `main` / `release-x-y-z` / ticket branch の
3 層で表現する。durable work item は GitHub Issue、dependency SoT は Issue
dependency graph である。branch topology だけで dependency を管理しない。
planning control plane は GitHub Projects (ADR-0009)。詳細手順は
`docs/operations/release-workflow.md`、binding workflow は
`.agents/skills/github-delivery/SKILL.md`。

## Decision

### 1. Branch 層と命名

- released: `main` (過去 release の integrated source state)
- active sprint: `release-<major>-<minor>-<patch>` (通常 1 週間、1 sprint = 1 version)
- ticket: `<issue-number>` (digits only)。`issue/` prefix、slug、title、work type 禁止

canonical pattern: `^(?:[0-9]+|release-[0-9]+-[0-9]+-[0-9]+|main)$`
(PR template と `tastile-precommit-review` Skill が spot check する)

### 2. Sprint cadence

通常 sprint は 1 週間。1 週間は planning cadence であり工期保証ではない。
release / roadmap / milestone の見積もりは `agent-delivery-estimation`
Skill の evidence-based policy に従い、主観的日数や linear agent scaling を
根拠にしない。緊急 patch も `main` 直変更せず patch release branch +
release PR を使う。

### 3. Ticket branch 開始手順 (全 actor に適用)

1. durable branch 作成
2. first meaningful commit を直ちに作成
3. canonical remote (`origin`) へ publish
4. remote head SHA が commit SHA と一致することを確認
5. 直ちに Draft PR 作成 (independent: base=`release-x-y-z`、
   hard dependency: base=immediate predecessor ticket branch)
6. Issue linkage / assignee / reviewer / labels / target release /
   stack context を設定
7. implementation 継続

publish + Draft PR なしで active implementation を継続しない。publish 権限の
ない worker は first commit 後ただちに Coordinator/Supervisor へ handoff する。

### 4. Stacked PR

same repository + same target release + real linear hard dependency の場合のみ、
dependent ticket PR を immediate predecessor ticket branch へ stack してよい。
stack は canonical Issue dependency graph の linear path の projection であり、
branching DAG を無理に 1 本化しない。stack-ready execution では reviewable
immutable predecessor snapshot があれば開始可能。predecessor 更新で downstream
SHA が変わったら required validation を新 SHA で再実行する。

### 5. Ticket Done 境界

ticket changes が target release trunk (`release-x-y-z`) へ land してから
GitHub Issue を明示 close する。intermediate predecessor branch への merge
だけでは Done にしない。non-default branch への merge では closing keyword
だけに依存しない。Project status 更新は ADR-0009 に従う。

### 6. `main` 保護と release-only 統合

`main` は ruleset `protect-main-and-releases` で保護する (deletion 禁止、
non-fast-forward 禁止、linear history、PR 必須、thread resolution 必須)。
`main` への正規 delivery path は `release-x-y-z -> main` の release PR のみ。
ruleset だけで head pattern を制約できないため、`base == main` の PR で head
が `release-*` であることを検証する required check
(`.github/workflows/release-source-check.yml`) を併用する (ADR-0012 も参照)。
