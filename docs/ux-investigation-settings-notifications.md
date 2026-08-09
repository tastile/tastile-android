# UX Investigation: Settings, Notifications, Reference, Button Alignment

Read-only gap analysis. No source files modified. All paths absolute.

## Section 1: Settings — Language

### Current state

The language picker lives in the mobile Settings screen, not the Account screen.

- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\tabs\SettingsScreen.kt`
  - `LanguageSection` composable at lines 223-257
  - Renders a `NiaSingleChoiceSegmentedButtonRow` (lines 244-255) with exactly 2 hard-coded segments (`AppLocale.JA`, `AppLocale.EN`)
  - Shape parameter hard-codes `count = 2` (lines 248, 253) — this is why the picker is "fixed to 2"
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\data\repository\UserSettingsRepository.kt`
  - `enum class AppLocale` at lines 127-134 — only `JA("ja")` and `EN("en")`
  - `getLocale()` / `setLocale()` at lines 24-31 persist to `SharedPreferences` under `KEY_LOCALE`
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\dashboard\DashboardViewModel.kt`
  - `_locale` StateFlow at line 204, `setLocale` setter at lines 973-976

### Why it is "fixed to 2"

Two layers must change together to add a language:
1. Add the entry to `AppLocale` (UserSettingsRepository.kt:127-134) and the matching `settings_language_xx` / `locale_label_xx` strings (default `values/strings.xml:65-67, 164-165`).
2. Update `LanguageSection` in SettingsScreen.kt to render `AppLocale.entries` dynamically instead of two hard-coded segments.

The picker UI is also a `SingleChoiceSegmentedButtonRow`, which does not scale well beyond ~4 options. For 8 locales (the planned set, see Section 5) the UI needs a different control — likely a `DropdownMenu` or `ExposedDropdownMenuBox`.

### Resource keys

- `settings_language`, `settings_language_ja`, `settings_language_en` (defaults; missing values fall back to English)
- `locale_label_ja`, `locale_label_en`
- All 8 locale folders exist on disk: `values/`, `values-de/`, `values-es/`, `values-fr/`, `values-ja/`, `values-ko/`, `values-pt-rBR/`, `values-zh-rCN/` — but only `values/` and `values-ja/` actually translate `settings_language*` and `locale_label_*`; the other 6 have empty placeholders (e.g. `values-de/strings.xml:132-134, 168-169`).

## Section 2: Notifications

### "Not supported in test" string — exact location

The string is **"Notifications unsupported"** (the user paraphrased it as "not supported in test"):

- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\res\values\strings.xml:77`
  - `<string name="settings_notifications_status_unsupported">Notifications unsupported</string>`
- Translated only in `values-ja/strings.xml:67` (`通知はサポートされていません`). Other 6 locales have empty placeholders.

It is displayed at:
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\tabs\SettingsScreen.kt:95-99`
  - Status is set to `R.string.settings_notifications_status_unsupported` when `canPostNotifications(context)` returns false (line 386-389: checks `Build.VERSION.SDK_INT < TIRAMISU || POST_NOTIFICATIONS granted`).
  - The displayed text comes from line 158: `status = stringResource(notificationStatusRes)`.

The status can become `unsupported` only when notifications are permanently blocked on the device. It does not mean "this build does not support it" — there is no `BuildConfig.DEBUG` guard around the test path.

### Test button does exist, but only fires a regular notification

- `SettingsScreen.kt:167-176` defines `onTest` callback.
- `SettingsScreen.kt:391-412` `postTestNotification()` builds a `NotificationCompat` (no full-screen intent, no `setFullScreenIntent`, no alarm sound) and posts it to channel `ALERTS` (line 11 of `ExecutionNotificationChannels.kt`).
- A real alarm-style test receiver exists but is **not wired**: `ExecutionAlarmTestReceiver.kt` (full file is 30 lines) builds an `Intent` for `ExecutionAlarmActivity`, but `ACTION_TEST_ALARM` is never sent from anywhere in app source. The receiver is registered in `AndroidManifest.xml:69-70` but is dead code from the UI's perspective.
- `ExecutionAlarmActivity.kt:178-184` does dismiss the alarm correctly via `dismissAlarm()`. The activity itself (lines 53-192) implements proper full-screen alarm UX: `setShowWhenLocked`, `setTurnScreenOn`, `requestDismissKeyguard`, looping alarm `MediaPlayer` with `USAGE_ALARM` (line 142), and `VibrationEffect.createWaveform(...)` (line 159).

### Alarm-style infrastructure is mostly in place

- `AndroidManifest.xml:5-8` declares all four required permissions: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `SCHEDULE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`.
- `ExecutionNotificationChannels.kt:34-50` creates the `execution-alarms-v2` channel with `IMPORTANCE_HIGH` + alarm ringtone + vibration.
- `ExecutionAlarmScheduler.kt` (full file 127 lines) uses `setExactAndAllowWhileIdle` (line 73) when `canScheduleExactAlarms()` returns true; falls back to `setAndAllowWhileIdle` otherwise.
- `ExecutionAlarmReceiver.kt:47` calls `ExecutionNotificationIntents.startAlarmActivity(...)` — this opens `ExecutionAlarmActivity` directly (full-screen), bypassing the notification shade.
- `ExecutionAlarmRescheduleReceiver.kt` re-arms after `BOOT_COMPLETED`, `TIMEZONE_CHANGED`, `TIME_SET`, `MY_PACKAGE_REPLACED`, and `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.

