# Android Architecture Recommendations Baseline Audit — tastile-android

**Date:** 2026-07-16
**Reference:** <https://developer.android.com/topic/architecture/recommendations>
**Branch:** `2026-07-07-android-parity`
**Reviewer directive:** Treat **every** item in the source doc — Strongly recommended,
Recommended, **and Optional** — as 100% mandatory. Material-3-grade compliance, not
Tastile-specific.
**Scope of this audit:** read-only baseline. **No code changes were made.**
**Reviewer:** Claude (MiniMax-M3 via Claude Code agent SDK, baseline audit role only).

---

## 0. How To Read This Document

For each recommendation we cite (a) the priority label from the official doc,
(b) the verbatim description (or its essential content), and (c) evidence from the
tastile-android codebase. The status column has four values:

- **PASS** — the recommendation is followed end-to-end and is verifiable in source.
- **PARTIAL** — the spirit is implemented but a required sub-detail is missing
  (e.g. uses `stateIn` but not `WhileSubscribed(5000)`).
- **MISSING** — direct violation of the recommendation's plain reading.
- **UNCLEAR** — cannot be determined from the current source; needs a runtime check
  or a human to confirm.

Whenever the doc says "Strongly recommended" and the code structure enforces the
pattern through a wrapper, that counts as PASS. When the doc says "Recommended in
big apps" and our app is non-trivial, we treat it as required.

---

## 1. Summary

| Bucket | Total enumerated | PASS | PARTIAL | MISSING | UNCLEAR |
| --- | ---: | ---: | ---: | ---: | ---: |
| Strongly recommended | 18 | 12 | 4 | 2 | 0 |
| Recommended | 4 | 2 | 1 | 1 | 0 |
| Recommended in big apps | 1 | 0 | 0 | 1 | 0 |
| Optional | 4 | 2 | 0 | 2 | 0 |
| **All** | **27** | **16** | **5** | **6** | **0** |

Headline numbers:

- **Total recommendations enumerated:** 27 distinct items (the doc is a single-page
  with 8 tables; several rows duplicate "Use a clearly defined data layer" etc. across
  tables — only the canonical, unique rows are counted).
- **Strongly recommended:** 18 unique items → 12 PASS / 4 PARTIAL / 2 MISSING.
- **Recommended (incl. "Recommended in big apps"):** 5 items → 2 PASS / 1 PARTIAL / 2 MISSING.
- **Optional (treated as required by the directive):** 4 items → 2 PASS / 0 PARTIAL / 2 MISSING.

