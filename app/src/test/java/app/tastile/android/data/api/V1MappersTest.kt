package app.tastile.android.data.api

import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V1MappersTest {

    // --- Legacy TileView mappings (kept for backward compat) ----------

    private fun legacyV1Tile(
        id: String = "tile-1",
        kind: Byte = V1NumericConstants.TileKind.RECURRING,
        ownerId: String = "owner-1",
        externalId: String? = null,
        title: String = "Hello",
        revision: Long = 1L
    ): TileView = TileView(
        id = id,
        kind = kind,
        ownerId = ownerId,
        externalId = externalId,
        content = TileContentView(title = title, note = null),
        visual = TileVisualView(color = null, icon = null),
        revision = revision
    )

    @Test
    fun execution_kind_maps_to_started_lifecycle() {
        val view = legacyV1Tile(kind = V1NumericConstants.TileKind.EXECUTION, title = "In flight")

        val tile = view.toTile(userId = "owner-1")

        assertEquals(TileLifecycle.STARTED.value, tile.lifecycle)
    }

    @Test
    fun placement_kind_maps_to_ready_lifecycle() {
        val view = legacyV1Tile(kind = V1NumericConstants.TileKind.PLACEMENT, title = "Scheduled")

        val tile = view.toTile(userId = "owner-1")

        assertEquals(TileLifecycle.READY.value, tile.lifecycle)
    }

    @Test
    fun recurring_kind_maps_to_ready_lifecycle() {
        val view = legacyV1Tile(kind = V1NumericConstants.TileKind.RECURRING, title = "Recurring")

        val tile = view.toTile(userId = "owner-1")

        assertEquals(TileLifecycle.READY.value, tile.lifecycle)
    }

    @Test
    fun unknown_kind_defaults_to_ready_lifecycle() {
        val view = legacyV1Tile(kind = 99.toByte(), title = "Future kind")

        val tile = view.toTile(userId = "owner-1")

        assertEquals(TileLifecycle.READY.value, tile.lifecycle)
    }

    @Test
    fun legacy_mapper_preserves_id_and_title() {
        val view = legacyV1Tile(id = "tile-xyz", title = "Buy milk")

        val tile = view.toTile(userId = "owner-1")

        assertEquals("tile-xyz", tile.id)
        assertEquals("Buy milk", tile.title)
    }

    @Test
    fun legacy_mapper_copies_userId_from_argument() {
        val view = legacyV1Tile(ownerId = "owner-from-v1")

        val tile = view.toTile(userId = "owner-from-args")

        assertEquals("owner-from-args", tile.userId)
    }

    @Test
    fun legacy_mapper_populates_localTileId_from_id() {
        val view = legacyV1Tile(id = "tile-abc")

        val tile = view.toTile(userId = "owner-1")

        assertEquals("tile-abc", tile.localTileId)
    }

    // --- New TileListView mappings (C1 forward-path) ------------------

    private fun listView(
        id: String = "tile-lv-1",
        title: String = "Hello",
        lifecycleCode: Int? = V1NumericConstants.LifecycleCode.READY,
        nextAction: String? = null,
        doneDefinition: String? = null,
        workedMinutes: Long? = null,
        breakMinutes: Long? = null,
        labels: List<String> = emptyList(),
        objectiveMode: Int? = null,
        targetWorkMin: Long? = null,
        targetRestMin: Long? = null,
        doneRule: Int? = null,
        resumeNote: String? = null,
        planId: String? = null,
        temporal: TileTemporalView? = null,
        recurrence: TileRecurrenceView? = null
    ): TileListView = TileListView(
        id = id,
        planId = planId,
        title = title,
        lifecycle = lifecycleCode,
        nextAction = nextAction,
        doneDefinition = doneDefinition,
        workedMinutes = workedMinutes,
        breakMinutes = breakMinutes,
        labels = labels,
        objectiveMode = objectiveMode,
        targetWorkMin = targetWorkMin,
        targetRestMin = targetRestMin,
        doneRule = doneRule,
        resumeNote = resumeNote,
        projectedNextStartAt = null,
        temporal = temporal,
        recurrence = recurrence,
    )

    @Test
    fun lifecycle_code_ready_maps_to_ready() {
        val tile = listView(lifecycleCode = V1NumericConstants.LifecycleCode.READY)
            .toTile(userId = "owner-1")
        assertEquals(TileLifecycle.READY.value, tile.lifecycle)
    }

    @Test
    fun lifecycle_code_started_maps_to_started() {
        val tile = listView(lifecycleCode = V1NumericConstants.LifecycleCode.STARTED)
            .toTile(userId = "owner-1")
        assertEquals(TileLifecycle.STARTED.value, tile.lifecycle)
    }

    @Test
    fun lifecycle_code_done_maps_to_done() {
        val tile = listView(lifecycleCode = V1NumericConstants.LifecycleCode.DONE)
            .toTile(userId = "owner-1")
        assertEquals(TileLifecycle.DONE.value, tile.lifecycle)
    }

    @Test
    fun lifecycle_code_closed_maps_to_archived() {
        val tile = listView(lifecycleCode = V1NumericConstants.LifecycleCode.CLOSED)
            .toTile(userId = "owner-1")
        assertEquals(TileLifecycle.ARCHIVED.value, tile.lifecycle)
    }

    @Test
    fun lifecycle_code_null_maps_to_ready() {
        val tile = listView(lifecycleCode = null).toTile(userId = "owner-1")
        assertEquals(TileLifecycle.READY.value, tile.lifecycle)
    }

    @Test
    fun unknown_lifecycle_code_defaults_to_ready() {
        val tile = listView(lifecycleCode = 99).toTile(userId = "owner-1")
        assertEquals(TileLifecycle.READY.value, tile.lifecycle)
    }

    @Test
    fun mapper_carries_text_fields() {
        val tile = listView(
            title = "Buy milk",
            nextAction = "Open the app",
            doneDefinition = "Milk in the fridge",
            resumeNote = "Last session ended early",
            planId = "plan-1",
        ).toTile(userId = "owner-1")
        assertEquals("Buy milk", tile.title)
        assertEquals("Open the app", tile.nextAction)
        assertEquals("Milk in the fridge", tile.doneDefinition)
        assertEquals("Last session ended early", tile.resumeNote)
        assertEquals("plan-1", tile.planId)
    }

    @Test
    fun mapper_carries_minutes_and_labels() {
        val tile = listView(
            workedMinutes = 35L,
            breakMinutes = 5L,
            labels = listOf("project:tastile", "urgent"),
            targetWorkMin = 50L,
            targetRestMin = 10L,
            objectiveMode = V1NumericConstants.ObjectiveMode.RECURRING,
            doneRule = V1NumericConstants.DoneRule.TIME_REACHED,
        ).toTile(userId = "owner-1")
        assertEquals(35L, tile.workedMinutes)
        assertEquals(5L, tile.breakMinutes)
        assertEquals(listOf("project:tastile", "urgent"), tile.labels)
        assertEquals(50L, tile.targetWorkMin)
        assertEquals(10L, tile.targetRestMin)
        assertEquals(V1NumericConstants.ObjectiveMode.RECURRING, tile.objectiveModeCode)
        assertEquals(V1NumericConstants.DoneRule.TIME_REACHED, tile.doneRuleCode)
    }

    @Test
    fun mapper_carries_temporal_dates() {
        val tile = listView(
            temporal = TileTemporalView(
                releaseAt = "2026-07-01T00:00:00Z",
                dueAt = "2026-07-08T23:59:59Z",
                fixedStart = "2026-07-02T09:00:00Z",
                fixedEnd = "2026-07-02T10:00:00Z",
                activeStart = "2026-07-02T09:05:00Z",
                activeEnd = "2026-07-02T09:55:00Z",
            )
        ).toTile(userId = "owner-1")
        assertEquals("2026-07-01T00:00:00Z", tile.releaseAt)
        assertEquals("2026-07-08T23:59:59Z", tile.dueAt)
        assertEquals("2026-07-02T09:00:00Z", tile.fixedStart)
        assertEquals("2026-07-02T10:00:00Z", tile.fixedEnd)
        assertEquals("2026-07-02T09:05:00Z", tile.activeStart)
        assertEquals("2026-07-02T09:55:00Z", tile.activeEnd)
    }

    @Test
    fun mapper_sets_isRecurring_when_recurrence_present() {
        val tileWith = listView(
            recurrence = TileRecurrenceView(
                stepMin = 30L,
                windowStartMin = 0L,
                windowEndMin = 240L,
                expression = "* * *",
            )
        ).toTile(userId = "owner-1")
        assertTrue(tileWith.isRecurring)

        val tileWithout = listView(recurrence = null).toTile(userId = "owner-1")
        assertEquals(false, tileWithout.isRecurring)
    }

    @Test
    fun mapper_propagates_userId_and_localTileId() {
        val tile = listView(id = "lv-99").toTile(userId = "owner-99")
        assertEquals("owner-99", tile.userId)
        assertEquals("lv-99", tile.localTileId)
        assertEquals("lv-99", tile.id)
    }

    @Test
    fun mapper_handles_null_temporal_cleanly() {
        val tile = listView(temporal = null).toTile(userId = "owner-1")
        assertNull(tile.releaseAt)
        assertNull(tile.dueAt)
        assertNull(tile.fixedStart)
        assertNull(tile.fixedEnd)
        assertNull(tile.activeStart)
        assertNull(tile.activeEnd)
    }

    @Test
    fun list_response_carries_all_list_view_tiles() {
        val response = V1ListTilesResponse(
            tiles = listOf(
                listView(id = "t-1", title = "Exec", lifecycleCode = V1NumericConstants.LifecycleCode.STARTED),
                listView(id = "t-2", title = "Place", lifecycleCode = V1NumericConstants.LifecycleCode.READY),
                listView(id = "t-3", title = "Recur", lifecycleCode = V1NumericConstants.LifecycleCode.DONE),
            ),
            nextActionableTileId = "t-1",
            nextActionableStartAt = "2026-07-07T09:00:00Z",
        )

        val tiles: List<Tile> = response.toTiles(userId = "owner-1")

        assertEquals(3, tiles.size)
        assertEquals("Exec", tiles[0].title)
        assertEquals(TileLifecycle.STARTED.value, tiles[0].lifecycle)
        assertEquals(TileLifecycle.READY.value, tiles[1].lifecycle)
        assertEquals(TileLifecycle.DONE.value, tiles[2].lifecycle)
        // next_* optional fields are surfaced at the response level for C3+;
        // assert they at least exist on the wire class.
        assertNotNull(response.nextActionableTileId)
        assertNotNull(response.nextActionableStartAt)
    }
}
