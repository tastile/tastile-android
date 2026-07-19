# WeekView Pinch Zoom Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Add DayView-equivalent two-finger anchored pinch zoom to WeekView while preserving one-finger vertical scrolling, horizontal week paging, the pinned translucent header, and event/grid alignment.

**Architecture:** Keep `weekZoom` hoisted in `TimelineScreen`; its existing `onZoomChange` callback is already wired. Add the DayView gesture state machine inside `WeekView`, consuming pointer changes only after two pointers are down. During a pinch, remeasure the Week body from the transient zoom and counter-translate it around the pinch centroid; after release, commit the new zoom and anchored `ScrollState` position.

**Tech Stack:** Kotlin, Jetpack Compose pointer input, Compose `ScrollState`, Robolectric Compose UI tests, Gradle/JDK 17.

**Constraints:** Work on the current branch; do not create a worktree, commit, or touch unrelated dirty files. Limit production edits to `WeekView.kt`. Keep the existing Week header opacity/layout fix intact.

---

### Task 1: Add failing Week gesture tests

**Files:**
- Create: `app/src/test/java/app/tastile/android/ui/mobile/calendar/WeekViewZoomTest.kt`
- Modify later: `app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekView.kt`

**Step 1: Write a pinch regression test**

Render `WeekView` in an `800.dp × 1200.dp` root with an injected `ScrollState(0)` and Compose-owned `zoom` state initialized to `1.5f`. Perform an outward two-pointer `pinch(...)` on a new `week-view-body` test tag. Capture `onZoomChange` and assert after idle that:

```kotlin
assertTrue(observedZoom > 1.5f)
assertTrue(scrollState.value > 0)
```

The first assertion proves Week consumes a two-pointer transform; the second proves the minute under the centroid remains anchored instead of zooming only from the top.

**Step 2: Write the gesture-arbitration regression test**

On the same injected body, perform a one-finger upward swipe and assert:

```kotlin
assertTrue(scrollState.value > 0)
assertEquals(1.5f, observedZoom, 0.0001f)
```

This locks the requirement that the pinch recognizer does not steal one-finger vertical scrolling. The recognizer must likewise leave one-finger horizontal movement unconsumed for `HorizontalPager`.

**Step 3: Run the pinch test and verify RED**

Run:

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.ui.mobile.calendar.WeekViewZoomTest.pinchOut_updatesZoomAndAnchorsScroll"
```

Expected: compilation/test failure because `WeekView` does not yet accept an injected `scrollState` and has no `week-view-body` pinch target.

---

### Task 2: Implement Day-equivalent Week pinch handling

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekView.kt:67-178`

**Step 1: Make Week scroll state injectable**

Add this defaulted parameter without changing existing callers:

```kotlin
scrollState: ScrollState = rememberScrollState(),
```

Remove the current local `val scrollState = rememberScrollState()`.

**Step 2: Add transient gesture state**

Mirror DayView's state and deferred scroll commit:

```kotlin
val latestZoom by rememberUpdatedState(zoom)
var pendingZoomScroll by remember { mutableStateOf<Int?>(null) }
var pinchZoom by remember { mutableStateOf<Float?>(null) }
var pinchTranslationY by remember { mutableFloatStateOf(0f) }

LaunchedEffect(zoom, pendingZoomScroll) {
    pendingZoomScroll?.let { target ->
        withFrameNanos { }
        scrollState.scrollTo(target)
        pendingZoomScroll = null
        pinchZoom = null
        pinchTranslationY = 0f
    }
}
```

Use `effectiveZoom = pinchZoom ?: zoom` when calling `computeWeekPxPerMin`.

**Step 3: Preserve the viewport/header layering**

Change the root to `BoxWithConstraints`. Keep the body starting at the root top and keep the pinned header as its later sibling with only the header offset by `TOP_BAR_TOTAL_HEIGHT()`.

Wrap the gutter and 7-column grid in the same shape DayView uses:

```kotlin
Column(
    Modifier
        .fillMaxSize()
        .background(background)
        .testTag("week-view-body")
        .pointerInput(Unit) { /* two-pointer loop */ }
        .verticalScroll(scrollState),
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(totalHeight)
            .graphicsLayer { translationY = pinchTranslationY },
    ) {
        // existing WeekTimeGutter + WeekViewFrame/WeekViewTile Box
    }
}
```

The single child `Row` ensures gutter labels, grid lines, event chips, and NowIndicator all receive the same transient translation. Do not apply translation or zoom to `WeekHeaderRow`.

**Step 4: Add the two-pointer state machine**

Copy DayView's `awaitEachGesture`/`awaitFirstDown(requireUnconsumed = false)` loop. Consume changes only when `pressed.size >= 2`. Record the initial distance, zoom, scroll, and centroid; calculate each outward/inward update with:

```kotlin
val newZoom = (initialZoom * currentDistance / initialDistance)
    .coerceIn(GridConstants.ZOOM_MIN, GridConstants.ZOOM_MAX)
val targetScroll = anchoredWeekZoomScrollTarget(
    currentScrollPx = initialScroll,
    anchorYpx = initialCentroidY,
    oldPxPerMin = computeWeekPxPerMin(initialZoom, totalMinutes) * density,
    newPxPerMin = computeWeekPxPerMin(newZoom, totalMinutes) * density,
    totalMinutes = totalMinutes,
    viewportPx = maxHeight.value * density,
)
```

Update `pinchZoom` and `pinchTranslationY = initialScroll - targetScroll` during the gesture. On release, set `pendingZoomScroll` and call `onZoomChange(finalZoom)`. If a second pointer never appears, clear transient state without consuming the one-finger gesture.

**Step 5: Add scoped anchored-scroll math**

Add `private fun anchoredWeekZoomScrollTarget(...)` beside `computeWeekPxPerMin`, using the same formula as DayView. Keep it private to avoid a calendar→tabs dependency and avoid refactoring DayView during this feature.

---

### Task 3: Verify behavior and regressions

**Files:**
- Test: `app/src/test/java/app/tastile/android/ui/mobile/calendar/WeekViewZoomTest.kt`
- Existing regression test: `app/src/test/java/app/tastile/android/ui/mobile/calendar/WeekViewHeaderTest.kt`

**Step 1: Run focused tests**

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.ui.mobile.calendar.WeekViewZoomTest" --tests "app.tastile.android.ui.mobile.calendar.WeekViewHeaderTest"
```

Expected: all tests pass. Confirm the existing body-behind-top-bar and pinned-header bounds tests remain green.

**Step 2: Build the APK**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL` and an updated `app/build/outputs/apk/debug/app-debug.apk`.

**Step 3: Review the scoped diff**

```bash
git diff --check -- app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekView.kt app/src/test/java/app/tastile/android/ui/mobile/calendar/WeekViewZoomTest.kt
git status --short
```

Verify no unrelated dirty file changed during this task. Request code review; if the Windows path-length issue prevents the review agent worktree, report that limitation instead of claiming review passed.

**Step 4: Verify on XIG03**

Install only after `adb devices -l` lists serial `8a3611541872`:

```bash
adb -s 8a3611541872 install -r -t app/build/outputs/apk/debug/app-debug.apk
```

In Week view, verify: two-finger spread zooms in, pinch zooms out, the minute under the centroid stays stable, one-finger vertical scroll still works, one-finger horizontal swipe still changes weeks, the pinned date header does not scale, and gutter/grid/events remain aligned. If the device is absent, leave this item explicitly unverified.
