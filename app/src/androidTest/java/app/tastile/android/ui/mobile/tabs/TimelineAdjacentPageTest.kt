package app.tastile.android.ui.mobile.tabs

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.data.tile.TileFilter
import app.tastile.android.data.timeline.TimelinePageKey
import app.tastile.android.data.timeline.TimelinePageRepository
import app.tastile.android.data.timeline.TimelinePageSnapshot
import app.tastile.android.data.timeline.TimelineRefreshDirection
import app.tastile.android.data.timeline.timelineScopeFingerprint
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.timeline.TimelinePageRefreshRequester
import app.tastile.android.ui.timeline.TimelinePageViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Real-device regression coverage for page-local timeline paging. Every scale
 * receives independent snapshot content and pager state; the dashboard's
 * legacy aggregate timeline is deliberately not part of this fixture.
 */
class TimelineAdjacentPageTest {
    @get:Rule
    val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test
    fun dayPager_keepsAdjacentSnapshotsDuringPartialDrag_andPrefetchesNext() {
        assertAdjacentPager(TimelineScale.Day, LocalDate.of(2026, 9, 16))
    }

    @Test
    fun weekPager_keepsAdjacentSnapshotsDuringPartialDrag_andPrefetchesNext() {
        assertAdjacentPager(TimelineScale.Week, LocalDate.of(2026, 9, 16))
    }

    @Test
    fun monthPager_keepsAdjacentSnapshotsDuringPartialDrag_andPrefetchesNext() {
        assertAdjacentPager(TimelineScale.Month, LocalDate.of(2026, 9, 16))
    }

    @Test
    fun scaleSwitch_retainsIndependentAnchorAndZoomForAllCalendarScales() {
        val anchor = LocalDate.of(2026, 9, 16)
        val owners = listOf("owner-b", "owner-a")
        val key = TimelinePageKey.forScope(
            accountId = "account-a",
            ownerIds = owners,
            zoneId = ZoneId.of("UTC"),
            scale = TimelineScale.Day,
            anchor = anchor,
        )
        val pageViewModel = TimelinePageViewModel(
            pageRepository = SnapshotRepository(),
            refreshRequester = TimelinePageRefreshRequester { _, _ -> },
            initialKey = key,
        )

        pageViewModel.setAnchor(TimelineScale.Day, anchor.plusDays(2))
        pageViewModel.setZoom(TimelineScale.Day, 2.25f)
        pageViewModel.setScale(TimelineScale.Week)
        pageViewModel.setAnchor(TimelineScale.Week, LocalDate.of(2026, 9, 21))
        pageViewModel.setZoom(TimelineScale.Week, 3.25f)
        pageViewModel.setScale(TimelineScale.Month)
        pageViewModel.setAnchor(TimelineScale.Month, LocalDate.of(2026, 11, 1))
        pageViewModel.setZoom(TimelineScale.Month, 4.25f)

        assertEquals(LocalDate.of(2026, 9, 18), pageViewModel.uiState.value.anchors[TimelineScale.Day])
        assertEquals(2.25f, pageViewModel.uiState.value.zoomFor(TimelineScale.Day))
        assertEquals(LocalDate.of(2026, 9, 21), pageViewModel.uiState.value.anchors[TimelineScale.Week])
        assertEquals(3.25f, pageViewModel.uiState.value.zoomFor(TimelineScale.Week))
        assertEquals(LocalDate.of(2026, 11, 1), pageViewModel.uiState.value.anchors[TimelineScale.Month])
        assertEquals(4.25f, pageViewModel.uiState.value.zoomFor(TimelineScale.Month))
        assertEquals(timelineScopeFingerprint(listOf("owner-a", "owner-b")), key.scopeFingerprint)
    }

