package app.tastile.android.ui.mobile.account

import app.tastile.android.data.repository.AccountProfile
import app.tastile.android.data.repository.AccountRepository
import app.tastile.android.data.repository.AccountTokenView
import app.tastile.android.data.repository.AccountTokenWithSecret
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * C7 — AccountViewModel state-transition tests.
 *
 * Covers the four observable transitions that the mobile sheets rely on:
 *
 *   - init block fires `loadProfile()` + `loadTokens()`
 *   - `sendEmailCode` / `verifyEmailCode` toggle `submitting`
 *   - `createToken` populates `created` with the raw secret
 *   - `revokeToken` reloads the list on success
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val mockRepository = mockk<AccountRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AccountViewModel(mockRepository)

    @Test
    fun `init loads profile and tokens`() = runTest {
        coEvery { mockRepository.loadProfile() } returns AccountProfile(
            username = "alice",
            sub = "sub-1",
            email = "alice@example.com",
            emailVerified = true,
            preferredUsername = null,
        )
        coEvery { mockRepository.listTokens() } returns listOf(
            AccountTokenView(
                id = "tk-1",
                displayName = "ci",
                tokenPrefix = "tk_abcd",
                createdAt = null,
                lastUsedAt = null,
                lastUsedPath = null,
                revokedAt = null,
            ),
        )

        val vm = viewModel()
        // UnconfinedTestDispatcher drains init synchronously.
        val profile = vm.profile.value.profile
        val tokens = vm.tokens.value.tokens

        assertNotNull(profile)
        assertEquals("alice@example.com", profile?.email)
        assertEquals(1, tokens.size)
        assertEquals("ci", tokens[0].displayName)
        coVerify(exactly = 1) { mockRepository.loadProfile() }
        coVerify(exactly = 1) { mockRepository.listTokens() }
    }

    @Test
    fun `loadProfile toggles loading and surfaces error on failure`() = runTest {
        coEvery { mockRepository.loadProfile() } throws IllegalStateException("http 502")
        coEvery { mockRepository.listTokens() } returns emptyList()

        val vm = viewModel()
        // Manually re-trigger so we can observe the failure transition.
        vm.loadProfile()

        val state = vm.profile.value
        assertFalse(state.loading)
        assertEquals("http 502", state.error)
    }

    @Test
    fun `sendEmailCode flips submitting then clears on success`() = runTest {
        coEvery { mockRepository.loadProfile() } returns AccountProfile(
            username = "u", sub = "s", email = "old@e", emailVerified = true, preferredUsername = null,
        )
        coEvery { mockRepository.listTokens() } returns emptyList()
        coEvery { mockRepository.startEmailChange("new@example.com") } returns Unit

        val vm = viewModel()
        vm.updatePendingEmail("new@example.com")
        vm.sendEmailCode()

        val state = vm.profile.value
        assertFalse(state.submitting)
        assertEquals(AccountViewModel.Notice.Success("code_sent"), state.notice)
        coVerify(exactly = 1) { mockRepository.startEmailChange("new@example.com") }
    }

    @Test
    fun `sendEmailCode noops when pendingEmail blank`() = runTest {
        coEvery { mockRepository.loadProfile() } returns AccountProfile(
            username = "u", sub = "s", email = "", emailVerified = false, preferredUsername = null,
        )
        coEvery { mockRepository.listTokens() } returns emptyList()

        val vm = viewModel()
        vm.updatePendingEmail("")
        vm.sendEmailCode()

        coVerify(exactly = 0) { mockRepository.startEmailChange(any()) }
    }

    @Test
    fun `verifyEmailCode surfaces error on bad code`() = runTest {
        coEvery { mockRepository.loadProfile() } returns AccountProfile(
            username = "u", sub = "s", email = "old@e", emailVerified = false, preferredUsername = null,
        )
        coEvery { mockRepository.listTokens() } returns emptyList()
        coEvery { mockRepository.verifyEmailChange("000000") } throws IllegalStateException("verify_failed")

        val vm = viewModel()
        vm.updateVerificationCode("000000")
        vm.verifyEmailCode()

        val state = vm.profile.value
        assertFalse(state.submitting)
        assertEquals(
            AccountViewModel.Notice.Error("verify_failed"),
            state.notice,
        )
    }

    @Test
    fun `createToken populates created with raw secret`() = runTest {
        coEvery { mockRepository.loadProfile() } returns AccountProfile(
            username = "u", sub = "s", email = "", emailVerified = false, preferredUsername = null,
        )
        coEvery { mockRepository.listTokens() } returns emptyList()
        coEvery { mockRepository.createToken("ci") } returns AccountTokenWithSecret(
            id = "tk-1",
            displayName = "ci",
            tokenPrefix = "tk_sec",
            createdAt = null,
            secret = "tk_secret_full",
        )

        val vm = viewModel()
        vm.createToken("ci")

        val state = vm.tokens.value
        assertNotNull(state.created)
        assertEquals("tk_secret_full", state.created?.secret)
        assertFalse(state.submitting)
    }

    @Test
    fun `revokeToken reloads list on success`() = runTest {
        coEvery { mockRepository.loadProfile() } returns AccountProfile(
            username = "u", sub = "s", email = "", emailVerified = false, preferredUsername = null,
        )
        // First call returns the live token; the reload after revoke returns it revoked.
        coEvery { mockRepository.listTokens() } returnsMany listOf(
            listOf(
                AccountTokenView(
                    id = "tk-1", displayName = "ci", tokenPrefix = "tk_a",
                    createdAt = null, lastUsedAt = null, lastUsedPath = null, revokedAt = null,
                ),
            ),
            listOf(
                AccountTokenView(
                    id = "tk-1", displayName = "ci", tokenPrefix = "tk_a",
                    createdAt = null, lastUsedAt = null, lastUsedPath = null,
                    revokedAt = "2026-07-15T00:00:00Z",
                ),
            ),
        )
        coEvery { mockRepository.revokeToken("tk-1") } returns Unit

        val vm = viewModel()
        assertEquals(1, vm.tokens.value.tokens.size)
        assertFalse(vm.tokens.value.tokens[0].isRevoked)

        vm.revokeToken("tk-1")

        val state = vm.tokens.value
        assertTrue(state.tokens[0].isRevoked)
        coVerify(exactly = 1) { mockRepository.revokeToken("tk-1") }
    }

    @Test
    fun `dismissCreatedToken clears created`() = runTest {
        coEvery { mockRepository.loadProfile() } returns AccountProfile(
            username = "u", sub = "s", email = "", emailVerified = false, preferredUsername = null,
        )
        coEvery { mockRepository.listTokens() } returns emptyList()
        coEvery { mockRepository.createToken("ci") } returns AccountTokenWithSecret(
            id = "tk-1", displayName = "ci", tokenPrefix = "tk_a",
            createdAt = null, secret = "tk_secret_full",
        )

        val vm = viewModel()
        vm.createToken("ci")
        assertNotNull(vm.tokens.value.created)

        vm.dismissCreatedToken()
        assertNull(vm.tokens.value.created)
    }
}

