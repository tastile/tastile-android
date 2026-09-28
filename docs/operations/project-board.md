# Project board (ADR-0009)

durable work item = GitHub Issue。Project v2 が canonical work state。各 Issue は
Project に linked、status 遷移ごとに必須 field を満たす。Project metadata は GitHub
UI / GraphQL API が canonical、repository 内では本ファイルと ADR-0009 のみを pin する。

## Status machine

```text
Backlog --> Ready --> In Progress --> In Review --> Done
```

| from | to | action |
| --- | --- | --- |
| (initial) | Backlog | Issue 起票 |
| Backlog | Ready | `priority` / `size` / `target_version` 必須 |
| Ready | In Progress | assignee 確定、`branch = <issue-number>` |
| In Progress | In Review | linked PR が `resolves #<n>` + 必須 marker 4 個 |
| In Review | Done | PR merged / closed、Issue closed |

## 必須 field (ADR-0009 §D-2)

| field | 値域 | 説明 |
| --- | --- | --- |
| `priority` | `P0 \| P1 \| P2 \| P3` | current sprint / next sprint / backlog |
| `size` | `XS \| S \| M \| L \| XL` | rough effort estimate |
| `target_version` | `release-x-y-z` 文字列 | 例: `release-0-4-0`。空値で backlog |
| `area` | multi-select | `dashboard \| mobile \| account \| design-system \| native \| sync \| release` |
| `execution_generation` | number ≥ 1 | ADR-0008 連動、recovery ごとに increment |

WIP cap: `In Progress` ≤ 3 ticket / owner (dry-run only; 強制降格は別 ADR)。

## 自動 / 手動

| automation | owner | trigger |
| --- | --- | --- |
| Issue template → Project field 自動提案 | Issue 起票者 | `.github/ISSUE_TEMPLATE/{bug,feature,chore}.yml` |
| Project field 不在の検出 | weekly cron | `.github/workflows/recovery-drill.yml` の拡張予定 |
| ticket trunk landing 後の Issue 明示 close + status Done | 手動 (trunk landing 確認後) | ticket changes が `release-x-y-z` へ land してから close。non-default base では `resolves #<n>` だけに依存しない (ADR-0009) |
| Release PR merge → board 同期 | manual | `release-branch-workflow` Skill |

## Issue 連動コマンド (例)

```bash
# 1. 起票
gh issue create --template feature.yml --title "[feature]: auth refresh"

# 2. Required field を埋める
gh issue edit <n> \
  --add-label "priority/P0" \
  --add-label "size/M" \
  --add-label "area/account"

# 3. Project に link
gh project link <project_id> --owner tastile --repo tastile-android

# 4. Status を Ready に進める (Project 経由)
#    GUI: Projects > board > Status = Ready
#    CLI: gh project item-edit --project-id <id> --id <item> --field-id Status --single-select-option-id <ready-id>
```

## 関連 ADR / 関連 Skill

- [ADR-0009](../adr/0009-github-projects-work-state.md)
- [ADR-0007](../adr/0007-release-branch-and-ticket-workflow.md)
- [ADR-0014](../adr/0014-linear-profile-non-adoption.md) (Linear 不採用)
- [`.agents/skills/github-delivery/SKILL.md`](../../.agents/skills/github-delivery/SKILL.md)
- [`.claude/skills/project-board/SKILL.md`](../../.claude/skills/project-board/SKILL.md) (Claude adapter、workspace-root canonical への pointer)
- [`.github/ISSUE_TEMPLATE/bug.yml`](../../.github/ISSUE_TEMPLATE/bug.yml)
- [`.github/ISSUE_TEMPLATE/feature.yml`](../../.github/ISSUE_TEMPLATE/feature.yml)
- [`.github/ISSUE_TEMPLATE/chore.yml`](../../.github/ISSUE_TEMPLATE/chore.yml)
