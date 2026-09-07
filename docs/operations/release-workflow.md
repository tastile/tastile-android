# Release sprint workflow (ADR-0007)

`sprint = 1 target release version`。PR / branch / tag の正本はこの ADR。
具体 release flow (tag 打ち / Play アップロード / GitHub Release) は
[`release-plan.md`](./release-plan.md) を参照。

## branch 階層

| layer | branch 名 | 何をするか |
| --- | --- | --- |
| released | `main` | 過去 release の integrated source state |
| active sprint | `release-<major>-<minor>-<patch>` | sprint ticket を集積する |
| ticket | `<issue-number>` (digits only) | 1 Issue = 1 branch |

## branch 取り方

```bash
# 1. 新しい sprint を起こす
git fetch origin
git switch main
git pull --ff-only
git switch -c release-0-4-0
git push -u origin release-0-4-0

# 2. ticket branch を起こす
git fetch origin
git switch release-0-4-0
git pull --ff-only
gh issue view 123 --json title,body   # Project 連動を確認
git switch -c 123
# ... 実装 ...
git push -u origin 123

# 3. Draft PR を開く (target: release-0-4-0)
gh pr create \
  --base release-0-4-0 --head 123 \
  --draft \
  --title "<conventional>: <summary>" \
  --body-file .github/PULL_REQUEST_TEMPLATE.md

# 4. Ready for review → Reviewer routing (sol-supervisor / lunar-implementer 等の mirror)
gh pr ready
```

## 禁止 branch 名

branch 名 canonical pattern は次の正規表現に従う。

```regex
^(?:[0-9]+|release-[0-9]+-[0-9]+-[0-9]+|main)$
```

`feature/*`、`fix-*`、`hotfix-*`、`wip-*` 等の prefix は禁止。`tastile-precommit-review`
Skill と `.agent-loop/Invoke-PreCommitReview.ps1` が commit 時に spot check する
(拡張は follow-up Enhancement)。

## PR body 必須 marker

PR body に `Issue:` / `Target Release:` / `Branch:` / `Execution Generation:` の
4 marker を必ず残す (canonical reference は
[`.claude/skills/release-branch-workflow/SKILL.md`](../../.claude/skills/release-branch-workflow/SKILL.md)
および workspace canonical
[`/.agents/skills/release-branch-workflow/SKILL.md`](../../../.agents/skills/release-branch-workflow/SKILL.md))。

## release PR (`release-x-y-z -> main`)

release 完了時の release PR は次の情報を必ず含む。

- release goal (1 段落)
- included Issues / PRs (`resolves #<n>` 表記)
- breaking changes
- migration notes
- full validation result (`./gradlew verify` / `assembleDebug` / `lintDebug` / focal test)
- known limitations
- version / release metadata (versionName / versionCode)

merge 後 contributor が `gpg` / `ssh-key` 署名付きで `v<version>` tag を打つ
(contributor の鍵)。`.github/workflows/release.yml` が AAB / Play / GitHub Release
に流す (`SECURITY` secret 経由)。

## 関連 ADR / 関連 Skill

- [ADR-0007](../../../docs/adr/0007-release-branch-and-ticket-workflow.md) (root)
- [`.agents/skills/release-branch-workflow/SKILL.md`](../../../.agents/skills/release-branch-workflow/SKILL.md) (root canonical)
- [`.claude/skills/release-branch-workflow/SKILL.md`](../../.claude/skills/release-branch-workflow/SKILL.md) (child Claude adapter)
- [`.github/PULL_REQUEST_TEMPLATE.md`](../../.github/PULL_REQUEST_TEMPLATE.md)
- [`./release-plan.md`](./release-plan.md) (per-version 具体手順)
