package app.tastile.android.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaLoadingWheel
import app.tastile.android.core.designsystem.component.NiaOutlinedButton
import app.tastile.android.core.designsystem.component.TastileCardActions
import app.tastile.android.core.designsystem.component.TastileCardActionRow
import app.tastile.android.core.designsystem.component.TastileCompactTileRow
import app.tastile.android.core.designsystem.component.TastileDashboardCardShell
import app.tastile.android.core.designsystem.component.TastileTileCard
import app.tastile.android.data.model.TileLifecycle

@Composable
fun ExecuteDashboardScreen(viewModel: DashboardViewModel) {
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val cards = viewModel.buildExecuteCards()
    val loadingCd = stringResource(R.string.dashboard_loading)

    if (loading && cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NiaLoadingWheel(contentDesc = loadingCd)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.dashboard_execute_title), style = MaterialTheme.typography.titleLarge) }
        items(cards, key = { it.id }) { card ->
            var expanded by remember(card.id) { mutableStateOf(false) }
            DashboardCardRenderer(
                card = card,
                expanded = expanded,
                onToggleExpanded = { expanded = !expanded },
                onAction = viewModel::handleCardAction
            )
        }
    }
}

@Composable
fun TilesDashboardScreen(viewModel: DashboardViewModel) {
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val cards = viewModel.buildTileCards()
    val loadingCd = stringResource(R.string.dashboard_loading)

    if (loading && cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NiaLoadingWheel(contentDesc = loadingCd)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.dashboard_tiles_work_title), style = MaterialTheme.typography.titleLarge) }
        items(cards, key = { it.id }) { card ->
            var expanded by remember(card.id) { mutableStateOf(false) }
            DashboardCardRenderer(
                card = card,
                expanded = expanded,
                onToggleExpanded = { expanded = !expanded },
                onAction = viewModel::handleCardAction
            )
        }
    }
}

@Composable
private fun DashboardCardRenderer(
    card: DashboardCardModel,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAction: (CardAction) -> Unit,
) {
    val headerActionTileId = when (card) {
        is DashboardCardModel.TimelineCard -> card.items.firstOrNull()?.tileId
        else -> card.id
    }
    val headerTitle = when (card) {
        is DashboardCardModel.TimelineCard -> stringResource(card.titleRes)
        else -> card.title
    }
    // TileLifecycle.fromString is case-sensitive and expects title case
    // ("Ready"/"Started"/"Done"/"Archived"). CardStatus.name returns uppercase
    // enum names, so map via CardStatus -> TileLifecycle instead.
    val lifecycle = when (card) {
        is DashboardCardModel.BaseCard, is DashboardCardModel.TimePriorityCard -> when (card.status) {
            CardStatus.READY -> TileLifecycle.READY
            CardStatus.STARTED -> TileLifecycle.STARTED
            CardStatus.DONE -> TileLifecycle.DONE
            CardStatus.ARCHIVED -> TileLifecycle.ARCHIVED
        }
        is DashboardCardModel.TimelineCard -> TileLifecycle.READY
    }

    TastileDashboardCardShell(
        header = {
            NiaOutlinedButton(
                text = { Text(stringResource(R.string.dashboard_prompt_button)) },
                onClick = { headerActionTileId?.let { onAction(CardAction.TriggerPrompt(it)) } },
            )
            Icon(
                imageVector = statusIcon(card.status),
                contentDescription = stringResource(R.string.dashboard_status_cd),
            )
            Text(headerTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        },
    ) {
        when (card) {
            is DashboardCardModel.BaseCard,
            is DashboardCardModel.TimePriorityCard,
            -> {
                TastileTileCard(
                    title = headerTitle,
                    lifecycle = lifecycle,
                    expanded = expanded,
                    onToggleExpanded = onToggleExpanded,
                    expandToggleContentDescription = stringResource(R.string.dashboard_expand_cd),
                    actions = {
                        TastileCardActionRow(
                            actions = when (card.status) {
                                CardStatus.READY -> TastileCardActions.Ready
                                CardStatus.STARTED -> TastileCardActions.Started
                                CardStatus.DONE, CardStatus.ARCHIVED -> TastileCardActions.DoneOrArchived
                            },
                            onStart = { onAction(CardAction.StartTile(card.id)) },
                            onComplete = { onAction(CardAction.CompleteTile(card.id)) },
                            onDefer = { onAction(CardAction.DeferTile(card.id)) },
                            onDelete = { onAction(CardAction.DeleteTile(card.id)) },
                            startLabel = { Text(stringResource(R.string.dashboard_card_start)) },
                            completeLabel = { Text(stringResource(R.string.dashboard_card_complete)) },
                            deferLabel = { Text(stringResource(R.string.dashboard_card_defer)) },
                            deleteLabel = { Text(stringResource(R.string.dashboard_card_delete)) },
                        )
                    },
                )
            }
            is DashboardCardModel.TimelineCard -> {
                card.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NiaOutlinedButton(
                            text = { Text(stringResource(R.string.dashboard_prompt_button)) },
                            onClick = { onAction(CardAction.TriggerPrompt(item.tileId)) },
                        )
                        Icon(
                            imageVector = statusIcon(item.status),
                            contentDescription = stringResource(R.string.dashboard_status_cd),
                        )
                        Text(item.timestampIso, style = MaterialTheme.typography.labelSmall)
                        Text("│", style = MaterialTheme.typography.labelSmall)
                        Text(item.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun statusIcon(status: CardStatus) = when (status) {
    CardStatus.READY -> Icons.Default.RadioButtonUnchecked
    CardStatus.STARTED -> Icons.Default.PlayArrow
    CardStatus.DONE, CardStatus.ARCHIVED -> Icons.Default.Check
}