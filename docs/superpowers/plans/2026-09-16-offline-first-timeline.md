# Offline-first Timeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Day / Week / Month を永続 local read model から即時かつ連続描画し、取得済み期間を offline read-only で利用可能にする。

**Architecture:** Compose は page 単位の immutable snapshot を Room の `Flow` から読む。API 通信は独立した sync repository が Room transaction を更新するだけで、UI は network response を直接待たない。Day / Week / Month は共通の日単位 membership を合成し、pager は現在 page と前後 page を同時に構成する。

**Tech Stack:** Kotlin 2.2.10、Jetpack Compose、Room 2.8.5 + KSP、Hilt、Coroutines / Flow、JUnit4、Robolectric、Android instrumented tests

**Spec:** `docs/superpowers/specs/2026-09-16-offline-first-timeline-design.md`

## Global Constraints

- UI は local persistent read model の `Flow` だけを描画し、API client や request result を直接参照しない。
- Day / Week / Month は同じ raw Timeline item と日単位 membership を共有し、scale ごとの duplicate cache を作らない。
- `TimelineQuery.range` は有限とし、core v1 の `TimelineItem`、include flags、UTC instant、numeric registry を再定義しない。
- offline は読み取り専用。command queue、楽観更新、conflict resolution を追加しない。
- 未取得期間と取得済み empty を区別し、API failure で正常な local data を削除しない。
- content equality は ID だけでなく `CoreTimelineItem` の全 canonical display field を対象とする。
- pager は current / previous / next page を同時構成し、page snapshot は page ごとに独立して更新する。
- 明示的 logout、account deletion、account switch 時は対象 account cache を削除する。
- source code、identifier、code comment、commit message は英語。internal development doc は日本語。
- 既存の未追跡 `docs/superpowers/specs/2026-09-07-ui-rebuild-design.md` を変更、stage、commit しない。
- 実装 WIP は 1。共有 filesystem 上で複数 writer を同時実行しない。

---

### Task 1: Room database and lossless timeline persistence

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/app/tastile/android/data/timeline/local/TimelineCacheEntities.kt`
- Create: `app/src/main/java/app/tastile/android/data/timeline/local/TimelineCacheDao.kt`
- Create: `app/src/main/java/app/tastile/android/data/timeline/local/TimelineCacheDatabase.kt`
- Create: `app/src/main/java/app/tastile/android/data/timeline/local/TimelineCacheMapper.kt`
- Create: `app/src/test/java/app/tastile/android/data/timeline/local/TimelineCacheDaoTest.kt`
- Create: `app/src/test/java/app/tastile/android/data/timeline/local/TimelineCacheMapperTest.kt`
- Modify: `app/src/main/java/app/tastile/android/data/di/ApiModule.kt`

**Interfaces:**
- Consumes: `CoreTimelineItem` from `core/CoreDtos.kt`.
- Produces: `TimelineCacheDao`, `TimelineCacheDatabase`, `TimelineItemEntity`, `TimelineDayMembershipEntity`, `TimelineCoverageEntity`, lossless mapper functions.

- [ ] **Step 1: Add Room test dependencies and write failing DAO tests**

Pin Room Android 2.x stable explicitly:

```kotlin
val roomVersion = "2.8.5"
implementation("androidx.room:room-runtime:$roomVersion")
implementation("androidx.room:room-ktx:$roomVersion")
ksp("androidx.room:room-compiler:$roomVersion")
testImplementation("androidx.room:room-testing:$roomVersion")
```

Create Robolectric tests proving:

```kotlin
@Test fun replaceDay_isAtomicAndMarksEmptyDayAvailable()
@Test fun observeRange_returnsItemsOverlappingEveryRequestedDayWithoutDuplicates()
@Test fun sameItemId_isIsolatedByAccountAndScope()
@Test fun purgeAccount_removesItemsMembershipAndCoverage()
```

- [ ] **Step 2: Run the DAO tests and verify RED**

Run:

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.data.timeline.local.TimelineCacheDaoTest"
```

Expected: compilation fails because the Room entities and DAO do not exist.

- [ ] **Step 3: Define normalized entities and DAO transaction**

Use composite keys and indexed UTC fields:

