package app.tastile.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object ExecutionNotificationChannels {
    const val STATUS = "execution-status"
    const val ALERTS = "execution-alerts"

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
    }
}
