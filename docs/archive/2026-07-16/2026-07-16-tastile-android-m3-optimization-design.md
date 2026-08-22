# Android Material 3 Optimization — Design

> **Complements** `2026-07-16-android-ui-misuse-redesign-design.md` (in this directory).
> Read the sibling spec first for tactical MUI component fixes.
> This spec covers the platform layer underneath those tactical fixes, the four
> optimization axes beyond component shapes, and the `ui/dashboard/**` migration
> that the tactical spec defers.

| Concern | Spec |
|---|---|
| Tactical MUI component swaps in `ui/mobile/**` (ListItem, FilledTonalButton, Spacing, fontWeight literals, English strings) | MUI Misuse Redesign (sibling spec) |
| Theme foundation + Dynamic Color + brand/gray fallback | **This spec (M1)** |
| Component API unification rules (L0 wrapper conventions) | **This spec (M2)** |
| State & recomposition (`@Stable`, `collectAsStateWithLifecycle`, `LazyColumn` keys) | **This spec (M3)** |
| Drawing performance (Timeline Macrobenchmark, `Modifier.drawBehind` audit) | **This spec (M4)** |
| Accessibility (Role, `contentDescription`, hit target, contrast) | **This spec (M5)** |
| `ui/dashboard/**` migration (Timeline, Dashboard, QuickCreate legacy, ManagementScreens, MonthCalendarScreen) | **This spec (M2 + M4 partial)** |
| Web parity content / i18n / composition (R1–R5) | `2026-07-07-android-content-parity.md` (untouched) |

---

## 1. Background

User asked on 2026-07-16 to optimize `tastile-android` for Material 3. Four
axes confirmed in scope:

- State / recomposition optimization
- Component API unification
- Drawing / scrolling performance
- Accessibility with Dynamic Color

Dynamic Color decided **ON**: Material You is the default on Android 12+; brand
palette is the fallback for `<12` and for the `ThemeMode.GRAY` / `BRAND`
selections. Existing `tastile-brands/` palette stays untouched.

The MUI Misuse Redesign spec is mid-execution (recent commits
`ea0c1ce` / `101eef2` / `71dbaf8` / `76c22ea` / `7ca690c` migrate sheets to the
mobile design system). This spec extends that work.

### User-reported hotspot

**TimelineScreen (Day/Week/Month)** is the only currently reported janky
surface. Phase M4 dedicates its Macrobenchmark and manual frame inspection to
this screen first; remaining screens are audited opportunistically.

### Parity safety

`2026-07-07-android-content-parity.md` rules R1–R5 are binding. This spec
introduces:

- **No new controls** beyond the Dynamic-Color toggle in Settings.
- **Two new i18n keys** only: `preferences.dynamic_color.label` and
  `preferences.dynamic_color.description`.
- **No layout reorder**.

The Dynamic-Color toggle is the single net new visible surface added.

---

## 2. Architecture overview

Three layers from the ground up:

1. **Theme / Token layer** (`ui/designsystem/`)
   - Holds `TastileTheme`, `BrandColors`, `grayColorScheme(dark)`,
     `LocalAppTouchTarget`, `LocalAppCornerRadius`.
   - Phase M1.

2. **Component / wrapper layer** (`ui/designsystem/AppComponents.kt`,
   `ui/mobile/designsystem/MobileComponents.kt`)
   - Holds all interactive wrappers.
   - L0 wrapper conventions enforced via lint custom rules.
   - Phase M2.

3. **Surface layer** (`ui/dashboard/**`, `ui/mobile/**`, `ui/account/**`)
   - Reads tokens from layer 1; uses wrappers from layer 2.
   - Per-surface optimization in Phases M3–M5.

Cross-cutting enforcement:

- `verifyDesignSystemImports` Gradle task is widened to forbid direct
  `material3.{Color, ColorScheme, Button, Card, ...}` imports in
  `ui/{dashboard, mobile, account}/**`.
- L0 wrapper conventions (see §4) enforced by custom lint rules.

---

