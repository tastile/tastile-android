package app.tastile.android.data.api

import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle

/**
 * Legacy projection from the nested `TileView` shape (content / visual /
 * kind Byte) onto the v0 [Tile] model. Retained for the few callers that
 * still hold a `TileView` after Macro Step 5 (the dispatcher reads
 * `TileDetailView`, not `TileView`).
 *
 * Lifecycle derivation:
 *  - kind=EXECUTION  -> STARTED (in-flight execution)
 *  - kind=PLACEMENT  -> READY
 *  - kind=RECURRING  -> READY
 *  - unknown kind    -> READY (defensive)
 */
fun TileView.toTile(userId: String): Tile {
    val lifecycle = when (kind) {
        V1NumericConstants.TileKind.EXECUTION -> TileLifecycle.STARTED.value
        V1NumericConstants.TileKind.PLACEMENT,
        V1NumericConstants.TileKind.RECURRING -> TileLifecycle.READY.value
        else -> TileLifecycle.READY.value
    }
    return Tile(
        id = id,
        userId = userId,
        localTileId = id,
        title = content.title,
        lifecycle = lifecycle
    )
}

/**
 * Primary projection for the v1 wire shape. Mirrors
 * `tastile-web/src/lib/utils/map-list-view-to-tile.ts`:
 *
 *  - `lifecycle` i16 code maps to [TileLifecycle] via
 *    [V1NumericConstants.LifecycleCode].
 *  - `temporal.active_start` -> `core.startedAt` (mirrored onto
 *    [Tile.activeStart] for now); same for `active_end` /
 *    `completedAt`.
 *  - All other list-view fields populate corresponding [Tile] fields
 *    introduced in C1 (see
 *    `tastile-android/docs/plans/2026-07-07-android-content-parity.md` §4.C1).
 *
 * Optional fields are defaulted when the backend omits them; `ignoreUnknownKeys`
 * on the Json instance absorbs fields the backend may add later.
 */
fun TileListView.toTile(userId: String): Tile {
    val lifecycleValue = when (lifecycle) {
        V1NumericConstants.LifecycleCode.STARTED -> TileLifecycle.STARTED.value
        V1NumericConstants.LifecycleCode.DONE -> TileLifecycle.DONE.value
        V1NumericConstants.LifecycleCode.CLOSED -> TileLifecycle.ARCHIVED.value
        V1NumericConstants.LifecycleCode.READY,
        null -> TileLifecycle.READY.value
        else -> TileLifecycle.READY.value
    }
    return Tile(
        id = id,
        userId = userId,
        localTileId = id,
        title = title,
        lifecycle = lifecycleValue,
        nextAction = nextAction,
        doneDefinition = doneDefinition,
        workedMinutes = workedMinutes,
        breakMinutes = breakMinutes,
        labels = labels,
        objectiveModeCode = objectiveMode,
        targetWorkMin = targetWorkMin,
        targetRestMin = targetRestMin,
        doneRuleCode = doneRule,
        resumeNote = resumeNote,
        projectedNextStartAt = projectedNextStartAt,
        activeStart = temporal?.activeStart,
        activeEnd = temporal?.activeEnd,
        releaseAt = temporal?.releaseAt,
        dueAt = temporal?.dueAt,
        fixedStart = temporal?.fixedStart,
        fixedEnd = temporal?.fixedEnd,
        planId = planId,
        isRecurring = recurrence != null,
    )
}

fun V1ListTilesResponse.toTiles(userId: String): List<Tile> =
    tiles.map { it.toTile(userId = userId) }
