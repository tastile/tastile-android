# Android UI Misuse Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace MUI anti-patterns in `tastile-android`'s mobile UI (`ui/mobile/**`) with Material 3 standard affordances, and plug the Web parity string leaks. Deliver a UI where every interactive element is identifiable by silhouette alone.

**Architecture:** Build a small set of shared components (`AppListItem`, `AppPickerButton`, `TimePickerSheet`, `DatePickerSheet`, `StatChip`, `SectionHeader`, `AppEmptyState`) on top of existing `MobileTokens`. Migrate each consumer file to those components. Add `MobileSpacing` tokens. Sweep ad-hoc `.dp` and `fontWeight` literals. No new dependencies.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (existing BOM), no new libraries.

**Reference:** Spec at `tastile-android/docs/superpowers/specs/2026-07-16-android-ui-misuse-redesign-design.md`. Read it before each task.

---

## File Structure

### New files (shared components)

| File | Responsibility |
| --- | --- |
| `mobile/designsystem/MobileSpacing.kt` | Spacing scale constants (`xxs..xxl`) |
| `mobile/designsystem/MobileComponents.kt` | `AppListItem`, `AppPickerButton`, `StatChip`, `SectionHeader`, `AppEmptyState`, `AppPrimaryButton`, `AppSecondaryButton`, `AppTertiaryButton` |

### New files (picker sheets)

| File | Responsibility |
| --- | --- |
| `mobile/components/picker/TimePickerSheet.kt` | M3 TimePicker wrapped in ModalBottomSheet |
| `mobile/components/picker/DatePickerSheet.kt` | M3 DatePicker wrapped in ModalBottomSheet |
| `mobile/components/picker/DurationPickerSheet.kt` | Hours/Minutes dial in ModalBottomSheet (replaces `DurationPickerDialog`) |
| `mobile/components/picker/ReferencePickerSheet.kt` | List of references with select handler |

### Modified files (consumers)

| File | Change |
| --- | --- |
| `mobile/MobileTopBar.kt` | ScaleDropdown uses `AppPickerButton` |
| `mobile/designsystem/MobileTokens.kt` | Re-export `MobileSpacing` (or move to dedicated file) |
| `mobile/sheets/quickcreate/QuickCreateBasePanel.kt` | 3 `Row.clickable{}` → `AppListItem` |
| `mobile/sheets/quickcreate/QuickCreateSubpanels.kt` | 5 `OutlinedTextField` → `AppPickerButton` + sheet |
| `mobile/panels/calendar/CalendarSectionContent.kt` | List items → `AppListItem`, header → `SectionHeader` |
| `mobile/panels/schedule/ScheduleSectionContent.kt` | Same |
| `mobile/panels/projects/ProjectsSectionContent.kt` | Same |
| `mobile/panels/references/ReferencesSectionContent.kt` | Same |
| `mobile/panels/preferences/PreferencesSectionContent.kt` | Same |
| `mobile/tabs/ExecuteScreen.kt` | "Next" → `AppListItem` + i18n |
| `mobile/tabs/TilesScreen.kt` | Stats wall → 3 `StatChip` + `SectionHeader` |
| `mobile/sheets/TileEditSheet.kt` | Button hierarchy + i18n |
| `mobile/sheets/AccountMenuSheet.kt` | Button hierarchy + i18n |
| `mobile/sheets/NotificationsSheet.kt` | i18n |
| `mobile/account/AccountSheet.kt` | Button hierarchy |
| `mobile/account/SubscriptionSheet.kt` | Button hierarchy |
| `mobile/account/TokensSheet.kt` | Button hierarchy |
| `app/src/main/res/values/strings.xml` | 6 new string entries |

### Untouched (out of scope)

- `ui/dashboard/**` (legacy desktop UI owns these)
- `MainActivity.kt`, `MobileNavGraph.kt` (no route changes)
- `tastile-brands/**` (no asset changes)

---

## Execution Strategy

Tasks are grouped into 8 phases. Within each phase, tasks are **independent** and can be dispatched to parallel subagents IF they touch different files. Tasks touching the same file are sequential.

