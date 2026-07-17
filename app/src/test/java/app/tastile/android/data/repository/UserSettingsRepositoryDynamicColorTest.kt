package app.tastile.android.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Persistence test for the M5 dynamic-color preference. Verifies the
 * `getDynamicColor` / `setDynamicColor` methods round-trip the
 * `dynamic_color` SharedPreferences key, that the default value
 * (`false`) kicks in when nothing has been written yet, and that
 * toggling back to `false` after `true` round-trips.
 *
 * Other agents (Task 6) may read this field from
 * [UserSettingsRepository]; this test only touches the dynamic_color
 * key and clears it in [setUp].
 */
@RunWith(AndroidJUnit4::class)
class UserSettingsRepositoryDynamicColorTest {

    private lateinit var repository: UserSettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Start each run from a clean dynamic_color value so defaults
        // and round-trips are observable.
        context.getSharedPreferences("tastile-user-settings", Context.MODE_PRIVATE)
            .edit()
            .remove("dynamic_color")
            .commit()
        repository = UserSettingsRepository(context)
    }

    @Test
    fun defaultsToFalseWhenUnset() {
        assertEquals(false, repository.getDynamicColor())
    }

    @Test
    fun roundTripsTrue() {
        repository.setDynamicColor(true)
        assertEquals(true, repository.getDynamicColor())
    }

    @Test
    fun roundTripsFalseAfterSwitch() {
        repository.setDynamicColor(true)
        repository.setDynamicColor(false)
        assertEquals(false, repository.getDynamicColor())
    }
}