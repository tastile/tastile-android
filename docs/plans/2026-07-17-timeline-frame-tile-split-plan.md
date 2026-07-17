# Timeline Frame/Tile Split (v37) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Split `ui/mobile/tabs/TimelineScreen.kt`'s Day/Week/Month content into static Frames (gutter + grid + headers) and dynamic Tile overlays, so the Frame renders immediately while the timeline fetch is still in flight. Mirrors `tastile-web` commit `24e365e perf(calendar)`.

**Architecture:** New `ui/mobile/calendar/` package hosts `DayView` / `DayViewFrame` / `DayViewTile` (and Week/Month equivalents). Each View composes its Frame + Tile as siblings inside one `verticalScroll` host so a single scroll-state value drives both children. Frame takes only `date / scale / zoom / density`-derived inputs; Tile takes only `blocks: List<PlacedBlock>`. Compose skips Frame recomposition when blocks change. Top-level `TimelineScreen.kt` shrinks from ~1324 lines to <700, retaining only HorizontalPager, scale dispatch, toolbar, and FAB.

**Tech Stack:** Kotlin 2.x, Jetpack Compose (Material3 via Nia/M3), Hilt-injected `DashboardViewModel`, kotlinx-coroutines `StateFlow`, JUnit4 + Robolectric (JVM), Compose UI Test (`androidTest`). No new dependencies.

**Reference design doc:** `docs/plans/2026-07-17-timeline-frame-tile-split-design.md`

**Branch:** `2026-07-07-android-parity` (no worktree — committed directly)

---

## Task 1: Extract grid constants to a dedicated file

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/calendar/GridConstants.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt` (remove the moved constants from the `private object ...` or top-level declarations near the top of the file)

**Step 1: Write a JVM test asserting the public constants exist with the expected names**

Create: `app/src/test/java/app/tastile/android/ui/mobile/calendar/GridConstantsTest.kt`

```kotlin
package app.tastile.android.ui.mobile.calendar

import org.junit.Assert.assertTrue
import org.junit.Test

class GridConstantsTest {
    @Test fun timeGutterWidth_isPositive() {
        assertTrue(GridConstants.TIME_GUTTER_WIDTH > 0)
    }

    @Test fun dayZoomBounds_areOrdered() {
        assertTrue(GridConstants.ZOOM_MIN < GridConstants.ZOOM_MAX)
        assertTrue(GridConstants.ZOOM_DEFAULT in GridConstants.ZOOM_MIN..GridConstants.ZOOM_MAX)
    }

    @Test fun dayRange_isFull24Hours() {
        assertEquals(0, GridConstants.DAY_START_HOUR)
        assertEquals(24, GridConstants.DAY_END_HOUR)
    }

    @Test fun scrollBuffer_isPositive() {
        assertTrue(GridConstants.SCROLL_BUFFER_MIN > 0)
    }

    private fun assertEquals(expected: Int, actual: Int) =
        org.junit.Assert.assertEquals(expected, actual)
}
```

**Step 2: Run the test, confirm FAIL with unresolved reference**

Run: `./gradlew :app:testDebugUnitTest --tests '*GridConstantsTest*'`
Expected: FAIL — `Unresolved reference: GridConstants`.

**Step 3: Create `GridConstants.kt`**

```kotlin
package app.tastile.android.ui.mobile.calendar

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object GridConstants {
    const val TIME_GUTTER_WIDTH: Dp = 48.dp
    const val DAY_START_HOUR: Int = 0
    const val DAY_END_HOUR: Int = 24
    const val ZOOM_MIN: Float = 1f
    const val ZOOM_MAX: Float = 6f
    const val ZOOM_DEFAULT: Float = 1f
    const val SCROLL_BUFFER_MIN: Int = 15
    const val MONTH_GRID_ROWS: Int = 6
    const val WEEK_DAYS: Int = 7
}
```

Note: `TIME_GUTTER_WIDTH` value (48.dp) matches the existing usage in `TimelineScreen.kt`. After Task 1's verification, the implementer should grep for the original constant value in the source file and align this if it differs.

**Step 4: Move the original constants from `TimelineScreen.kt` to `GridConstants.kt`**

In `TimelineScreen.kt`, locate the existing top-level constants (currently near the top of the file — see `TimelineScreen.kt:1-100`). For each constant being moved:
1. Cut the declaration line from `TimelineScreen.kt`.
2. Paste it into `GridConstants.kt` (rename if needed to match the names above).
3. Update all references in `TimelineScreen.kt` to the qualified `GridConstants.X`.

Reference: this task ONLY moves constants. No logic moves in Task 1. The file should still compile.

**Step 5: Re-run test, expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests '*GridConstantsTest*'`
Expected: PASS.

**Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/calendar/GridConstants.kt \
        app/src/test/java/app/tastile/android/ui/mobile/calendar/GridConstantsTest.kt \
        app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt
git commit -m "refactor(android): extract calendar grid constants to GridConstants.kt"
```

---

## Task 2: Extract NowIndicator to a dedicated Composable

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/calendar/NowIndicator.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt` (remove the inline block, replace with `NowIndicator(...)` call from the new package)

**Step 1: Write a Compose UI test asserting presence of the indicator dot and line**

Create: `app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/NowIndicatorTest.kt`

```kotlin
package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class NowIndicatorTest {
    @get:Rule val compose = createComposeRule()

    private val now = Instant.parse("2026-07-17T08:30:00Z")

    @Test fun nowIndicator_dotAndLine_areDisplayed() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 1200.dp)) {
                    NowIndicator(
                        nowProvider = { now },
                        pxPerMin = 1f,
                        dayRangeStartHour = 0,
                        dayRangeEndHour = 24,
                        modifier = Modifier.testTag("now-indicator")
                    )
                }
            }
        }
        compose.onNodeWithTag("now-indicator-dot").assertIsDisplayed()
        compose.onNodeWithTag("now-indicator-line").assertIsDisplayed()
    }
}
```

**Step 2: Run, expect FAIL**

Run: `./gradlew :app:connectedDebugAndroidTest --tests '*NowIndicatorTest*'`
Expected: FAIL — `Unresolved reference: NowIndicator`.

If no device/emulator is available, run on JVM with Robolectric for placeholder check:
Run: `./gradlew :app:testDebugUnitTest --tests '*NowIndicatorTest*'`
Expected: FAIL (same reason). Note in PR that the real assertion runs on device.

**Step 3: Create `NowIndicator.kt`**

```kotlin
package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun NowIndicator(
    nowProvider: () -> java.time.Instant?,
    pxPerMin: Float,
    dayRangeStartHour: Int,
    dayRangeEndHour: Int,
    modifier: Modifier = Modifier,
) {
    val now = nowProvider() ?: return
    val minutesOfDay = ((now.epochSecond / 60) % (24 * 60)).toInt()
    val pxPerMinEff = pxPerMin.coerceAtLeast(0.0001f)
    val nowY = ((minutesOfDay - dayRangeStartHour * 60) * pxPerMinEff).dp
    Box(modifier) {
        Box(
            modifier = Modifier
                .offset(y = nowY - 5.dp)
                .size(10.dp)
                .background(Color.Red, CircleShape)
                .testTag("now-indicator-dot")
        )
        Box(
            modifier = Modifier
                .offset(y = nowY - 1.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.Red)
                .testTag("now-indicator-line")
        )
    }
}
```

**Step 4: Replace the inline block in `TimelineScreen.kt`**

Find lines 619–636 (the NowLine offset Box). Cut. Import `app.tastile.android.ui.mobile.calendar.NowIndicator`. Replace the cut block with:

```kotlin
NowIndicator(
    nowProvider = { java.time.Instant.now() },
    pxPerMin = pxPerMin,
    dayRangeStartHour = 0,
    dayRangeEndHour = 24,
    modifier = Modifier.fillMaxWidth(),
)
```

