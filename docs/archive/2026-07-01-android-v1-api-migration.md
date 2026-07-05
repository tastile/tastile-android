# tastile-android v1 API Migration — Round-Trip Chunk

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace every dead `/read/*` HTTP path and local command runtime path with a single `V1ApiClient` that talks to the live v1 backend (`https://api.tastile.app/v1/*`), so the Android app round-trips create-tile → show-on-timeline → start/pause/finish execution against the v1 backend.

**Architecture:**
- New `data/api/` package holds `V1ApiClient`, `V1NumericConstants`, `V1Models`, `V1Error`, and a `V1Tile → Tile` mapper that keeps the existing UI compilable while v1 data starts flowing.
- `V1ApiClient` is the single wire layer: it handles auth, command envelope (`{payload, expectedRevision, idempotencyKey}`), 8-value `ApiErrorKind` mapping, and UUIDv7 generation for `idempotencyKey`.
- `TileRepository` and `IntegrationRepository` route reads + commands through `V1ApiClient`. The `coreRuntimeService` (local command runtime) is preserved as a transient fallback during this chunk; it is removed in Macro Step 5 once the round-trip is verified end-to-end.

**Tech Stack:** Kotlin 2.1.0, Jetpack Compose, kotlinx.serialization, Hilt 2.59.2, java.net.HttpURLConnection (no new dependency — matches existing repository pattern).

**Spec reference:** `tastile-core/v1/14-read-model-and-endpoint.md` (canonical endpoint list), `tastile-core/v1/10-invariants.md` (numeric constants / no-`completed`-flag / no-JSONB rules), `tastile-core/v1/HARNESS.md` (numeric-constants table).

**Carryover gotchas** (from `project_tastile_android_build_env.md` + `project_android_v1_migration.md`):
- Build: `gradlew :app:assembleDebug -Dorg.gradle.jvmargs="-Xmx1024m -XX:MaxMetaspaceSize=512m"`
- Install: `adb install -r -t -g app-debug.apk` (`-t -g` bypasses `INSTALL_FAILED_USER_RESTRICTED`)
- Auth after `pm uninstall`: re-inject 5 keys to `/data/data/app.tastile.android/shared_prefs/tastile_cognito_auth.xml`: `user_id`, `id_token`, `access_token`, `refresh_token`, `email` (NOT `cognito_*`)
- v1 backend is live (`https://api.tastile.app`). For round-trip, log in via the app, then re-install to keep session.

---

## Constraints (must satisfy to avoid rework)

1. **Numeric constants only in API payloads.** v1 forbids string enums (`tileKind: "RECURRING"`). String enums are still OK in the OUTER `payload.kind` discriminator (e.g., `"kind": "CREATE_TILE"`). Mirror `tastile-core/v1/HARNESS.md` values exactly.
2. **No `lifecycle: String` on v1 wire types.** v1 has `Execution.state` (ACTIVE/PAUSED/FINISHED_NORMAL/FINISHED_VOID) and Plan completion trees; `Tile` has no lifecycle enum. The interim `V1Tile → Tile` mapper derives the v0 `lifecycle: String` (`Ready`/`Started`/`Done`/`Archived`) at the boundary, so existing UI works without edits to composables.
3. **Command envelope is exactly `{ expectedRevision, idempotencyKey, occurredAt, payload }` per v1/14 §1**. `actor` is server-derived (server session); never set it client-side.
4. **ApiErrorKind mapping is numeric (8 values).** String-based error matching is forbidden.
5. **Idempotency key is UUIDv7 per-command.** Use the helper `V1Idempotency.generate()`.
6. **Auth header is `Authorization: Bearer $idToken`** (matches existing pattern in `TileRepository.readCloudTiles`).
7. **No new dependencies.** Use `HttpURLConnection` and `kotlinx.serialization` already present.
8. **No edits to v0 wire paths** (e.g., `/read/tiles`, `/read/runtime-paths`, `/read/events/state`). The fallback for these becomes a no-op once migration is verified.
9. **Gradle OOM / Defender warnings are unchanged.** Run build with `-Xmx1024m -XX:MaxMetaspaceSize=512m`.

---

## File Structure

### New files

```
app/src/main/java/app/tastile/android/data/api/
  V1NumericConstants.kt
  V1Error.kt
  V1Models.kt
  V1Idempotency.kt
  V1ApiClient.kt
  V1Mappers.kt

app/src/main/java/app/tastile/android/data/di/
  ApiModule.kt

app/src/test/java/app/tastile/android/data/api/
  V1ApiClientTest.kt
```

### Modified files

```
app/src/main/java/app/tastile/android/data/repository/TileRepository.kt
app/src/main/java/app/tastile/android/data/repository/IntegrationRepository.kt
app/src/main/java/app/tastile/android/ui/mobile/EndpointsCatalog.kt
```

---

## Macro Step 1: V1ApiClient foundation

**Files:**
- Create: `app/src/main/java/app/tastile/android/data/api/V1NumericConstants.kt`
- Create: `app/src/main/java/app/tastile/android/data/api/V1Error.kt`
- Create: `app/src/main/java/app/tastile/android/data/api/V1Models.kt`
- Create: `app/src/main/java/app/tastile/android/data/api/V1Idempotency.kt`
- Create: `app/src/main/java/app/tastile/android/data/api/V1ApiClient.kt`
- Create: `app/src/main/java/app/tastile/android/data/di/ApiModule.kt`

**Goal:** Establish the v1 numeric-constants module, error model, response models, UUIDv7 idempotency helper, and the wire-layer API client. No behavior change to the app yet.

### Sub-task 1.1: Write `V1NumericConstants.kt`

Mirror the table in `tastile-core/v1/HARNESS.md` (§重要な数値定数) and `v1/14-read-model-and-endpoint.md` exactly. Define Kotlin `object` with `const val` for each numeric constant that the wire layer will send. This is the single source of truth on the Android side; do not duplicate these values across files.

