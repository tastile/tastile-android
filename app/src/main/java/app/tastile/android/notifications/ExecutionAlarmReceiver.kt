package app.tastile.android.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
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
import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.UserSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExecutionAlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var scheduler: ExecutionAlarmScheduler

    @Inject
    lateinit var coreRuntimeService: CoreRuntimeService

    @Inject
    lateinit var userSettingsRepository: UserSettingsRepository

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarmId = intent.getStringExtra(ExecutionAlarmScheduler.EXTRA_ALARM_ID).orEmpty()
                if (alarmId.isBlank()) return@launch
                if (!scheduler.isAlarmStillRelevant(alarmId)) return@launch

                if (canPostNotifications(context)) {
                    ExecutionNotificationChannels.ensure(context)
                    val type = intent.getStringExtra(ExecutionAlarmScheduler.EXTRA_TRIGGER_TYPE)
                        ?.let { rawType -> enumValues<AlarmTriggerType>().firstOrNull { it.name == rawType } }
                        ?: AlarmTriggerType.PROMPT
                    val tileTitle = intent.getStringExtra(ExecutionAlarmScheduler.EXTRA_TILE_TITLE).orEmpty()
                    val locale = userSettingsRepository.getLocale()
                    val content = snapshotPromptContent(locale)
                        ?: alarmContent(type, tileTitle, locale)
                    try {
                        NotificationManagerCompat.from(context).notify(
                            alarmId.hashCode(),
                            NotificationCompat.Builder(context, ExecutionNotificationChannels.ALERTS)
                                .setSmallIcon(R.drawable.ic_notification_tastile)
                                .setContentTitle(content.first)
                                .setContentText(content.second)
                                .setStyle(NotificationCompat.BigTextStyle().bigText(content.second))
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setCategory(NotificationCompat.CATEGORY_ALARM)
                                .setAutoCancel(true)
                                .setContentIntent(ExecutionNotificationIntents.openApp(context))
                                .build()
                        )
                    } catch (_: SecurityException) {
                        return@launch
                    }
                }
                scheduler.rescheduleFromCurrentState()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun alarmContent(type: AlarmTriggerType, tileTitle: String, locale: AppLocale): Pair<String, String> {
        val ja = locale == AppLocale.JA
        return when (type) {
            AlarmTriggerType.PROMPT -> if (ja) {
                "次の判断が必要です" to "「$tileTitle」に関する prompt を確認してください。"
            } else {
                "Decision required" to "Review the prompt for \"$tileTitle\"."
            }
            AlarmTriggerType.FIXED_START -> if (ja) {
                "予定時刻です" to "「$tileTitle」を開始する時刻です。"
            } else {
                "Scheduled start" to "It's time to start \"$tileTitle\"."
            }
        }
    }

    private fun snapshotPromptContent(locale: AppLocale): Pair<String, String>? {
        val snapshot = try {
            coreRuntimeService.currentSnapshot()
        } catch (_: CoreBridgeError) {
            return null
        }
        val prompt = snapshot.promptQueue.firstOrNull() ?: return null
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

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}

private object ExecutionNotificationIntents {
    fun openApp(context: Context) = android.app.PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )
}
