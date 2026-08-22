# Timeline Frame/Tile Split Design (v37)

> **Goal:** Split `TimelineScreen.kt`'s Day/Week/Month content into static Frames (gutter + grid + headers) and dynamic Tiles (block overlays), so the Frame renders immediately even when the timeline fetch is still in flight. Mirrors `tastile-web`'s `24e365e perf(calendar)` refactor.

**Branch:** `2026-07-07-android-parity`
**Source:** `tastile-web` commit `24e365e` (extract memoized frames, tiles stream without flicker)
**Surface:** `ui/mobile/tabs/TimelineScreen.kt` (1324 lines today) + `DashboardViewModel.refreshTimeline()`

---

## 1. Architecture — Frame / Tile separation

### File layout

New directory: `app/src/main/java/app/tastile/android/ui/mobile/calendar/`

```
calendar/
├── GridConstants.kt          # TIME_GUTTER_WIDTH, ZOOM_MIN/MAX, header heights
├── NowIndicator.kt           # red dot + 2dp horizontal line (split out from DayContentLayer)
├── DayView.kt                # Day wrapper: scroll host + Frame + Tile siblings
├── DayViewFrame.kt           # time gutter + 25 grid lines + (empty) tap slots
├── DayViewTile.kt            # block rectangles + NowIndicator overlay
├── WeekView.kt               # 7-col wrapper, scroll host + headers
├── WeekViewFrame.kt          # 7-col grid + time gutter + DOW header
├── WeekViewTile.kt           # per-day column blocks + today's NowIndicator
├── MonthView.kt              # 7x6 wrapper + DOW header
├── MonthViewFrame.kt         # 42 cells with borders + day numbers + selection
└── MonthEventIndicator.kt    # 6dp count dot / pill inside MonthViewFrame cells
```

`TimelineScreen.kt` keeps HorizontalPager, scale dispatch, toolbar, FAB. Bulk code moves out — target: under 700 lines.

### What moves where

| Origin (`TimelineScreen.kt`) | Destination | Why |
| --- | --- | --- |
| `DayContentLayer` 560–638 (Canvas grid + `blocks.forEach` + NowLine) | `DayViewFrame` (Canvas+slot only) + `DayViewTile` (blocks+now-line) | Only the Canvas layer needs to be stable when blocks change |
| `WeekDayColumn` 972–1041 | `WeekViewFrame` (grid) + `WeekViewTile` (per-day blocks) | Same pattern |
| `MonthDayCell` 1114–1156 | `MonthViewFrame` (day number + borders) + `MonthEventIndicator` (count dot) | Cell-internal cohabitation, matches web's `MonthEventTile` |
| `TimeGutterContent` 641–669 / `WeekTimeGutter` 942–969 | folded into respective `*ViewFrame.kt` | No separate file |
| `toDayBlocks` 1188 / `assignLanes` 1213 | `DayView.kt` private | DAG computation is data work, not Frame/Tile concern |

`DashboardViewModel.refreshTimeline()` 574–588 is **untouched**. Its `try` block already preserves the previous `_timeline` on fetch failure, satisfying the "keep previous snapshot" requirement with zero VM changes.

### Why Frame and Tile are siblings in one scroll

Web's pattern puts `DayViewTile` inside `DayViewFrame`'s render-prop slot, sharing scroll state. On Compose, a cleaner mirror is:

```kotlin
@Composable
fun DayView(date, zoom, blocks, scrollState, onCreateAt, onEditEvent) {
    Box(Modifier.fillMaxSize().verticalScroll(scrollState)) {
        DayViewFrame(date = date, pxPerMin = pxPerMin(zoom), ...)
        DayViewTile(blocks = blocks, pxPerMin = pxPerMin(zoom), ...)
    }
}
```

Both children occupy the same scroll viewport; Frame's Canvas and Tile's Boxes translate together. `pxPerMin` is computed once in `DayView` and passed identically to both children so a zoom change drives synchronized recomposition.