The variable `pxPerMin` must be in scope at this site — if not, lift it from the existing local calculation.

**Step 5: Build, run targeted tests**

Run: `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest --tests '*NowIndicator*'`
Expected: build green; unit test reports compile-time type-check.

**Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/calendar/NowIndicator.kt \
        app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/NowIndicatorTest.kt \
        app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt
git commit -m "refactor(android): extract NowIndicator to dedicated Composable"
```

---

## Task 3: Split Day view into DayViewFrame + DayViewTile

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/calendar/DayView.kt`
- Create: `app/src/main/java/app/tastile/android/ui/mobile/calendar/DayViewFrame.kt`
- Create: `app/src/main/java/app/tastile/android/ui/mobile/calendar/DayViewTile.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt:368-638` (remove `DayGrid` and `DayContentLayer` definitions; in the dispatch site, call `DayView(...)` instead)

**Step 1: Write a Compose UI test for `DayViewFrame`**

Create: `app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/DayViewFrameTest.kt`

```kotlin
package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class DayViewFrameTest {
    @get:Rule val compose = createComposeRule()

    @Test fun frame_drawsCanvasAndEmptySlotText() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 1200.dp)) {
                    DayViewFrame(
                        date = LocalDate.parse("2026-07-17"),
                        pxPerMin = 1f,
                        scrollState = androidx.compose.foundation.rememberScrollState(),
                        onCreateAt = { _, _ -> },
                        modifier = Modifier.testTag("day-view-frame"),
                    )
                }
            }
        }
        compose.onNodeWithTag("day-view-frame").assertIsDisplayed()
        compose.onNodeWithText("00:00").assertIsDisplayed()
        compose.onNodeWithText("12:00").assertIsDisplayed()
        compose.onNodeWithText("24:00").assertIsDisplayed()
        // The Canvas itself doesn't expose a text node; assert the Frame composable didn't crash
        // by checking no exception on a tag-search fallback.
        compose.onNodeWithTag("day-view-frame-grid-lines", useUnmergedTree = true)
            .assertCountEquals(1)
    }
}
```

**Step 2: Write a Compose UI test for `DayViewTile`**

Create: `app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/DayViewTileTest.kt`

```kotlin
package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DayViewTileTest {
    @get:Rule val compose = createComposeRule()

    private val zone = ZoneId.of("UTC")

    private fun placedBlock(title: String, startMin: Int, endMin: Int, lane: Int = 0, laneCount: Int = 1) =
        PlacedBlock(
            item = app.tastile.android.data.model.CoreTimelineItem(
                id = "x", title = title, startMinutesOfDay = startMin,
                endMinutesOfDay = endMin,
            ),
            startMinutes = startMin, endMinutes = endMin,
            laneIndex = lane, laneCount = laneCount,
        )

    @Test fun tile_emptyBlocks_doesNotRenderText() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 1200.dp)) {
                    DayViewTile(
                        blocks = emptyList(),
                        date = LocalDate.parse("2026-07-17"),
                        pxPerMin = 1f,
                        scrollState = androidx.compose.foundation.rememberScrollState(),
                        onEditEvent = {},
                        modifier = Modifier.testTag("day-view-tile"),
                    )
                }
            }
        }
        compose.onNodeWithText("Block Title").assertDoesNotExist()
    }

    @Test fun tile_oneBlock_rendersItsTitle() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 1200.dp)) {
                    DayViewTile(
                        blocks = listOf(placedBlock("Lunch", startMin = 720, endMin = 750)),
                        date = LocalDate.parse("2026-07-17"),
                        pxPerMin = 1f,
                        scrollState = androidx.compose.foundation.rememberScrollState(),
                        onEditEvent = {},
                        modifier = Modifier.testTag("day-view-tile"),
                    )
                }
            }
        }
        compose.onNodeWithText("Lunch").assertIsDisplayed()
    }
}
```

**Step 3: Run, expect FAIL (refs unresolved)**