## 3. Phase M1 — Theme + Dynamic Color

### 3.1 Goals

- Single entry point `TastileTheme(themeMode, dynamicColor, content)`.
- Material You active on Android 12+ by default; brand fallback otherwise.
- `ThemeMode.SYSTEM / LIGHT / DARK / GRAY / BRAND` enumerate correctly.
- `GrayColors` object is removed; replaced by `grayColorScheme(dark)`.
- `AppTheme` object stays as a façade
  (`AppTheme.colors = MaterialTheme.colorScheme`); call sites unchanged.

### 3.2 File map

| Status | Path |
|---|---|
| New | `app/src/main/java/app/tastile/android/ui/designsystem/Theme.kt` |
| New | `app/src/main/java/app/tastile/android/ui/designsystem/BrandColors.kt` |
| Edit | `app/src/main/java/app/tastile/android/ui/designsystem/AppTheme.kt` (remove `GrayColors`, re-export façade) |
| Edit | `app/src/main/java/app/tastile/android/ui/designsystem/AppTypography.kt` (consolidate `fontWeight` literals — also covered by MUI Misuse §3.7) |
| Edit | `app/src/main/java/app/tastile/android/ui/designsystem/AppShapes.kt` |
| Edit | All root composables that call `MaterialTheme { ... }` directly (e.g. `MainActivity`, `MobileScaffold`, `DashboardScreens`) → call `TastileTheme { ... }` |

### 3.3 `TastileTheme` skeleton

```kotlin
@Composable
fun TastileTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK  -> true
        else            -> isSystemInDarkTheme()
    }
    val ctx = LocalContext.current
    val cs = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        themeMode == ThemeMode.GRAY -> grayColorScheme(dark)
        else                        -> brandColorScheme(dark)
    }
    MaterialTheme(
        colorScheme = cs,
        typography  = appTypography,
        shapes      = appShapes,
    ) {
        CompositionLocalProvider(
            LocalAppTouchTarget  provides 48.dp,
            LocalAppCornerRadius provides AppCorner,
        ) { content() }
    }
}
```

### 3.4 Completion KPI

- `ThemeTest` passes for all 8 cells of `(themeMode × dynamicColor × dark)`.
- `TastileThemeSnapshotTest` (Robolectric + Paparazzi or compose-ui-test
  screenshot capture) matches fixtures for:
  - `dynamicLight` and `dynamicDark` using a stubbed wallpaper color,
  - `brandLight` and `brandDark` from `BrandColors.kt` fixtures,
  - `grayLight` and `grayDark` matching web's `#F4F4F5` / `#18181B`.

---

## 4. Phase M2 — Component API unification

### 4.1 L0 wrapper conventions

| # | Rule | Enforcement |
|---|------|-------------|
| C1 | `Modifier` is the **first** optional parameter after any required labels | lint custom `WrapperParameterOrder` |
| C2 | `enabled: Boolean = true` is the **last** optional parameter | lint custom `WrapperParameterOrder` |
| C3 | Wrappers receiving a lambda or `Color` directly are `@Stable`; data classes received are `@Immutable` | lint custom `WrapperStability` |
| C4 | No `Color(0x...` literals outside `BrandColors.kt` / `GrayColors.kt` (which is renamed `grayColorScheme`) | `verifyDesignSystemImports` + grep CI |
| C5 | Every interactive wrapper sets `Modifier.semantics { contentDescription = ...; role = Role.X }` | a11y review checklist (Phase M5) |

### 4.2 Component inventory

In scope for M2:

- `AppPrimaryButton`, `AppSecondaryButton`, `AppTonalButton`, `AppTextButton`
- `AppListItem`
- `AppPickerButton`, `AppPickerButtonCompact`
- `AppSectionHeader`, `AppStatChip`
- `AppEmptyState`

Out of scope (MUI Misuse spec, do not duplicate):

