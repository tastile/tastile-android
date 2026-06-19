package app.tastile.android.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Switch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.ui.designsystem.AppBodyText
import app.tastile.android.ui.designsystem.AppDangerButton
import app.tastile.android.ui.designsystem.AppInlineError
import app.tastile.android.ui.designsystem.AppOutlinedPanel
import app.tastile.android.ui.designsystem.AppPrimaryButton
import app.tastile.android.ui.designsystem.AppScreenTitle
import app.tastile.android.ui.designsystem.AppSecondaryButton
import app.tastile.android.ui.designsystem.AppTonalButton
import app.tastile.android.ui.designsystem.AppTheme

@Composable
fun IntegrationsDashboardScreen(viewModel: DashboardViewModel) {
    val integration by viewModel.googleCalendarIntegration.collectAsStateWithLifecycle()
    val syncPlan by viewModel.calendarSyncPlanPreview.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
    ) {
        AppScreenTitle("Integrations")
        AppOutlinedPanel {
            AppBodyText("Google Calendar")
            AppBodyText(if (integration?.connected == true) "接続済み" else "未接続")
            AppBodyText("Read: " + if (integration?.canRead == true) "enabled" else "disabled")
            AppBodyText("Write: " + if (integration?.canWrite == true) "enabled" else "disabled")
            AppBodyText("Account: " + (integration?.accountEmail ?: "not linked"))
            AppBodyText("Sync mode: " + (integration?.syncMode ?: "push_only"))
            AppBodyText("Target calendar: " + (integration?.selectedCalendarId ?: "primary"))
            AppBodyText("Plan: " + (syncPlan?.syncMode ?: "push_only") + " / " + (syncPlan?.readPolicy ?: "import_and_block_scheduling") + " / " + (syncPlan?.writePolicy ?: "tastile_owned_only"))
            AppBodyText("Last synced: " + (integration?.lastSyncedAt ?: "never"))

            if (integration?.connected == true) {
                val currentIntegration = integration
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 88.dp, max = 220.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    items(4) { idx ->
                        when (idx) {
                            0 -> AppPrimaryButton(text = "Sync now", onClick = { viewModel.syncGoogleCalendarNow() }, modifier = Modifier.fillMaxWidth())
                            1 -> AppDangerButton(text = "Disconnect", onClick = { viewModel.disconnectGoogleCalendar() }, modifier = Modifier.fillMaxWidth())
                            2 -> AppTonalButton(text = "Push only", onClick = { viewModel.updateGoogleCalendarPolicy("push_only", currentIntegration?.selectedCalendarId) }, modifier = Modifier.fillMaxWidth())
                            else -> AppSecondaryButton(text = "Bidirectional", onClick = { viewModel.updateGoogleCalendarPolicy("bidirectional", currentIntegration?.selectedCalendarId) }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            } else {
                AppPrimaryButton(
                    text = "Connect",
                    onClick = { viewModel.connectGoogleCalendar() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (!error.isNullOrBlank()) {
            AppInlineError(error.orEmpty())
        }
    }
}

@Composable
fun SettingsDashboardScreen(viewModel: DashboardViewModel) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    val securityLockEnabled by viewModel.securityLockEnabled.collectAsStateWithLifecycle()
    val securityLockTimeoutMinutes by viewModel.securityLockTimeoutMinutes.collectAsStateWithLifecycle()
    val daemonStatusSummary by viewModel.daemonStatusSummary.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    fun t(ja: String, en: String): String = if (locale == AppLocale.JA) ja else en

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppScreenTitle(t("設定", "Settings"))

        AppOutlinedPanel {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppBodyText(t("テーマ", "Theme"))
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
        }

        AppOutlinedPanel {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppBodyText(t("言語", "Language"))
                AppLocale.entries.forEach { lang ->
                    SelectRow(
                        selected = locale == lang,
                        label = if (lang == AppLocale.JA) "日本語" else "English",
                        onClick = { viewModel.setLocale(lang) }
                    )
                }
            }
        }

        AppOutlinedPanel {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppBodyText(t("セキュリティロック", "Security Lock"))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppBodyText(t("起動時に端末ロック解除を要求", "Require device unlock on launch"))
                    Switch(
                        checked = securityLockEnabled,
                        onCheckedChange = { viewModel.setSecurityLockEnabled(it) }
                    )
                }
                AppBodyText(t("10分以上アプリを離れた後に有効", "Applies after leaving the app for 10+ minutes"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSecondaryButton(text = "-5", onClick = { viewModel.setSecurityLockTimeoutMinutes(securityLockTimeoutMinutes - 5) })
                    AppBodyText("$securityLockTimeoutMinutes min")
                    AppSecondaryButton(text = "+5", onClick = { viewModel.setSecurityLockTimeoutMinutes(securityLockTimeoutMinutes + 5) })
                }
            }
        }

        AppTonalButton(
            text = t("ログアウト", "Sign Out"),
            onClick = { viewModel.signOut() },
            modifier = Modifier.fillMaxWidth()
        )

        AppOutlinedPanel {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppBodyText(t("デーモン", "Daemon"))
                AppBodyText(daemonStatusSummary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppPrimaryButton(text = t("状態更新", "Refresh"), onClick = { viewModel.refreshDaemonStatus() })
                    AppTonalButton(text = t("Tick実行", "Trigger Tick"), onClick = { viewModel.triggerDaemonTick() })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSecondaryButton(text = t("ローカル再構築", "Reset Local"), onClick = { viewModel.resetLocalSyncData() })
                    AppSecondaryButton(text = t("再ダウンロード", "Redownload"), onClick = { viewModel.redownloadRemoteSyncData() })
                }
            }
        }

        if (!error.isNullOrBlank()) {
            AppInlineError(error.orEmpty())
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

@Composable
private fun SelectRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AppBodyText(label)
        if (selected) {
            AppTonalButton(text = "Selected", onClick = onClick)
        } else {
            AppSecondaryButton(text = "Select", onClick = onClick)
        }
    }
}
