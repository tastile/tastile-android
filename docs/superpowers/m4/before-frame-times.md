# M4 Baseline — Timeline Frame Times

- **Date:** 2026-07-16
- **Branch:** 2026-07-07-android-parity
- **HEAD SHA:** 8715f71705ec4a8594bc4adea69bbe2d4f2e90f9
- **Task:** M4 Task 2 (Capture `before` Timeline frame P95)
- **Harness:** `app/src/androidTest/java/app/tastile/android/benchmark/TimelineBenchmark.kt`
- **Benchmark deps:** `androidx.benchmark:benchmark-macro-junit4:1.3.3` (added in commit 23dd928)
- **Device profile:** Pixel 6 API 33 (default)
- **Iterations:** 10
- **Compilation mode:** DEFAULT
- **Startup mode:** WARM

## Frame metrics

| Metric          | Value                    |
| --------------- | ------------------------ |
| Frame P50       | BLOCKED — see Status     |
| Frame P95       | BLOCKED — see Status     |
| Frame P99       | BLOCKED — see Status     |
| Jank count      | BLOCKED — see Status     |

## Status

**BLOCKED — requires CI runner.** This Windows host cannot execute `./gradlew`
due to the JDK 17 PATH workaround issue (see memory
`project_android_jdk17_path_workaround.md` — Oracle Java 8 PATH entries preempt
JDK 17, which AGP 9.2.1 requires for `class file 61`). The harness is in place
and ready for CI; no local baseline captured.

This matches the project's precedent for `androidTest` tests that gate on
CI-only resources: the harness ships so CI can run it, and downstream tasks
(M4-T3 / M4-T4 optimizations + M4-T5 `after-frame-times.md`) reference this
file as the before-snapshot placeholder.

## Reproduction (CI)

```bash
./gradlew :app:pixel6Benchmark \
    -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.benchmark.TimelineBenchmark
```

Results land under `app/build/outputs/connect_android_test_additional_output/`
as `runBenchmarkAndroidTest-*.json`. Frame P50/P95/P99 are reported under the
`frameDurationP50/P95/P99` keys; jank count is `jankCount`.

The two `@Test` methods capture orthogonal workloads:

| Test method                                | Workload exercised                                                |
| ------------------------------------------ | ----------------------------------------------------------------- |
| `timelineDayScrollFrameTimeP95`            | Vertical scroll on the day grid (recomposition + Canvas redraw)   |
| `timelineDayHorizontalPagerFrameTimeP95`   | Horizontal swipe between day pages (HorizontalPager offscreen comp)|

Both methods share the same iteration / metric configuration. They run in
parallel under gradle-managed test shards; results should be concatenated when
diffing against `after-frame-times.md`.

## Selector stability

The harness targets `Modifier.testTag(...)` markers defined in
`ui/mobile/tabs/TimelineScreen.kt`:

| Selector                  | Widget                              |
| ------------------------- | ----------------------------------- |
| `calendar-mode-day`       | `Text("DAY")` in CalendarToolbar    |
| `calendar-mode-list`      | `Text("LIST")` in CalendarToolbar   |
| `calendar-today`          | `Text("Today")` in CalendarToolbar  |

If the testTag is missing on the installed APK (e.g. legacy dashboard
`ui/dashboard/TimelineScreen.kt` build), each test calls
`assumeTrue(false)` and is recorded by JUnit as **ignored** rather than failed.
This keeps the harness runnable across the dashboard ↔ mobile TimelineScreen
transition without a hard selector-coupling to a single screen variant.

## Notes

This is the M4 baseline. Will be re-captured as `after-frame-times.md` in M4
Task 5 after Tasks 3–4 apply Timeline optimizations.

The harness source — verbatim — lives alongside this file at
`docs/superpowers/m4/TimelineBenchmark-source-listing.md` so a CI runner can
cross-reference the implementation without touching the live source.
