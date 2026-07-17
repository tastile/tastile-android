# TimelineScreen スケール切替 UI を Material 3 の ExposedDropdownMenuBox に置換

**日付:** 2026-07-17
**対象:** tastile-android / TimelineScreen 右上の Day/Week/Month ピッカー
**スコープ:** トリガの見た目を M3 標準へ揃える。状態/ロジック/アクセシビリティは不変。

## 背景

現状は `app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt` 内の
`ScaleDropdown` で、自前の `Surface(onClick)` トリガ + `DropdownMenu` を組み立てている。
半透明グラデーションのトップバー内で異質に見えやすく、M3 公式のドロップダウン
`ExposedDropdownMenuBox` パターンに揃える。

## 目標

- スケール切替 UI を Material 3 の `ExposedDropdownMenuBox` + `OutlinedTextField(readOnly)` パターンに置換する。
- 現行の挙動と外部仕様を保ったまま、トリガ部の見た目を M3 標準に揃える。
- 既存の `MobileTopBarTest` を継続パスさせ、アクセシビリティ仕様を壊さない。

## 非目標

- `TimelineScale` 列挙体の中身や並び順の変更。
- トップバー以外のレイアウト調整、右側アイコン群、グラデーション背景の変更。
- ドロップダウン項目のキーボード操作拡張（TalkBack のローテーション挙動は標準 M3 で十分とする）。
- i18n / 翻訳キー追加。`scale.name` を素直に出力する現行動作を維持する。

## 設計

### 影響ファイル

- 変更: `app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt`
  - `ScaleDropdown` を `ExposedDropdownMenuBox` パターンに書き換え。
  - 不要になる `CompactPickerButton` を削除（呼び出し元はこの private 関数のみ）。
  - 既存の `Surface(onClick)` / `BorderStroke` 関連の import を整理。
- 変更なし: `MobileTopBarTest.kt`、`DashboardViewModel.kt`（`TimelineScale`）、`MobileScaffold.kt`、テストデータ。

### UI 仕様

`ExposedDropdownMenuBox` を導入し、トリガを `OutlinedTextField` で構成する。

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaleDropdown(
    scale: TimelineScale,
    onScaleChange: (TimelineScale) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.semantics { contentDescription = "Scale: ${scale.name}" },
    ) {
        OutlinedTextField(
            value = scale.name,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .width(112.dp),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TimelineScale.entries.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = entry.name,
                            fontWeight = if (entry == scale) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onScaleChange(entry)
                        expanded = false
                    },
                )
            }
        }
    }
}
```

- トリガ幅 112.dp は `Day`/`Week`/`Month` の最長ラベルに余白を足した固定値（テキストフィールド既定密度で 1 行に収まる）。
- 項目側のフォント差は従来どおり維持。タップ時の挙動・状態遷移は不変。
- `Modifier.semantics { contentDescription = "Scale: ${scale.name}" }` を `ExposedDropdownMenuBox` に付与して既存テストの `onNodeWithContentDescription("Scale: Day")` を継続パスさせる。
- `CompactPickerButton` 関連（`Surface`、`BorderStroke`、`Row`、`Icons.Outlined.ArrowDropDown` のうちボタン側の使用）を削除し、必要な import のみ残す。

### アクセシビリティ

- トップバーは `Role.Button` 相当のトリガを継続提供（`ExposedDropdownMenuBox` 内部の `OutlinedTextField` は M3 既定で clickable フォーカスを獲得）。
- `contentDescription` は `"Scale: ${scale.name}"` を維持。`MobileTopBarTest` のアサーションを破壊しない。

## テスト

- 既存: `app/src/test/java/app/tastile/android/ui/mobile/MobileTopBarTest.kt` はそのまま通す。
  - 変更点は `ScaleDropdown` 内部のみで `MobileTopBar` 公開 API は不変。
- 追加: `MobileTopBarTest` にスケールピッカーを開いて `Week` を選択するケースを追加。
  - `rule.onNodeWithContentDescription("Scale: Day")` を perform click → `onNodeWithText("Week")` を perform click → `rule.onNodeWithContentDescription("Scale: Week")` を assertIsDisplayed。
  - ラムダの `onScaleChange` 呼び出し回数を `AtomicInteger` で検証。
- 手動検証: `assembleDebug` 後、エミュレータまたは実機で `Day` タップ→`Week` へ切替できることと、シェブロン回転・項目ハイライトが M3 標準どおりであることを確認する。

## リスク

- `ExposedDropdownMenuBox` は `ExperimentalMaterial3Api` 注釈付き。`build.gradle.kts` の `compose-bom:2024.12.01` 配下では安定して使用可能（他画面 `MemoScreen.kt:109` 等で既に `@OptIn` 付きで使用実績あり）。
- トリガを `OutlinedTextField` に置き換えるため、トップバー左端の `IconButton` と高さが揃わなくなる可能性がある。`OutlinedTextField` のデフォルト高さは ~56dp なので `heightIn(min = 48.dp)` を持つ他のボタンと同等。問題があればトリガの `Modifier.heightIn` で微調整。
- グラデーション背景の半透明で `OutlinedTextField` の輪郭が薄くなる場合は、`colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()` の引数で輪郭色を `MaterialTheme.colorScheme.outline` に明示する。