- `ListItem` adoption for tappable rows — see MUI Misuse §3.1
- `FilledTonalButton` adoption — see MUI Misuse §3.2
- `OutlinedTextField` → sheet-picker conversion — see MUI Misuse §3.3
- `Card(onClick = ...)` replacement — see MUI Misuse §3.4
- `MobileSpacing` token definitions — see MUI Misuse §3.5
- Web-parity strings sweep — see MUI Misuse §3.6
- Typography `fontWeight` literal sweep — see MUI Misuse §3.7 (this spec extends it to `ui/dashboard/`)

### 4.3 Design system boundary decision tree

Where does a new wrapper belong?

```
Is this wrapper also used by ui/dashboard/ (tablet / future desktop)?
├─ Yes → ui/designsystem/AppComponents.kt
└─ No
   └─ Does its implementation reference mobile-only CompositionLocal
       or ContextWrapper (mobile sheet semantics, mobile splash API)?
       ├─ Yes → ui/mobile/designsystem/MobileComponents.kt
       └─ No  → prefer ui/designsystem/ (single source of truth)
```

When in doubt: prefer `ui/designsystem/`; move to `mobile/` only when a
`mobile/`-specific symbol is required by the wrapper implementation itself.

### 4.4 Guard extension

Replace the existing one-file allowlist in `app/build.gradle.kts`:

```kotlin
val designSystemGuardFiles: List<File> = run {
    val roots = listOf(
        "app/src/main/java/app/tastile/android/ui/dashboard",
        "app/src/main/java/app/tastile/android/ui/mobile",
        "app/src/main/java/app/tastile/android/ui/account",
    )
    roots.flatMap { root ->
        fileTree(root) { include("**/*.kt") }.files
    }
}
```

Forbidden import prefixes (rejected by `verifyDesignSystemImports`):
`androidx.compose.material3.Color`, `ColorScheme`, `Button`, `Card`,
`FilledTonalButton`, `OutlinedButton`, `TextButton`, `AssistChip`,
`ElevatedButton`, `ElevatedCard`, `OutlinedCard`, `FilterChip`,
`InputChip`, `SuggestionChip`, `Surface(onClick = ...)`.

Whitelist: `Icon`, `Text`, `HorizontalDivider`, `VerticalDivider`,
`CircularProgressIndicator`, `LinearProgressIndicator`.

A first commit widens the guard and immediately fixes the violations it
surfaces, so subsequent phases operate on a clean baseline.

### 4.5 Completion KPI

- `./gradlew :app:check` runs the extended `verifyDesignSystemImports`,
  `verifyNoEmbeddedServerSecrets`; both green.
- `WrapperParameterOrder` and `WrapperStability` lint tests pass on
  `ui/{dashboard, mobile, account}/**`.
- All wrappers covered by §8 unit tests. The `WrapperParameterOrder` and `WrapperStability` lint rule implementations (and their unit tests) are added in M2; they are not assumed to exist already.

---

## 5. Phase M3 — State / recomposition

### 5.1 Goals

- Every `collectAsState()` call site migrated to `collectAsStateWithLifecycle()`.
- Every `LazyColumn` / `LazyVerticalGrid` has an explicit `key = { ... }`.
- Compose Compiler Reports show net-positive growth in skippable-function count.

### 5.2 File map

| Status | Path |
|---|---|
| Edit | All ViewModels emitting Compose state — confirm `@Stable` / `@Immutable` annotations on data classes exposed to UI |
| Edit | All Compose call sites of `collectAsState()` — switch to `collectAsStateWithLifecycle()` |
| Edit | All `LazyColumn` / `LazyVerticalGrid` — add `key = { it.stableId }` |
| New | `app/src/test/java/app/tastile/android/lint/LazyColumnKeyRuleTest.kt` |
| New | `app/src/test/java/app/tastile/android/lint/CollectAsStateRuleTest.kt` |

### 5.3 Out of scope (explicit)

- Splitting `DashboardViewModel` into smaller ViewModels — deferred. The parity
  plan treats `DashboardViewModel` as the cross-screen coordination surface.
  M3 only stabilizes the StateFlow ↔ Compose boundary; it does not change
  ViewModel topology.