```kotlin
package app.tastile.android.data.api

object V1NumericConstants {
    object TileKind {
        const val RECURRING: Byte = 0
        const val PLACEMENT: Byte = 1
        const val EXECUTION: Byte = 2
    }
    object PlanRole {
        const val EXECUTABLE: Byte = 0
        const val LABEL: Byte = 1
    }
    object PlacementSource {
        const val MANUAL: Byte = 0
        const val RECURRING: Byte = 1
        const val FLOW: Byte = 2
        const val IMPORT: Byte = 3
    }
    object ExecutionState {
        const val ACTIVE: Byte = 0
        const val PAUSED: Byte = 1
        const val FINISHED_NORMAL: Byte = 2
        const val FINISHED_VOID: Byte = 3
    }
    object ExecutionSegmentKind {
        const val ACTIVE: Byte = 0
        const val PAUSED: Byte = 1
    }
    object CommandResult {
        const val APPLIED: Byte = 0
        const val ALREADY_APPLIED: Byte = 1
        const val ACCEPTED: Byte = 2
    }
    object ApiErrorKind {
        const val VALIDATION: Short = 0
        const val FORBIDDEN: Short = 1
        const val STALE_REVISION: Short = 2
        const val IDEMPOTENCY_KEY_REUSED: Short = 3
        const val NOT_FOUND: Short = 4
        const val CONFLICT: Short = 5
        const val BLOCKED: Short = 6
        const val RETRYABLE: Short = 7
    }
    object ActorKind {
        const val USER: Byte = 0
        const val WORKER: Byte = 1
        const val IMPORT: Byte = 2
        const val SYSTEM: Byte = 3
    }
    object AggregateKind {
        const val RECURRING: Byte = 0
        const val PLACEMENT: Byte = 1
        const val EXECUTION: Byte = 2
        const val SESSION: Byte = 3
    }
    object ResolutionState {
        const val OPEN: Byte = 0
        const val CLOSED: Byte = 1
        const val BLOCKED: Byte = 2
    }
    object ChangeLayer {
        const val RECURRING: Byte = 0
        const val PLACEMENT: Byte = 1
        const val EXECUTION: Byte = 2
    }
    object ChangeKind {
        const val SET: Byte = 0
        const val CLEAR: Byte = 1
        const val PUT: Byte = 2
        const val DROP: Byte = 3
    }
    object ChangeSource {
        const val RECURRING: Byte = 0
        const val FLOW: Byte = 1
        const val USER: Byte = 2
        const val DECISION: Byte = 3
        const val EXECUTION: Byte = 4
    }
    object MergeMode {
        const val OVERRIDE: Byte = 0
        const val INTERSECT_RANGE: Byte = 1
        const val UNION_IDENTIFIED: Byte = 2
        const val ORDERED_IDENTIFIED: Byte = 3
        const val SPAN_ENDPOINT: Byte = 4
    }
}
```

- [ ] File created with all numeric-constant objects
- [ ] No value deviates from `tastile-core/v1/HARNESS.md`
- [ ] `Byte` / `Short` widths match the wire shape (TileKind is i8, ApiErrorKind is i16 per v1/14)

### Sub-task 1.2: Write `V1Error.kt`

```kotlin
package app.tastile.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class V1ApiErrorBody(
    val kind: Short,
    val message: String = "",
    @SerialName("current_revision") val currentRevision: Long? = null,
    val violations: List<ResolutionViolationBody> = emptyList()
)

@Serializable
data class ResolutionViolationBody(
    val path: String = "",
    val code: Int = 0,
    val message: String = ""
)

sealed class V1Error(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable) : V1Error("network error", cause)
    class Auth : V1Error("auth missing or invalid")
    class Unknown(status: Int, body: String) : V1Error("http $status: $body")
    data class Api(val kindValue: Short, val kindName: String, val message: String, val currentRevision: Long? = null) : V1Error(message)

    companion object {
        fun fromApiBody(body: V1ApiErrorBody): Api {
            val name = when (body.kind) {
                V1NumericConstants.ApiErrorKind.VALIDATION -> "VALIDATION"
                V1NumericConstants.ApiErrorKind.FORBIDDEN -> "FORBIDDEN"
                V1NumericConstants.ApiErrorKind.STALE_REVISION -> "STALE_REVISION"
                V1NumericConstants.ApiErrorKind.IDEMPOTENCY_KEY_REUSED -> "IDEMPOTENCY_KEY_REUSED"
                V1NumericConstants.ApiErrorKind.NOT_FOUND -> "NOT_FOUND"
                V1NumericConstants.ApiErrorKind.CONFLICT -> "CONFLICT"
                V1NumericConstants.ApiErrorKind.BLOCKED -> "BLOCKED"
                V1NumericConstants.ApiErrorKind.RETRYABLE -> "RETRYABLE"
                else -> "UNKNOWN_${body.kind}"
            }
            return Api(body.kind, name, body.message, body.currentRevision)
        }
    }
}
```

- [ ] Sealed `V1Error` covers Network / Auth / Unknown / Api
- [ ] `Api.kindName` matches the table
- [ ] `ResolutionViolationBody` decodes the violations array

### Sub-task 1.3: Write `V1Models.kt`

