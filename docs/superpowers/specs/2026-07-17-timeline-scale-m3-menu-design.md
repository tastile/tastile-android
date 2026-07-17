# TimelineScreen スケール切替 UI を Material 3 の Menu パターンに揃える

**日付:** 2026-07-17
**対象:** tastile-android / TimelineScreen 右上の Day/Week/Month ピッカー
**スコープ:** ピル型バッチトリガ + M3 `DropdownMenu` の組み合わせに揃え、ポップアップの見た目を M3 標準へ揃える。挙動・公開 API・アクセシビリティ仕様は不変。

## 背景

`app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt` の `ScaleDropdown` は、自前の `Surface(onClick)` ピル型トリガ + `DropdownMenu` ポップアップで構成されている。公式の [Material 3 menu レシピ](https://developer.android.com/develop/ui/compose/components/menu) に沿って、ピル型トリガはそのままで、ポップアップを M3 標準の `DropdownMenu` / `DropdownMenuItem` に統一する。

> 「ドロップダウン」（`ExposedDropdownMenuBox` + テキストフィールド）は対象外。あくまでバッチ型トリガ + 一時的ポップアップの "menu" レシピを採用する。

## 目標

- スケール切替 UI を「ピル型バッチトリガ + M3 `DropdownMenu`」の構成に揃える。
- ピル型トリガの見た目は現行のものを維持（角丸 50% / outline border / シェブロン）。
- ポップアップは M3 標準の `DropdownMenu` / `DropdownMenuItem` を継続利用し、選択中スケールを `FontWeight.SemiBold` で示す振る舞いを保つ。
- 既存の `MobileTopBarTest`（`Scale: Day` 検出 + 新規追加した「Week 選択」テスト）を継続パスさせる。
- アクセシビリティ仕様（`contentDescription = "Scale: ${scale.name}"`）を不変に保つ。

## 非目標

- `TimelineScale` 列挙体の中身や並び順の変更。
- トップバー以外のレイアウト調整、右側アイコン群、グラデーション背景の変更。
- テキストフィールド型ピッカー（`ExposedDropdownMenuBox`）への置換。
- ポップアップ項目へのアイコン追加 / セパレータ / ヘッダ追加などの装飾変更。
- i18n / 翻訳キー追加。`scale.name` を素直に出力する現行動作を維持する。

## 設計

### 影響ファイル

- 変更: `app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt`
  - `ScaleDropdown` を「`CompactPickerButton` ピル + `DropdownMenu`」構成に書き直す。
  - `CompactPickerButton` プライベートコンポーザブルを `MobileTopBar.kt` 内に保持する。
  - M3 関連 import を `// m2-allow:` マーカー規約に沿って整理する。
- 変更なし: `MobileTopBarTest.kt`（既存テスト + 新規 `scale picker opens and selects new scale` テストはそのまま通す）、`DashboardViewModel.kt`（`TimelineScale`）、`MobileScaffold.kt`、テストデータ。

### UI 仕様

`Box` 内にピル型トリガ（`CompactPickerButton`）+ `DropdownMenu` を入れ、公式の "Create a basic drop-down menu" レシピに準拠する。`ExposedDropdownMenuBox` / `OutlinedTextField` / `menuAnchor()` は使わない。

```kotlin
@Composable
private fun ScaleDropdown(
    scale: TimelineScale,
    onScaleChange: (TimelineScale) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        CompactPickerButton(
            label = scale.name,
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = "Scale: ${scale.name}" },
        )
        DropdownMenu(
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

/**
 * Compact pill-shaped picker button — wraps [Surface] with a pill shape and
 * outline border so the top-bar dropdown trigger matches the Material 3
 * surface interaction behaviour (built-in ripple via [Surface]).
 */
@Composable
private fun CompactPickerButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
```

- ピル型トリガは 1 行のラベル + シェブロン。`Surface(onClick)` がリップルとクリックスライダを M3 標準で提供する。
- `contentDescription` はピル型トリガに付与し、既存テスト / 新規テスト双方の `onNodeWithContentDescription("Scale: …")` アサーションをパスさせる。
- `DropdownMenu` 内の `DropdownMenuItem` フォント差は従来どおり（選択中スケールのみ `FontWeight.SemiBold`）。

### アクセシビリティ

- ピル型トリガは `Surface(onClick)` により M3 標準の `Role.Button` 相当のフォーカスを獲得。
- `contentDescription = "Scale: ${scale.name}"` はピル型トリガに付与し、新規追加した「Week 選択」テストでも `Scale: Day` → クリック → `Week` 選択 → `Scale: Week` の再描画が検証できる。
- `Icons.Outlined.ArrowDropDown` の `contentDescription` は `null`（装飾的）。

## テスト

- 既存: `app/src/test/java/app/tastile/android/ui/mobile/MobileTopBarTest.kt` の `top bar renders menu, scale, notifications, avatar controls` はそのまま通す（`Scale: Day` を `onNodeWithContentDescription` で検出する既存アサーションを破壊しない）。
- 追加: 既に追加済みの `scale picker opens and selects new scale` テストで「ピル型トリガをクリック → `Week` を選択 → 親状態が `TimelineScale.Week` に更新」を検証する。`AtomicInteger` 経由の `onScaleChange` 呼び出し検証もそのまま通る。
- 手動検証: `assembleDebug` 後、エミュレータまたは実機で Day ピッカーをタップ → メニュー展開 → Week を選択 → ピル型トリガのラベルが `Week` に更新されることを確認する。トップバーの他のアイコン（menu/notifications/avatar）が影響を受けていないことも確認する。

## リスク

- `CompactPickerButton` の `Surface(onClick)` を使う場合、`MaterialTheme.colorScheme.outline` の半透明で枠線が見えにくくなる可能性がある。必要なら `border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))` のような微調整を許容する。
- `m2-allow` マーカーが付かない M3 import が混じると `verifyDesignSystemImports` タスクが失敗する。新規追加する M3 import には直前行の `// m2-allow: m3-component` を必ず付ける。
- `DropdownMenu` のオフセットがピル型トリガ直下からずれる場合、`DropdownMenu(offset = DpOffset(0.dp, 4.dp))` での微調整を許容する。
