package app.tastile.android.data.timeline.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TimelineCacheDaoTest {

    private lateinit var database: TimelineCacheDatabase
    private lateinit var dao: TimelineCacheDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TimelineCacheDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.timelineCacheDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replaceDay_isAtomicAndMarksEmptyDayAvailable() = runBlocking {
        val day = "2026-09-16"
        val original = timelineEntity("placement-1", "Original")
        val replacement = timelineEntity("placement-2", "Replacement")

        dao.replaceDays(
            items = listOf(original),
            memberships = listOf(membership(day, original.itemId)),
            coverage = listOf(coverage(day, fetchedAtEpochMs = 100L)),
        )
        assertEquals(listOf(original), dao.observeRange(ACCOUNT_ID, SCOPE_KEY, ZONE_ID, listOf(day)).first())

        dao.replaceDays(
            items = listOf(replacement),
            memberships = listOf(membership(day, replacement.itemId)),
            coverage = listOf(coverage(day, fetchedAtEpochMs = 200L)),
        )
        assertEquals(listOf(replacement), dao.observeRange(ACCOUNT_ID, SCOPE_KEY, ZONE_ID, listOf(day)).first())

        dao.replaceDays(
            items = emptyList(),
            memberships = emptyList(),
            coverage = listOf(coverage(day, fetchedAtEpochMs = 300L)),
        )

        assertEquals(emptyList<TimelineItemEntity>(), dao.observeRange(ACCOUNT_ID, SCOPE_KEY, ZONE_ID, listOf(day)).first())
        assertEquals(
            listOf(coverage(day, fetchedAtEpochMs = 300L)),
            dao.observeCoverage(ACCOUNT_ID, SCOPE_KEY, ZONE_ID, listOf(day)).first(),
        )
    }

    @Test
    fun observeRange_returnsItemsOverlappingEveryRequestedDayWithoutDuplicates() = runBlocking {
        val firstDay = "2026-09-16"
        val secondDay = "2026-09-17"
        val overnight = timelineEntity(
            id = "overnight",
            title = "Overnight",
            startEpochMs = 100L,
            endEpochMs = 300L,
        )
        val second = timelineEntity(
            id = "second",
            title = "Second",
            startEpochMs = 250L,
            endEpochMs = 400L,
        )

        dao.replaceDays(
            items = listOf(overnight, second),
            memberships = listOf(
                membership(firstDay, overnight.itemId),
                membership(secondDay, overnight.itemId),
                membership(secondDay, second.itemId),
            ),
            coverage = listOf(
                coverage(firstDay, fetchedAtEpochMs = 100L),
                coverage(secondDay, fetchedAtEpochMs = 100L),
            ),
        )

        assertEquals(
            listOf(overnight, second),
            dao.observeRange(
                accountId = ACCOUNT_ID,
                scopeKey = SCOPE_KEY,
                zoneId = ZONE_ID,
                localDates = listOf(firstDay, secondDay),
                rangeStartEpochMs = 150L,
                rangeEndEpochMs = 350L,
            ).first(),
        )
    }

    @Test
    fun sameItemId_isIsolatedByAccountAndScope() = runBlocking {
        val day = "2026-09-16"
        val accountItem = timelineEntity("shared-id", "Account A", accountId = "account-a")
        val otherAccountItem = timelineEntity("shared-id", "Account B", accountId = "account-b")
        val otherScopeItem = timelineEntity("shared-id", "Scope B", scopeKey = "scope-b")

        dao.replaceDays(
            items = listOf(accountItem, otherAccountItem, otherScopeItem),
            memberships = listOf(
                membership(day, accountItem.itemId),
                membership(day, otherAccountItem.itemId, accountId = "account-b"),
                membership(day, otherScopeItem.itemId, scopeKey = "scope-b"),
            ),
            coverage = listOf(
                coverage(day),
                coverage(day, accountId = "account-b"),
                coverage(day, scopeKey = "scope-b"),
            ),
        )

        assertEquals(
            listOf(accountItem),
            dao.observeRange("account-a", SCOPE_KEY, ZONE_ID, listOf(day)).first(),
        )
        assertEquals(
            listOf(otherAccountItem),
            dao.observeRange("account-b", SCOPE_KEY, ZONE_ID, listOf(day)).first(),
        )
        assertEquals(
            listOf(otherScopeItem),
            dao.observeRange(ACCOUNT_ID, "scope-b", ZONE_ID, listOf(day)).first(),
        )
    }

    @Test
    fun purgeAccount_removesItemsMembershipAndCoverage() = runBlocking {
        val day = "2026-09-16"
        val retained = timelineEntity("retained", "Retained", accountId = "account-b")
        val purged = timelineEntity("purged", "Purged")

        dao.replaceDays(
            items = listOf(retained, purged),
            memberships = listOf(
                membership(day, retained.itemId, accountId = "account-b"),
                membership(day, purged.itemId),
            ),
            coverage = listOf(
                coverage(day, accountId = "account-b"),
                coverage(day),
            ),
        )

        dao.purgeAccount(ACCOUNT_ID)

        assertEquals(
            emptyList<TimelineItemEntity>(),
            dao.observeRange(ACCOUNT_ID, SCOPE_KEY, ZONE_ID, listOf(day)).first(),
        )
        assertEquals(
            emptyList<TimelineCoverageEntity>(),
            dao.observeCoverage(ACCOUNT_ID, SCOPE_KEY, ZONE_ID, listOf(day)).first(),
        )
        assertEquals(
            listOf(retained),
            dao.observeRange("account-b", SCOPE_KEY, ZONE_ID, listOf(day)).first(),
        )
    }

    @Test
    fun recordFailures_updatesMetadataOnlyAndKeepsCachedRows() = runBlocking {
        val day = "2026-09-16"
        val item = timelineEntity("kept", "Kept")
        val row = coverage(day, fetchedAtEpochMs = 100L)
        dao.replaceDays(
            items = listOf(item),
            memberships = listOf(membership(day, item.itemId)),
            coverage = listOf(row),
        )

        dao.recordFailures(
            listOf(
                row.copy(
                    fetchedAtEpochMs = 999L,
                    lastFailureKind = "network",
                    lastAccessedAtEpochMs = 999L,
                    refreshGeneration = 2L,
                ),
            ),
        )

        assertEquals(
            listOf(item),
            dao.observeRange(ACCOUNT_ID, SCOPE_KEY, ZONE_ID, listOf(day)).first(),
        )
        assertEquals(
            row.copy(
                lastFailureKind = "network",
                refreshGeneration = 2L,
            ),
            dao.observeCoverage(ACCOUNT_ID, SCOPE_KEY, ZONE_ID, listOf(day)).first().single(),
        )
    }

    @Test
    fun replaceDays_ignoresOlderRefreshGeneration() = runBlocking {
        val day = "2026-09-16"
        val newer = timelineEntity("newer", "Newer")
        val older = timelineEntity("older", "Older")
        dao.replaceDays(
            items = listOf(newer),
            memberships = listOf(membership(day, newer.itemId)),
            coverage = listOf(coverage(day, fetchedAtEpochMs = 200L).copy(refreshGeneration = 2L)),
        )

        dao.replaceDays(
            items = listOf(older),
            memberships = listOf(membership(day, older.itemId)),
            coverage = listOf(coverage(day, fetchedAtEpochMs = 300L).copy(refreshGeneration = 1L)),
        )

        assertEquals(
            listOf(newer),
            dao.observeRange(ACCOUNT_ID, SCOPE_KEY, ZONE_ID, listOf(day)).first(),
        )
        assertEquals(
            2L,
            dao.observeCoverage(ACCOUNT_ID, SCOPE_KEY, ZONE_ID, listOf(day)).first().single().refreshGeneration,
        )
    }

    private fun timelineEntity(
        id: String,
        title: String,
        accountId: String = ACCOUNT_ID,
        scopeKey: String = SCOPE_KEY,
        startEpochMs: Long = 100L,
        endEpochMs: Long? = 200L,
    ) = TimelineItemEntity(
        accountId = accountId,
        scopeKey = scopeKey,
        itemId = id,
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs,
        payloadJson = "{}",
        contentHash = title,
    )

    private fun membership(
        localDate: String,
        itemId: String,
        accountId: String = ACCOUNT_ID,
        scopeKey: String = SCOPE_KEY,
    ) = TimelineDayMembershipEntity(
        accountId = accountId,
        scopeKey = scopeKey,
        zoneId = ZONE_ID,
        localDate = localDate,
        itemId = itemId,
    )

    private fun coverage(
        localDate: String,
        accountId: String = ACCOUNT_ID,
        scopeKey: String = SCOPE_KEY,
        fetchedAtEpochMs: Long = 100L,
    ) = TimelineCoverageEntity(
        accountId = accountId,
        scopeKey = scopeKey,
        zoneId = ZONE_ID,
        localDate = localDate,
        fetchedAtEpochMs = fetchedAtEpochMs,
        contractVersion = 1,
        lastFailureKind = null,
        lastAccessedAtEpochMs = fetchedAtEpochMs,
        refreshGeneration = 1L,
    )

    private companion object {
        const val ACCOUNT_ID = "account-a"
        const val SCOPE_KEY = "scope-a"
        const val ZONE_ID = "UTC"
    }
}