| Phase | Tasks | Files touched |
| --- | --- | --- |
| 0 — Tokens | 0.1, 0.2 | `MobileTokens.kt`, `strings.xml` |
| 1 — Shared components | 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7 | New files in `mobile/designsystem/` and `mobile/components/picker/` |
| 2 — QuickCreate | 2.1, 2.2, 2.3 | 3 files in `mobile/sheets/quickcreate/` |
| 3 — Top bar | 3.1 | `MobileTopBar.kt` |
| 4 — Side panel sections | 4.1, 4.2, 4.3, 4.4, 4.5 | 5 files in `mobile/panels/` |
| 5 — Tabs | 5.1, 5.2 | `ExecuteScreen.kt`, `TilesScreen.kt` |
| 6 — Small sheets | 6.1, 6.2 | `TileEditSheet.kt`, `AccountMenuSheet.kt`, `NotificationsSheet.kt` + account/* |
| 7 — Sweep | 7.1 | All `mobile/**/*.kt` |
| 8 — Verify | 8.1, 8.2, 8.3 | Whole app |

**Concurrency rule:** dispatch up to 4 subagents in parallel when their tasks touch disjoint files. Sequencethe dependency: Phase 0 → Phase 1 → Phase 2-6 → Phase 7 → Phase 8.

**Branch:** stay on `2026-07-07-android-parity`. Commit per task.

---

## Phase 0 — Tokens

### Task 0.1: Add `MobileSpacing` tokens

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileTokens.kt:11-49`

- [ ] **Step 1: Add the `Spacing` object inside `MobileTokens`**

Open `MobileTokens.kt`. After the `object MobileTokens {` opening brace, before `val topBarHeight`, add:

```kotlin
    /**
     * Canonical spacing scale. Replace ad-hoc .dp literals (4/6/8/12/16/24)
     * with these constants so the UI speaks one vocabulary.
     */
    object Spacing {
        val xxs = 2.dp
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
    }
```

- [ ] **Step 2: Verify the file compiles**

Run: `cd app && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL with no new errors.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileTokens.kt
git commit -m "feat(android): add MobileSpacing token scale"
```

### Task 0.2: Add new `R.string.*` resources for parity strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Append the new entries**

Add at the end of `strings.xml`, just before `</resources>`:

```xml
    <!-- MUI Redesign — parity strings (mobile UI) -->
    <string name="empty_tiles_title">No tiles yet</string>
    <string name="empty_tiles_hint">Create a tile from the Projects tab to seed the timeline.</string>
    <string name="empty_projects_title">No projects yet</string>
    <string name="empty_projects_hint">Add a project to start organising tiles.</string>
    <string name="empty_schedule_title">Nothing scheduled</string>
    <string name="empty_schedule_hint">Quick-create a tile or pick a time window to begin.</string>
    <string name="empty_blocks_title">No blocks in this view</string>
    <string name="empty_blocks_hint">Create a tile to seed the timeline.</string>
    <string name="tile_occurrence_label">Occurrence: %1$s</string>
    <string name="execute_next_label">Next: %1$s</string>
    <string name="tiles_stat_open">Open · %1$d</string>
    <string name="tiles_stat_estimated">Estimated · %1$dm</string>
    <string name="tiles_stat_sections">Sections · %1$d</string>
    <string name="picker_time_start">Start time</string>
    <string name="picker_time_end">End time</string>
    <string name="picker_date_start">Start date</string>
    <string name="picker_date_end">End date</string>
    <string name="picker_duration_label">Duration</string>
    <string name="picker_reference_label">Reference</string>
    <string name="common_confirm">Confirm</string>
    <string name="common_cancel">Cancel</string>
    <string name="common_dismiss">Dismiss</string>
```

Note: keys are kept stable (no rename of existing keys, per parity contract).

- [ ] **Step 2: Verify**

Run: `cd app && ./gradlew processDebugResources 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat(android): add MUI redesign i18n keys"
```

---

## Phase 1 — Shared components

### Task 1.1: Create `AppListItem` + button helpers in `MobileComponents.kt`

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileComponents.kt`

- [ ] **Step 1: Create the file with all component signatures**

```kotlin
package app.tastile.android.ui.mobile.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * M3 ListItem wrapped for tappable rows. Replaces ad-hoc `Row.clickable{}`
 * patterns so every interactive row carries M3's built-in ripple, min-height
 * (56.dp single / 72.dp two-line), and shape.
 */
@Composable
fun AppListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailing: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    ListItem(
        headlineContent = { Text(headline, style = MaterialTheme.typography.bodyLarge) },
        modifier = if (onClick != null) modifier.then(Modifier.padding(0.dp)) else modifier,
        supportingContent = supporting?.let { { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        leadingContent = leadingContent ?: leading?.let { { Icon(it, contentDescription = null) } },
        trailingContent = trailingContent ?: trailing?.let { { Icon(it, contentDescription = null) } },
        colors = if (selected) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else ListItemDefaults.colors(),
        tonalElevation = if (onClick != null) 1.dp else 0.dp,
    )
}

/** Primary CTA. Use at most once per surface. */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = Button(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }

/** Secondary CTA. Important secondary actions. */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = FilledTonalButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }

/** Tertiary CTA. Cancellation, alternate actions. */
@Composable
fun AppTertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }

