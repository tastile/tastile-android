package app.tastile.android.data.repository

import app.tastile.android.data.api.ApplyFeedbackPayload
import app.tastile.android.data.api.FeedbackChangeDto
import app.tastile.android.data.api.PendingSessionView
import app.tastile.android.data.api.SessionDetailView
import app.tastile.android.data.api.V1ApiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(private val api: V1ApiClient) {
    suspend fun pending(): List<PendingSessionView> = api.listPendingSessions()
    suspend fun detail(sessionId: String): SessionDetailView = api.readSession(sessionId)

    suspend fun submitFeedback(sessionId: String, baseRevision: Long, changes: List<FeedbackChangeDto>) {
        api.submitSessionFeedback(
            sessionId,
            ApplyFeedbackPayload(sessionId = sessionId, baseRevision = baseRevision, changes = changes),
        )
    }
}
