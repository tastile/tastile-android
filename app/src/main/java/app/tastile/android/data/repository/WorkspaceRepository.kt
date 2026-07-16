package app.tastile.android.data.repository

import app.tastile.android.data.api.CreateWorkspaceInput
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1ApiErrorBody
import app.tastile.android.data.api.Workspace
import app.tastile.android.data.api.UpdateWorkspaceInput
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Workspace (= project) CRUD against the v1 access surface.
 *
 * Web parity: mirrors `tastile-web/src/lib/hooks/use-projects.ts` (which
 * calls `getCoreClient().call("listMyWorkspaces" | "createWorkspace" |
 * "deleteWorkspace" | "updateSubject")`) onto the typed
 * `V1ApiClient` methods.  The 409-slug-conflict case surfaces as a
 * thrown `WorkspaceSlugConflictException` so callers can render
 * the same inline red error text web shows.
 *
 * Endpoints (per `crates/v1/api/src/handlers/access.rs`):
 *   GET    /v1/access/subjects?kind=1      → list workspaces
 *   POST   /v1/access/workspaces           → create
 *   DELETE /v1/access/subjects/{id}        → delete
 */
@Singleton
class WorkspaceRepository @Inject constructor(
    private val v1ApiClient: V1ApiClient,
) {
    suspend fun list(): List<Workspace> =
        v1ApiClient.listWorkspaces().items

    suspend fun create(input: CreateWorkspaceInput): Workspace =
        v1ApiClient.createWorkspace(input)

    suspend fun delete(id: String) {
        v1ApiClient.deleteWorkspace(id)
    }

    suspend fun update(id: String, input: UpdateWorkspaceInput): Workspace =
        v1ApiClient.updateWorkspace(id, input)
}

class WorkspaceSlugConflictException(message: String) : RuntimeException(message)

internal fun Throwable.asWorkspaceSlugConflict(): Boolean {
    var ex: Throwable? = this
    while (ex != null) {
        // The V1ApiClient wraps upstream errors as V1Error / IOException;
        // backend returns 409 + ApiErrorKind.CONFLICT for slug collisions.
        val msg = ex.message.orEmpty()
        if (msg.contains("CONFLICT", ignoreCase = true)) return true
        if (msg.contains("slug", ignoreCase = true) && msg.contains("conflict", ignoreCase = true)) return true
        ex = ex.cause
    }
    return false
}

@Suppress("UnusedReceiverParameter")
internal val V1ApiErrorBody.Companion.ConflictKind
    get() = app.tastile.android.data.api.V1NumericConstants.ApiErrorKind.CONFLICT
