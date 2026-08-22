# TimelineScreen スケール切替 UI を Material 3 の ExposedDropdownMenuBox に置換する実装プラン

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `MobileTopBar` の `ScaleDropdown` を M3 公式の `ExposedDropdownMenuBox` パターンに置き換え、トリガ部を Material 3 標準の見た目に揃える。

**Architecture:** 既存の `MobileTopBar` 公開 API は不変とし、private な `ScaleDropdown` の実装だけ書き換える。`OutlinedTextField(readOnly)` + `menuAnchor()` をトリガに、シェブロンは `ExposedDropdownMenuDefaults.TrailingIcon` を使う。`Modifier.semantics { contentDescription = "Scale: ${scale.name}" }` を `ExposedDropdownMenuBox` に移し、既存テストとアクセシビリティ仕様を維持する。

**Tech Stack:** Jetpack Compose, Material 3 (compose-bom:2024.12.01), `androidx.compose.ui.test` (JVM unit tests), Gradle 9.x / AGP 9.x / JDK 17.

## 影響ファイル

- 変更: `app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt`
  - `ScaleDropdown` を `ExposedDropdownMenuBox` パターンに書き換え。
  - 不要になった `CompactPickerButton` を削除。
  - 不要 import (`BorderStroke`, `Surface`, `RoundedCornerShape`, `ArrowDropDown` ボタ側) を整理し、必要 import (`ExposedDropdownMenuBox`, `ExposedDropdownMenu`, `ExposedDropdownMenuDefaults`, `OutlinedTextField`, `ExperimentalMaterial3Api`, `width`) を追加。
- 変更: `app/src/test/java/app/tastile/android/ui/mobile/MobileTopBarTest.kt`
  - 既存テストはそのまま。
  - 新規テストとしてスケール切替の動作を 1 件追加（Step 1.2）。

---

## Task 1: スケール切替 UI の M3 化

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt:1-272`
- Modify: `app/src/test/java/app/tastile/android/ui/mobile/MobileTopBarTest.kt:1-58`

### Step 1.1: 既存テストの事前確認

既存 `MobileTopBarTest` が緑であることを確認する（`ScaleDropdown` 差し替え前のベースライン）。

Run: `./gradlew :app:testDebugUnitTest --tests app.tastile.android.ui.mobile.MobileTopBarTest`
Expected: `BUILD SUCCESSFUL`、テスト 1 件成功。

### Step 1.2: 失敗するテストを追加

`app/src/test/java/app/tastile/android/ui/mobile/MobileTopBarTest.kt` の末尾に以下を追加する。

```kotlin
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.util.concurrent.atomic.AtomicInteger

// ... 既存 import に `onNodeWithText`, `performClick`, `AtomicInteger` を追加

    @Test
    fun `scale picker opens and selects new scale`() {
        val selected = AtomicInteger(-1)

        rule.setContent {
            MobileTopBar(
                title = "Execute",
                scale = TimelineScale.Day,
                onScaleChange = { selected.set(it.ordinal) },
                onMenu = {},
                onNotifications = {},
                onAvatar = {},
            )
        }

        rule.onNodeWithContentDescription("Scale: Day").performClick()
        rule.onNodeWithText("Week").performClick()
        rule.onNodeWithContentDescription("Scale: Week").assertIsDisplayed()

        assertEquals(TimelineScale.Week.ordinal, selected.get())
    }
```

Run: `./gradlew :app:testDebugUnitTest --tests app.tastile.android.ui.mobile.MobileTopBarTest.scale_picker_opens_and_selects_new_scale`
Expected: FAIL（現状の実装では `onNodeWithContentDescription("Scale: Day")` をクリックしても `onNodeWithText("Week")` がメニュー項目として表示されない、または選択後に `contentDescription` が `Scale: Week` へ更新されないため）。

### Step 1.3: `MobileTopBar.kt` の import 整理

`app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt` の import ブロックを以下に置換する。

```kotlin
package app.tastile.android.ui.mobile

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenuItem
// m2-allow: m3-component
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.ExposedDropdownMenuBox
// m2-allow: m3-component
import androidx.compose.material3.ExposedDropdownMenuDefaults
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: m3-component
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.dashboard.TimelineScale
import coil.compose.AsyncImage
```

`m2-allow` マーカーが必要なのは `app/build.gradle.kts` の `verifyDesignSystemImports` タスクが禁止している M3 系 import のみ。`OutlinedTextField` / `ExposedDropdownMenuBox` / `ExposedDropdownMenuDefaults` / `ExperimentalMaterial3Api` は M3 コンポーネントなので、各 import の直前行に `// m2-allow: m3-component` を入れる（`ExperimentalMaterial3Api` も同様に `m3-component` 区分で扱う）。

### Step 1.4: `ScaleDropdown` を `ExposedDropdownMenuBox` に置換

`MobileTopBar.kt` の `ScaleDropdown` 関数と `CompactPickerButton` 関数を以下に置換する（`CompactPickerButton` は削除）。

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

### Step 1.5: テスト再実行

Run: `./gradlew :app:testDebugUnitTest --tests app.tastile.android.ui.mobile.MobileTopBarTest`
Expected: 既存テスト + 新規テスト計 2 件成功。

### Step 1.6: デザインシステムガードの検証

`MobileTopBar.kt` の import が `// m2-allow` マーカー規約に沿っているか確認する。

Run: `./gradlew :app:verifyDesignSystemImports`
Expected: `BUILD SUCCESSFUL`、違反なし。

### Step 1.7: コミット

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt \
        app/src/test/java/app/tastile/android/ui/mobile/MobileTopBarTest.kt
git commit -m "feat(android): switch timeline scale dropdown to M3 ExposedDropdownMenuBox"
```

---

## Task 2: 静的解析とビルド検証

**Files:**
- Modify: なし

### Step 2.1: デザインシステム・ブリッジ検証

Run: `./gradlew :app:check`
Expected: `BUILD SUCCESSFUL`（`verifyDesignSystemImports` + `verifyNoEmbeddedServerSecrets` を含む全 `check` タスクが緑）。

### Step 2.2: Debug ビルド

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`、APK が `app/build/outputs/apk/debug/app-debug.apk` に生成される。

### Step 2.3: 動作確認（任意・手元端末がある場合）

エミュレータまたは接続済み実機で Day ピッカーをタップ → メニュー展開 → Week を選択 → ピッカーラベルが Week に更新されることを確認する。トップバーの他のアイコン（menu/notifications/avatar）が影響を受けていないことも確認する。

### Step 2.4: コミット（変更があった場合のみ）

Step 2.1–2.3 で生成物が更新された場合は差分を確認した上でコミットする（通常は APK 等は git 管理外のためコミット不要）。
