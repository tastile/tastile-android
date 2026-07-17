package app.tastile.android.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton
import app.tastile.android.core.designsystem.component.NiaLoadingWheel
import app.tastile.android.core.designsystem.component.NiaOutlinedButton
import app.tastile.android.core.designsystem.component.NiaOutlinedCard
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AppLocale

@Composable
fun ExecuteDashboardScreen(viewModel: DashboardViewModel) {
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    fun t(ja: String, en: String): String = if (locale == AppLocale.JA) ja else en
    val cards = viewModel.buildExecuteCards()

    if (loading && cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { NiaLoadingWheel(contentDesc = "Loading") }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(t("実行", "Execute"), style = MaterialTheme.typography.titleLarge) }
        items(cards, key = { it.id }) { card ->
            DashboardCardRenderer(
                card = card,
                onAction = viewModel::handleCardAction
            )
        }
    }
}

@Composable
fun TilesDashboardScreen(viewModel: DashboardViewModel) {
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    fun t(ja: String, en: String): String = if (locale == AppLocale.JA) ja else en
    val cards = viewModel.buildTileCards()

    if (loading && cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { NiaLoadingWheel(contentDesc = "Loading") }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(t("タイル", "Tiles"), style = MaterialTheme.typography.titleLarge) }
        items(cards, key = { it.id }) { card ->
            DashboardCardRenderer(
                card = card,
                onAction = viewModel::handleCardAction
            )
        }
    }
}

@Composable
private fun TileCompactCard(tile: Tile?, onStart: (String) -> Unit) {
    if (tile == null) {
        Text("No tile", style = MaterialTheme.typography.bodySmall)
        return
    }
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusCircle(
            lifecycle = lifecycle,
            onClick = if (lifecycle == TileLifecycle.READY) ({ onStart(tile.id) }) else null
        )
        Text(tile.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TileExpandableCard(
    tile: Tile,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onDefer: () -> Unit,
    onDelete: () -> Unit
) {
    val expanded = remember(tile.id) { mutableStateOf(false) }
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded.value = !expanded.value }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusCircle(
                lifecycle = lifecycle,
                onClick = if (lifecycle == TileLifecycle.READY) onStart else null
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tile.title, style = MaterialTheme.typography.titleSmall)
                Text(tile.lifecycle, style = MaterialTheme.typography.labelSmall)
            }
            Icon(
                imageVector = if (expanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand"
            )
        }

        if (expanded.value) {
            HorizontalDivider()
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tile.nextAction?.takeIf { it.isNotBlank() }?.let {
                    Text("Next: $it", style = MaterialTheme.typography.bodySmall)
                }
                tile.doneDefinition?.takeIf { it.isNotBlank() }?.let {
                    Text("Done: $it", style = MaterialTheme.typography.bodySmall)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (lifecycle == TileLifecycle.READY) {
                        NiaButton(text = { Text("Start") }, onClick = onStart)
                    }
                    if (lifecycle == TileLifecycle.STARTED) {
                        NiaButton(text = { Text("Complete") }, onClick = onComplete)
                    }
                    NiaFilledTonalButton(text = { Text("Defer") }, onClick = onDefer)
                    NiaOutlinedButton(text = { Text("Delete") }, onClick = onDelete)
                }
            }
        }
    }
}

@Composable
private fun StatusCircle(lifecycle: TileLifecycle, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (lifecycle) {
                TileLifecycle.DONE -> "✓"
                TileLifecycle.STARTED -> "▶"
                TileLifecycle.READY -> "○"
                TileLifecycle.ARCHIVED -> "·"
            },
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun DashboardCardRenderer(
    card: DashboardCardModel,
    onAction: (CardAction) -> Unit
) {
    val headerActionTileId = when (card) {
        is DashboardCardModel.TimelineCard -> card.items.firstOrNull()?.tileId
        else -> card.id
    }

    NiaOutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NiaOutlinedButton(
                text = { Text("Prompt") },
                onClick = { headerActionTileId?.let { onAction(CardAction.TriggerPrompt(it)) } }
            )
            Icon(imageVector = statusIcon(card.status), contentDescription = "Status")
            Text(card.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }

        when (card) {
            is DashboardCardModel.BaseCard -> CardPrimaryActions(card.id, card.status, onAction)
            is DashboardCardModel.TimePriorityCard -> CardPrimaryActions(card.id, card.status, onAction)
            is DashboardCardModel.TimelineCard -> {
                card.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NiaOutlinedButton(
                            text = { Text("Prompt") },
                            onClick = { onAction(CardAction.TriggerPrompt(item.tileId)) }
                        )
                        Icon(imageVector = statusIcon(item.status), contentDescription = "Status")
                        Text(item.timestampIso, style = MaterialTheme.typography.labelSmall)
                        Text("│", style = MaterialTheme.typography.labelSmall)
                        Text(item.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardPrimaryActions(
    tileId: String,
    status: CardStatus,
    onAction: (CardAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (status) {
            CardStatus.READY -> {
                NiaButton(text = { Text("Start") }, onClick = { onAction(CardAction.StartTile(tileId)) })
                NiaOutlinedButton(text = { Text("Delete") }, onClick = { onAction(CardAction.DeleteTile(tileId)) })
            }
            CardStatus.STARTED -> {
                NiaButton(text = { Text("Complete") }, onClick = { onAction(CardAction.CompleteTile(tileId)) })
                NiaFilledTonalButton(text = { Text("Defer") }, onClick = { onAction(CardAction.DeferTile(tileId)) })
            }
            CardStatus.DONE, CardStatus.ARCHIVED -> {
                NiaOutlinedButton(text = { Text("Delete") }, onClick = { onAction(CardAction.DeleteTile(tileId)) })
            }
        }
    }
}

private fun statusIcon(status: CardStatus) = when (status) {
    CardStatus.READY -> Icons.Default.RadioButtonUnchecked
    CardStatus.STARTED -> Icons.Default.PlayArrow
    CardStatus.DONE, CardStatus.ARCHIVED -> Icons.Default.Check
}
