package app.tastile.android.ui.mobile.tabs

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.notifications.ExecutionAlarmActivity
import app.tastile.android.notifications.ExecutionAlarmTestReceiver
import app.tastile.android.notifications.ExecutionNotificationChannels
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.components.LocalePickerDialog
import app.tastile.android.ui.dashboard.components.ThemePickerDialog
import app.tastile.android.ui.dashboard.components.TimeoutPickerDialog
import app.tastile.android.ui.designsystem.AppTheme

@Composable
fun SettingsScreen(
    viewModel: DashboardViewModel,
) {
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val securityLockEnabled by viewModel.securityLockEnabled.collectAsStateWithLifecycle()
    val timeoutMin by viewModel.securityLockTimeoutMinutes.collectAsStateWithLifecycle()

    var showLocale by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showTimeout by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var notificationGranted by remember { mutableStateOf(canPostNotifications(context)) }
    var notificationStatus by remember { mutableStateOf("") }
    var fullScreenGranted by remember { mutableStateOf(canUseFullScreenIntent(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationGranted = granted
        notificationStatus = if (granted) "Notifications enabled" else "Notifications denied"
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        SettingsRow(
            icon = "🌐",
            label = "Locale",
            value = localeLabel(locale),
            onClick = { showLocale = true },
        )
        SettingsRow(
            icon = "🎨",
            label = "Theme",
            value = themeLabel(theme),
            onClick = { showTheme = true },
        )
        SecurityLockRow(
            enabled = securityLockEnabled,
            timeoutMinutes = timeoutMin,
            onToggle = { viewModel.setSecurityLockEnabled(it) },
            onTimeout = { showTimeout = true },
        )
        NotificationSettingsSection(
            granted = notificationGranted,
            status = notificationStatus,
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notificationGranted = true
                    notificationStatus = "Notifications enabled"
                }
            },
            fullScreenGranted = fullScreenGranted,
            onRequestFullScreen = {
                openFullScreenIntentSettings(context)
                fullScreenGranted = canUseFullScreenIntent(context)
                notificationStatus = "Enable full-screen alarms in system settings"
            },
            onTestNotification = {
                notificationGranted = canPostNotifications(context)
                fullScreenGranted = canUseFullScreenIntent(context)
                if (notificationGranted) {
                    postTestNotification(context)
                    notificationStatus = "Test notification sent"
                } else {
                    notificationStatus = "Allow notifications before testing"
                }
            },
            onTestAlarm = {
                notificationGranted = canPostNotifications(context)
                fullScreenGranted = canUseFullScreenIntent(context)
                if (notificationGranted) {
                    postTestAlarm(context)
                    notificationStatus = "Alarm will open in 3 seconds"
                } else {
                    notificationStatus = "Allow notifications before testing alarms"
                }
            },
        )
        SettingsRow(
            icon = "🔒",
            label = "Privacy",
            value = "›",
            onClick = { showPrivacy = true },
        )
        SettingsRow(
            icon = "ℹ",
            label = "About",
            value = "›",
            onClick = { showAbout = true },
        )
    }

    if (showLocale) {
        LocalePickerDialog(
            current = locale,
            onPick = { viewModel.setLocale(it); showLocale = false },
            onDismiss = { showLocale = false },
        )
    }
    if (showTheme) {
        ThemePickerDialog(
            current = theme,
            onPick = { viewModel.setThemeMode(it); showTheme = false },
            onDismiss = { showTheme = false },
        )
    }
    if (showTimeout) {
        TimeoutPickerDialog(
            currentMinutes = timeoutMin,
            onPick = { viewModel.setSecurityLockTimeoutMinutes(it); showTimeout = false },
            onDismiss = { showTimeout = false },
        )
    }
    if (showPrivacy) {
        PrivacyDialog(onDismiss = { showPrivacy = false })
    }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun SecurityLockRow(
    enabled: Boolean,
    timeoutMinutes: Int,
    onToggle: (Boolean) -> Unit,
    onTimeout: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.xs)
            .semantics(mergeDescendants = true) { contentDescription = "Security lock" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Text("🔒", style = MaterialTheme.typography.bodyMedium)
        Column(modifier = Modifier.weight(1f)) {
            Text("Security lock", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Require biometric to open the app",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
    if (enabled) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onTimeout)
                .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
                .semantics { contentDescription = "Lock timeout" },
        ) {
            Text(
                "Timeout: $timeoutMinutes min  ›",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: String,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = AppTheme.spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Text(icon, style = MaterialTheme.typography.bodyMedium)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("tastile stores your tiles and execution history on AWS (Cognito + RDS).")
                Text(
                    "View the full privacy policy:",
                    style = MaterialTheme.typography.bodySmall,
                )
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://tastile.app/privacy"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                        .padding(vertical = 4.dp),
                ) {
                    Text("tastile.app/privacy", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("unknown")
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About tastile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version: $version")
                Row(
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rebuildup/tastile"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                        .padding(vertical = 4.dp),
                ) {
                    Text("github.com/rebuildup/tastile", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun NotificationSettingsSection(
    granted: Boolean,
    status: String,
    fullScreenGranted: Boolean,
    onRequestPermission: () -> Unit,
    onRequestFullScreen: () -> Unit,
    onTestNotification: () -> Unit,
    onTestAlarm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription = "Notifications: ${if (granted) "allowed" else "blocked"}, alarms: ${if (fullScreenGranted) "allowed" else "limited"}"
            },
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔔 Notifications", style = MaterialTheme.typography.bodyMedium)
            Text(
                if (granted && fullScreenGranted) "Alarm ready" else if (granted) "Limited" else "Blocked",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onRequestPermission) {
                Text("Allow")
            }
            Button(onClick = onRequestFullScreen) {
                Text("Full screen")
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onTestNotification) {
                Text("Test")
            }
            Button(onClick = onTestAlarm) {
                Text("Alarm")
            }
        }
        if (status.isNotBlank()) {
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun localeLabel(l: AppLocale): String = when (l) {
    AppLocale.JA -> "日本語"
    AppLocale.EN -> "English"
}

private fun themeLabel(t: ThemeMode): String = when (t) {
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun canPostNotifications(context: android.content.Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

private fun canUseFullScreenIntent(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
    return manager.canUseFullScreenIntent()
}

private fun openFullScreenIntentSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    context.startActivity(
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
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

@SuppressLint("MissingPermission")
private fun postTestAlarm(context: android.content.Context) {
    if (!canPostNotifications(context)) return
    ExecutionNotificationChannels.ensure(context)
    val title = "Tastile alarm"
    val body = "This is an alarm-style test notification from Tastile."
    val triggerAtMillis = System.currentTimeMillis() + 3_000L
    val receiverIntent = Intent(context, ExecutionAlarmTestReceiver::class.java).apply {
        action = ExecutionAlarmTestReceiver.ACTION_TEST_ALARM
        putExtra(ExecutionAlarmTestReceiver.EXTRA_TITLE, title)
        putExtra(ExecutionAlarmTestReceiver.EXTRA_BODY, body)
        putExtra(ExecutionAlarmActivity.EXTRA_NOTIFICATION_ID, TEST_ALARM_NOTIFICATION_ID)
    }
    val operation = PendingIntent.getBroadcast(
        context,
        TEST_ALARM_NOTIFICATION_ID,
        receiverIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val showIntent = PendingIntent.getActivity(
            context,
            TEST_ALARM_NOTIFICATION_ID,
            Intent(context, ExecutionAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(ExecutionAlarmActivity.EXTRA_TITLE, title)
                putExtra(ExecutionAlarmActivity.EXTRA_BODY, body)
                putExtra(ExecutionAlarmActivity.EXTRA_NOTIFICATION_ID, TEST_ALARM_NOTIFICATION_ID)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), operation)
    } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
    }
}

private const val TEST_NOTIFICATION_ID = 491
private const val TEST_ALARM_NOTIFICATION_ID = 492