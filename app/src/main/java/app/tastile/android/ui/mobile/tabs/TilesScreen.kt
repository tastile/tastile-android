package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.designsystem.AppLoading
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel

@Composable
fun TilesScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel = hiltViewModel(),
) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(TileFilter.ALL) }

    if (loading && tiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppLoading()
        }
        return
    }

    val filtered = tiles.filter { filter.matches(it) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        FilterRow(current = filter, onChange = { filter = it })
        filtered.forEach { tile ->
            TileRow(
                tile = tile,
                onClick = {
                    viewModel.selectTile(tile.id)
                    overlay.show(Overlay.TileEdit(tile.id))
                },
            )
        }
    }
}

private enum class TileFilter { ALL, ACTIVE, DONE;
    fun matches(t: Tile): Boolean = when (this) {
        ALL -> true
        ACTIVE -> TileLifecycle.fromString(t.lifecycle) != TileLifecycle.DONE
        DONE -> TileLifecycle.fromString(t.lifecycle) == TileLifecycle.DONE
    }
}

@Composable
private fun FilterRow(current: TileFilter, onChange: (TileFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        TileFilter.entries.forEach { f ->
            val glyph = when (f) {
                TileFilter.ALL -> "⋯"
                TileFilter.ACTIVE -> "⏱"
                TileFilter.DONE -> "✓"
            }
            val mark = if (f == current) "[$glyph]" else glyph
            Text(
                text = mark,
                modifier = Modifier
                    .clickable { onChange(f) }
                    .padding(AppTheme.spacing.xs)
                    .semantics(mergeDescendants = true) { contentDescription = "Filter: ${f.name}" },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun TileRow(tile: Tile, onClick: () -> Unit) {
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    val glyph = when (lifecycle) {
        TileLifecycle.DONE -> "✓"
        TileLifecycle.STARTED -> "▶"
        TileLifecycle.READY -> "○"
        TileLifecycle.ARCHIVED -> "·"
    }
    val stateLabel = when (lifecycle) {
        TileLifecycle.DONE -> "done"
        TileLifecycle.STARTED -> "started"
        TileLifecycle.READY -> "ready"
        TileLifecycle.ARCHIVED -> "archived"
    }
    Text(
        text = "$glyph ${tile.title}",
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(AppTheme.spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = "$stateLabel: ${tile.title}" },
        style = MaterialTheme.typography.bodyMedium,
    )
}
