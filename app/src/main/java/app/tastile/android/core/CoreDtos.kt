package app.tastile.android.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal val coreJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

@Serializable
data class CoreCommandRequest(
    val type: String,
    val payload: JsonObject = buildJsonObject { },
    val requestId: String? = null
) {
    fun toJson(): String = coreJson.encodeToString(this)
}

@Serializable
data class CoreActorRecord(
    val actorType: String,
    val actorId: String
)

@Serializable
data class CoreEventEnvelopeRecord(
    val eventId: String,
    val aggregateId: String,
    val occurredAt: String,
    val actor: CoreActorRecord,
    val causedByCommandId: String? = null,
    val requestId: String? = null,
    val event: JsonObject
) {
    fun toJson(): String = coreJson.encodeToString(this)
}

@Serializable
data class CoreTileSnapshot(
    val id: String,
    val title: String,
    val lifecycle: String,
    val semanticRole: String = "work",
    val fixedStartAt: String? = null,
    val fixedEndAt: String? = null
)

@Serializable
data class CoreInProgressTileSnapshot(
    val tileId: String,
    val title: String,
    val phaseKind: String,
    val startedAt: String,
    val phaseEndsAt: String? = null
)

@Serializable
data class CorePromptQueueItem(
    val promptId: String,
    val tileId: String? = null,
    val kind: String,
    val severity: String,
    val suggestedMinutes: Int? = null,
    val reasons: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val scheduledAt: String,
    val reason: String,
    val status: String
)

@Serializable
data class CoreTimelineItem(
    val id: String,
    val tileId: String? = null,
    val title: String,
    val type: String,
    val status: String,
    val startAt: String,
    val endAt: String? = null
)

@Serializable
data class CoreSnapshot(
    val revision: Long,
    val activeTileId: String? = null,
    val phaseKind: String? = null,
    val phaseStartedAt: String? = null,
    val phaseEndsAt: String? = null,
    val tiles: List<CoreTileSnapshot> = emptyList(),
    val inProgressTiles: List<CoreInProgressTileSnapshot> = emptyList(),
    val promptQueue: List<CorePromptQueueItem> = emptyList(),
    val timeline: List<CoreTimelineItem> = emptyList()
) {
    fun toJson(): String = coreJson.encodeToString(this)

    companion object {
        fun fromJson(json: String): CoreSnapshot {
            return try {
                coreJson.decodeFromString<CoreSnapshot>(json)
            } catch (error: SerializationException) {
                throw CoreBridgeError.SnapshotParseFailed(rawPayload = json, source = error)
            }
        }
    }
}

@Serializable
data class CoreCommandError(
    val code: String,
    val message: String,
    val details: JsonObject? = null
)

@Serializable
data class CoreCommandResponse(
    val accepted: Boolean,
    val requestId: String? = null,
    val commandId: String? = null,
    val eventIds: List<String> = emptyList(),
    val metadata: JsonObject? = null,
    val error: CoreCommandError? = null
) {
    fun generatedTileId(): String? = metadata?.get("tileId")?.jsonPrimitive?.contentOrNull
    fun emittedEvents(): List<CoreEventEnvelopeRecord> {
        val events = metadata?.get("emittedEvents") ?: return emptyList()
        return try {
            coreJson.decodeFromJsonElement(ListSerializer(CoreEventEnvelopeRecord.serializer()), events)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    companion object {
        fun fromJson(json: String): CoreCommandResponse {
            return try {
                coreJson.decodeFromString<CoreCommandResponse>(json)
            } catch (error: SerializationException) {
                throw CoreBridgeError.CommandResponseParseFailed(rawPayload = json, source = error)
            }
        }
    }
}

typealias CoreCommandAck = CoreCommandResponse
