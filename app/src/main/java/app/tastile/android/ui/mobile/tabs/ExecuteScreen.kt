package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.isStarted
import app.tastile.android.ui.designsystem.AppLoading
import app.tastile.android.ui.designsystem.AppTheme

@Composable
fun ExecuteScreen(viewModel: DashboardViewModel) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    if (loading && tiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppLoading()
        }
        return
    }

    val active = tiles.firstOrNull { it.isStarted() }
    val displayTiles = active?.let { a -> tiles.filterNot { it.id == a.id } } ?: tiles

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        active?.let { ActiveTileRow(tile = it) }
        Text("Today's tiles", style = MaterialTheme.typography.labelSmall)
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            modifier = Modifier.fillMaxWidth(),
        ) {
            displayTiles.forEach { tile ->
                TileRow(tile = tile)
            }
        }
    }
}

@Composable
private fun ActiveTileRow(tile: Tile) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
    ) {
        Text("▶ ${tile.title}", style = MaterialTheme.typography.titleMedium)
        tile.nextAction?.takeIf { it.isNotBlank() }?.let { nextAction ->
            Text("Next: $nextAction", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TileRow(tile: Tile) {
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    val glyph = when (lifecycle) {
        TileLifecycle.DONE -> "✓"
        TileLifecycle.STARTED -> "▶"
        TileLifecycle.READY -> "○"
        TileLifecycle.ARCHIVED -> "·"
    }
    val stateLabel = remember(lifecycle) {
        when (lifecycle) {
            TileLifecycle.DONE -> "done"
            TileLifecycle.STARTED -> "started"
            TileLifecycle.READY -> "ready"
            TileLifecycle.ARCHIVED -> "archived"
        }
    }
    Text(
        text = "$glyph ${tile.title}",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$stateLabel: ${tile.title}"
        },
    )
}
