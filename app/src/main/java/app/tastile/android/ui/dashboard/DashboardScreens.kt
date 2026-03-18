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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.ThemeMode

@Composable
fun ExecuteDashboardScreen(viewModel: DashboardViewModel) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    fun t(ja: String, en: String): String = if (locale == AppLocale.JA) ja else en
    val activeTile = tiles.firstOrNull { it.isStarted() }
    val notDoneTiles = tiles.filterNot { it.isDone() }
    val suggestion = notDoneTiles.firstOrNull { !it.isStarted() } ?: activeTile
    val timelineTiles = notDoneTiles.take(6)
    val pendingDelete = remember { mutableStateOf<Tile?>(null) }

    if (loading && tiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(t("実行", "Execute"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }

        item {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t("現在の実行", "Active Execution"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(activeTile?.title ?: t("待機中", "Idle"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t("次のタイル", "Next Tile"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TileCompactCard(tile = suggestion, onStart = { id -> viewModel.startTile(id) })
                    }
                }
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t("タイムライン", "Timeline"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (timelineTiles.isEmpty()) {
                            Text(t("予定されたタイルはありません", "No upcoming tiles"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            timelineTiles.forEach { tile ->
                                TileCompactCard(tile = tile, onStart = { id -> viewModel.startTile(id) })
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t("実行可能タイル", "Ready Tiles"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { activeTile?.let { viewModel.completeTile(it.id) } },
                            enabled = activeTile != null,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(t("アクティブ完了", "Complete Active Tile"))
                        }
                    }

                    if (notDoneTiles.isEmpty()) {
                        Text(t("まだタイルがありません。Newから作成してください。", "No tiles yet. Click New to create one."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        notDoneTiles.forEach { tile ->
                            TileExpandableCard(
                                tile = tile,
                                onStart = { viewModel.startTile(tile.id) },
                                onComplete = { viewModel.completeTile(tile.id) },
                                onDefer = { viewModel.deferTile(tile.id) },
                                onDelete = { pendingDelete.value = tile }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete.value?.let { tile ->
        AlertDialog(
            onDismissRequest = { pendingDelete.value = null },
            title = { Text(t("タイル削除", "Delete Tile")) },
            text = { Text(t("「${tile.title}」を削除しますか？", "Delete \"${tile.title}\"?")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTile(tile.id)
                        pendingDelete.value = null
                    }
                ) { Text(t("削除", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete.value = null }) { Text(t("キャンセル", "Cancel")) }
            }
        )
    }
}

@Composable
fun TilesDashboardScreen(viewModel: DashboardViewModel) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    fun t(ja: String, en: String): String = if (locale == AppLocale.JA) ja else en
    val pendingDelete = remember { mutableStateOf<Tile?>(null) }

    if (loading && tiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(t("タイル", "Tiles"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(tiles, key = { it.id }) { tile ->
            TileExpandableCard(
                tile = tile,
                onStart = { viewModel.startTile(tile.id) },
                onComplete = { viewModel.completeTile(tile.id) },
                onDefer = { viewModel.deferTile(tile.id) },
                onDelete = { pendingDelete.value = tile }
            )
        }
    }

    pendingDelete.value?.let { tile ->
        AlertDialog(
            onDismissRequest = { pendingDelete.value = null },
            title = { Text(t("タイル削除", "Delete Tile")) },
            text = { Text(t("「${tile.title}」を削除しますか？", "Delete \"${tile.title}\"?")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTile(tile.id)
                        pendingDelete.value = null
                    }
                ) { Text(t("削除", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete.value = null }) { Text(t("キャンセル", "Cancel")) }
            }
        )
    }
}

@Composable
fun IntegrationsDashboardScreen() {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Integrations", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("外部連携管理 - Github, Discord, Linear, Notion等", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsDashboardScreen(viewModel: DashboardViewModel) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    fun t(ja: String, en: String): String = if (locale == AppLocale.JA) ja else en

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(t("設定", "Settings"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t("テーマ", "Theme"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ThemeMode.entries.forEach { mode ->
                    SelectRow(
                        selected = theme == mode,
                        label = when (mode) {
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.GRAY -> "Gray"
                            ThemeMode.DARK -> "Dark"
                        },
                        onClick = { viewModel.setThemeMode(mode) }
                    )
                }
            }
        }

        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t("言語", "Language"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                AppLocale.entries.forEach { lang ->
                    SelectRow(
                        selected = locale == lang,
                        label = if (lang == AppLocale.JA) "日本語" else "English",
                        onClick = { viewModel.setLocale(lang) }
                    )
                }
            }
        }
    }
}

@Composable
fun AccountDashboardScreen(viewModel: DashboardViewModel) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
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
        Text(t("アカウント設定", "Account Settings"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
            "profile" -> Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("プロフィール情報", "Profile Information"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(profile?.displayName ?: t("表示名なし", "No display name"), style = MaterialTheme.typography.bodyLarge)
                    Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            "subscription" -> Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("サブスクリプション", "Subscription"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            }
            "statistics" -> Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t("タイル統計", "Tile Statistics"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(t("総数", "Total") + ": $total")
                    Text(t("完了", "Completed") + ": $completed")
                    Text(t("進行中", "In Progress") + ": $started")
                    Text(t("準備完了", "Ready") + ": $ready")
                    Text(t("完了率", "Completion Rate") + ": $completionRate%")
                }
            }
            "usage" -> Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("利用ダッシュボード", "Usage Dashboard"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(t("準備中: 利用分析チャートを追加予定です。", "Coming Soon: analytics charts for productivity and focus trends."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• " + t("Tiles Over Time", "Tiles Over Time"))
                    Text("• " + t("Completion Rate", "Completion Rate"))
                    Text("• " + t("Focus Time", "Focus Time"))
                    Text("• " + t("Activity Heatmap", "Activity Heatmap"))
                }
            }
        }
    }
}

@Composable
private fun TileCompactCard(tile: Tile?, onStart: (String) -> Unit) {
    if (tile == null) {
        Text("No tile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusCircle(
            lifecycle = lifecycle,
            onClick = if (lifecycle == TileLifecycle.READY) ({ onStart(tile.id) }) else null
        )
        Text(tile.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
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

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
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
                    Text(tile.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(tile.lifecycle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Next: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    tile.doneDefinition?.takeIf { it.isNotBlank() }?.let {
                        Text("Done: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
private fun StatusCircle(lifecycle: TileLifecycle, onClick: (() -> Unit)?) {
    val bg = when (lifecycle) {
        TileLifecycle.DONE -> MaterialTheme.colorScheme.primary
        TileLifecycle.STARTED -> MaterialTheme.colorScheme.tertiary
        TileLifecycle.READY -> MaterialTheme.colorScheme.surfaceContainerHighest
        TileLifecycle.ARCHIVED -> MaterialTheme.colorScheme.surfaceContainer
    }
    val fg = when (lifecycle) {
        TileLifecycle.DONE, TileLifecycle.STARTED -> MaterialTheme.colorScheme.onPrimary
        TileLifecycle.READY -> MaterialTheme.colorScheme.onSurfaceVariant
        TileLifecycle.ARCHIVED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(bg, CircleShape)
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
            color = fg,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun SelectRow(selected: Boolean, label: String, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(shape = RoundedCornerShape(10.dp), color = bg, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = fg, fontWeight = FontWeight.Medium)
            OutlinedButton(onClick = onClick, modifier = Modifier.height(32.dp)) { Text(if (selected) "Selected" else "Select") }
        }
    }
}