Run: `./gradlew :app:testDebugUnitTest --tests '*DayViewFrameTest*' --tests '*DayViewTileTest*'`
Expected: FAIL — `Unresolved reference` for `DayViewFrame`, `DayViewTile`, `PlacedBlock`.

**Step 4: Create `DayView.kt`** (top-level wrapper, owns the scroll host and the pxPerMin helper)

```kotlin
package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.tastile.android.domain.model.PlacedBlock
import java.time.LocalDate

@Composable
fun DayView(
    date: LocalDate,
    zoom: Float,
    blocks: List<PlacedBlock>,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    onCreateAt: (hour: Int, minute: Int) -> Unit,
    onEditEvent: (app.tastile.android.data.model.CoreTimelineItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pxPerMin = remember(zoom) {
        // Mobile portrait, 24h fits at ZOOM_MIN; pxPerMin scaled by zoom.
        // Stub: maps zoom=1 to a baseline. Final formula lives in PxPerMinTest.
        1f * zoom.coerceIn(GridConstants.ZOOM_MIN, GridConstants.ZOOM_MAX)
    }
    Box(modifier.fillMaxSize().verticalScrollInternal(scrollState).testTag("day-view-root")) {
        DayViewFrame(
            date = date,
            pxPerMin = pxPerMin,
            scrollState = scrollState,
            onCreateAt = onCreateAt,
            modifier = Modifier.fillMaxSize(),
        )
        DayViewTile(
            blocks = blocks,
            date = date,
            pxPerMin = pxPerMin,
            scrollState = scrollState,
            onEditEvent = onEditEvent,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

The `verticalScrollInternal` is a private wrapper that calls `Modifier.verticalScroll(scrollState)`. (Implementer should inline this — kept here for clarity.)

**Step 5: Create `DayViewFrame.kt`** (Canvas + slot clicks; no blocks)

Move lines 587–599 (Canvas drawing 25 hour lines) and lines around 670 for `TimeGutterContent` into a single file. The Frame accepts `onCreateAt` for empty-slot taps. No `blocks` parameter. Slot tap calls `onCreateAt(hour, minute)` based on `y`-coordinate. The Canvas is drawn with `Canvas(Modifier.fillMaxSize()) { drawLine(...) }` for each hour mark.

**Step 6: Create `DayViewTile.kt`** (block overlays + NowIndicator)

Move lines 600–636 (the `blocks.forEach` block and the inline NowLine) into a single file. Internal NowIndicator call replaced by import of Task 2's `NowIndicator(...)`. Compose UI Test sees the title text through the Block composable.

**Step 7: Move data-carrier functions into `DayView.kt` private**

Move `toDayBlocks()` (currently line 1188) and `assignLanes()` (line 1213) into `DayView.kt` as `private` top-level functions in the same file. They take `List<CoreTimelineItem>` and `LocalDate` and return `List<PlacedBlock>`. Existing dashboard-side unit test `ui/dashboard/TimelineScreenLayoutTest.kt` may need an import path update if it references these by FQ name.

**Step 8: Replace inline day branch in `TimelineScreen.kt`**

In `TimelineScreen.kt` `when (scale)` branch for `TimelineScale.Day`, the call site previously invoked `DayGrid(...)`. Replace with:

```kotlin
import app.tastile.android.ui.mobile.calendar.DayView

DayGrid(...)   // becomes
DayView(
    date = pageDay,
    zoom = dayZoom,
    blocks = pageBlocks,
    scrollState = dayScrollState,
    onCreateAt = { h, m -> /* unchanged */ },
    onEditEvent = { /* unchanged */ },
)
```

**Step 9: Build + tests + dashboard test**

Run: `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest`
Expected: build green; all existing tests still pass.

**Step 10: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/calendar/DayView.kt \
        app/src/main/java/app/tastile/android/ui/mobile/calendar/DayViewFrame.kt \
        app/src/main/java/app/tastile/android/ui/mobile/calendar/DayViewTile.kt \
        app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/DayViewFrameTest.kt \
        app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/DayViewTileTest.kt \
        app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt \
        app/src/test/java/app/tastile/android/ui/dashboard/TimelineScreenLayoutTest.kt
git commit -m "refactor(android): split Day view into DayViewFrame + DayViewTile"
```

