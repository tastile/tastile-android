package app.tastile.android.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.value) ?: ThemeMode.SYSTEM.value
        return ThemeMode.from(raw)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.value) }
    }

    fun getLocale(): AppLocale {
        val raw = prefs.getString(KEY_LOCALE, AppLocale.JA.value) ?: AppLocale.JA.value
        return AppLocale.from(raw)
    }

    fun setLocale(locale: AppLocale) {
        prefs.edit { putString(KEY_LOCALE, locale.value) }
    }

    fun getSecurityLockEnabled(): Boolean = prefs.getBoolean(KEY_SECURITY_LOCK_ENABLED, false)

    fun setSecurityLockEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SECURITY_LOCK_ENABLED, enabled) }
    }

    fun getSecurityLockTimeoutMinutes(): Int =
        prefs.getInt(KEY_SECURITY_LOCK_TIMEOUT_MINUTES, 10).coerceIn(1, 240)

    fun setSecurityLockTimeoutMinutes(minutes: Int) {
        prefs.edit { putInt(KEY_SECURITY_LOCK_TIMEOUT_MINUTES, minutes.coerceIn(1, 240)) }
    }

    fun recordSecurityLockLeftAt(nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit { putLong(KEY_SECURITY_LOCK_LEFT_AT_MILLIS, nowMillis) }
    }

    fun shouldRequireSecurityUnlock(nowMillis: Long = System.currentTimeMillis()): Boolean =
        SecurityLockPolicy.shouldRequireUnlock(
            enabled = getSecurityLockEnabled(),
            timeoutMinutes = getSecurityLockTimeoutMinutes(),
            lastLeftAtMillis = prefs.getLong(KEY_SECURITY_LOCK_LEFT_AT_MILLIS, 0L),
            nowMillis = nowMillis
        )

    /**
     * Schedule right-pane view mode (C11). Persists the user's last
     * choice between "Recurring Tiles" and "Upcoming Deadlines";
     * defaults to "recurring" when unset.
     */
    fun getScheduleView(): String =
        prefs.getString(KEY_SCHEDULE_VIEW, SCHEDULE_VIEW_DEFAULT) ?: SCHEDULE_VIEW_DEFAULT

    fun setScheduleView(view: String) {
        prefs.edit { putString(KEY_SCHEDULE_VIEW, view) }
    }

    /**
     * Material You dynamic color enabled (M5, Android 12+).
     * Defaults to `false` so first-run installs use the brand palette
     * until the user opts in via Settings → Preferences → Appearance.
     */
    fun getDynamicColor(): Boolean =
        prefs.getBoolean(KEY_DYNAMIC_COLOR, DYNAMIC_COLOR_DEFAULT)

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DYNAMIC_COLOR, enabled) }
    }

    companion object {
        private const val PREFS_NAME = "tastile-user-settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LOCALE = "locale"
        private const val KEY_SECURITY_LOCK_ENABLED = "security_lock_enabled"
        private const val KEY_SECURITY_LOCK_TIMEOUT_MINUTES = "security_lock_timeout_minutes"
        private const val KEY_SECURITY_LOCK_LEFT_AT_MILLIS = "security_lock_left_at_millis"
        private const val KEY_SCHEDULE_VIEW = "schedule_view"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val SCHEDULE_VIEW_DEFAULT = "recurring"
        private const val DYNAMIC_COLOR_DEFAULT = false
    }
}

object SecurityLockPolicy {
    fun shouldRequireUnlock(
        enabled: Boolean,
        timeoutMinutes: Int,
        lastLeftAtMillis: Long,
        nowMillis: Long
    ): Boolean {
        if (!enabled || lastLeftAtMillis <= 0L) return false
        val timeoutMillis = timeoutMinutes.coerceIn(1, 240) * 60_000L
        return nowMillis - lastLeftAtMillis >= timeoutMillis
    }
}

enum class ThemeMode(val value: String) {
    SYSTEM("system"),    // follow system: light or dark
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun from(value: String): ThemeMode = when (value) {
            SYSTEM.value -> SYSTEM
            LIGHT.value -> LIGHT
            DARK.value -> DARK
            // legacy "brand"/"gray" values now map to SYSTEM (forward compat for existing installs
            // that had BRAND or GRAY persisted before they were purged)
            "brand", "gray" -> SYSTEM
            else -> SYSTEM   // unknown persisted value → default to SYSTEM (new users, upgrades)
        }
    }
}

enum class AppLocale(val value: String) {
    JA("ja"),
    EN("en");

    companion object {
        fun from(value: String): AppLocale = entries.firstOrNull { it.value == value } ?: JA
    }
}