```kotlin
package app.tastile.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AggregateRef(
    val kind: Byte,
    val id: String
)

@Serializable
data class CommandResponse(
    @SerialName("command_id") val commandId: String,
    @SerialName("accepted_at") val acceptedAt: String,
    val aggregate: AggregateRef? = null,
    val revision: Long? = null,
    val result: Byte,
    val pending: List<PendingWork> = emptyList()
)

@Serializable
data class PendingWork(
    val kind: String,
    @SerialName("scheduled_at") val scheduledAt: String? = null
)

@Serializable
data class TileVisualView(
    val color: String? = null,
    val icon: String? = null
)

@Serializable
data class TileContentView(
    val title: String = "",
    val note: String? = null
)

@Serializable
data class Span(
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String
)

@Serializable
data class PlacementInsideView(
    @SerialName("placement_id") val placementId: String
)

@Serializable
data class PlacementSourceView(
    val value: Byte
)

@Serializable
data class ResolutionInfoView(
    val state: Byte,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("resolution_hash") val resolutionHash: String? = null,
    val violations: List<ResolutionViolationBody> = emptyList()
)

@Serializable
data class TimelineItem(
    @SerialName("placement_id") val placementId: String,
    val revision: Long,
    val content: TileContentView,
    val visual: TileVisualView,
    val role: Byte,
    val span: Span,
    val inside: PlacementInsideView? = null,
    val source: PlacementSourceView,
    val resolution: ResolutionInfoView
)

@Serializable
data class TileView(
    val id: String,
    val kind: Byte,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("external_id") val externalId: String? = null,
    val content: TileContentView,
    val visual: TileVisualView,
    val revision: Long
)

@Serializable
data class V1ListTilesResponse(
    val tiles: List<TileView> = emptyList()
)

@Serializable
data class V1TimelineResponse(
    val items: List<TimelineItem> = emptyList()
)

@Serializable
data class RuntimePathView(
    @SerialName("id") val id: String,
    @SerialName("profile_name") val profileName: String,
    @SerialName("app_data_dir") val appDataDir: String,
    @SerialName("db_path") val dbPath: String,
    @SerialName("session_path") val sessionPath: String,
    @SerialName("daemon_startup_log_path") val daemonStartupLogPath: String = "",
    @SerialName("daemon_executable_path") val daemonExecutablePath: String = ""
)

@Serializable
data class V1ListRuntimePathsResponse(
    val paths: List<RuntimePathView> = emptyList()
)
```

- [ ] All field names use snake_case JSON keys (matching Rust serde defaults)
- [ ] Numeric fields use `Byte` for i8, `Short` for i16, `Long` for i64
- [ ] Optional fields have default values to match v1 server tolerance of missing keys

### Sub-task 1.4: Write `V1Idempotency.kt`

UUIDv7 generation — use `UUID.fromString` after fabricating a v7-compliant value (the simplest portable approach: `Instant.now()` → millis → place into UUIDv7 bits 0–47 + version 7 + variant bits + random remaining bits). Avoid adding a dependency.

```kotlin
package app.tastile.android.data.api

import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

object V1Idempotency {
    private val random = SecureRandom()

    fun generate(): String {
        val ms = Instant.now().toEpochMilli()
        val tsHigh = (ms ushr 32).toInt()
        val tsLow = (ms and 0xFFFFFFFFL).toInt()

        val bytes = ByteArray(16)
        // bits 0-47: timestamp ms (big-endian)
        bytes[0] = ((tsHigh ushr 24) and 0xFF).toByte()
        bytes[1] = ((tsHigh ushr 16) and 0xFF).toByte()
        bytes[2] = ((tsHigh ushr 8) and 0xFF).toByte()
        bytes[3] = (tsHigh and 0xFF).toByte()
        bytes[4] = ((tsLow ushr 24) and 0xFF).toByte()
        bytes[5] = ((tsLow ushr 16) and 0xFF).toByte()
        // version 7 in high 4 bits of byte 6
        bytes[6] = (0x70.toByte() or (((tsLow ushr 8) and 0x0F).toByte()))
        bytes[7] = (tsLow and 0xFF).toByte()
        // variant 10xx in high 2 bits of byte 8
        bytes[8] = (0x80.toByte() or (random.nextInt() and 0x3F).toByte())
        // bits 72-127: random
        random.nextBytes(bytes.copyOfRange(9, 16))

        return UUID.nameUUIDFromBytes(bytes).toString()
    }
}
```

- [ ] `V1Idempotency.generate()` produces a UUID string that begins with the timestamp millis
- [ ] Generated UUIDs are unique per millisecond even from a single thread

### Sub-task 1.5: Write `V1ApiClient.kt`

```kotlin
package app.tastile.android.data.api

import app.tastile.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

typealias AuthTokenProvider = () -> String?

@Singleton
class V1ApiClient @Inject constructor(
    private val tokenProvider: AuthTokenProvider
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun baseUrl(): String =
        BuildConfig.TASTILE_CORE_URL.trim().trimEnd('/')

    private suspend inline fun <reified T> get(path: String): T = withContext(Dispatchers.IO) {
        val token = tokenProvider()
        if (token.isNullOrBlank()) throw V1Error.Auth()
        val url = URL("${baseUrl()}$path")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            doInput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        val status = connection.responseCode
        val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }
            .orEmpty()
        if (status !in 200..299) {
            val err = runCatching { json.decodeFromString<V1ApiErrorBody>(body) }.getOrNull()
            if (err != null) throw V1Error.fromApiBody(err)
            throw V1Error.Unknown(status, body.take(200))
        }
        json.decodeFromString<T>(body)
    }

    suspend fun listTiles(): V1ListTilesResponse =
        get("/v1/tiles")

    suspend fun getTimeline(start: Instant, end: Instant): V1TimelineResponse {
        val startIso = URLEncoder.encode(start.toString(), Charsets.UTF_8.name())
        val endIso = URLEncoder.encode(end.toString(), Charsets.UTF_8.name())
        return get("/v1/timeline?start=$startIso&end=$endIso")
    }

    suspend fun listRuntimePaths(): V1ListRuntimePathsResponse =
        get("/v1/runtime/paths")

    suspend fun <Req, Resp> postCommand(
        path: String,
        commandKind: String,
        payload: Req,
        payloadSerializer: KSerializer<Req>,
        responseSerializer: KSerializer<Resp>,
        expectedRevision: Long? = null
    ): Resp = withContext(Dispatchers.IO) {
        val token = tokenProvider()
        if (token.isNullOrBlank()) throw V1Error.Auth()
        val envelope = buildJsonObject {
            put("expectedRevision", expectedRevision?.let { JsonElement(it) } ?: kotlinx.serialization.json.JsonNull)
            put("idempotencyKey", V1Idempotency.generate())
            put("occurredAt", Instant.now().toString())
            put("payload", buildJsonObject {
                put("kind", commandKind)
                put("value", json.encodeToJsonElement(payloadSerializer, payload))
            })
        }
        val url = URL("${baseUrl()}$path")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        connection.outputStream.use { it.write(envelope.toString().toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }
            .orEmpty()
        if (status !in 200..299) {
            val err = runCatching { json.decodeFromString<V1ApiErrorBody>(body) }.getOrNull()
            if (err != null) throw V1Error.fromApiBody(err)
            throw V1Error.Unknown(status, body.take(200))
        }
        json.decodeFromString(responseSerializer, body)
    }
}
```