---

## Task 4: Split Week view into WeekViewFrame + WeekViewTile

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekView.kt`
- Create: `app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekViewFrame.kt`
- Create: `app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekViewTile.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt:731-1041`

Pattern mirrors Task 3 with these specifics:
- `WeekView` accepts `weekStart: LocalDate`, `zoom: Float`, `blocksByDay: List<List<PlacedBlock>>`, etc.
- `WeekViewFrame` draws 7 day-of-week headers + 7 columns of hour grid lines + shared time gutter.
- `WeekViewTile` iterates `blocksByDay`, placing per-day block boxes in their respective columns. Today's column calls `NowIndicator(...)` from Task 2.
- `WeekTimeGutter` (lines 942–969) folds into `WeekViewFrame`.

**Step 1: Write `WeekViewFrameTest`**

```kotlin
package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class WeekViewFrameTest {
    @get:Rule val compose = createComposeRule()

    @Test fun weekFrame_rendersSevenDayHeaders() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(800.dp, 600.dp)) {
                    WeekViewFrame(
                        weekStart = LocalDate.parse("2026-07-13"),
                        pxPerMin = 1f,
                        scrollState = androidx.compose.foundation.rememberScrollState(),
                        onOpenDay = {},
                        modifier = Modifier,
                    )
                }
            }
        }
        // 7 day-of-week headers, locale-aware short labels. Accept any of the common forms.
        for (label in listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) {
            compose.onNodeWithText(label, substring = true).assertIsDisplayed()
        }
    }
}
```

**Step 2-7: Mirror Task 3 step 2-7 for Week.**

Files match table at top of this task; the structure is identical. After implementing, update the dispatch site in `TimelineScreen.kt`:

```kotlin
CalendarViewMode.WEEK -> Unit   // becomes
WeekView(
    weekStart = ...,
    zoom = weekZoom,
    blocksByDay = ...,
    scrollState = weekScrollState,
    onOpenDay = { ... },
    onEditEvent = { ... },
)
```

**Step 8: Build + test**

Run: `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest --tests '*Week*'`
Expected: green.

**Step 9: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekView.kt \
        app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekViewFrame.kt \
        app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekViewTile.kt \
        app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/WeekViewFrameTest.kt \
        app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt
git commit -m "refactor(android): split Week view into WeekViewFrame + WeekViewTile"
```

---

## Task 5: Split Month view into MonthViewFrame + MonthEventIndicator

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/calendar/MonthView.kt`
- Create: `app/src/main/java/app/tastile/android/ui/mobile/calendar/MonthViewFrame.kt`
- Create: `app/src/main/java/app/tastile/android/ui/mobile/calendar/MonthEventIndicator.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt:1044-1156`

**Step 1: Write `MonthViewFrameTest`**

```kotlin
package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthViewFrameTest {
    @get:Rule val compose = createComposeRule()

    @Test fun monthFrame_renders42Cells() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(800.dp, 800.dp)) {
                    MonthViewFrame(
                        monthStart = YearMonth.of(2026, 7).atDay(1),
                        selectedDay = LocalDate.parse("2026-07-17"),
                        today = LocalDate.parse("2026-07-17"),
                        onOpenDay = {},
                        modifier = Modifier,
                    )
                }
            }
        }
        compose.onAllNodesWithTag("month-day-cell").assertCountEquals(42)
    }
}
```

**Step 2: Write `MonthEventIndicatorTest`**

```kotlin
package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MonthEventIndicatorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun indicator_zeroCount_isHidden() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(60.dp, 60.dp)) {
                    MonthEventIndicator(count = 0, inMonth = true, modifier = Modifier)
                }
            }
        }
        compose.onNodeWithTag("month-event-indicator").assertDoesNotExist()
    }

    @Test fun indicator_positiveCount_isVisible() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(60.dp, 60.dp)) {
                    MonthEventIndicator(count = 3, inMonth = true, modifier = Modifier.testTag("month-event-indicator"))
                }
            }
        }
        compose.onNodeWithTag("month-event-indicator").assertIsDisplayed()
    }
}
```

**Step 3-6: Mirror Task 3 for Month.** Files at top of this task.

**Specific design notes for Month (per the agreed approach "セル内同居"):**
- `MonthEventIndicator` is a small composable rendered inside each `MonthViewFrame` cell's Box — it does NOT live in a separate overlay.
- `MonthViewFrame` lays out 42 `MonthDayCellFrame` Composables (date text + borders only), each containing the date number AND a `MonthEventIndicator(count = itemsByDate[date] ?: 0)`.

**Step 7: Build + test**

Run: `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest --tests '*Month*'`
Expected: green.

**Step 8: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/calendar/MonthView.kt \
        app/src/main/java/app/tastile/android/ui/mobile/calendar/MonthViewFrame.kt \
        app/src/main/java/app/tastile/android/ui/mobile/calendar/MonthEventIndicator.kt \
        app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/MonthViewFrameTest.kt \
        app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/MonthEventIndicatorTest.kt \
        app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt
git commit -m "refactor(android): split Month view into MonthViewFrame + MonthEventIndicator"
```