```kotlin
@Entity(primaryKeys = ["accountId", "scopeKey", "itemId"], indices = [Index("startEpochMs"), Index("endEpochMs")])
data class TimelineItemEntity(
    val accountId: String,
    val scopeKey: String,
    val itemId: String,
    val startEpochMs: Long,
    val endEpochMs: Long?,
    val payloadJson: String,
    val contentHash: String,
)

@Entity(primaryKeys = ["accountId", "scopeKey", "zoneId", "localDate", "itemId"])
data class TimelineDayMembershipEntity(
    val accountId: String,
    val scopeKey: String,
    val zoneId: String,
    val localDate: String,
    val itemId: String,
)

@Entity(primaryKeys = ["accountId", "scopeKey", "zoneId", "localDate"])
data class TimelineCoverageEntity(
    val accountId: String,
    val scopeKey: String,
    val zoneId: String,
    val localDate: String,
    val fetchedAtEpochMs: Long,
    val contractVersion: Int,
    val lastFailureKind: String?,
    val lastAccessedAtEpochMs: Long,
    val refreshGeneration: Long,
)
```

`TimelineCacheDao.replaceDays(...)` must run under `@Transaction`: upsert items, replace memberships only for returned coverage days, then upsert coverage even when the item list is empty. Do not use destructive migration fallback.

- [ ] **Step 4: Implement lossless mapping and verify content equality**

Serialize `CoreTimelineItem` using the existing `kotlinx.serialization` configuration. Hash the canonical serialized payload rather than item ID. Add a round-trip fixture with every nullable/non-null field populated and assert equality:

```kotlin
assertEquals(source, mapper.fromEntity(mapper.toEntity(accountId, scopeKey, source)))
assertNotEquals(mapper.contentHash(source), mapper.contentHash(source.copy(title = "changed")))
```

- [ ] **Step 5: Provide the database through Hilt and verify GREEN**

Create `TimelineCacheDatabase` at schema version 1 and provide singleton database/DAO instances. Run:

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.data.timeline.local.*"
```

Expected: all Task 1 tests pass.

- [ ] **Step 6: Commit Task 1**

```bash
git add app/build.gradle.kts app/src/main/java/app/tastile/android/data/timeline/local app/src/test/java/app/tastile/android/data/timeline/local app/src/main/java/app/tastile/android/data/di/ApiModule.kt
git commit -m "feat: add persistent timeline cache"
```

### Task 2: Page keys, immutable snapshots, and local-only repository

**Files:**
- Create: `app/src/main/java/app/tastile/android/data/timeline/TimelinePageModels.kt`
- Create: `app/src/main/java/app/tastile/android/data/timeline/TimelinePageRepository.kt`
- Create: `app/src/main/java/app/tastile/android/data/timeline/DefaultTimelinePageRepository.kt`
- Create: `app/src/test/java/app/tastile/android/data/timeline/TimelinePageRepositoryTest.kt`
- Create: `app/src/test/java/app/tastile/android/data/timeline/TimelinePageModelsTest.kt`
- Modify: `app/src/main/java/app/tastile/android/data/di/ApiModule.kt`

**Interfaces:**
- Consumes: `TimelineCacheDao` and entities from Task 1.
- Produces: `TimelinePageKey`, `TimelinePageSnapshot`, `TimelinePageRepository.observePage(key)`, `purgeAccount(accountId)`.

- [ ] **Step 1: Write failing page normalization tests**

Cover exact anchors and visible days:

```kotlin
@Test fun dayKey_containsOneLocalDate()
@Test fun weekKey_normalizesToMondayAndContainsSevenDays()
@Test fun monthKey_containsAllDatesInTheVisibleSixWeekGrid()
@Test fun dstSpringAndFallDays_keepLocalDateMembership()
```

The month implementation may return five weeks when the calendar grid actually needs five; it must return exactly the dates the UI renders, including adjacent-month cells.

- [ ] **Step 2: Run model tests and verify RED**

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.data.timeline.TimelinePageModelsTest"
```

Expected: missing page model types.

- [ ] **Step 3: Implement immutable page contracts**

Use persistent immutable collections at the UI boundary:

```kotlin
data class TimelinePageKey(
    val accountId: String,
    val scopeKey: String,
    val zoneId: ZoneId,
    val scale: TimelineScale,
    val anchor: LocalDate,
)

@Immutable
data class TimelinePageSnapshot(
    val key: TimelinePageKey,
    val items: ImmutableList<CoreTimelineItem>,
    val coverage: ImmutableMap<LocalDate, TimelineCoverageState>,
    val lastUpdatedAt: Instant?,
    val isRefreshing: Boolean,
    val isOffline: Boolean,
)
```

Add `implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.5.1")`.

- [ ] **Step 4: Write failing repository snapshot tests**

Use a fake DAO `Flow` to prove:

