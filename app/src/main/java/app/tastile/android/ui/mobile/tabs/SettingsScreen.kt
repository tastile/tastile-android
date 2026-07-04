package app.tastile.android.ui.mobile.tabs

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.Build
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.notifications.ExecutionAlarmActivity
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.notifications.ExecutionAlarmTestReceiver
import app.tastile.android.notifications.ExecutionNotificationChannels
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection

@Composable
fun SettingsScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel = hiltViewModel(),
) {
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var notificationGranted by remember {
        mutableStateOf(canPostNotifications(context))
    }
    var notificationStatus by remember { mutableStateOf("") }
    var fullScreenGranted by remember { mutableStateOf(canUseFullScreenIntent(context)) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
        notificationStatus = if (granted) "Notifications enabled" else "Notifications blocked"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
    ) {
        SettingsRow(icon = "🌐", label = "Locale", value = localeLabel(locale), overlay = overlay)
        SettingsRow(icon = "🎨", label = "Theme", value = "gray", overlay = overlay)
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
            }
        )
        SettingsRow(icon = "🔒", label = "Privacy", value = "›", overlay = overlay)
        SettingsRow(icon = "ℹ", label = "About", value = "›", overlay = overlay)
    }
}

private fun localeLabel(l: AppLocale): String = when (l) {
    AppLocale.JA -> "ja"
    AppLocale.EN -> "en"
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
                style = MaterialTheme.typography.bodySmall
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

@Composable
private fun SettingsRow(
    icon: String,
    label: String,
    value: String,
    overlay: OverlayViewModel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                overlay.show(Overlay.SidePanel(SidePanelSection.Preferences))
            }
            .padding(vertical = AppTheme.spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$icon $label", style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
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
