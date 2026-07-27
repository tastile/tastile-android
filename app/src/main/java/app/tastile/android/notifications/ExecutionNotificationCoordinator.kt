package app.tastile.android.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tastile.android.MainActivity
import app.tastile.android.R
import app.tastile.android.core.CoreBridgeError
import app.tastile.android.core.CorePromptQueueItem
import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.UserSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecutionNotificationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val coreRuntimeService: CoreRuntimeService,
    private val alarmScheduler: ExecutionAlarmScheduler
) {
    fun start() {
        ExecutionNotificationChannels.ensure(context)
        syncOnce()
    }

    fun stop() {
        NotificationManagerCompat.from(context).cancel(ALERT_NOTIFICATION_ID)
        alarmScheduler.cancelAll()
    }

    fun syncOnce() {
        val userId = authRepository.currentUserId()
        if (userId.isNullOrBlank()) {
            NotificationManagerCompat.from(context).cancel(ALERT_NOTIFICATION_ID)
            return
        }

        val snapshot = currentSnapshotOrNull() ?: return
        alarmScheduler.reschedule(snapshot)

        val prompt = snapshot.promptQueue.firstOrNull() ?: return
        if (!canPostNotifications()) return

        val content = promptContent(prompt)
        try {
            NotificationManagerCompat.from(context).notify(
                ALERT_NOTIFICATION_ID,
                NotificationCompat.Builder(context, ExecutionNotificationChannels.ALERTS)
                    .setSmallIcon(R.drawable.ic_notification_tastile)
                    .setContentTitle(content.first)
                    .setContentText(content.second)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content.second))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(createOpenAppIntent())
                    .build()
            )
        } catch (_: SecurityException) {
            return
        }
    }

    private fun promptContent(prompt: CorePromptQueueItem): Pair<String, String> {
        // Locale-specific wording now flows through stringResource; the same resource key
        // resolves to the JA value in values-ja and to the EN value in the default locale.
        return when (prompt.kind) {
            "end_break" -> context.getString(R.string.notif_alarm_title_break_finished) to
                context.getString(R.string.notif_alarm_body_break_finished)
            "start_tile" -> context.getString(R.string.notif_alarm_title_tile_ready) to
                context.getString(R.string.notif_alarm_body_tile_ready)
            else -> context.getString(R.string.notif_alarm_title_decision_required) to
                context.getString(R.string.notif_alarm_body_decision_default)
        }
    }

    private fun currentSnapshotOrNull(): app.tastile.android.core.CoreSnapshot? {
        return try {
            coreRuntimeService.currentSnapshot()
        } catch (_: CoreBridgeError.LibraryLoadFailed) {
            null
        } catch (_: CoreBridgeError.NativeMethodUnavailable) {
            null
        } catch (_: CoreBridgeError.SnapshotParseFailed) {
            null
        }
    }

    private fun createOpenAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val ALERT_NOTIFICATION_ID = 402
    }
}
