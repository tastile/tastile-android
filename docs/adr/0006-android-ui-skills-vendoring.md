# ADR-0006: Android/Compose 向け third-party Skill の vendoring

- Status: Accepted
- Date: 2026-08-25
- Scope: `tastile-android/`

## Context

`app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account}/` 配下の Compose UI を、
Material 3 / adaptive / edge-to-edge / Navigation 3 / Compose performance 等の最新 Google 公式
および community 標準に沿って継続的に改善していくため、15 件の third-party Claude Skill を
この repository 内にプロジェクトローカルへ導入する。

採用した upstream (4 repo, 15 skill, すべて Apache-2.0):

- [android/skills](https://github.com/android/skills) (Google 公式) — `adaptive`,
  `edge-to-edge`, `navigation-3`, `testing-setup`, `styles`, `android-cli`
- [hamen/material-3-skill](https://github.com/hamen/material-3-skill) — `material-3`
- [chrisbanes/skills](https://github.com/chrisbanes/skills) — `compose-state-and-effects`,
  `compose-performance`, `compose-component-design`, `compose-animations`,
  `compose-focus-navigation`, `compose-ui-testing-patterns`
- [hamen/compose_skill](https://github.com/hamen/compose_skill) — `compose-agent`,
  `jetpack-compose-audit`

検討した代替案:

- **手動 vendor (ファイルコピー)**: 個別ファイルを選んでコピー。upstream drift 追従が手作業。
  採用案と比較し、refresh のたびに手作業が必要で誤りやすい。
- **Plugin marketplace (プロジェクトローカルファイルなし)**: `/plugin marketplace add ...` を
  各自が実行。`AGENTS.md` の方針「contributor が同じ agent 体験を得る」と相性が悪い。
- **`git submodule`**: pointer 方式を取れない。submodule は commit pin だが working tree へ
 自動展開されず agent loader から直接見えない。
- **`git subtree --squash` (採用)**: upstream 全体を取り込みつつ、canonical pointer
  で agent loader に見せる surface を選別。`git subtree pull` で 1 commit 更新。

## Decision

### 1. Prefix は `.agents/upstream-skills/<repo-slug>/`

- Root `.gitignore` line 21-22 で `reference/` と `.reference/` は ignore され、
  `git subtree add` が書けない。
- `.claude/*` は line 34-38 で大部分 ignore されるが `.claude/skills/` は whitelist。
- `.agents/` は ignore されておらず tracked。
- `upstream-skills/` という sub-namespace で「canonical skill ではない」と一目で区別できる。
- `AGENTS.md` line 9 「`.agents/skills/` が canonical home」という文と矛盾しない。

### 2. Canonical pointer + Claude adapter の 2 層

- `.agents/skills/<name>/SKILL.md` — canonical pointer。本体は upstream path を指す 5 行
  + ライセンス attribution 1 行 + Tastile 固有の注記 (該当 skill のみ)。
- `.claude/skills/<name>/SKILL.md` — Claude Code adapter。既存 4 skill (buildconfig-guard-check
  等) と同じ boilerplate。canonical を指し、upstream path も併記。
- `description:` frontmatter は両者で verbatim 同一 (これが agent loader の trigger 一致
  に必要)。

### 3. `git subtree add --squash` を必ず使う

- 各 upstream が 1 squash commit に圧縮され、parent log が clean。
- 将来の `git subtree pull --squash` も 1 commit で済む。
- `upstream-skills/` 配下を編集しない運用ルール (README に明記) と組み合わせれば
  conflict しない。

### 4. Default branch を pin

- `hamen/material-3-skill` は `master`、他 3 つは `main`。`git subtree {add,pull}` で
  branch を明示する。default branch 変更に引っかからない。

### 5. 編集禁止エリア

- `.agents/upstream-skills/<repo>/` 配下は直接編集禁止。必要なら upstream に PR を出して
  subtree pull で取り込む。

### 6. License 取り扱い

- 全 4 upstream が Apache-2.0。`LICENSE` / `LICENSE.txt` は各 subtree 内に保持される
  (subtree add した upstream のファイル群をそのまま tracking する)。
- 各 canonical pointer の 1 行目に attribution
  (`> Upstream: <repo> @ <sha-12> · Apache-2.0.`) を入れる。
- Tastile 固有の注記 (design system の cross-reference 等) は pointer 内にローカル
  extension として書き、upstream subtree は触らない。

## 結果として作成された layout

```
.agents/
├── skills/                          ← canonical pointer (15 new + 4 existing)
│   ├── <name>/SKILL.md              15 個 (本 ADR の対象)
│   └── (buildconfig-guard-check / design-system-imports-check /
│        openapi-spec-refresh / tastile-precommit-review — 既存, 変更なし)
└── upstream-skills/                 ← subtree root
    ├── android-skills/              @ aaca635061a4
    ├── material-3-skill/            @ 14385f2bf380
    ├── chrisbanes-skills/           @ 948acbbd6c44
    └── compose-skill/               @ f815c31d6cc1

.claude/skills/                      ← Claude Code adapter (15 new + 4 existing)
docs/adr/0006-android-ui-skills-vendoring.md  (本ファイル)
```

## Trade-offs

- **+ full upstream tree を取り込む**: refresh が `git subtree pull` 1 コマンドで済み、
  license / provenance が保全される。
- **− 不要な upstream skill も tree に残る**: 今回採用していない 38 個の skill (android/skills
  の残り 16 個、chrisbanes/skills の 9 個、hamen/compose_skill の 0 個) も `upstream-skills/`
  配下に存在する。`AGENTS.md` 冒頭の list に名前がないので agent loader には見えず、
  サイズは増えるが機能影響なし。
- **− pointer の `description:` を verbatim 同期する必要がある**: upstream drift 時に
  自動 re-sync はしない。`git subtree pull` 後に手動で 5 行 diff する運用ルール
  (`.agents/upstream-skills/README.md` に記載) で吸収する。
- **+ Build-time guard を変更不要**: `verifyDesignSystemImports` (lines 176-204) と
  `verifyNoEmbeddedServerSecrets` (lines 206-225) は `app/src/main` しか scan しないので、
  新規 SKILL.md ファイルで build が壊れることはない。

## Security / License 考察

- 全 4 upstream が Apache-2.0 であり、Tastile はそれぞれを再配布可能。
- 各 subtree 内の `LICENSE` ファイルが保持され、pointer 1 行で attribution しているので
  Apache-2.0 §4(a) を満たす。
- `compose-skill/skills/jetpack-compose-audit/SKILL.md` の frontmatter に `allowed-tools`
  (`Read, Glob, Grep, Write, Bash, Agent`) が宣言されているが、これは skill 発火時に
  Claude へ許可する tool を upstream 設計で指定しているだけであり、tastile リポジトリの
  `Bash` 実行自体には既存の `git-guard.mjs` PreToolUse hook がそのまま機能する。追加の
  hook 投入は不要。
- `check-agent-environment.ps1` line 40-67 の `$requiredFiles` allowlist は presence
  チェックのみなので、新規 skill 追加で reject されることはない。

## 再評価 (Re-evaluation) triggers

- upstream `description:` frontmatter が non-backward-compatible に変化し、pointer 同期
  だけでは trigger が壊れる場合
- 採用 4 repo のいずれかが `references/` を超える `scripts/` 等を持ち、pointer からの
  参照経路を再設計する必要が出た場合
- `git subtree pull` が conflict を生むケース (運用違反が疑われる)
- `scripts/check-agent-environment.ps1` の `$requiredFiles` が allowlist から denylist
  へ意味を変え、追加 skill を reject し始めた場合
- 採用 15 skill のうち、利用実績がゼロで保守負担が上回るものが出てきた場合