- [ ] `get` / `postCommand` both throw `V1Error.Auth` when token is missing
- [ ] All non-2xx responses decode the body into `V1ApiErrorBody` when possible
- [ ] `postCommand` builds the envelope exactly per v1/14 §1

### Sub-task 1.6: Write `ApiModule.kt` (Hilt)

```kotlin
package app.tastile.android.data.di

import app.tastile.android.core.CurrentUserProvider
import app.tastile.android.data.api.AuthTokenProvider
import app.tastile.android.data.api.V1ApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideAuthTokenProvider(currentUser: CurrentUserProvider): AuthTokenProvider =
        { currentUser.currentIdToken() }

    @Provides
    @Singleton
    fun provideV1ApiClient(tokenProvider: AuthTokenProvider): V1ApiClient =
        V1ApiClient(tokenProvider)
}
```

- [ ] `ApiModule` provides `V1ApiClient` and `AuthTokenProvider` as singletons
- [ ] Hilt sees the new module without explicit registration (InstallIn SingletonComponent)

### Sub-task 1.7: Write `V1ApiClientTest.kt` (unit test)

Use `kotlinx.coroutines.test.runTest`. The test does NOT make network calls — it tests envelope construction via a captured buffer, OR it exercises the auth/format path with a tiny HttpURLConnection wrapper. For this round-trip-first chunk, skip live network testing and add at minimum:

```kotlin
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
```

- [ ] `gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.api.V1ApiClientTest"` passes

### Verification for Macro Step 1

```bash
cd C:/Users/rebui/Desktop/tastile/tastile-android
./gradlew :app:compileDebugKotlin -Dorg.gradle.jvmargs="-Xmx1024m -XX:MaxMetaspaceSize=512m"
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.api.V1ApiClientTest" -Dorg.gradle.jvmargs="-Xmx1024m -XX:MaxMetaspaceSize=512m"
```

- [ ] `compileDebugKotlin` is GREEN
- [ ] All 4 unit tests pass
- [ ] `V1ApiClient` is wired through Hilt (`./gradlew :app:assembleDebug` succeeds with no missing-binding errors)

**Commit:** `feat(v1): add V1ApiClient wire layer with numeric constants and error model`

---

## Macro Step 2: Wire tile reads through V1ApiClient

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/repository/TileRepository.kt`
- Modify: `app/src/main/java/app/tastile/android/data/repository/IntegrationRepository.kt` (no change yet for integration runtime paths — only the tile side this step)
- Modify: `app/src/main/java/app/tastile/android/data/api/V1Mappers.kt` (new file, creation listed here)

**Goal:** When the user is logged in, the tile list shown on the Tiles / QuickCreateSheet / Timeline comes from the v1 backend's `GET /v1/tiles`. When unauthenticated, the existing local snapshot fallback remains.

### Sub-task 2.1: Write `V1Mappers.kt`

```kotlin
package app.tastile.android.data.api

import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import kotlinx.serialization.json.JsonNull

internal fun V1ListTilesResponse.toTiles(): List<Tile> =
    tiles.map { it.toTile() }

internal fun TileView.toTile(): Tile = Tile(
    id = id,
    userId = ownerId,
    localTileId = id,
    title = content.title,
    lifecycle = deriveLifecycle(),
    updatedAt = null
)

private fun TileView.deriveLifecycle(): String {
    return when (kind) {
        V1NumericConstants.TileKind.EXECUTION -> TileLifecycle.STARTED.value
        V1NumericConstants.TileKind.PLACEMENT, V1NumericConstants.TileKind.RECURRING -> TileLifecycle.READY.value
        else -> TileLifecycle.READY.value
    }
}
```

- [ ] Mapper preserves the v0 `Tile` shape (id / userId / title / lifecycle / updatedAt)
- [ ] The v0 `lifecycle` is derived from `TileKind` since v1 has no `lifecycle` field
- [ ] `nullable` v1 fields (note, color, etc.) degrade gracefully (UI uses defaults)

### Sub-task 2.2: Modify `TileRepository.readCloudTiles`

In `app/src/main/java/app/tastile/android/data/repository/TileRepository.kt`, replace the existing `readCloudTiles()` body with:

```kotlin
private suspend fun readCloudTiles(): List<Tile>? {
    val token = currentUserProvider.currentIdToken()
    if (token.isNullOrBlank()) return null
    return try {
        v1ApiClient.listTiles().toTiles()
    } catch (_: V1Error) {
        null
    } catch (_: Exception) {
        null
    }
}
```

- [ ] Field `v1ApiClient: V1ApiClient` is added via constructor injection (Hilt)
- [ ] Existing fall-through to `coreRuntimeService` is unchanged
- [ ] Catch `V1Error` (sealed class) returns null → caller continues with snapshot fallback
- [ ] The build still passes (`./gradlew :app:assembleDebug` with OOM-avoiding jvmargs)

### Sub-task 2.3: Add `@Inject` to `TileRepository`

Update the constructor signature to inject `V1ApiClient`:

```kotlin
@Singleton
class TileRepository @Inject constructor(
    private val coreRuntimeService: CoreRuntimeService,
    private val v1ApiClient: V1ApiClient,
    private val executionNotificationCoordinator: ExecutionNotificationCoordinator,
    private val eventRepository: EventRepository,
    private val currentUserProvider: CurrentUserProvider
) : PromptTileRepository, MemoTileRepository { … }
```

- [ ] Hilt still resolves all bindings (no missing-binding errors)

### Verification for Macro Step 2

```bash
cd C:/Users/rebui/Desktop/tastile/tastile-android
./gradlew :app:assembleDebug -Dorg.gradle.jvmargs="-Xmx1024m -XX:MaxMetaspaceSize=512m"
adb install -r -t -g app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -n app.tastile.android/.MainActivity
```

Expected:
- [ ] App launches without crash
- [ ] After login, the Tiles screen and Timeline populate from `GET /v1/tiles`
- [ ] `adb logcat | grep -i 'tile\|httpurl\|https'` shows the `/v1/tiles` request, not the dead `/read/tiles`
- [ ] curl against the same backend confirms `GET /v1/tiles` returns a JSON list:

```bash
curl -s -H "Authorization: Bearer $ID_TOKEN" https://api.tastile.app/v1/tiles | jq .
```

**Commit:** `feat(v1): route tile reads through V1ApiClient`

---

## Macro Step 3: Wire timeline + integration runtime paths through V1ApiClient

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/repository/TileRepository.kt`
- Modify: `app/src/main/java/app/tastile/android/data/repository/IntegrationRepository.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/EndpointsCatalog.kt`