```kotlin
@Test fun observePage_readsOnlyLocalDaoAndNeverCallsNetwork()
@Test fun unchangedRows_reuseSnapshotInstance()
@Test fun changedTitle_emitsNewSnapshotEvenWhenIdsMatch()
@Test fun neverFetchedAndFetchedEmptyRemainDistinct()
@Test fun dayWeekMonthComposeTheSameMembershipRows()
```

- [ ] **Step 5: Implement local-only repository and verify GREEN**

The repository must `distinctUntilChanged` on full immutable snapshot content and use a small per-key instance cache so equal content reuses the previous snapshot instance. It must not import `V1ApiClient`, Retrofit, OkHttp, or `TileRepository`.

Run:

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.data.timeline.*"
```

- [ ] **Step 6: Commit Task 2**

```bash
git add app/src/main/java/app/tastile/android/data/timeline app/src/test/java/app/tastile/android/data/timeline app/src/main/java/app/tastile/android/data/di/ApiModule.kt app/build.gradle.kts
git commit -m "feat: expose local timeline page snapshots"
```

### Task 3: Background synchronization and coverage-aware prefetch

**Files:**
- Create: `app/src/main/java/app/tastile/android/data/timeline/TimelineSyncRepository.kt`
- Create: `app/src/main/java/app/tastile/android/data/timeline/DefaultTimelineSyncRepository.kt`
- Create: `app/src/main/java/app/tastile/android/data/timeline/TimelineRefreshCoordinator.kt`
- Create: `app/src/test/java/app/tastile/android/data/timeline/TimelineSyncRepositoryTest.kt`
- Create: `app/src/test/java/app/tastile/android/data/timeline/TimelineRefreshCoordinatorTest.kt`
- Modify: `app/src/main/java/app/tastile/android/data/di/ApiModule.kt`

**Interfaces:**
- Consumes: canonical timeline fetch through existing `TileRepository.getTimeline(start, end, ownerIds)` and Task 1 DAO.
- Produces: `requestRefresh(keys, direction)`, transactional cache writes, in-flight request coalescing.

- [ ] **Step 1: Write failing sync correctness tests**

Tests must cover:

```kotlin
@Test fun refresh_writesItemsMembershipAndCoverageInOneDaoCall()
@Test fun emptySuccess_marksDaysAvailableAndRemovesOldMembership()
@Test fun networkFailure_preservesExistingRowsAndRecordsFailure()
@Test fun overlappingRequests_areCoalesced()
@Test fun olderGeneration_cannotOverwriteNewerCoverage()
@Test fun overnightItem_isAssignedToEveryOverlappingLocalDate()
```

- [ ] **Step 2: Run sync tests and verify RED**

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.data.timeline.TimelineSyncRepositoryTest"
```

- [ ] **Step 3: Implement finite range synchronization**

The sync repository must:

```kotlin
interface TimelineSyncRepository {
    suspend fun refresh(request: TimelineRefreshRequest): TimelineRefreshResult
}
```

Normalize and merge adjacent missing/stale local dates into finite UTC ranges using the page zone. Fetch raw items, validate times, assign memberships by interval overlap, and call one DAO transaction. Never clear cache before network success.

- [ ] **Step 4: Write failing prefetch priority tests**

```kotlin
@Test fun coordinator_prioritizesCurrentMissingThenSwipeDirectionThenOppositeThenStale()
@Test fun coordinator_doesNotRefreshFreshCoverage()
@Test fun coordinator_refreshesStaleCoverageWithoutBlockingSnapshots()
```

- [ ] **Step 5: Implement coordinator and verify GREEN**

Keep network jobs outside UI state. `requestRefresh` returns immediately after enqueueing work in the injected application scope. Store refreshing/failure metadata through DAO so UI derives it from local state.

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.data.timeline.TimelineSyncRepositoryTest" --tests "app.tastile.android.data.timeline.TimelineRefreshCoordinatorTest"
```

- [ ] **Step 6: Commit Task 3**

```bash
git add app/src/main/java/app/tastile/android/data/timeline app/src/test/java/app/tastile/android/data/timeline app/src/main/java/app/tastile/android/data/di/ApiModule.kt
git commit -m "feat: synchronize timeline cache in background"
```

### Task 4: Page-scoped screen state and continuous Day / Week / Month pager

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/timeline/TimelinePageViewModel.kt`
- Create: `app/src/main/java/app/tastile/android/ui/timeline/TimelinePageUiState.kt`
- Create: `app/src/test/java/app/tastile/android/ui/timeline/TimelinePageViewModelTest.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/calendar/DayView.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/calendar/WeekView.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/calendar/MonthView.kt`
- Create: `app/src/androidTest/java/app/tastile/android/ui/mobile/tabs/TimelineAdjacentPageTest.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardViewModel.kt`

