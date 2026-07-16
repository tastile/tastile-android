package app.tastile.android.domain.usecase

import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.TileFilter
import app.tastile.android.domain.repository.TileRepository
import javax.inject.Inject

/**
 * Loads the visible tiles for the current view, honoring the supplied
 * [TileFilter]. One-shot, suspend-based — call sites drive their own
 * caching / flow conversion when needed.
 *
 * Demonstrative use case for R23 (domain layer): keeps the ViewModel
 * oblivious to whether tiles come from the v1 cloud read or a local
 * fallback, and gives a single seam to add cross-cutting concerns
 * (logging, retries, telemetry) in the future.
 */
class GetTilesUseCase @Inject constructor(
    private val tileRepository: TileRepository,
) {
    suspend operator fun invoke(filter: TileFilter = TileFilter.DEFAULT): List<Tile> =
        tileRepository.getTiles(filter).tiles
}