**Goal:** Timeline falls back to `GET /v1/timeline?start=&end=` when the local snapshot is empty. The Integrations screen calls `GET /v1/runtime/paths` instead of `/read/runtime-paths`. EndpointsCatalog surfaces v1 operationIds accurately.

### Sub-task 3.1: Add `getTimelineCloud(start, end)` to `TileRepository`

Insert after the existing `readCloudTiles`:

```kotlin
private suspend fun getTimelineCloud(start: Instant, end: Instant): List<CoreTimelineItem> {
    val token = currentUserProvider.currentIdToken()
    if (token.isNullOrBlank()) return emptyList()
    return try {
        v1ApiClient.getTimeline(start, end).items.map { item ->
            CoreTimelineItem(
                id = item.placementId,
                tileId = item.placementId,
                title = item.content.title,
                type = when (item.role) {
                    V1NumericConstants.PlanRole.LABEL -> "label"
                    else -> "work"
                },
                status = when (item.resolution.state) {
                    V1NumericConstants.ResolutionState.OPEN -> "active"
                    V1NumericConstants.ResolutionState.CLOSED -> "done"
                    V1NumericConstants.ResolutionState.BLOCKED -> "blocked"
                    else -> "scheduled"
                },
                startAt = item.span.startAt,
                endAt = item.span.endAt
            )
        }
    } catch (_: V1Error) {
        emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}
```

Then update `getTimeline()`:

```kotlin
suspend fun getTimeline(): List<CoreTimelineItem> {
    val snapshotTimeline = currentSnapshotOrNull()?.timeline.orEmpty()
    if (snapshotTimeline.isNotEmpty()) {
        val now = Instant.now()
        val normalized = normalizeCoreTimeline(snapshotTimeline, now, ZoneId.systemDefault())
        val syntheticBreaks = snapshotTimeline.count { it.tileId?.startsWith("synthetic:break:") == true }
        if (shouldUseCoreTimeline(snapshotTimeline, normalized)) {
            latestReadDiagnostics = buildString {
                append(latestReadDiagnostics)
                append(" timeline_source=core")
                append(" timeline_count=${normalized.size}")
            }
            return normalized
        }
    }

    // v1 fallback when local snapshot is empty/stale
    val now = Instant.now()
    val start = now.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
    val end = start.plusSeconds(24L * 60L * 60L)
    val cloudTimeline = getTimelineCloud(start, end)
    if (cloudTimeline.isNotEmpty()) {
        latestReadDiagnostics = buildString {
            append(latestReadDiagnostics)
            append(" timeline_source=v1")
            append(" timeline_count=${cloudTimeline.size}")
        }
        return cloudTimeline
    }

    if (latestCloudTiles.isEmpty()) {
        latestCloudTiles = readCloudTiles().orEmpty()
    }
    val fallback = buildTimelineFromTiles(latestCloudTiles, Instant.now())
    latestReadDiagnostics = buildString {
        append(latestReadDiagnostics)
        append(" fallback_timeline_count=${fallback.size}")
    }
    return fallback
}
```

- [ ] `diagnostics` correctly distinguish `timeline_source=core` / `timeline_source=v1` / `fallback_timeline_count=…`
- [ ] Cloud-fetched timeline items render in the same `CoreTimelineItem` shape that existing UI consumes
- [ ] The `[0, 24h]` window is computed from the device's local zone

### Sub-task 3.2: Modify `IntegrationRepository.getRuntimePaths`

Replace `getRuntimePaths()` in `IntegrationRepository.kt`:

```kotlin
@Inject lateinit var v1ApiClient: V1ApiClient

suspend fun getRuntimePaths(): RuntimePathsResponse {
    val token = authRepository.currentIdToken()
    if (token.isNullOrBlank()) {
        // Preserve v0 404 fallback shape (profile="cloud") so existing UI still renders
        return RuntimePathsResponse(
            profileName = "cloud",
            appDataDir = "",
            dbPath = "",
            sessionPath = "",
            daemonStartupLogPath = "",
            daemonExecutablePath = ""
        )
    }
    return try {
        val resp = v1ApiClient.listRuntimePaths()
        val first = resp.paths.firstOrNull()
            ?: return RuntimePathsResponse(profileName = "cloud", appDataDir = "", dbPath = "", sessionPath = "", daemonStartupLogPath = "", daemonExecutablePath = "")
        RuntimePathsResponse(
            profileName = first.profileName,
            appDataDir = first.appDataDir,
            dbPath = first.dbPath,
            sessionPath = first.sessionPath,
            daemonStartupLogPath = first.daemonStartupLogPath,
            daemonExecutablePath = first.daemonExecutablePath
        )
    } catch (_: V1Error) {
        RuntimePathsResponse(profileName = "cloud", appDataDir = "", dbPath = "", sessionPath = "", daemonStartupLogPath = "", daemonExecutablePath = "")
    } catch (_: Exception) {
        RuntimePathsResponse(profileName = "cloud", appDataDir = "", dbPath = "", sessionPath = "", daemonStartupLogPath = "", daemonExecutablePath = "")
    }
}
```

