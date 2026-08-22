# i18n full coverage for tastile-android

## Goal

Bring every user-visible string under `stringResource(R.string.*)` lookup across
all 5 supported locales (`values/`, `values-es/`, `values-ja/`, `values-ko/`,
`values-zh-rCN/`) so locale switching on the device actually changes what the
user sees, including the previously untranslated QuickCreate panel, ViewToggle,
and tile / project card labels.

## Background

- `app/src/main/res/values/strings.xml` already carries ~598 keys with equal
  width in every locale file (596 / 598 per locale).
- A `Text(text = "...")` / placeholder grep across `app/src/main/java` returns
  ~40 distinct call sites in 19 files where a literal English or Japanese
  string is rendered directly without `stringResource`.
- Examples (non-exhaustive):
  - `ui/mobile/panels/timeline/TimelineMetaPills.kt:47,52,57,62,67` pill labels
  - `core/designsystem/component/ViewToggle.kt:120,121,135,136` "Compact view" /
    "Expanded view"
  - `ui/login/LoginScreen.kt:93,99,156` button / hint labels
  - `ui/mobile/tabs/tiles/TilesSectionColumn.kt:96` and several `Tabs*` /
    `TileCard` rows with inline text
  - `QuickCreateSubpanels.kt` / `QuickCreateSheet.kt` form labels + placeholders
    (large section — the most user-visible omission)

## Scope

### In

1. Re-read every existing key in `values/strings.xml` to spot keys whose value
   is a hard-coded English string already used by code (`"Quick create"`,
   `"Projects"`, `"Tasks"`, etc.). Those are the safest first targets because
   no new key is needed — only the call site changes.
2. Add any **new** keys required by call sites that do not have an existing
   one (likely ≤ 30 new keys total).
3. Replace every identified literal in `.kt` with `stringResource(R.string.X)`
   (or `pluralStringResource` / `getString` for context-descriptions).
4. Add or correct translations in all 5 `values*/strings.xml` so that locale
   switching produces output matching the corresponding English label.

### Out

- Brand string `app_name = Tastile` is `translatable="false"` and must stay so.
- Server-returned copy (e.g. errors with `ApiError.message`) is out of scope —
  those live in `tastile-core` strings, not this repo.
- Hard-coded `R.string.*` IDs in test fixtures stay as-is (tests pin to a
  specific key regardless of translation).
- Adding new locales beyond the 5 existing ones is out of scope.

## File list

