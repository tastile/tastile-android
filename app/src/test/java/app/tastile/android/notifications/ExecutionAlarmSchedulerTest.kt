package app.tastile.android.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionAlarmSchedulerTest {

    @Test
    fun shouldUseExactAlarm_returnsTrueBelowAndroidS() {
        assertTrue(shouldUseExactAlarm(apiLevel = 30, canScheduleExactAlarms = false))
    }

    @Test
    fun shouldUseExactAlarm_requiresPermissionOnAndroidSAndAbove() {
        assertEquals(false, shouldUseExactAlarm(apiLevel = 31, canScheduleExactAlarms = false))
        assertEquals(true, shouldUseExactAlarm(apiLevel = 31, canScheduleExactAlarms = true))
    }

    @Test
    fun alarmIntentUri_isUniquePerAlarmId() {
        assertEquals("tastile://execution-alarm/alarm-a", alarmIntentUri("alarm-a"))
        assertEquals("tastile://execution-alarm/alarm-b", alarmIntentUri("alarm-b"))
    }
}
