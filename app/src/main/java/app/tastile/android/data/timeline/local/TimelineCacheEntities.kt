package app.tastile.android.data.timeline.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "timeline_items",
    primaryKeys = ["accountId", "scopeKey", "itemId"],
    indices = [
        Index(value = ["startEpochMs"]),
        Index(value = ["endEpochMs"]),
    ],
)
data class TimelineItemEntity(
    val accountId: String,
    val scopeKey: String,
    val itemId: String,
    val startEpochMs: Long,
    val endEpochMs: Long?,
    val payloadJson: String,
    val contentHash: String,
)

@Entity(
    tableName = "timeline_day_memberships",
    primaryKeys = ["accountId", "scopeKey", "zoneId", "localDate", "itemId"],
)
data class TimelineDayMembershipEntity(
    val accountId: String,
    val scopeKey: String,
    val zoneId: String,
    val localDate: String,
    val itemId: String,
)

@Entity(
    tableName = "timeline_coverage",
    primaryKeys = ["accountId", "scopeKey", "zoneId", "localDate"],
)
data class TimelineCoverageEntity(
    val accountId: String,
    val scopeKey: String,
    val zoneId: String,
    val localDate: String,
    val fetchedAtEpochMs: Long,
    val contractVersion: Int,
    val lastFailureKind: String?,
    val lastAccessedAtEpochMs: Long,
    val refreshGeneration: Long,
)
