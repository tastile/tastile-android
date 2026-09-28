package app.tastile.android.util

import app.tastile.android.data.api.AggregateRef
import app.tastile.android.data.api.CommandResponse
import app.tastile.android.data.api.V1ApiClient
import io.mockk.coEvery
import io.mockk.mockk
import java.util.concurrent.atomic.AtomicInteger

/**
 * Singleton fake network boundary for the QuickCreate gesture canary.
 *
 * Exactly ONE seam is faked: the [V1ApiClient] (network edge). Everything
 * above it — real Composable, real pointer dispatch, real `onClick`, real
 * [app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateSubmissionViewModel],
 * real `QuickCreateSubmissionDispatcher` — stays production. Faking any
 * higher (ViewModel mock, gateway直結) would hide the wiring failure class
 * this canary exists to catch.
 *
 * Implemented as a process-wide singleton (not per-test) because Hilt
 * `@TestInstallIn` modules are objects instantiated at graph-creation time,
 * before any `@Before` runs. Tests share it via [reset] + the monotonic
 * [createSourceTileCalls] counter (monotonic so `waitUntil` never flaps on
 * the ViewModel's post-success state reset).
 */
object QuickCreateCanaryBackend {
    val createSourceTileCalls = AtomicInteger(0)

    val client: V1ApiClient = mockk(relaxed = true)

    init {
        coEvery { client.createSourceTile(any()) } answers {
            createSourceTileCalls.incrementAndGet()
            CommandResponse(
                commandId = "cmd-canary",
                acceptedAt = "2026-09-15T00:00:00Z",
                aggregate = AggregateRef(1, "tile-canary"),
                aggregateMeta = null,
                result = 1,
            )
        }
    }

    fun reset() {
        createSourceTileCalls.set(0)
    }
}