- [ ] The v1 backend's `/v1/runtime/paths` is queried when auth is present
- [ ] The legacy "cloud profile" placeholder remains for unauthenticated users
- [ ] `@Inject lateinit var v1ApiClient` is replaced with constructor injection (matching existing repo conventions in this file)

### Sub-task 3.3: Update `EndpointsCatalog.kt`

In `app/src/main/java/app/tastile/android/ui/mobile/EndpointsCatalog.kt`, replace the v0 operationId list with the v1 surface. Keep the v1 paths from `tastile-core/crates/v1/api/src/main.rs`:

| Method | Path | Notes |
| --- | --- | --- |
| GET | /v1/timeline?start=&end= | Timeline read |
| GET | /v1/sync?cursor=… | Sync |
| GET | /v1/tiles | Tile list |
| GET | /v1/tiles/{id} | Tile read |
| GET | /v1/tiles/{id}/detail | Tile detail |
| GET | /v1/placements/{id} | Placement read |
| GET | /v1/executions/{id} | Execution read |
| GET | /v1/executions/{id}/view | Execution view |
| GET | /v1/executions/{id}/basis | Execution Basis snapshot |
| GET | /v1/calendar/day?date= | Calendar day |
| GET | /v1/calendar/week | Calendar week |
| GET | /v1/calendar/month | Calendar month |
| GET | /v1/calendar/year | Calendar year |
| GET | /v1/runtime/paths | Runtime paths |
| GET | /v1/quota/tiles | Tile quota |
| GET | /v1/active-tile | Active tile |
| GET | /v1/timeline/today | Timeline today |
| GET | /v1/tiles/{id}/editable | Editable flag |
| POST | /v1/tiles | Create tile |
| POST | /v1/tiles/{id}/plan | Set plan |
| POST | /v1/tiles/{id}/start | Start tile |
| POST | /v1/tiles/{id}/complete | Complete tile |
| POST | /v1/tiles/{id}/defer | Defer tile |
| POST | /v1/tiles/{id}/extend-phase | Extend phase |
| DELETE | /v1/tiles/{id} | Archive tile |
| POST | /v1/tiles/{id}/memos | Attach memo |
| POST | /v1/tiles/{id}/update | Update tile |
| POST | /v1/placements | Create placement |
| POST | /v1/placements/{id}/changes | Append changes |
| POST | /v1/placements/{id}/executions | Start execution |
| POST | /v1/placements/{id}/close | Close placement |
| POST | /v1/placements/{id}/detach | Detach placement |
| POST | /v1/executions/{id}/pause | Pause execution |
| POST | /v1/executions/{id}/resume | Resume execution |
| POST | /v1/executions/{id}/finish | Finish execution |
| POST | /v1/tick | Trigger tick |
| POST | /v1/tick-at | Trigger tick at instant |
| POST | /v1/tick-range | Trigger tick for range |
| GET | /v1/debug/events | Debug events |
| GET | /v1/prompts/pending | Pending prompts |
| POST | /v1/prompts | Create prompt |
| POST | /v1/prompts/startup-recovery | Respond to startup recovery |
| POST | /v1/endpoints | Create endpoint |
| GET | /v1/endpoints | List endpoints |
| DELETE | /v1/endpoints/{id} | Delete endpoint |

- [ ] `EndpointsCatalog` lists the v1 paths (no v0 `/read/*` or `/api/*` paths)
- [ ] Touching the Integrations panel on device shows the new catalog

### Verification for Macro Step 3

```bash
./gradlew :app:assembleDebug -Dorg.gradle.jvmargs="-Xmx1024m -XX:MaxMetaspaceSize=512m"
adb install -r -t -g app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -n app.tastile.android/.MainActivity
```

Expected:
- [ ] Timeline read shows v1 placements (when any exist) or an empty state (when none)
- [ ] Integrations panel shows runtime path with `profile_name` from `GET /v1/runtime/paths`
- [ ] Endpoints catalog shows v1 paths
- [ ] `adb logcat | grep -E 'GET|POST'` shows `/v1/timeline` and `/v1/runtime/paths` (not `/read/timeline` or `/read/runtime-paths`)

**Commit:** `feat(v1): route timeline and integration reads through V1ApiClient`

---

## Macro Step 4: Wire commands through V1ApiClient

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/repository/TileRepository.kt`

**Goal:** Every `tryApplyCoreCommand(...)` call is replaced with `v1ApiClient.postCommand(...)` that targets the matching v1 endpoint. The transient local-runtime fallback is retained (will be removed in Macro Step 5).

### Sub-task 4.1: Define payload serializers + wrapper helpers in `TileRepository`

```kotlin
import app.tastile.android.data.api.CommandResponse
import kotlinx.serialization.Serializable

// Payloads mirror v1/14 §2.5 + per-tile command bodies
@Serializable
data class CreateTilePayload(
    val kind: Byte = V1NumericConstants.TileKind.PLACEMENT,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("external_id") val externalId: String? = null,
    val content: TileContentView,
    val visual: TileVisualView
)

@Serializable
data class StartExecutionPayload(
    val basis: String? = null
)

