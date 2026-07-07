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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.designsystem.AppLoading
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel

private enum class TileFilter { ALL, ACTIVE, DONE;
    fun matches(t: Tile): Boolean = when (this) {
        ALL -> true
        ACTIVE -> TileLifecycle.fromString(t.lifecycle) != TileLifecycle.DONE
        DONE -> TileLifecycle.fromString(t.lifecycle) == TileLifecycle.DONE
    }
    val label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            FilterRow(current = filter, onChange = { filter = it })
            if (filtered.isEmpty()) {
                EmptyState(filter = filter)
            } else {
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

        ExtendedFloatingActionButton(
            onClick = { overlay.show(Overlay.QuickCreate) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppTheme.spacing.md),
            text = { Text("New") },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
        )
    }
}

@Composable
private fun FilterRow(current: TileFilter, onChange: (TileFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        TileFilter.entries.forEach { f ->
            FilterChip(
                selected = f == current,
                onClick = { onChange(f) },
                label = { Text(f.label) },
                modifier = Modifier.semantics { contentDescription = "Filter: ${f.label}" },
            )
        }
    }
}

@Composable
private fun EmptyState(filter: TileFilter) {
    val msg = when (filter) {
        TileFilter.ALL -> "No tiles yet"
        TileFilter.ACTIVE -> "No active tiles"
        TileFilter.DONE -> "No done tiles"
    }
    Text(
        msg,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = AppTheme.spacing.md),
    )
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
    val stateLabel = lifecycle.name.lowercase()
    val project = tile.annotationConditions?.get("project")?.toString()
        ?.let { Regex("\"project:([^\"]+)\"").find(it)?.groupValues?.getOrNull(1) }
    val dueAt = tile.temporalConditions?.get("due_at")?.toString()?.trim('"')
        ?: tile.annotationConditions?.get("due_at")?.toString()?.trim('"')
    val isRecurring = tile.annotationConditions?.containsKey("recurrence") == true ||
                      tile.temporalConditions?.containsKey("recurrence") == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(AppTheme.spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = "$stateLabel: ${tile.title}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Text("$glyph", style = MaterialTheme.typography.bodyMedium)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tile.title, style = MaterialTheme.typography.bodyMedium)
            val meta = buildString {
                if (!project.isNullOrBlank()) append(project)
                if (!dueAt.isNullOrBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(dueAt.take(10))
                }
                if (isRecurring) {
                    if (isNotEmpty()) append(" · ")
                    append("↻")
                }
            }
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}