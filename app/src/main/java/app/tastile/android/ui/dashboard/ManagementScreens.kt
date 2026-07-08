package app.tastile.android.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.ui.designsystem.AppBodyText
import app.tastile.android.ui.designsystem.AppDangerButton
import app.tastile.android.ui.designsystem.AppInlineError
import app.tastile.android.ui.designsystem.AppOutlinedPanel
import app.tastile.android.ui.designsystem.AppPrimaryButton
import app.tastile.android.ui.designsystem.AppScreenTitle
import app.tastile.android.ui.designsystem.AppSecondaryButton
import app.tastile.android.ui.designsystem.AppTonalButton

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
        AppScreenTitle(t("アカウント設定", "Account Settings"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                "profile" to t("プロフィール", "Profile"),
                "subscription" to t("サブスク", "Subscription"),
                "statistics" to t("統計", "Statistics"),
                "usage" to t("利用状況", "Usage")
            ).forEach { (key, label) ->
                if (activeTab.value == key) {
                    AppTonalButton(
                        text = label,
                        onClick = { activeTab.value = key },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    AppSecondaryButton(
                        text = label,
                        onClick = { activeTab.value = key },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        when (activeTab.value) {
            "profile" -> AppOutlinedPanel {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppBodyText(t("プロフィール情報", "Profile Information"))
                    AppBodyText(profile?.displayName ?: t("表示名なし", "No display name"))
                    AppBodyText(email)
                    AppPrimaryButton(
                        text = t("Webでアカウント設定を開く", "Open Web Account Settings"),
                        onClick = { uriHandler.openUri("https://app.tastile.app/dashboard/account") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppDangerButton(
                        text = t("ログアウト", "Sign Out"),
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            "subscription" -> AppOutlinedPanel {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppBodyText(t("サブスクリプション", "Subscription"))
                    AppBodyText(t("現在のプラン", "Current Plan") + ": ${profile?.plan ?: "free"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppPrimaryButton(text = t("Proへアップグレード", "Upgrade to Pro"), onClick = { uriHandler.openUri("https://tastile.app/api/stripe/checkout") })
                        AppSecondaryButton(text = t("請求管理", "Manage Billing"), onClick = { uriHandler.openUri("https://tastile.app/api/stripe/portal") })
                    }
                }
            }

            "statistics" -> AppOutlinedPanel {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppBodyText(t("タイル統計", "Tile Statistics"))
                    AppBodyText(t("総数", "Total") + ": $total")
                    AppBodyText(t("完了", "Completed") + ": $completed")
                    AppBodyText(t("進行中", "In Progress") + ": $started")
                    AppBodyText(t("準備完了", "Ready") + ": $ready")
                    AppBodyText(t("完了率", "Completion Rate") + ": $completionRate%")
                    AppBodyText(t("診断", "Diagnostics") + ": $statsDiagnostics")
                }
            }

            "usage" -> AppOutlinedPanel {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppBodyText(t("利用ダッシュボード", "Usage Dashboard"))
                    AppBodyText(t("準備中: 利用分析チャートを追加予定です。", "Coming Soon: analytics charts for productivity and focus trends."))
                    AppBodyText("• " + t("Tiles Over Time", "Tiles Over Time"))
                    AppBodyText("• " + t("Completion Rate", "Completion Rate"))
                    AppBodyText("• " + t("Focus Time", "Focus Time"))
                    AppBodyText("• " + t("Activity Heatmap", "Activity Heatmap"))
                }
            }
        }

        if (!error.isNullOrBlank()) {
            AppInlineError(error.orEmpty())
        }
    }
}
