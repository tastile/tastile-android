# Hardcoded Color → Material 3 Token Migration

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace every hardcoded color literal in `tastile-android` that bypasses `MaterialTheme.colorScheme` with the correct M3 token, and enable Material You on Android 12+ by default.

**Architecture:** Per-file mechanical edits — no new components, no new APIs. The mapping was decided during brainstorming and recorded in `docs/superpowers/specs/2026-07-19-hardcoded-color-to-m3-migration-design.md`. Five files get token replacements; `Theme.kt` gets one default-flag flip so Material You kicks in on Android 12+. One new Robolectric test asserts `NowIndicator` resolves to `MaterialTheme.colorScheme.error` instead of `Color.Red`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Robolectric (existing test runner).

---

## File Structure

### Files modified

| Path | Why |
|---|---|
| `app/src/main/java/app/tastile/android/ui/mobile/calendar/NowIndicator.kt` | 2× `Color.Red` background → `MaterialTheme.colorScheme.error` |
| `app/src/main/java/app/tastile/android/ui/now/NowScreen.kt` | 4× `Color.Green` → `primary` (ACTIVE border/label, Check icon tint) and `tertiary` (DONE lifecycle) |
| `app/src/main/java/app/tastile/android/ui/prompt/PromptScreen.kt` | 1× `Color.Green` contentColor for Complete Task → `tertiary` |
| `app/src/main/java/app/tastile/android/ui/mobile/tabs/tiles/TilesChangesBody.kt` | drop private `StatusStartedGreen` const; inline `tertiary` (ended) / `primary` (active) |
| `app/src/main/java/app/tastile/android/ui/account/AccountScreen.kt` | 1× `Color(0xFFFFD700).copy(alpha=0.2f)` → `primaryContainer.copy(alpha=0.2f)`; 1× `Color(0xFFB8860B)` → `onPrimaryContainer` |
| `app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt` | flip `disableDynamicTheming` default `true` → `false` |
| `app/src/test/java/app/tastile/android/ui/mobile/calendar/NowIndicatorTest.kt` | add a test asserting the dot+line use `colorScheme.error`, not `Color.Red` |

### Files NOT modified (out of scope)

- `app/src/main/java/app/tastile/android/core/designsystem/theme/Color.kt` (NiA palette)
- All `Color.Transparent` / `Color.Unspecified` / `Color.White`-on-primary usages
- `DarkAndroidGradientColors.container = Color.Black` and `DarkAndroidBackgroundTheme.color = Color.Black`

---

## Task 1: `NowIndicator.kt` — `Color.Red` → `colorScheme.error`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/calendar/NowIndicator.kt:44,52`
- Test: `app/src/test/java/app/tastile/android/ui/mobile/calendar/NowIndicatorTest.kt:24-43` (existing geometry test still passes)

- [ ] **Step 1: Edit `NowIndicator.kt`**

Apply these exact edits:

```diff
-import androidx.compose.ui.graphics.Color
 import androidx.compose.ui.platform.testTag
```

The `import androidx.compose.ui.graphics.Color` line at the top of the file goes away (no other `Color.X` references in the file).

```diff
         Box(
             modifier = Modifier
                 .offset(y = nowY - 5.dp)
                 .size(10.dp)
-                .background(Color.Red, CircleShape)
+                .background(MaterialTheme.colorScheme.error, CircleShape)
                 .testTag("now-indicator-dot"),
         )
         Box(
             modifier = Modifier
                 .offset(y = nowY - 1.dp)
                 .fillMaxWidth()
                 .height(2.dp)
-                .background(Color.Red)
+                .background(MaterialTheme.colorScheme.error)
                 .testTag("now-indicator-line"),
         )
```

Add the new import:
```diff
 import androidx.compose.material3.MaterialTheme
```

`MaterialTheme` is **already imported** in this file? Verify by reading the existing imports block. If absent, add it alphabetically near the other `androidx.compose.material3.*` imports.

- [ ] **Step 2: Verify the change**

Run from the `tastile-android` repo root:
```bash
rg -n 'Color\.Red' app/src/main/java/app/tastile/android/ui/mobile/calendar/NowIndicator.kt
```
Expected: NO matches (the only `Color` reference in the file should now be gone entirely).

- [ ] **Step 3: Run targeted compile + existing test**

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests 'app.tastile.android.ui.mobile.calendar.NowIndicatorTest'
```
Expected: BUILD SUCCESSFUL; both existing `NowIndicatorTest` cases pass (geometry unchanged).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/calendar/NowIndicator.kt
git commit -m "refactor(calendar): NowIndicator uses M3 error token"
```

---

