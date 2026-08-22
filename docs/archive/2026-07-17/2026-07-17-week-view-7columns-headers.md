# Week View 7-column Layout & Header Consolidation Fix

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the Android Week view render as a real 7-column time-grid (currently collapses to a single column), and remove the duplicated day-of-week header that appears twice in the layout — the date row stays pinned under the toolbar and does not scroll with the body.

**Architecture:** The 7-column `WeekViewTile` already exists in `ui/mobile/calendar/WeekViewTile.kt` and is correctly written, but the host `WeekView.kt` lays the Frame and Tile out as siblings inside a `Row` that gives both `Modifier.fillMaxWidth()`, which collapses the Tile to 0 px. The DOW header (`Mon..Sun` + day numbers) is rendered both inside `WeekViewFrame.kt` (Column header Box) **and** as a pinned `WeekHeaderRow` in `WeekView.kt` (under the toolbar). We strip the inner header from `WeekViewFrame` (so it owns only the grid-lines Canvas), overlay Frame+Tile inside one `Box(weight=1f)` exactly like `DayView` does, and keep the existing pinned `WeekHeaderRow` in `WeekView.kt` (with a new `testTag` so tests can target it).

**Tech Stack:** Kotlin 2.x, Jetpack Compose (Material3 via Nia/M3), JUnit4 + Robolectric + Compose UI Test.

**Branch:** `2026-07-07-android-parity` (no worktree — committed directly, after the uncommitted QuickCreate-related changes on `mobile/tabs/TimelineScreen.kt` are resolved by their owner).

**Reference design doc:** `tastile-android/docs/plans/2026-07-17-timeline-frame-tile-split-design.md` §2 (per-component split).

---

## Task 1: Strip the inner DOW header from `WeekViewFrame` (it must not own clickable day columns)

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekViewFrame.kt:60-121` (Column + Box + private `DayHeader`)
- Test: `app/src/test/java/app/tastile/android/ui/mobile/calendar/WeekViewFrameTest.kt` (drop / move existing two header-asserting tests)

**Step 1: Drop the existing header-related Frame tests**

In `WeekViewFrameTest.kt`:
- Delete `frame_dowHeader_rendersAllSevenDays` (DOW assertion now belongs to `WeekViewHeaderTest`).
- Delete `frame_sevenColumns_rendered` (the 7-column assert is now about `WeekView`, not `WeekViewFrame`).

Keep `frame_root_andCanvas_areDisplayed` — asserts `week-view-frame` + `week-view-frame-grid-lines` exist.

**Step 2: Run Frame tests to confirm they still compile / fail on the right assertion**

Run: `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.calendar.WeekViewFrameTest"`
Expected: `frame_root_andCanvas_areDisplayed` PASS; the two deleted tests are gone.

**Step 3: Remove the inner DOW header from `WeekViewFrame`**

In `WeekViewFrame.kt`:

- Delete the `Box` block (currently lines 66-96, the one with `Modifier.fillMaxWidth().height(WEEK_HEADER_HEIGHT).background(...)` containing the `Row { Spacer + DayHeader × 7 + HorizontalDivider }`).
- Delete the private `DayHeader` composable at the bottom of the file (currently lines 123-155).
- The remaining `Column` body becomes only the grid-lines `Canvas(modifier = Modifier.weight(1f).fillMaxWidth()...)`. Remove the orphaned imports (`HorizontalDivider`, `Spacer`, `Row`, `Text`, `FontWeight`, `clickable`, `Alignment`, `testTag` reference inside the deleted `DayHeader`).
- Keep `testTag("week-view-frame")` on the outer `Column` and `testTag("week-view-frame-grid-lines")` on the Canvas so the surviving test passes.
- Add `testTag("week-view-frame-grid")` if missing — used by `WeekView` test below to scope a `onAllNodesWithTag` to grid-only.

Updated `Column` body should look like:

```kotlin
Column(modifier = modifier.testTag("week-view-frame").fillMaxSize()) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .border(width = 0.5.dp, color = outlineColor)
            .testTag("week-view-frame-grid-lines"),
    ) {
        val pxPerMinPx = pxPerMin * density
        for (h in 0..endHour) {
            val y = h * 60 * pxPerMinPx
            drawLine(
                color = outlineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }
    }
}
```

Imports cleanup:
- Remove: `HorizontalDivider`, `Spacer`, `Row`, `clickable`, `FontWeight`, `DateTimeFormatter`, `Locale`.
- Keep: `Canvas` (via the FQN already used), `Offset`, `Box`, `Column`, `Modifier`, `fillMaxSize`, `fillMaxWidth`, `weight`, `border`, `MaterialTheme`, `testTag`, `dp`.