/** Quaternary CTA. Dismiss / "learn more" only. */
@Composable
fun AppDismissButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = TextButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }
```

- [ ] **Step 2: Verify compile**

Run: `cd app && ./gradlew compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileComponents.kt
git commit -m "feat(android): add AppListItem and App*Button helpers"
```

### Task 1.2: Create `AppPickerButton` in `MobileComponents.kt`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileComponents.kt`

- [ ] **Step 1: Append `AppPickerButton` + `SectionHeader` + `StatChip` + `AppEmptyState`**

Add at the end of `MobileComponents.kt`:

```kotlin
// ----- Imports to add at top of file -----
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Button-shaped input. Replaces OutlinedTextField for constrained values
 * (time / date / reference). Click opens a picker sheet via [onClick].
 */
@Composable
fun AppPickerButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Box(Modifier.size(8.dp))
                }
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
    }
}

/** Section header with divider underline. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = MobileSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(modifier = Modifier.padding(top = MobileSpacing.xs))
    }
}

/** Coloured stat chip — replaces wall-of-text summary strings. */
@Composable
fun StatChip(
    label: String,
    value: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = {},
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = foreground)
                Text(" · ", color = foreground)
                Text(value, style = MaterialTheme.typography.labelLarge, color = foreground)
            }
        },
        colors = AssistChipDefaults.assistChipColors(containerColor = background, labelColor = foreground),
        modifier = modifier,
    )
}

/** Empty state — icon + title + hint. */
@Composable
fun AppEmptyState(
    icon: ImageVector,
    titleRes: Int,
    hintRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(MobileSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Box(Modifier.size(MobileSpacing.md))
        Text(
            androidx.compose.ui.res.stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(Modifier.size(MobileSpacing.xs))
        Text(
            androidx.compose.ui.res.stringResource(hintRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `cd app && ./gradlew compileDebugKotlin 2>&1 | tail -30`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileComponents.kt
git commit -m "feat(android): add AppPickerButton, SectionHeader, StatChip, AppEmptyState"
```