- Wholesale switch to `kotlinx.collections.immutable.PersistentList` —
  applied opportunistically per wrapper when immutability clearly helps
  recomposition; otherwise deferred to a future spec.

### 5.4 Completion KPI

- `collectAsStateWithLifecycle` coverage = 100% (lint enforces; CLI grep
  audit shows zero `import androidx.compose.runtime.collectAsState` outside
  test code).
- `LazyColumnKeyRuleTest` passes for every file under `ui/mobile/**` and
  `ui/dashboard/**` containing a `LazyColumn` / `LazyVerticalGrid`.
- Compose Compiler Reports before/after diff (logged to
  `logs/m3-skippable/{before,after}.txt`) shows ≥ 5 functions gained
  skippable status after M3.

---

## 6. Phase M4 — Drawing performance (Timeline-first)

### 6.1 Goals

- Eliminate the reported Timeline jank.
- Audit `Modifier.drawBehind` call sites and remove dead ones.
- Keep Timeline v34–v36 Canvas-based grid/scroll architecture intact; add
  optimizations on top, do not rewrite.

### 6.2 Macrobenchmark addition

A single Macrobenchmark test is added inside `:app` (NOT a new module) to
keep the project graph simple and the CI footprint small. Dependencies:

```kotlin
androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:1.3.3")
androidTestImplementation("androidx.benchmark:benchmark-junit4:1.3.3")
```

Test file:
`app/src/androidTest/java/app/tastile/android/benchmark/TimelineScreenDayScrollBenchmark.kt`.

Measures: `frameTimeP95` during a 2-second scripted pinch + scroll gesture on
the Day view, 5 iterations, median of `frameTimeP95`.

Output: `logs/m4-perf/{before,after}.txt`.

### 6.3 Modifier audit

For every `Modifier.drawBehind` call site, log to `logs/m4-drawbehind.md`:

- File path and line.
- Verdict: `keep` / `replace with Surface(tonalElevation = ...)` / `remove`.
- Author and review signoff.

### 6.4 Completion KPI

- Timeline Day-view `frameTimeP95` improves by ≥ 10%, or absolute < 16.7 ms
  (≥ 60fps median) after M4. Stored in `logs/m4-perf/`.
- `Modifier.drawBehind` audit log complete and signed off in the M4 PR.

---

## 7. Phase M5 — Accessibility

### 7.1 Goals

- All interactive wrappers expose `Modifier.semantics { contentDescription = ...; role = Role.X }`.
- `Modifier.minimumInteractiveComponentSize()` enforced via
  `LocalAppTouchTarget` on every interactive wrapper.
- Color contrast verified WCAG AA for all `colorScheme` pairs in current palette.
- Dynamic Color toggle in Settings → Preferences → Appearance.

### 7.2 File map

| Status | Path |
|---|---|
| Edit | `ui/mobile/account/PreferencesSheet.kt` — add `DynamicColor` toggle (visible only on Android 12+) |
| New | `app/src/main/res/values/strings.xml` entries `preferences.dynamic_color.label` and `preferences.dynamic_color.description` |
| Edit | `data/repository/PreferencesRepository.kt` — add `dynamicColor: Boolean` to the preferences model |
| Edit | `ui/dashboard/DashboardViewModel.kt` — propagate `dynamicColor` to `TastileTheme` via CompositionLocal |
| Edit | All wrappers under `ui/designsystem/` and `ui/mobile/designsystem/` to ensure `Modifier.semantics` is set |
| New | `tastile-android/docs/specs/m5-contrast.md` — contrast audit table |

### 7.3 Color contrast audit

Read existing `BrandColors.kt` / `grayColorScheme(dark)` and produce a
markdown table at `tastile-android/docs/specs/m5-contrast.md` with rendered
RGB pairs and AA verdict. Values failing AA are fixed in the same PR with
conservative token shifts (not a full palette redesign).

### 7.4 Completion KPI

