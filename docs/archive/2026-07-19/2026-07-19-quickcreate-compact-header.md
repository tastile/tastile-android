# QuickCreate Compact Header

Date: 2026-07-19
Branch: main (no worktree, per user instruction)

## Goal

QuickCreate の base sheet (PanelSheet 経由) のヘッダを tight にする。

現状: `ModalBottomSheet` 既定の `dragHandle` (BottomSheetDefaults.DragHandle()) が独立した行として描かれ、その下パディング + `Row(☓ | title | Create)` が縦方向に間延びしている。

目標: ☓ / Create をハンドルの近く／同じ列に置き、ハンドル起因のスタイル間延びを排除する。subpanel 側の `StackedSheet` は既に独自の tight レイアウトなので変更しない。他 UI (Account / Subscription / Tokens / Picker 系 / Notifications / TileEdit / QuickCreate 以外) は PanelSheet の既定挙動を維持。

## Reference

参照ソース: https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/BottomSheet.kt

要点:

- `ModalBottomSheet(... dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() }, content)` の `dragHandle` は content の上 に M3 が配置する独立スロット。
- `dragHandle = null` → ハンドル非表示 (content は独立 Column)。
- `dragHandle = { ... }` → 渡した composable がハンドルスロットに入る。
- `BottomSheetDefaults.DragHandle()` を再描画せず、参照コードそのまま呼ぶ。

方針: `compactHeader = true` のときだけ `dragHandle = { Column { BottomSheetDefaults.DragHandle(); Row(buttons) } }` を渡し、ハンドルとボタンを同一 Column に同居させる。`compactHeader = false` (既定) は dragHandle を渡さず M3 既定位置 (content の上) を維持。

## Touch points

| ファイル | 変更 |
|---|---|
| `app/src/main/java/app/tastile/android/ui/mobile/sheets/PanelSheet.kt` | `compactHeader: Boolean = false` param 追加。true 分岐で `dragHandle` に Column{DrawHandle + Row} を渡す。false 分岐は既存コードと完全同一。`headerRow` をローカル lambda として抽出して両分岐で再利用。 |
| `app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateSheetMobile.kt` | 既存 `PanelSheet(...)` 呼び出し (line 103 付近) に `compactHeader = true` を 1 行追加。 |

差分行数: PanelSheet.kt に +25 行程度 (既存行は不変)。QuickCreateSheetMobile.kt に +1 行。

## Steps

1. `PanelSheet.kt` を Edit で更新:
   - param 追加: `compactHeader: Boolean = false`
   - `headerRow` をローカル lambda として抽出
   - `ModalBottomSheet` 呼び出しに `dragHandle = if (compactHeader) { { Column { BottomSheetDefaults.DragHandle(); Row(headerRow) } } } else null`
   - body を `if (compactHeader) ... else ...` で分岐 (compactHeader 時は body Column から `fillMaxHeight(0.88f)` を外す)
2. `QuickCreateSheetMobile.kt` を Edit で更新:
   - `PanelSheet(` 呼び出しに `compactHeader = true` 追加
3. WSL 経由 (`tastile-android.wslc`) でビルド検証:
   - `./gradlew compileDebugKotlin`
   - `./gradlew :app:assembleDebug`
4. APK を実機 / エミュレータへインストール (Xiami MIUI 経路は memory 参照: `MSYS_NO_PATHCONV=1 adb push` + `pm install --user 0 -r`)
5. QuickCreate シナリオの動作確認:
   - base sheet 表示時に M3 DragHandle + ☓ + Create が tight に並ぶ
   - subpanel 遷移時に `StackedSheet` の tight ヘッダと整合
   - ☓ タップで overlay.dismiss、Create タップで submit
   - 他 UI (Account / Subscription / Tokens / Picker / Notifications) のパネル外観・挙動が変わらない

## Validation criteria

- `compileDebugKotlin` がエラーなく通る
- `assembleDebug` が APK を生成する
- 他 UI の regression なし (drag handle 位置・ボタン位置・submit 動作すべて不変)
- QuickCreate base sheet のヘッダ高さが現行比で明らかに短縮している
- subpanel (StackedSheet) のドラッグ・☓・back 動作が不変

## Rollback

両ファイルを `git checkout -- <path>` で 1 コマンド戻せる。`compactHeader` パラメータは独立した optional 引数なので、削除すれば他 UI に副作用なし。