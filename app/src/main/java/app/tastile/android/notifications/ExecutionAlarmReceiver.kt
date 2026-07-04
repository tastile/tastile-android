package app.tastile.android.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

                val type = intent.getStringExtra(ExecutionAlarmScheduler.EXTRA_TRIGGER_TYPE)
                    ?.let { rawType -> enumValues<AlarmTriggerType>().firstOrNull { it.name == rawType } }
                    ?: AlarmTriggerType.PROMPT
                val tileTitle = intent.getStringExtra(ExecutionAlarmScheduler.EXTRA_TILE_TITLE).orEmpty()
                val locale = userSettingsRepository.getLocale()
                val content = snapshotPromptContent(locale)
                    ?: alarmContent(type, tileTitle, locale)
                ExecutionNotificationIntents.startAlarmActivity(context, content.first, content.second, alarmId.hashCode())
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

}

private object ExecutionNotificationIntents {
    fun openAlarm(context: Context, title: String, body: String, notificationId: Int) = PendingIntent.getActivity(
        context,
        notificationId,
        alarmActivityIntent(context, title, body, notificationId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun startAlarmActivity(context: Context, title: String, body: String, notificationId: Int) {
        runCatching { context.startActivity(alarmActivityIntent(context, title, body, notificationId)) }
    }

    private fun alarmActivityIntent(context: Context, title: String, body: String, notificationId: Int): Intent {
        return Intent(context, ExecutionAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(ExecutionAlarmActivity.EXTRA_TITLE, title)
            putExtra(ExecutionAlarmActivity.EXTRA_BODY, body)
            putExtra(ExecutionAlarmActivity.EXTRA_NOTIFICATION_ID, notificationId)
        }
    }
}
