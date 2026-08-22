package app.tastile.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import app.tastile.android.R

object ExecutionNotificationChannels {
    const val STATUS = "execution-status"
    const val ALERTS = "execution-alerts"
    const val ALARMS = "execution-alarms-v2"

    fun ensure(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                STATUS,
                context.getString(R.string.channel_status_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_status_description)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALERTS,
                context.getString(R.string.channel_prompts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_prompts_description)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALARMS,
                context.getString(R.string.channel_alarms_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_alarms_description)
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
