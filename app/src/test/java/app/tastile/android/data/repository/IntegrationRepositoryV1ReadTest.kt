package app.tastile.android.data.repository

import app.tastile.android.data.api.RuntimePathView
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1Error
import app.tastile.android.data.api.V1ListRuntimePathsResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class IntegrationRepositoryV1ReadTest {

    @Test
    fun getRuntimePaths_callsV1ApiClientAndMapsFirstPath() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.listRuntimePaths() } returns V1ListRuntimePathsResponse(
            paths = listOf(
                RuntimePathView(
                    id = "p-1",
                    profileName = "cloud-a",
                    appDataDir = "/data/a",
                    dbPath = "/data/a/db",
                    sessionPath = "/data/a/session",
                    daemonStartupLogPath = "/data/a/startup.log",
                    daemonExecutablePath = "/data/a/daemon"
                ),
                RuntimePathView(
                    id = "p-2",
                    profileName = "cloud-b",
                    appDataDir = "/data/b",
                    dbPath = "/data/b/db",
                    sessionPath = "/data/b/session",
                    daemonStartupLogPath = "/data/b/startup.log",
                    daemonExecutablePath = "/data/b/daemon"
                )
            )
        )
        val repository = IntegrationRepository(
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentIdToken() } returns "token-abc"
            },
            v1ApiClient = apiClient
        )

        val paths = repository.getRuntimePaths()

        coVerify(exactly = 1) { apiClient.listRuntimePaths() }
        assertEquals("cloud-a", paths.profileName)
        assertEquals("/data/a", paths.appDataDir)
        assertEquals("/data/a/db", paths.dbPath)
        assertEquals("/data/a/session", paths.sessionPath)
        assertEquals("/data/a/startup.log", paths.daemonStartupLogPath)
        assertEquals("/data/a/daemon", paths.daemonExecutablePath)
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1 "))
    }

    @Test
    fun getRuntimePaths_returnsEmptyDefaultWhenV1Throws_networkError() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.listRuntimePaths() } throws V1Error.Network(IOException("boom"))
        val repository = IntegrationRepository(
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentIdToken() } returns "token-abc"
            },
            v1ApiClient = apiClient
        )

        val paths = repository.getRuntimePaths()

        assertEquals("cloud", paths.profileName)
        assertEquals("", paths.appDataDir)
        assertEquals("", paths.dbPath)
        assertEquals("", paths.sessionPath)
        assertEquals("", paths.daemonStartupLogPath)
        assertEquals("", paths.daemonExecutablePath)
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1_unavailable"))
    }

    @Test
    fun getRuntimePaths_returnsEmptyDefaultWhenV1Throws_authError() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.listRuntimePaths() } throws V1Error.Auth()
        val repository = IntegrationRepository(
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentIdToken() } returns "token-abc"
            },
            v1ApiClient = apiClient
        )

        val paths = repository.getRuntimePaths()

        assertEquals("cloud", paths.profileName)
        assertEquals("", paths.appDataDir)
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1_unavailable"))
    }

    @Test
    fun getRuntimePaths_skipsV1WhenNoToken() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val repository = IntegrationRepository(
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentIdToken() } returns null
            },
            v1ApiClient = apiClient
        )

        val paths = repository.getRuntimePaths()

        coVerify(exactly = 0) { apiClient.listRuntimePaths() }
        assertEquals("cloud", paths.profileName)
        assertEquals("", paths.appDataDir)
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1_skipped reason=no_token"))
    }

    @Test
    fun getRuntimePaths_preservesV1SuccessDiagnostic() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.listRuntimePaths() } returns V1ListRuntimePathsResponse(
            paths = listOf(
                RuntimePathView(
                    id = "p-1",
                    profileName = "cloud-a",
                    appDataDir = "/data/a",
                    dbPath = "/data/a/db",
                    sessionPath = "/data/a/session"
                )
            )
        )
        val repository = IntegrationRepository(
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentIdToken() } returns "token-abc"
            },
            v1ApiClient = apiClient
        )

        repository.getRuntimePaths()
        // success diagnostic preserved exactly (no overwrite by outer caller)
        val diagnostic = repository.latestReadDiagnostics()
        assertTrue(
            "expected source=v1 success diagnostic, got: $diagnostic",
            diagnostic.startsWith("source=v1 ")
        )
        assertTrue(
            "expected count=1 in diagnostic, got: $diagnostic",
            diagnostic.contains("count=1")
        )
        assertTrue(
            "expected user_match=true in diagnostic, got: $diagnostic",
            diagnostic.contains("user_match=true")
        )
    }
}