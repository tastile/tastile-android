# Material 3 Expressive 移行設計

- Status: Draft → Review
- Date: 2026-09-02
- Owner: tastile-android maintainers
- Scope: `app/` 配下全体、`designsystem/` 配下を中心、`ui/{dashboard,mobile,account}/` は token / wrapper 経由のみ

## 1. Background

`androidx.compose.material3:material3` を `1.5.0-alpha24` で固定したまま、designsystem 配下に 23 コンポーネント・14 トークン群を抱える。直近のコミット群（`25c8c60 refactor(designsystem): migrate TastileStatusCircle to shape tokens` 等）で token 集約は進んだが、M3 Expressive（2025年5月公開）の新コンポーネントは未着手。

- 既存: `Material 3 Expressive を使うようにしてください` というユーザ指示
- 既存: `verifyDesignSystemImports` で `ui/{dashboard,mobile,account}/` 直下の `androidx.compose.material3.*` 直接 import を禁止。許可には `// m2-allow:` marker が必要
- 既存: 直書き `RoundedCornerShape(<n>.dp)` も designsystem 外で禁止

M3 Expressive は新 API（`LoadingIndicator` / FAB Menu / ButtonGroup / `MotionScheme` など）と新 shape token（`large-increased` / `extra-large-increased` / `extra-extra-large`）を導入する。既存 token 体系と衝突しないよう、designsystem ラッパー経由で公開する。

## 2. Goals & Non-Goals

### Goals

1. LoadingWheel を M3 Expressive `LoadingIndicator` に置換
2. `MotionScheme` を designsystem に導入し、spring physics を FAB / Sheet / Indicator に適用
3. FAB Menu（`TastileFabMenu`）を designsystem に追加し、QuickCreate の入口を再設計
4. ButtonGroup（`TastileButtonGroup`）を designsystem に追加し、XS / S / M / L / XL サイズをサポート
5. `material3` を最新 α へ bump（破壊的変更への追従）
6. `// m3e-allow:` marker を guard に追加し、M3 Expressive API の境界を可視化

### Non-Goals

- Web 側 / Flutter 側 / Desktop 側の同期（この repo は Android のみ）
- Theme のカラースキーム刷新（colorScheme 自体は不変）
- Visual regression test（Paparazzi/Roborazzi）の導入
- 既存 ViewToggle / SegmentedButton の全面置換（当面は残置。新規画面は TastileButtonGroup を使う）
- Dark mode 専用調整（自動追従で十分）

## 3. Architecture

### 3.1 ファイル配置

```
app/src/main/java/app/tastile/android/core/designsystem/
├── theme/
│   ├── TastileMotionTokens.kt        [NEW] MotionScheme + spring physics tokens
│   ├── TastileShapes.kt              [MOD] large-increased / extra-large-increased / extra-extra-large tokens 追加
│   ├── TastileSpacingTokens.kt       [MOD] 必要箇所のみ 8dp grid を反映
│   └── Theme.kt                      [MOD] LocalTastileMotionScheme の CompositionLocalProvider 注入
├── component/
│   ├── LoadingWheel.kt               [REWRITE] LoadingIndicator (M3 Expressive) へ置換
│   ├── TastileFabMenu.kt             [NEW] FAB Menu wrapper
│   ├── TastileButtonGroup.kt         [NEW] ButtonGroup wrapper + XS-XL サイズ対応
│   └── ... 既存ファイルは触らない

app/src/main/java/app/tastile/android/ui/dashboard/
└── QuickCreateSheet.kt               [MOD] FAB Menu エントリポイント経由の起動に置換

app/src/test/.../buildlogic/
└── VerifyDesignSystemImportsGuardTest.kt   [MOD] `// m3e-allow:` marker 検証を追加

app/build.gradle.kts                  [MOD] material3 を最新 α へ bump、`// m3e-allow:` guard 追加
```

### 3.2 Phase / Task グラフ

```
Phase 0 ─ material3 α bump + guard marker 拡張（単一 agent）
   │
   ▼
Phase 1 ─ MotionScheme + Shape token 拡張 + Theme 注入（単一 agent）
   │
   ├──────────────────┬──────────────────┐
   ▼                  ▼                  ▼
Phase 2a           Phase 2b           Phase 2c
LoadingWheel      TastileFabMenu     TastileButtonGroup
                  + QuickCreate       + ViewToggle XS-XL
   │                  │                  │
   └──────────────────┴──────────────────┘
                       ▼
                   Phase 3 ─ 統合検証（verify + smoke）
