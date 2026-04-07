package app.tastile.android.sync

import app.tastile.android.core.CoreActorRecord
import app.tastile.android.core.CoreEventEnvelopeRecord
import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.data.repository.EventRepository
import app.tastile.android.data.repository.EventRow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface CoreEventSyncService {
    suspend fun syncUserEvents(userId: String)
}

@Singleton
class DefaultCoreEventSyncService @Inject constructor(
    private val eventRepository: EventRepository,
    private val coreRuntimeService: CoreRuntimeService
) : CoreEventSyncService {
    override suspend fun syncUserEvents(userId: String) {
        val rows = try {
            eventRepository.loadAll(userId)
        } catch (error: Exception) {
            if (isLegacyEventsTableMissing(error)) {
                coreRuntimeService.replaceEventLog(emptyList())
                return
            }
            throw error
        }
        val envelopes = rows.map(::toCoreEnvelope)
        coreRuntimeService.replaceEventLog(envelopes)
    }

    private fun isLegacyEventsTableMissing(error: Exception): Boolean {
        if (!hasHttpStatusCode(error, 404) && error.javaClass.simpleName != "NotFoundRestException") return false
        val message = error.message.orEmpty().lowercase()
        return message.contains("public.events") || message.contains("table 'events'") || message.contains("relation \"events\"")
    }

    private fun hasHttpStatusCode(error: Throwable, expected: Int): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (extractStatusCode(current) == expected) return true
            current = current.cause
        }
        return false
    }

    private fun extractStatusCode(error: Throwable): Int? {
        val methodNames = listOf("getStatus", "getStatusCode", "getCode")
        for (methodName in methodNames) {
            val value = runCatching {
                error.javaClass.methods.firstOrNull { it.name == methodName && it.parameterCount == 0 }?.invoke(error)
            }.getOrNull()
            val parsed = parseStatusCode(value)
            if (parsed != null) return parsed
        }

        val fieldNames = listOf("status", "statusCode", "code")
        for (fieldName in fieldNames) {
            val value = runCatching {
                error.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }.get(error)
            }.getOrNull()
            val parsed = parseStatusCode(value)
            if (parsed != null) return parsed
        }
        return null
    }

    private fun parseStatusCode(value: Any?): Int? {
        return when (value) {
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun toCoreEnvelope(row: EventRow): CoreEventEnvelopeRecord {
        return CoreEventEnvelopeRecord(
            eventId = normalizeUuid(row.eventId),
            aggregateId = row.aggregateId,
            occurredAt = row.occurredAt,
            actor = CoreActorRecord(
                actorType = normalizeActorType(row.actorType),
                actorId = normalizeUuid(row.actorId)
            ),
            causedByCommandId = row.causedByCommandId?.let(::normalizeUuid),
            requestId = row.requestId?.let(::normalizeUuid),
            event = normalizeEventJson(row)
        )
    }

    private fun normalizeEventJson(row: EventRow): JsonObject {
        val payload = normalizeKeys(row.payloadJson ?: row.eventPayload ?: buildJsonObject { })
        return buildJsonObject {
            put("type", JsonPrimitive(row.eventType))
            payload.jsonObject.forEach { (key, value) ->
                if (key != "type") {
                    put(key, value)
                }
            }
        }
    }

    private fun normalizeKeys(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> buildJsonObject {
                element.forEach { (key, value) ->
                    put(normalizeKey(key), normalizeKeys(value))
                }
            }
            is JsonArray -> JsonArray(element.map(::normalizeKeys))
            else -> element
        }
    }

    private fun normalizeKey(key: String): String {
        val normalized = StringBuilder(key.length + 4)
        key.forEachIndexed { index, ch ->
            if (ch.isUpperCase()) {
                if (index > 0) normalized.append('_')
                normalized.append(ch.lowercaseChar())
            } else {
                normalized.append(ch)
            }
        }
        return normalized.toString()
    }

    private fun normalizeActorType(raw: String): String {
        return raw.lowercase().replace(' ', '_')
    }

    private fun normalizeUuid(raw: String): String {
        return runCatching { UUID.fromString(raw).toString() }
            .getOrElse { UUID.nameUUIDFromBytes(raw.toByteArray(StandardCharsets.UTF_8)).toString() }
    }
}
