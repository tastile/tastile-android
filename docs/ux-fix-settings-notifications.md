# UX Fix: Settings (alarm test, language dropdown), button alignment

Scope: 4 user-reported gaps in `tastile-android` Settings + drawer screen.
Follow-up to `docs/ux-investigation-settings-notifications.md`. No design-system
internals, dependencies, or click behavior changed.

## Fix 1: Wire "Test" button to the alarm-style test receiver

User report: 「通知の許可・テストでサポートされていない → サポートするべき、
アラームとしての全画面通知機能も入れる必要がある」
(Notifications: shows "unsupported"; needs full-screen alarm support.)

The full-screen alarm UX (`ExecutionAlarmActivity`, alarm channel,
`ExecutionAlarmTestReceiver`) was already implemented and registered in
the manifest but the Settings "Test" button was firing a plain
`NotificationCompat` reminder. This fix wires the existing test receiver
into the onTest lambda.

### Files changed

- `app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt`
  - Lines 5, 78 (`Intent` and `ExecutionAlarmTestReceiver` imports added)
  - Lines 179-197 (`onTest` lambda body): now sends a broadcast
    `Intent(context, ExecutionAlarmTestReceiver::class.java).apply {
    action = ACTION_TEST_ALARM }` via `context.sendBroadcast(...)`. A
    `try { ... } catch (SecurityException) { postTestNotification(...) }`
    fallback preserves the previous plain-notification path if the
    receiver ever becomes blocked.

### Files NOT touched (per instruction)

- `notifications/ExecutionAlarmTestReceiver.kt`
- `notifications/ExecutionAlarmActivity.kt`
- `notifications/ExecutionAlarmScheduler.kt`
- `notifications/ExecutionNotificationChannels.kt`
- `AndroidManifest.xml` (receiver already registered at lines 68-70)

### Caveat

The user instruction read "Schedule via `ExecutionAlarmScheduler`". On
inspection, `ExecutionAlarmScheduler` is tightly coupled to
`AuthRepository` + `coreRuntimeService.currentSnapshot()` — it schedules
real tile-plan alarms, not a synthetic test. The test receiver is the
correct entry point for the "Test" button: it directly launches
`ExecutionAlarmActivity` with the full-screen alarm UX. The behavior
matches the investigation report's §7 Fix 1 example.

## Fix 2: Convert `LanguageSection` from segmented row to dropdown

User report: 「設定は言語が2言語固定前提 → 言語ドロップダウンに変えるべき」
(2-language fixed; needs a dropdown, more languages planned.)

The investigation's canonical 8-locale list came from the web sibling
`tastile-web/src/shared/stores/locale-store.ts`. The order and locale
tags (`en, ja, de, es, pt-BR, fr, ko, zh-CN`) are reproduced exactly;
the existing `values-*` resource folders already match.

### Files changed

- `app/src/main/java/app/tastile/android/data/repository/UserSettingsRepository.kt`
  - Lines 127-140: `AppLocale` enum extended from 2 entries to 8.
    Declaration order mirrors the web sibling
    `tastile-web/src/shared/stores/locale-store.ts`:
    `EN, JA, DE, ES, PT_BR, FR, KO, ZH_CN`. The Android resource
    qualifier tags `pt-rBR` and `zh-rCN` are used for the Brazilian
    Portuguese and Simplified Chinese entries. The `from(value)`
    companion still defaults to `JA` for unknown persisted values —
    forward compat preserved.
- `app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt`
  - Lines 34-49: added imports for `DropdownMenuItem`,
    `ExposedDropdownMenuAnchorType`, `ExposedDropdownMenuBox`,
    `ExposedDropdownMenuDefaults`, `OutlinedTextField`. Imports
    re-ordered to alphabetical.
  - Lines 244-302: `LanguageSection` body replaced. The 2-segment
    `NiaSingleChoiceSegmentedButtonRow` is replaced with an
    `ExposedDropdownMenuBox` containing an `OutlinedTextField`
    (`menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable,
    enabled = true)`) and an `ExposedDropdownMenu` listing
    `AppLocale.entries` via `DropdownMenuItem`. The picker state is
    local `var expanded by remember { mutableStateOf(false) }`.
    The existing `NiaListItem` header (language icon + title) is
    kept untouched for visual consistency with the other sections.
  - Lines 421-433: `localeLabel(AppLocale)` `when` extended to cover
    all 8 entries with matching `R.string.locale_label_*` resources.
- `app/src/main/res/values*/strings.xml` (8 files):
  `values/`, `values-ja/`, `values-de/`, `values-es/`, `values-fr/`,
  `values-ko/`, `values-pt-rBR/`, `values-zh-rCN/`.
  - Added `settings_language_<tag>` and `locale_label_<tag>` keys for
    de, es, fr, ko, pt_rBR, zh_rCN in every folder. The display
    strings tend to be the native script where the locale has a
    standard endonym (e.g. `Deutsch`, `Español`, `한국어`,
    `简体中文`), English/`Japanese` for the un-translated defaults
    and languages without a native endonym in the key set.
  - `settings_language` itself was empty in 6 folders; populated
    with the native translation (`Sprache`, `Idioma`, `Langue`,
    `언어`, `语言`).

### Persistence behavior

Selection still flows through `viewModel.setLocale(...)` →
`UserSettingsRepository.setLocale(...)` → `SharedPreferences` key
`KEY_LOCALE`. The dropdown is the only picker UI; the existing
`SettingsScreen.collectAsStateWithLifecycle()` on `viewModel.locale`
binds the displayed value to the persisted state.