**Interfaces:**
- Consumes: Task 2 page repository and Task 3 refresh coordinator.
- Produces: page-keyed UI state; pager content that receives only one page snapshot.

- [ ] **Step 1: Write failing ViewModel tests for three visible pages**

```kotlin
@Test fun visiblePages_observePreviousCurrentAndNextKeys()
@Test fun swipeDirection_requestsDirectionalPrefetch()
@Test fun switchingScale_preservesIndependentAnchorAndZoom()
@Test fun unrelatedPageEmission_doesNotReplaceCurrentSnapshot()
```

- [ ] **Step 2: Run ViewModel tests and verify RED**

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.ui.timeline.TimelinePageViewModelTest"
```

- [ ] **Step 3: Implement page-scoped state holder**

Expose one state object whose page map is immutable:

```kotlin
@Immutable
data class TimelinePageUiState(
    val scale: TimelineScale,
    val anchors: ImmutableMap<TimelineScale, LocalDate>,
    val pages: ImmutableMap<TimelinePageKey, TimelinePageSnapshot>,
    val isReadOnly: Boolean,
)
```

Do not expose API loading jobs or raw responses. Remove Timeline list ownership and refresh methods from `DashboardViewModel` only after all Timeline call sites use the new state holder.

- [ ] **Step 4: Write failing Compose test for adjacent pages**

Add stable test tags containing scale and normalized anchor. Assert current and both adjacent page nodes exist with `beyondViewportPageCount = 1`, and a partial swipe retains both origin and destination content.

```kotlin
composeRule.onNodeWithTag("timeline-day-2026-09-16").assertExists()
composeRule.onNodeWithTag("timeline-day-2026-09-17").assertExists()
```

- [ ] **Step 5: Refactor pager and renderers**

Use separate remembered pager/zoom state per scale. Give `HorizontalPager` a stable anchor key and `beyondViewportPageCount = 1`. Each page obtains only its snapshot and passes immutable item/block collections to `DayView`, `WeekView`, or `MonthView`. Do not collect the global `DashboardViewModel.timeline` in `TimelineScreen`.

- [ ] **Step 6: Verify unit and instrumented tests**

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.ui.timeline.*" --tests "app.tastile.android.ui.mobile.tabs.Timeline*"
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.ui.mobile.tabs.TimelineAdjacentPageTest
```

- [ ] **Step 7: Commit Task 4**

```bash
git add app/src/main/java/app/tastile/android/ui/timeline app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt app/src/main/java/app/tastile/android/ui/mobile/calendar app/src/main/java/app/tastile/android/ui/dashboard/DashboardViewModel.kt app/src/test/java/app/tastile/android/ui app/src/androidTest/java/app/tastile/android/ui/mobile/tabs/TimelineAdjacentPageTest.kt
git commit -m "perf: render timeline from page-local snapshots"
```

### Task 5: Offline read-only entry, mutation gating, logout purge, and retention

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/navigation/AppRoot.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/navigation/MainActivity.kt`
- Modify: `app/src/main/java/app/tastile/android/data/auth/AuthRepository.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt`
- Create: `app/src/main/java/app/tastile/android/data/timeline/TimelineCachePruner.kt`
- Create: `app/src/test/java/app/tastile/android/data/timeline/TimelineCachePrunerTest.kt`
- Create: `app/src/test/java/app/tastile/android/ui/timeline/TimelineOfflinePolicyTest.kt`
- Modify: `app/src/androidTest/java/app/tastile/android/ui/navigation/MainActivityAuthGateTest.kt`

**Interfaces:**
- Consumes: cached authenticated identity, security lock state, page coverage, DAO purge/prune methods.
- Produces: offline cached-account entry policy, read-only UI policy, logout purge, 90-day protected retention window.

- [ ] **Step 1: Write failing offline policy tests**

```kotlin
@Test fun cachedIdentityAndNetworkFailure_entersTimelineReadOnly()
@Test fun explicitLogout_neverAllowsOfflineEntryAndPurgesAccountCache()
@Test fun offlineMutationActions_areDisabled()
@Test fun uncoveredOfflinePage_showsNotDownloadedInsteadOfEmpty()
```

- [ ] **Step 2: Run policy tests and verify RED**

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.ui.timeline.TimelineOfflinePolicyTest"
```