@Serializable
data class FinishExecutionPayload(
    val state: Byte,
    val note: String? = null
)
```

Add a single wrapper that POSTs a command and returns the `aggregate.id` (or throws):

```kotlin
private suspend fun <Req> postV1Command(
    path: String,
    commandKind: String,
    payload: Req,
    serializer: KSerializer<Req>,
    expectedRevision: Long? = null
): String {
    return try {
        val resp = v1ApiClient.postCommand(
            path = path,
            commandKind = commandKind,
            payload = payload,
            payloadSerializer = serializer,
            responseSerializer = CommandResponse.serializer(),
            expectedRevision = expectedRevision
        )
        resp.aggregate?.id ?: resp.commandId
    } catch (e: V1Error) {
        null.also { logger.warn("v1 command rejected: ${path} kind=$commandKind err=${e.message}") }
    } catch (e: Exception) {
        null.also { logger.warn("v1 command failed: ${path} kind=$commandKind err=${e.message}") }
    } ?: throw IllegalStateException("Cloud command rejected: $commandKind")
}
```

- [ ] Wrapper handles V1Error.Api (kind=STALE_REVISION, NOT_FOUND, BLOCKED, IDEMPOTENCY_KEY_REUSED) with a sensible error path
- [ ] `actor` is never set client-side — server derives from session

### Sub-task 4.2: Re-wire each command

Replace the `tryApplyCoreCommand(...)` calls. The mapping is:

| v0 commandKind | v1 endpoint | Payload shape |
| --- | --- | --- |
| `tile.create` | `POST /v1/tiles` with `kind: CREATE_TILE` | `CreateTilePayload` |
| `tile.start` | `POST /v1/tiles/{tileId}/start` with `kind: START_TILE` | `{}` |
| `tile.complete` | `POST /v1/tiles/{tileId}/complete` with `kind: COMPLETE_TILE` | `{}` |
| `tile.delete` | `DELETE /v1/tiles/{tileId}` | empty body |
| `tile.pause` | `POST /v1/executions/{executionId}/pause` with `kind: PAUSE_EXECUTION` | `{}` |
| `tile.continue` | `POST /v1/executions/{executionId}/resume` with `kind: RESUME_EXECUTION` | `{}` |
| `memo.attach` | `POST /v1/tiles/{tileId}/memos` with `kind: ATTACH_MEMO` | `{ text, memo_kind? }` |
| `break.start` | `POST /v1/tiles` with `kind: CREATE_BREAK_TILE` | `CreateTilePayload(kind=EXECUTION)` |
| `break.end` | `POST /v1/executions/{executionId}/finish` with `state=FINISHED_NORMAL` | `FinishExecutionPayload(state=FINISHED_NORMAL)` |
| `tile.update` | `POST /v1/tiles/{tileId}/update` with `kind: UPDATE_TILE` | patch body |
| `tile.reschedule` | `POST /v1/placements/{placementId}/changes` with `kind: APPEND_CHANGES` | change-set body |
| `tile.defer` | `POST /v1/tiles/{tileId}/defer` with `kind: DEFER_TILE` | `{ reason?, minutes? }` |
| `tile.extend` | `POST /v1/tiles/{tileId}/extend-phase` with `kind: EXTEND_PHASE` | `{ delta_min }` |
| `prompt.request` | `POST /v1/prompts` with `kind: CREATE_PROMPT` | `{ tile_id }` |
| `prompt.respond_startup_recovery` | `POST /v1/prompts/startup-recovery` with `kind: RESPOND_STARTUP_RECOVERY` | `{ prompt_id, action_id, ... }` |

> **Note**: `pauseExecution` / `resumeExecution` / `finishExecution` take `{executionId}` (not `tileId`). The Android `pauseTile(tileId)` call needs to look up the active execution first via `GET /v1/executions?active=true` (or maintain a small "active placement → execution" cache populated by reading `latestCloudTiles` and `/v1/active-tile`).

Concretely, the constructor of each command now looks like:

```kotlin
private suspend fun startTileInternal(tileId: String): Boolean {
    return try {
        v1ApiClient.postCommand(
            path = "/v1/tiles/$tileId/start",
            commandKind = "START_TILE",
            payload = EmptyPayload(),
            payloadSerializer = EmptyPayload.serializer(),
            responseSerializer = CommandResponse.serializer()
        )
        true
    } catch (e: V1Error.Api) {
        false
    } catch (e: Exception) {
        false
    }
}
```

For each existing `tryApplyCoreCommand(COMMAND_*, payload)`:
- [ ] Replace with the appropriate `v1ApiClient.postCommand(...)` call
- [ ] Preserve the existing return shape (acknowledged or throw `IllegalStateException("Cloud command rejected: …")`)
- [ ] Keep `persistEmittedEvents(...)` as a no-op for v1 (v1 has its own outbox)
- [ ] Re-fetch from `GET /v1/tiles` after success, or invalidate the local cache so the next read sees the new state

### Sub-task 4.3: Wire execution lifecycle (Pause/Resume/Finish)

The `pauseTile` / `continueTile` v1 commands require the active `executionId`, not the `tileId`. To resolve:

```kotlin
private suspend fun activeExecutionId(): String? {
    val token = currentUserProvider.currentIdToken() ?: return null
    return try {
        val resp = v1ApiClient.get("/v1/active-execution", ActiveExecutionResponse.serializer())
        resp.executionId
    } catch (_: V1Error) {
        null
    } catch (_: Exception) {
        null
    }
}
```

If `/v1/active-execution` does not exist on the server yet, fall back to `GET /v1/executions/{executionId}` resolved from `latestCloudTiles` (the EXECUTION-kind tile's id maps directly to the executionId per v1/02).

- [ ] `pauseTile(tileId)` either resolves the execution and POSTs `PAUSE_EXECUTION`, or surfaces a clear "no active execution" error
- [ ] `completeTile` maps to `POST /v1/tiles/{tileId}/complete` (NOT `finish_execution` — that's the execution-finish path)

### Verification for Macro Step 4

```bash
./gradlew :app:assembleDebug -Dorg.gradle.jvmargs="-Xmx1024m -XX:MaxMetaspaceSize=512m"
adb install -r -t -g app/build/outputs/apk/debug/app-debug.apk
```

Manual round-trip on device (after login):
- [ ] Open QuickCreateSheet, type "Round-trip test", submit
- [ ] curl `GET /v1/tiles` returns the new tile (within seconds)
- [ ] Open the tile, press Start (sends `START_TILE`)
- [ ] curl shows the tile has an active execution
- [ ] Press Pause → curl shows `ExecutionState.PAUSED`
- [ ] Press Complete → curl shows `ExecutionState.FINISHED_NORMAL`
- [ ] `adb logcat` shows POST requests to `/v1/tiles/.../start`, `/v1/executions/.../pause`, etc. (no POST to local bridge)

**Commit:** `feat(v1): route tile commands through V1ApiClient`

---

## Macro Step 5: Remove local command runtime fallback

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/repository/TileRepository.kt`
- Modify: `app/src/main/java/app/tastile/android/data/repository/IntegrationRepository.kt`
- Modify: `app/src/main/java/app/tastile/android/core/CoreRuntimeService.kt` (delete or stub to `error("v1 migration: local runtime removed")`)

