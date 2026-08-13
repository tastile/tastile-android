package app.tastile.android.data.execution

import app.tastile.android.data.api.ExecutionView
import app.tastile.android.data.api.V1ApiClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ExecutionRepositoryTest {
    @Test
    fun restore_revalidates_persisted_paused_execution_after_process_restart() = runTest {
        val api = mockk<V1ApiClient>()
        val store = InMemoryExecutionIdStore("execution-1")
        coEvery { api.readExecution("execution-1") } returns ExecutionView(
            id = "execution-1",
            tileId = "tile-1",
            state = 1,
            placementId = "placement-1",
        )
        val repository = ExecutionRepository(api, store)

        assertEquals(1, repository.restoreActiveExecution()?.state)
        assertEquals("execution-1", store.executionId)
    }
}