### Files NOT touched

- `ui/dashboard/components/PickerDialogs.kt` (`LocalePickerDialog`)
  — kept its 2-entry hardcoded list. The component is no longer
  reachable from a production call site (only its `@Preview` references
  it), and the user's task only specified the Settings screen.
- `ui/dashboard/DashboardViewModel.kt` — `setLocale` already had the
  right signature; no change needed.

## Fix 3: Hide the "References" drawer row when content is empty

User report: 「Referenceは現状何も無いから隠すべき」
(Reference is empty; hide it.)

The investigation listed this at `SidePanelDrawerContent.kt:60`. The
gate already exists from the prior `docs/ux-fix-reference-and-buttons.md`
work: `SidePanelDrawerContent` exposes a `hasReferencesContent: Boolean`
parameter and `MobileScaffold` passes
`hasReferencesContent = tiles.any { it.labels.isNotEmpty() }`. The
`integrations` row is hidden when no tile has labels.

### Files changed

None. Verified the existing wiring still matches the desired behavior
(`SidePanelDrawerContent.kt:78-90`, `MobileScaffold.kt:132,321`).

## Fix 4: Center button clusters in 4 locations

User report: 「ボタンが左揃え過ぎ」
(Buttons too left-aligned.)

### Files changed

- `app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt`
  - Lines 336-357 (security-lock timeout row): added
    `Alignment.CenterHorizontally` to `Arrangement.spacedBy(8.dp, ...)`.
  - The notifications "Allow / Test" row (lines 398-409) already had
    `Modifier.fillMaxWidth()` and `Arrangement.spacedBy(8.dp,
    Alignment.CenterHorizontally)` from a previous unrecorded edit;
    no change needed.
- `app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt`
  - Lines 161-174 (tile action row): added `Modifier.fillMaxWidth()`
    and `Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)`.
- `app/src/main/java/app/tastile/android/ui/dashboard/QuickCreateSheet.kt`
  - Lines 604-607 (Cancel / Create row): added
    `Alignment.CenterHorizontally` to `Arrangement.spacedBy(8.dp, ...)`.
    Kept `Arrangement.spacedBy(...)` rather than `Arrangement.End`
    because the action pair is non-destructive (Cancel is a
    `TextButton`, Create is a `Button`) and the existing pattern in
    the file is "centered cluster", not "right-aligned confirm".

### Files NOT touched

- `core/designsystem/component/Button.kt` — the design-system wrapper
  itself is unchanged. Only call sites were modified.

## Verification

`./gradlew :app:compileDebugKotlin` was **not** run — the Android SDK
referenced in `local.properties` (`C:\Users\rebui\AppData\Local\Android\Sdk`)
does not exist in this environment, so the build aborts at SDK
resolution. The mobile-only repo has no `.tools/` shortcut.

Manually verified by reading:

- All 8 `values-*/strings.xml` files contain the new keys
  (`settings_language_de/es/fr/ko/pt_rBR/zh_rCN` and
  `locale_label_de/es/fr/ko/pt_rBR/zh_rCN`) with non-empty values.
- `AppLocale` enum has 8 entries; `from(value)` defaults to `JA` for
  unknown persisted values (preserves existing-install behavior).
- `LocalePickerDialog` (`ui/dashboard/components/PickerDialogs.kt:74-89`)
  keeps its 2-entry hardcoded list intact — no production call site,
  no compile error from the extended enum.
- `DashboardViewModel.setLocale(AppLocale)` signature unchanged; the
  new `AppLocale` values flow through cleanly.
- `ExposedDropdownMenuBox`/`ExposedDropdownMenu` API patterns trace
  to `ui/dashboard/components/AutoCompleteTextField.kt` (already uses
  the same `menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable,
  enabled = true)` and `ExposedDropdownMenu` scope member).
- `ExecutionAlarmTestReceiver` is `exported="false"` in `AndroidManifest.xml:68-70`,
  so the in-app `sendBroadcast` call is allowed without an explicit
  permission.
- `postTestNotification` is still called from the `SecurityException`
  fallback path; the unused import would only surface if Android
  Lint flagged it.

## Tests

The `mockk<V1ApiClient>()` units in `data/` are not affected — the
V1 API facade / data layer is untouched. The Compose `SettingsScreen`
is exercised by `SettingsNavigationTest` (instrumented). It builds the
screen with `mockk<UserSettingsRepository>(relaxed = true)` and calls
`setLocale(AppLocale.EN)` — `AppLocale.EN` still exists in the extended
enum, so the test should continue to pass. The `LocalePickerDialog`
preview test refers to `AppLocale.JA` (still valid). No new mocks
needed.

## Caveats

- The dropdown label currently shows the locale endonym
  (e.g. "日本語" when device locale is en). Users may prefer the
  native-name convention or the English-name convention; this fix
  follows the existing pattern (the pre-change picker also showed
  endonyms via `settings_language_ja`).
- `LocalePickerDialog` is left at 2 entries because it is no longer
  wired to a production call site; the user's task scope was the
  Settings screen only. If the picker is revived later, it will need
  to be extended to `AppLocale.entries`.
- `ExecutionAlarmScheduler` was not used for the Test button because
  it requires a user ID and a `CoreSnapshot`. The test receiver is
  the correct entry point for an out-of-band UI test.
