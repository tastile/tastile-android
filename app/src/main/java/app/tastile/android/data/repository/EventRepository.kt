package app.tastile.android.data.repository

import app.tastile.android.core.CoreEventEnvelopeRecord
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class EventRow(
    @SerialName("event_id") val eventId: String,
    @SerialName("aggregate_id") val aggregateId: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("event_payload") val eventPayload: JsonObject? = null,
    @SerialName("payload_json") val payloadJson: JsonObject? = null,
    @SerialName("occurred_at") val occurredAt: String,
    @SerialName("actor_type") val actorType: String,
    @SerialName("actor_id") val actorId: String,
    @SerialName("caused_by_command_id") val causedByCommandId: String? = null,
    @SerialName("request_id") val requestId: String? = null,
    @SerialName("sequence_number") val sequenceNumber: Long
)

@Singleton
class EventRepository @Inject constructor(
    private val client: SupabaseClient
) {
    companion object {
        private const val TABLE_EVENTS = "events"
    }

    suspend fun loadAll(userId: String): List<EventRow> {
        return client.from(TABLE_EVENTS)
            .select {
                filter { eq("user_id", userId) }
                order("occurred_at", Order.ASCENDING)
            }
            .decodeList<EventRow>()
    }

    suspend fun append(
        userId: String,
        aggregateId: String,
        eventType: String,
        payload: JsonObject,
        occurredAtIso: String,
        actorType: String = "human",
        actorId: String = "self",
        causedByCommandId: String? = null,
        requestId: String? = null
    ) {
        client.from(TABLE_EVENTS).insert(
            mapOf(
                "user_id" to userId,
                "event_id" to UUID.randomUUID().toString(),
                "aggregate_id" to aggregateId,
                "event_type" to eventType,
                "event_payload" to payload,
                "payload_json" to payload,
                "occurred_at" to occurredAtIso,
                "actor_type" to actorType,
                "actor_id" to actorId,
                "caused_by_command_id" to (causedByCommandId ?: UUID.randomUUID().toString()),
                "request_id" to requestId,
                "sequence_number" to System.currentTimeMillis()
            )
        )
    }

    suspend fun appendEmittedEvent(
        userId: String,
        envelope: CoreEventEnvelopeRecord,
        sequenceNumber: Long
    ) {
        val eventType = envelope.event["type"]?.jsonPrimitive?.content ?: return
        client.from(TABLE_EVENTS).insert(
            mapOf(
                "user_id" to userId,
                "event_id" to envelope.eventId,
                "aggregate_id" to envelope.aggregateId,
                "event_type" to eventType,
                "event_payload" to envelope.event,
                "payload_json" to envelope.event,
                "occurred_at" to envelope.occurredAt,
                "actor_type" to envelope.actor.actorType,
                "actor_id" to envelope.actor.actorId,
                "caused_by_command_id" to envelope.causedByCommandId,
                "request_id" to envelope.requestId,
                "sequence_number" to sequenceNumber
            )
        )
    }
}