```

各 Phase の Task は **単一責務・単一 PR 候補** に分割し、独立ファイル集合についてはサブエージェント並列実行する。

## 4. Components

### 4.1 LoadingWheel

`androidx.compose.material3.LoadingIndicator` へ全面置換。

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package app.tastile.android.core.designsystem.component

@Composable
fun LoadingWheel(modifier: Modifier = Modifier, ...) {
    LoadingIndicator(
        modifier = modifier.size(LocalTastileShapeTokens.current.loadingWheelSize),
        color = LocalTastileStatusTokens.current.loadingColor,
    )
}
```

- ファイル先頭 `@file:OptIn` で集中宣言
- サイズ・色は LocalToken 経由（既存パターン踏襲）

### 4.2 TastileFabMenu

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package app.tastile.android.core.designsystem.component

@Composable
fun TastileFabMenu(
    mainIcon: ImageVector,
    mainLabel: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<FabMenuItem>,
    modifier: Modifier = Modifier,
)
```

- 展開 / collapse は `LocalTastileMotionScheme.current.defaultSpatialSpec` で spring 物理アニメーション
- `BackHandler(enabled = expanded) { onExpandedChange(false) }` を内部に組み込み
- `FabMenuItem` は sealed class（icon + label + onClick）で公開

呼び出し側（`QuickCreateSheet.kt`）は FAB 起動パスのみ TastileFabMenu 経由に切替。既存ロジック（Sheet 内容、validation、submit）は維持。

### 4.3 TastileButtonGroup

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TastileButtonGroup(
    items: List<ButtonGroupItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    size: ButtonGroupSize = ButtonGroupSize.Medium, // XS / S / M / L / XL
    modifier: Modifier = Modifier,
)
```

- 既存の `ViewToggle` / `SegmentedButton` とは独立した新コンポーネント
- 既存画面では当面 ViewToggle 残置。新規画面は TastileButtonGroup を直接利用
- 5 サイズそれぞれに `LocalTastileShapeTokens.current.buttonGroupSizeXs` … `Xl` を割り当て

### 4.4 TastileMotionTokens

```kotlin
@Immutable
data class TastileMotionScheme(
    val defaultSpatialSpec: Spring<Float>,
    val defaultEffectsSpec: Spring<Float>,
    val slowSpatialSpec: Spring<Float>,
    val fastSpatialSpec: Spring<Float>,
)

val LocalTastileMotionScheme = staticCompositionLocalOf { TastileMotionDefaults.scheme() }
```

`MaterialTheme` の拡張プロパティではなく、独立した `LocalTastileMotionScheme` として公開（既存の `LocalTastileCardRoleTokens` 等のパターンに揃える）。

### 4.5 Shape token 拡張

`TastileShapes.kt` に以下を追加：

```kotlin
val ExtraLargeIncreased: CornerBasedShape = RoundedCornerShape(32.dp)
val ExtraExtraLarge: CornerBasedShape = RoundedCornerShape(48.dp)
```

`designsystem/` 配下なので `RoundedCornerShape(<n>.dp)` guard の対象外。違反しない。

## 5. Data Flow / Token 伝播経路

```
TastileMotionTokens.kt      → staticCompositionLocalOf<TastileMotionScheme>
TastileShapeTokens.kt       → staticCompositionLocalOf<TastileShapeTokens>（既存）
TastileCardRoleTokens.kt    → staticCompositionLocalOf<...>（既存）
TastileStatusTokens.kt      → staticCompositionLocalOf<...>（既存）
        │
        ▼
TastileTheme.kt
    CompositionLocalProvider(
        LocalTastileMotionScheme provides TastileMotionDefaults.scheme(),
        LocalTastileShapeTokens provides TastileShapeTokens(),
        ...
    ) {
        MaterialTheme(
            colorScheme = ...,
            shapes = Shapes(...),  // 新 large-increased / extra-large-increased 追加
            ...
        )
    }
        │
        ▼
designsystem/component/*.kt
    各 composable が LocalTastileMotionScheme.current で spring spec を取得
    shapes は MaterialTheme.shapes から取得（既存パターン）
        │
        ▼
ui/{dashboard,mobile,account}/   ← Local* 経由でのみアクセス（既存 guard 維持）
```

