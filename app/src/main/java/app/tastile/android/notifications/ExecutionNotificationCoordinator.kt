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
import app.tastile.android.data.repository.AppLocale
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
    private val userSettingsRepository: UserSettingsRepository,
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

        val locale = userSettingsRepository.getLocale()
        val content = promptContent(prompt, locale)
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

    private fun promptContent(prompt: CorePromptQueueItem, locale: AppLocale): Pair<String, String> {
        val ja = locale == AppLocale.JA
        return when (prompt.kind) {
            "end_break" -> if (ja) {
                "休憩終了" to "休憩が終わりました。戻るか延長するか決めてください。"
            } else {
                "Break finished" to "The break is over. Decide whether to return or extend it."
            }
            "start_tile" -> if (ja) {
                "開始候補があります" to "次に始めるタイルを確認してください。"
            } else {
                "Tile ready to start" to "Review the next tile that should start now."
            }
            else -> if (ja) {
                "次の判断が必要です" to "進行中のタイルについて、完了・延長・中断を判断してください。"
            } else {
                "Decision required" to "Decide whether to complete, extend, or defer the current tile."
            }
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
