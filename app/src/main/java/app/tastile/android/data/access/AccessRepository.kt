package app.tastile.android.data.access

import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.auth.AuthRepository
import app.tastile.android.data.model.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for owner-scoped subjects (Workspaces/Projects in the
 * v1 server model).
 *
 * The server exposes WORKSPACE rows through `GET /v1/access/subjects?kind=1`.
 * Personal (`kind=0`) is synthesized from the authenticated user's id and
 * pinned to the top of the cache. Disabled rows are filtered here so the UI
 * does not have to.
 */
@Singleton
class AccessRepository @Inject constructor(
    private val v1ApiClient: V1ApiClient,
    private val authRepository: AuthRepository,
) {
    private val _workspaces = MutableStateFlow<List<Workspace>>(emptyList())
    val workspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    private val refreshMutex = Mutex()

    suspend fun refresh() {
        refreshMutex.withLock {
            val response = v1ApiClient.listWorkspaces()
            val mapped = response.items
                .map { it.toDomain() }
                .filter { it.disabledAt.isNullOrBlank() }
            val personal = synthesizePersonal(authRepository.currentUserId())
            _workspaces.value = listOfNotNull(personal) + mapped
        }
    }

    fun cached(): List<Workspace> = _workspaces.value

    private fun synthesizePersonal(userId: String?): Workspace? {
        if (userId.isNullOrBlank()) return null
        return Workspace(
            id = userId,
            kind = Workspace.KIND_USER,
            displayName = "",
            slug = null,
            email = null,
            parentSubjectId = null,
            color = null,
            ownerUserId = userId,
            disabledAt = null,
            createdAt = "",
            updatedAt = "",
        )
    }
}

/**
 * Map the v1 wire DTO ([app.tastile.android.data.api.Workspace]) to
 * the Android domain model. Lives in this file because it's only
 * meaningful alongside the repository's contract.
 */
private fun app.tastile.android.data.api.Workspace.toDomain(): Workspace =
    Workspace(
        id = id,
        kind = kind.toInt(),
        displayName = displayName,
        slug = slug,
        email = email,
        parentSubjectId = parentSubjectId,
        color = color,
        ownerUserId = ownerUserId,
        disabledAt = disabledAt,
        createdAt = createdAt.orEmpty(),
        updatedAt = updatedAt.orEmpty(),
    )
