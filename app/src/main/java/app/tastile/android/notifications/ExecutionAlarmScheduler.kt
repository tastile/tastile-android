package app.tastile.android.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.getSystemService
import app.tastile.android.core.CoreBridgeError
import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.data.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Clock
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecutionAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val coreRuntimeService: CoreRuntimeService
) {
    private val alarmManager: AlarmManager by lazy { context.getSystemService()!! }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun rescheduleFromCurrentState() {
        val userId = authRepository.currentSession?.user?.id
        if (userId.isNullOrBlank()) {
            cancelAll()
            return
        }
        currentSnapshotOrNull()?.let(::reschedule) ?: cancelAll()
    }

    suspend fun isAlarmStillRelevant(alarmId: String): Boolean {
        authRepository.currentSession?.user?.id ?: return false
        val snapshot = currentSnapshotOrNull() ?: return false
        return ExecutionAlarmPlanner.plan(snapshot, Clock.System.now()).any { it.id == alarmId }
    }

    fun reschedule(snapshot: app.tastile.android.core.CoreSnapshot) {
        cancelAll()
        val alarms = ExecutionAlarmPlanner.plan(snapshot, Clock.System.now())
        if (alarms.isEmpty()) return
        alarms.forEach { schedule(it) }
        prefs.edit().putStringSet(KEY_SCHEDULED_ALARMS, alarms.map { it.id }.toSet()).apply()
    }

    fun cancelAll() {
        prefs.getStringSet(KEY_SCHEDULED_ALARMS, emptySet()).orEmpty().forEach { alarmId ->
            alarmManager.cancel(buildPendingIntent(createIntentPayload(alarmId, AlarmTriggerType.FIXED_START, "", "")))
        }
        prefs.edit().remove(KEY_SCHEDULED_ALARMS).apply()
    }

    @SuppressLint("MissingPermission")
    private fun schedule(spec: ScheduledAlarmSpec) {
        val pendingIntent = buildPendingIntent(createIntentPayload(spec.id, spec.type, spec.tileId, spec.tileTitle))
        val triggerAtMillis = spec.triggerAt.toEpochMilliseconds()
        val canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            false
        }
        val canUseExactAlarm = shouldUseExactAlarm(Build.VERSION.SDK_INT, canScheduleExactAlarms)
        if (canUseExactAlarm) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun createIntentPayload(alarmId: String, type: AlarmTriggerType, tileId: String, tileTitle: String): Intent {
        return Intent(context, ExecutionAlarmReceiver::class.java).apply {
            action = ACTION_EXECUTION_ALARM
            data = Uri.parse(alarmIntentUri(alarmId))
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_TRIGGER_TYPE, type.name)
            putExtra(EXTRA_TILE_ID, tileId)
            putExtra(EXTRA_TILE_TITLE, tileTitle)
        }
    }

    private fun buildPendingIntent(intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun currentSnapshotOrNull(): app.tastile.android.core.CoreSnapshot? {
        return try {
            coreRuntimeService.currentSnapshot()
        } catch (_: CoreBridgeError) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "tastile-exact-alarms"
        private const val KEY_SCHEDULED_ALARMS = "scheduled_alarm_ids"

        const val ACTION_EXECUTION_ALARM = "app.tastile.android.ACTION_EXECUTION_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_TRIGGER_TYPE = "extra_trigger_type"
        const val EXTRA_TILE_ID = "extra_tile_id"
        const val EXTRA_TILE_TITLE = "extra_tile_title"
    }
}

internal fun shouldUseExactAlarm(apiLevel: Int, canScheduleExactAlarms: Boolean): Boolean {
    return apiLevel < Build.VERSION_CODES.S || canScheduleExactAlarms
}

internal fun alarmIntentUri(alarmId: String): String {
    val encodedAlarmId = URLEncoder.encode(alarmId, StandardCharsets.UTF_8.name())
    return "tastile://execution-alarm/$encodedAlarmId"
}
