# Android M3 Optimization — Phase M5 (Accessibility + Dynamic-Color Toggle) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every interactive wrapper screen-reader friendly and surface a user-facing Dynamic-Color toggle in Settings → Preferences → Appearance. Specifically:

1. Every interactive wrapper (in `ui/designsystem/` and `ui/mobile/designsystem/`) sets `Modifier.semantics { contentDescription = ...; role = Role.X }` and is `minimumInteractiveComponentSize()` compliant.
2. The current brand/gray color tokens are audited for WCAG AA contrast; any failing pair is shifted conservatively in the same PR.
3. `PreferencesRepository` exposes `dynamicColor: Boolean`; the toggle surfaces only on Android 12+.
4. Two new i18n keys: `preferences.dynamic_color.label`, `preferences.dynamic_color.description`.

**Architecture:** All wrapper annotation + the contrast audit land as one PR. The DC settings toggle + i18n + repository changes land as a second PR to keep the surface-area diff tight and reviewable.

**Tech Stack:** Kotlin 2.x, Compose Material3 BOM 2024.12.01, AGP 9.2.1, JUnit 4.13.2, Robolectric 4.14.

**Spec reference:** `tastile-android/docs/superpowers/specs/2026-07-16-tastile-android-m3-optimization-design.md` §7 (Phase M5).

**Parity reminder:** R1–R5 binding. This phase adds the *only* net new visible surface: the DC toggle. Two new i18n keys total.

---

## File structure

### New files

| Path | Responsibility |
|---|---|
| `app/src/test/java/app/tastile/android/ui/designsystem/MobileAccessibilityTest.kt` | Compose UI test asserting every wrapper exposes `contentDescription` and `Role` |
| `tastile-android/docs/specs/m5-contrast.md` | WCAG AA contrast table; computed from current `BrandColors` and gray values |

### Modified files

| Path | Change |
|---|---|
| `app/src/main/java/app/tastile/android/ui/designsystem/AppComponents.kt` | Per wrapper: `Modifier.semantics { contentDescription = ...; role = Role.X }` + `minimumInteractiveComponentSize()` where applicable |
| `app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileComponents.kt` | Same |
| `app/src/main/java/app/tastile/android/ui/designsystem/BrandColors.kt` | Conservative token shifts for any contrast failures (audit-driven) |
| `app/src/main/java/app/tastile/android/data/repository/PreferencesRepository.kt` | Add `dynamicColor: Boolean` (default `true`) |
| `app/src/main/java/app/tastile/android/ui/mobile/account/PreferencesSheet.kt` | Add `DynamicColor` toggle row, hidden on `< 31` |
| `app/src/main/res/values/strings.xml` | Add the two new keys |
| `app/src/main/java/app/tastile/android/ui/dashboard/DashboardViewModel.kt` | Surface `dynamicColor` to `TastileTheme(..., dynamicColor = ...)` via CompositionLocal |

---

## Tasks

### Task 1: WCAG AA contrast audit

**Files:**
- Create: `tastile-android/docs/specs/m5-contrast.md`

- [ ] **Step 1: Inventory M3 color pairs**

For every (surface, on-surface, role-1, role-2) pair in `BrandColors.kt`, render the actual RGBA in a known background and compute the contrast ratio. Pairs in scope:

```
primary  / onPrimary
primaryContainer / onPrimaryContainer
secondary / onSecondary
tertiary / onTertiary
error / onError
background / onBackground
surface / onSurface
surfaceVariant / onSurfaceVariant
outline / background
outline / surface
```

Repeat for the dark scheme and for the gray scheme (`grayColorScheme` defined in `Theme.kt`).

- [ ] **Step 2: Compute contrast**

Quick reference — contrast ratio formula:

```
(r1 + 0.05) / (r2 + 0.05)
```

where `r1 ≥ r2` and `r` is the relative luminance. Use a Kotlin test or a web-based checker (`webaim.org/resources/contrastchecker/`) to fill the table.

- [ ] **Step 3: Write `m5-contrast.md`**

```markdown
# Phase M5 Contrast Audit

Verified against current `BrandColors.kt` and `grayColorScheme` in `Theme.kt`.
All M3 role pairs checked for WCAG AA (4.5:1 normal, 3:1 large).

| Scheme | Pair | Foreground | Background | Ratio | AA verdict |
|---|---|---|---|---|---|
| brandLight | primary / onPrimary | … | … | 7.2 | PASS |
| brandLight | surface / onSurface | … | … | 13.5 | PASS |
| … | | | | | |
| brandDark | primary / onPrimary | … | … | 8.4 | PASS |
| grayLight | outline / surface | … | … | 2.9 | FAIL — shift |
| … | | | | | |
```