### 5.1 Marker 体系

| marker | 用途 | 例 |
|---|---|---|
| `// m2-allow:` | 既存の `androidx.compose.material3.*` 直接 import | `import androidx.compose.material3.Button` |
| `// m3e-allow:` | 新規。M3 Expressive パッケージ / `ExperimentalMaterial3ExpressiveApi` 使用箇所 | `import androidx.compose.material3.LoadingIndicator`（experimental package）|

`app/build.gradle.kts` の `collectDesignSystemViolations` を拡張し、`ui/{dashboard,mobile,account}/` 配下で以下を検出した場合に m3e-allow marker を要求：

- `import androidx.compose.material3.*experimental*.*`
- `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`

`designsystem/` 配下は引き続き guard 対象外。

### 5.2 State holder 影響

- `DashboardViewModel` / `AccountViewModel` / `OverlayViewModel` には **触らない**
- `QuickCreateSheet.kt` の表示トリガーのローカル State のみ `TastileFabMenu` の `expanded` 制御へ切替

## 6. Error Handling

### 6.1 α dependency breakage

- `verify` 失敗時は alpha bump PR の責務として修正
- 起動時 `ClassNotFoundException` は既存 `CrashReportRepository` 経路で観測（**新規追加なし**）
- Phase 3 で `connectedDebugAndroidTest` の `@Smoke` タグが付いた Dashboard / QuickCreate 起動テストを追加し、Expressive コンポーネントの inflate 失敗を検出

### 6.2 guard violation

- 既存 `verifyDesignSystemImports` の fail-fast 経路に乗る
- 違反時のエラー出力に「`// m3e-allow:` を前行に付与してください」を追記（`formatDesignSystemViolations` 拡張）

### 6.3 Motion physics のフレーム落ち

- spring physics はレイアウト中 continuous に走査 → 低スペック端末で fps 落ちの可能性
- Phase 3 で `gfxinfo` 取得 → 60fps 維持できない端末があれば MotionScheme の stiffness を緩める escape を運用で用意
- spec 段階ではパラメータ凍結しない。実装フェーズで実機測定してから `TastileMotionDefaults` の定数として固定

### 6.4 FAB Menu collapse 中のジェスチャー競合

- 実装時に `BackHandler(enabled = expanded) { onExpandedChange(false) }` を組み込み（spec は責務のみ明示）
- テストは Robolectric で `BackHandler` 経由の dismiss を検証

### 6.5 Loading Indicator の infinite 表示

- 10 秒タイムアウトで error 状態遷移 → 既存 LoadingWheel の挙動を踏襲（**新規追加なし**）

## 7. Testing

### 7.1 既存テストの維持

- `:app:testDebugUnitTest` 全件 PASS 維持
- `VerifyDesignSystemImportsGuardTest.kt` の synthetic tmp dir 再帰ヘルパーを `m3e-allow` 用ケースに拡張

### 7.2 新規テスト

| テスト対象 | 種別 | 配置 | 検証内容 |
|---|---|---|---|
| `TastileMotionTokens` | JVM unit | `app/src/test/.../designsystem/theme/` | defaults が空でないこと、Spring.spec パラメータが妥当範囲 |
| `LoadingWheel` | Compose UI test | `app/src/test/.../designsystem/component/` | `LoadingIndicator` が描画されること、`LocalTastileStatusTokens` を反映すること |
| `TastileFabMenu` | Compose UI test | 同上 | 展開 / collapse、`BackHandler`、`onItemSelected` 発火、a11y semantics |
| `TastileButtonGroup` | Compose UI test | 同上 | 選択状態遷移、5 サイズ描画、`onItemSelected` 発火 |
| `verifyDesignSystemImports` guard | JVM unit | `app/src/test/.../buildlogic/` | 新 `// m3e-allow:` ロジックの陽性 / 陰性ケース |
| QuickCreate FAB Menu 統合 | Robolectric | `app/src/test/.../ui/dashboard/` | FAB Menu 起動 → シート表示 → dismiss |
| Dashboard smoke | instrumented | `app/src/androidTest/.../ui/dashboard/` | `@Tag("smoke")` 既存タグに追加、Phase 3 で投入 |

### 7.3 Visual regression

- Phase 3 で Paparazzi / Roborazzi ベース追加は **scope 外**
- ユーザー要望があれば別タスクとして Phase 3 内で再提案

