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

    companion object {
        private const val PREFS_NAME = "tastile-user-settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LOCALE = "locale"
    }
}

enum class ThemeMode(val value: String) {
    LIGHT("light"),
    GRAY("gray"),
    DARK("dark");

    companion object {
        fun from(value: String): ThemeMode = entries.firstOrNull { it.value == value } ?: DARK
    }
}

enum class AppLocale(val value: String) {
    JA("ja"),
    EN("en");

    companion object {
        fun from(value: String): AppLocale = entries.firstOrNull { it.value == value } ?: JA
    }
}
