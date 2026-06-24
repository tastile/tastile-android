package app.tastile.android.notifications

/**
 * Display payload for a pending notification surfaced in [NotificationsSheet].
 *
 * The repository contract only requires a human-readable [label] for now.
 * Future fields (timestamp, trigger time, delivery history) can be added here
 * without breaking the sheet signature.
 */
data class NotificationItem(val label: String)