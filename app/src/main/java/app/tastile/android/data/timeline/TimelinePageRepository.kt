package app.tastile.android.data.timeline

import kotlinx.coroutines.flow.Flow

/** Local-only read contract for one page of the timeline. */
interface TimelinePageRepository {
    fun observePage(key: TimelinePageKey): Flow<TimelinePageSnapshot>

    suspend fun purgeAccount(accountId: String)
}
