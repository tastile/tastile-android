package app.tastile.android.data.timeline.local

import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.core.coreJson
import kotlinx.serialization.encodeToString
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.OffsetDateTime

/**
 * Maps the canonical core timeline DTO to the local normalized read model.
 *
 * The serialized payload is retained alongside indexed UTC fields so a cache
 * round-trip does not reconstruct or reinterpret core data on the client.
 */
object TimelineCacheMapper {

    fun toEntity(
        accountId: String,
        scopeKey: String,
        item: CoreTimelineItem,
    ): TimelineItemEntity {
        return TimelineItemEntity(
            accountId = accountId,
            scopeKey = scopeKey,
            itemId = item.id,
            startEpochMs = parseEpochMs(item.startAt),
            endEpochMs = item.endAt?.let(::parseEpochMs),
            payloadJson = coreJson.encodeToString(CoreTimelineItem.serializer(), item),
            contentHash = contentHash(item),
        )
    }

    fun fromEntity(entity: TimelineItemEntity): CoreTimelineItem {
        return coreJson.decodeFromString(CoreTimelineItem.serializer(), entity.payloadJson)
    }

    fun contentHash(item: CoreTimelineItem): String {
        val payload = coreJson.encodeToString(CoreTimelineItem.serializer(), item)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun parseEpochMs(value: String): Long =
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
}

fun CoreTimelineItem.toTimelineCacheEntity(
    accountId: String,
    scopeKey: String,
): TimelineItemEntity = TimelineCacheMapper.toEntity(accountId, scopeKey, this)

fun TimelineItemEntity.toCoreTimelineItem(): CoreTimelineItem = TimelineCacheMapper.fromEntity(this)
