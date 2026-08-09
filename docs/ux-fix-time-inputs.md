# UX Fix — Dashboard Time + Weekday Inputs (2026-08-09)

User feedback: dashboard tile creation's time inputs were text-only. Investigation
in `docs/ux-investigation-tile-creation.md` flagged three text-only fields in
`QuickCreateSheet.kt`: `recurrenceStartTime`, `recurrenceEndTime`, and
`recurrenceWeekdaysCsv`. This change replaces them with proper Compose widgets
while keeping the wire format untouched.

## Files changed

- **NEW** `app/src/main/java/app/tastile/android/ui/dashboard/components/TimePickerField.kt`
  Small dashboard-components helper: read-only HH:mm `OutlinedTextField` paired
  with an `IconButton` (Schedule icon) that opens an `AlertDialog` containing
  the m3 `TimePicker`. Writes back zero-padded `HH:mm` on confirm. The wire
  format `HH:mm` stays the canonical state shape, so callers don't need to
  introduce a `LocalTime` dependency.

- `app/src/main/java/app/tastile/android/ui/dashboard/QuickCreateSheet.kt`
  - Imports `TimePickerField`.
  - Replaced the `OutlinedTextField` for `recurrenceStartTime` (was lines
    395-401) and `recurrenceEndTime` (was lines 403-409) with `TimePickerField`
    composables. State holders (`recurrenceStartTime` / `recurrenceEndTime`)
    remain `String` so the existing `parseTimeToMinutes` consumers at lines
    156-157 and the submit payload at lines 656-657 keep working unchanged.
  - Replaced the `recurrenceWeekdaysCsv` `OutlinedTextField` (was lines
    364-370) with a new private `WeekdayChipRow` composable. The chip row
    derives a `Set<Int>` view of the canonical CSV (integers in `0..6` where
    `0=Mon ... 6=Sun`) and toggles chips, writing back to the same CSV string.
    The submit-time split at lines 650-653 keeps consuming the CSV unchanged.

## Why

- The text-only HH:MM inputs forced users to know the wire format. Replacing
  them with `TimePicker` matches the existing pattern already used by
  `DateTimeField` (start/end of `useStartAt` / `useEndAt` blocks) and the
  mobile `TimePickerSheet`. The dashboard composer was the only place left
  with raw `OutlinedTextField` for time-of-day.
- The CSV weekday input was the only weekday pick surface without chips. The
  mobile `QuickCreateSubpanels.kt:1207-1229` already has the same chip pattern
  (`LocalWeekdayPicker`), so dashboard now matches it visually.

## Trade-offs / judgment calls

- **Inline `WeekdayChipRow` vs extract `LocalWeekdayPicker`**: the existing
  mobile `LocalWeekdayPicker` is `internal` to `ui.mobile.sheets.quickcreate`
  and is tightly coupled to a `testTag` callback used by the mobile
  instrumentation tests. Extracting it to a shared `ui/components/` location
  would have required making it `public`, threading test tags through the
  dashboard composer (or creating a parameter overload), and verifying the
  mobile tests still resolve. The inline approach mirrors the visual style
  exactly (same Mo..Su labels, same `FilterChip` spacing) and keeps each
  composer's UX responsibility local. The CSV representation is the canonical
  state shape in both, so the wire shape is identical.
- **`TimePickerField` placement**: lived alongside `DurationInput`,
  `DurationPickerDialog`, `SectionBlock`, and `PickerDialogs` in
  `ui/dashboard/components/` because it is a dashboard-only widget. No
  design-system changes; just a new top-level component in the existing
  dashboard components folder.
- **`is24Hour = true`**: matches the existing `DateTimeField` so all dashboard
  time pickers behave consistently. Mobile uses `TimePickerSheet` which has
  its own 24h logic; behaviour parity is acceptable here.
- **CSV ordering**: chips emit `Mo,We,Fr` order on save (canonical 0..6 order)
  even if the user toggles them in a different order. This matches the
  `split(",").mapNotNull { toIntOrNull() }.map { it.coerceIn(0, 6) }`
  ordering the submit path already produces from any CSV order.

## Verification

- `./gradlew :app:verifyDesignSystemImports` — **PASS**. New `m2-allow:`
  markers are present on every M3 import in `TimePickerField.kt`, which is
  under `ui/dashboard/` (a guarded root).
- `./gradlew :app:verifyNoEmbeddedServerSecrets` — **PASS**.
- `./gradlew :app:compileDebugKotlin` — **NOT RUN**: this sandbox has no
  Android SDK installed (`sdk.dir` in `local.properties` points to a missing
  directory). Verification by code review:
  - All imports match existing patterns used by the dashboard composer
    (`OutlinedTextField`, `Text`, `Icon`, `IconButton`, `TextButton`,
    `AlertDialog`, `TimePicker`, `rememberTimePickerState`,
    `ExperimentalMaterial3Api`).
  - `FilterChip` is already imported in `QuickCreateSheet.kt` for the tag
    chips.
  - `TimePickerField`'s signature `(value: String, label: String,
    onValueChange: (String) -> Unit, modifier: Modifier = Modifier)` is
    compatible with the existing call sites.
  - `WeekdayChipRow`'s signature `(csv: String, onCsvChange: (String) -> Unit)`
    matches the call site.
  - `String.format(Locale.US, "%02d:%02d", ...)` mirrors `DateTimeField`'s
    exact write-back pattern.

## Caveats

- No on-device verification was performed (no Android SDK in this sandbox).
- No tests target `QuickCreateSheet.kt` directly (confirmed by listing
  `app/src/test/.../ui/dashboard/` — only `DashboardViewModel`,
  `CalendarNavigation`, `DashboardCardMapper`, `MonthCalendarScreen`,
  `TimelineScreenLayout` tests exist). No test breakage is expected.
- The new `TimePickerField` is a private-shape (single-row, no date companion)
  variant of `DateTimeField`. If `DateTimeField` is later generalized to use
  `TimePickerField` for its time row, the duplicated dialog wiring could be
  removed. Out of scope for this change.