package app.tastile.android.data.timeline.local

import app.tastile.android.core.CoreTimelineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TimelineCacheMapperTest {

    @Test
    fun roundTrip_preservesEveryTimelineField() {
        val source = CoreTimelineItem(
            id = "placement-1",
            tileId = "tile-1",
            sourceKind = 4,
            title = "Deep work",
            type = "work",
            status = "active",
            startAt = "2026-09-16T09:15:00Z",
            endAt = "2026-09-16T10:45:00Z",
            sourceTileId = "source-tile-1",
        )

        val entity = TimelineCacheMapper.toEntity("account-a", "scope-a", source)

        assertEquals(source, TimelineCacheMapper.fromEntity(entity))
    }

    @Test
    fun contentHash_changesWhenPayloadContentChanges() {
        val source = CoreTimelineItem(
            id = "placement-1",
            tileId = "tile-1",
            sourceKind = 4,
            title = "Deep work",
            type = "work",
            status = "active",
            startAt = "2026-09-16T09:15:00Z",
            endAt = "2026-09-16T10:45:00Z",
            sourceTileId = "source-tile-1",
        )

        assertNotEquals(
            TimelineCacheMapper.contentHash(source),
            TimelineCacheMapper.contentHash(source.copy(title = "Changed")),
        )
    }
}