### Touch (string resource files)

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-es/strings.xml`
- `app/src/main/res/values-ja/strings.xml`
- `app/src/main/res/values-ko/strings.xml`
- `app/src/main/res/values-zh-rCN/strings.xml`

### Touch (call sites to convert)

- `app/src/main/java/app/tastile/android/core/designsystem/component/AppComponents.kt`
- `app/src/main/java/app/tastile/android/core/designsystem/component/ViewToggle.kt`
- `app/src/main/java/app/tastile/android/ui/account/AccountScreen.kt`
- `app/src/main/java/app/tastile/android/ui/billing/BillingScreen.kt`
- `app/src/main/java/app/tastile/android/ui/dashboard/components/HelpBadge.kt`
- `app/src/main/java/app/tastile/android/ui/dashboard/QuickCreateSheet.kt`
- `app/src/main/java/app/tastile/android/ui/dashboard/QuickCreateSubpanels.kt`
- `app/src/main/java/app/tastile/android/ui/login/LoginScreen.kt`
- `app/src/main/java/app/tastile/android/ui/memo/MemoScreen.kt`
- `app/src/main/java/app/tastile/android/ui/mobile/account/TokensSheet.kt`
- `app/src/main/java/app/tastile/android/ui/mobile/calendar/DayViewTile.kt`
- `app/src/main/java/app/tastile/android/ui/mobile/panels/projects/NewProjectForm.kt`
- `app/src/main/java/app/tastile/android/ui/mobile/panels/timeline/TimelineBlockList.kt`
- `app/src/main/java/app/tastile/android/ui/mobile/panels/timeline/TimelineMetaPills.kt`
- `app/src/main/java/app/tastile/android/ui/mobile/tabs/ExecuteScreen.kt`
- `app/src/main/java/app/tastile/android/ui/mobile/tabs/ProjectsScreen.kt`
- `app/src/main/java/app/tastile/android/ui/mobile/tabs/tiles/TileCard.kt`
- `app/src/main/java/app/tastile/android/ui/mobile/tabs/tiles/TilesSectionColumn.kt`
- `app/src/main/java/app/tastile/android/ui/mobile/tabs/tiles/TilesTimelineBody.kt`

(During implementation the diff may surface a couple of additional call sites
beyond this list — the goal is exhaustive coverage, not a fixed inventory.)

## Implementation

### Phase 1 — Discovery (read-only)

Re-read every grep hit in source order. For each `Text(...)` or `placeholder =`
literal, decide one of:

- **A** — the value matches an existing key → replace call site only.
- **B** — value does not match any key → add key (English default) + reuse.
- **C** — value is dynamic / interpolated, not a translatable literal → skip.

Skip categories (debug-only labels, `testTag` strings, `Modifier.padding`
arguments, regex patterns, telemetry IDs, etc.) are explicitly out of scope.

### Phase 2 — Add keys

New keys go into all 5 locale files in alphabetical position (Android Studio
will reorder on next sync, so we approximate). Translations are produced from
the English value using the existing translation tone seen elsewhere in the
repo; the call sites reviewed at PR time.

### Phase 3 — Replace call sites

One atomic pass per file. Each `Text(text = "...")` becomes
`Text(stringResource(R.string.x))`. For `OutlinedTextField(..., placeholder =
{ Text("...") })` we route to `placeholder = { Text(stringResource(...)) }`.
For non-Composable call sites (e.g. Snackbar / Toast) we use
`LocalContext.current.getString(R.string.x)`.

### Phase 4 — Verification

`./gradlew verify`:
- Lint flags no missing translations when `lint` runs on `app:lintRelease` /
  `app:lintDebug`.
- Spot-check at least one screen per tab (dashboard, tasks, projects, tiles)
  in English, 日本語, 简体中文 via Android Studio Preview renderers.
- If Preview renderers are unavailable, instead run the app on device and
  flip system language in Settings → system → Languages & input.

### Phase 5 — Commit hygiene

- One commit per phase if the diff is large enough to merit it (translations
  fall naturally into A: "i18n keys", B: "call site rewires", C: "translations").
- Commit messages: Conventional Commits (`refactor(i18n): ...`,
  `fix(i18n): ...`, `docs(plan): add 2026-08-12 i18n full coverage`).
- The plan document itself ships in its own commit.

## Invariant alignment

- v1 spec: not touched (no schema, no API).
- Plugin contract: AGENTS.md Workspace Rules section "internal development docs
  are Japanese" applies to this plan doc (written in English here for the
  contract's `docs.plans/` convention); user-facing strings remain in the
  supported locales.
- m2-allow guard: this work introduces new `// m2-allow` lines if any
  resources touch `androidx.compose.material3.*` directly inside the listed UI
  folders; follow the existing convention `// m2-allow: <reason>` on the line
  immediately preceding each import.

## Open questions

1. Should we tighten the
   `verifyDesignSystemImports` Gradle guard to also scan for
   `Text(\s*text\s*=\s*"[A-Z]` regex (literal English-looking strings)? That
   would prevent regression but is a separate change — flag for follow-up, not
   this plan.
2. Some `AccountDropdownMenu.kt` items use inline emoji glyphs as the
   `IconButton` content description. Should those escape to the `:emoji:`
   pattern or stay literal? Default: keep literal — emoji is not a translatable
   text in this design.

## Rollback

If a regression blocks ship, the regressions live in the affected
`strings.xml` + call site files. Reverting the plan commits (max 3) restores
the previous state. The plan doc itself can stay or be moved to
`docs/archive/` depending on subsequent AGENTS.md direction.

## Verification

- `./gradlew verify --no-daemon` — Lint passes, no missing translations.
- `app:testDebugUnitTest` — all unit tests stay green (no behavioural change
  to ViewModels because the changes are pure textual).
- Manual: switch system language and confirm dashboard / tasks / projects /
  tiles / quick-create panels render in the chosen language including the
  previously-broken QuickCreate panel.