---

## Task 6: Delete moved Composables from `TimelineScreen.kt`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt`

After Tasks 1–5, all the moved code already lives in the new files. This task deletes the originals:

- Delete `private fun DayGrid(...)` and `private fun DayContentLayer(...)`
- Delete `private fun WeekView(...)` and `private fun WeekDayColumn(...)` and `private fun WeekTimeGutter(...)`
- Delete `private fun MonthView(...)` and `private fun MonthDayCell(...)`
- Delete `private fun TimeGutterContent(...)` (folded into DayViewFrame)
- Delete `private fun NowIndicator(...)-equivalent-Block` (folded into Task 2)
- Delete `toDayBlocks` and `assignLanes` from this file (now in DayView.kt)
- Delete the constants moved in Task 1

`TimelineScreen.kt` should now be under 660 lines, retaining:
- `fun TimelineScreen(...)`
- Pager state, scale dispatch, toolbar/FAB
- `HorizontalPager` contents for each scale calling into `DayView`, `WeekView`, `MonthView`

**Step 1: Verify line count**

Run: `wc -l app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt`
Expected: under 660 lines.

**Step 2: Build + full test suite**

Run: `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest`
Expected: green; no existing tests broken.

**Step 3: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt
git commit -m "refactor(android): delete moved Composables from TimelineScreen.kt"
```

---

## Task 7: Add loading-state and snapshot tests; run benchmark

**Files:**
- Create: `app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/TimelineScreenLoadingTest.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/calendar/DayViewRefreshSnapshotTest.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/calendar/PxPerMinTest.kt`

**Step 1: Write `TimelineScreenLoadingTest`** — proves Frame renders even when timeline is empty

```kotlin
package app.tastile.android.ui.mobile.calendar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.OverlayViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class TimelineScreenLoadingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun frame_rendersImmediately_whenTimelineEmpty() = runTest {
        // Inject a ViewModel stub here. The exact wiring depends on the test instrumentation;
        // for v37, use a manually-constructed stub via DashboardViewModel.replaceTilesForTest(emptyList()).
        // (...)
        compose.setContent { MaterialTheme { /* mount TimelineScreen with stubbed VM */ } }
        compose.onNodeWithTag("day-view-root").assertIsDisplayed()
        compose.onNodeWithTag("day-view-frame").assertIsDisplayed()
    }
}
```

**Step 2: Write `DayViewRefreshSnapshotTest`** — proves VM preserves previous timeline on fetch failure

```kotlin
package app.tastile.android.ui.mobile.calendar

import app.tastile.android.data.repository.TileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals
import java.time.Instant

