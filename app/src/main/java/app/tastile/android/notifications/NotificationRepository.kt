package app.tastile.android.notifications

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Read-only source of pending notifications consumed by [NotificationsSheet].
 *
 * For this pass the repository owns an in-memory [MutableStateFlow] that
 * starts empty. Wiring it up to [ExecutionNotificationCoordinator] is deferred
 * to a later task — Task 17 only needs the sheet to render the empty state
 * and any items the repository exposes.
 */
@Singleton
class NotificationRepository @Inject constructor() {

    private val _pending = MutableStateFlow<List<NotificationItem>>(emptyList())
    val pending: StateFlow<List<NotificationItem>> = _pending.asStateFlow()
}