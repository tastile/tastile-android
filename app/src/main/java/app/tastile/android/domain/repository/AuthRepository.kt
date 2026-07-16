package app.tastile.android.domain.repository

import app.tastile.android.data.repository.TastileAuthState
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer read contract for auth state.
 *
 * Wraps the raw `StateFlow<TastileAuthState>` exposed by
 * [app.tastile.android.data.repository.AuthRepository] as a plain [Flow]
 * so use cases stay framework-agnostic (no `StateFlow`, no WhileSubscribed
 * policy at this layer).
 */
interface AuthRepository {
    val authState: Flow<TastileAuthState>

    fun currentUserId(): String?
}
