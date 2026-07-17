package app.tastile.android.data.repository

import app.tastile.android.data.api.CreateWorkspaceInput
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1ListWorkspacesResponse
import app.tastile.android.data.api.Workspace
import app.tastile.android.data.api.UpdateWorkspaceInput
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * C5 — Projects side panel CRUD wiring test.
 *
 * Verifies that WorkspaceRepository maps the three required v1 endpoints
 * exactly as `tastile-web/src/lib/hooks/use-projects.ts` does:
 *
 *   - list    → V1ApiClient.listWorkspaces       (GET /v1/access/subjects?kind=1)
 *   - create  → V1ApiClient.createWorkspace      (POST /v1/access/workspaces)
 *   - delete  → V1ApiClient.deleteWorkspace      (DELETE /v1/access/subjects/{id})
 *
 * The 409 slug-conflict case must surface as WorkspaceSlugConflictException
 * so the UI can render the same inline red error text web shows.
 */
class WorkspaceRepositoryTest {

    private fun ws(
        id: String = "ws-1",
        name: String = "Inbox",
        slug: String? = "inbox",
        color: String? = "#6b7280",
        parent: String? = null,
    ): Workspace = Workspace(
        id = id,
        kind = 1,
        displayName = name,
        slug = slug,
        email = null,
        parentSubjectId = parent,
        color = color,
        ownerUserId = "user-1",
        disabledAt = null,
        createdAt = "2026-07-01T00:00:00Z",
        updatedAt = "2026-07-01T00:00:00Z",
    )

    @Test
    fun list_delegatesToV1ApiClientListWorkspaces() = runTest {
        val apiClient = mockk<V1ApiClient>()
        val ws1 = ws(id = "ws-1", name = "Inbox")
        val ws2 = ws(id = "ws-2", name = "Work", parent = "ws-1")
        coEvery { apiClient.listWorkspaces() } returns V1ListWorkspacesResponse(
            items = listOf(ws1, ws2),
            count = 2,
        )

        val repository = WorkspaceRepository(apiClient)
        val items = repository.list()

        coVerify(exactly = 1) { apiClient.listWorkspaces() }
        assertEquals(2, items.size)
        assertEquals("ws-1", items[0].id)
        assertEquals("ws-2", items[1].id)
    }

    @Test
    fun list_returnsEmptyWhenNoWorkspaces() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.listWorkspaces() } returns V1ListWorkspacesResponse(
            items = emptyList(),
            count = 0,
        )

        val repository = WorkspaceRepository(apiClient)
        val items = repository.list()

        assertTrue(items.isEmpty())
    }

    @Test
    fun create_delegatesToV1ApiClientCreateWorkspace() = runTest {
        val apiClient = mockk<V1ApiClient>()
        val created = ws(id = "ws-new", name = "Q3 Launch")
        val input = CreateWorkspaceInput(
            displayName = "Q3 Launch",
            slug = "q3-launch",
            color = "#3b82f6",
            parentSubjectId = null,
        )
        coEvery { apiClient.createWorkspace(input) } returns created

        val repository = WorkspaceRepository(apiClient)
        val result = repository.create(input)

        coVerify(exactly = 1) { apiClient.createWorkspace(input) }
        assertEquals("ws-new", result.id)
        assertEquals("Q3 Launch", result.displayName)
    }

    @Test
    fun create_passesThroughOptionalSlugColorParent() = runTest {
        val apiClient = mockk<V1ApiClient>()
        val input = CreateWorkspaceInput(
            displayName = "Sub",
            slug = "sub",
            color = "#10b981",
            parentSubjectId = "ws-parent",
        )
        coEvery { apiClient.createWorkspace(input) } returns ws(id = "ws-sub", name = "Sub", parent = "ws-parent")

        val repository = WorkspaceRepository(apiClient)
        repository.create(input)

        coVerify(exactly = 1) {
            apiClient.createWorkspace(
                match {
                    it.displayName == "Sub" &&
                        it.slug == "sub" &&
                        it.color == "#10b981" &&
                        it.parentSubjectId == "ws-parent"
                },
            )
        }
    }

    @Test
    fun delete_delegatesToV1ApiClientDeleteWorkspace() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.deleteWorkspace("ws-1") } returns Unit

        val repository = WorkspaceRepository(apiClient)
        repository.delete("ws-1")

        coVerify(exactly = 1) { apiClient.deleteWorkspace("ws-1") }
    }

    @Test
    fun update_delegatesTypedPatchToV1ApiClient() = runTest {
        val apiClient = mockk<V1ApiClient>()
        val input = UpdateWorkspaceInput("Renamed", "renamed", "#112233", "parent")
        coEvery { apiClient.updateWorkspace("ws-1", input) } returns ws(id = "ws-1", name = "Renamed", parent = "parent")

        val result = WorkspaceRepository(apiClient).update("ws-1", input)

        coVerify(exactly = 1) { apiClient.updateWorkspace("ws-1", input) }
        assertEquals("Renamed", result.displayName)
        assertEquals("parent", result.parentSubjectId)
    }

    @Test
    fun create_propagatesV1Error() = runTest {
        val apiClient = mockk<V1ApiClient>()
        val input = CreateWorkspaceInput(
            displayName = "Bad",
            slug = null,
            color = null,
            parentSubjectId = null,
        )
        coEvery { apiClient.createWorkspace(input) } throws
            IllegalStateException("HTTP 409 CONFLICT — slug conflict")

        val repository = WorkspaceRepository(apiClient)
        try {
            repository.create(input)
            fail("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(
                "expected slug conflict message, got: ${e.message}",
                e.message!!.contains("slug", ignoreCase = true),
            )
            assertTrue(
                "expected conflict marker, got: ${e.message}",
                e.message!!.contains("CONFLICT", ignoreCase = true),
            )
        }
    }

    // --- asWorkspaceSlugConflict() helper --------------------------------

    @Test
    fun asWorkspaceSlugConflict_returnsTrueForConflictMessage() {
        val ex = IllegalStateException("HTTP 409 CONFLICT — slug 'inbox' already taken")
        assertTrue(ex.asWorkspaceSlugConflict())
    }

    @Test
    fun asWorkspaceSlugConflict_returnsFalseForNonConflictMessage() {
        val ex = IllegalStateException("HTTP 500 internal server error")
        assertEquals(false, ex.asWorkspaceSlugConflict())
    }

    @Test
    fun asWorkspaceSlugConflict_walksCauseChain() {
        val root = IllegalStateException("slug conflict")
        val mid = RuntimeException("wrapper", root)
        val top = IllegalStateException("HTTP 409", mid)
        assertTrue(top.asWorkspaceSlugConflict())
    }
}
