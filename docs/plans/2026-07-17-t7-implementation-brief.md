# T7 Implementation Brief — v37 Timeline Frame/Tile Split

**Audience:** T7 implementer. **Branch:** `2026-07-07-android-parity`. **Working dir:** `tastile-android/` (no worktree).

---

## 1. Existing benchmark inventory

`app/src/androidTest/java/app/tastile/android/benchmark/TimelineBenchmark.kt` (file present, 161 lines):

- `@Test fun timelineDayScrollFrameTimeP95()` — vertical scroll workload on Day view. 10 iterations, WARM startup, FrameTimingMetric. Day-mode toggle, Today snap, three DOWN/UP sweeps on the scrollable container.
- `@Test fun timelineDayHorizontalPagerFrameTimeP95()` — horizontal swipe between day pages via shared `HorizontalPager`. 10 iterations, scroll LEFT/RIGHT x2.

Run via: `./gradlew :app:pixel6Benchmark -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.benchmark.TimelineBenchmark`. Output JSON drops to `app/build/outputs/connect_android_test_additional_output/`.

**KPI gates already instrumented:** `timelineDayScrollFrameTimeP95`, `timelineDayHorizontalPagerFrameTimeP95` — these are the v37 KPI gate per design §3 "Benchmark gate". No new benchmark methods are required by the plan.

**Missing gap:** No pre-v37 baseline number has been captured yet. T7 must record `main` branch's same metrics BEFORE the T6 merge lands (or at minimum, alongside the v37 run with a known delta).

---

## 2. New Compose UI tests — coverage status (per parallel T3 / T3.5 / T4 / T5 work)

| Test class | Owner task | Path | Status (2026-07-17 scan) |
| --- | --- | --- | --- |
| `DayViewFrameTest` | T3.5 | `app/src/test/java/app/tastile/android/ui/mobile/calendar/DayViewFrameTest.kt` | **EXISTS** (Robolectric, 4 methods: root+canvas, height=1440.dp, 5 pixel samples, tap→(hour,15min)) |
| `DayViewTileTest` | T3 | `app/src/test/java/app/tastile/android/ui/mobile/calendar/DayViewTileTest.kt` | **EXISTS** (Robolectric, 1 method: empty-blocks root displayed) |
| `NowIndicatorTest` | T2 | `app/src/test/java/app/tastile/android/ui/mobile/calendar/NowIndicatorTest.kt` | **EXISTS** (Robolectric, 2 methods incl. JST zone math) |
| `GridConstantsTest` | T1 | `app/src/test/java/app/tastile/android/ui/mobile/calendar/GridConstantsTest.kt` | **EXISTS** (pure JVM, 7 methods) |
| `WeekViewFrameTest` | T4 | `app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/` | **MISSING** — T7 must add |
| `WeekViewTileTest` | T4 | same | **MISSING** — T7 must add |
| `MonthViewFrameTest` | T5 | same | **MISSING** — T7 must add |
| `MonthEventIndicatorTest` | T5 | same | **MISSING** — T7 must add |
| `TimelineScreenLoadingTest` | T7 | same | **MISSING** — T7 must add |

**Note:** Day tests landed in JVM-only (`app/src/test/.../`) under Robolectric, not `androidTest/`. The plan's original §3 specified `androidTest/` location. T7 should either (a) mirror them to `androidTest/` for `AndroidJUnit4`, or (b) document the deviation in the PR description and rely on Robolectric for Day coverage. Recommend option (b) — the Robolectric versions already exercise semantics + pixel sampling, which is strictly more coverage than a plain `androidTest` smoke test.

**Gaps the T7 implementer must fill:**
- Week/Month Compose UI tests (currently no test files at all — both Frame and Tile halves need new tests).
- `TimelineScreenLoadingTest` — proves Frame renders with empty timeline (core "Frame-first" assertion).

---

## 3. New JVM tests to add

### a. `DayViewRefreshSnapshotTest` — Robolectric, verifies VM preserves prior snapshot on fetch failure

**File path:** `app/src/test/java/app/tastile/android/ui/mobile/calendar/DayViewRefreshSnapshotTest.kt`
**Class:** `class DayViewRefreshSnapshotTest`
**Annotation:** `@RunWith(RobolectricTestRunner::class)` (matches sibling DayView* tests; Hilt direct JVM instantiation is fragile without `HiltAndroidRule`).
**Method:** `@Test fun timeline_preservesPriorValue_onFetchError()`

