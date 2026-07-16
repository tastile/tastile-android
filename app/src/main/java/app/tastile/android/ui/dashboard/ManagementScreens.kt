package app.tastile.android.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton
import app.tastile.android.core.designsystem.component.NiaOutlinedButton
import app.tastile.android.core.designsystem.component.NiaOutlinedCard
import app.tastile.android.data.repository.AppLocale

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
        Text(t("アカウント設定", "Account Settings"), style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                "profile" to t("プロフィール", "Profile"),
                "subscription" to t("サブスク", "Subscription"),
                "statistics" to t("統計", "Statistics"),
                "usage" to t("利用状況", "Usage")
            ).forEach { (key, label) ->
                if (activeTab.value == key) {
                    NiaFilledTonalButton(
                        text = { Text(label) },
                        onClick = { activeTab.value = key },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    NiaOutlinedButton(
                        text = { Text(label) },
                        onClick = { activeTab.value = key },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        when (activeTab.value) {
            "profile" -> NiaOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("プロフィール情報", "Profile Information"))
                    Text(profile?.displayName ?: t("表示名なし", "No display name"))
                    Text(email)
                    NiaButton(
                        text = { Text(t("Webでアカウント設定を開く", "Open Web Account Settings")) },
                        onClick = { uriHandler.openUri("https://app.tastile.app/dashboard/account") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    NiaOutlinedButton(
                        text = { Text(t("ログアウト", "Sign Out")) },
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            "subscription" -> NiaOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("サブスクリプション", "Subscription"))
                    Text(t("現在のプラン", "Current Plan") + ": ${profile?.plan ?: "free"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NiaButton(text = { Text(t("Proへアップグレード", "Upgrade to Pro")) }, onClick = { uriHandler.openUri("https://tastile.app/api/stripe/checkout") })
                        NiaOutlinedButton(text = { Text(t("請求管理", "Manage Billing")) }, onClick = { uriHandler.openUri("https://tastile.app/api/stripe/portal") })
                    }
                }
            }

            "statistics" -> NiaOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t("タイル統計", "Tile Statistics"))
                    Text(t("総数", "Total") + ": $total")
                    Text(t("完了", "Completed") + ": $completed")
                    Text(t("進行中", "In Progress") + ": $started")
                    Text(t("準備完了", "Ready") + ": $ready")
                    Text(t("完了率", "Completion Rate") + ": $completionRate%")
                    Text(t("診断", "Diagnostics") + ": $statsDiagnostics")
                }
            }

            "usage" -> NiaOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("利用ダッシュボード", "Usage Dashboard"))
                    Text(t("準備中: 利用分析チャートを追加予定です。", "Coming Soon: analytics charts for productivity and focus trends."))
                    Text("• " + t("Tiles Over Time", "Tiles Over Time"))
                    Text("• " + t("Completion Rate", "Completion Rate"))
                    Text("• " + t("Focus Time", "Focus Time"))
                    Text("• " + t("Activity Heatmap", "Activity Heatmap"))
                }
            }
        }

        if (!error.isNullOrBlank()) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