**Step 4: Run Frame test → must PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.calendar.WeekViewFrameTest"`
Expected: 1 passed (`frame_root_andCanvas_areDisplayed`); no failure, no compile error.

**Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekViewFrame.kt \
        app/src/test/java/app/tastile/android/ui/mobile/calendar/WeekViewFrameTest.kt
git commit -m "refactor(week): strip inner DOW header from WeekViewFrame

Frame now owns only the grid-lines Canvas; the pinned day-of-week
header lives in WeekView.kt's WeekHeaderRow (one source of truth).
Tests for DOW labels and 7 day-column testTags moved to
WeekViewHeaderTest in the next commit."
```

---

## Task 2: Fix `WeekView.kt` so Frame and Tile overlay (root cause of "崩れている")

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekView.kt:79-148` (the body Row)
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekView.kt:163-202` (`WeekHeaderRow` — add `testTag`)
- Create: `app/src/test/java/app/tastile/android/ui/mobile/calendar/WeekViewHeaderTest.kt` (replaces Task 1's deleted assertions)

**Step 1: Write the failing test (frame+tile overlay, 7 columns visible)**

Create `WeekViewHeaderTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeekViewHeaderTest {
    @get:Rule val compose = createComposeRule()

    private fun renderWeek() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 1200.dp)) {
                    WeekView(
                        items = emptyList(),
                        weekStart = LocalDate.of(2026, 7, 13),
                        zone = ZoneId.of("UTC"),
                        onOpenDay = {},
                        zoom = 1f,
                        onZoomChange = {},
                        onEditEvent = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun pinHeader_rendersSevenDayColumns() = runTest {
        renderWeek()
        compose.onAllNodesWithTag("week-view-pin-header-day-column")
            .assertCountEquals(7)
    }

    @Test fun pinHeader_dowLabelsExist() = runTest {
        renderWeek()
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
            compose.onNodeWithText(it, useUnmergedTree = true).assertExists()
        }
    }

    @Test fun frame_gridLinesCanvasDisplayed() = runTest {
        renderWeek()
        compose.onNodeWithTag("week-view-frame-grid-lines").assertIsDisplayed()
    }

    @Test fun tile_sevenColumnsDisplayed() = runTest {
        renderWeek()
        compose.onAllNodesWithTag("week-view-tile-event-column").assertCountEquals(7)
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.calendar.WeekViewHeaderTest"`
Expected: FAIL — `week-view-pin-header-day-column` and `week-view-tile-event-column` do not exist (Tasks 3 + 2's testTag wiring not yet in place); `tile_sevenColumnsDisplayed` will also FAIL because the host Row still collapses Tile width.

**Step 2: Tag the pinned header columns in `WeekHeaderRow`**

In `WeekView.kt::WeekHeaderRow` (currently lines 163-202), each `Column(modifier = Modifier.weight(1f)...).padding(vertical = 4.dp)` representing a day of week gets `.testTag("week-view-pin-header-day-column")` appended to its `Modifier` chain.

Diff:

```kotlin
Column(
    modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .clickable { onOpenDay(day) }
        .padding(vertical = 4.dp)
        .testTag("week-view-pin-header-day-column"),
    ...
)
```

Add `import androidx.compose.ui.platform.testTag`.

**Step 3: Tag each per-day `Box` in `WeekViewTile.kt` (currently lines 58-67)**

In `WeekViewTile.kt` the per-day Box (one per weekday) currently has `.weight(1f).fillMaxHeight().border(...).clickable {...}`. Append `.testTag("week-view-tile-event-column")` to its `Modifier` chain.

```kotlin
Box(
    modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        .clickable { /* day-column tap is handled in the Frame */ }
        .testTag("week-view-tile-event-column"),
)
```

Add `import androidx.compose.ui.platform.testTag`.

**Step 4: Overlay Frame + Tile inside one `Box(weight=1f)` (the actual fix)**

In `WeekView.kt::WeekView` body (currently lines 110-148), replace the body Row's children:

Before:

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(scrollState),
) {
    WeekTimeGutter(endHour = ..., pxPerHour = ..., totalHeight = ..., modifier = Modifier.width(GridConstants.TIME_GUTTER_WIDTH))
    WeekViewFrame(weekStart, pxPerMin, onOpenDay, modifier = Modifier.fillMaxWidth().height(totalHeight))
    Box(modifier = Modifier.fillMaxWidth().height(totalHeight)) {
        WeekViewTile(weekStart, blocksByDay, pxPerMin, zone, scrollState, onEditEvent, modifier = Modifier.fillMaxSize())
    }
}
```

After:

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(scrollState),
) {
    WeekTimeGutter(
        endHour = GridConstants.DAY_END_HOUR,
        pxPerHour = (pxPerMin * 60).dp,
        totalHeight = totalHeight,
        modifier = Modifier.width(GridConstants.TIME_GUTTER_WIDTH),
    )
    // Frame + Tile overlay inside one weighted Box so the Tile gets
    // nonzero width (single-scroll multi-layer pattern, identical to
    // DayView's Scaffold body Row).
    Box(
        modifier = Modifier
            .weight(1f)
            .height(totalHeight),
    ) {
        WeekViewFrame(
            weekStart = weekStart,
            pxPerMin = pxPerMin,
            onOpenDay = onOpenDay,
            modifier = Modifier.fillMaxSize(),
        )
        WeekViewTile(
            weekStart = weekStart,
            blocksByDay = blocksByDay,
            pxPerMin = pxPerMin,
            zone = zone,
            scrollState = scrollState,
            onEditEvent = onEditEvent,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

Rationale: with `Modifier.fillMaxWidth()` on two non-weighted Row children sharing a Row, the second child gets 0px because Compose weighs them by intrinsic measure. The Tile Box currently gets 0 px → no chip column visible at all. Wrapping both in a `weight(1f)` Box (sibling overlay) makes each fill the weighted slot (DayView's pattern).

**Step 5: Run all Week tests → ALL PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.calendar.*"`
Expected:
- `WeekViewFrameTest.frame_root_andCanvas_areDisplayed` PASS
- `WeekViewTileTest.*` PASS
- `WeekViewHeaderTest.*` (4 new tests) PASS

If `pinHeader_rendersSevenDayColumns` returns fewer than 7 (e.g. 0) the testTag wiring is wrong — re-check Step 2.

**Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekView.kt \
        app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekViewTile.kt \
        app/src/test/java/app/tastile/android/ui/mobile/calendar/WeekViewHeaderTest.kt
git commit -m "fix(week): overlay 7-column Frame+Tile in one weighted Box

DayView's sibling-overlay pattern was missing from the Week body Row:
both WeekViewFrame and WeekViewTile used Modifier.fillMaxWidth(),
which collapsed the Tile to zero width and broke the column layout.

Frame+Tile now sit inside one Box(weight=1f) so both fill the
weighted slot and translate together under the single scroll state.

Also tags:
- week-view-pin-header-day-column on the pinned WeekHeaderRow in
  WeekView.kt (one source of truth for DOW + day numbers; the
  inner header was removed from WeekViewFrame in the previous commit).
- week-view-tile-event-column on each per-day column Box in
  WeekViewTile.kt so tests can count 7 columns.

Refs: docs/plans/2026-07-17-week-view-7columns-headers.md"
```

---

## Task 3: Remove residual unused state in `WeekView.kt`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekView.kt:149-153` (the `@Suppress("UNUSED_EXPRESSION") onZoomChange` workaround)

**Step 1: Drop the suppressed expression and the unused parameter**

`onZoomChange` is in the public Week signature (kept for symmetry with DayView) but never used in Week. Replace the trailing `@Suppress("UNUSED_EXPRESSION") onZoomChange` block (lines 149-153) with a one-line `// Week has no pinch zoom yet; onZoomChange matches DayView for forward symmetry.`

Keep the `onZoomChange` **parameter** in `fun WeekView(...)` — removing it would require touching `TimelineScreen.kt` and is out of scope here.

**Step 2: Verify build + tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all week tests green.

**Step 3: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekView.kt
git commit -m "chore(week): drop unused-expression suppression on onZoomChange

Replaced with a single-line comment; parameter is kept for API
symmetry with DayView."
```

---

## Verification (manual / via device)

1. Build: `./gradlew :app:installDebug`
2. Open the app, navigate to **Week** via the scale dropdown.
3. **7 columns** are visible side-by-side (one per weekday), each as a separate slot — not a single column.
4. The **DOW + day-number header** row appears exactly **once**, directly below the top toolbar, and **does not scroll** with the body when the user scrolls the grid vertically.
5. The **time-gutter** (00 / 04 / 08 / …) on the left scrolls with the body.
6. Event chips overlay the grid lines of their respective day columns.

## Rollback plan

Each task is a single commit. The three commits are independent:

- Revert Task 2 (`fix(week): overlay ...`) → restored to a single-column Week with the original Frame-internal header. Build still green.
- Revert Task 1 (`refactor(week): strip inner DOW header ...`) → restores the inner `DayHeader` Box and Frame's two deleted test slots. No runtime behavior change (just visual duplication that the user already dislikes).
- Revert Task 3 (`chore(week): drop unused-expression suppression`) → restores the suppressed expression; no behavior change.

If only Task 2 is reverted, the Frame still owns the DOW header so the layout is back to the original broken-but-duplicate state.
