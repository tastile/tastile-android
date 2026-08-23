# ADR 0001 — Kotlin LSP を project-local に導入する

- 調査日: 2026-08-23
- Status: Accepted
- Supersedes: なし
- Superseded by: なし

## Context / 解決したい capability

Android Compose プロジェクトで、Android Studio 以外の editor
(Visual Studio Code / Cursor / Neovim / Zed / Sublime) を使う contributor でも
Kotlin 言語サービスを享受できるようにしたい。JetBrains の `kotlin-lsp`
(Kotlin/kotlin-lsp) は公式 LSP 実装だが、alpha status + experimental AGP
support + part of codebase closed-source という制約がある。

## Decision

以下の最小セットを project-local で導入する:

- `scripts/kotlin-lsp-release.json` — version / URL / SHA-256 pin manifest
- `scripts/install-kotlin-lsp.{ps1,sh}` — 公式 archive を `.tools/kotlin-lsp/<version>/`
  へ download / verify / extract する bootstrap
- `scripts/kotlin-lsp-launcher.{ps1,sh}` — JDK 25 を解決して server を起動する
  static launcher (manifest を読んで version 解決)
- AGENTS.md に LSP setup / 再評価条件セクション
- `.tools/` は既存どおり gitignored (vendor は tracking しない)

Editor pointer (例: VSCode の `.vscode/settings.json` の `kotlin.lsp.executable`)
は **commmit しない**。developer 各自が editor を vendor-neutral に保つため。
Bootstrap script が launcher の path を print するので、editor 設定は各自で行う。

## Status に関する注記

v262.4739.0 以降の `kotlin-lsp` は README に "currently in the Alpha state"
と明記されている。AGP の扱いは "experimental Android Gradle Plugin support"
と changelog に書いてある (v262.4739.0 LSP-842 で初導入、本プロジェクトで
採用する v262.9593.0 でも experimental のまま)。Kotlin 2.2.10 を含む最新
KGP と組み合わせた安定性は JetBrains が nightly を追跡しているのみで、
本プロジェクト側での検証は限定的。

加えて v262.4739.0 以降、JDK 25 が必須 (IJPL-221307)。本プロジェクトの build
JDK は 17 / 21 (toolchain auto-resolution) なので、lsp 専用に JDK 25 を別途
用意する必要がある。launcher は次の優先順で解決する:

1. `$KOTLIN_LSP_JAVA_HOME` または `$JAVA25_HOME`
2. `.tools/jdk-25/` (vendored、bootstrap で download)
3. 標準 install 配下 (Adoptium / Homebrew / apt temurin-25)

## Selection reason

- **Necessity**: editor-agnostic な Kotlin 言語サービスを IDE 非依存 contributor
  に提供する。Android Studio 開発者には既存どおり影響なし。
- **Reproducibility**: version + URL + SHA-256 を manifest に pin することで、
  fresh clone から `./scripts/install-kotlin-lsp.{ps1,sh}` 一発で再構築できる。
  meta-prompt §3, §7 適合。
- **Maintainability**: source = JetBrains 公式リポジトリ、stable channel
  (alpha 表記) のみ参照。community wrapper は介在しない。
- **Determinism**: SHA-256 検証付き download + extract。SHA 不一致で即 fail。
- **Security / supply chain**: 公式 GitHub release + JetBrains CDN。
  third-party redirect なし。公式 `.sha256` ファイルで cross-check。
- **License**: standalone archive = Apache 2.0 (kotlin/kotlin-lsp LICENSE.txt)。
  VSCode Marketplace 版 "Kotlin by JetBrains" は JetBrains Free Plugin
  License だが、本 ADR では standalone archive のみを採用するため無関係。
- **Cross-platform**: Windows x64 / arm64, Linux x64 / arm64, macOS x64 /
  arm64 を manifest の 6 platform 分用意。`*.sit` (macOS) は StuffIt 必須の
  ため bootstrap は manual extraction を案内 (要手作業)。
