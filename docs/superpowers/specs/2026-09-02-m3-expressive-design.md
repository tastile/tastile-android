# Material 3 Expressive 移行設計

- Status: Review (rev 2 — 6 review findings addressed)
- Date: 2026-09-02
- Owner: tastile-android maintainers
- Scope: `app/` 配下全体、`designsystem/` 配下を中心、`ui/{dashboard,mobile,account}/` は token / wrapper 経由のみ

## 1. Background

`androidx.compose.material3:material3` を `1.5.0-alpha24` で固定したまま、designsystem 配下に 23 コンポーネント・14 トークン群を抱える。M3 Expressive（2025年5月公開）の新 API（`LoadingIndicator` / `MotionScheme` / FAB Menu / ButtonGroup）は未着手。

- 既存導線: QuickCreate は `ui/mobile/tabs/TimelineScreen.kt:205` および `ui/mobile/tabs/TilesScreen.kt:124` の FAB（`NiaFloatingActionButton` / `NiaExtendedFloatingActionButton`）→ `Overlay.QuickCreate` → `ui/mobile/sheets/QuickCreateSheetMobile.kt:78`
- 既存: `verifyDesignSystemImports` で `ui/{dashboard,mobile,account}/` 直下の `androidx.compose.material3.*` 直接 import を禁止。許可には `// m2-allow:` marker が必要
- 既存: 直書き `RoundedCornerShape(<n>.dp)` も designsystem 外で禁止
- 既存 `NiaLoadingWheel` 公開シグネチャ: `NiaLoadingWheel(contentDesc: String, modifier: Modifier = Modifier, wheelSize: Dp = 48.dp)` および `NiaOverlayLoadingWheel(contentDesc: String, modifier: Modifier = Modifier)`（合計 8 call sites）

M3 Expressive は新 API と新 shape token（`large-increased` / `extra-large-increased` / `extra-extra-large`）を導入する。既存 token 体系と衝突しないよう、designsystem ラッパー経由で公開する。

## 2. Goals & Non-Goals

### Goals

1. `NiaLoadingWheel` の内部実装を M3 Expressive `LoadingIndicator` に置換（公開シグネチャ維持・全 8 call sites 不変）
2. `MaterialTheme(motionScheme = MotionScheme.expressive())` を designsystem の `Theme.kt` に導入し、Material3 コンポーネントへ spring physics を伝播
3. FAB Menu（`TastileFabMenu`）を designsystem に追加し、QuickCreate 入口（`TimelineScreen.kt` / `TilesScreen.kt` の FAB）を再設計
4. ButtonGroup（`TastileButtonGroup`）を designsystem に追加し、XS / S / M / L / XL サイズ + 選択 semantics を契約化
5. `material3` を `1.5.0-alpha27`（2026-08-26 公開、最新 α）へ pin
6. designsystem に対する shape token 追加（`LargeIncreased` / `ExtraLargeIncreased` / `ExtraExtraLarge`）

### Non-Goals

- Web 側 / Flutter 側 / Desktop 側の同期
- Theme のカラースキーム刷新（`colorScheme` は不変）
- Visual regression test（Paparazzi/Roborazzi）の導入
- 既存 `ViewToggle` / `SegmentedButton` の全面置換（当面は残置。新規画面は TastileButtonGroup を使う）
- `NiaLoadingWheel` の timeout / error 遷移追加（既存は表示専用、component 単体では error state を決定しない）
- `// m3e-allow:` marker の追加（後述 §5.1 で削除理由明記）

## 3. Architecture

### 3.1 ファイル配置

