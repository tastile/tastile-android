# Panel Leading-Column Alignment — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Align the left edge of every row in the QuickCreate base panel and all routed subpanels to a single x-coordinate (56.dp from row start), using M3 `ListItem`'s `leadingContent` slot width as the canonical anchor.

**Architecture:** Introduce a single `PanelTokens` object holding `LeadingColumnWidth = 56.dp` (= M3 ListItem 16+24+16). Apply via per-row padding/contentPadding adjustments grouped by row class (ListItem / text input / outlined button / chip row / text field). No new wrapper components — keep `NiaListItem` / `NiaOutlinedButton` as the M3 surface; just align the x-coordinate through their existing modifier slots. Verify with a single Compose UI test asserting x-equality across row types, plus a static `rg` guard for regression prevention.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (`ListItem`, `OutlinedButton`, `OutlinedTextField`), Robolectric + Compose UI Test (existing), `rg` (static guard).

**Design reference:** `docs/plans/2026-07-17-panel-leading-column-alignment-design.md`

**Branch:** `main` (current `tastile-android` HEAD; no separate feature branch needed for this single-PR scope).

---

## Task 1: Add `PanelTokens` (Case A/B foundation)

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/PanelTokens.kt`
- Create: `app/src/test/java/app/tastile/android/core/designsystem/theme/PanelTokensTest.kt`

**Step 1.1: Write the failing test**

`PanelTokensTest.kt`:

```kotlin
package app.tastile.android.core.designsystem.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PanelTokensTest {
    @Test fun `LeadingColumnWidth equals 56dp — M3 ListItem slot`() {
        assertThat(PanelTokens.LeadingColumnWidth.value).isEqualTo(56f)
    }

    @Test fun `LeadingIconSize equals 24dp — M3 ListItem leading icon`() {
        assertThat(PanelTokens.LeadingIconSize.value).isEqualTo(24f)
    }

    @Test fun `LeadingColumnGap equals 16dp — M3 ListItem leading-to-text gap`() {
        assertThat(PanelTokens.LeadingColumnGap.value).isEqualTo(16f)
    }

    @Test fun `LeadingColumnWidth is the sum of icon size plus gap plus 16dp start padding`() {
        val expected = PanelTokens.LeadingIconSize.value +
            PanelTokens.LeadingColumnGap.value +
            16f
        assertThat(PanelTokens.LeadingColumnWidth.value).isEqualTo(expected)
    }
}
```

**Step 1.2: Run test, verify it fails (file missing)**

```
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.theme.PanelTokensTest"
```

Expected: FAIL — `Unresolved reference: PanelTokens`.

**Step 1.3: Create `PanelTokens.kt`**

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object PanelTokens {
    val LeadingColumnWidth: Dp = 56.dp
    val LeadingIconSize: Dp = 24.dp
    val LeadingColumnGap: Dp = 16.dp
}
```

**Step 1.4: Run test, verify pass**

```
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.theme.PanelTokensTest"
```

Expected: PASS (4 tests).

**Step 1.5: Commit**

```bash
git add app/src/main/java/app/tastile/android/core/designsystem/theme/PanelTokens.kt \
        app/src/test/java/app/tastile/android/core/designsystem/theme/PanelTokensTest.kt
git commit -m "feat(designsystem): add PanelTokens leading-column constants"
```

---

## Task 2: Fix `EditableTitleField` (Case C)

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt:400-403` (the inner `Column.padding`)
- Create: `app/src/test/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateLeadingColumnAlignmentTest.kt`

**Step 2.1: Write the failing alignment test**

`QuickCreateLeadingColumnAlignmentTest.kt` — first case only at this stage:

```kotlin
package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.theme.NiaTheme
import org.junit.Rule
import org.junit.Test

class QuickCreateLeadingColumnAlignmentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `title text and ListItem headline share the same x-coordinate`() {
        composeTestRule.setContent {
            NiaTheme {
                QuickCreatePanelContent(
                    store = QuickCreateStateStore(),
                    onClose = {},
                )
            }
        }
        // Title field (Case C) and Time EssentialRow (Case A) — first verifiable pair.
        composeTestRule.onNodeWithTag("quick-create-title")
            .assertLeftPositionInRootIsEqualTo(56.dp)  // initial anchor; will adjust after impl
        composeTestRule.onNodeWithTag("quick-create-essential-time")
            .assertLeftPositionInRootIsEqualTo(56.dp)
    }
}
```

**Step 2.2: Run test, verify it fails**

```
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateLeadingColumnAlignmentTest"
```

Expected: FAIL — title at `x = 16.dp`, time at `x = 56.dp` (asymmetric).

**Step 2.3: Apply Case C fix**

In `QuickCreateBasePanel.kt`, find the inner `Column` inside `EditableTitleField` (around line 400-403):

Before:
```kotlin
Column(
    modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
) {
```

After:
```kotlin
Column(
    modifier = modifier
        .fillMaxWidth()
        .padding(start = 56.dp, end = 16.dp),
) {
```

Use the new token rather than the literal — replace `56.dp` with `PanelTokens.LeadingColumnWidth` (add the import).

**Step 2.4: Run test, verify pass**

```
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateLeadingColumnAlignmentTest"
```

Expected: PASS — title at `x = 56.dp`, time at `x = 56.dp`.

**Step 2.5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt \
        app/src/test/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateLeadingColumnAlignmentTest.kt
git commit -m "fix(quickcreate): align EditableTitleField text to leading column (Case C)"
```

---

## Task 3: Fix outlined buttons (Case D)

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt` (three button callsites at lines 178-192, 215-241, 256-270 + any text-only button found via grep in `QuickCreateSubpanels.kt`)

**Step 3.1: Extend the alignment test (add button cases)**

Add to `QuickCreateLeadingColumnAlignmentTest.kt`:

```kotlin
@Test fun `Add condition and Add task buttons align to leading column`() {
    composeTestRule.setContent {
        NiaTheme {
            QuickCreatePanelContent(
                store = QuickCreateStateStore(),
                onClose = {},
            )
        }
    }
    composeTestRule.onNodeWithTag("quick-create-condition-add")
        .assertLeftPositionInRootIsEqualTo(56.dp)
    composeTestRule.onNodeWithTag("quick-create-add-task")
        .assertLeftPositionInRootIsEqualTo(56.dp)
}
```

Note: `assertLeftPositionInRootIsEqualTo` checks the *left edge of the node's composable bounds*, which for a button with icon is the icon's left edge. To check the *text x-position* inside the button, use `onNodeWithText("Add condition or group").assertLeftPositionInRootIsEqualTo(56.dp)` instead. The implementer should pick whichever represents "the user-visible text x" — confirm with the visual screenshot in Task 7.

**Step 3.2: Run extended test, verify fail**

```
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateLeadingColumnAlignmentTest"
```

Expected: FAIL — button text x ≠ 56.dp.

**Step 3.3: Apply Case D fixes**

Three `NiaOutlinedButton` callsites in `QuickCreateBasePanel.kt` change `contentPadding` from `start = 16.dp` to `start = 0.dp` (Case D-1 / D-3):

Before (each of the three):
```kotlin
contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 24.dp, bottom = 8.dp),
```

After:
```kotlin
contentPadding = PaddingValues(start = 0.dp, top = 8.dp, end = 24.dp, bottom = 8.dp),
```

For text-only outlined buttons (Case D-2 — e.g. "Move up", "Move down" inside subpanel task editors), find them via:

```
rg "NiaOutlinedButton|OutlinedButton" app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubpanels.kt
```

For each text-only button: keep `contentPadding(start = 16.dp, ...)`, but prepend `Box(Modifier.size(PanelTokens.LeadingIconSize))` + `Spacer(Modifier.width(PanelTokens.LeadingColumnGap))` inside the content lambda.

**Step 3.4: Run test, verify pass**

```
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateLeadingColumnAlignmentTest"
```

Expected: PASS — all button text at `x = 56.dp`.

**Step 3.5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt \
        app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubpanels.kt \
        app/src/test/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateLeadingColumnAlignmentTest.kt
git commit -m "fix(quickcreate): align outlined buttons to leading column (Case D)"
```

---

## Task 4: Fix base-panel chip row (Case E base)

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt:115-122`

**Step 4.1: Extend alignment test (add chip-start case)**

Add to `QuickCreateLeadingColumnAlignmentTest.kt`:

```kotlin
@Test fun `base panel chip row first chip aligns to leading column`() {
    composeTestRule.setContent {
        NiaTheme {
            QuickCreatePanelContent(
                store = QuickCreateStateStore(),
                onClose = {},
            )
        }
    }
    composeTestRule.onNodeWithTag("quick-create-organize-row")
        .assertLeftPositionInRootIsEqualTo(56.dp)
}
```

**Step 4.2: Run test, verify fail**

```
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateLeadingColumnAlignmentTest"
```

Expected: FAIL — chip row currently starts at `x = 16.dp`.

**Step 4.3: Apply Case E (base) fix**

In `QuickCreateBasePanel.kt` (line ~119):

Before:
```kotlin
.padding(horizontal = 16.dp)
```

After:
```kotlin
.padding(start = PanelTokens.LeadingColumnWidth, end = 16.dp)
```

(Add `import app.tastile.android.core.designsystem.theme.PanelTokens`.)

**Step 4.4: Run test, verify pass**

```
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateLeadingColumnAlignmentTest"
```

Expected: PASS — chip row starts at `x = 56.dp`.

**Step 4.5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt \
        app/src/test/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateLeadingColumnAlignmentTest.kt
git commit -m "fix(quickcreate): align base chip row to leading column (Case E base)"
```

---

## Task 5: Fix subpanel chip rows + text fields (Case E subpanel + Case F)

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubpanels.kt` (9 chip-row FlowRow callsites + any `OutlinedTextField` callsites that use `padding(horizontal = 16.dp)`)

**Step 5.1: Extend alignment test (add subpanel chip-start cases)**

Add to `QuickCreateLeadingColumnAlignmentTest.kt`:

```kotlin
@Test fun `subpanel chip rows align to leading column`() {
    val store = QuickCreateStateStore()
    composeTestRule.setContent {
        NiaTheme {
            QuickCreateSubpanel(
                panel = QuickCreatePanel.Time,
                draft = store.state.value,
                store = store,
                onBack = {},
                projects = emptyList(),
                knownTags = emptyList(),
            )
        }
    }
    composeTestRule.onNodeWithTag("quick-create-when-chips")
        .assertLeftPositionInRootIsEqualTo(56.dp)
    composeTestRule.onNodeWithTag("quick-create-time-of-day-chips")
        .assertLeftPositionInRootIsEqualTo(56.dp)
}
```

**Step 5.2: Run test, verify fail**

```
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateLeadingColumnAlignmentTest"
```

Expected: FAIL — subpanel chip rows still at `x = 16.dp`.

**Step 5.3: Apply Case E (subpanel) + Case F fixes**

Find every `padding(horizontal = 16.dp)` in `QuickCreateSubpanels.kt` that wraps a `FlowRow` or `OutlinedTextField`:

```
rg "padding\(horizontal = 16\.dp\)" app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubpanels.kt
```

For each:
- If the surrounding composable is a `FlowRow` chip row → change to `padding(start = PanelTokens.LeadingColumnWidth, end = 16.dp)`.
- If the surrounding composable is an `OutlinedTextField` → change to `padding(start = PanelTokens.LeadingColumnWidth, end = 16.dp)`.

Do **not** touch:
- The outer `Column.padding(horizontal = 16.dp, vertical = 12.dp)` at line ~157 (panel-edge inset, out of scope).
- `AppPickerButton` callsites (independent layout).

Add the import: `import app.tastile.android.core.designsystem.theme.PanelTokens`.

**Step 5.4: Run test, verify pass**

```
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateLeadingColumnAlignmentTest"
```

Expected: PASS — all chip rows + text fields aligned.

**Step 5.5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubpanels.kt \
        app/src/test/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateLeadingColumnAlignmentTest.kt
git commit -m "fix(quickcreate): align subpanel chip rows + text fields (Case E/F)"
```

---

## Task 6: Add static guard (regression prevention)

**Files:**
- Create: `scripts/panel-leading-column-guard.sh`

**Step 6.1: Write the guard script**

```bash
#!/usr/bin/env bash
# scripts/panel-leading-column-guard.sh
#
# Asserts that no panel-row Modifier.padding/contentPadding reverts the
# leading-column alignment. Allowed exceptions (per design doc):
#   - EditableTitleField (Case C uses start=56, end=16)
#   - Text-only outlined buttons (Case D-2)
#   - Panel outer Column inset (not a row)
set -euo pipefail

PKG="app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate"
fail=0

echo "[1/2] Checking for ad-hoc padding(horizontal = 16.dp) on chip/row/field modifiers..."
hits=$(rg -n "padding\(horizontal = 16\.dp\)" "$PKG" || true)
if [[ -n "$hits" ]]; then
  echo "FAIL: found unaligned padding(horizontal=16.dp):"
  echo "$hits"
  fail=1
fi

echo "[2/2] Checking for contentPadding(start = 16.dp) on outlined buttons (excluding D-2)..."
hits=$(rg -n "contentPadding = PaddingValues\(start = 16\.dp" "$PKG" || true)
if [[ -n "$hits" ]]; then
  echo "FAIL: found unaligned contentPadding(start=16.dp):"
  echo "$hits"
  fail=1
fi

if [[ $fail -eq 0 ]]; then
  echo "OK: no leading-column regressions."
fi
exit $fail
```

Make executable: `chmod +x scripts/panel-leading-column-guard.sh`.

**Step 6.2: Run the guard, verify pass**

```
bash scripts/panel-leading-column-guard.sh
```

Expected: `OK: no leading-column regressions.` (exit 0).

If it reports a hit, that hit must be either:
- An allowed exception (Case D-2 text-only button — extend the script with a `grep -v` filter for the specific test tag), or
- A genuinely missed row — go back and fix per Case C/D/E/F rules.

**Step 6.3: Commit**

```bash
git add scripts/panel-leading-column-guard.sh
git commit -m "chore(quickcreate): add panel leading-column static guard"
```

---

## Task 7: Visual confirmation + final PR

**Files:**
- Create: `app/build/outputs/alignment-base.png` (and similar) — captured artifact, committed for the record

**Step 7.1: Build the debug APK**

```
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. APK at `app/build/outputs/apk/debug/app-debug.apk`.

**Step 7.2: Install on device**

```
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/
adb shell pm install -i com.android.shell -r /data/local/tmp/app-debug.apk
```

Expected: `Success`.

**Step 7.3: Capture panel screenshots via `chrome-devtools` MCP**

This is an Android app, not a browser — `chrome-devtools` MCP cannot reach it. Use `adb exec-out screencap` instead:

```
adb shell am start -n app.tastile.android/.MainActivity
sleep 2
adb shell input tap <open-quick-create-coordinates>
sleep 1
adb exec-out screencap -p > app/build/outputs/alignment-base.png
```

Tap coordinates for the "+ Create" entry point are device-specific; on a 1080×2400 Pixel-class device they're approximately (980, 220). Verify visually, retake if misaligned.

Repeat for three subpanels (Time / Recurring / Completion):
- Time: `adb exec-out screencap -p > app/build/outputs/alignment-subpanel-time.png`
- Recurring: ... (tap the Recurring row, then capture)
- Completion: ...

**Step 7.4: Visually verify alignment**

Open each PNG. Confirm:
- Title text x ≈ `Add task` text x ≈ `Behavior` text x ≈ first chip x.
- All baseline-aligned within ~4.dp tolerance (subpixel rendering variance).

If any are off, revert the offending task's commit, re-fix, and re-capture.

**Step 7.5: Commit artifacts + push**

```bash
git add app/build/outputs/alignment-*.png
git commit -m "test(quickcreate): capture alignment screenshots (visual confirmation)"
git push origin main
```

**Step 7.6: Open PR (if remote)**

If `tastile-android` has a remote, open a PR titled:
> "fix(quickcreate): align panel rows to M3 ListItem leading-column slot"

Description body: bullet summary of Cases A–F + screenshots + test command output.

---

## Verification summary

After all 7 tasks complete, the following must all be true:

1. `./gradlew :app:testDebugUnitTest` — PASS (PanelTokensTest + QuickCreateLeadingColumnAlignmentTest).
2. `./gradlew :app:compileDebugKotlin` — 0 errors.
3. `bash scripts/panel-leading-column-guard.sh` — exit 0.
4. APK installs cleanly, launches without crash.
5. Manual screenshot review: title text, `Add task`/`Add condition`/`References` text, chip-start, and ListItem text all share the same x within ~4.dp.

## Rollback

Single PR, single feature branch. `git revert <merge-sha>` restores prior layout. `PanelTokens.kt` is the only new source file; downstream impact is contained to `quickcreate/` package.

## Out-of-scope reminders

- Do NOT modify `MobileTokens` (separate design-system layer).
- Do NOT touch the panel outer `Column.padding(horizontal = 16.dp, vertical = 12.dp)` in `QuickCreateSubpanel` (line 157) — that is panel-edge inset, not a row.
- Do NOT touch `AppPickerButton` callsites — its internal layout is independent.
- Do NOT touch pre-existing uncommitted changes in `QuickCreateSheetMobile.kt`, `TimelineScreen.kt`, `AccountDropdownMenuTest.kt`, etc. — these belong to other in-progress work.