When `blocks` changes, Frame sees no param churn → its `Canvas` (and the slot-tap Box) skip recomposition. Tile recomposes, replacing only its overlays. When `date` or `zoom` changes, both recompose — that's the cost web takes too.

---

## 2. Data flow — VM to Frame/Tile

```
DashboardViewModel
  └─ _timeline: MutableStateFlow<List<CoreTimelineItem>>      (existing, unchanged)
       │
       │ collectAsStateWithLifecycle()                          (line 123)
       ▼
TimelineScreen
  ├─ activeTimeline = remember(timeline) { timeline }           (existing line 135)
  ├─ when (scale)
  │    ├─ Day   → DayView  date=pageDay, blocks=toDayBlocks(activeTimeline, pageDay, zone)
  │    ├─ Week  → WeekView weekStart, blocksByDay=...for each day
  │    └─ Month → MonthView monthStart, itemsByDate=countByDate(activeTimeline, monthStart)
  └─ Each View computes its derived `blocks`/`blocksByDay`/`itemsByDate` inside `remember(...)`
     so the computation is cached per (timeline, pageDay/week/month).
```

`refreshTimeline()` refresh triggers:
- `setOwnerFilter()` line 151 — fires when workspace filter changes
- `setCalendarMinimumDuration()` line 236 — fires on min-duration change
- `refreshAll()` line 591 — full reload (auth/profile refresh path)
- `TimelineScreen.init {}` (TBD: confirm) — initial load on screen mount

Refreshes land as fresh `_timeline.value = ...` only on success. Failure (network, parse error) leaves the previous list intact, so `activeTimeline` keeps showing tiles while a tiny inline indicator (`isLoadingTimeline` if added later) signals the refresh.

### Zoom during gesture

`dayZoom` is a `MutableState<Float>` hoisted above the HorizontalPager; both Frame and Tile observe it via the `pxPerMin = remember(zoom, density) { ... }` derivation in `DayView`. During a pinch, both recompose every frame. On gesture end, the value settles via `animateFloatAsState` (target spec = easing). Same for `weekZoom` on Week.

This deliberately matches web's `use-zoom` "defer setZoom to gesture end" pattern minus the explicit end-commit — Compose's `animateFloatAsState` keeps the visual smooth without the extra commit step.

---

## 3. Testing strategy

### Existing tests (must keep green)

| File | Asserts | v37 risk |
| --- | --- | --- |
| `ui/mobile/tabs/TimelineZoomMathTest.kt` | `anchoredZoomScrollTarget()` math | Move only — no signature change |
| `ui/mobile/tabs/CalendarEventControlsTest.kt` | project cascade / dispatch | Untouched |
| `ui/dashboard/TimelineScreenLayoutTest.kt` | `arrangeVisibleBlocks()` | May need import path update if `toDayBlocks`/`assignLanes` re-exported from `DayView.kt` |
| `ui/dashboard/MonthCalendarScreenTest.kt` | `buildMonthCalendarCells` | Untouched |
| `app/src/androidTest/.../TimelineBenchmark.kt` | Day frame-timing p95 | **KPI gate** for v37 — must not regress |

### New Compose UI tests (under `app/src/androidTest/.../ui/mobile/calendar/`)

1. `DayViewFrameTest.kt` — render `DayViewFrame` with `zoom=1f`, empty blocks. Assert 24 horizontal grid lines drawn (`drawIntoCanvas` pixel read at expected `y`). Assert hour labels "00:00", "06:00", "12:00", "18:00", "24:00" via `onNodeWithText`.
2. `DayViewTileTest.kt` — render `DayViewTile` with one `PlacedBlock`. Assert title text exists, bounding box `y` is `startMinutes * pxPerMin` ± 2dp. Empty list → no block nodes.
3. `WeekViewFrameTest.kt` — 7 day-of-week header labels "Mon".."Sun" via `onNodeWithText`. Body shows 7 columns.
4. `MonthViewFrameTest.kt` — exactly 42 day cells render, out-of-month cells visibly deemphasized.
5. `TimelineScreenLoadingTest.kt` — install `TimelineScreen` with `viewModel.timeline = []`. Within 500ms, `onNodeWithTag("day-grid-line-canvas")` is displayed. Proves Frame renders without waiting for tiles.