- `MobileAccessibilityTest` (Compose UI) passes: every wrapper produces a node
  with `contentDescription` and a `Role`.
- WCAG AA contrast table complete; failing pairs (if any) fixed.
- Dynamic Color toggle round-trips Settings → main UI without process restart.

---

## 8. Test plan

**Unit (JVM):**

- `ThemeTest` — 8-cell matrix of `(themeMode × dynamicColor × dark)`.
- `TokenTest` — no `Color(0x` literals outside `BrandColors.kt` /
  `grayColorScheme`.
- `LocalAppTouchTargetTest` — wrappers refuse render below 48 dp.
- `LazyColumnKeyRuleTest`, `CollectAsStateRuleTest` — lint rule unit tests.

**Compose UI:**

- `AppPrimaryButtonTest`, `AppListItemTest`, `AppPickerButtonTest` —
  semantic + Role assertions.
- `TastileThemeSnapshotTest` — Robolectric + Paparazzi (or compose-ui-test
  screenshot capture) for the 6 fixture pairs in §3.4.

**Macrobenchmark:**

- `TimelineScreenDayScrollBenchmark.kt` — see §6.

**Manual on Pixel 6 emulator (440 dpi, baseline device per memory `feedback_verify_ui_in_browser.md`):**

Each Phase: tap through all primary surfaces (Quick create, side panel,
account sheet, Timeline Day/Week/Month, notifications). Screenshot before and
after; archived to `logs/{m1..m5}/`.

---

## 9. Migration / phasing

Single mainline branch; per-phase PRs (parity plan's "single branch + single
PR" cadence is preserved by treating each Phase as one logical PR):

| Phase | PR title (suggested) | Includes |
|---|---|---|
| M1 | `feat(android): add TastileTheme with Dynamic Color + brand/gray fallback` | TastileTheme, BrandColors, AppTypography cleanup, root composable migration |
| M2 | `feat(android): unify component wrappers + extend DesignSystem guard` | L0 conventions, guard extension, audit existing wrappers |
| M3 | `refactor(android): stabilize state for skippable recomposition` | collectAsStateWithLifecycle migration, LazyColumn keys, @Stable annotations |
| M4 | `perf(android): Timeline Macrobenchmark + drawBehind audit` | Benchmark test, Timeline tweaks, audit log |
| M5 | `feat(android): accessibility pass + Dynamic Color toggle` | A11y semantics, DC settings toggle, contrast table |

Parity safety (R1–R5) verified per PR via:

- `rg 'import androidx.compose.material3\.(Color|ColorScheme|Button|Card|FilledTonalButton|OutlinedButton|TextButton|AssistChip)' app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account}/` returns 0.
- Per-surface sanity check during PR review: control counts identical to web.
- New i18n keys are the two `preferences.dynamic_color.*` keys only.

---

## 10. Out of scope (deferred)

- Compose Multiplatform / desktop shared code.
- Material 3 Expressive animations (BOM dependency bump; separate spec).
- Wear / TV / foldables.
- `tastile-brands/` palette redesign (separate ownership).
- Iconography overhaul.
- `tastile-web` M3 work (separate spec).
- Splitting `DashboardViewModel` (parity plan keeps it as the coordination surface).
- New controls for parity-only screens.
- Per-locale `values-ja/strings.xml` (deferred until a localization milestone).

---

## 11. Open questions

- Should M3 typography `fontWeight` literals all collapse to `Typography`
  roles in M1 (this spec) or fully inside the MUI Misuse spec? Current
  recommendation: start in MUI Misuse for `ui/mobile/**`; this spec extends
  the cleanup to `ui/dashboard/**` in M1.
- Should the Macrobenchmark be limited to Day view, or expanded to Week and
  Month? Recommendation: Day view only at first; expand in a follow-up if
  Week/Month report jank after M4 ships.
- Should `tastile-brands/` iconography be ported into android theme assets
  alongside the M1 token layer, or held for a later spec? Recommendation:
  held for a later spec — current M3 scope is tokens and components, not
  iconography.
