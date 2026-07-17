package app.tastile.android.ui.mobile.panels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.api.CreateWorkspaceInput
import app.tastile.android.data.api.Workspace
import app.tastile.android.data.api.UpdateWorkspaceInput
import app.tastile.android.data.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the [ProjectsSectionContent] panel. Mirrors the
 * `loading / error / workspaces / refresh` quartet from
 * `tastile-web/src/lib/hooks/use-projects.ts`.
 *
 * Owns three orthogonal flows:
 *   * [state]         — the read-model surfaced to the panel.
 *   * [creating]      — whether the inline +New form is open.
 *   * [selectedOwnerId] — which workspace (if any) is the active owner
 *                        filter on the dashboard; mirrors `?owner=<id>`
 *                        URL state in web.
 *
 * The `selectedOwnerId` is held here so [ProjectsSectionContent] is
 * the single source of truth for the
 * `selectedWorkspaceId` flow into tiles filtering, exactly as web
 * derives the URL `?owner=` from the same component.
 */
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
) : ViewModel() {

    data class State(
        val workspaces: List<Workspace> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val createBusy: Boolean = false,
        val createError: String? = null,
        val updateBusy: Boolean = false,
        val updateError: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    private val _selectedOwnerId = MutableStateFlow<String?>(null)
    val selectedOwnerId: StateFlow<String?> = _selectedOwnerId.asStateFlow()

    private val _deleteCandidate = MutableStateFlow<Workspace?>(null)
    val deleteCandidate: StateFlow<Workspace?> = _deleteCandidate.asStateFlow()

    private val _editCandidate = MutableStateFlow<Workspace?>(null)
    val editCandidate: StateFlow<Workspace?> = _editCandidate.asStateFlow()

    fun requestDelete(workspace: Workspace) { _deleteCandidate.value = workspace }
    fun cancelDelete() { _deleteCandidate.value = null }
    fun requestEdit(workspace: Workspace) { _editCandidate.value = workspace }
    fun cancelEdit() { _editCandidate.value = null }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { workspaceRepository.list() }
                .onSuccess { ws ->
                    _state.update { it.copy(workspaces = ws, loading = false, error = null) }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(loading = false, error = err.message ?: "load failed")
                    }
                }
        }
    }

    fun openCreateForm() {
        _creating.value = true
    }

    fun closeCreateForm() {
        _creating.value = false
        _state.update { it.copy(createError = null) }
    }

    /**
     * @param parentSubjectId mobile-port companion to web's parent selector.
     *        Defaults to null (top level) since Android doesn't currently
     *        surface the parent dropdown UI (web-only control).
     */
    fun create(
        name: String,
        slug: String?,
        color: String,
        parentSubjectId: String? = null,
    ) {
        if (name.isBlank()) {
            _state.update { it.copy(createError = "name required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(createBusy = true, createError = null) }
            runCatching {
                workspaceRepository.create(
                    CreateWorkspaceInput(
                        displayName = name.trim(),
                        slug = slug?.trim()?.takeIf { it.isNotEmpty() },
                        color = color,
                        parentSubjectId = parentSubjectId,
                    ),
                )
            }.onSuccess { ws ->
                _state.update {
                    it.copy(createBusy = false, createError = null, workspaces = it.workspaces + ws)
                }
                _creating.value = false
                _selectedOwnerId.value = ws.id
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        createBusy = false,
                        createError = err.message ?: "create failed",
                    )
                }
            }
        }
    }

    fun selectOwner(id: String?) {
        _selectedOwnerId.value = id
    }

    fun clearOwnerFilter() {
        _selectedOwnerId.value = null
    }

    fun deleteWorkspace(id: String) {
        viewModelScope.launch {
            runCatching { workspaceRepository.delete(id) }
                .onSuccess {
                    _state.update { s ->
                        s.copy(workspaces = s.workspaces.filterNot { it.id == id })
                    }
                    if (_selectedOwnerId.value == id) _selectedOwnerId.value = null
                    _deleteCandidate.value = null
                }
                .onFailure { err ->
                    _state.update { it.copy(error = err.message ?: "delete failed") }
                }
        }
    }

    fun update(
        id: String,
        name: String,
        slug: String?,
        color: String?,
        parentSubjectId: String?,
    ) {
        if (name.isBlank()) {
            _state.update { it.copy(updateError = "name required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(updateBusy = true, updateError = null) }
            runCatching {
                workspaceRepository.update(
                    id,
                    UpdateWorkspaceInput(
                        displayName = name.trim(),
                        slug = slug?.trim()?.takeIf { it.isNotEmpty() },
                        color = color?.trim()?.takeIf { it.isNotEmpty() },
                        parentSubjectId = parentSubjectId,
                    ),
                )
            }.onSuccess { updated ->
                _state.update { s ->
                    s.copy(
                        updateBusy = false,
                        updateError = null,
                        workspaces = s.workspaces.map { if (it.id == updated.id) updated else it },
                    )
                }
            }.onFailure { err ->
                _state.update { it.copy(updateBusy = false, updateError = err.message ?: "update failed") }
            }
        }
    }
}
