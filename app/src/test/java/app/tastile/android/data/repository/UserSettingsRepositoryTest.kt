package app.tastile.android.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserSettingsRepositoryTest {

    private fun newRepo(): UserSettingsRepository =
        UserSettingsRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun setSecurityLockTimeout_clampsBelowOneToOne() {
        val repo = newRepo()
        repo.setSecurityLockTimeoutMinutes(0)
        assertEquals(1, repo.getSecurityLockTimeoutMinutes())
    }

    @Test
    fun setSecurityLockTimeout_clampsAbove240To240() {
        val repo = newRepo()
        repo.setSecurityLockTimeoutMinutes(300)
        assertEquals(240, repo.getSecurityLockTimeoutMinutes())
    }

    @Test
    fun setSecurityLockTimeout_passesThroughValueInRange() {
        val repo = newRepo()
        repo.setSecurityLockTimeoutMinutes(45)
        assertEquals(45, repo.getSecurityLockTimeoutMinutes())
    }
}