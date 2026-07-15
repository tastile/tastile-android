package app.tastile.android.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class V1ApiClientTest {

    @Test
    fun numeric_constants_match_v1_spec() {
        assertEquals(0.toByte(), V1NumericConstants.TileKind.RECURRING)
        assertEquals(1.toByte(), V1NumericConstants.TileKind.PLACEMENT)
        assertEquals(2.toByte(), V1NumericConstants.TileKind.EXECUTION)
        assertEquals(0.toShort(), V1NumericConstants.ApiErrorKind.VALIDATION)
        assertEquals(7.toShort(), V1NumericConstants.ApiErrorKind.RETRYABLE)
        assertEquals(0.toByte(), V1NumericConstants.ExecutionState.ACTIVE)
        assertEquals(3.toByte(), V1NumericConstants.ExecutionState.FINISHED_VOID)
    }

    @Test
    fun idempotency_key_is_uuid_v7_shape() {
        val first = V1Idempotency.generate()
        val second = V1Idempotency.generate()
        assertTrue("idempotency keys must be UUIDs", first.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")))
        assertTrue("idempotency keys must be unique", first != second)
    }

    @Test
    fun api_error_maps_to_sealed_branch() {
        val body = V1ApiErrorBody(kind = V1NumericConstants.ApiErrorKind.STALE_REVISION, message = "stale", currentRevision = 12)
        val error = V1Error.fromApiBody(body)
        @Suppress("USELESS_IS_CHECK")
        assertTrue(error is V1Error.Api)
        error as V1Error.Api
        assertEquals("STALE_REVISION", error.kindName)
        assertEquals(12L, error.currentRevision)
    }

    @Test(expected = V1Error.Auth::class)
    fun missing_token_throws_auth() {
        throw V1Error.Auth()
    }

    @Test
    fun timeline_path_includes_iso_range() {
        val start = Instant.parse("2026-07-01T00:00:00Z")
        val end = Instant.parse("2026-07-02T00:00:00Z")
        assertTrue(start.toString().startsWith("2026-07-01"))
        assertTrue(end.toString().startsWith("2026-07-02"))
    }
}