**Pseudo-code structure:**
1. Construct a `DashboardViewModel` directly via its no-arg or Hilt-free constructor (or via `HiltAndroidRule` if the constructor requires it — fall back to Robolectric + manual injection).
2. Seed `viewModel.replaceTilesForTest(initialList)` (or however T3/T4's VM exposes seed — confirm by reading `DashboardViewModel`).
3. Provide a `TileRepository` stub whose `getTimeline()` throws `RuntimeException("net down")`.
4. Trigger the refresh path that T6 settled on — `setOwnerFilter(...)` or `refreshAll()`. Await via `viewModel.timeline.first()`.
5. `assertEquals(initialList, valueAfter)`.

**No new Hilt wiring beyond `HiltAndroidRule` if needed.** Robolectric already covers `DashboardViewModel.refreshTimeline()` (lines 574–588 are unchanged; the `try` block already preserves `_timeline` on failure — the test pins that behavior).

### b. `PxPerMinTest` — pure-function assertions

**File path:** `app/src/test/java/app/tastile/android/ui/mobile/calendar/PxPerMinTest.kt`
**Class:** `class PxPerMinTest` (no Robolectric, no Android — pure JVM, `@RunWith` not required).

**Methods (illustrative — implementer may add more):**
- `zoomMin_saturates_toDayOnScreen` — at `zoom = ZOOM_MIN = 1f`, `pxPerMin` should size the day to fit the available height (with SCROLL_BUFFER_MIN).
- `zoomMax_keepsBlocksVisible` — at `zoom = ZOOM_MAX = 6f`, `pxPerMin` should be 6× the baseline (allowing block rectangles up to 6× larger per minute).
- `dayRange_forcedTo0..24` — out-of-range `availableHeightDp` does not change the 24h-constant.
- `availableHeightOver1440_baseline` — `pxPerMin` at `ZOOM_DEFAULT` equals `availableHeightDp / 1440` (matches v34 logic from `TimelineScreen.kt`).

**Structure:** `PxPerMin` becomes an `internal` top-level (or `@VisibleForTesting`) in `DayView.kt`. Pseudo-code:
```
fun computePxPerMin(zoom, availableHeightDp, density) -> Float
Tests call computePxPerMin(...) directly and assert bounds + linearity invariants.
```
No Hilt, no Robolectric.

---

## 4. Benchmark strategy

**Baseline methodology:** Run the existing `TimelineBenchmark` macrobenchmark under identical conditions on `main` (pre-merge) and on `2026-07-07-android-parity` (post-merge of T1..T6). `FrameTimingMetric` over 10 iterations, `CompilationMode.DEFAULT`, `StartupMode.WARM`. Read the `FrameTimingP95` value from each JSON report under `app/build/outputs/connect_android_test_additional_output/`.

**Pre-v37 capture (BLOCKER before T6 ships):** Before Task 6 deletes the moved Composables from `TimelineScreen.kt`, the implementer must run the benchmark on a temporary commit of `2026-07-07-android-parity` that contains only T1..T5 (additive file creation, no deletion of `DayContentLayer`). Record `baseline_p95_ms` in the PR description.

**Threshold:** v37 accepted if `v37_p95_ms ≤ baseline_p95_ms × 1.05` (i.e., regression budget is +5%). Regression → revert T6, ship v37 cut at T5, schedule v38 follow-up per design §4.

**CI placement:** No CI workflow file currently invokes the benchmark (`grep "pixel6Benchmark" .github/workflows/` returns nothing). **Recommendation for T7:** Add a non-blocking `workflow_dispatch` job that runs the benchmark on the emulator runner and uploads the JSON as a PR artifact. Wire it as informational only — do NOT gate merges on the benchmark number yet (insufficient emulator capacity + cold-build variance). Note this in the PR under "Open questions".

---

## 5. PR checklist

- **Branch:** `2026-07-07-android-parity` (not main, no worktree — implementer follows the plan's note).
- **Expected commits (in order):**
  1. `refactor(android): extract calendar grid constants to GridConstants.kt` (T1)
  2. `refactor(android): extract NowIndicator to dedicated Composable` (T2)
  3. `refactor(android): split Day view into DayViewFrame + DayViewTile` (T3)
  3a. (T3.5 cleanup commits if DayViewFrame/Tile land in separate passes)
  4. `refactor(android): split Week view into WeekViewFrame + WeekViewTile` (T4)
  5. `refactor(android): split Month view into MonthViewFrame + MonthEventIndicator` (T5)
  6. `refactor(android): delete moved Composables from TimelineScreen.kt` (T6)
  7. `test(android): add Frame/Tile split coverage (loading, snapshot, pxPerMin)` (T7)
- **PR title:** `feat(android): v37 Timeline Frame/Tile split (Day/Week/Month)`
- **PR description:**
  - Links: design (`docs/plans/2026-07-17-timeline-frame-tile-split-design.md`), plan (`docs/plans/2026-07-17-timeline-frame-tile-split-plan.md`), this brief.
  - Summarize 3 review findings the split fixes (scroll drift, recompose thrash on block updates, no early Frame before fetch — all per design §1 "Why Frame and Tile are siblings in one scroll").
  - 10 new files under `ui/mobile/calendar/`: GridConstants, NowIndicator, DayView, DayViewFrame, DayViewTile, WeekView, WeekViewFrame, WeekViewTile, MonthView, MonthViewFrame, MonthEventIndicator (11 — note: design says 10 but plan adds a separate `MonthEventIndicator` plus its wrapper `MonthView`).
  - Test files: DayViewFrameTest, DayViewTileTest, NowIndicatorTest, GridConstantsTest (already in), plus NEW WeekViewFrameTest, WeekViewTileTest, MonthViewFrameTest, MonthEventIndicatorTest, TimelineScreenLoadingTest, DayViewRefreshSnapshotTest, PxPerMinTest.
  - Benchmark gates: `timelineDayScrollFrameTimeP95`, `timelineDayHorizontalPagerFrameTimeP95` — record pre/post numbers in the PR.
- **Pre-merge checks:**
  - `./gradlew :app:assembleDebug` green
  - `./gradlew :app:testDebugUnitTest` green (includes the new Robolectric + JVM tests)
  - Benchmark baseline recorded (pasted in PR body)
  - No `Any` introduced in new files (verify with `grep -r ": Any" app/src/main/java/app/tastile/android/ui/mobile/calendar/`)
  - No `git stash` artifacts in working tree (`git stash list` must be empty for new stashes — leave user's pre-existing stashes alone per memory)
  - `TimelineScreen.kt` under 660 lines (`wc -l`)

---

## 6. Rollback strategy

The natural rollback is `git revert <T6-commit-sha>` — T6 is the only commit that **deletes** code from `TimelineScreen.kt`. T1..T5 are additive (new files), so reverting them in reverse is also safe but unnecessary if T6 alone is the cause.

If post-merge observation reveals Frame stability regression (the only class of failure T7 cannot catch — visual / runtime feel):
- **Immediate mitigation:** swap the `DayView(...)` dispatch in `TimelineScreen.kt` back to a copy of the pre-split `DayGrid(...)` body (the original 560–638 lines). This is a 3-line call-site change with no side effects — `DayGrid` would need to be preserved temporarily as a `private fun` for one PR cycle.
- **Follow-up:** schedule v38 to fix the regression; ship v37 as a pure refactor (no Frame/Tile benefit until v38 lands).

If a JVM/Compose test is found buggy:
- **T1–T5 tests:** fix the test only. Source code under `ui/mobile/calendar/` is covered by sibling tests, so single-test fixes have no blast radius.
- **T7 tests:** if `PxPerMinTest` reveals the extracted formula regresses, fix the formula in `DayView.kt`, not the test.

---

## Open questions for the T7 implementer to flag in the PR

1. Did T3 / T3.5 / T4 / T5 land tests in `app/src/test/` (JVM/Robolectric) or `app/src/androidTest/` (device)? Plan §3 said `androidTest`; reality is `test/`. Either mirror them or document the deviation.
2. Were the 3 hard-review findings from the design doc review (scroll drift, recompose thrash, no early Frame) actually surfaced as observable defects pre-v37, or are they speculative? If speculative, frame the PR as "preemptive refactor" not "fixes N regressions".
3. Benchmark baseline: is the implementer able to capture a `main` p95 number on this Windows host (needs an Android emulator)? If not, document the gap in the PR and flag follow-up to wire a CI job.