```
app/src/main/java/app/tastile/android/core/designsystem/
├── theme/
│   ├── TastileShapes.kt              [MOD] LargeIncreased / ExtraLargeIncreased / ExtraExtraLarge 追加
│   └── Theme.kt                      [MOD] MaterialTheme(motionScheme = MotionScheme.expressive()) を追加
├── component/
│   ├── LoadingWheel.kt               [MOD] 内部実装を LoadingIndicator (experimental) に置換。公開シグネチャ維持
│   ├── TastileFabMenu.kt             [NEW] FAB Menu wrapper
│   ├── TastileButtonGroup.kt         [NEW] ButtonGroup wrapper + XS-XL サイズ + 選択 semantics
│   └── ... 既存ファイルは触らない

app/src/main/java/app/tastile/android/ui/mobile/tabs/
├── TimelineScreen.kt                 [MOD] NiaFloatingActionButton → TastileFabMenu への置換（Line 205 周辺）
└── TilesScreen.kt                    [MOD] NiaExtendedFloatingActionButton → TastileFabMenu への置換（Line 124 周辺）

app/src/main/java/app/tastile/android/ui/mobile/sheets/
└── QuickCreateSheetMobile.kt         [READ-ONLY] Overlay.QuickCreate のエントリは維持。TastileFabMenu 経由で起動される前提

app/build.gradle.kts                  [MOD] material3 を 1.5.0-alpha27 に pin
```

NOTE: `TastileSpacingTokens.kt` は本 scope では触らない。既存 8dp grid 体系は M3 Expressive と独立に維持。

### 3.2 Phase / Task グラフ

```
Phase 0 ─ material3 1.5.0-alpha27 pin + Theme.kt へ motionScheme 注入（単一 agent）
   │
   ├──────────────────┬──────────────────┐
   ▼                  ▼                  ▼
Phase 1a           Phase 1b           Phase 1c
LoadingWheel       TastileFabMenu     TastileButtonGroup
実装置換            新規作成           新規作成
   │                  │                  │
   │                  ▼                  │
   │             Phase 2b                │
   │             QuickCreate FAB         │
   │             経路置換                │
   ▼                  │                  │
Phase 2a              │                  │
8 call sites          │                  │
動作確認               │                  │
   └──────────────────┴──────────────────┘
                       ▼
                   Phase 3 ─ 統合検証（verify + smoke）
```

`Phase 1a / 1b / 1c` は独立ファイル集合なので並列実行可能。`Phase 2b` は `Phase 1b` の TastileFabMenu 公開 API に依存するため、Phase 1b 完了後。

## 4. Components

### 4.1 LoadingWheel（実装置換・シグネチャ維持）

`androidx.compose.material3.LoadingIndicator` へ内部実装を置換。**公開シグネチャ（`NiaLoadingWheel` / `NiaOverlayLoadingWheel`）は不変。8 call sites 全てを変更しない。**

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package app.tastile.android.core.designsystem.component