### 7.4 Coverage threshold

- 既存の 80% lines / branches / methods / instructions 維持
- 割る場合は `BLOCKED` rationale を別 tracking doc に書く（`app/build.gradle.kts` のテストセクションには記載しない）

## 8. Migration Order & Parallel Execution

### Phase 0: dependency bump + guard（単一 agent、順序保証）

- Task 0.1: `app/build.gradle.kts` の material3 を最新 α へ bump
- Task 0.2: `collectDesignSystemViolations` を拡張（`// m3e-allow:` 検出）
- Task 0.3: `formatDesignSystemViolations` のメッセージに m3e-allow 案内を追加
- Task 0.4: `VerifyDesignSystemImportsGuardTest.kt` のテストケース追加

### Phase 1: theme & token 拡張（単一 agent、順序保証）

- Task 1.1: `TastileMotionTokens.kt` 新規作成
- Task 1.2: `TastileShapes.kt` に ExtraLargeIncreased / ExtraExtraLarge 追加
- Task 1.3: `Theme.kt` の `CompositionLocalProvider` へ `LocalTastileMotionScheme` 注入
- Task 1.4: `TastileMotionTokens` の JVM unit test

### Phase 2: 並列実行（最大 3 sub-agents、ファイル集合が disjoint）

- **Phase 2a** (sub-agent A):
  - Task 2a.1: `LoadingWheel.kt` 書き換え
  - Task 2a.2: `LoadingWheel` の Compose UI test

- **Phase 2b** (sub-agent B):
  - Task 2b.1: `TastileFabMenu.kt` 新規作成
  - Task 2b.2: `TastileFabMenu` の Compose UI test
  - Task 2b.3: `QuickCreateSheet.kt` の FAB Menu 起動経路切替
  - Task 2b.4: `QuickCreate` の Robolectric 統合 test

- **Phase 2c** (sub-agent C):
  - Task 2c.1: `TastileButtonGroup.kt` 新規作成
  - Task 2c.2: `TastileButtonGroup` の Compose UI test

Phase 2 の各 sub-agent は **同じファイル集合を触らない** ことを前提に並列実行。

### Phase 3: 統合検証（単一 agent）

- Task 3.1: `./gradlew verify` 緑化
- Task 3.2: `gfxinfo` で motion physics の fps 計測
- Task 3.3: instrumented `@Tag("smoke")` Dashboard / QuickCreate 起動テスト追加
- Task 3.4: ドキュメント更新（`docs/superpowers/m3/` 配下、`README.md` の Material 3 セクション）

## 9. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| material3 α の破壊的変更 | CI fail | Phase 0 で bump のみ先行。`verify` が落ちたら即修正可能な粒度で PR 化 |
| `ExperimentalMaterial3ExpressiveApi` の API 改名 | ビルド失敗 | `@file:OptIn` を集中宣言し、変更時に修正箇所を限定 |
| Spring physics の端末依存 fps 落ち | UX 低下 | Phase 3 で gfxinfo 計測 → パラメータ escape 用意 |
| `// m3e-allow:` marker の運用浸透不足 | guard violation | pre-commit hook / CI fail-fast で検出。新規画面追加時のレビュー項目に「m3e-allow の妥当性」を追加 |
| FAB Menu と既存 FAB の二重定義 | UI 衝突 | QuickCreate 以外の既存 FAB 利用箇所は本 scope では触らない。Phase 2b 完了後に利用箇所棚卸し |

## 10. References

- `app/build.gradle.kts` — guard 実装箇所、`material3` dependency 定義
- `app/src/main/java/app/tastile/android/core/designsystem/theme/ThemeTokenLocals.kt` — 既存 Local* パターン
- `app/src/main/java/app/tastile/android/core/designsystem/component/LoadingWheel.kt` — 既存実装（書き換え対象）
- `app/src/main/java/app/tastile/android/ui/dashboard/QuickCreateSheet.kt` — FAB Menu 統合対象
- `app/src/test/.../buildlogic/VerifyDesignSystemImportsGuardTest.kt` — guard test
- `docs/superpowers/m3/before-reports/` — Compose Compiler Reports baseline
- `.agents/upstream-skills/material-3-skill/skills/material-3/references/component-catalog.md` — M3 Expressive component 仕様
- `CLAUDE.md` — Build and Verify / Build-Time Hard Requirements / Working Rules
