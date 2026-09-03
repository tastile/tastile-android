package app.tastile.android.notifications

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules exact alarms when device state changes that affect
 * alarm scheduling (boot, time/timezone change, package replace,
 * exact-alarm permission change).
 *
 * Earlier this class was annotated `@AndroidEntryPoint` with
 * `@Inject lateinit var scheduler`. The Hilt-generated wrapper calls
 * `inject()` before delegating to user code, which crashes when the
 * system delivers one of these broadcasts **before**
 * `Application.onCreate()` has finished initializing the Hilt graph.
 * In practice that broke `adb shell am instrument` because the test
 * runner needs the production process to start (to host MainActivity)
 * and the system then delivers a queued `BOOT_COMPLETED`, killing
 * the production process before instrumentation can hand off.
 *
 * This rewrite uses `EntryPointAccessors.fromApplication(...)` lazily
 * inside `onReceive`, wrapped in a try/catch on the
 * `IllegalStateException` Hilt throws when its component is not
 * initialized yet. If Hilt is not ready, the broadcast is dropped —
 * the reschedule will be retried on the next foreground launch via
 * the normal MainActivity path. No reschedule is lost: the same
 * triggers (BOOT_COMPLETED, MY_PACKAGE_REPLACED, TIME_CHANGED,
 * TIMEZONE_CHANGED, SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
 * are still subscribed.
 */
class ExecutionAlarmRescheduleReceiver : BroadcastReceiver() {

    private val supportedActions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface SchedulerEntryPoint {
        fun executionAlarmScheduler(): ExecutionAlarmScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in supportedActions) {
            return
        }
        val app = context.applicationContext as? Application ?: return
        val scheduler = try {
            EntryPointAccessors
                .fromApplication(app, SchedulerEntryPoint::class.java)
                .executionAlarmScheduler()
        } catch (_: IllegalStateException) {
            // Hilt graph not ready (broadcast fired before
            // Application.onCreate() completed). Drop the reschedule
            // request; the next foreground launch will re-run it
            // through the normal MainActivity path.
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