### The real gap

The "Test" button at `SettingsScreen.kt:360-362` posts a plain reminder notification (line 399-407) instead of firing the alarm-style receiver. Wiring `onTest` to send `Intent(context, ExecutionAlarmTestReceiver::class.java).setAction(ACTION_TEST_ALARM)` (or directly launching `ExecutionAlarmActivity`) would expose the alarm UX without scheduling.

There is no `BuildConfig.DEBUG` guard on the test path — the "unsupported" label is purely a runtime permission-state label, not a build-time gate.

## Section 3: Reference screen

### Current state

The drawer label says "References" but the screen it navigates to is the legacy Integrations placeholder.

- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\SidePanelDrawerContent.kt:60`
  - `DrawerRoute("integrations", "References", Icons.Outlined.Link)` — the route id is `integrations`, the visible label is "References".
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\MobileScaffold.kt:231-234`
  - Route `"integrations"` -> `IntegrationsScreen`.
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\tabs\IntegrationsScreen.kt`
  - Full file is 71 lines. Renders a static notice that Google Calendar integration is "outside v1 scope". No data source, no list, no empty-state toggle. It is a placeholder by design, not a broken screen.
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\data\repository\ReferenceOverlayStore.kt`
  - Exists for the label-overlay feature (toggle which labels show on tiles) — not for the References screen.

### Why it is empty

