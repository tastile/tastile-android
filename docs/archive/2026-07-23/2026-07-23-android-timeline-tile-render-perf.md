# 2026-07-23 Android: Timeline tile render perf

## Goal

体感できる Timeline 描画の軽量化。Day/Week/Month 切替、Pager swipe、pinch zoom、
初回ロード全ての症状で、改善が lab APK 上で観察できることを acceptance とする。
Macrobenchmark 等は打たない (best-effort)。

## Root causes (確認済み)

1. `DayViewTile.nowProvider` default `{ Instant.now() }` が 毎 compose で新 lambda
   → `MinuteTicker.LaunchedEffect(dateKey, nowProvider)` が coroutine を常時再起動
   → Day 画面で 1 分毎に ticker coroutine churn が累積
2. `TimelineScreen.onEditEvent` / `onOpenDay` が 毎 compose で新 closure
   → `DayViewTile` / `WeekViewTile` まで param propagation し recomposition cascade
3. `WeekViewTile` 内部 `LocalDate.now()` (line 58) / `WeekView.WeekHeaderRow` 内部
   `LocalDate.now()` (line 300) / `DayViewTile` 内部 `LocalDate.now()` (line 102)
   が composable 本体で読まれ、call site 時刻で recomposition 引き金が不定形に
4. `DayViewTile.blocks.forEach { b -> ... }` と `WeekViewTile.blocks.forEach` に
   `key` がない → Compose が位置再利用できず reorder で chip 全部再生成
5. `TimelineScreen.activeTimeline = remember(timeline) { timeline }` が no-op

## Fixes (surgical, all in `ui/mobile/calendar/` + `ui/mobile/tabs/TimelineScreen.kt`)

### 1. `MinuteTicker` lambda churn 修正 (DayViewTile.kt)

`DayViewTile` の `nowProvider: () -> Instant?` default を廃止し、
`nowInstant: Instant?` の **値** を直接渡す API に変更。
`nowInstant` の state + per-minute `LaunchedEffect` は `DayViewScaffold` に hoist。

これで:
- `LaunchedEffect` の key は `dateKey` のみ → coroutine churn 解消
- `DayViewTile` 自体は時刻依存の state を持たず recomposition トリガが安定

### 2. `onEditEvent` / `onOpenDay` を `remember` 化 (TimelineScreen.kt)

`val onOpenDay = remember(viewModel) { { day -> ... } }`
`val onEditEvent = remember(viewModel) { { item -> ... } }`

子の `DayView / WeekView / MonthView` まで新しい callback が流れなくなり、
composition 安定性が向上。

### 3. `LocalDate.now()` の hoist (WeekViewTile.kt:58, WeekView.kt:300, DayViewTile.kt:102)

`today = remember { LocalDate.now() }` を呼び元の `WeekView`/`DayView` 直下で 1 回
計算し、子に param で渡す。`WeekViewTile` / `WeekHeaderRow` / `DayViewTile` 本体で
の `LocalDate.now()` 呼び出しを全廃。

合わせて `WeekMinuteTicker` / `MinuteTicker` の `LaunchedEffect` coroutine が
実際に「今日」を表示中のみ走ることを保証 (dateKey ≠ today なら即座に stop)。

### 4. `key = b.id` 付与 (DayViewTile.kt:70, WeekViewTile.kt:71)

`blocks.forEach { b -> ... }` を `forEach(key = { b.id })` のブロックリスト
(compose-foundation の `forEach` は key を受け取らないので、
`key(...) { ... }` を chip ごとに付ける形に書換) → 行ごとに stable slot。

### 5. `activeTimeline` no-op 削除 (TimelineScreen.kt:63)

`val activeTimeline = remember(timeline) { timeline }` を削除して `timeline` を
直接使用。

## Out of scope

- `assignLanes` の O(N²) → O(N log N) 化 (test 困難 + 体感改善は限定的)
- `toDayBlocks` の 731 ページ一括集計化 (activeTimeline 全体を invalidate する
  trigger は現状 SSE/update のみで頻度が低い)
- ViewModel 側 timeline の date bucketing (別 PR)
- LazyColumn 化 (Day の chip 数は実用 ~10 で no-op)

## Verification

- `gradlew :app:assembleLab` でビルド通過
- `gradlew :app:testLab` で既存 unit test 緑
- Lint 通過
- (manual) lab APK インストール → Day 表示 → Week 切替 → Month 切替 の体感速度
  → Day へ戻る → Pager swipe で前後ページ → pinch zoom → 体感改善確認
