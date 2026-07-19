# Now Indicator Grid Alignment Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Center the current-time dot on the Day/Week time-grid left edge while keeping the horizontal line aligned with the grid.

**Architecture:** Day and Week both render the shared `NowIndicator` composable. Move only its 10dp dot 5dp left; keep the line, time calculation, color, and callers unchanged.

**Tech Stack:** Kotlin, Jetpack Compose, Robolectric Compose UI tests, Gradle

---

### Task 1: Add the geometry regression tests

**Files:**
- Modify: `app/src/test/java/app/tastile/android/ui/mobile/calendar/NowIndicatorTest.kt:3-10,23-42`
- Modify: `app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/NowIndicatorColorTest.kt:10-20,50`

**Step 1: Write the failing assertions**

Import `assertLeftPositionInRootIsEqualTo` and assert the dot starts at `-5dp` in the existing unit fixture. Add this independently executable instrumentation test:

```kotlin
@Test
fun nowIndicator_dotCenterAlignsWithGridLeftEdge() {
    compose.setContent {
        MaterialTheme {
            Box(Modifier.size(400.dp, 1200.dp)) {
                NowIndicator(
                    nowProvider = { Instant.parse("2026-07-17T08:30:00Z") },
                    zone = ZoneOffset.UTC,
                    pxPerMin = 1f,
                    dayRangeStartHour = 0,
                    dayRangeEndHour = 24,
                )
            }
        }
    }

    compose.onNodeWithTag("now-indicator-dot")
        .assertLeftPositionInRootIsEqualTo((-5).dp)
}
```

The fixtures render the indicator at the root's left edge. A 10dp dot centered on that edge must therefore start at `-5dp`.

**Step 2: Run the instrumentation test to verify RED**

The current working tree contains an unrelated QuickCreate unit-test compile error, so `debugUnitTest` cannot reach the focused test without modifying out-of-scope work. Run:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.ui.mobile.calendar.NowIndicatorColorTest#nowIndicator_dotCenterAlignsWithGridLeftEdge
```

Expected: FAIL because the current dot starts at `0dp` instead of `-5dp`.

### Task 2: Center the dot on the grid edge

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/calendar/NowIndicator.kt:39-45`

**Step 1: Apply the minimal geometry change**

Change the dot offset to:

```kotlin
.offset(x = (-5).dp, y = nowY - 5.dp)
```

Do not change the horizontal line. It must continue to start at `x = 0` and fill the grid width.

**Step 2: Run the focused test to verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests '*NowIndicatorTest'
```

Expected: PASS.

### Task 3: Verify shared Day/Week behavior

**Files:**
- Verify only: `app/src/main/java/app/tastile/android/ui/mobile/calendar/DayViewTile.kt`
- Verify only: `app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekViewTile.kt`

**Step 1: Run relevant JVM verification**

```bash
./gradlew :app:testDebugUnitTest --tests '*NowIndicatorTest' --tests '*DayViewTileTest' --tests '*WeekView*Test'
```

Expected: PASS.

**Step 2: Build and install the debug APK**

```bash
./gradlew :app:assembleDebug
```

Install with the existing Xiaomi-safe `adb push` plus `pm install --user 0 -r` flow.

**Step 3: Inspect both views on the device**

Confirm in Day and Week:

- the dot center sits on the time-grid left boundary;
- the straight line starts exactly at that boundary and continues right;
- vertical time alignment, color, and line thickness are unchanged.

**Step 4: Audit scope and request review**

Review only the plan, `NowIndicator.kt`, and `NowIndicatorTest.kt` changes. Preserve all pre-existing working-tree changes. Do not commit unless the user explicitly requests it.
