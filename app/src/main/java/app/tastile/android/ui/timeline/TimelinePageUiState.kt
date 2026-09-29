package app.tastile.android.ui.timeline

import androidx.compose.runtime.Immutable
import app.tastile.android.data.timeline.TimelinePageKey
import app.tastile.android.data.timeline.TimelinePageSnapshot
import app.tastile.android.ui.dashboard.TimelineScale
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * Immutable state needed to render the visible timeline pages.
 *
 * Page snapshots are keyed by their canonical [TimelinePageKey]. Keeping the
 * key in the map (rather than exposing a single list) prevents an update for
 * another pager page from replacing the currently rendered snapshot.
 */
@Immutable
data class TimelinePageUiState(
    val scale: TimelineScale = TimelineScale.Day,
    val anchors: ImmutableMap<TimelineScale, LocalDate> = defaultTimelineAnchors(),
    val pages: ImmutableMap<TimelinePageKey, TimelinePageSnapshot> = persistentMapOf(),
    val isReadOnly: Boolean = false,
    /** The independently retained pinch zoom for each timeline scale. */
    val zooms: ImmutableMap<TimelineScale, Float> = defaultTimelineZooms(),
    /** Scope identity used by the active page request. */
    val scopeFingerprint: String? = null,
    /** Normalized owner selection used by the active page request. */
    val ownerIds: ImmutableList<String> = persistentListOf(),
) {
    /** Anchor of the currently selected scale. */
    val currentAnchor: LocalDate
        get() = anchors[scale] ?: LocalDate.now()

    /** Alias for callers that render the selected page directly. */
    val anchor: LocalDate
        get() = currentAnchor

    /** Zoom of the currently selected scale. */
    val currentZoom: Float
        get() = zoomFor(scale)

    /** Alias for callers that render the selected scale directly. */
    val zoom: Float
        get() = currentZoom

    /** Returns the retained zoom for [scale], or the default zoom. */
    fun zoomFor(scale: TimelineScale): Float = zooms[scale] ?: DEFAULT_TIMELINE_ZOOM

    companion object {
        /** Creates an empty state anchored to [today] for every scale. */
        fun initial(
            today: LocalDate = LocalDate.now(),
            isReadOnly: Boolean = false,
        ): TimelinePageUiState = TimelinePageUiState(
            anchors = defaultTimelineAnchors(today),
            isReadOnly = isReadOnly,
        )
    }
}

internal const val DEFAULT_TIMELINE_ZOOM: Float = 1.5f

private fun defaultTimelineAnchors(today: LocalDate = LocalDate.now()): ImmutableMap<TimelineScale, LocalDate> =
    persistentMapOf(
        TimelineScale.Day to today,
        TimelineScale.Week to today,
        TimelineScale.Month to today,
        TimelineScale.List to today,
    )

private fun defaultTimelineZooms(): ImmutableMap<TimelineScale, Float> =
    persistentMapOf(
        TimelineScale.Day to DEFAULT_TIMELINE_ZOOM,
        TimelineScale.Week to DEFAULT_TIMELINE_ZOOM,
        TimelineScale.Month to DEFAULT_TIMELINE_ZOOM,
        TimelineScale.List to DEFAULT_TIMELINE_ZOOM,
    )