### Task 1.3: Create `TimePickerSheet`

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/components/picker/TimePickerSheet.kt`

- [ ] **Step 1: Create the picker sheet**

```kotlin
package app.tastile.android.ui.mobile.components.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
import app.tastile.android.ui.mobile.designsystem.MobileSpacing
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Time picker in a bottom sheet. Pass the current value in via [initial]; on
 * confirm the new LocalTime is delivered to [onConfirm]. On dismiss, [onDismiss]
 * is called (sheet also handles back press via the system gesture).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    titleRes: Int = R.string.picker_duration_label,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MobileSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MobileSpacing.md),
        ) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            TimePicker(
                hour = hour,
                minute = minute,
                onHourChange = { hour = it },
                onMinuteChange = { minute = it },
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MobileSpacing.sm),
            ) {
                AppTertiaryButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = {
                        scope.launch { sheetState.hide(); onDismiss() }
                    },
                    modifier = Modifier.weight(1f),
                )
                AppPrimaryButton(
                    text = stringResource(R.string.common_confirm),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onConfirm(LocalTime.of(hour, minute))
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `cd app && ./gradlew compileDebugKotlin 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL. If `TimePicker` complains about `@ExperimentalMaterial3Api`, the `@OptIn` annotation on the function should already cover it. If still flagged, also add `@OptIn` to the `TimePicker` call site.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/components/picker/TimePickerSheet.kt
git commit -m "feat(android): add TimePickerSheet"
```

### Task 1.4: Create `DatePickerSheet`

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/components/picker/DatePickerSheet.kt`

- [ ] **Step 1: Create the date picker sheet**

```kotlin
package app.tastile.android.ui.mobile.components.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
import app.tastile.android.ui.mobile.designsystem.MobileSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    titleRes: Int = R.string.picker_date_start,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppPrimaryButton(
                text = stringResource(R.string.common_confirm),
                onClick = {
                    val millis = state.selectedDateMillis ?: return@AppPrimaryButton
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    onConfirm(date)
                },
            )
        },
        dismissButton = {
            AppTertiaryButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
            )
        },
    ) {
        Column(modifier = Modifier.padding(MobileSpacing.md)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        }
        DatePicker(state = state)
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `cd app && ./gradlew compileDebugKotlin 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/components/picker/DatePickerSheet.kt
git commit -m "feat(android): add DatePickerSheet"
```

### Task 1.5: Create `DurationPickerSheet`

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/components/picker/DurationPickerSheet.kt`

- [ ] **Step 1: Create the duration picker sheet**

```kotlin
package app.tastile.android.ui.mobile.components.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.dashboard.components.DurationPickerDialog
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
import app.tastile.android.ui.mobile.designsystem.MobileSpacing
import kotlinx.coroutines.launch

/**
 * Wrapper around the existing [DurationPickerDialog] but presented in a
 * ModalBottomSheet instead of an AlertDialog. Returns total minutes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationPickerSheet(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val initialHours = initialMinutes / 60
    val initialMins = initialMinutes % 60
    var hours by remember { mutableIntStateOf(initialHours) }
    var minutes by remember { mutableIntStateOf(initialMins) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MobileSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MobileSpacing.md),
        ) {
            Text(stringResource(R.string.picker_duration_label), style = MaterialTheme.typography.titleMedium)
            DurationPickerDialog.Picker(hours = hours, minutes = minutes, onHoursChange = { hours = it }, onMinutesChange = { minutes = it })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MobileSpacing.sm),
            ) {
                AppTertiaryButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = { scope.launch { sheetState.hide(); onDismiss() } },
                    modifier = Modifier.weight(1f),
                )
                AppPrimaryButton(
                    text = stringResource(R.string.common_confirm),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onConfirm(hours * 60 + minutes)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
```

NOTE: If `DurationPickerDialog.Picker` is not a public composable in the existing `DurationPickerDialog.kt`, refactor that file's internal picker composable to be `internal` (or extract it into a separate file `DurationPickerContent.kt`) and re-export from the sheet. Read `app/src/main/java/app/tastile/android/ui/dashboard/components/DurationPickerDialog.kt` first to understand the structure.

- [ ] **Step 2: Verify compile**

Run: `cd app && ./gradlew compileDebugKotlin 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL (or one refactor of DurationPickerDialog).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/components/picker/DurationPickerSheet.kt
git commit -m "feat(android): add DurationPickerSheet"
```

### Task 1.6: Create `ReferencePickerSheet`

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/components/picker/ReferencePickerSheet.kt`

- [ ] **Step 1: Create the reference picker sheet**

```kotlin
package app.tastile.android.ui.mobile.components.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.ui.mobile.designsystem.AppListItem
import app.tastile.android.ui.mobile.designsystem.MobileSpacing
import app.tastile.android.ui.mobile.panels.references.ReferencesLabelList

/**
 * Reference picker sheet. Lists references from [ReferencesLabelList] and
 * delivers the chosen reference id/label back to the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferencePickerSheet(
    references: List<ReferenceOption>,
    onSelect: (ReferenceOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MobileSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
        ) {
            Text(stringResource(R.string.picker_reference_label), style = MaterialTheme.typography.titleMedium)
            if (references.isEmpty()) {
                Text(stringResource(R.string.empty_projects_title), style = MaterialTheme.typography.bodyMedium)
            } else {
                references.forEach { ref ->
                    AppListItem(
                        headline = ref.label,
                        supporting = ref.id,
                        leading = Icons.Outlined.Tag,
                        onClick = { onSelect(ref) },
                    )
                }
            }
        }
    }
}

data class ReferenceOption(val id: String, val label: String)
```

NOTE: `ReferencesLabelList` is the existing list component; if its public API does not match, just enumerate `references` directly (the component receives a list of `ReferenceOption` already).

- [ ] **Step 2: Verify compile**

Run: `cd app && ./gradlew compileDebugKotlin 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/components/picker/ReferencePickerSheet.kt
git commit -m "feat(android): add ReferencePickerSheet"
```

### Task 1.7: Verify shared components compile + screenshot test

**Files:**
- (No new files)

- [ ] **Step 1: Build full debug APK**

Run: `cd app && ./gradlew assembleDebug 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL. All Phase 0 + Phase 1 components compile.

- [ ] **Step 2: Install + verify components exist**

Run (using MIUI workaround):
```bash
MSYS_NO_PATHCONV=1 adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/tastile.apk
MSYS_NO_PATHCONV=1 adb shell pm install -i com.miui.global.packageinstaller -r /data/local/tmp/tastile.apk
MSYS_NO_PATHCONV=1 adb shell rm /data/local/tmp/tastile.apk
```
Expected: `Success`. App opens without crash. Components are unused so no visible UI change yet — that's expected.

- [ ] **Step 3: Commit a marker (only if any change was needed)**

If any fixes were needed in Phase 0/1, commit:
```bash
git add -A
git commit -m "chore(android): phase 1 components verified, build clean"
```

If no changes, skip this step.

---

## Phase 2 — QuickCreate sheet (the worst offender)

### Task 2.1: Refactor `QuickCreateBasePanel.kt` — Row.clickable → AppListItem

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt:95,100,125`

- [ ] **Step 1: Read the file**

Run: `read_file` and identify the three `Row(... .clickable{})` blocks at lines 95, 100, 125.

- [ ] **Step 2: Add imports**

At the top of the file, add:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import app.tastile.android.ui.mobile.designsystem.AppListItem
```

(The exact icons depend on what the row currently represents — pick the closest semantic match.)

- [ ] **Step 3: Replace each `Row(... .clickable{})` with `AppListItem`**

Replace each `Row(modifier = Modifier.fillMaxWidth().clickable { store.openSubpanel(...) }, horizontalArrangement = Arrangement.SpaceBetween) { ... }` block with:

```kotlin
AppListItem(
    headline = "...",          // existing title text
    supporting = "...",        // existing supporting text, if any
    leading = Icons.Outlined.X, // semantic icon (Tune, Schedule, Checklist, etc.)
    trailing = Icons.Outlined.ArrowForward,
    onClick = { store.openSubpanel(...) },
)
```

Preserve all `testTag`s — add `Modifier.semantics { testTagsAsResourceId = true }` if needed to keep them working. The `testTag` modifier should remain on the row:

```kotlin
AppListItem(
    modifier = Modifier.semantics { testTagsAsResourceId = true }.testTag("quick-create-condition-card"),
    ...
)
```

If `Modifier.testTag` cannot attach directly to `AppListItem`, wrap it: `Box(Modifier.testTag("...")) { AppListItem(...) }`.

- [ ] **Step 4: Verify compile + UI**

Run: `cd app && ./gradlew assembleDebug 2>&1 | tail -15`
Then install with MIUI workaround and open QuickCreate sheet. The three trigger rows should look like proper ListItems (with leading icon + trailing chevron) and be obviously tappable.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt
git commit -m "refactor(android): QuickCreate subpanel triggers use AppListItem"
```

### Task 2.2: Refactor `QuickCreateSubpanels.kt` — TextField → AppPickerButton

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubpanels.kt:111-135`

- [ ] **Step 1: Read the file**

Identify the 5 `OutlinedTextField` blocks (reference ID, reference label, timeOfDayStart, timeOfDayEnd, window bounds start, window bounds end). Each parses/serializes strings (`HH:mm` for time, `yyyy-MM-dd` for date).

- [ ] **Step 2: Add imports**

At the top:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.ui.mobile.components.picker.DatePickerSheet
import app.tastile.android.ui.mobile.components.picker.ReferencePickerSheet
import app.tastile.android.ui.mobile.components.picker.ReferenceOption
import app.tastile.android.ui.mobile.components.picker.TimePickerSheet
import app.tastile.android.ui.mobile.designsystem.AppPickerButton
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
```

- [ ] **Step 3: Add `showPicker` state holder inside the composable**

At the top of the composable that owns these fields, add:

```kotlin
var showStartTime by remember { mutableStateOf(false) }
var showEndTime by remember { mutableStateOf(false) }
var showStartDate by remember { mutableStateOf(false) }
var showEndDate by remember { mutableStateOf(false) }
var showReferencePicker by remember { mutableStateOf(false) }
val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
val dateFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
```

- [ ] **Step 4: Replace each `OutlinedTextField` with `AppPickerButton`**

Example pattern:

```kotlin
// Before:
OutlinedTextField(draft.time.timeOfDayStart, { value -> store.updateTime(draft.time.copy(timeOfDayStart = value)) }, label = { Text("Start time") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-time-of-day-start"))

// After:
AppPickerButton(
    label = stringResource(R.string.picker_time_start),
    value = draft.time.timeOfDayStart.ifBlank { "--:--" },
    onClick = { showStartTime = true },
    leadingIcon = Icons.Outlined.AccessTime,
    modifier = Modifier.fillMaxWidth().testTag("quick-create-time-of-day-start"),
)

if (showStartTime) {
    TimePickerSheet(
        initial = runCatching { LocalTime.parse(draft.time.timeOfDayStart, timeFmt) }.getOrElse { LocalTime.of(9, 0) },
        onConfirm = { time ->
            store.updateTime(draft.time.copy(timeOfDayStart = time.format(timeFmt)))
            showStartTime = false
        },
        onDismiss = { showStartTime = false },
    )
}
```

Apply the same pattern to:
- `timeOfDayEnd` (TimePickerSheet)
- `window.bounds.start` / `window.bounds.end` (DatePickerSheet)
- `window.referenceId` (ReferencePickerSheet)
- `draft.time.referenceId` (ReferencePickerSheet)

Keep `draft.time.referenceLabel` as `OutlinedTextField` (free-form text the user types).

- [ ] **Step 5: Verify compile + UI**

Run: `cd app && ./gradlew assembleDebug 2>&1 | tail -15`
Install and open QuickCreate → Time subpanel. Each previously-text input should now be a button. Tap one — picker sheet opens. Confirm — value updates. Cancel — sheet closes without change.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubpanels.kt
git commit -m "refactor(android): QuickCreate time/date/reference inputs use pickers"
```

### Task 2.3: QuickCreate footer button hierarchy

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt` (footer)

- [ ] **Step 1: Locate footer Save / Cancel buttons**

Look for `TextButton(onClick = ...)` patterns near the bottom of the file.

- [ ] **Step 2: Apply button hierarchy**

Replace:
- Primary "Save" / "Create" → `AppPrimaryButton`
- "Cancel" → `AppTertiaryButton`
- "Dismiss" (close X) → `AppDismissButton`

If a `TextButton` is the primary CTA anywhere in QuickCreate, switch it.

- [ ] **Step 3: Verify + commit**

Run: `cd app && ./gradlew assembleDebug 2>&1 | tail -10`
Install + open QuickCreate. The Save button should be visually distinct (filled primary color) vs Cancel (outlined).
```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt
git commit -m "refactor(android): QuickCreate footer uses M3 button hierarchy"
```

---

## Phase 3 — Top bar

### Task 3.1: `MobileTopBar` ScaleDropdown uses `AppPickerButton`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt:114-164`

- [ ] **Step 1: Read the ScaleDropdown composable**

Currently a custom `Row.clip(pillShape).border().clickable()`.

- [ ] **Step 2: Replace with `AppPickerButton` (compact variant)**

Add a compact `AppPickerButton` overload to `MobileComponents.kt`:

```kotlin
@Composable
fun AppPickerButtonCompact(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
```

(Note: this requires `import androidx.compose.material3.Surface` and `import androidx.compose.foundation.BorderStroke`.)

In `MobileTopBar.kt`, replace the custom `ScaleDropdown` body with:

```kotlin
AppPickerButtonCompact(
    label = scale.name,
    onClick = { expanded = true },
    modifier = Modifier.semantics { contentDescription = "Scale: ${scale.name}" },
)
// existing DropdownMenu follows unchanged
```

- [ ] **Step 3: Verify + commit**

Run: `cd app && ./gradlew assembleDebug 2>&1 | tail -10`
Install and open the app. The scale pill in the top bar should have a visible ripple when tapped (verify by tapping and watching the highlight).
```bash
git add app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileComponents.kt
git commit -m "refactor(android): ScaleDropdown uses AppPickerButtonCompact (with ripple)"
```

---

## Phase 4 — Side panel sections

These tasks are independent (5 different files). Dispatch in parallel.

### Task 4.1: Calendar section uses `AppListItem` + `SectionHeader`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/panels/CalendarSectionContent.kt` (or whatever file currently owns the Calendar panel content — confirm by reading)

- [ ] **Step 1: Read the file**

Find all `Row(... .clickable{})` items and the section title text.

- [ ] **Step 2: Replace section title with `SectionHeader`**

```kotlin
SectionHeader(title = stringResource(R.string.section_calendar_title))  // add this string key in a follow-up if missing
```

If no existing string key, hardcode the title for now (e.g., `"Calendar"`) and add an i18n TODO comment.

- [ ] **Step 3: Replace each item with `AppListItem`**

```kotlin
AppListItem(
    headline = "Today",
    leading = Icons.Outlined.Today,
    onClick = { /* existing onClick */ },
)
```

- [ ] **Step 4: Verify + commit**

```bash
cd app && ./gradlew assembleDebug 2>&1 | tail -10
git add app/src/main/java/app/tastile/android/ui/mobile/panels/CalendarSectionContent.kt
git commit -m "refactor(android): Calendar section uses AppListItem + SectionHeader"
```

### Task 4.2: Schedule section

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/panels/schedule/ScheduleSectionContent.kt` (or read to find the right file)

