package app.tastile.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME, ThemeMode.DARK.value) ?: ThemeMode.DARK.value
        return ThemeMode.from(raw)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.value).apply()
    }

    fun getLocale(): AppLocale {
        val raw = prefs.getString(KEY_LOCALE, AppLocale.JA.value) ?: AppLocale.JA.value
        return AppLocale.from(raw)
    }

    fun setLocale(locale: AppLocale) {
        prefs.edit().putString(KEY_LOCALE, locale.value).apply()
    }

    fun getSecurityLockEnabled(): Boolean = prefs.getBoolean(KEY_SECURITY_LOCK_ENABLED, true)

    fun setSecurityLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURITY_LOCK_ENABLED, enabled).apply()
    }

    fun getSecurityLockTimeoutMinutes(): Int =
        prefs.getInt(KEY_SECURITY_LOCK_TIMEOUT_MINUTES, 10).coerceIn(1, 240)

    fun setSecurityLockTimeoutMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SECURITY_LOCK_TIMEOUT_MINUTES, minutes.coerceIn(1, 240)).apply()
    }

    fun recordSecurityLockLeftAt(nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_SECURITY_LOCK_LEFT_AT_MILLIS, nowMillis).apply()
    }

    fun shouldRequireSecurityUnlock(nowMillis: Long = System.currentTimeMillis()): Boolean =
        SecurityLockPolicy.shouldRequireUnlock(
            enabled = getSecurityLockEnabled(),
            timeoutMinutes = getSecurityLockTimeoutMinutes(),
            lastLeftAtMillis = prefs.getLong(KEY_SECURITY_LOCK_LEFT_AT_MILLIS, 0L),
            nowMillis = nowMillis
        )

    companion object {
        private const val PREFS_NAME = "tastile-user-settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LOCALE = "locale"
        private const val KEY_SECURITY_LOCK_ENABLED = "security_lock_enabled"
        private const val KEY_SECURITY_LOCK_TIMEOUT_MINUTES = "security_lock_timeout_minutes"
        private const val KEY_SECURITY_LOCK_LEFT_AT_MILLIS = "security_lock_left_at_millis"
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
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun from(value: String): ThemeMode = when (value) {
            LIGHT.value -> LIGHT
            DARK.value -> DARK
            else -> DARK
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
