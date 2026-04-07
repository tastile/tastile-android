package app.tastile.android.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExecutionAlarmRescheduleReceiver : BroadcastReceiver() {
    private val supportedActions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
    )

    @Inject
    lateinit var scheduler: ExecutionAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in supportedActions) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.rescheduleFromCurrentState()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