- [ ] **Steps 1-4: same pattern as Task 4.1, with appropriate icons (Icons.Outlined.Schedule, etc.)**

Commit: `refactor(android): Schedule section uses AppListItem + SectionHeader`

### Task 4.3: Projects section

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/panels/projects/ProjectsSectionContent.kt`

- [ ] **Steps 1-4: same pattern; replace "No projects yet" with `AppEmptyState(icon = Icons.Outlined.FolderOff, titleRes = R.string.empty_projects_title, hintRes = R.string.empty_projects_hint)`**

Commit: `refactor(android): Projects section uses AppListItem + SectionHeader + empty state`

### Task 4.4: References section

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/panels/references/ReferencesSectionContent.kt`

- [ ] **Steps 1-4: same pattern**

Commit: `refactor(android): References section uses AppListItem + SectionHeader`

### Task 4.5: Preferences section

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/panels/preferences/PreferencesSectionContent.kt` (or wherever preferences panel content lives)

- [ ] **Steps 1-4: same pattern**

Commit: `refactor(android): Preferences section uses AppListItem + SectionHeader`

---

## Phase 5 — Tabs

### Task 5.1: ExecuteScreen — "Next" line + i18n

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/ExecuteScreen.kt:164`

- [ ] **Step 1: Find the "Next: $next" line**

