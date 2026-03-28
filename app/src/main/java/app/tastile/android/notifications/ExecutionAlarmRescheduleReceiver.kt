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
    @Inject
    lateinit var scheduler: ExecutionAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.rescheduleFromCurrentState()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