@Composable
fun NiaLoadingWheel(
    contentDesc: String,
    modifier: Modifier = Modifier,
    wheelSize: Dp = 48.dp,  // 既存既定値を維持
) {
    LoadingIndicator(
        modifier = modifier
            .size(wheelSize)
            .semantics(mergeDescendants = true) {
                contentDescription = contentDesc
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            .testTag("loadingWheel"),
        color = MaterialTheme.colorScheme.onBackground,  // 既存 colorScheme 利用、token 追加なし
    )
}
```

**受入条件:**
- `contentDescription` 維持
- `progressBarRangeInfo = Indeterminate` 維持
- `testTag("loadingWheel")` 維持
- 8 call sites（`DashboardScreens.kt:48, 75` / `ExecuteScreen.kt:235` / `TokensSheet.kt:154` / `AccountSheet.kt:172` / `ProjectsSectionContent.kt:121` / `AppComponents.kt:333, 356`）のコンパイル・動作不変
- 新規 token（`loadingColor` / `wheelSize`）は **追加しない**。色は `MaterialTheme.colorScheme.onBackground`、サイズは引数既定値 `48.dp` で既存挙動を再現
- `NiaOverlayLoadingWheel` は `Surface` ラッパーとして残置

### 4.2 TastileFabMenu（新規）

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package app.tastile.android.core.designsystem.component

sealed class FabMenuItem {
    abstract val icon: ImageVector
    abstract val label: String
    data class Action(
        override val icon: ImageVector,
        override val label: String,
        val onClick: () -> Unit,
    ) : FabMenuItem()
}

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

- 展開 / collapse は `MaterialTheme.motionScheme.defaultSpatialSpec`（Expressive）で spring 物理アニメーション
- `BackHandler(enabled = expanded) { onExpandedChange(false) }` を内部に組み込み
- a11y semantics: 展開状態は `Role.Button` + `stateDescription = expanded/collapsed`
- `items` が空の場合: `IllegalArgumentException`（fail-fast）
- `items.size > MAX_MENU_ITEMS` の場合: 上限値（例: 6）まで切り詰め + `LazyList` スクロール

**呼び出し側の統合**（Phase 2b）:
- `TimelineScreen.kt:205` の `NiaFloatingActionButton` を `TastileFabMenu` に置換
- `TilesScreen.kt:124` の `NiaExtendedFloatingActionButton` を `TastileFabMenu` に置換
- 各 menu item の `onClick` で `overlay.show(Overlay.QuickCreate)`（既存）と将来的な追加カテゴリ（event / task / note 等）
- `QuickCreateSheetMobile.kt` は **無変更**（`Overlay.QuickCreate` 受信ロジックが既存）

### 4.3 TastileButtonGroup（新規 + 契約）

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package app.tastile.android.core.designsystem.component

enum class ButtonGroupSize { Xs, S, M, L, Xl }

data class ButtonGroupItem(
    val icon: ImageVector? = null,
    val label: String,
    val enabled: Boolean = true,
)

@Composable
fun TastileButtonGroup(
    items: List<ButtonGroupItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    size: ButtonGroupSize = ButtonGroupSize.M,
    modifier: Modifier = Modifier,
)
```

**契約:**

| 項目 | 仕様 |
|---|---|
| 高さ | 各サイズで `dp` 固定値: Xs=32, S=40, M=48, L=56, Xl=64（M3 標準サイズに整合） |
| Padding | horizontal: Xs=8, S=12, M=16, L=20, Xl=24 |
| Icon サイズ | Xs=16, S=18, M=20, L=24, Xl=28（label 文字高の ~70%） |
| Text サイズ | Xs=LabelSmall, S=LabelSmall, M=LabelMedium, L=LabelLarge, Xl=LabelLarge |
| Touch target | 最低 48dp（仕様上の高さ未満のサイズ Xs=32 は `minimumInteractiveComponentSize` 適用で外側 48dp 確保） |
| 選択 semantics | `Role.Tab` + `selected = (index == selectedIndex)`。TalkBack で「selected, 3 of 5」と読み上げ |
| `selectedIndex` 範囲外 | `IllegalArgumentException`（fail-fast）。`items.indices` の範囲外は呼び出し側バグ |
| `items` 空 | `IllegalArgumentException`（fail-fast）。最低 1 項目必須 |
| `enabled = false` の item | グレーアウト、`onItemSelected` 発火しない、ripple 無効 |
| 高さ / padding / icon-size / text-style | `TastileShapeTokens` ではなく `TastileButtonGroupTokens`（新規）に集約。Shape token は形状のみで高さ・padding は責務外 |

### 4.4 MaterialTheme motionScheme 統合（designsystem/Theme.kt）

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TastileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkColorScheme(...) else lightColorScheme(...)
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = TastileShapes,
        typography = TastileTypography,
        motionScheme = MotionScheme.expressive(),  // NEW
        content = content,
    )
}
```

- `LocalTastileMotionScheme` などの独自 CompositionLocal は **作らない**（公式 `MaterialTheme.motionScheme` 経由でアクセス）
- M3 コンポーネントが自動的に expressive motion を採用
- designsystem 独自 component は `MaterialTheme.motionScheme.defaultSpatialSpec` 等を直接参照

### 4.5 Shape token 拡張（TastileShapes.kt 追記）

```kotlin
val LargeIncreased: CornerBasedShape = RoundedCornerShape(20.dp)
val ExtraLargeIncreased: CornerBasedShape = RoundedCornerShape(32.dp)
val ExtraExtraLarge: CornerBasedShape = RoundedCornerShape(48.dp)
```

`designsystem/` 配下なので `RoundedCornerShape(<n>.dp)` guard の対象外。

## 5. Data Flow / Token 伝播経路

```
TastileShapes.kt                  → 既存 + LargeIncreased / ExtraLargeIncreased / ExtraExtraLarge
Theme.kt                          → MaterialTheme(motionScheme = MotionScheme.expressive())
        │
        ▼