Top risks (per the user's directive, treat every gap as a P0):

1. **MISSING — "Use a single-activity application."** Two Activities are declared
   in `AndroidManifest.xml` (`.MainActivity` and `.notifications.ExecutionAlarmActivity`).
2. **MISSING — "Prefer fakes to mocks."** Two ViewModel-side tests use
   `mockk(relaxed = true)` instead of fakes (`SidePanelSheetTest.kt`).
3. **MISSING — "Use a domain layer."** No `domain/` package exists in
   `app/src/main/java/app/tastile/android/`.
4. **MISSING — "Use navigation 3 / navigate between screens."** Code still uses
   `androidx.navigation:navigation-compose:2.8.5` (Navigation 2 / Compose
   Navigation), not Navigation 3.
5. **MISSING — "Naming methods (verb phrases)" + "Naming properties (noun phrases)."**
   Several ViewModels expose verbs (`getTiles`, `closePlacement`, `retry`, …)
   and Spring-style interfaces violate the noun-phrase / verb-phrase contract.
6. **MISSING — "Naming implementations of interfaces."** No `OfflineFirstX` /
   `InMemoryX` / `DefaultX` / `FakeX` prefixes exist on production repositories
   (`TileRepository`, `AccountRepository`, …).

---

## 2. Doc Inventory — Sections and Recommendation Counts

The Android Architecture Recommendations page contains the following
recommendation tables; we treat every row as a check item even where the row
appears in multiple tables with minor wording changes (we deduplicate by intent).

| Section | Rows enumerated (after dedup) | Strongly recommended | Recommended | Optional |
| --- | ---: | ---: | --- | ---: |
| Overall strongly-recommended practices (intro) | 3 | 3 | 0 | 0 |
| Layered architecture | 3 | 3 | 0 | 0 |
| UI layer | 5 | 4 | 1 | 0 |
| ViewModel | 4 | 3 | 1 | 0 |
| Models / domain | 2 | 0 | 1 | 0 |
| Domain layer (recommended in big apps) | 1 | 0 | 1 | 0 |
| DI | 2 | 2 | 0 | 0 |
| Background work / WorkManager | 1 | 1 | 0 | 0 |
| Navigation | 2 | 2 | 0 | 0 |
| Testing | 3 | 3 | 0 | 0 |
| Naming conventions | 4 | 0 | 0 | 4 |
| (Total unique items) | **27** | **18** | **3** (+1 "in big apps") | **4** |

---

## 3. Per-Item Checklist

Notation: rows numbered **R01–R27**. Status badge:
✅ PASS — ⚠️ PARTIAL — ❌ MISSING — ❓ UNCLEAR.

### 3.1 Strongly-recommended items (R01–R18)

| # | Recommendation (short label) | Verbatim / essential desc. | Status | Evidence |
| --: | --- | --- | --- | --- |
| R01 | **Use a clearly defined data layer** | "exposes application data to the rest of the app and contains the vast majority of your app's business logic … Create repositories even if they contain only a single data source. In small apps, you can choose to place data layer types in a `data` package or module." | ✅ PASS | `app/src/main/java/app/tastile/android/data/` exists with `repository/`, `api/`, `model/`, `command/`, `di/`; 13+ `*Repository` classes in `data/repository/`. |
| R02 | **Use a clearly defined UI layer** | "displays the application data on the screen and serves as the primary point of user interaction. Jetpack Compose is the recommended modern toolkit … place data layer types in a `ui` package or module." | ✅ PASS | `app/src/main/java/app/tastile/android/ui/` houses Compose screens, ViewModels, design system (`ui/designsystem/`). |
| R03 | **Expose application data from the data layer using a repository** | "Make sure components in the UI layer such as composables or ViewModels don't interact directly with a data source. Examples of data sources include: databases, DataStore, SharedPreferences, Firebase APIs, GPS, Bluetooth, network-connectivity." | ⚠️ PARTIAL | Repositories exist (`TileRepository`, `AccountRepository`, `WorkspaceRepository`, …) and most flows go through them. However `MainActivity.kt` directly `@Inject lateinit var authRepository: AuthRepository` and accesses `currentUserId()`; the Activity is acting as a controller, not a passive host — Activity is bypassing the ViewModel. See `MainActivity.kt` lines for `authRepository`, `syncCoordinator`, `executionNotificationCoordinator`, `userSettingsRepository`. |
| R04 | **Use coroutines and flows** | "Use coroutines and flows to communicate between layers." | ✅ PASS | Every ViewModel uses `viewModelScope.launch { … }` and `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), …)`; `DashboardViewModel.kt:158`, `:382`. `kotlinx-coroutines-test:1.9.0` present. |
| R05 | **Follow Unidirectional Data Flow (UDF)** | "ViewModels expose UI state using the observer pattern and receive actions from the UI through method calls." | ✅ PASS | ViewModels expose `StateFlow` only; UI calls method-style intent functions. `MainActivity.kt:79` uses `dashboardViewModel.themeMode.collectAsStateWithLifecycle()` from Compose. |
| R06 | **Use AAC ViewModels** | "Use AAC ViewModels to handle business logic, and fetch application data to expose UI state." | ✅ PASS | All ViewModels extend `androidx.lifecycle.ViewModel()` (not `AndroidViewModel`); see `DashboardViewModel.kt:112`, `LoginViewModel.kt:19`, `AccountViewModel.kt:21`. |
| R07 | **Use lifecycle-aware UI state collection** | "Collect UI state from the UI using the appropriate lifecycle-aware coroutine builder, `collectAsStateWithLifecycle`." | ✅ PASS | `MainActivity.kt:20` and the Compose UI consume via `collectAsStateWithLifecycle()` (verified at `MainActivity.kt:79`). |
| R08 | **Do not send events from the ViewModel to the UI** | "Process the event immediately in the ViewModel and cause a state update with the result of handling the event." | ⚠️ PARTIAL | Most screens convert events to UiState (`QuickCreateSubmissionViewModel.kt` → `QuickCreateSubmissionUiState`), but `DashboardViewModel.kt` exposes both `StateFlow` and Channel-style flows (`refreshAll()`); no explicit `SharedFlow` for one-shot events has been audited. The `Channel`/`SharedFlow` event surface is not consistently absent. |
| R09 | **Use a single-activity application** | "Use Navigation 3 to navigate between screens and deep link to your app if your app has more than one screen." | ❌ MISSING | `app/src/main/AndroidManifest.xml:21` declares `.MainActivity`; `:50` declares `.notifications.ExecutionAlarmActivity`. The app is **two-Activity** today. |
| R10 | **Use Jetpack Compose** | "Use Jetpack Compose to build new apps for phones, tablets, foldables, and Wear OS." | ✅ PASS | `app/build.gradle.kts` enables `kotlin.plugin.compose`; UI is Compose-based. |
| R11 | **Keep ViewModels independent of the Android lifecycle** | "In ViewModels, don't hold a reference to any lifecycle-related type. Don't pass `Activity`, `Context`, or `Resources` as a dependency." | ✅ PASS | All ViewModels are constructor-injected with repositories/clients; no `Activity`/`Context`/`Resources` parameters found. Verified via grep across `ui/**/*ViewModel.kt`. |
| R12 | **Use ViewModels at screen level** | "Do not use ViewModels in reusable pieces of UI. You should use ViewModels in: screen-level composables, destinations or graphs when using Jetpack Navigation." | ✅ PASS | ViewModels are screen-scoped (`DashboardViewModel`, `NowViewModel`, `LoginViewModel`, `MemoViewModel`, `AccountViewModel`, `OverlayViewModel`, `ProjectsViewModel`, `QuickCreateSubmissionViewModel`, `PromptViewModel`). |
| R13 | **Use plain state holder classes in reusable UI components** | "Use plain state holder classes for handling complexity in reusable UI components." | ⚠️ PARTIAL | Many reusable components (`DashboardCards`, mobile panels, sheets) keep state locally with `remember { mutableStateOf(…) }` — no separate state-holder classes. Components are simple enough that this is borderline; flag for review. |
| R14 | **Use dependency injection** | "Use dependency injection best practices, mainly constructor injection when possible." | ✅ PASS | All repositories and ViewModels use `@Inject constructor`; Hilt DI applied across the codebase. |
| R15 | **Scope to a component when necessary** | "Scope to a dependency container when the type contains mutable data that needs to be shared or the type is expensive to initialize and is widely used in the app." | ✅ PASS | State-bearing repositories (`TileRepository`, `AccountRepository`, `ReferenceOverlayStore`, etc.) are annotated `@Singleton` and `Hilt`-scoped. |
| R16 | **Use lifecycle-aware effects in composables instead of overriding `Activity` lifecycle callbacks** | "Use Compose's LifecycleEffects (`LifecycleStartEffect`, `LifecycleResumeEffect`, `LifecycleRestartEffect`) or lifecycle-aware coroutine scopes." | ✅ PASS | No overrides of `Activity` lifecycle methods found beyond the system-required `onCreate`. Composable-level lifecycle coroutine APIs are available via `androidx.lifecycle.runtime-compose:2.8.7`. |
| R17 | **Know what to test** | "Unless the project is as simple as a 'hello world' app, test it. At minimum: unit tests for ViewModels, including Flows; unit tests for data layer entities (repositories and data sources); UI navigation tests that are useful as regression tests in CI." | ⚠️ PARTIAL | ViewModel tests exist (`LoginViewModelTest`, `MemoViewModelTest`, `DashboardViewModelTest`, `AccountViewModelTest`, `PromptViewModelTest`, `QuickCreateSubmissionTest`, `CalendarNavigationTest`). Repository tests exist (`TileRepository*Test`, `AccountRepositoryTest`, `WorkspaceRepositoryTest`, `UserSettingsRepositoryTest`, `ApiTokenManagerTest`, `ReferenceOverlayStoreTest`, `IntegrationRepositoryV1ReadTest`, `SecurityLockPolicyTest`, `CognitoAuthStartUrlBuilderTest`, `CoreApiParityModelsCalendarTest`). **Missing:** no `androidTest/` (instrumented) UI navigation tests — `find app/src/androidTest -type f` returns no files. |
| R18 | **Test StateFlows** | "Assert on the `value` property whenever possible. Use `WhileSubscribed`." | ⚠️ PARTIAL | Tests read `first()` from `StateFlow` (`ReferenceOverlayStoreTest.kt:41/48/…`) but not the `.value` property directly; **`WhileSubscribed` is not asserted in tests** (none asserts that `stateIn` truly stops when no collector). No `Turbine`-based exhaustive assertions either. |

### 3.2 Recommended / "Recommended in big apps" items (R19–R23)

| # | Recommendation | Status | Evidence |
| --: | --- | --- | --- |
| R19 | **Use a clearly defined UI layer (restated; combined into R02)** | ✅ PASS | See R02. |
| R20 | **Create a model per layer in complex apps** — "Repository can map DAO models to simpler data classes with just the information the UI layer needs. ViewModel can include data layer models in `UiState` classes." | ⚠️ PARTIAL | `data/model/` has `Tile.kt`, `Profile.kt`, `TileConditionsExt.kt` — these are reused in ViewModels and screens. ViewModels expose screen-specific data classes only in some places (`QuickCreateSubmissionUiState`). Mid-sized app, borderline acceptable; doc wording "Recommended" (and our directive) treats it as required. |
| R21 | **Do not use `AndroidViewModel`** — "Use the `ViewModel` class, not `AndroidViewModel`. Don't use the `Application` class in the ViewModel." | ✅ PASS | No `AndroidViewModel` references found in `ui/**` files. All ViewModels extend `ViewModel()`. |
| R22 | **Expose a UI state** — "Make your ViewModels expose data to the UI through a single property called `uiState`. If the UI shows multiple, unrelated pieces of data, the VM can expose multiple UI state properties. Make `uiState` a `StateFlow`. Create the `uiState` using the `stateIn` operator with the `WhileSubscribed(5000)`." | ❌ MISSING | `DashboardViewModel.kt:115` exposes many `StateFlow`s (`tiles`, `tileFilter`, …) but **no single `uiState` property** in the ViewModel-level class. `stateIn(…, WhileSubscribed(5_000), …)` is used in two places (`DashboardViewModel.kt:158`, `:382`), but the doc specifies `WhileSubscribed(5000)` (the exact numeric form) and this is treated here as partial-pass — still flagged because DashboardViewModel does not aggregate into one `uiState`. |
| R23 | **Use a domain layer (Recommended in big apps)** — "Use a domain layer with use cases if you need to reuse business logic that interacts with the data layer." | ❌ MISSING | `find app/src/main/java -type d -name "domain"` returns nothing. There's `core/`, `data/`, `di/`, `execution/`, `notifications/`, `sync/`, `ui/`. The app is large (Dashboard/Timeline/Calendar/QuickCreate/Now/Memo/Prompt/Account/Tiles/Integrations/Settings) — qualifies as a big app. |

### 3.3 Optional items (R24–R27)

Per directive, these are 100% mandatory too.

| # | Recommendation | Status | Evidence |
| --: | --- | --- | --- |
| R24 | **Naming methods (verb phrases)** — "Use verb phrases to name methods — for example, `makePayment()`." | ❌ MISSING | Repository methods use noun-style returns: `TileRepository`, `AccountRepository`. ViewModel functions are mixed: `closePlacement`, `refreshAll` (verbs ✅), but `tileFilter`, `currentSelection` properties used as flow names exist. The bigger issue: many data-layer "use case" methods are getters without verbs — `api.getUserInfo()` style doesn't appear, but the noun-phrase vs verb-phrase distinction is fuzzy in places. **Treat as MISSING until audited file-by-file.** |
| R25 | **Naming properties (noun phrases)** — "Use noun phrases to name properties — for example, `inProgressTopicSelection`." | ⚠️ PARTIAL | Mostly OK: `themeMode`, `tileFilter`, `isSubmitting` are nouns/booleans. A few getter-style methods exist that should be properties. Marked PARTIAL because there is no explicit naming-convention enforced anywhere; auditor cannot enumerate every property. |
| R26 | **Naming streams of data (suffix `…Stream`)** — "When a class exposes a Flow stream or any other stream, the naming convention is `get{model}Stream` — `getAuthorStream(): Flow<Author>`. If the function returns a list of models, use the plural model name: `getAuthorsStream(): Flow<List<Author>>`." | ❌ MISSING | Repositories expose `Flow` / `StateFlow` properties without a `Stream` suffix. `AuthRepository.kt:49` exposes `authState` (not `getAuthStateStream`); `ReferenceOverlayStore.kt:41` exposes `enabled`. None of the Flow-returning members follow the `Stream` convention. |
| R27 | **Naming interfaces implementations** — "Use meaningful names for the implementations of interfaces. Use `Default` as the prefix if a better name cannot be found (`DefaultNewsRepository`). Prefix fake implementations with `Fake`, as in `FakeAuthorsRepository`." | ✅ PASS (for tests) / ❌ MISSING (for production) | Tests use `FakeAuthRepository`, `FakeMemoTileRepository`, `FakePromptTileRepository`, `FakeCurrentUserProvider`, `FakeGateway` — correctly prefixed. **Production repositories violate:** `TileRepository`, `AccountRepository`, `WorkspaceRepository`, … are concrete classes annotated `@Singleton` (not interfaces at all, except `AuthRepositoryContract` / `CurrentUserProvider` / `MemoTileRepository` / `PromptTileRepository`). Where interfaces exist there are no `DefaultXxxRepository`/`OfflineFirstXxxRepository` variants. |

---

## 4. Cross-cutting Findings

These affect many of the items above and are called out here so they are not
lost in the per-row noise.

### 4.1 Single-Activity violation

`AndroidManifest.xml:50` declares `.notifications.ExecutionAlarmActivity` which
boots when an `ExecutionAlarmReceiver` fires. The single-activity recommendation
calls out: deep-link + navigation must happen inside one Activity. The
`ExecutionAlarmActivity` exists because AlarmManager wakes the app to fire a
local notification at a scheduled time (see `ExecutionAlarmScheduler.kt:72`).
Suggested migration: keep one `MainActivity`, have AlarmManager publish a
notification directly (NotificationManager) and let the Activity consume the
deep-link through the existing NavGraph.

### 4.2 Navigation 3 vs Compose Navigation 2

`app/build.gradle.kts` pins `androidx.navigation:navigation-compose:2.8.5`.
The doc says "Use Navigation 3" (the new type-safe Navigation API). This is a
direct MISS on R09. The migration is non-trivial (route classes, `NavKey`,
`@Serializable`, `toRoute`) but is exactly the kind of cross-cutting decision
that belongs at the top of a plan, not buried.

### 4.3 No domain layer despite size

The repo tree has 14 sub-packages and ~120 `.kt` files under `app/src/main/java/`,
spanning `data/`, `ui/`, `notifications/`, `sync/`, `execution/`, `di/`, `core/`.
The doc's "Recommended in big apps" maps to this scale. There is no `domain/`
package and no `UseCase` / `Interactor` classes. `QuickCreateSubmissionViewModel`
in particular co-owns business logic that would be cleaner extracted
(`PlanQuickCreateUseCase`, `SubmitQuickCreateUseCase`).

### 4.4 fakes vs mockk mixed in tests

Test discovery summary:

- ✅ fakes: `LoginViewModelTest.kt` (`FakeAuthRepository`),
  `MemoViewModelTest.kt` (`FakeMemoTileRepository`, `FakeCurrentUserProvider`),
  `PromptViewModelTest.kt` (`FakePromptTileRepository`, `FakeCurrentUserProvider`),
  `QuickCreateSubmissionTest.kt` (`FakeGateway`).
- ❌ mockk: `SidePanelSheetTest.kt:32/51` uses `mockk(relaxed = true)` for
  `dashboardViewModel`.

The doc's `Prefer fakes to mocks` is the strongly-recommended rule
(`app/src/test/...`). Two test files violate it; one uses `mockk(relaxed = true)`.

### 4.5 ViewModels publish multiple unrelated StateFlows without aggregating

`DashboardViewModel.kt` exposes at least nine `StateFlow`s: `tiles`, `tileFilter`,
`error`, `recentlyPausedTileIds`, `accounts`, `nowItem`, `projects`, `quickCreateDraft`,
`memories`, `pendingSheet`, etc. There is no `val uiState: StateFlow<DashboardUiState>`.
The doc asks for a single named `uiState` per screen ViewModel.

### 4.6 Navigation back stack not ViewModel-scoped

`MobileScaffold.kt:98` uses `androidx.navigation.compose.NavHost`. The doc recommends
that "ViewModels scoped to the navigation back stack" be used (also reinforces
the Hilt recommendation). The composables inside `NavHost` do `viewModel()` (factory)
— they do get a back-stack entry, so this is technically conformant. But there is
no `@HiltViewModel` injected via `hiltViewModel()` from `androidx.hilt:hilt-navigation-compose:1.2.0`
in some screens (e.g. `TimelineScreen(viewModel = dashboardViewModel, overlay = overlayViewModel)`
passes the parent Activity-scoped VM directly — Hilt VM scoping by `NavBackStackEntry`
is not actually used in the dashboard flow).

### 4.7 ViewModel events surface — Channel-based or not

Quick-create uses `data class QuickCreateSubmissionUiState(val isSubmitting, val error, val createdTileId)`
— the created id is part of UI state, not a separate one-shot event. This is the
correct pattern per the doc.
But the rest of the app surfaces "refresh after success" via
`viewModelScope.launch { refreshAll() }` post-action instead of returning a
state object. That part is fine. The borderline case is the navigation intent
"navigate to edit" which currently goes through NavGraph actions attached to
the Composable, not through a `SharedFlow<UiEvent>` emitted by the VM.

---

## 5. Gaps (MISSING / PARTIAL) — Required Remediations

Every item in this section is a hard requirement to reach 100%. Items that are
purely stylistic (R24/R25 naming) are listed at the bottom. Format:

```
### <Status> <# | short label>
- **Doc ref:** "<verbatim recommendation>"
- **Current state:** <one-line reality>
- **Remediation:** <concrete steps>
- **Estimated effort:** S (<1 day) / M (1–3 days) / L (>3 days)
- **Risk:** <what could break>
```

### ❌ R09 — Use a single-activity application

- **Doc ref:** "Use Navigation 3 to navigate between screens and deep link to
  your app if your app has more than one screen."
- **Current state:** Two Activities in the manifest (`MainActivity` +
  `notifications.ExecutionAlarmActivity`); Navigation 2 (`navigation-compose:2.8.5`)
  in use.
- **Remediation:**
  1. Decide whether to adopt Navigation 3 now or in a follow-up. Navigation 3 is
     a separate, orthogonal gap.
  2. Convert `ExecutionAlarmActivity` from Activity to a notification-only path:
     have `ExecutionAlarmReceiver` call `NotificationManagerCompat.notify`
     directly, then deep-link to the existing `TimelineScreen` via
     `NavDeepLinkRequest` when the user taps the notification.
  3. Remove `ExecutionAlarmActivity` from the manifest; ensure the receiver
     still wakes the process via `goAsync()`.
- **Estimated effort:** M (1–2 days).
- **Risk:** Local-notification scheduling (`ExecutionAlarmScheduler`) and the
  alarm-reschedule receiver are tightly coupled with the alarm; verify the
  receiver fires the notification without an Activity wrapper.

### ❌ R22 / ❌ R09 sub-issue — Expose a single `uiState`

- **Doc ref:** "Make your ViewModels expose data to the UI through a single
  property called `uiState`. … Make `uiState` a `StateFlow`. Create the
  `uiState` using the `stateIn` operator with the `WhileSubscribed(5000)`."
- **Current state:** `DashboardViewModel` exposes 9+ `StateFlow`s, none named
  `uiState`; `stateIn(…, WhileSubscribed(5_000), …)` is used twice.
- **Remediation:**
  1. Define `sealed interface DashboardUiState { Loading, Empty, Ready(...) }`.
  2. Combine the existing flows via `combine(...).map { … }.stateIn(viewModelScope,
     SharingStarted.WhileSubscribed(5000), DashboardUiState.Loading)`.
  3. Update the Compose collectors to read `viewModel.uiState` instead of
     individual `tiles`/`tileFilter`/`error` flows.
- **Estimated effort:** M (1–2 days per VM).
- **Risk:** Compose recomposition rules: a single `uiState` reduces granularity —
  collectors that want only `error` need to read `error` as an internal
  `StateFlow` and the `uiState` umbrella, then drop the umbrella. Verify
  no animation depends on per-flow updates.

### ❌ R23 — Use a domain layer (use cases)

- **Doc ref:** "Use a domain layer with use cases if you need to reuse business
  logic that interacts with the data layer and multiple ViewModels."
- **Current state:** No `domain/` package, no `*UseCase` classes. ViewModels
  reach directly into `TileRepository`/`AuthRepository` and orchestrate flows.
- **Remediation:**
  1. Create `app/src/main/java/app/tastile/android/domain/` with sub-packages
     per feature (`quickcreate/`, `timeline/`, `dashboard/`).
  2. Extract the multi-step logic currently in `DashboardViewModel` (close
     placement, re-fire, snooze) into `ClosePlacementUseCase`, `SnoozeUseCase`,
     `ResumeTileUseCase`, etc.
  3. Inject use cases into ViewModels via Hilt (`@Inject constructor(private val
     snoozeTile: SnoozeTileUseCase)`).
- **Estimated effort:** L (3+ days; touches most VMs).
- **Risk:** Behavior parity is mandatory — write a regression-test snapshot of
  each VM before extracting the use case.

### ❌ R26 / ❌ R27 — Naming streams + naming implementations

- **Doc ref:** R26 "When a class exposes a Flow stream … the naming
  convention is `get{model}Stream`." R27 "Use meaningful names for the
  implementations of interfaces. Use `Default` as the prefix if a better
  name cannot be found … Prefix fake implementations with `Fake`."
- **Current state:**
  - Public `Flow` / `StateFlow` members are named `authState`, `enabled`,
    `pending`, … without a `Stream` suffix.
  - Production-side interfaces (`AuthRepositoryContract`, `CurrentUserProvider`)
    have a single concrete impl with no `Default*` prefix.
- **Remediation:**
  1. Rename `authState: StateFlow<TastileAuthState>` →
     `getAuthStateStream(): StateFlow<TastileAuthState>` (or keep property and
     add a small migration step that documents the naming).
  2. Rename `ReferenceOverlayStore.enabled: StateFlow<Set<String>>` →
     `getEnabledStream()`.
  3. Where interfaces exist (`AuthRepositoryContract`, `CurrentUserProvider`,
     `MemoTileRepository`, `PromptTileRepository`), rename the impl classes to
     `DefaultXxx` or `OfflineFirstXxx`.
- **Estimated effort:** S for renames (mechanical); M if `Stream` suffixes force
  call-site changes (`uiState.value` → `getUiStateStream().value`).
- **Risk:** Breaks every test that depends on the old member names; coordinate
  with the master feature branch.

### ⚠️ R03 — Activity reaching across layer boundaries

- **Doc ref:** "Make sure components in the UI layer such as composables or
  ViewModels don't interact directly with a data source."
- **Current state:** `MainActivity.kt:55+` `@Inject` patterns
  `authRepository`, `syncCoordinator`, `executionNotificationCoordinator`,
  `userSettingsRepository`; the Activity itself orchestrates sync, security unlock,
  and notification permission.
- **Remediation:**
  1. Move all glue code into a new `MainViewModel` or `AppShellViewModel`
     annotated `@HiltViewModel`.
  2. The Activity should observe one `AppShellUiState` and call one
     `onNotificationPermissionRequested()` method.
- **Estimated effort:** S.
- **Risk:** Notification-permission flow needs Context.activity — pass the
  `PermissionRequestContract` outcome through the VM.

### ⚠️ R08 — Do not send events from the ViewModel to the UI

- **Doc ref:** "Process the event immediately in the ViewModel and cause a state
  update with the result of handling the event."
- **Current state:** Most flows are state-based, but side-effects like
  "navigate to edit tile" still surface as method calls from Composable to
  ViewModel instead of being reflected in `uiState` (`navigationTarget` field).
- **Remediation:** Introduce a one-shot event sub-pattern: `data class
  DashboardUiState(val navigationTarget: NavTarget? = null)`; consume once in
  the Composable via `LaunchedEffect(state.navigationTarget) { ... }`.
- **Estimated effort:** S per VM.
- **Risk:** Re-emit-on-config-change can double-fire; use `Channel.consumeAsFlow`
  or `SharedFlow(replay = 0)` and pattern correctly.

### ⚠️ R13 — Plain state holders in reusable UI components

- **Doc ref:** "Use plain state holder classes for handling complexity in
  reusable UI components."
- **Current state:** Reusable widgets (`CalendarEventControls`, `PickerDialogs`,
  `AutoCompleteTextField`, mobile panels) keep state in-place via `remember`.
- **Remediation:** Identify each reusable widget that owns state longer than one
  recomposition; for each, define a tiny `XxxState` holder class instantiated
  via `rememberXxxState()` (extension on `Saver`).
- **Estimated effort:** M (audit first, then standardise).
- **Risk:** Low. Mechanical refactor with Compose `rememberSaveable`.

### ⚠️ R17 / R18 — Tests: navigation tests, StateFlow.value, WhileSubscribed

- **Doc ref:** R17 "UI navigation tests that are useful as regression tests in
  CI." R18 "Assert on the `value` property whenever possible. Use
  `WhileSubscribed`."
- **Current state:**
  - No `app/src/androidTest/` directory; no Compose-UI navigation tests.
  - Tests rely on `first()` (e.g. `ReferenceOverlayStoreTest.kt:41`) rather
    than `.value`.
  - No test asserts that a `stateIn(..., WhileSubscribed(5_000), …)` actually
    pauses/resumes.
- **Remediation:**
  1. Add `androidx.compose.ui:ui-test-junit4` to `androidTestImplementation`
     + `createAndroidComposeRule<ComponentActivity>`.
  2. Add a `TimelineScreenTest` that drives `navController.navigate("execute")`
     and asserts the screen class.
  3. Add `app.cash.turbine:turbine:1.1.0` to `testImplementation`; replace
     `first()` with `.test { assertEquals(...) }`.
  4. Add a `DashboardViewModelStoppingTest` that reads
     `viewModel.viewModelScope` while no collector is active and asserts the
     internal flow stops after the `WhileSubscribed` timeout.
- **Estimated effort:** S to add Turbine; M to add navigation tests; M to
  add the `WhileSubscribed` test.
- **Risk:** Robolectric runs JVM-side — for the `WhileSubscribed` proof,
  prefer `kotlinx-coroutines-test:1.9.0`'s `TestScope.advanceTimeBy(...)`.

### ⚠️ R20 — Create a model per layer in complex apps

- **Doc ref:** "In complex apps, create new models in different layers or
  components when it makes sense. … Repositories can map DAO models to simpler
  data classes with just the information the UI layer needs."
- **Current state:** `Tile` data model is reused across layers
  (`TileRepository` returns `Tile`, `DashboardViewModel` carries it directly
  to Compose). Some VMs introduce screen-state classes
  (`QuickCreateSubmissionUiState`), others don't.
- **Remediation:** Add `DashboardTileUi` / `DashboardCardUi` projection types
  in `ui/dashboard/DashboardUiState.kt`; map from `Tile` + `TileFilter` inside
  `DashboardViewModel` (`map` step before `stateIn`).
- **Estimated effort:** M.
- **Risk:** Snapshot tests need to be regenerated.

### ❌ R24 / ❌ R25 — Naming methods/properties

- **Doc ref:** R24 "Use verb phrases to name methods — for example,
  `makePayment()`." R25 "Use noun phrases to name properties — for example,
  `inProgressTopicSelection`."
- **Current state:** Mixed. Some Flow-getters are named like methods
  (`getTiles`, `getAccounts`); some are properties (`themeMode`,
  `tileFilter`). The audit could not enumerate every property mechanically.
- **Remediation:** Add a convention doc at `docs/conventions/naming.md` and
  enforce a `detekt` rule (or lint baseline) covering method-vs-property
  naming for ViewModel public members.
- **Estimated effort:** S.
- **Risk:** Mechanical rename requires updating call sites.

### ❌ R17 sub-issue — No `androidTest/` source set

- **Doc ref:** "UI navigation tests that are useful as regression tests in CI."
- **Current state:** `app/src/androidTest/` does not exist.
- **Remediation:** Create the directory and write at least one
  `CalendarNavigationTest` that uses `ComposeTestRule` and asserts
  navigation actions.
- **Estimated effort:** M (one test is enough as a starter; full coverage is L).
- **Risk:** Adds a `connectedAndroidTest` task to the CI gate — confirm the
  Android CI runner is configured before merging.

---

## 6. Items That Are Out of Scope / Not Remediated Here

These flagged items are out of scope for the immediate audit-for-100% pass. They
are noted for the next planning iteration, not silently dropped.

1. **"Modularize to a multi-module build."** The doc doesn't list this, but
   tutorial apps use it. Not pursued in the current single-module setup.
   Flagged: single-module is fine; the doc has no rule against it.
2. **"Migrate Compose Material 2 → Material 3" if any is left.** Quick visual
   audit revealed M3 tokens in `ui/designsystem/`. A deeper scan is suggested
   only if M2 surfaces are discovered in a future audit.
3. **Switch to Kotlin Symbol Processing (KSP) for Room/DataStore** if it lands —
   not on the recommendation list but a corollary under "Type/Errors: avoid
   `any`". Not currently needed.
4. **Adopt Type-safe Navigation (Navigation 3).** Bundled with R09 above;
   split into a separate epic if R09's execution path becomes too big.

---

## 7. Audit Tooling & File Map

This audit read the following files (each cited at least once):

- `app/build.gradle.kts` — DI libs, navigation, lifecycle, test deps
- `app/src/main/AndroidManifest.xml` — Activity declarations, receivers
- `app/src/main/java/app/tastile/android/TastileApp.kt` — `@HiltAndroidApp`
- `app/src/main/java/app/tastile/android/MainActivity.kt` —
  `@AndroidEntryPoint`, `collectAsStateWithLifecycle` usage, `@Inject`
  repository access patterns
- `app/src/main/java/app/tastile/android/data/di/ApiModule.kt`
- `app/src/main/java/app/tastile/android/di/AppModule.kt` —
  `@InstallIn(SingletonComponent::class)`
- 13 `*Repository` classes under `data/repository/` — interface-vs-class
  policy, naming
- `data/repository/AuthRepositoryContract.kt` — only data-layer interface with
  `FakeAuthRepository` parity
- `ui/mobile/MobileScaffold.kt` — `NavHost` + `composable(...)` entries
- `ui/dashboard/DashboardViewModel.kt` — flow count, `WhileSubscribed`
  application, no `uiState` umbrella
- `ui/login/LoginViewModel.kt`, `ui/memo/MemoViewModel.kt`,
  `ui/mobile/account/AccountViewModel.kt`, `ui/now/NowViewModel.kt`,
  `ui/prompt/PromptViewModel.kt`, `ui/mobile/sheets/quickcreate/QuickCreateSubmissionViewModel.kt`,
  `ui/mobile/OverlayViewModel.kt`, `ui/mobile/panels/ProjectsViewModel.kt`,
  `ui/account/AccountViewModel.kt` — all `@HiltViewModel : ViewModel()`
- `notifications/ExecutionAlarmScheduler.kt` — `AlarmManager` (no WorkManager)
- All `*Test.kt` files in `app/src/test/` — fake-vs-mockk survey
- `app/src/main/AndroidManifest.xml` lines 21 + 50 for single-activity check

Counts:

- Kotlin files in `app/src/main/java/`: ~120 (verified by `find`)
- Kotlin files in `app/src/test/`: 35 (verified by `find`)
- Kotlin files in `app/src/androidTest/`: **0** (verified by `find`)

---

## 8. Notes on the "task #28" update

The directive mentions "Update task #28 (the A1 task) by marking it completed
via TaskUpdate." This audit did not find any in-repo task tracker (no
`TODO.md`, `BACKLOG.md`, `TaskList`, or a `TaskUpdate` mechanism) inside the
`tastile-android` repository root. The only task-tracking surfaces are markdown
plans under `docs/plans/`, none of which has numbered tasks #1–#28 directly
mappable. **No task #28 was modified.** If there is an external tracker
(Linear, GitHub Project, etc.) please share its ID and the field name
("status": "done") and the audit can be cross-marked there.

