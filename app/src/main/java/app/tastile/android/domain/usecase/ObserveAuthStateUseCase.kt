package app.tastile.android.domain.usecase

import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the current [TastileAuthState] from the data layer.
 *
 * Use cases are the natural seam to add filtering (e.g. collapse
 * `Loading` → `Unauthenticated` for first-paint), debouncing, or
 * derivation of higher-level signals (`isReadyToSync`, etc.) without
 * threading those concerns back into every ViewModel.
 */
class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<TastileAuthState> = authRepository.getAuthStateStream
}
