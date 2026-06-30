package app.tastile.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed payload shapes for the v1 command endpoints wired in Macro Step 4.
 * They mirror the Rust definitions in `tastile-core/crates/v1/domain/src/command.rs`.
 *
 * For optional updates (`title`, `description`, `color`, `icon`, `external_id`),
 * `null` means "leave unchanged". This is the Kotlin convention used by Step 4;
 * clearing an existing value is not currently expressible from v0 callers and
 * would be a follow-up if needed. See v0->v1 field mapping notes in
 * `V1CommandDispatcher.kt`.
 *
 * `SetTileLifecyclePayload.bumpExtend=true` carries the `bump_extend` server flag
 * for the extend-phase flow. Other tiles (state=0..2 with `deferredUntil` /
 * `completedAt`) leave it `false`.
 */
@Serializable
data class CreateTilePayload(
    val kind: Byte,
    val title: String,
    val description: String? = null,
    val color: String? = null,
    val icon: String? = null,
    @SerialName("external_id") val externalId: String? = null,
    @SerialName("plan_role") val planRole: Byte
)

@Serializable
data class ArchiveTilePayload(
    @SerialName("tile_id") val tileId: String
)

@Serializable
data class UpdateTilePayload(
    @SerialName("tile_id") val tileId: String,
    val title: String? = null,
    val description: String? = null,
    val color: String? = null,
    val icon: String? = null,
    @SerialName("external_id") val externalId: String? = null
)

@Serializable
data class SetTileLifecyclePayload(
    @SerialName("tile_id") val tileId: String,
    val state: Short? = null,
    @SerialName("deferred_until") val deferredUntil: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("bump_extend") val bumpExtend: Boolean = false
)

@Serializable
data class AttachMemoPayload(
    @SerialName("tile_id") val tileId: String,
    val body: String
)