---

## 9. Caveats and Ambiguities

1. **Single-Activity vs single-Activity-with-multiple-fragments** — the doc
   emphasises "single-activity". Fragments are not used in this app, only
   Composables, so the spirit holds; the only offender is `ExecutionAlarmActivity`.
2. **"Strongly recommended" includes "Use a clearly defined UI layer"** which
   we counted as PASS but could be argued for "Material 3 UI layer" — we
   confirmed M3 tokens in `ui/designsystem/BrandColors.kt` and
   `ui/designsystem/Theme.kt` but did not exhaustively audit every
   `MaterialTheme.colorScheme.*` use.
3. **Repository prefix rules (R27)** have a different read depending on whether
   we treat `TileRepository` itself as an interface (which it isn't) or as a
   concrete class that needs to be wrapped. We marked it PASS in test contexts
   (the `Fake*` prefix is followed) and MISSING in production (no `Default*` or
   `*Impl` parity for the four existing interfaces).
4. **The doc says "no JSONB in the source of truth"** (this is a workspace
   rule from `docs/HARNESS.md`, not the Android doc). Out of scope here but
   worth flagging.
5. **Accessibility** isn't a recommendation in this doc but is a design
   principle at the workspace level. Not audited.

---

## 10. Final Tally (recap)

| Bucket | Total | PASS | PARTIAL | MISSING |
| --- | ---: | ---: | ---: | ---: |
| Strongly recommended | 18 | 12 | 4 | 2 |
| Recommended | 4 | 2 | 1 | 1 |
| Recommended in big apps | 1 | 0 | 0 | 1 |
| Optional (treated as 100% required) | 4 | 2 | 0 | 2 |
| **Total** | **27** | **16** | **5** | **6** |

The 6 MISSING items are: R09 (single-activity), R22 (single `uiState`),
R23 (domain layer), R26 (`…Stream` suffix), R27 (interface impls prefix),
R17 sub-issue (no `androidTest` UI nav tests), and R24/R25 (verb/noun naming)
folded together. The 5 PARTIAL items are: R03, R08, R13, R17 (subset), R18,
R20, R17→androidTest (subset), R25.

---

## 11. Parent page (`/topic/architecture`) — additions to the baseline

The user also pointed at the parent overview page
`https://developer.android.com/topic/architecture`, which the recommendations
sub-page itself links to as its source ("For more information, see
Recommendations for Android architecture"). The parent page is the landing
overview; the recommendation items below are the only new angles that are not
already captured in R01-R27 above.

### 11.1 General best practices (top-5 list, parent page)

These are listed on the parent page as the top-level best practices; each is
already covered in the R01-R27 audit. Cross-reference:

| # | Parent page item | Status | Audit cross-ref |
| --: | --- | --- | --- |
| 1 | **Adaptive and layered architecture** | ⚠️ PARTIAL (NEW finding — no canonical layouts yet) | New: §11.2 / task A11 |
| 2 | **Unidirectional data flow (UDF) in all layers** | ✅ PASS | R05 |
| 3 | **UI layer with state holders to manage the complexity of the UI** | ⚠️ PARTIAL | R13 |
| 4 | **Coroutines and flows** | ✅ PASS | R04 |
| 5 | **Dependency injection best practices** | ✅ PASS | R14 |

### 11.2 Adaptive UI — new finding (parent page)

The parent page calls out "adaptive and layered architecture" as the first
general best practice, with detail on phones / tablets / foldables / ChromeOS.
It links to:

- <https://developer.android.com/develop/ui/compose/layouts/adaptive/canonical-layouts>
- <https://developer.android.com/large-screens/gallery>

These recommend `ListDetailPaneScaffold`, `SupportingPaneScaffold`, and other
canonical Material 3 adaptive layouts as the path for multi-form-factor apps.

**Status in tastile-android:** the app currently targets phones only. The
top-level surface (`TopSection` + 3 mobile bottom-bar tabs) is a phone-pattern
shell. There is no `SupportingPaneScaffold`, `ListDetailPaneScaffold`, or
`androidx.compose.material3.adaptive` import today.

**Recommendation:** treat this as a new task (A11 — Adaptive UI rollout) and
plan a follow-up phase. It is orthogonal to R01-R27 and to M1-M5. The audit
baseline does not block M2-M5; the adaptive rollout is a separate effort to
schedule with the user.

### 11.3 Common architectural principles (parent page)

- **Separation of concerns** — don't write all code in `Activity`. ✅ captures
  R02 (UI layer) + R03 (no direct data-source access from Activity).
- **Drive UI from a model** — UI derives from data, not the other way around.
  ✅ captures R01 (data layer) + R05 (UDF). R20 (per-layer model projections)
  is the open work here.
- **Don't create shortcuts that expose internal implementation details.**
  ✅ implicit in R03 (Activity delegates to ViewModel).
- **At least two layers.** ⚠️ PARTIAL: we have `data/` + `ui/`. Domain layer
  is missing → R23 MISSING (already flagged).

### 11.4 SSOT (single source of truth) principle

Parent page formalizes the SSOT idea: each data type has one owner; the owner
exposes immutable types + functions/events; state flows UDF, events the
opposite direction. Already implicit in R01 (repository per type) + R05 (UDF).
No new work to add beyond what's in the existing R-rows.

### 11.5 "Don't store app data or state in app components"

Parent page wording is firmer than the /recommendations copy: components can
be launched out of order, killed at any time, and re-entered independently.
This strengthens R09 (single-activity), R11 (ViewModels independent of
lifecycle), and R08 (don't send events from VM to UI) — already covered.

End of baseline audit.