MaterialTheme (公式 CompositionLocal)
        │
        ├─ colorScheme (既存)
        ├─ shapes (MaterialTheme.shapes でアクセス)
        ├─ typography (既存)
        └─ motionScheme (NEW: MaterialTheme.motionScheme でアクセス)
        │
        ▼
designsystem/component/*.kt
    - M3 コンポーネントは motionScheme を自動採用
    - 独自 component は MaterialTheme.motionScheme.defaultSpatialSpec を利用
        │
        ▼
ui/{dashboard,mobile,account}/   ← designsystem 経由でのみアクセス
```

### 5.1 Marker 体系（簡素化）

| marker | 用途 |
|---|---|
| `// m2-allow:` | 既存の `androidx.compose.material3.*` 直接 import（変更なし）|
| ~~`// m3e-allow:`~~ | **削除** |

**`// m3e-allow:` を追加しない理由:**
1. M3 Expressive API は通常の `androidx.compose.material3` パッケージ内にあり、パッケージ名による検出は不可能
2. 安定 API になった FAB Menu / ButtonGroup は `@ExperimentalMaterial3ExpressiveApi` 注釈も付かず、annotation 検出も不可能
3. designsystem が guard 対象外なので、designsystem 内の component が Expressive API を吸収し、UI 層は wrapper のみ参照 → UI 層で marker を必要とする状況が発生しない
4. 仮に将来 UI 層で直接 Expressive API を使う要件が出たら、**symbol allowlist**（`LoadingIndicator` / `MotionScheme` 等の完全修飾名）で検出する別 guard を後付けする

`app/build.gradle.kts` の `collectDesignSystemViolations` には **手を加えない**。

### 5.2 State holder 影響

- `DashboardViewModel` / `AccountViewModel` / `OverlayViewModel` には **触らない**
- `Overlay.QuickCreate` の表示契機（TimelineScreen / TilesScreen の FAB クリック）のみが TastileFabMenu に置換される
- `QuickCreateSheetMobile.kt` 自体は無変更

## 6. Error Handling

### 6.1 α dependency breakage

- `verify` 失敗時は dependency bump PR の責務として修正
- 起動時 `ClassNotFoundException` は既存 `CrashReportRepository` 経路で観測（**新規追加なし**）
- Phase 3 で `connectedDebugAndroidTest` の Dashboard / QuickCreate 起動テストを追加し、Expressive コンポーネントの inflate 失敗を検出（具体的な test class への参照は §7 参照）

### 6.2 guard violation

- 既存 `verifyDesignSystemImports` の fail-fast 経路に乗る（**変更なし**）
- §5.1 の通り marker 体系を拡張しないので `formatDesignSystemViolations` も変更なし

### 6.3 Motion physics のフレーム落ち

- spring physics はレイアウト中 continuous に走査 → 低スペック端末で fps 落ちの可能性
- Phase 3 で `gfxinfo` 取得 → 60fps 維持できない端末があれば `MotionScheme.expressive()` のデフォルト stiffness では無く、stiffness を緩めた独自 `MotionScheme` インスタンスへの escape を運用で用意
- spec 段階ではパラメータ凍結しない。実装フェーズで実機測定してから凍結

### 6.4 FAB Menu collapse 中のジェスチャー競合

- 実装時に `BackHandler(enabled = expanded) { onExpandedChange(false) }` を組み込み（spec は責務のみ明示）
- テストは Robolectric で `BackHandler` 経由の dismiss を検証

### 6.5 LoadingWheel の無限表示（旧 §6.5 削除）

- 旧 spec の「10 秒タイムアウトで error 遷移」は **虚偽** だったため削除
- 現行 `NiaLoadingWheel` は表示専用で timeout を持たず、複数の異なる非同期処理から共有される。component 自体が error state を決定する責務を持たない
- error state を必要とするなら、各 state holder / repository 側で timeout 実装する別要件。**本 scope 外**

## 7. Testing

### 7.1 既存テストの維持

- `:app:testDebugUnitTest` 全件 PASS 維持
- `VerifyDesignSystemImportsGuardTest.kt` は **無変更**（marker 拡張しないため）
- LoadingWheel の 8 call sites 全てに対応する既存 test（`TastileCardActionRowTest.kt` 等）は無変更

### 7.2 新規テスト

| テスト対象 | 種別 | 配置 | 検証内容 |
|---|---|---|---|
| `NiaLoadingWheel` 実装置換 | Compose UI test | `app/src/test/.../designsystem/component/LoadingWheelTest.kt`（新規） | `LoadingIndicator` 描画、`contentDescription`、`progressBarRangeInfo.Indeterminate`、`testTag("loadingWheel")`、8 call sites のスモーク（それぞれの呼び出し箇所で描画されること） |
| `TastileFabMenu` | Compose UI test | `app/src/test/.../designsystem/component/TastileFabMenuTest.kt`（新規） | 展開 / collapse、`BackHandler`、`onItemSelected` 発火、a11y semantics、空リスト時例外、TalkBack stateDescription |
| `TastileButtonGroup` | Compose UI test | `app/src/test/.../designsystem/component/TastileButtonGroupTest.kt`（新規） | 5 サイズ描画、選択状態遷移、`selectedIndex` 範囲外例外、空リスト例外、`enabled=false` の非発火、48dp touch target 検証 |
| QuickCreate FAB Menu 統合 | Robolectric | `app/src/test/.../ui/mobile/tabs/TimelineScreenTest.kt`（新規）または既存ファイル拡張 | FAB Menu 起動 → Overlay.QuickCreate 発火 → QuickCreateSheetMobile 表示 → dismiss |
| Timeline / Tiles FAB 切替 | Robolectric | `app/src/test/.../ui/mobile/tabs/` | `NiaFloatingActionButton` / `NiaExtendedFloatingActionButton` の呼び出しが `TastileFabMenu` 経由になっていること（移行確認） |
| Dashboard / QuickCreate smoke | instrumented | `app/src/androidTest/.../ui/navigation/`（既存 MainActivityAuthGateTest 等と並列） | Dashboard 起動 → QuickCreate FAB Menu → QuickCreate sheet 表示の一連フロー |

### 7.3 Smoke test 実行方法

- JUnit 4 採用（`junit:junit:4.13.2`）。`@Tag` / `@Category` の filtering 機構は未導入
- Phase 3 で smoke test を追加する際は **明示的な class 指定で実行**:
  ```
  ./gradlew :app:connectedDebugAndroidTest \
    --tests "app.tastile.android.ui.navigation.MainActivityAuthGateTest" \
    --tests "app.tastile.android.ui.navigation.QuickCreateSmokeTest" \
    --tests "app.tastile.android.ui.navigation.DashboardSmokeTest"
  ```
- `@Category(Smoke::class)` を後付けで導入する場合:
  - `androidx.test.filters.LargeTest` / `SmallTest` は androidx.test.runner に既存だが smoke 用カテゴリは無し
  - 新規 `SmokeCategory` interface を作り、`addCategory` runner 設定を `app/build.gradle.kts` の `androidTest` ブロックに追加（Phase 3 で必要性が確認された場合のみ。**必須ではない**）

### 7.4 Visual regression

- Phase 3 で Paparazzi / Roborazzi ベース追加は **scope 外**

### 7.5 Coverage threshold

- 既存の 80% lines / branches / methods / instructions 維持
- 割る場合は `BLOCKED` rationale を別 tracking doc に書く

## 8. Migration Order & Parallel Execution

### Phase 0: dependency pin + Theme 更新（単一 agent、順序保証）

- Task 0.1: `app/build.gradle.kts` の material3 を `1.5.0-alpha27` に pin
- Task 0.2: `Theme.kt` の `MaterialTheme(...)` 呼び出しに `motionScheme = MotionScheme.expressive()` 追加
- Task 0.3: `TastileShapes.kt` に LargeIncreased / ExtraLargeIncreased / ExtraExtraLarge 追加
- Task 0.4: `./gradlew verify` 緑化確認（既存 test の非破壊確認）

### Phase 1: コンポーネント単位（並列実行可能、独立ファイル集合）

- **Phase 1a** (sub-agent A): `LoadingWheel.kt` 実装置換
  - Task 1a.1: `LoadingWheel.kt` を LoadingIndicator ベースに書き換え（公開シグネチャ維持）
  - Task 1a.2: `LoadingWheelTest.kt`（新規）で描画 + a11y + testTag 検証
  - Task 1a.3: 8 call sites の動作確認テスト

- **Phase 1b** (sub-agent B): `TastileFabMenu` 新規作成
  - Task 1b.1: `TastileFabMenu.kt` 新規作成
  - Task 1b.2: `TastileFabMenuTest.kt`（新規）

- **Phase 1c** (sub-agent C): `TastileButtonGroup` 新規作成
  - Task 1c.1: `TastileButtonGroup.kt` 新規作成（§4.3 契約準拠）
  - Task 1c.2: `TastileButtonGroupTest.kt`（新規）

### Phase 2: QuickCreate FAB 経路置換（単一 agent、Phase 1b 完了後）

- Task 2.1: `TimelineScreen.kt:205` の `NiaFloatingActionButton` を `TastileFabMenu` 経由に置換
- Task 2.2: `TilesScreen.kt:124` の `NiaExtendedFloatingActionButton` を `TastileFabMenu` 経由に置換
- Task 2.3: Robolectric 統合テスト（FAB クリック → `Overlay.QuickCreate` 発火 → `QuickCreateSheetMobile` 表示）
- Task 2.4: `QuickCreateSheetMobile.kt` は無変更であることを確認

### Phase 3: 統合検証（単一 agent）

- Task 3.1: `./gradlew verify` 緑化
- Task 3.2: `gfxinfo` で motion physics の fps 計測（Timeline / Tiles FAB 展開計測）
- Task 3.3: instrumented smoke（§7.3 の `--tests` 経路で MainActivity 経由 QuickCreate 起動）
- Task 3.4: ドキュメント更新（`docs/superpowers/m3/` 配下、`README.md` の Material 3 セクション）

## 9. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| material3 1.5.0-alpha27 の破壊的変更 | CI fail | Phase 0 で pin のみ先行。`verify` が落ちたら即修正可能な粒度で PR 化 |
| `ExperimentalMaterial3ExpressiveApi` の API 改名（LoadingIndicator は alpha19 で stable 昇格取り消しの経歴あり） | ビルド失敗 | `@file:OptIn` を集中宣言し、変更時に修正箇所を限定 |
| Spring physics の端末依存 fps 落ち | UX 低下 | Phase 3 で gfxinfo 計測 → パラメータ escape 用意 |
| FAB Menu 旧 FAB との二重定義 | UI 衝突 | QuickCreate 以外の既存 FAB 利用箇所は本 scope では触らない。Phase 2 完了後に利用箇所棚卸し |
| LoadingWheel 内部実装置換で 8 call sites のうち 1 箇所でも描画崩れ | 回帰 | Phase 1a で 8 call sites 全てに対する smoke test 必須化 |

## 10. References

- `app/build.gradle.kts` — guard 実装箇所、`material3` dependency 定義
- `app/src/main/java/app/tastile/android/core/designsystem/theme/ThemeTokenLocals.kt` — 既存 Local* パターン（motionScheme は独自 Local を作らず MaterialTheme 経由）
- `app/src/main/java/app/tastile/android/core/designsystem/component/LoadingWheel.kt` — 既存実装（公開シグネチャ維持・内部置換）
- `app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt:205` — NiaFloatingActionButton 起点
- `app/src/main/java/app/tastile/android/ui/mobile/tabs/TilesScreen.kt:124` — NiaExtendedFloatingActionButton 起点
- `app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateSheetMobile.kt:78` — Overlay.QuickCreate 受信（無変更）
- `app/src/main/java/app/tastile/android/ui/mobile/Overlay.kt` — Overlay.QuickCreate 定義
- `app/src/test/.../buildlogic/VerifyDesignSystemImportsGuardTest.kt` — guard test（無変更）
- `docs/superpowers/m3/before-reports/` — Compose Compiler Reports baseline
- `https://developer.android.com/jetpack/androidx/releases/compose-material3` — material3 リリース履歴（2026-08-26: 1.5.0-alpha27）
- `https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme` — MotionScheme 公式 API
- `CLAUDE.md` — Build and Verify / Build-Time Hard Requirements / Working Rules
