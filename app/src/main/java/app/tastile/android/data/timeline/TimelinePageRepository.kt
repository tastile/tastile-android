package app.tastile.android.data.timeline

import app.tastile.android.core.CoreTimelineItem
import kotlinx.coroutines.flow.Flow

/** Local-only read contract for one page of the timeline. */
interface TimelinePageRepository {
    fun observePage(key: TimelinePageKey): Flow<TimelinePageSnapshot>

    /**
     * Observes one local projection range from the Room read model. Unlike
     * page navigation, a range projection must remain one DAO Flow even when
     * it spans many custom-range dates.
     */
    fun observeProjection(request: TimelineProjectionRequest): Flow<List<CoreTimelineItem>>

    suspend fun purgeAccount(accountId: String)
}
