package app.tastile.android.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityLockPolicyTest {
    @Test
    fun requiresUnlockWhenElapsedPastTimeout() {
        assertTrue(
            SecurityLockPolicy.shouldRequireUnlock(
                enabled = true,
                timeoutMinutes = 10,
                lastLeftAtMillis = 1_000L,
                nowMillis = 601_000L
            )
        )
    }

    @Test
    fun skipsWhenDisabled() {
        assertFalse(
            SecurityLockPolicy.shouldRequireUnlock(
                enabled = false,
                timeoutMinutes = 10,
                lastLeftAtMillis = 1_000L,
                nowMillis = 601_000L
            )
        )
    }
}
