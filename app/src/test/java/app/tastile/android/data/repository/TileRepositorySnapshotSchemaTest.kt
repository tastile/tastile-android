package app.tastile.android.data.repository

import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.core.CoreTimelineItem
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class TileRepositorySnapshotSchemaTest {

    @Test
    fun snapshotRowToTile_mapsCompletedSnapshotToDoneLifecycle() {
        val row = SnapshotTileRow(
            tileId = "tile-1",
            title = "From web",
            closedAt = "2026-04-04T12:00:00Z",
            tileJson = buildJsonObject {
                put(
                    "core",
                    buildJsonObject {
                        put("title", JsonPrimitive("From web"))
                        put("nextAction", JsonPrimitive("ship"))
                    }
                )
            }
        )

        val tile = snapshotRowToTile(row, "user-1")

        assertEquals("tile-1", tile.id)
        assertEquals(TileLifecycle.DONE.value, tile.lifecycle)
        assertEquals("ship", tile.nextAction)
    }

    @Test
    fun snapshotRowToTile_mapsStartedSnapshotToStartedLifecycle() {
        val row = SnapshotTileRow(
            tileId = "tile-2",
            title = "Active",
            tileJson = buildJsonObject {
                put(
                    "core",
                    buildJsonObject {
                        put("title", JsonPrimitive("Active"))
                        put("startedAt", JsonPrimitive("2026-04-04T12:00:00Z"))
                    }
                )
            }
        )

        val tile = snapshotRowToTile(row, "user-1")

        assertEquals("tile-2", tile.id)
        assertEquals(TileLifecycle.STARTED.value, tile.lifecycle)
    }

    @Test
    fun buildTimelineFromTiles_createsTimelineEntriesWhenCoreTimelineIsEmpty() {
        val started = SnapshotTileRow(
            tileId = "tile-started",
            title = "Active",
            tileJson = buildJsonObject {
                put(
                    "core",
                    buildJsonObject {
                        put("title", JsonPrimitive("Active"))
                        put("startedAt", JsonPrimitive("2026-04-05T00:00:00Z"))
                    }
                )
            }
        )
        val done = SnapshotTileRow(
            tileId = "tile-done",
            title = "Done",
            closedAt = "2026-04-05T00:30:00Z",
            tileJson = buildJsonObject {
                put(
                    "core",
                    buildJsonObject {
                        put("title", JsonPrimitive("Done"))
                        put("startedAt", JsonPrimitive("2026-04-04T23:00:00Z"))
                        put("completedAt", JsonPrimitive("2026-04-05T00:30:00Z"))
                    }
                )
            }
        )

        val tiles = listOf(
            snapshotRowToTile(started, "user-1"),
            snapshotRowToTile(done, "user-1")
        )

        val timeline = buildTimelineFromTiles(tiles, Instant.parse("2026-04-05T01:00:00Z"))

        val filtered = timeline.filter { it.tileId != null }
        assertEquals(2, filtered.size)
        val byId = filtered.associateBy { it.tileId }
        val startedTimeline = byId["tile-started"]
        val doneTimeline = byId["tile-done"]
        assertNotNull(startedTimeline)
        assertNotNull(doneTimeline)
        assertEquals("active", startedTimeline?.status)
        assertNotNull(startedTimeline?.endAt)
        assertEquals("done", doneTimeline?.status)
        assertEquals("2026-04-05T00:30:00Z", doneTimeline?.endAt)
    }

    @Test
    fun snapshotRowToTile_usesTemporalFixedStartWhenCoreStartedAtIsMissing() {
        val row = SnapshotTileRow(
            tileId = "tile-fixed-start",
            title = "Scheduled",
            tileJson = buildJsonObject {
                put(
                    "core",
                    buildJsonObject {
                        put("title", JsonPrimitive("Scheduled"))
                    }
                )
                put(
                    "temporal",
                    buildJsonObject {
                        put("fixedStart", JsonPrimitive("2026-04-05T08:00:00Z"))
                        put("fixedEnd", JsonPrimitive("2026-04-05T08:30:00Z"))
                    }
                )
            }
        )

        val tile = snapshotRowToTile(row, "user-1")
        val timeline = buildTimelineFromTiles(listOf(tile), Instant.parse("2026-04-05T09:00:00Z"))

        assertEquals("2026-04-05T08:00:00Z", tile.createdAt)
        assertEquals(1, timeline.size)
        assertEquals("2026-04-05T08:00:00Z", timeline.first().startAt)
        assertEquals("2026-04-05T08:30:00Z", timeline.first().endAt)
    }

    @Test
    fun snapshotRowToTile_mapsTemporalSnakeCaseAndBuildsScheduledDurationBlock() {
        val row = SnapshotTileRow(
            tileId = "tile-scheduled",
            title = "Planned block",
            tileJson = buildJsonObject {
                put(
                    "core",
                    buildJsonObject {
                        put("title", JsonPrimitive("Planned block"))
                    }
                )
                put(
                    "temporal",
                    buildJsonObject {
                        put("fixed_start", JsonPrimitive("2026-04-05T10:00:00Z"))
                        put("fixed_end", JsonPrimitive("2026-04-05T11:00:00Z"))
                    }
                )
            }
        )

        val tile = snapshotRowToTile(row, "user-1")
        val timeline = buildTimelineFromTiles(listOf(tile), Instant.parse("2026-04-05T09:00:00Z"))

        assertEquals("Ready", tile.lifecycle)
        assertEquals(1, timeline.size)
        assertEquals("2026-04-05T10:00:00Z", timeline.first().startAt)
        assertEquals("2026-04-05T11:00:00Z", timeline.first().endAt)
    }

    @Test
    fun buildTimelineFromTiles_skipsUnschedulableReadyTileWithoutTimeRange() {
        val row = SnapshotTileRow(
            tileId = "tile-no-time",
            title = "No time",
            tileJson = buildJsonObject {
                put(
                    "core",
                    buildJsonObject {
                        put("title", JsonPrimitive("No time"))
                    }
                )
            }
        )

        val tile = snapshotRowToTile(row, "user-1")
        val timeline = buildTimelineFromTiles(listOf(tile), Instant.parse("2026-04-05T09:00:00Z"))

        assertEquals(0, timeline.size)
    }

    @Test
    fun normalizeCoreTimeline_filtersSyntheticBreaksAndClampsToday() {
        val now = Instant.parse("2026-04-05T12:00:00Z")
        val source = listOf(
            CoreTimelineItem(
                id = "break-1",
                tileId = "synthetic:break:1:2:3",
                title = "Break (30min)",
                type = "break",
                status = "scheduled",
                startAt = "2026-04-05T10:00:00Z",
                endAt = "2026-04-05T10:30:00Z"
            ),
            CoreTimelineItem(
                id = "work-1",
                tileId = "tile-1",
                title = "Work",
                type = "work",
                status = "scheduled",
                startAt = "2026-04-04T23:30:00Z",
                endAt = "2026-04-05T00:30:00Z"
            )
        )

        val normalized = normalizeCoreTimeline(source, now, ZoneOffset.UTC)

        assertEquals(1, normalized.size)
        assertEquals("tile-1", normalized.first().tileId)
        assertEquals("2026-04-05T00:00:00Z", normalized.first().startAt)
        assertEquals("2026-04-05T00:30:00Z", normalized.first().endAt)
    }

    @Test
    fun shouldUseCoreTimeline_rejectsSyntheticBreakOnlyCoreTimeline() {
        val raw = listOf(
            CoreTimelineItem(
                id = "break-1",
                tileId = "synthetic:break:1:2:3",
                title = "Break (30min)",
                type = "break",
                status = "scheduled",
                startAt = "2026-04-05T10:00:00Z",
                endAt = "2026-04-05T10:30:00Z"
            )
        )
        val normalized = normalizeCoreTimeline(raw, Instant.parse("2026-04-05T12:00:00Z"), ZoneOffset.UTC)

        val useCore = shouldUseCoreTimeline(raw, normalized)

        assertEquals(false, useCore)
    }
}
