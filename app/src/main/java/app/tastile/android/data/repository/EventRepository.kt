package app.tastile.android.data.repository

import app.tastile.android.core.CoreEventEnvelopeRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
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
class EventRepository @Inject constructor() {
    suspend fun loadAll(userId: String): List<EventRow> = emptyList()

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
    ) = Unit

    suspend fun appendEmittedEvent(
        userId: String,
        envelope: CoreEventEnvelopeRecord,
        sequenceNumber: Long
    ) = Unit
}
