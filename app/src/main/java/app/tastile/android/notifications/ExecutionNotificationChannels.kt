package app.tastile.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager

object ExecutionNotificationChannels {
    const val STATUS = "execution-status"
    const val ALERTS = "execution-alerts"
    const val ALARMS = "execution-alarms-v2"

    fun ensure(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                STATUS,
                "Execution status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the current active execution without making noise."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALERTS,
                "Execution prompts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Intervention prompts when Tastile needs a human decision."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALARMS,
                "Execution alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm-style prompts that need immediate attention."
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
            }
        )
    }
}