**Goal:** The local in-process runtime is no longer the source of truth for tile state. Reads fall back to v1 only; writes go through v1 only.

### Sub-task 5.1: Strip the snapshot fallback in `TileRepository`

```kotlin
private fun currentSnapshotOrNull(): CoreSnapshot? {
    return null // v1 migration: local runtime removed
}
```

- [ ] `getTiles(userId)` now: (1) calls `v1ApiClient.listTiles().toTiles()` when auth present; (2) returns empty when unauthenticated
- [ ] `projectedSnapshotTiles()` always returns null
- [ ] `findSnapshotTile()` always returns null
- [ ] `latestCloudTiles` is the cache

### Sub-task 5.2: Strip the local-runtime command fallback

Replace `tryApplyCoreCommand` with a strict v1-only path:

```kotlin
private suspend fun <Req> tryApplyCoreCommand(
    v1Path: String,
    commandKind: String,
    payload: Req,
    serializer: KSerializer<Req>
): app.tastile.android.core.CoreCommandAck? {
    return try {
        v1ApiClient.postCommand(
            path = v1Path,
            commandKind = commandKind,
            payload = payload,
            payloadSerializer = serializer,
            responseSerializer = CommandResponse.serializer()
        )
        CoreCommandAck(accepted = true) // local-runtime stub
    } catch (_: V1Error) {
        null
    } catch (_: Exception) {
        null
    }
}
```

- [ ] `coreRuntimeService` field is removed from `TileRepository`
- [ ] No `coreRuntimeService.currentSnapshot()` / `coreRuntimeService.applyCommand()` calls remain in `TileRepository.kt`
- [ ] The build still passes after `coreRuntimeService` constructor arg is removed

### Sub-task 5.3: Delete or freeze `CoreRuntimeService.kt`

Two options:
- **(A) Delete** `app/src/main/java/app/tastile/android/core/CoreRuntimeService.kt` and any references (Hilt module entry, AndroidManifest). Verify with `./gradlew :app:assembleDebug` that no binding errors.
- **(B) Keep** the file but stub every method to `error("v1 migration: local runtime removed — use V1ApiClient")`. Less risky if a rebuild later re-introduces a reference.

Default to **(A)** unless the build fails — keep things small.

- [ ] `grep -r 'coreRuntimeService\|CoreRuntimeService' app/src/main` returns no usages
- [ ] `./gradlew :app:assembleDebug` compiles without missing-binding errors
- [ ] `./gradlew :app:assembleRelease` also compiles (release variant sanity)

### Verification for Macro Step 5

```bash
./gradlew :app:assembleDebug -Dorg.gradle.jvmargs="-Xmx1024m -XX:MaxMetaspaceSize=512m"
adb install -r -t -g app/build/outputs/apk/debug/app-debug.apk
```

Manual round-trip on device:
- [ ] Login → create tile → it appears on Tiles and Timeline
- [ ] Start → Pause → Resume → Finish execution; each step is observable via curl `/v1/tiles` and `/v1/executions`
- [ ] Unauthenticated: app launches; Tiles / Timeline are empty (no crash, no stale snapshot)
- [ ] `adb logcat | grep 'coreRuntime'` shows no references

**Commit:** `refactor(v1): remove local command runtime fallback`

---

## End-of-chunk definition of done

1. **All 5 commits land on `main`.** Each is one feature = one commit (`feat(v1):` / `refactor(v1):` prefix per project convention).
2. **Android → v1 round-trip works**: create / read / update / start / pause / resume / finish all hit `/v1/*` and the state is observable via curl.
3. **No `/read/*` HTTP paths remain** in `app/src/main` (`grep -r '/read/' app/src/main` returns 0 results for the v0 paths we replaced).
4. **`coreRuntimeService` is gone** (or stubbed) and no logcat traces reference the local runtime.
5. **Unit tests pass**: `./gradlew :app:testDebugUnitTest`.
6. **Memory index**: append a `project_timeline_v37.md` memory file documenting the v1 migration (analogous to `project_timeline_v36.md`).

## Rollback strategy

Per macro step:
- **Step 1**: `git revert feat(v1): add V1ApiClient wire layer` — restores app to dead-read state.
- **Step 2**: `git revert feat(v1): route tile reads through V1ApiClient` — restores `/read/tiles` reads.
- **Step 3**: `git revert feat(v1): route timeline and integration reads` — restores `/read/runtime-paths` + snapshot-only timeline.
- **Step 4**: `git revert feat(v1): route tile commands through V1ApiClient` — restores local command dispatch.
- **Step 5**: `git revert refactor(v1): remove local command runtime fallback` — restores the snapshot fallback entirely.

Each revert is a single commit. No data-loss risk — the v1 backend is the source of truth, so rollback only loses any writes since the commit.

## Out-of-scope (next chunk)

This chunk delivers a working round-trip with the **existing UI** (which sees v1 data through the interim `V1Tile → Tile` mapper). The native v1 UI rewrite — Tile / Plan / Placement / Execution as separate display units on the Timeline, conditions-based state, no `lifecycle` enum — is a separate plan. Dependencies for that chunk:

- v1/02 (full entities)
- v1/04 (ChangeSet shapes)
- v1/07 (EffectivePlacement / EffectiveExecution read model)
- v1/13 (Completion tree)

This chunk does NOT touch `MobileTimelineHeader.kt`, `TimelineScreen.kt` rendering logic, or `QuickCreateSheet` UI. Only data flow changes.