- [ ] **Step 4: For every FAIL, propose token shift**

Conservative shifts (no palette redesign): nudge `outline` darker by one M3 step, or `surfaceVariant` lighter by one M3 step. Anything that touches brand color (`primary`, `tertiary`, `error`) requires brand-team signoff in the PR.

- [ ] **Step 5: Commit audit (no fixes yet — fixes land in PR-2 of M5)**

```bash
git add -f tastile-android/docs/specs/m5-contrast.md
git commit -m "docs(android): M5 contrast audit baseline"
```

---

### Task 2: Apply conservative shifts for FAIL pairs

**Files:**
- Edit: `app/src/main/java/app/tastile/android/ui/designsystem/BrandColors.kt` (and `Theme.kt#grayColorScheme`)

- [ ] **Step 1: For each FAIL row in `m5-contrast.md`, apply the proposed shift**

```kotlin
// Before
val Outline = Color(0xFFD4D4D8)
// After (example: darker)
val Outline = Color(0xFFA1A1AA)
```

- [ ] **Step 2: Re-run the audit (re-compute the failed pairs) and append "FIXED" to the table**
- [ ] **Step 3: Run `./gradlew :app:assembleDebug :app:testDebugUnitTest`, expect green**
- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/
git commit -m "fix(android): M5 contrast shifts for FAILS in m5-contrast.md"
```

---

### Task 3: a11y semantics + minimum interactive component size on wrappers

**Files:**
- Edit: `app/src/main/java/app/tastile/android/ui/designsystem/AppComponents.kt`
- Edit: `app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileComponents.kt`

- [ ] **Step 1: Add semantics to every interactive wrapper**

For each interactive wrapper, ensure:

```kotlin
@Stable
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = Button(
    onClick = onClick,
    modifier = modifier
        .semantics { contentDescription = text; role = Role.Button }
        .minimumInteractiveComponentSize(),
    enabled = enabled,
) { Text(text) }
```

For `AppListItem`:

```kotlin
.semantics(mergeDescendants = true) {
    contentDescription = "$headline ${supporting.orEmpty()}".trim()
    role = Role.Button
}
```

For icon-only `AppIconButton` (add if not yet present):

```kotlin
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector,
) = IconButton(
    onClick = onClick,
    modifier = modifier
        .semantics { this.contentDescription = contentDescription; role = Role.Button }
        .minimumInteractiveComponentSize(),
    enabled = enabled,
) { Icon(icon, contentDescription = null) }
```

- [ ] **Step 2: Apply per wrapper family, one commit per family**

```bash
git add app/src/main/java/app/tastile/android/ui/designsystem/AppComponents.kt
git commit -m "feat(android): a11y semantics + min-touch on button wrappers"
git add app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileComponents.kt
git commit -m "feat(android): a11y semantics on mobile chips + pickers"
# …
```

- [ ] **Step 3: Run `./gradlew :app:lint :app:assembleDebug :app:testDebugUnitTest`, expect green**

---

### Task 4: `MobileAccessibilityTest` (Compose UI)

**Files:**
- Create: `app/src/test/java/app/tastile/android/ui/designsystem/MobileAccessibilityTest.kt`

- [ ] **Step 1: Test**

```kotlin
package app.tastile.android.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithRole
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import app.tastile.android.data.repository.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MobileAccessibilityTest {
    @get:Rule val rule = createComposeRule()

    @Test fun `AppPrimaryButton exposes role and contentDescription`() {
        rule.setContent {
            TastileTheme(themeMode = ThemeMode.BRAND, dynamicColor = false) {
                AppPrimaryButton(text = "Save", onClick = {})
            }
        }
        rule.onNodeWithText("Save").assertIsDisplayed()
        rule.onNodeWithRole(Role.Button).assertContentDescriptionEquals("Save")
    }

    // … per wrapper: AppListItem, AppPickerButton, AppSecondaryButton, AppTextButton,
    //    AppIconButton (if added in Task 3).
}
```

- [ ] **Step 2: Run, expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests '*MobileAccessibilityTest*'`
Expected: every wrapper test PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/app/tastile/android/ui/designsystem/MobileAccessibilityTest.kt
git commit -m "test(android): MobileAccessibilityTest enforces semantics + Role on every wrapper"
```

---

### Task 5: `PreferencesRepository` — add `dynamicColor`

**Files:**
- Edit: `app/src/main/java/app/tastile/android/data/repository/PreferencesRepository.kt`

- [ ] **Step 1: Read existing repo**

Locate where the `ThemeMode` and other preferences are stored (likely DataStore or SharedPreferences). Add a sibling `dynamicColor: Flow<Boolean>` defaulting to `true`.

```kotlin
val dynamicColor: Flow<Boolean> = dataStore.data
    .map { it[dynamicColorKey] ?: true }