- **Context cost**: 起動は JVM 1 process。`kotlin.lsp.*` が agent context に
  入る経路は opencode の IDE 統合次第のため、本プロジェクト config 側で固定
  する schema は無し。
- **Version**: latest stable (alpha channel だが stable 表記の release
  branch) `v262.9593.0` を pin。再評価条件は本 ADR 末尾。

## Alternatives considered

- **Detekt / ktlint 静的解析追加**: 既存の `:lint-rules` カスタム Android
  Lint 規則 + `kotlin.code.style=official` で同等目的の重複になるため見送り
  (meta-prompt §6 重複実装禁則)。
- **Serena / ast-grep**: symbol-level navigation / structural refactor 専用。
  repo size が中規模で text search (`rg`) と KGP の symbol 解析で現状充分。
  必要になったら再評価。
- **claude-mem / persistent memory MCP**: meta-prompt §8 で default 禁止。
  この Android project の source of truth は `docs/architecture.md` / design
  docs / workspace `docs/HARNESS.md` で implicit memory 不要。
- **Context7 (external doc retrieval MCP)**: 同上。外部 content は
  non-authoritative という meta-prompt §8 の制約があり、AGP / Compose /
  KGP / Hilt の official docs は contributor 側で固定 URL 参照可能。
- **Gradle MCP (`./gradlew` 代行)**: 既に deterministic CLI で `./gradlew`
  直叩きできる。context overhead > 効果。
- **project-local Gradle plugin として導入**: 配布モデルに存在しない
  (kotlin-lsp は GitHub Releases ZIP か VSIX)。Gradle plugin 化するなら
  2nd-party fork が必要で、maintainability / security の要件 (§6) に反する。

## Re-evaluation condition (再評価 trigger)

次のいずれかが発生したら再評価し、必要なら ADR を update / supersede:

1. JetBrains が `kotlin-lsp` の README / release note で stable 宣言
   (現在の "currently in the Alpha state" 表記が外れた時)。
2. AGP support が "experimental" 表記から昇格した時 (本プロジェクトの
   AGP 9.3.1 + Kotlin 2.2.10 との整合がとれる独立 evidence が出る時)。
3. Android Studio 以外の editor がチームの primary IDE になった時
   (現状 Android Studio が primary、VSCode は少数派と推定)。
4. 公式 source archive の URL 構造が変わった時 (release naming convention
   の変更)。その場合 `scripts/install-kotlin-lsp.{ps1,sh}` と manifest を
   修正。
5. build 側 Kotlin version を 2.3.0+ へ bump する時 (kotlin-lsp changelog
   は KGP version と密接に link)。backend core との v1 API contract 維持
   と合わせて計画的に bump する。
6. macOS .sit archive を自動抽出する必要が出た時 (StuffIt 以外の選択肢、
   例: `unar` への依存採用)。現状は macOS 利用者が居ないため manual step。

## Unaccepted items (記録のみ)

この ADR では導入を見送った (上に列挙したもの以外):

- 既存 `docs/superpowers/m5/wcag-audit-script.py` は no-new-Python 規則制定
  前の WCAG AA contrast audit 用 read-only script として残置。
- 既存 Android Lint `disable +=` 禁止方針は維持 (AGENTS.md の Build-Time
  Hard Requirements に記載)。
- `.tools/migrate_designsystem.py` は既に gitignored なので commit は
  されていない。project-local な toolchain auxiliary script として残置。

## Cross-references

- meta-prompt §6 plugin / tool 選定基準
- meta-prompt §7 ADR 必須化
- meta-prompt §11 internal docs は日本語
- AGENTS.md "Build and Verify" / "Toolchain" / "Working Rules"
- `docs/architecture.md` の Compose UI → … → Core の層構造
- `.wslc/` dev container (本 ADR では container 側に kotlin-lsp を入れない;
  開発者 host editor のみ対象)