    private fun assertAdjacentPager(scale: TimelineScale, inputAnchor: LocalDate) {
        val anchor = app.tastile.android.data.timeline.normalizeTimelineAnchor(scale, inputAnchor)
        val owners = listOf("owner-b", "owner-a")
        val key = TimelinePageKey.forScope(
            accountId = "account-a",
            ownerIds = owners,
            zoneId = ZoneId.of("UTC"),
            scale = scale,
            anchor = anchor,
        ).normalized()
        val repository = SnapshotRepository()
        val refreshes = RecordingRefreshRequester()
        val pageViewModel = TimelinePageViewModel(
            pageRepository = repository,
            refreshRequester = refreshes,
            initialKey = key,
        )
        val dashboard = mockk<DashboardViewModel>(relaxed = true)
        every { dashboard.selectedDay } returns MutableStateFlow(inputAnchor)
        every { dashboard.scale } returns MutableStateFlow(scale)
        every { dashboard.timelineAccountId } returns "account-a"
        every { dashboard.tileFilter } returns MutableStateFlow(TileFilter(ownerIds = owners))
        val overlay = mockk<OverlayViewModel>(relaxed = true)

        compose.setContent {
            TastileTheme {
                TimelineScreen(
                    viewModel = dashboard,
                    overlay = overlay,
                    pageViewModel = pageViewModel,
                )
            }
        }
        compose.waitForIdle()

        val previous = shiftAnchor(key, -1)
        val current = key.normalized()
        val next = shiftAnchor(key, 1)
        val previousTag = pageTag(previous)
        val currentTag = pageTag(current)
        val nextTag = pageTag(next)

        compose.onNodeWithTag(previousTag, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(currentTag, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(nextTag, useUnmergedTree = true).assertExists()
        assertSnapshotRendered(repository.snapshotFor(previous))
        assertSnapshotRendered(repository.snapshotFor(current))
        assertSnapshotRendered(repository.snapshotFor(next))
        assertNotEquals(
            repository.snapshotFor(current).items.single().title,
            repository.snapshotFor(next).items.single().title,
        )

        // Move by a viewport-relative distance rather than relying on a fixed
        // pixel threshold. Both page roots must stay composed after this
        // partial drag, regardless of device density or test window width.
        compose.onNodeWithTag(currentTag, useUnmergedTree = true).performTouchInput {
            val distance = width * 0.40f
            down(Offset(centerX + distance / 2f, centerY))
            moveBy(Offset(-distance, 0f), delayMillis = 350)
            up()
        }
        compose.waitForIdle()
        compose.onNodeWithTag(currentTag, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(nextTag, useUnmergedTree = true).assertExists()

        // A complete gesture settles on the destination and sends a
        // directional refresh intent. The distance is still derived from the
        // measured viewport, not a magic 48px offset.
        compose.onNodeWithTag(currentTag, useUnmergedTree = true).performTouchInput {
            val distance = width * 0.84f
            down(Offset(centerX + distance / 2f, centerY))
            moveBy(Offset(-distance, 0f), delayMillis = 500)
            up()
        }
        compose.waitForIdle()

        assertTrue(refreshes.intents.any { it.direction == TimelineRefreshDirection.Next })
        assertEquals(next.anchor, pageViewModel.currentPageKey?.anchor)
        verify { dashboard.setSelectedDay(next.anchor) }
        compose.onNodeWithTag(nextTag, useUnmergedTree = true).assertExists()
    }

    private fun assertSnapshotRendered(snapshot: TimelinePageSnapshot) {
        compose.onNodeWithContentDescription(
            timelineSnapshotSemantics(snapshot),
            useUnmergedTree = true,
        ).assertExists()
    }

    private fun shiftAnchor(key: TimelinePageKey, amount: Long): TimelinePageKey {
        val anchor = when (key.scale) {
            TimelineScale.Day,
            TimelineScale.List,
            -> key.anchor.plusDays(amount)

            TimelineScale.Week -> key.anchor.plusWeeks(amount)
            TimelineScale.Month -> key.anchor.plusMonths(amount)
        }
        return key.copy(anchor = anchor).normalized()
    }

    private fun pageTag(key: TimelinePageKey): String =
        "timeline-${key.scale.name.lowercase(Locale.ROOT)}-${key.normalizedAnchor}"

    private data class RefreshIntent(
        val keys: List<TimelinePageKey>,
        val direction: TimelineRefreshDirection,
    )

    private class RecordingRefreshRequester : TimelinePageRefreshRequester {
        val intents = CopyOnWriteArrayList<RefreshIntent>()

        override fun requestRefresh(
            keys: Collection<TimelinePageKey>,
            direction: TimelineRefreshDirection,
        ) {
            intents += RefreshIntent(keys.toList(), direction)
        }
    }

    private class SnapshotRepository : TimelinePageRepository {
        private val snapshots = CopyOnWriteArrayList<TimelinePageSnapshot>()

        override fun observePage(key: TimelinePageKey): Flow<TimelinePageSnapshot> {
            val normalized = key.normalized()
            val snapshot = TimelinePageSnapshot(
                key = normalized,
                items = persistentListOf(
                    CoreTimelineItem(
                        id = "item-${normalized.scale}-${normalized.anchor}",
                        title = "${normalized.scale.name}-${normalized.anchor}",
                        type = "placement",
                        status = "open",
                        startAt = "${normalized.anchor}T09:00:00Z",
                        endAt = "${normalized.anchor}T10:00:00Z",
                    ),
                ),
            )
            snapshots += snapshot
            return flowOf(snapshot)
        }

        override suspend fun purgeAccount(accountId: String) = Unit

        fun snapshotFor(key: TimelinePageKey): TimelinePageSnapshot = snapshots
            .last { it.key.normalized() == key.normalized() }
    }
}
