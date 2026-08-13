package app.tastile.android.data.execution

import android.content.Context
import app.tastile.android.data.api.ExecutionView
import app.tastile.android.data.api.V1ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface ExecutionIdStore {
    var executionId: String?
}

class InMemoryExecutionIdStore(override var executionId: String? = null) : ExecutionIdStore

@Singleton
class SharedPreferencesExecutionIdStore @Inject constructor(
    @ApplicationContext context: Context,
) : ExecutionIdStore {
    private val preferences = context.getSharedPreferences("execution_state", Context.MODE_PRIVATE)
    override var executionId: String?
        get() = preferences.getString(KEY, null)
        set(value) {
            preferences.edit().also { editor ->
                if (value == null) editor.remove(KEY) else editor.putString(KEY, value)
            }.apply()
        }

    private companion object { const val KEY = "execution_id" }
}

@Singleton
class ExecutionRepository @Inject constructor(
    private val api: V1ApiClient,
    private val idStore: ExecutionIdStore,
) {
    suspend fun restoreActiveExecution(): ExecutionView? {
        idStore.executionId?.let { id ->
            val persisted = runCatching { api.readExecution(id) }.getOrNull()
            if (persisted != null && persisted.state in NON_TERMINAL) return persisted
            idStore.executionId = null
        }
        val id = api.getActiveTile()?.executionId ?: return null
        return api.readExecution(id).takeIf { it.state in NON_TERMINAL }
            ?.also { idStore.executionId = it.id }
    }

    suspend fun start(placementId: String): ExecutionView {
        val id = requireNotNull(api.startExecution(placementId).aggregate?.id) {
            "start execution response did not include an execution id"
        }
        idStore.executionId = id
        return api.readExecution(id)
    }

    suspend fun pause(id: String): ExecutionView {
        api.pauseExecution(id)
        idStore.executionId = id
        return api.readExecution(id)
    }

    suspend fun resume(id: String): ExecutionView {
        api.resumeExecution(id)
        idStore.executionId = id
        return api.readExecution(id)
    }

    suspend fun finish(id: String, kind: Int = 0, note: String? = null) {
        api.finishExecution(id, kind, note)
        if (idStore.executionId == id) idStore.executionId = null
    }

    private companion object { val NON_TERMINAL = setOf(0, 1) }
}