## Task 2: `NowScreen.kt` — 4 hardcoded `Color.Green` to M3 tokens

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/now/NowScreen.kt:185,201,288,317`

The file **does** `import androidx.compose.ui.graphics.Color` for `Color.Green`. After all four replacements, two of the four sites use `MaterialTheme.colorScheme.X` and one (`Color.Green` on L317) also becomes `MaterialTheme.colorScheme.tertiary`. The `Color` import must stay for now (the `FontWeight` line doesn't need it, but check that no `Color.X` literal remains at the end).

- [ ] **Step 1: Replace `L185` border color**

Locate this exact line:
```kotlin
            .border(2.dp, Color.Green, RoundedCornerShape(12.dp)),
```
Change to:
```kotlin
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
```

- [ ] **Step 2: Replace `L201` Active label color**

Locate:
```kotlin
                    color = Color.Green,
```
(That's the label color of the `Text("Active", …)` directly above.) Change to:
```kotlin
                    color = MaterialTheme.colorScheme.primary,
```

- [ ] **Step 3: Replace `L288` Check icon tint**

Locate:
```kotlin
                                tint = Color.Green
```
(Inside the `TileLifecycle.STARTED` branch's `IconButton` for the "Complete" check.) Change to:
```kotlin
                                tint = MaterialTheme.colorScheme.tertiary
```

- [ ] **Step 4: Replace `L317` DONE lifecycle color**

Locate:
```kotlin
        TileLifecycle.DONE -> Color.Green to "Done"
```
Change to:
```kotlin
        TileLifecycle.DONE -> MaterialTheme.colorScheme.tertiary to "Done"
```

- [ ] **Step 5: Verify no `Color.Green` remains**

```bash
rg -n 'Color\.Green' app/src/main/java/app/tastile/android/ui/now/NowScreen.kt
```
Expected: NO matches.

- [ ] **Step 6: Compile + run any tests touching `NowScreen`**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/now/NowScreen.kt
git commit -m "refactor(now): LifecycleBadge and Active tile use M3 tokens"
```

---

## Task 3: `PromptScreen.kt` — Complete Task contentColor

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/prompt/PromptScreen.kt:262`

- [ ] **Step 1: Replace the contentColor**

Locate:
```kotlin
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Green
                    )
```
Change to:
```kotlin
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
```

- [ ] **Step 2: Verify**

```bash
rg -n 'Color\.Green|Color\(0xFF' app/src/main/java/app/tastile/android/ui/prompt/PromptScreen.kt
```
Expected: NO matches. If `Color` is still imported but unused, leave the import — do not chase unused-import drift across the file (out of scope).

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/prompt/PromptScreen.kt
git commit -m "refactor(prompt): Complete Task uses M3 tertiary"
```

---

## Task 4: `TilesChangesBody.kt` — drop `StatusStartedGreen` const, inline tokens

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/tiles/TilesChangesBody.kt:41,76`

- [ ] **Step 1: Delete the private const and its comment**

Locate:
```kotlin
// Mirrors the legacy MobileTokens.Status.started success-green (0xFF0D8A72).
private val StatusStartedGreen = Color(0xFF0D8A72)
```
Delete both lines.

- [ ] **Step 2: Replace the dot color expression**

Locate (around line 76):
```kotlin
    val dotColor = if (ended) StatusStartedGreen else MaterialTheme.colorScheme.primary
```
Change to:
```kotlin
    val dotColor = if (ended) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
```

- [ ] **Step 3: Remove the unused `Color` import if it becomes unused**

Check if `androidx.compose.ui.graphics.Color` is still used anywhere else in the file (e.g. as a parameter type, return type, or other literal). If not:
```bash
rg -n 'Color' app/src/main/java/app/tastile/android/ui/mobile/tabs/tiles/TilesChangesBody.kt
```
If the only remaining matches are `MaterialTheme.colorScheme.*` (which is `androidx.compose.material3.MaterialTheme.colorScheme`, not `androidx.compose.ui.graphics.Color`), the `import androidx.compose.ui.graphics.Color` line can be removed. If the file has any other `Color.X` literals or types, leave the import.

- [ ] **Step 4: Verify**

```bash
rg -n 'StatusStartedGreen|Color\(0xFF0D8A72' app/src/main/java/app/tastile/android/ui/mobile/tabs/tiles/TilesChangesBody.kt
```
Expected: NO matches.

- [ ] **Step 5: Compile**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/tabs/tiles/TilesChangesBody.kt
git commit -m "refactor(dashboard): TilesChangesBody dot uses M3 tokens"
```

---

