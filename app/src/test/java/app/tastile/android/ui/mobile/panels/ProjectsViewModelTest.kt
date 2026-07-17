package app.tastile.android.ui.mobile.panels

import app.tastile.android.data.api.UpdateWorkspaceInput
import app.tastile.android.data.api.Workspace
import app.tastile.android.data.repository.WorkspaceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun workspace(id: String, name: String, parent: String? = null) = Workspace(
        id = id, displayName = name, parentSubjectId = parent, color = "#6b7280",
    )

    @Test
    fun update_sendsAllWebEditFieldsAndReplacesWorkspace() = runTest(dispatcher) {
        val repository = mockk<WorkspaceRepository>()
        val existing = workspace("one", "Before")
        val updated = workspace("one", "After", "parent")
        coEvery { repository.list() } returns listOf(existing)
        coEvery { repository.update("one", any()) } returns updated
        val viewModel = ProjectsViewModel(repository)
        advanceUntilIdle()

        viewModel.update("one", "After", "after", "#abcdef", "parent")
        advanceUntilIdle()

        coVerify { repository.update("one", UpdateWorkspaceInput("After", "after", "#abcdef", "parent")) }
        assertEquals("After", viewModel.state.value.workspaces.single().displayName)
        assertEquals("parent", viewModel.state.value.workspaces.single().parentSubjectId)
    }

    @Test
    fun selectedOwner_canSelectAndClearAllProjects() = runTest(dispatcher) {
        val repository = mockk<WorkspaceRepository>()
        coEvery { repository.list() } returns emptyList()
        val viewModel = ProjectsViewModel(repository)
        advanceUntilIdle()

        viewModel.selectOwner("project-1")
        assertEquals("project-1", viewModel.selectedOwnerId.value)
        viewModel.clearOwnerFilter()
        assertNull(viewModel.selectedOwnerId.value)
    }
}
