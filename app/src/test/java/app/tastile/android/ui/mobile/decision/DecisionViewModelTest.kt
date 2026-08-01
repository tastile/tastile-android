package app.tastile.android.ui.mobile.decision

import app.tastile.android.data.api.FeedbackChangeDto
import app.tastile.android.data.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class DecisionViewModelTest {
    @Test
    fun answer_refreshes_timeline_only_after_feedback_succeeds() = runTest {
        val sessions = mockk<SessionRepository>()
        val timeline = mockk<TimelineRefresh>()
        coEvery { sessions.submitFeedback("session-1", 7, any()) } returns Unit
        coEvery { timeline.refresh() } returns Unit
        val viewModel = DecisionViewModel(sessions, timeline)

        viewModel.answer(
            sessionId = "session-1",
            baseRevision = 7,
            changes = listOf(FeedbackChangeDto(JsonPrimitive("target"), JsonPrimitive("key"), 0, JsonPrimitive("answer"))),
        )

        coVerify(exactly = 1) { timeline.refresh() }
    }
}
