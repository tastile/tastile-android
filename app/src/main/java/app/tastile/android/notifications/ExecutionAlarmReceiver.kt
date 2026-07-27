package app.tastile.android.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import app.tastile.android.R
import app.tastile.android.core.CoreBridgeError
import app.tastile.android.core.CoreRuntimeService
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

    // The manifest declares SCHEDULE_EXACT_ALARM (via AlarmManager.setExact*).
    // BroadcastReceivers do not get a runtime permission prompt; the
    // permission gate is the manifest entry, and lint follows the chain
    // through ExecutionAlarmScheduler.schedule() which carries the matching
    // @RequiresPermission.
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
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
                val content = snapshotPromptContent(context)
                    ?: alarmContent(context, type, tileTitle)
                ExecutionNotificationIntents.startAlarmActivity(context, content.first, content.second, alarmId.hashCode())
                scheduler.rescheduleFromCurrentState()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun alarmContent(context: Context, type: AlarmTriggerType, tileTitle: String): Pair<String, String> {
        // Locale-specific wording now flows through stringResource; the same resource key
        // resolves to the JA value in values-ja and to the EN value in the default locale,
        // so explicit AppLocale branching is no longer required here.
        return when (type) {
            AlarmTriggerType.PROMPT ->
                context.getString(R.string.notif_alarm_title_decision_required) to
                    context.getString(R.string.notif_alarm_body_decision_tile, tileTitle)
            AlarmTriggerType.FIXED_START ->
                context.getString(R.string.notif_alarm_title_scheduled_start) to
                    context.getString(R.string.notif_alarm_body_scheduled_start, tileTitle)
        }
    }

    private fun snapshotPromptContent(context: Context): Pair<String, String>? {
        val snapshot = try {
            coreRuntimeService.currentSnapshot()
        } catch (_: CoreBridgeError) {
            return null
        }
        val prompt = snapshot.promptQueue.firstOrNull() ?: return null
        return when (prompt.kind) {
            "end_break" -> context.getString(R.string.notif_alarm_title_break_finished) to
                context.getString(R.string.notif_alarm_body_break_finished)
            "start_tile" -> context.getString(R.string.notif_alarm_title_tile_ready) to
                context.getString(R.string.notif_alarm_body_tile_ready)
            else -> context.getString(R.string.notif_alarm_title_decision_required) to
                context.getString(R.string.notif_alarm_body_decision_default)
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