### New JVM tests (under `app/src/test/.../ui/mobile/calendar/`)

6. `DayViewRefreshSnapshotTest.kt` — under Robolectric, kick off `refreshTimeline()`, force a thrown exception from `tileRepository.getTimeline`, assert `viewModel.timeline.value` equals the pre-call snapshot.
7. `PxPerMinTest.kt` — pure-function assertions on `pxPerMin(zoom, availableHeightDp, density)` — bounds `ZOOM_MIN..ZOOM_MAX`, day range 0..24 forced, `availableHeight/1440` baseline. Lifts the v34 logic out of `TimelineScreen.kt` so it can be regression-tested.

### Benchmark gate

`TimelineBenchmark` is the KPI. Run on `main` (pre-v37) and on v37 in identical conditions. Accept v37 if Day frame p95 ≤ baseline + 5%. Regression → revert Step 6 and stop at Step 5, schedule v38 follow-up.

---

## 4. Migration plan (7 steps)

| Step | Change | Verification |
| --- | --- | --- |
| 1 | Create `calendar/GridConstants.kt`, move constants from `TimelineScreen.kt` | `./gradlew :app:compileDebugKotlin` |
| 2 | Create `calendar/NowIndicator.kt`, move lines 619–636 | Build |
| 3 | Create `DayView.kt` + `DayViewFrame.kt` + `DayViewTile.kt`, split `DayContentLayer`. Rename `DayGrid` references to `DayView` | `DayViewFrameTest` + `DayViewTileTest` green |
| 4 | Create `WeekView.kt` + `WeekViewFrame.kt` + `WeekViewTile.kt`, split `WeekDayColumn` | `WeekViewFrameTest` green |
| 5 | Create `MonthView.kt` + `MonthViewFrame.kt` + `MonthEventIndicator.kt`, split `MonthDayCell` | `MonthViewFrameTest` green |
| 6 | Delete moved Composables from `TimelineScreen.kt`, update dispatch to call `ui.mobile.calendar.*`. Move `toDayBlocks`/`assignLanes` private into `DayView.kt` | `assembleDebug` + all existing tests green |
| 7 | Add `TimelineScreenLoadingTest` + `DayViewRefreshSnapshotTest` + `PxPerMinTest`. Re-run `TimelineBenchmark`. Push. Open PR | All green + bench recorded |

Each Step is an independent commit. Step 6 is the only large diff; reverting it restores the original 1324-line `TimelineScreen.kt`.

---

## Risks and rollback

- **Risk:** Frame/Tile boundary drawn wrong (Frame references `blocks` accidentally, or Tile references `pxPerMin` from wrong scope). Detect via `TimelineScreenLoadingTest` failing on Step 7.
- **Risk:** `BoxWithConstraints` interaction with new sibling layout. Detect via Compose preview + the existing `TimelineBenchmark` regressing.
- **Risk:** Scroll position zeroed when scroll host changes structure during Day pager swipe. Detect by manual smoke + `TimelineBenchmark.dayHorizontalPagerFrameTimeP95`.
- **Rollback:** Revert Step 6 commit (returns to monolithic `TimelineScreen.kt`). Steps 1–5 are additive file creation; revert them in reverse if even that is needed.

## Definition of Done

- `app/src/main/java/.../ui/mobile/calendar/` contains the 10 listed files.
- `TimelineScreen.kt` is under 660 lines.
- `TimelineBenchmark.dayScrollFrameTimeP95` ≤ baseline + 5%.
- `TimelineScreenLoadingTest` green (Frame renders even with empty timeline).
- `DayViewRefreshSnapshotTest` green (`refreshTimeline` failure preserves snapshot).
- All existing tests pass.
