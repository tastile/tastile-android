package app.tastile.android.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.ThemeMode

@Composable
fun ExecuteDashboardScreen(viewModel: DashboardViewModel) {
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    fun t(ja: String, en: String): String = if (locale == AppLocale.JA) ja else en
    val cards = viewModel.buildExecuteCards()

    if (loading && cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(t("実行", "Execute")) }
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(t("タイル", "Tiles")) }
        items(cards, key = { it.id }) { card ->
            DashboardCardRenderer(
                card = card,
                onAction = viewModel::handleCardAction
            )
        }
    }
}

@Composable
fun IntegrationsDashboardScreen(viewModel: DashboardViewModel) {
    val integration by viewModel.googleCalendarIntegration.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Integrations")
        Text("Google Calendar")
        Text(if (integration?.connected == true) "接続済み" else "未接続")
        Text("Read: " + if (integration?.canRead == true) "enabled" else "disabled")
        Text("Write: " + if (integration?.canWrite == true) "enabled" else "disabled")
        Text("Account: " + (integration?.accountEmail ?: "not linked"))
        Text("Last synced: " + (integration?.lastSyncedAt ?: "never"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (integration?.connected == true) {
                OutlinedButton(onClick = { viewModel.disconnectGoogleCalendar() }) { Text("Disconnect") }
                OutlinedButton(onClick = { viewModel.syncGoogleCalendarNow() }) { Text("Sync now") }
            } else {
                OutlinedButton(onClick = { viewModel.connectGoogleCalendar() }) { Text("Connect") }
            }
        }
        if (!error.isNullOrBlank()) {
            Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SettingsDashboardScreen(viewModel: DashboardViewModel) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    val daemonStatusSummary by viewModel.daemonStatusSummary.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    fun t(ja: String, en: String): String = if (locale == AppLocale.JA) ja else en

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(t("設定", "Settings"))

        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t("テーマ", "Theme"))
            ThemeMode.entries.forEach { mode ->
                SelectRow(
                    selected = theme == mode,
                    label = when (mode) {
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    onClick = { viewModel.setThemeMode(mode) }
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t("言語", "Language"))
            AppLocale.entries.forEach { lang ->
                SelectRow(
                    selected = locale == lang,
                    label = if (lang == AppLocale.JA) "日本語" else "English",
                    onClick = { viewModel.setLocale(lang) }
                )
            }
        }

        OutlinedButton(
            onClick = { viewModel.signOut() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(t("ログアウト", "Sign Out"))
        }

        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t("デーモン", "Daemon"))
            Text(
                text = daemonStatusSummary,
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.refreshDaemonStatus() }) { Text(t("状態更新", "Refresh")) }
                OutlinedButton(onClick = { viewModel.triggerDaemonTick() }) { Text(t("Tick実行", "Trigger Tick")) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.resetLocalSyncData() }) { Text(t("ローカル再構築", "Reset Local")) }
                OutlinedButton(onClick = { viewModel.redownloadRemoteSyncData() }) { Text(t("再ダウンロード", "Redownload")) }
            }
        }

        if (!error.isNullOrBlank()) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun AccountDashboardScreen(viewModel: DashboardViewModel) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val statsDiagnostics by viewModel.statsDiagnostics.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    fun t(ja: String, en: String): String = if (locale == AppLocale.JA) ja else en
    val activeTab = remember { mutableStateOf("profile") }
    val total = tiles.size
    val completed = tiles.count { it.isDone() }
    val started = tiles.count { it.isStarted() }
    val ready = total - completed - started
    val completionRate = if (total == 0) 0 else (completed * 100) / total

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(t("アカウント設定", "Account Settings"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                "profile" to t("プロフィール", "Profile"),
                "subscription" to t("サブスク", "Subscription"),
                "statistics" to t("統計", "Statistics"),
                "usage" to t("利用状況", "Usage")
            )
                .forEach { (key, label) ->
                    OutlinedButton(onClick = { activeTab.value = key }, modifier = Modifier.weight(1f)) { Text(label) }
                }
        }

        when (activeTab.value) {
            "profile" -> Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("プロフィール情報", "Profile Information"))
                Text(profile?.displayName ?: t("表示名なし", "No display name"), style = MaterialTheme.typography.bodyLarge)
                Text(email, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(t("ログアウト", "Sign Out"))
                }
            }
            "subscription" -> Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("サブスクリプション", "Subscription"))
                Text(t("現在のプラン", "Current Plan") + ": ${profile?.plan ?: "free"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { uriHandler.openUri("https://tastile.app/api/stripe/checkout") }) {
                        Text(t("Proへアップグレード", "Upgrade to Pro"))
                    }
                    OutlinedButton(onClick = { uriHandler.openUri("https://tastile.app/api/stripe/portal") }) {
                        Text(t("請求管理", "Manage Billing"))
                    }
                }
            }
            "statistics" -> Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(t("タイル統計", "Tile Statistics"))
                Text(t("総数", "Total") + ": $total")
                Text(t("完了", "Completed") + ": $completed")
                Text(t("進行中", "In Progress") + ": $started")
                Text(t("準備完了", "Ready") + ": $ready")
                Text(t("完了率", "Completion Rate") + ": $completionRate%")
                Text(
                    text = t("診断", "Diagnostics") + ": $statsDiagnostics",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            "usage" -> Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("利用ダッシュボード", "Usage Dashboard"))
                Text(t("準備中: 利用分析チャートを追加予定です。", "Coming Soon: analytics charts for productivity and focus trends."))
                Text("• " + t("Tiles Over Time", "Tiles Over Time"))
                Text("• " + t("Completion Rate", "Completion Rate"))
                Text("• " + t("Focus Time", "Focus Time"))
                Text("• " + t("Activity Heatmap", "Activity Heatmap"))
            }
        }

        if (!error.isNullOrBlank()) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall
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
                        IconButton(onClick = onStart) { Icon(Icons.Default.PlayArrow, contentDescription = "Start") }
                    }
                    if (lifecycle == TileLifecycle.STARTED) {
                        IconButton(onClick = onComplete) { Icon(Icons.Default.PlayArrow, contentDescription = "Complete") }
                    }
                    OutlinedButton(onClick = onDefer, modifier = Modifier.height(34.dp)) { Text("Defer") }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.height(34.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
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
            .background(MaterialTheme.colorScheme.surface)
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
private fun SelectRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        OutlinedButton(onClick = onClick, modifier = Modifier.height(32.dp)) { Text(if (selected) "Selected" else "Select") }
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

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        headerActionTileId?.let { onAction(CardAction.TriggerPrompt(it)) }
                    },
                    enabled = headerActionTileId != null
                ) {
                    Icon(
                        imageVector = statusIcon(card.status),
                        contentDescription = "Trigger Prompt"
                    )
                }
                Text(card.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }

            when (card) {
                is DashboardCardModel.BaseCard -> {
                    CardPrimaryActions(card.id, card.status, onAction)
                }
                is DashboardCardModel.TimePriorityCard -> {
                    CardPrimaryActions(card.id, card.status, onAction)
                }

                is DashboardCardModel.TimelineCard -> {
                    card.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onAction(CardAction.TriggerPrompt(item.tileId)) }) {
                                Icon(
                                    imageVector = statusIcon(item.status),
                                    contentDescription = "Trigger Prompt"
                                )
                            }
                            Text(item.timestampIso, style = MaterialTheme.typography.labelSmall)
                            Text("│", style = MaterialTheme.typography.labelSmall)
                            Text(item.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        }
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
    val actions = when (status) {
        CardStatus.READY -> listOf(
            "Start" to CardAction.StartTile(tileId),
            "Delete" to CardAction.DeleteTile(tileId)
        )
        CardStatus.STARTED -> listOf(
            "Complete" to CardAction.CompleteTile(tileId),
            "Defer" to CardAction.DeferTile(tileId),
            "Break" to CardAction.StartBreak,
            "Extend+10" to CardAction.ExtendTile(minutes = 10)
        )
        CardStatus.DONE, CardStatus.ARCHIVED -> listOf(
            "Delete" to CardAction.DeleteTile(tileId)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { (label, action) ->
            OutlinedButton(
                onClick = { onAction(action) },
                modifier = Modifier.height(34.dp)
            ) {
                Text(label)
            }
        }
    }
}

private fun statusIcon(status: CardStatus) = when (status) {
    CardStatus.READY -> Icons.Default.RadioButtonUnchecked
    CardStatus.STARTED -> Icons.Default.PlayArrow
    CardStatus.DONE, CardStatus.ARCHIVED -> Icons.Default.Check
}
