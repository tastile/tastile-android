package app.tastile.android.sync

import app.tastile.android.core.CoreCommandAck
import app.tastile.android.core.CoreEventEnvelopeRecord
import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.core.CoreSnapshot
import app.tastile.android.data.repository.EventRepository
import app.tastile.android.data.repository.EventRow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreEventSyncServiceTest {

    @Test
    fun syncUserEvents_normalizesRemoteRowsAndReplacesCoreEventLog() = runTest {
        val repository = mockk<EventRepository>()
        val runtime = mockk<CoreRuntimeService>()
        val importedSlot = slot<List<CoreEventEnvelopeRecord>>()
        val service = DefaultCoreEventSyncService(repository, runtime)

        coEvery { repository.loadAll("user-1") } returns listOf(
            EventRow(
                eventId = "11111111-1111-1111-1111-111111111111",
                aggregateId = "tile:33333333-3333-3333-3333-333333333333",
                eventType = "tile_created",
                payloadJson = buildJsonObject {
                    put(
                        "tile",
                        buildJsonObject {
                            put(
                                "core",
                                buildJsonObject {
                                    put("id", JsonPrimitive("33333333-3333-3333-3333-333333333333"))
                                    put("title", JsonPrimitive("Imported"))
                                    put("nextAction", JsonPrimitive("Normalize me"))
                                }
                            )
                        }
                    )
                },
                occurredAt = "2026-03-27T09:00:00Z",
                actorType = "human",
                actorId = "self",
                causedByCommandId = "not-a-uuid",
                requestId = "also-not-a-uuid",
                sequenceNumber = 1L
            )
        )
        every { runtime.replaceEventLog(capture(importedSlot)) } returns CoreCommandAck(accepted = true)
        every { runtime.applyCommand(any()) } returns CoreCommandAck(accepted = true)
        every { runtime.currentSnapshot() } returns CoreSnapshot(revision = 0)

        service.syncUserEvents("user-1")

        coVerify(exactly = 1) { repository.loadAll("user-1") }
        assertEquals(1, importedSlot.captured.size)
        val imported = importedSlot.captured.single()
        assertEquals("tile_created", imported.event["type"]?.toString()?.trim('"'))
        assertEquals("human", imported.actor.actorType)
        assertTrue(imported.actor.actorId.matches(UUID_REGEX))
        assertTrue(imported.causedByCommandId?.matches(UUID_REGEX) == true)
        assertTrue(imported.requestId?.matches(UUID_REGEX) == true)
        assertTrue(imported.event.toString().contains("next_action"))
    }

    @Test
    fun syncUserEvents_clearsCoreWhenLegacyEventsTableIsMissing() = runTest {
        val repository = mockk<EventRepository>()
        val runtime = mockk<CoreRuntimeService>()
        val importedSlot = slot<List<CoreEventEnvelopeRecord>>()
        val service = DefaultCoreEventSyncService(repository, runtime)

        coEvery { repository.loadAll("user-1") } throws NotFoundRestException(
            "Could not find the table 'public.events' in the schema cache"
        )
        every { runtime.replaceEventLog(capture(importedSlot)) } returns CoreCommandAck(accepted = true)
        every { runtime.applyCommand(any()) } returns CoreCommandAck(accepted = true)
        every { runtime.currentSnapshot() } returns CoreSnapshot(revision = 12)

        service.syncUserEvents("user-1")

        assertEquals(emptyList<CoreEventEnvelopeRecord>(), importedSlot.captured)
    }

    companion object {
        private val UUID_REGEX = Regex("^[0-9a-fA-F-]{36}$")
    }

    private class NotFoundRestException(message: String) : RuntimeException(message)
}
