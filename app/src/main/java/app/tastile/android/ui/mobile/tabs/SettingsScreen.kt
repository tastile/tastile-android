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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
// m2-allow: primitive
import androidx.compose.material3.CenterAlignedTopAppBar
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.IconButton
// m2-allow: state-holder
import androidx.compose.material3.ListItemDefaults
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.Scaffold
// m2-allow: state-holder
import androidx.compose.material3.SegmentedButtonDefaults
// m2-allow: primitive
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
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaListItem
import app.tastile.android.core.designsystem.component.NiaSegmentedButton
import app.tastile.android.core.designsystem.component.NiaSingleChoiceSegmentedButtonRow
import app.tastile.android.core.designsystem.component.NiaSwitch
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.notifications.ExecutionNotificationChannels
import app.tastile.android.ui.dashboard.DashboardViewModel

private const val TIMEOUT_STEP = 5
private const val TIMEOUT_MIN = 1
private const val TIMEOUT_MAX = 240
private const val TEST_NOTIFICATION_ID = 491

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit,
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
            },
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationGranted = granted
        notificationStatus = context.getString(
            if (granted) R.string.settings_notifications_status_allowed
            else R.string.settings_notifications_status_denied,
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
                        R.string.settings_notifications_status_allowed,
                    )
                }
            },
            onTest = {
                val grantedNow = canPostNotifications(context)
                notificationGranted = grantedNow
                if (grantedNow) {
                    postTestNotification(context)
                    notificationStatus = context.getString(
                        R.string.settings_notifications_test,
                    )
                } else {
                    notificationStatus = context.getString(
                        R.string.settings_notifications_status_denied,
                    )
                }
            },
        )
        }
    }
}

@Composable
private fun ThemeSection(
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
) {
    val themeA11y = stringResource(R.string.settings_theme)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = themeA11y
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NiaListItem(
            headlineContent = { Text(stringResource(R.string.settings_theme)) },
            supportingContent = { Text(themeLabel(current)) },
            leadingContent = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
        NiaSingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            NiaSegmentedButton(
                selected = current == ThemeMode.LIGHT,
                onClick = { onPick(ThemeMode.LIGHT) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            ) { Text(stringResource(R.string.settings_theme_light)) }
            NiaSegmentedButton(
                selected = current == ThemeMode.DARK,
                onClick = { onPick(ThemeMode.DARK) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            ) { Text(stringResource(R.string.settings_theme_dark)) }
            NiaSegmentedButton(
                selected = current == ThemeMode.SYSTEM,
                onClick = { onPick(ThemeMode.SYSTEM) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            ) { Text(stringResource(R.string.settings_theme_system)) }
        }
    }
}

@Composable
private fun LanguageSection(
    current: AppLocale,
    onPick: (AppLocale) -> Unit,
) {
    val languageA11y = stringResource(R.string.settings_language)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = languageA11y
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NiaListItem(
            headlineContent = { Text(stringResource(R.string.settings_language)) },
            supportingContent = { Text(localeLabel(current)) },
            leadingContent = { Icon(Icons.Outlined.Language, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
        NiaSingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            NiaSegmentedButton(
                selected = current == AppLocale.JA,
                onClick = { onPick(AppLocale.JA) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(R.string.settings_language_ja)) }
            NiaSegmentedButton(
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val a11yLabel = stringResource(R.string.settings_security_lock_off)
        NiaListItem(
            headlineContent = {
                Text(
                    stringResource(
                        if (enabled) R.string.settings_security_lock_on
                        else R.string.settings_security_lock_off,
                    ),
                )
            },
            leadingContent = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingContent = { NiaSwitch(checked = enabled, onCheckedChange = onToggle) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = a11yLabel
                },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
        if (enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NiaButton(onClick = onDecrement) {
                    Text(stringResource(R.string.settings_security_lock_timeout_decrease))
                }
                Text(
                    stringResource(
                        R.string.settings_security_lock_timeout_label,
                        timeoutMinutes.coerceIn(TIMEOUT_MIN, TIMEOUT_MAX),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                NiaButton(onClick = onIncrement) {
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
    val notifA11y = stringResource(
        if (granted) R.string.settings_notifications_status_allowed
        else R.string.settings_notifications_status_denied,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = notifA11y
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.mobile_top_notifications))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NiaButton(onClick = onAllow) {
                Text(stringResource(R.string.settings_notifications_allow))
            }
            NiaButton(onClick = onTest) {
                Text(stringResource(R.string.settings_notifications_test))
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
    ThemeMode.SYSTEM -> "Device"
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
            .build(),
    )
}