companion object {
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
}
```

- [ ] **Step 2: Add a `setDynamicColor(enabled: Boolean)` method**

```kotlin
suspend fun setDynamicColor(enabled: Boolean) {
    dataStore.edit { prefs -> prefs[dynamicColorKey] = enabled }
}
```

- [ ] **Step 3: Run `./gradlew :app:testDebugUnitTest --tests '*PreferencesRepository*'`, expect green**
- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/tastile/android/data/repository/PreferencesRepository.kt
git commit -m "feat(android): PreferencesRepository.dynamicColor (default true)"
```

---

### Task 6: Settings toggle + i18n

**Files:**
- Edit: `app/src/main/res/values/strings.xml`
- Edit: `app/src/main/java/app/tastile/android/ui/mobile/account/PreferencesSheet.kt`
- Edit: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardViewModel.kt`

- [ ] **Step 1: Add i18n strings**

```xml
<string name="preferences_dynamic_color_label">System colors</string>
<string name="preferences_dynamic_color_description">Match the wallpaper on Android 12 and later.</string>
```

- [ ] **Step 2: Add `Switch` to `PreferencesSheet`**

```kotlin
@Composable
fun DynamicColorPreferenceRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return  // hidden on < 31
    ListItem(
        headlineContent = { Text(stringResource(R.string.preferences_dynamic_color_label)) },
        supportingContent = { Text(stringResource(R.string.preferences_dynamic_color_description)) },
        trailingContent = {
            Switch(checked = enabled, onCheckedChange = onToggle)
        },
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "Dynamic Color toggle"
            role = Role.Switch
        },
    )
}
```

- [ ] **Step 3: Wire to `DashboardViewModel`**

```kotlin
val dynamicColor: StateFlow<Boolean> = preferencesRepository.dynamicColor
    .stateIn(viewModelScope, SharingStarted.Eagerly, true)

fun setDynamicColor(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
}
```

- [ ] **Step 4: Surface in the root composable**

```kotlin
@Composable
fun Root(...) {
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    TastileTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        // …
    }
}
```

- [ ] **Step 5: Run `./gradlew :app:assembleDebug :app:testDebugUnitTest`, expect green**
- [ ] **Step 6: Manual verify**

Build, install, toggle the switch in Settings → Preferences → Appearance. Confirm UI re-tints immediately.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/ app/src/main/res/
git commit -m "feat(android): Dynamic Color toggle in Preferences (Android 12+)"
```

---

### Task 7: Open two PRs (M5 split into PR-1 = contrast + a11y, PR-2 = DC toggle)

- [ ] **Step 1: Push and open PR-1**

Title: `feat(android): accessibility pass (semantics + WCAG AA shifts)`
Body:

```
### Phase M5 (PR-1): Accessibility
- Per-wrapper Modifier.semantics + minimumInteractiveComponentSize.
- WCAG AA contrast shifts for any FAIL pairs in m5-contrast.md.
- MobileAccessibilityTest enforces semantics + Role on every wrapper.
- Parity safety: no new controls; no new i18n keys (PR-2).

Spec: docs/superpowers/specs/2026-07-16-tastile-android-m3-optimization-design.md §7
```

- [ ] **Step 2: After PR-1 merges, push PR-2**

Title: `feat(android): Dynamic Color toggle + i18n keys`
Body:

```
### Phase M5 (PR-2): Dynamic Color Toggle
- PreferencesRepository.dynamicColor (default true).
- Settings → Preferences → Appearance shows the toggle on Android 12+.
- Two new i18n keys: preferences.dynamic_color.{label,description}.
- Parity safety: this is the only net new visible surface added by M3 plan.

Spec: docs/superpowers/specs/2026-07-16-tastile-android-m3-optimization-design.md §7
```

---

## Completion KPI recap

- [ ] `m5-contrast.md` complete; all M3 color pairs AA verified.
- [ ] Every interactive wrapper annotated with `semantics { contentDescription, role }`.
- [ ] `MobileAccessibilityTest` PASS for every wrapper.
- [ ] `PreferencesRepository.dynamicColor` round-trips through disk; `setDynamicColor` callable.
- [ ] DC toggle visible only on Android 12+.
- [ ] Two new i18n keys present in `strings.xml`.
- [ ] Manual verification: DC toggle round-trip does not require process restart.
- [ ] Two PRs opened, both green.

---

## Out of scope for this plan

- Adding `values-ja/strings.xml` localization.
- Per-locale `values-<locale>/strings.xml` snapshots.
- More sophisticated TalkBack custom actions (the spec keeps this to the wrappers, not surface-level gesture exposition).
- Wear / TV / Foldable a11y — defer.
