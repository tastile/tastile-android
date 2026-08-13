package app.tastile.android.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.tastile.android.MainActivity
import app.tastile.android.R
import app.tastile.android.data.notification.PushEndpointRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Receives provider-translated FCM data notifications for Core decision sessions. */
@AndroidEntryPoint
class TastileFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var endpoints: PushEndpointRepository

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { endpoints.register(token) }.onFailure(Throwable::printStackTrace)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val title = message.data["title"] ?: message.notification?.title ?: getString(R.string.app_name)
        val body = message.data["body"] ?: message.notification?.body.orEmpty()
        val sessionId = message.data["session_id"].orEmpty()
        val openApp = PendingIntent.getActivity(
            this,
            sessionId.hashCode(),
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_SESSION_ID, sessionId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        ExecutionNotificationChannels.ensure(this)
        val notification = Notification.Builder(this, ExecutionNotificationChannels.ALERTS)
            .setSmallIcon(R.drawable.ic_notification_tastile)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(sessionId.hashCode(), notification)
    }

    companion object {
        const val EXTRA_SESSION_ID = "app.tastile.android.extra.SESSION_ID"
    }
}
