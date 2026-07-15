package app.tastile.android.ui.mobile.tabs

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.notifications.ExecutionNotificationChannels
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.designsystem.AppListRow
import app.tastile.android.ui.designsystem.AppPageColumn
import app.tastile.android.ui.designsystem.AppTheme

private const val TIMEOUT_STEP = 5
private const val TIMEOUT_MIN = 1
private const val TIMEOUT_MAX = 240
private const val TEST_NOTIFICATION_ID = 491

@Composable
fun SettingsScreen(
    viewModel: DashboardViewModel,
) {
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val securityLockEnabled by viewModel.securityLockEnabled.collectAsStateWithLifecycle()
    val timeoutMin by viewModel.securityLockTimeoutMinutes.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var notificationGranted by remember { mutableStateOf(canPostNotifications(context)) }
    var notificationStatus by remember {
        mutableStateOf(
            if (canPostNotifications(context)) {
                context.getString(R.string.settings_notifications_status_allowed)
            } else {
                context.getString(R.string.settings_notifications_status_unsupported)
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationGranted = granted
        notificationStatus = context.getString(
            if (granted) R.string.settings_notifications_status_allowed
            else R.string.settings_notifications_status_denied
        )
    }

    AppPageColumn {
        ThemeSection(
            current = theme,
            onPick = { viewModel.setThemeMode(it) },
        )
        LanguageSection(
            current = locale,
            onPick = { viewModel.setLocale(it) },
        )
        SecurityLockSection(
            enabled = securityLockEnabled,
            timeoutMinutes = timeoutMin,
            onToggle = { viewModel.setSecurityLockEnabled(it) },
            onDecrement = {
                viewModel.setSecurityLockTimeoutMinutes(timeoutMin - TIMEOUT_STEP)
            },
            onIncrement = {
                viewModel.setSecurityLockTimeoutMinutes(timeoutMin + TIMEOUT_STEP)
            },
        )
        NotificationsSection(
            granted = notificationGranted,
            status = notificationStatus,
            onAllow = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notificationGranted = true
                    notificationStatus = context.getString(
                        R.string.settings_notifications_status_allowed
                    )
                }
            },
            onTest = {
                val grantedNow = canPostNotifications(context)
                notificationGranted = grantedNow
                if (grantedNow) {
                    postTestNotification(context)
                    notificationStatus = context.getString(
                        R.string.settings_notifications_test
                    )
                } else {
                    notificationStatus = context.getString(
                        R.string.settings_notifications_status_denied
                    )
                }
            },
        )
    }
}

@Composable
private fun ThemeSection(
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = stringResource(R.string.settings_theme)
            },
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        AppListRow(
            label = stringResource(R.string.settings_theme),
            meta = themeLabel(current),
            leading = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
            onClick = {},
            description = stringResource(R.string.settings_theme),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = current == ThemeMode.LIGHT,
                onClick = { onPick(ThemeMode.LIGHT) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            ) { Text(stringResource(R.string.settings_theme_light)) }
            SegmentedButton(
                selected = current == ThemeMode.GRAY,
                onClick = { onPick(ThemeMode.GRAY) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            ) { Text(stringResource(R.string.settings_theme_gray)) }
            SegmentedButton(
                selected = current == ThemeMode.DARK,
                onClick = { onPick(ThemeMode.DARK) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            ) { Text(stringResource(R.string.settings_theme_dark)) }
        }
    }
}

@Composable
private fun LanguageSection(
    current: AppLocale,
    onPick: (AppLocale) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = stringResource(R.string.settings_language)
            },
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        AppListRow(
            label = stringResource(R.string.settings_language),
            meta = localeLabel(current),
            leading = { Icon(Icons.Outlined.Language, contentDescription = null) },
            onClick = {},
            description = stringResource(R.string.settings_language),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = current == AppLocale.JA,
                onClick = { onPick(AppLocale.JA) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(R.string.settings_language_ja)) }
            SegmentedButton(
                selected = current == AppLocale.EN,
                onClick = { onPick(AppLocale.EN) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(R.string.settings_language_en)) }
        }
    }
}

@Composable
private fun SecurityLockSection(
    enabled: Boolean,
    timeoutMinutes: Int,
    onToggle: (Boolean) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        AppListRow(
            label = stringResource(
                if (enabled) R.string.settings_security_lock_on
                else R.string.settings_security_lock_off
            ),
            leading = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailing = { Switch(checked = enabled, onCheckedChange = onToggle) },
            onClick = { onToggle(!enabled) },
            description = stringResource(R.string.settings_security_lock_off),
        )
        if (enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.component.listRowIndent),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onDecrement) {
                    Text(stringResource(R.string.settings_security_lock_timeout_decrease))
                }
                Text(
                    stringResource(
                        R.string.settings_security_lock_timeout_label,
                        timeoutMinutes.coerceIn(TIMEOUT_MIN, TIMEOUT_MAX)
                    ),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurface,
                )
                Button(onClick = onIncrement) {
                    Text(stringResource(R.string.settings_security_lock_timeout_increase))
                }
            }
        }
    }
}

@Composable
private fun NotificationsSection(
    granted: Boolean,
    status: String,
    onAllow: () -> Unit,
    onTest: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = if (granted) {
                    stringResource(R.string.settings_notifications_status_allowed)
                } else {
                    stringResource(R.string.settings_notifications_status_denied)
                }
            },
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = AppTheme.colors.onSurfaceVariant,
                )
                Text(stringResource(R.string.mobile_top_notifications))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onAllow) {
                Text(stringResource(R.string.settings_notifications_allow))
            }
            Button(onClick = onTest) {
                Text(stringResource(R.string.settings_notifications_test))
            }
        }
        if (status.isNotBlank()) {
            Text(status, style = AppTheme.typography.bodySmall)
        }
    }
}

private fun localeLabel(l: AppLocale): String = when (l) {
    AppLocale.JA -> "日本語"
    AppLocale.EN -> "English"
}

private fun themeLabel(t: ThemeMode): String = when (t) {
    ThemeMode.LIGHT -> "Light"
    ThemeMode.GRAY -> "Gray"
    ThemeMode.DARK -> "Dark"
}

private fun canPostNotifications(context: android.content.Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private fun postTestNotification(context: android.content.Context) {
    if (!canPostNotifications(context)) return
    ExecutionNotificationChannels.ensure(context)
    NotificationManagerCompat.from(context).notify(
        TEST_NOTIFICATION_ID,
        NotificationCompat.Builder(context, ExecutionNotificationChannels.ALERTS)
            .setSmallIcon(R.drawable.ic_notification_tastile)
            .setContentTitle("Tastile")
            .setContentText("This is a test notification from Tastile.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("This is a test notification from Tastile."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
    )
}