Two causes: (a) the screen label is misleading ("References" maps to `IntegrationsScreen`'s "Google Calendar" notice), and (b) the IntegrationsScreen is intentionally a static notice until v2 (per its own doc-comment, lines 19-26). There is no backing list to populate.

### When should it be hidden?

When v2 ships the actual references feature, the IntegrationsScreen should either be replaced by a real ReferencesScreen or the drawer entry should be hidden behind a feature flag / when `integrations.isEmpty()`. Today there is no toggle — the row is always visible.

## Section 4: Button alignment — 3 examples

Pattern observed across multiple screens: `Row(...)` containing buttons has no `Modifier.fillMaxWidth()` and no `horizontalArrangement` that produces centered / evenly-spread alignment, so children default to `Arrangement.Start` (left edge of the row).

### Example 1 — SettingsScreen Notifications allow/test buttons (clearest issue)

`C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\tabs\SettingsScreen.kt:353-363`

```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    NiaButton(onClick = onAllow) { Text(stringResource(R.string.settings_notifications_allow)) }
    NiaButton(onClick = onTest) { Text(stringResource(R.string.settings_notifications_test)) }
}
```

No `Modifier.fillMaxWidth()`. Default horizontal arrangement = `Arrangement.Start`. Buttons render flush left.

### Example 2 — SettingsScreen security-lock timeout row

`C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\tabs\SettingsScreen.kt:291-312`

```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    NiaButton(onClick = onDecrement) { Text("-5") }
    Text(timeout)
    NiaButton(onClick = onIncrement) { Text("+5") }
}
```

`fillMaxWidth()` is set but `Arrangement.spacedBy(8.dp)` only adds 8.dp between children — no center or end alignment, so the cluster sits on the left.

### Example 3 — DashboardScreens tile action row

`C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\dashboard\DashboardScreens.kt:161-170`

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    if (lifecycle == TileLifecycle.READY) { NiaButton(text = { Text("Start") }, onClick = onStart) }
    if (lifecycle == TileLifecycle.STARTED) { NiaButton(text = { Text("Complete") }, onClick = onComplete) }
    NiaFilledTonalButton(text = { Text("Defer") }, onClick = onDefer)
    NiaOutlinedButton(text = { Text("Delete") }, onClick = onDelete)
}
```

Same pattern: no `fillMaxWidth()` and only `spacedBy(8.dp)`. Buttons cluster on the left.

### Contrast — correctly aligned examples

- `AccountScreen.kt:135-142, 147-154`: `Modifier = Modifier.fillMaxWidth()` and the button is the direct child of a `Column`, so it spans the full width. **Note:** this is `fillMaxWidth` but the button is the only row child, not a `Row` cluster. The text inside is left-padded by `ButtonDefaults.ContentPadding` which is what the user perceives as "buttons too far left" — they are aligned to start, not centered.
- `LoginScreen.kt:117-128`: also `fillMaxWidth()` for a single-button Column.
- `QuickCreateSheet.kt:610-613`: `Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp))` containing `TextButton` (Cancel) + `Button` (Create). The pair sits on the left because `spacedBy` does not center; user would expect Cancel/Create right-aligned or full-width.

## Section 5: Cross-repo pointers

### Planned locale list (web)

`C:\Users\rebui\Desktop\tastile\tastile-web\src\shared\stores\locale-store.ts:3`

```ts
export type Locale = "en" | "ja" | "de" | "es" | "pt-BR" | "fr" | "ko" | "zh-CN";
export const DEFAULT_LOCALE: Locale = "ja";
export const FALLBACK_LOCALE: Locale = "en";
```

Eight locales. Android already has all 8 `values-*` folders; only `values/` and `values-ja/` are populated with `settings_language*` / `locale_label_*` keys. Web's `GeneralPreferencesPage.tsx:125-133` also only offers the same 2 fixed segments (ja/en) — so the "more languages planned" gap is shared with web and needs both repos to extend in lockstep. No ADR or planning doc in either repo lists the locale roadmap.

### Alarm UX (web has no equivalent)

The web client uses `Notification` API (`tastile-web/src/views/dashboard/GeneralPreferencesPage.tsx:53-103`) — no full-screen / lock-screen override possible in a browser. Alarm-style UX is Android-only. No cross-repo pattern to copy.

### Reference screen (web has no equivalent)

Web has no "References" tab or drawer entry. `find /c/Users/rebui/Desktop/tastile/tastile-web/src -path "*reference*"` only turns up `tile/model/v1/reference.ts` and `dashboard/preferences` directories. The "References" drawer item is an Android-only legacy. Safe to hide without coordination.

## Section 6: Priority recommendations

| # | Issue | Impact | Effort | Notes |
|---|---|---|---|---|
| 1 | Notifications "Test" button posts a regular notification, not the alarm-style full-screen one (SettingsScreen.kt:391-412 vs ExecutionAlarmTestReceiver.kt) | High — alarm-style is the headline feature promised by the architecture docs | Low — wire `onTest` to send `ACTION_TEST_ALARM` to `ExecutionAlarmTestReceiver`; channel and activity already exist |
| 2 | Language picker is hard-coded to 2 segments; planned list is 8 (SettingsScreen.kt:223-257, UserSettingsRepository.kt:127-134) | Medium — feature is shipped but blocks adding locales | Medium — replace segmented row with `DropdownMenu`, extend `AppLocale` enum, populate 6 missing locale string folders in lockstep with web |
| 3 | Notifications button row clusters on the left (SettingsScreen.kt:353-363) | Medium — visible UX imbalance | Low — add `Modifier.fillMaxWidth()` and `horizontalArrangement = Arrangement.SpaceBetween` or `Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)` |
| 4 | Reference drawer item always shows (SidePanelDrawerContent.kt:60) even when content is a static v1-out-of-scope notice | Low — informational only | Low — gate the row on a feature flag or remove entirely until v2 |
| 5 | Other button-cluster rows use the same left-default pattern (DashboardScreens.kt:161-170, QuickCreateSheet.kt:610-613) | Low | Low — global `Row` modifier pattern fix |

The single biggest UX win is item 1: the alarm-style full-screen experience is fully implemented in code, just disconnected from the Settings UI's Test button.

## Section 7: Concrete file:line fixes

### Fix 1 — Wire Test button to the alarm-style receiver

`C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\tabs\SettingsScreen.kt:167-176`

Replace the body of the `onTest` lambda so it dispatches `ACTION_TEST_ALARM` to `ExecutionAlarmTestReceiver` (or directly launches `ExecutionAlarmActivity`). No need to touch the test receiver or the activity — both already exist.

```kotlin
onTest = {
    val grantedNow = canPostNotifications(context)
    notificationGranted = grantedNow
    if (grantedNow) {
        val intent = Intent(context, ExecutionAlarmTestReceiver::class.java).apply {
            action = ExecutionAlarmTestReceiver.ACTION_TEST_ALARM
        }
        context.sendBroadcast(intent)
        notificationStatusRes = R.string.settings_notifications_test
    } else {
        notificationStatusRes = R.string.settings_notifications_status_denied
    }
}
```

Add an import: `import android.content.Intent` and `import app.tastile.android.notifications.ExecutionAlarmTestReceiver`.

### Fix 2 — Language picker → dropdown

`C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\tabs\SettingsScreen.kt:223-257`

Replace `LanguageSection`'s segmented row body with a `NiaListItem` whose `trailingContent` is an `ExposedDropdownMenuBox` listing `AppLocale.entries` mapped through `localeLabel(...)`. Also extend `localeLabel` at lines 374-378 to cover all 8 entries (the strings exist as `locale_label_de/es/fr/ko/pt-rBR/zh-rCN` once added to the per-locale `strings.xml`).

`C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\data\repository\UserSettingsRepository.kt:127-134`

Add the 6 missing locales:

```kotlin
enum class AppLocale(val value: String) {
    JA("ja"), EN("en"), DE("de"), ES("es"), FR("fr"), KO("ko"), PT_BR("pt-rBR"), ZH_CN("zh-rCN");
    companion object {
        fun from(value: String): AppLocale = entries.firstOrNull { it.value == value } ?: JA
    }
}
```

Populate the matching `settings_language_*` / `locale_label_*` strings in the 6 currently-empty locale folders.

### Fix 3 — Center the notification button row

`C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\tabs\SettingsScreen.kt:353-363`

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
) {
    NiaButton(onClick = onAllow) { Text(stringResource(R.string.settings_notifications_allow)) }
    NiaButton(onClick = onTest) { Text(stringResource(R.string.settings_notifications_test)) }
}
```

