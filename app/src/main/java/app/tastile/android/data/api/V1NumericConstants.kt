package app.tastile.android.data.api

object V1NumericConstants {
    object TileKind {
        const val RECURRING: Byte = 0
        const val PLACEMENT: Byte = 1
        const val EXECUTION: Byte = 2
    }
    object PlanRole {
        const val EXECUTABLE: Byte = 0
        const val LABEL: Byte = 1
    }
    object PlacementSource {
        const val MANUAL: Byte = 0
        const val RECURRING: Byte = 1
        const val FLOW: Byte = 2
        const val IMPORT: Byte = 3
    }
    object ExecutionState {
        const val ACTIVE: Byte = 0
        const val PAUSED: Byte = 1
        const val FINISHED_NORMAL: Byte = 2
        const val FINISHED_VOID: Byte = 3
    }
    object ExecutionSegmentKind {
        const val ACTIVE: Byte = 0
        const val PAUSED: Byte = 1
    }
    object CommandResult {
        const val APPLIED: Byte = 0
        const val ALREADY_APPLIED: Byte = 1
        const val ACCEPTED: Byte = 2
    }
    object ApiErrorKind {
        const val VALIDATION: Short = 0
        const val FORBIDDEN: Short = 1
        const val STALE_REVISION: Short = 2
        const val IDEMPOTENCY_KEY_REUSED: Short = 3
        const val NOT_FOUND: Short = 4
        const val CONFLICT: Short = 5
        const val BLOCKED: Short = 6
        const val RETRYABLE: Short = 7
    }
    object ActorKind {
        const val USER: Byte = 0
        const val WORKER: Byte = 1
        const val IMPORT: Byte = 2
        const val SYSTEM: Byte = 3
    }
    object AggregateKind {
        const val RECURRING: Byte = 0
        const val PLACEMENT: Byte = 1
        const val EXECUTION: Byte = 2
        const val SESSION: Byte = 3
    }
    object ResolutionState {
        const val OPEN: Byte = 0
        const val CLOSED: Byte = 1
        const val BLOCKED: Byte = 2
    }
    object ChangeLayer {
        const val RECURRING: Byte = 0
        const val PLACEMENT: Byte = 1
        const val EXECUTION: Byte = 2
    }
    object ChangeKind {
        const val SET: Byte = 0
        const val CLEAR: Byte = 1
        const val PUT: Byte = 2
        const val DROP: Byte = 3
    }
    object ChangeSource {
        const val RECURRING: Byte = 0
        const val FLOW: Byte = 1
        const val USER: Byte = 2
        const val DECISION: Byte = 3
        const val EXECUTION: Byte = 4
    }
    object MergeMode {
        const val OVERRIDE: Byte = 0
        const val INTERSECT_RANGE: Byte = 1
        const val UNION_IDENTIFIED: Byte = 2
        const val ORDERED_IDENTIFIED: Byte = 3
        const val SPAN_ENDPOINT: Byte = 4
    }
}