class DayViewRefreshSnapshotTest {
    @Test fun timeline_preservesPriorValue_onFetchError() = runTest {
        // Construct DashboardViewModel with a TileRepository that throws on getTimeline().
        // Call setOwnerFilter(...) to trigger refreshTimeline().
        // Assert: viewModel.timeline.value equals the pre-call snapshot.
        // Implementation requires Hilt's @HiltViewModel construction; if Hilt doesn't
        // support direct JVM instantiation, use Robolectric + HiltAndroidRule.
        val initialList = listOf(/* one CoreTimelineItem */)
        // vm.replaceTilesForTest(initialList)
        val throwRepo = object : TileRepository { override suspend fun getTimeline(s: Instant, e: Instant, o: List<String>) = throw RuntimeException("net down") }
        // Construct vm, call setOwnerFilter(""), await it.
        val valueAfter = vm.timeline.first()
        assertEquals(initialList, valueAfter)
    }
}
```

**Step 3: Write `PxPerMinTest`** — pins the pxPerMin formula

```kotlin
package app.tastile.android.ui.mobile.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PxPerMinTest {
    @Test fun zoomMin_saturates_toDayOnScreen() {
        // Same logic the implementer extracted from DayGrid's pxPerMin calculation
        val pxPerMin = computePxPerMin(zoom = 1f, availableHeightDp = 800f)
        assertTrue(pxPerMin > 0f)
    }

    @Test fun zoomMax_keepsWithinBounds() {
        val pxPerMin = computePxPerMin(zoom = GridConstants.ZOOM_MAX, availableHeightDp = 800f)
        assertTrue(pxPerMin >= 1f * GridConstants.ZOOM_MAX * 0.001f) // sanity floor
    }
}
```

The `computePxPerMin` function is a `internal` top-level in `DayView.kt`. The implementer extracts the existing inline math from `TimelineScreen.kt`'s DayGrid body during Task 3's DayView creation.

**Step 4: Run all new tests, expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests '*TimelineScreenLoading*' --tests '*DayViewRefresh*' --tests '*PxPerMin*'`
Expected: green.

**Step 5: Run `TimelineBenchmark`, record p95**

Run: `./gradlew :app:pixel6Benchmark -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.benchmark.TimelineBenchmark`
Expected: a JSON report at `app/build/outputs/connect_android_test_additional_output/`. Read `timelineDayScrollFrameTimeP95` and compare to the pre-v37 baseline (capture the same number on `main` first if not yet recorded).

**Step 6: Compose final commit and push**

```bash
git add app/src/androidTest/java/app/tastile/android/ui/mobile/calendar/TimelineScreenLoadingTest.kt \
        app/src/test/java/app/tastile/android/ui/mobile/calendar/DayViewRefreshSnapshotTest.kt \
        app/src/test/java/app/tastile/android/ui/mobile/calendar/PxPerMinTest.kt \
        docs/plans/2026-07-17-timeline-frame-tile-split-design.md \
        docs/plans/2026-07-17-timeline-frame-tile-split-plan.md
git commit -m "test(android): add Frame/Tile split coverage (loading, snapshot, pxPerMin)"
git push origin 2026-07-07-android-parity
```

**Step 7: Open PR**

Use `gh pr create` against `main`. PR body summarizes Step 1–7 commits, references the design doc, attaches the p95 numbers, and flags any open TODOs (e.g., if benchmark required emulator).

---

## Out-of-scope (carryover)

These are NOT part of v37 and should be tracked as v38+:

- Web-feature parity gaps in `MonthEventIndicator` (web shows pill titles; Android currently shows a count dot only).
- Spec parity for `CalendarProjectionResponse` (unused on Android today; v0 legacy `ui/dashboard/TimelineScreen.kt`).
- `toDayBlocks` performance with >500 tiles (currently O(N²) via `assignLanes`).
- Removing the dead `ui/dashboard/TimelineScreen.kt` `DayAgendaScreen` / `WeekAgendaScreen` / `MonthCalendarScreen` stubs.