Apply the same `Modifier.fillMaxWidth()` + `spacedBy(8.dp, Alignment.CenterHorizontally)` to:
- `SettingsScreen.kt:291-296` (security-lock timeout row)
- `DashboardScreens.kt:161-170` (tile action row)
- `QuickCreateSheet.kt:610-613` (Cancel/Create row — or use `Arrangement.End` to keep destructive action right-aligned)

### Fix 4 — Hide References drawer row

`C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\SidePanelDrawerContent.kt:56-61`

Remove the `integrations` row until v2 actually has references content, or wrap it in a feature flag check. If kept, fix the label mismatch by renaming `IntegrationsScreen` content to "References" or rename the drawer label to "Integrations".

---

## Quick answers

- **"Not supported in test" string location**: `app/src/main/res/values/strings.xml:77` (key `settings_notifications_status_unsupported`, value `Notifications unsupported`). It is shown by `SettingsScreen.kt:95-99, 158` when `canPostNotifications()` returns false at runtime — there is no build-time `BuildConfig.DEBUG` guard.
- **Planned language list** (from web sibling `tastile-web/src/shared/stores/locale-store.ts:3`): `en, ja, de, es, pt-BR, fr, ko, zh-CN` (8 locales). Android's `AppLocale` enum only has `JA, EN` today; the 8 `values-*` folders exist on disk but 6 are empty for the language keys.
- **Top-3 most impactful fixes**:
  1. Wire the SettingsScreen "Test" button to `ExecutionAlarmTestReceiver` (single high-impact change, alarm UX already built end-to-end).
  2. Convert `LanguageSection` from segmented row to dropdown and extend `AppLocale` to 8 entries.
  3. Add `Modifier.fillMaxWidth()` + `Alignment.CenterHorizontally` to the notification/security-lock/tile-action button rows to fix the left-cluster pattern.