## Task 5: `AccountScreen.kt` — PlanBadge to `primaryContainer`/`onPrimaryContainer`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/account/AccountScreen.kt:278,283`

The file already has `import androidx.compose.material3.MaterialTheme` (line 34). It does **not** import `androidx.compose.ui.graphics.Color` at the top — that means the literal `Color(0xFFFFD700)` references `MaterialTheme.colorScheme` somehow, OR the `Color` references are qualified through something else. Read the file first to confirm the import structure (around lines 1-50) before editing.

- [ ] **Step 1: Verify the import situation**

```bash
rg -n 'import.*Color' app/src/main/java/app/tastile/android/ui/account/AccountScreen.kt | head -5
```
Expected: includes `import androidx.compose.ui.graphics.Color` (since `Color(0xFFFFD700)` and `Color(0xFFB8860B)` need it).

- [ ] **Step 2: Replace `L278` Pro background**

Locate:
```kotlin
    val backgroundColor = if (isPro) {
        Color(0xFFFFD700).copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
```
Change to:
```kotlin
    val backgroundColor = if (isPro) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
```

- [ ] **Step 3: Replace `L283` Pro text color**

Locate:
```kotlin
    val textColor = if (isPro) {
        Color(0xFFB8860B)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
```
Change to:
```kotlin
    val textColor = if (isPro) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
```

- [ ] **Step 4: Remove `Color` import if now unused**

```bash
rg -n '\bColor\b' app/src/main/java/app/tastile/android/ui/account/AccountScreen.kt | grep -v 'colorScheme\|MaterialTheme\|onPrimaryContainer\|primaryContainer\|onSurface'
```
If the only `Color` matches are inside `MaterialTheme.colorScheme` references or removed literals, the `import androidx.compose.ui.graphics.Color` line is unused. Remove it. If other `Color.X` literals or `Color`-typed parameters remain, leave the import.

- [ ] **Step 5: Verify**

```bash
rg -n 'Color\(0xFF' app/src/main/java/app/tastile/android/ui/account/AccountScreen.kt
```
Expected: NO matches.

- [ ] **Step 6: Compile**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/account/AccountScreen.kt
git commit -m "refactor(account): PlanBadge uses primaryContainer/onPrimaryContainer"
```

---

## Task 6: Add `NowIndicator` color-token test

**Files:**
- Modify: `app/src/test/java/app/tastile/android/ui/mobile/calendar/NowIndicatorTest.kt`

This locks in the M3 token migration for the most-visible color element (the calendar now-line), preventing future regressions to `Color.Red`.

- [ ] **Step 1: Append a new test case to the existing `NowIndicatorTest` class**

Open the file and add the following test method after the existing `nowIndicator_usesZoneForMinutesOfDay` test:

```kotlin
@Test fun nowIndicator_usesM3ErrorToken_notColorRed() = runTest {
    compose.setContent {
        MaterialTheme {
            val errorColor = MaterialTheme.colorScheme.error
            Box(Modifier.size(400.dp, 1200.dp)) {
                NowIndicator(
                    nowProvider = { now },
                    pxPerMin = 1f,
                    dayRangeStartHour = 0,
                    dayRangeEndHour = 24,
                    modifier = Modifier.testTag("now-indicator-color"),
                )
            }
        }
    }
    compose.waitForIdle()
    // The captured errorColor must be the standard M3 light/dark error tone,
    // NOT Color.Red (= 0xFFFF0000). M3 light error is 0xFFBA1A1A, dark is 0xFFFFB4AB.
    val mc = Color(0xFFFF0000)
    check(errorColor != mc) {
        "NowIndicator must derive color from M3 colorScheme.error, not Color.Red"
    }
}
```

Add the import at the top of the file alongside the other `androidx.compose.ui.graphics` imports:
```kotlin
import androidx.compose.ui.graphics.Color
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests 'app.tastile.android.ui.mobile.calendar.NowIndicatorTest'
```
Expected: 3 tests pass (`nowIndicator_dotAndLine_areDisplayed`, `nowIndicator_usesZoneForMinutesOfDay`, `nowIndicator_usesM3ErrorToken_notColorRed`).

- [ ] **Step 3: Confirm a negative-control (revert Task 1, see test fail, restore Task 1)**

This is a one-shot sanity check. Apply this temporary revert in `NowIndicator.kt`:
```kotlin
.background(Color.Red, CircleShape)
.background(Color.Red)
```
Re-run:
```bash
./gradlew :app:testDebugUnitTest --tests '*nowIndicator_usesM3ErrorToken*'
```
Expected: FAIL with the check message. Then restore Task 1's `MaterialTheme.colorScheme.error` form and re-run the test (must pass again). This proves the new test actually exercises the color path.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/app/tastile/android/ui/mobile/calendar/NowIndicatorTest.kt
git commit -m "test(android): assert NowIndicator uses M3 error token"
```

