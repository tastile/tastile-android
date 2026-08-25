# `.agents/upstream-skills/` — Vendored Third-Party Skills

このディレクトリは 4 つの upstream GitHub repository を `git subtree --squash` で
取り込んだスナップショットである。`upstream-skills/<repo>/` 配下のファイルは upstream の
そのままで、手動編集禁止。更新は `git subtree pull` で行う。

## 取り込み済み upstream

| Subtree prefix | Upstream repo | 取得 branch | 取得 SHA (12) | License |
| --- | --- | --- | --- | --- |
| `upstream-skills/android-skills/` | https://github.com/android/skills | `main` | `aaca635061a4` | Apache-2.0 |
| `upstream-skills/material-3-skill/` | https://github.com/hamen/material-3-skill | `master` | `14385f2bf380` | Apache-2.0 |
| `upstream-skills/chrisbanes-skills/` | https://github.com/chrisbanes/skills | `main` | `948acbbd6c44` | Apache-2.0 |
| `upstream-skills/compose-skill/` | https://github.com/hamen/compose_skill | `main` | `f815c31d6cc1` | Apache-2.0 |

各 subtree の完全な commit SHA を再確認するには `git log -1 --format=%H <prefix>` を実行する。

## 設計の前提 (抜粋)

- **触らない**: `.agents/upstream-skills/<repo>/` 配下を直接編集しない。編集すると
  `git subtree pull` が conflict する。必要なら PR を upstream に出す。
- **発見経路**: ツールや人間は `.agents/skills/<name>/SKILL.md` (canonical pointer) を
  起点にする。`upstream-skills/` は canonical ではなく raw スナップショット。
- **License**: 各 upstream の `LICENSE` / `LICENSE.txt` が subtree 内に保持される。
  Apache-2.0 §4(a) に基づく attribution は各 pointer の 1 行目で行う。
- **参照整合性**: `description:` frontmatter は pointer と upstream で verbatim 同期する。
  `rg '^description: ' .agents/skills/<name>/SKILL.md` と upstream 側を比較すれば
  drift が分かる。

## 将来の更新手順

新しい upstream を取り込むとき:

```bash
cd <repo root>
git fetch <upstream-remote> <branch>
NEW_SHA=$(git ls-remote <upstream-remote> <branch> | awk '{print $1}')
git subtree pull --prefix=.agents/upstream-skills/<repo-slug> \
    --squash --message "subtree: pull upstream <owner>/<repo> @ ${NEW_SHA:0:12}" \
    <upstream-remote> <branch>

# 影響する pointer (`.agents/skills/<name>/SKILL.md` と `.claude/skills/<name>/SKILL.md`) の
# `description:` を upstream 新版に同期する。`name:` が upstream で変わったら
# directory 名も rename する。
```

`description:` が upstream で non-backward-compatible に変わった場合は
`docs/adr/0006-android-ui-skills-vendoring.md` の re-evaluation trigger に従い
判断する。