Currently: `text = "Next: $next"`

- [ ] **Step 2: Replace with `AppListItem` + i18n**

```kotlin
AppListItem(
    headline = stringResource(R.string.execute_next_label, next.toString()),
    leading = Icons.Outlined.PlayArrow,
    onClick = { /* existing handler */ },
)
```

- [ ] **Step 3: Verify + commit**

```bash
cd app && ./gradlew assembleDebug 2>&1 | tail -10
git add app/src/main/java/app/tastile/android/ui/mobile/tabs/ExecuteScreen.kt
git commit -m "refactor(android): ExecuteScreen 'Next' uses AppListItem + i18n"
```

### Task 5.2: TilesScreen — stats wall → StatChips

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TilesScreen.kt:170`

- [ ] **Step 1: Find the stats text**

Currently: `text = "Open: $tilesCount · Estimated: ${tilesCount * 30}m · Sections: ${grouped.size}"`

- [ ] **Step 2: Replace with three `StatChip`s in a Row**

```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(MobileSpacing.md),
    horizontalArrangement = Arrangement.spacedBy(MobileSpacing.sm),
) {
    StatChip(
        label = stringResource(R.string.tiles_stat_open_label),
        value = tilesCount.toString(),
        background = MaterialTheme.colorScheme.secondaryContainer,
        foreground = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    StatChip(
        label = stringResource(R.string.tiles_stat_estimated_label),
        value = "${tilesCount * 30}",
        background = MaterialTheme.colorScheme.tertiaryContainer,
        foreground = MaterialTheme.colorScheme.onTertiaryContainer,
    )
    StatChip(
        label = stringResource(R.string.tiles_stat_sections_label),
        value = grouped.size.toString(),
        background = MaterialTheme.colorScheme.surfaceVariant,
        foreground = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
```

Add label-only string keys: `tiles_stat_open_label` = `"Open"`, etc. (in strings.xml).

- [ ] **Step 3: Replace project rows with `AppListItem`**

If the screen lists projects as `Row.clickable { ... }`, replace each with `AppListItem` (leading project icon, headline = project name, supporting = estimate, trailing = chevron).

- [ ] **Step 4: Verify + commit**

```bash
cd app && ./gradlew assembleDebug 2>&1 | tail -10
git add app/src/main/java/app/tastile/android/ui/mobile/tabs/TilesScreen.kt app/src/main/res/values/strings.xml
git commit -m "refactor(android): TilesScreen stats use StatChips, rows use AppListItem"
```

---

## Phase 6 — Small sheets

### Task 6.1: TileEditSheet, AccountMenuSheet, NotificationsSheet

**Files:**
- Modify:
  - `app/src/main/java/app/tastile/android/ui/mobile/sheets/TileEditSheet.kt`
  - `app/src/main/java/app/tastile/android/ui/mobile/sheets/AccountMenuSheet.kt`
  - `app/src/main/java/app/tastile/android/ui/mobile/sheets/NotificationsSheet.kt`

- [ ] **Step 1: Apply button hierarchy to each**

For each sheet, replace `TextButton` with the appropriate `AppPrimaryButton` / `AppSecondaryButton` / `AppTertiaryButton` / `AppDismissButton`.

- [ ] **Step 2: Replace hardcoded strings**

In `TileEditSheet.kt:76`, replace `"Occurrence: $placementId"` with:
```kotlin
Text(stringResource(R.string.tile_occurrence_label, placementId))
```

- [ ] **Step 3: Replace `Row.clickable` with `AppListItem`** (if any in these files)

- [ ] **Step 4: Verify + commit**

```bash
cd app && ./gradlew assembleDebug 2>&1 | tail -10
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/TileEditSheet.kt app/src/main/java/app/tastile/android/ui/mobile/sheets/AccountMenuSheet.kt app/src/main/java/app/tastile/android/ui/mobile/sheets/NotificationsSheet.kt
git commit -m "refactor(android): small sheets use M3 button hierarchy + i18n"
```

### Task 6.2: AccountSheet, SubscriptionSheet, TokensSheet

**Files:**
- Modify:
  - `app/src/main/java/app/tastile/android/ui/mobile/account/AccountSheet.kt`
  - `app/src/main/java/app/tastile/android/ui/mobile/account/SubscriptionSheet.kt`
  - `app/src/main/java/app/tastile/android/ui/mobile/account/TokensSheet.kt`

- [ ] **Step 1-4: same pattern as Task 6.1**

Commit: `refactor(android): account sheets use M3 button hierarchy + i18n`

---

## Phase 7 — Sweep

### Task 7.1: Sweep ad-hoc `.dp` and `fontWeight` literals

**Files:**
- All of `app/src/main/java/app/tastile/android/ui/mobile/**/*.kt`

- [ ] **Step 1: Grep for remaining literals**

```bash
rg 'fontWeight\s*=' app/src/main/java/app/tastile/android/ui/mobile/
rg -E '\b(2|4|6|8|10|12|14|16|20|24|28|32)\.dp' app/src/main/java/app/tastile/android/ui/mobile/ -t kotlin
```

For each occurrence:
- If it's spacing (padding, Spacer, Arrangement.spacedBy, vertical/horizontal arrangement) → migrate to `MobileSpacing.*`.
- If it's decorative (icon size, sheet corner radius, hit target) → leave as-is.

For each `fontWeight = FontWeight.X`:
- If it's overriding an existing M3 typography role's weight → delete the override (let M3 decide).
- If it's on a custom text that genuinely needs the weight → migrate to use a specific M3 typography role that has that weight built in.

- [ ] **Step 2: Verify zero remaining literals**

```bash
rg 'fontWeight\s*=' app/src/main/java/app/tastile/android/ui/mobile/ -t kotlin | wc -l
# expected: 0
rg -E '\b(4|6|8|12)\.dp' app/src/main/java/app/tastile/android/ui/mobile/ -t kotlin | wc -l
# expected: only decorative sizes (icon, sheet corner) remain — manual review
```

- [ ] **Step 3: Build + commit**

```bash
cd app && ./gradlew assembleDebug 2>&1 | tail -10
git add app/src/main/java/app/tastile/android/ui/mobile/
git commit -m "refactor(android): migrate ad-hoc dp and fontWeight to tokens"
```

---

## Phase 8 — Verify

### Task 8.1: Build debug APK

- [ ] **Step 1: Clean build**

```bash
cd app && ./gradlew clean assembleDebug 2>&1 | tail -20
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Spot-check APK is up to date**

```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
```
Expected: file size roughly matches the previous build (~30 MB).

### Task 8.2: Install + visual verification

- [ ] **Step 1: Install with MIUI workaround**

```bash
MSYS_NO_PATHCONV=1 adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/tastile.apk
MSYS_NO_PATHCONV=1 adb shell pm install -i com.miui.global.packageinstaller -r /data/local/tmp/tastile.apk
MSYS_NO_PATHCONV=1 adb shell rm /data/local/tmp/tastile.apk
```
Expected: `Success`.

- [ ] **Step 2: Launch and screenshot each surface**

For each of: Timeline, Tasks, Projects, References, Preferences, AccountMenu, Notifications, QuickCreate (open + each subpanel), SidePanel (open each section).

Use `mcp__chrome-devtools__navigate_page` (or take_screenshot via the chrome-devtools MCP) — but since this is Android, use `adb shell screencap` and pull:

```bash
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png /tmp/screen.png
```

Verify each surface matches the acceptance criteria in the spec (§5).

- [ ] **Step 3: Capture before/after comparison**

Save the screenshots to `docs/superpowers/specs/2026-07-16-android-ui-misuse-redesign-screenshots/` for reference.

### Task 8.3: Run existing tests

- [ ] **Step 1: Run unit + instrumentation tests**

```bash
cd app && ./gradlew testDebugUnitTest connectedDebugAndroidTest 2>&1 | tail -20
```
Expected: all tests pass. If any fail, fix before proceeding.

- [ ] **Step 2: Final commit (if any fix needed)**

```bash
git add -A
git commit -m "test(android): verify MUI redesign passes existing test suite"
```

---

## Definition of Done

- All tasks 0.1 through 8.3 are checked off.
- `git log --oneline` shows one commit per task (or per phase if grouped).
- APK builds and installs without errors.
- Every interactive element on Timeline, Tasks, Projects, References, Preferences, QuickCreate, AccountMenu, Notifications, SidePanel sections is identifiable as a tap target by silhouette (ripple, elevation, shape).
- Zero `fontWeight` literals in `mobile/`. Zero hardcoded English in `mobile/` `Text(...)` calls (use `R.string.*`). Zero `TextButton` for primary/secondary actions.
- `MUI redesign` spec acceptance criteria (`docs/superpowers/specs/2026-07-16-android-ui-misuse-redesign-design.md` §5) all pass.

---

## Execution Notes for the implementer

- **Subagent dispatching**: use `superpowers:subagent-driven-development`. Each phase's tasks are independent across files; dispatch up to 4 in parallel within a phase. Wait for one phase to land before starting the next.
- **Build verification**: every task ends with `./gradlew assembleDebug` succeeding. Don't ship broken builds between tasks.
- **Device verification**: tasks that change visible UI end with an `adb install` + manual sanity check. Use the existing Redmi XIG03 connection.
- **Spec drift**: if a task reveals the spec is incomplete or wrong, escalate back to the user before continuing. Don't invent scope.
- **Memory updates**: after the redesign lands, update memory `feedback_web_composition_parity` if anything noteworthy was learned.