- [ ] **Step 3: Implement explicit offline identity state**

Represent offline cached-account access explicitly; do not reinterpret arbitrary authentication errors as authenticated:

```kotlin
sealed interface TimelineAccessMode {
    data object Online : TimelineAccessMode
    data class OfflineReadOnly(val accountId: String) : TimelineAccessMode
    data object SignedOut : TimelineAccessMode
}
```

Only a previously authenticated, locally persisted identity that has not been explicitly logged out may enter `OfflineReadOnly`. Apply the existing security-lock rule before rendering cached private data.

- [ ] **Step 4: Gate mutation controls and purge on logout/account switch**

All Timeline create/edit/execution callbacks must render disabled or route to a localized offline explanation when read-only. Do not enqueue commands. Invoke `TimelinePageRepository.purgeAccount` on explicit logout, account deletion, and account switch.

- [ ] **Step 5: Write failing retention tests and implement pruning**

Protect local dates within today ±90 days. Outside that window delete least-recently-accessed coverage first, then orphan memberships/items. Pruning runs in background and never from a Compose render path.

```kotlin
@Test fun prune_neverDeletesTodayPlusOrMinusNinetyDays()
@Test fun prune_deletesLeastRecentlyAccessedCoverageAndOrphanItems()
```

- [ ] **Step 6: Verify Task 5 GREEN**

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.ui.timeline.*" --tests "app.tastile.android.data.timeline.TimelineCachePrunerTest"
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.ui.navigation.MainActivityAuthGateTest
```

- [ ] **Step 7: Commit Task 5**

```bash
git add app/src/main/java/app/tastile/android/ui/navigation app/src/main/java/app/tastile/android/data/auth app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt app/src/main/java/app/tastile/android/data/timeline app/src/test/java/app/tastile/android app/src/androidTest/java/app/tastile/android/ui/navigation/MainActivityAuthGateTest.kt
git commit -m "feat: support offline read-only timeline"
```

### Task 6: Contract, performance, real API, and full verification

**Files:**
- Create: `app/src/test/java/app/tastile/android/data/timeline/TimelineContractParityTest.kt`
- Create: `app/src/androidTest/java/app/tastile/android/ui/mobile/tabs/TimelineOfflineDeviceTest.kt`

**Interfaces:**
- Consumes: complete Tasks 1–5 candidate.
- Produces: contract evidence, compiler reports, real-device and real-API evidence; no new production behavior.

- [ ] **Step 1: Add contract parity regression test**

Round-trip a canonical `CoreTimelineItem` fixture through API model → Room entity → page snapshot and assert every field is preserved. Add a reflection/serialization key assertion matching the current canonical generated/OpenAPI model; do not add aliases.

- [ ] **Step 2: Run focused and full JVM verification**

```bash
./gradlew testDebugUnitTest --tests "app.tastile.android.data.timeline.*" --tests "app.tastile.android.ui.timeline.*"
./gradlew testDebugUnitTest
./gradlew lintDebug
```

- [ ] **Step 3: Generate and inspect Compose compiler reports**

```bash
./gradlew :app:compileDebugKotlin -PcomposeReports=true
```

Verify `TimelineScreen`, page content, `DayView`, `WeekView`, and `MonthView` are skippable and unchanged page parameters retain equality/identity expected under strong skipping.

- [ ] **Step 4: Build, install, and test the real device**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
./gradlew connectedDebugAndroidTest
```

On device `8a3611541872`, use an authenticated real account without logging credentials. Exercise Day / Week / Month slow swipe, round trip, process kill/restart, airplane mode over previously fetched ranges, uncovered range, and reconnect.

- [ ] **Step 5: Capture API and frame evidence**

Reset `dumpsys gfxinfo`, execute the same transition before/after, and record total/janky frames. Capture network requests to prove cache revisit does not require an API response and background refresh does not clear UI. Store logs only under root `.tmp/`; do not commit them.

- [ ] **Step 6: Run repository gate**

```bash
./gradlew verify
```

Expected: PASS with no warning suppression or skipped applicable tests.

- [ ] **Step 7: Commit verification assets**

```bash
git add app/src/test/java/app/tastile/android/data/timeline/TimelineContractParityTest.kt app/src/androidTest/java/app/tastile/android/ui/mobile/tabs/TimelineOfflineDeviceTest.kt
git commit -m "test: verify offline-first timeline behavior"
```
