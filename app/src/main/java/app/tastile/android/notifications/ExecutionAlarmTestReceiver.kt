package app.tastile.android.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ExecutionAlarmTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Tastile alarm"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "This is an alarm-style test notification from Tastile."
        val notificationId = intent.getIntExtra(
            ExecutionAlarmActivity.EXTRA_NOTIFICATION_ID,
            ExecutionAlarmActivity.DEFAULT_NOTIFICATION_ID
        )
        context.startActivity(
            Intent(context, ExecutionAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(ExecutionAlarmActivity.EXTRA_TITLE, title)
                putExtra(ExecutionAlarmActivity.EXTRA_BODY, body)
                putExtra(ExecutionAlarmActivity.EXTRA_NOTIFICATION_ID, notificationId)
            }
        )
    }

    companion object {
        const val EXTRA_TITLE = "extra_test_alarm_title"
        const val EXTRA_BODY = "extra_test_alarm_body"
        const val ACTION_TEST_ALARM = "app.tastile.android.ACTION_TEST_ALARM"
    }
}