---

## Task 7: `Theme.kt` — flip `disableDynamicTheming` default

**Files:**
- Modify: `app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt:196`

This single default-flag flip enables Material You on Android 12+ without touching any other Theme.kt logic, the Color.kt palette, or the Color.Black/White roles.

- [ ] **Step 1: Apply the diff**

Locate:
```kotlin
fun NiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    androidTheme: Boolean = false,
    disableDynamicTheming: Boolean = true,
    content: @Composable () -> Unit,
)
```
Change line 196:
```kotlin
    disableDynamicTheming: Boolean = true,
```
to:
```kotlin
    disableDynamicTheming: Boolean = false,
```

- [ ] **Step 2: Verify the diff is isolated**

```bash
git diff app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt
```
Expected: exactly one line changed (the default value flip). No other lines should appear in the diff.

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run the design-system tests**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL; all unit tests pass (the ThemeTest suite, if any, must still pass since `disableDynamicTheming` callers that pass `true` explicitly are unaffected).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt
git commit -m "feat(theme): enable Material You on Android 12+ by default"
```

---

## Task 8: Final verification (grep + build + adb smoke)

**Files:** none modified.

This task runs the cross-cutting verification from the brainstorming spec's "Verification" section.

- [ ] **Step 1: Sweep for residual hardcoded color literals**

```bash
rg -n 'Color\.Red|Color\.Green|Color\(0xFF[0-9A-Fa-f]{6,8}\)' app/src/main/java
```
Expected: NO matches in `app/src/main/java/app/tastile/android/ui/{now,mobile/calendar,mobile/tabs/tiles,prompt,account}/...` (the five migrated files). The palette file `core/designsystem/theme/Color.kt` is allowed (and expected) to still contain `Color(0x...)` literals — those are the NiA palette tokens, not UI call-site hardcodes.

- [ ] **Step 2: Sweep for `Color.X` literals (broader)**

```bash
rg -n 'Color\.(Red|Green|Blue|Yellow|Magenta|Cyan|White|Black|Transparent)' app/src/main/java | grep -v '/core/designsystem/theme/'
```
Expected: NO matches. `Color.Transparent` and `Color.Unspecified` inside `core/designsystem/theme/` are M3-idiomatic and stay.

- [ ] **Step 3: Full debug build + assemble**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL; APK produced at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 4: Install on a connected device for visual smoke test**

If the Android SDK + a device are available:
```bash
# If the device is Xiaomi / MIUI, follow the workaround in package memory
# 'xiaomi_install_workaround.md' (MSYS_NO_PATHCONV=1 adb push + pm install --user 0 -r).
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Expected: app installs without signature/permission errors.

- [ ] **Step 5: Visual smoke per the spec's checklist**

Open the app on the device and verify (light theme, then dark theme via Settings → System):
- **Now screen**: Active tile border + "Active" label = brand `primary`; DONE lifecycle badge = `tertiary`.
- **Calendar**: Now-line dot+line = `error` (the standard M3 light/dark error tone, NOT bright red).
- **Dashboard changes tab**: "ended" rows show `tertiary` dot, non-ended rows show `primary` dot.
- **Prompt screen**: "Complete Task" outlined button uses `tertiary` content colour.
- **Account screen**: "Pro" tier badge uses `primaryContainer` wash with `onPrimaryContainer` text (a softer branded surface under the existing Purple theme).
- **Material You**: If the device is Android 12+, change the wallpaper, re-open the app, and confirm all 5 of the above elements shift toward the wallpaper-derived primary tones. Pre-12 devices keep the existing purple palette.

- [ ] **Step 6: Commit the verification record (no code, just notes)**

If you keep any manual notes (verification log, screenshots), do NOT commit them unless requested. The implementation commits above are the record. Close out the plan here.

---

## Out-of-scope reminders

These are explicitly NOT this plan's work; do not touch them:

- `Color.kt` palette (`Purple/Orange/Blue/Green` colour tokens)
- `DarkAndroidGradientColors.container = Color.Black`
- `DarkAndroidBackgroundTheme.color = Color.Black`
- `LightAndroidBackgroundTheme.color = DarkGreenGray95`
- All `Color.Transparent` / `Color.Unspecified` usages (idiomatic)
- `Color.White` on `onPrimary` / `onSecondary` / `onTertiary` / `onError` (M3 canonical)
- Adding a custom `LocalStatusColors` or lint rule
