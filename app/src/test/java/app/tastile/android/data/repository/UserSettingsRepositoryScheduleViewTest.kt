package app.tastile.android.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Persistence test for the C11 schedule view toggle. Verifies the new
 * `getScheduleView` / `setScheduleView` methods round-trip the
 * `schedule_view` SharedPreferences key, and that the default value
 * kicks in when nothing has been written yet.
 *
 * Other agents (C9) may append additional SharedPreferences keys to
 * [UserSettingsRepository] for their own features; this test only
 * touches the schedule_view key and clears it in [setUp].
 */
@RunWith(AndroidJUnit4::class)
class UserSettingsRepositoryScheduleViewTest {

    private lateinit var repository: UserSettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Start each run from a clean schedule_view value so defaults
        // and round-trips are observable.
        context.getSharedPreferences("tastile-user-settings", Context.MODE_PRIVATE)
            .edit()
            .remove("schedule_view")
            .commit()
        repository = UserSettingsRepository(context)
    }

    @Test
    fun defaultsToRecurringWhenUnset() {
        assertEquals("recurring", repository.getScheduleView())
    }

    @Test
    fun roundTripsUpcomingValue() {
        repository.setScheduleView("upcoming")
        assertEquals("upcoming", repository.getScheduleView())
    }

    @Test
    fun roundTripsRecurringAfterSwitch() {
        repository.setScheduleView("upcoming")
        repository.setScheduleView("recurring")
        assertEquals("recurring", repository.getScheduleView())
    }
}
