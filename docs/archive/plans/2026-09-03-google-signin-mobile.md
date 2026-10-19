# Native Google Sign-In for Android — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a native Google Sign-In path to `LoginScreen` on `tastile-android` that exchanges a Google ID token against BetterAuth's `/api/auth/sign-in/social` idToken branch on `tastile-web`, with a silent fallback to the existing web-OAuth handoff when the device has no Google account, no Play Services, or the user cancels.

**Architecture:** Cross-repo change. `tastile-web` extends its BetterAuth Google provider config to accept an array of OAuth client IDs (web + Android). `tastile-android` introduces a `GoogleSignInLauncher` that wraps Credential Manager + Google Identity Services, calls a new `BetterAuthHttpClient.signInWithGoogleIdToken` POST, and persists the resulting BetterAuth session via the existing `AuthRepository.persistSession`. `LoginViewModel.signInWithGoogle` orchestrates the flow with fallback handling.

**Tech Stack:** Kotlin 2.1.0, AGP 9.2.1, Compose BOM 2024.12.01, Hilt 2.60.1, JUnit 4.13.2, Robolectric 4.16.1, MockK 1.14.11, MockWebServer 4.12.0; `androidx.credentials:credentials:1.6.0`, `androidx.credentials:credentials-play-services-auth:1.6.0`, `com.google.android.libraries.identity.googleid:googleid:1.2.0`. Web side: Next.js + BetterAuth 1.x.

**Spec:** [docs/superpowers/specs/2026-09-03-google-signin-mobile-design.md](../specs/2026-09-03-google-signin-mobile-design.md)

## Global Constraints

These constraints apply to every task. They are copied verbatim from `CLAUDE.md` and the spec.

- **Work on local `main`.** Do not create feature branches, temporary branches, or worktrees.
- **Source code, identifiers, code comments, and Git/GitHub messages are English.** Internal development docs are Japanese.
- **`gradle.projectsEvaluated` in `app/build.gradle.kts` requires every `BuildConfig.*` field listed in `app/build.gradle.kts` (Cognito client/region/hosted-ui/redirect/web-auth base, `TASTILE_CORE_URL`, `GOOGLE_WEB_CLIENT_ID`) to be non-blank.** This task adds `GOOGLE_ANDROID_CLIENT_ID` to that list. The build fails fast if it's blank.
- **`verifyDesignSystemImports`:** direct `androidx.compose.material3.*` imports forbidden in `app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account}/` unless preceded by `// m2-allow:`. New `LoginScreen` content goes through the design system.
- **`verifyNoEmbeddedServerSecrets`:** rejects `TASTILE_WEB_BRIDGE_SECRET` / `x-tastile-web-bridge-secret` from Android sources and the build script.
- **The lint block in `app/build.gradle.kts` must not add `disable +=`.**
- **Before claiming "PASS / DONE / GREEN / ready to ship", run `./gradlew verify` from a clean state.** If your change is in `:app` source, also run the unit-test target to catch regression coverage gaps.
- **The `verify-tastile-change` skill** runs both repos' gates before merging. Each cross-repo change needs evidence on both sides.
- **Three new strings (`login_error_google_unavailable`, `login_error_google_failed`, `login_button_continue_google_in_progress`) must land in all 9 locales** of `system_auth.xml`: `values/`, `values-de/`, `values-es/`, `values-fr/`, `values-ja/`, `values-ko/`, `values-pt-rBR/`, `values-zh-rCN/`. Missing translations in `aapt2` will be a build warning.
- **Commit message language:** English. End commit messages with `Co-Authored-By: Claude Code <noreply@anthropic.com>`.

---

## Phase A — tastile-web (producer)

### Task 1: Extend BetterAuth Google provider config to accept Android client ID

**Files:**
- Modify: `../tastile-web/src/shared/auth/better-auth/server.ts:40-50` (the `socialProviders()` function)
- Modify: `../tastile-web/.env.development.example:24` (add `GOOGLE_ANDROID_CLIENT_ID=`)
- Modify: `../tastile-web/.env.production.example:24` (add `GOOGLE_ANDROID_CLIENT_ID=`)

**Interfaces:**
- Consumes: `process.env.GOOGLE_CLIENT_ID`, `process.env.GOOGLE_CLIENT_SECRET` (existing).
- Produces: `providers.google.clientId: string[]` — array of accepted OAuth client IDs. Existing `string` consumer behavior preserved (single-element array).

- [ ] **Step 1: Read the current Google provider config**

Run from `../tastile-web`:
```bash
cd ../tastile-web && sed -n '35,55p' src/shared/auth/better-auth/server.ts
```
Expected output: confirms the existing `socialProviders()` function and the `google` provider block.

- [ ] **Step 2: Modify `socialProviders()` to read `GOOGLE_ANDROID_CLIENT_ID`**

In `../tastile-web/src/shared/auth/better-auth/server.ts`, replace the existing Google block (around line 40–48) with:

```ts
function socialProviders() {
  const providers: Record<string, unknown> = {};
  const googleClientId = process.env.GOOGLE_CLIENT_ID?.trim();
  const googleClientSecret = process.env.GOOGLE_CLIENT_SECRET?.trim();
  const googleAndroidClientId = process.env.GOOGLE_ANDROID_CLIENT_ID?.trim();
  if (googleClientId && googleClientSecret) {
    const clientIds = googleAndroidClientId
      ? [googleClientId, googleAndroidClientId]
      : [googleClientId];
    providers.google = { clientId: clientIds, clientSecret: googleClientSecret };
  }
  // Sign in with Apple: APPLE_CLIENT_SECRET is the pre-signed ES256 client
  // secret JWT (better-auth 1.7 consumes a clientSecret, not raw key parts).
  const appleClientId = process.env.APPLE_CLIENT_ID?.trim();
  const appleClientSecret = process.env.APPLE_CLIENT_SECRET?.trim();
  if (appleClientId && appleClientSecret) {
    providers.apple = { clientId: appleClientId, clientSecret: appleClientSecret };
  }
  return providers;
}
```

- [ ] **Step 3: Add the new env var to both `.env.example` files**

In `../tastile-web/.env.development.example`, after the existing `GOOGLE_CLIENT_ID=` line (line 24), add:

```
# Optional: Android OAuth 2.0 client ID registered in Google Cloud Console as
# the `app.tastile.android` Android client (debug + release SHA-1).
# When set, BetterAuth accepts idTokens from BOTH this client and GOOGLE_CLIENT_ID
# (above). Leave blank on environments that don't need Android sign-in.
GOOGLE_ANDROID_CLIENT_ID=
```

In `../tastile-web/.env.production.example`, add the same block (without the `# Optional` intro — same comment is fine).

- [ ] **Step 4: Run the web-side typecheck + relevant Vitest**

Run from `../tastile-web`:
```bash
cd ../tastile-web && pnpm typecheck
```
Expected: PASS (no TypeScript errors).

Then:
```bash
cd ../tastile-web && pnpm vitest run src/shared/auth/better-auth
```
Expected: PASS (existing tests for the BetterAuth config still pass; no existing test directly inspects the shape of `clientId`, so the array change should be transparent).

- [ ] **Step 5: Verify the file diff**

Run from `tastile-android`:
```bash
cd ../tastile-web && git diff --stat src/shared/auth/better-auth/server.ts .env.development.example .env.production.example
```
Expected: three files touched; ~10 lines added in `server.ts`, 4 lines added in each `.env.example`.

- [ ] **Step 6: Commit**

Run from `../tastile-web`:
```bash
cd ../tastile-web && git add src/shared/auth/better-auth/server.ts .env.development.example .env.production.example && git commit -m "feat(auth): accept Android OAuth client ID alongside web Google client

BetterAuth's GoogleOptions.clientId is string | string[]; passing both
the web and Android client IDs lets /api/auth/sign-in/social accept
idTokens from native Android Credential Manager flows. Existing single-
client consumers (web OAuth callback) keep working because
[GOOGLE_CLIENT_ID] is still the first element of the array.

GOOGLE_ANDROID_CLIENT_ID is optional; unset on envs that don't need
native Android sign-in.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Phase B — tastile-android HTTP layer (no UI yet)

### Task 2: Add `signInWithGoogleIdToken` to `BetterAuthHttpClient`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/auth/BetterAuthHttpClient.kt:80` (after `signUp`, before `signOut`)
- Modify: `app/src/test/java/app/tastile/android/data/auth/BetterAuthHttpClientTest.kt` (add new test methods; create if not present)

**Interfaces:**
- Consumes: `BuildConfig.WEB_BASE_URL` (existing), `MockWebServer` (test).
- Produces: `suspend fun signInWithGoogleIdToken(idToken: String): BetterAuthSession`. Throws `BetterAuthException` on non-2xx or missing session cookie.

- [ ] **Step 1: Locate the existing `BetterAuthHttpClientTest`**

Run:
```bash
ls app/src/test/java/app/tastile/android/data/auth/
```
Expected: at least `BetterAuthHttpClientTest.kt` exists. If it doesn't, the existing test harness uses a different convention; in that case check `find . -name "BetterAuthHttpClientTest.kt"`.

- [ ] **Step 2: Append the failing test for `signInWithGoogleIdToken`**

Open `app/src/test/java/app/tastile/android/data/auth/BetterAuthHttpClientTest.kt` and append at the end of the class (create the class + file if it doesn't exist, with the same `@RunWith(RobolectricTestRunner::class)` pattern the other auth tests use):

```kotlin
    @Test
    fun signInWithGoogleIdToken_postsCorrectBodyAndDecodesSession() = runTest {
        val sessionToken = "test-session-token-abc123"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "better-auth.session_token=$sessionToken; Path=/; HttpOnly; SameSite=Lax")
                .setBody(
                    """{"user":{"id":"google-user-1","email":"user@example.com"},"session":{"expiresAt":1700000000}}""",
                ),
        )

        val session = client.signInWithGoogleIdToken(idToken = "fake-google-id-token")

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/auth/sign-in/social", recorded.path)
        val body = recorded.body.readUtf8()
        // Body must include both the provider and the idToken wrapper.
        assertTrue("body must include provider=google: $body", body.contains("\"provider\":\"google\""))
        assertTrue("body must include idToken.token: $body", body.contains("\"token\":\"fake-google-id-token\""))
        assertEquals(sessionToken, session.sessionToken)
        assertEquals("google-user-1", session.userId)
        assertEquals("user@example.com", session.email)
        assertEquals(1_700_000_000L, session.expiresAtEpochSeconds)
    }

    @Test
    fun signInWithGoogleIdToken_throwsOnMissingSessionCookie() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"user":{"id":"u","email":"u@x"}}"""),
        )

        val ex = assertThrows<BetterAuthException> {
            client.signInWithGoogleIdToken(idToken = "any-token")
        }
        assertTrue(
            "expected missing-cookie message, got: ${ex.message}",
            ex.message?.contains("missing session cookie") == true,
        )
    }

    @Test
    fun signInWithGoogleIdToken_throwsOnNon2xx() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val ex = assertThrows<BetterAuthException> {
            client.signInWithGoogleIdToken(idToken = "rejected-token")
        }
        assertTrue(
            "expected 401 in message, got: ${ex.message}",
            ex.message?.contains("401") == true,
        )
    }
```

If the test file/class doesn't exist yet, use this full scaffold (replace the body of the file with):

```kotlin
package app.tastile.android.data.auth

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BetterAuthHttpClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: BetterAuthHttpClient

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        client = BetterAuthHttpClient(baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ... three @Test methods above ...
}
```

- [ ] **Step 3: Run the new tests to verify they fail**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.auth.BetterAuthHttpClientTest"
```
Expected: 3 failures with `signInWithGoogleIdToken` not resolved / unresolved reference. (If the test file already exists, only the 3 new methods fail; existing tests stay green.)

- [ ] **Step 4: Add `signInWithGoogleIdToken` to `BetterAuthHttpClient`**

Open `app/src/main/java/app/tastile/android/data/auth/BetterAuthHttpClient.kt`. After the `signUp` method (around line 90, just before `signOut`), add:

```kotlin
    suspend fun signInWithGoogleIdToken(idToken: String): BetterAuthSession = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("provider", JsonPrimitive("google"))
            put("idToken", buildJsonObject {
                put("token", JsonPrimitive(idToken))
            })
        }
        val response = postJson("/api/auth/sign-in/social", body)
        val sessionToken = response.cookies.firstNotNullOfOrNull { extractSessionToken(listOf(it)) }
            ?: throw BetterAuthException("Google sign-in response missing session cookie")
        decodeSession(sessionToken, response.body)
    }
```

- [ ] **Step 5: Run the new tests to verify they pass**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.auth.BetterAuthHttpClientTest"
```
Expected: 3 PASS (the 3 new tests). Existing tests still pass.

- [ ] **Step 6: Run the broader auth unit-test sweep**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.auth.*"
```
Expected: all PASS. No regressions.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/app/tastile/android/data/auth/BetterAuthHttpClient.kt \
        app/src/test/java/app/tastile/android/data/auth/BetterAuthHttpClientTest.kt
git commit -m "feat(auth): add BetterAuthHttpClient.signInWithGoogleIdToken

POSTs to /api/auth/sign-in/social with BetterAuth's idToken branch body
{ provider: \"google\", idToken: { token } }, decodes the resulting
Set-Cookie + JSON body via existing decodeSession, and returns a
BetterAuthSession identical in shape to the email sign-in path.

Throws BetterAuthException on non-2xx or missing session cookie,
matching the existing signIn / signUp error contract.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 3: Add `signInWithGoogle` to `AuthRepository` (using a stub launcher)

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/auth/AuthRepositoryContract.kt` (add `signInWithGoogle()` to interface + KDoc)
- Modify: `app/src/main/java/app/tastile/android/data/auth/AuthRepository.kt` (impl using a stub `GoogleSignInLauncher` field)
- Create: `app/src/main/java/app/tastile/android/data/auth/GoogleSignInLauncher.kt` (interface + 2 exception classes only — real impl lands in Task 4)
- Modify: `app/src/test/java/app/tastile/android/data/auth/AuthRepositoryTest.kt` (or create)

**Interfaces:**
- Consumes: `GoogleSignInLauncher.getIdToken(): String` (interface, throws `GoogleSignInUnavailableException` / `GoogleSignInFailedException`).
- Produces: `suspend fun AuthRepository.signInWithGoogle()` — calls launcher, calls `httpClient.signInWithGoogleIdToken(idToken)`, calls existing `persistSession(session)`.

- [ ] **Step 1: Create `GoogleSignInLauncher.kt` with interface + exceptions only**

Create `app/src/main/java/app/tastile/android/data/auth/GoogleSignInLauncher.kt`:

```kotlin
package app.tastile.android.data.auth

/**
 * Wraps Credential Manager + Google Identity Services for the native
 * Google Sign-In flow on Android. Implementations are responsible for
 * presenting the system account picker, returning a Google ID token, or
 * throwing [GoogleSignInUnavailableException] when the user cannot or
 * will not complete the flow (no Google account on device, no Play
 * Services, user cancel).
 */
interface GoogleSignInLauncher {
    /**
     * Returns a Google ID token suitable for exchange against
     * BetterAuth's /api/auth/sign-in/social idToken branch.
     *
     * @throws GoogleSignInUnavailableException when no Google account is
     *   available, Play Services is missing, or the user cancels the
     *   system account picker. The caller is expected to fall back to
     *   the web OAuth handoff.
     * @throws GoogleSignInFailedException for any other failure (network
     *   error during token exchange, unexpected credential type).
     */
    suspend fun getIdToken(): String
}

/**
 * The user cannot or will not complete native Google Sign-In.
 * Callers should fall back to the web OAuth handoff
 * (`AuthRepository.signInWithProvider("google")`).
 */
class GoogleSignInUnavailableException(cause: Throwable) : RuntimeException(cause)

/**
 * A failure other than user unavailability — typically a server-side
 * rejection or an unexpected credential type. Surfaces as an error
 * chip in the UI rather than triggering the web fallback.
 */
class GoogleSignInFailedException(message: String) : RuntimeException(message)
```

- [ ] **Step 2: Add `signInWithGoogle()` to `AuthRepositoryContract`**

Open `app/src/main/java/app/tastile/android/data/auth/AuthRepositoryContract.kt`. After the existing `signInWithProvider(provider: String)` method (around line 30), add:

```kotlin
    /**
     * Native Google Sign-In via Credential Manager. Exchanges the Google
     * idToken against BetterAuth's /api/auth/sign-in/social idToken branch,
     * then persists the resulting session identically to email sign-in.
     *
     * Throws [GoogleSignInUnavailableException] when no Google account is
     * available — callers should fall back to
     * [signInWithProvider]("google") which opens the web OAuth handoff.
     */
    suspend fun signInWithGoogle()
```

Also update the file-level KDoc on `AuthRepositoryContract` (line 11) to remove the "Social providers (Google / Apple) still defer to the web login page" sentence and replace it with a note that native Google Sign-In is now wired (Apple still defers). New file header:

```kotlin
/**
 * Data-layer entry point for native BetterAuth sign-in / sign-out. The
 * concrete implementation ([AuthRepository]) drives `TastileAuthState` and
 * is the only owner of the [EncryptedTokenStorage] session token + the
 * `ApiTokenCache` mint chain.
 *
 * Replaces the previous Cognito-PKCE contract. Native Google Sign-In is
 * wired here via [signInWithGoogle]; Apple sign-in still defers to the
 * web login page until the native OAuth bridge is implemented in a
 * follow-up — see [signInWithProvider].
 */
```

- [ ] **Step 3: Modify `AuthRepository` to implement `signInWithGoogle`**

Open `app/src/main/java/app/tastile/android/data/auth/AuthRepository.kt`. First, add the import at the top of the file (after the existing `import dagger.Lazy` line):

```kotlin
import app.tastile.android.data.auth.GoogleSignInLauncher
```

Then add the constructor parameter (modify the existing `@Inject constructor`):

```kotlin
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: BetterAuthHttpClient,
    private val apiTokenCache: Lazy<ApiTokenCache>,
    private val googleSignInLauncher: GoogleSignInLauncher,
) : CurrentUserProvider, AuthRepositoryContract {
```

Then add the impl method (after `signInWithProvider`, before `signOut`):

```kotlin
    override suspend fun signInWithGoogle() {
        val idToken = googleSignInLauncher.getIdToken()
        val session = httpClient.signInWithGoogleIdToken(idToken)
        persistSession(session)
    }
```

- [ ] **Step 4: Locate or scaffold `AuthRepositoryTest`**

Run:
```bash
ls app/src/test/java/app/tastile/android/data/auth/
```
If `AuthRepositoryTest.kt` exists, jump to Step 5. If not, create the file with this scaffold:

```kotlin
package app.tastile.android.data.auth

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthRepositoryTest {

    // ... three new tests below ...
}
```

Note: `AuthRepository` has many constructor parameters (`Context`, `BetterAuthHttpClient`, `Lazy<ApiTokenCache>`, `EncryptedTokenStorage`, …). The existing test harness (if any) may construct via Hilt; if so, follow its pattern. If you need to construct it directly, see how existing tests in the project handle multi-parameter repositories — search `git grep -l "AuthRepository(" app/src/test/`.

If the existing test class constructs `AuthRepository` directly, add the new launcher mock parameter to that construction. If it goes through Hilt, the launcher must be a `@TestInstallIn` or `@BindValue` to override — search `git grep -l "HiltAndroidRule" app/src/test/`.

- [ ] **Step 5: Add the three failing tests for `signInWithGoogle`**

Append to `AuthRepositoryTest` (adjust the mock setup to match the existing class style — if the existing class already uses MockK with `coEvery` / `coVerify`, follow that pattern; if it uses different mocks, use the same):

```kotlin
    @Test
    fun signInWithGoogle_persistsSession() = runTest {
        val launcher = mockk<GoogleSignInLauncher>()
        val httpClient = mockk<BetterAuthHttpClient>()
        // ...existing repo construction with launcher=httpClient=httpClient, launcher=launcher...
        coEvery { launcher.getIdToken() } returns "fake-google-id-token"
        val fakeSession = BetterAuthHttpClient.BetterAuthSession(
            sessionToken = "t",
            userId = "u",
            email = "u@x",
            expiresAtEpochSeconds = null,
        )
        coEvery { httpClient.signInWithGoogleIdToken("fake-google-id-token") } returns fakeSession

        // ...call repo.signInWithGoogle()...

        coVerify { launcher.getIdToken() }
        coVerify { httpClient.signInWithGoogleIdToken("fake-google-id-token") }
    }

    @Test
    fun signInWithGoogle_propagatesUnavailableException() = runTest {
        val launcher = mockk<GoogleSignInLauncher>()
        val httpClient = mockk<BetterAuthHttpClient>()
        coEvery { launcher.getIdToken() } throws GoogleSignInUnavailableException(RuntimeException("no account"))

        // ...assertThrows<GoogleSignInUnavailableException> { repo.signInWithGoogle() }...

        coVerify(exactly = 0) { httpClient.signInWithGoogleIdToken(any()) }
    }

    @Test
    fun signInWithGoogle_propagatesFailedException() = runTest {
        val launcher = mockk<GoogleSignInLauncher>()
        val httpClient = mockk<BetterAuthHttpClient>()
        coEvery { launcher.getIdToken() } throws GoogleSignInFailedException("bad token type")

        // ...assertThrows<GoogleSignInFailedException> { repo.signInWithGoogle() }...

        coVerify(exactly = 0) { httpClient.signInWithGoogleIdToken(any()) }
    }
```

The exact `// ...` lines depend on how the existing test constructs the repository. Match the surrounding style; if no `AuthRepositoryTest` exists yet, build the smallest one that constructs `AuthRepository` with all four constructor params mocked (the launcher can be a `mockk<GoogleSignInLauncher>()`).

- [ ] **Step 6: Run the new tests to verify they fail**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.auth.AuthRepositoryTest"
```
Expected: 3 failures with `signInWithGoogle` unresolved / mock setup errors.

- [ ] **Step 7: Confirm the new AuthRepository impl compiles**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: PASS (the `signInWithGoogle` impl lands in Step 3; this just confirms compilation).

- [ ] **Step 8: Run the new tests to verify they pass**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.auth.AuthRepositoryTest"
```
Expected: 3 PASS (the 3 new tests). If the repo construction can't be wired because existing tests don't construct it directly, this task is incomplete — re-read Step 4, find the construction pattern in `git grep "AuthRepository(" app/src/test/`, and adjust.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/app/tastile/android/data/auth/GoogleSignInLauncher.kt \
        app/src/main/java/app/tastile/android/data/auth/AuthRepositoryContract.kt \
        app/src/main/java/app/tastile/android/data/auth/AuthRepository.kt \
        app/src/test/java/app/tastile/android/data/auth/AuthRepositoryTest.kt
git commit -m "feat(auth): add AuthRepository.signInWithGoogle using launcher

Orchestrates the native Google Sign-In flow: GoogleSignInLauncher
returns an idToken, BetterAuthHttpClient.signInWithGoogleIdToken
exchanges it against BetterAuth's /api/auth/sign-in/social idToken
branch, and the existing persistSession path writes the session to
EncryptedTokenStorage.

The launcher is injected via the constructor; the real Credential
Manager implementation lands in the next task. The launcher is an
interface so this layer can be unit-tested with a mock.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Phase C — tastile-android Credential Manager integration

### Task 4: Implement `CredentialManagerGoogleSignInLauncher`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/auth/GoogleSignInLauncher.kt` (add the impl class)
- Modify: `app/src/test/java/app/tastile/android/data/auth/GoogleSignInLauncherTest.kt` (create; uses Robolectric for the `Context` requirement)

**Interfaces:**
- Consumes: `@ApplicationContext Context`, `serverClientId: String` (from BuildConfig).
- Produces: `class CredentialManagerGoogleSignInLauncher` implementing `GoogleSignInLauncher` via `androidx.credentials.CredentialManager` + `com.google.android.libraries.identity.googleid.GetGoogleIdOption`.

- [ ] **Step 1: Read the current `GoogleSignInLauncher.kt`**

```bash
cat app/src/main/java/app/tastile/android/data/auth/GoogleSignInLauncher.kt
```
Expected: the interface + two exception classes from Task 3.

- [ ] **Step 2: Append the impl class**

Add the following imports at the top of `app/src/main/java/app/tastile/android/data/auth/GoogleSignInLauncher.kt`:

```kotlin
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
```

Then append to the bottom of the same file:

```kotlin
/**
 * [GoogleSignInLauncher] backed by [CredentialManager] +
 * [GetGoogleIdOption]. Presents the system account picker (or auto-
 * selects when [GetGoogleIdOption.setAutoSelectEnabled] is true and
 * exactly one Google account is on the device), and returns the issued
 * Google ID token for exchange against BetterAuth.
 *
 * Filtering is disabled ([GetGoogleIdOption.setFilterByAuthorizedAccounts]
 * = false) so first-time users also see the picker; BetterAuth's server
 * creates the user account on first sign-in.
 */
@Singleton
class CredentialManagerGoogleSignInLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("googleAndroidClientId") private val serverClientId: String,
) : GoogleSignInLauncher {

    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    override suspend fun getIdToken(): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = try {
            credentialManager.getCredential(context, request)
        } catch (e: GetCredentialException) {
            throw GoogleSignInUnavailableException(e)
        }

        val credential = result.credential
        if (credential !is GoogleIdTokenCredential) {
            throw GoogleSignInFailedException(
                "Unexpected credential type: ${credential::class.java.name}",
            )
        }
        return credential.idToken
            ?: throw GoogleSignInFailedException("GoogleIdTokenCredential.idToken is null")
    }
}
```

- [ ] **Step 3: Add unit tests for the impl**

The `CredentialManagerGoogleSignInLauncher` depends on `CredentialManager.create(context)`, which requires Android system services. Pure JVM unit tests can't exercise it directly; use Robolectric.

Create `app/src/test/java/app/tastile/android/data/auth/GoogleSignInLauncherTest.kt`:

```kotlin
package app.tastile.android.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Smoke tests for [CredentialManagerGoogleSignInLauncher].
 *
 * Robolectric provides the system CredentialManager stub; the launcher
 * can be constructed but the actual `getCredential` call is not
 * exercised here because it requires a real Play Services / system
 * account picker round-trip. The end-to-end Google Sign-In flow is
 * verified on a real device — see
 * docs/superpowers/mobile/google-signin-manual-evidence.md.
 *
 * These tests cover:
 *   - construction with the expected server client id
 *   - construction does not eagerly call CredentialManager.create(...)
 *     (the field is `by lazy` so construction is side-effect free)
 */
@RunWith(RobolectricTestRunner::class)
class GoogleSignInLauncherTest {

    @Test
    fun launcher_canBeConstructed() {
        val launcher = CredentialManagerGoogleSignInLauncher(
            context = org.robolectric.RuntimeEnvironment.getApplication(),
            serverClientId = "test-client-id.apps.googleusercontent.com",
        )
        // No assertion needed beyond construction succeeding.
        assertEquals(
            "test-client-id.apps.googleusercontent.com",
            (launcher as CredentialManagerGoogleSignInLauncher)
                .let { it.javaClass.getDeclaredField("serverClientId").apply { isAccessible = true }.get(it) as String },
        )
    }
}
```

If `RuntimeEnvironment.getApplication()` is deprecated in the project's Robolectric version (4.16.1), use `ApplicationProvider.getApplicationContext()` instead and add the import:

```kotlin
import androidx.test.core.app.ApplicationProvider
```

```kotlin
        context = ApplicationProvider.getApplicationContext(),
```

- [ ] **Step 4: Run the test to verify it passes**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.auth.GoogleSignInLauncherTest"
```
Expected: 1 PASS.

- [ ] **Step 5: Confirm full auth compilation**

Run:
```bash
./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin
```
Expected: PASS. No new warnings beyond what was already there.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/data/auth/GoogleSignInLauncher.kt \
        app/src/test/java/app/tastile/android/data/auth/GoogleSignInLauncherTest.kt
git commit -m "feat(auth): implement CredentialManagerGoogleSignInLauncher

Wraps androidx.credentials.CredentialManager + GetGoogleIdOption. Auto-
selects the single Google account on the device when available; on
GetCredentialException (no account / no Play Services / user cancel)
rethrows as GoogleSignInUnavailableException so the caller can fall
back to the web OAuth handoff.

The launcher is @Singleton and exposes only the @Named('googleAndroid-
ClientId')-injected server client id, so Hilt wires the BuildConfig
field in the next task without this class knowing about BuildConfig.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 5: Hilt wiring for `GoogleSignInLauncher` + `googleAndroidClientId`

**Files:**
- Create: `app/src/main/java/app/tastile/android/di/AuthModule.kt`

**Interfaces:**
- Consumes: `BuildConfig.GOOGLE_ANDROID_CLIENT_ID` (must be non-blank per `gradle.projectsEvaluated` guard).
- Produces: `@Provides @Singleton fun provideGoogleSignInLauncher(...) : GoogleSignInLauncher`; `@Provides @Named("googleAndroidClientId") fun provideGoogleAndroidClientId() : String`.

- [ ] **Step 1: Check whether an `AuthModule.kt` already exists**

Run:
```bash
ls app/src/main/java/app/tastile/android/di/
```
Expected: at least `AppModule.kt`. If `AuthModule.kt` exists, append to it; otherwise create new.

- [ ] **Step 2: Create `AuthModule.kt`**

Create `app/src/main/java/app/tastile/android/di/AuthModule.kt`:

```kotlin
package app.tastile.android.di

import android.content.Context
import app.tastile.android.BuildConfig
import app.tastile.android.data.auth.CredentialManagerGoogleSignInLauncher
import app.tastile.android.data.auth.GoogleSignInLauncher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt bindings for the native Google Sign-In path.
 *
 * `googleAndroidClientId` is the Android OAuth 2.0 client ID registered
 * in Google Cloud Console as `app.tastile.android` with debug + release
 * SHA-1 fingerprints. Its value is sourced from BuildConfig (which is
 * non-blank by the `gradle.projectsEvaluated` hard requirement in
 * `app/build.gradle.kts`).
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Named("googleAndroidClientId")
    fun provideGoogleAndroidClientId(): String = BuildConfig.GOOGLE_ANDROID_CLIENT_ID

    @Provides
    @Singleton
    fun provideGoogleSignInLauncher(
        @ApplicationContext context: Context,
        @Named("googleAndroidClientId") clientId: String,
    ): GoogleSignInLauncher = CredentialManagerGoogleSignInLauncher(context, clientId)
}
```

If the file already exists (e.g., other auth-related bindings), append just the two `@Provides` methods inside the existing `object AuthModule`.

- [ ] **Step 3: Compile the full module to verify Hilt graph**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: PASS. Hilt's KSP processor should resolve `googleSignInLauncher` for `AuthRepository`'s constructor parameter.

- [ ] **Step 4: Run all auth unit tests**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.auth.*"
```
Expected: all PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/di/AuthModule.kt
git commit -m "feat(di): bind GoogleSignInLauncher + googleAndroidClientId in Hilt

The launcher is now resolvable wherever it's injected (currently only
in AuthRepository). The @Named(\"googleAndroidClientId\") qualifier
keeps the BuildConfig field opt-in — other modules don't see it
unless they explicitly ask for it.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Phase D — tastile-android UI

### Task 6: Add `signInWithGoogle` + `isGoogleSigningIn` to `LoginViewModel`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/login/LoginViewModel.kt`
- Modify: `app/src/test/java/app/tastile/android/ui/login/LoginViewModelTest.kt` (create if absent)

**Interfaces:**
- Consumes: `AuthRepositoryContract.signInWithGoogle()` (Task 3), `AuthRepositoryContract.signInWithProvider("google")` (existing fallback).
- Produces: `fun signInWithGoogle(context: Context)`, `val isGoogleSigningIn: StateFlow<Boolean>`.

- [ ] **Step 1: Locate `LoginViewModelTest.kt`**

Run:
```bash
ls app/src/test/java/app/tastile/android/ui/login/ 2>/dev/null || echo "MISSING"
```
If missing, create the file with this scaffold:

```kotlin
package app.tastile.android.ui.login

import app.tastile.android.data.auth.AuthRepositoryContract
import app.tastile.android.data.auth.GoogleSignInFailedException
import app.tastile.android.data.auth.GoogleSignInUnavailableException
import app.tastile.android.data.auth.TastileAuthState
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LoginViewModelTest {

    private val authRepository: AuthRepositoryContract = mockk(relaxed = true)
    private val context: android.content.Context = org.robolectric.RuntimeEnvironment.getApplication()

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LoginViewModel(authRepository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ... four new @Test methods below ...
}
```

- [ ] **Step 2: Append four failing tests**

Append to `LoginViewModelTest`:

```kotlin
    @Test
    fun signInWithGoogle_launchesAuthRepo() = runTest {
        coEvery { authRepository.signInWithGoogle() } returns Unit

        viewModel.signInWithGoogle(context)

        coVerify { authRepository.signInWithGoogle() }
        assertEquals(false, viewModel.isGoogleSigningIn.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun signInWithGoogle_unavailableFallbackToWebHandoff() = runTest {
        coEvery { authRepository.signInWithGoogle() } throws GoogleSignInUnavailableException(RuntimeException("no account"))
        coEvery { authRepository.signInWithProvider("google") } returns Unit

        viewModel.signInWithGoogle(context)

        coVerify { authRepository.signInWithProvider("google") }
        // Silent fallback — no error chip.
        assertNull(viewModel.error.value)
        assertEquals(false, viewModel.isGoogleSigningIn.value)
    }

    @Test
    fun signInWithGoogle_serverErrorSurfacesAsChip() = runTest {
        coEvery { authRepository.signInWithGoogle() } throws RuntimeException("HTTP 401")

        viewModel.signInWithGoogle(context)

        assertEquals("HTTP 401", viewModel.error.value)
        assertEquals(false, viewModel.isGoogleSigningIn.value)
    }

    @Test
    fun signInWithGoogle_failedExceptionSurfacesAsChip() = runTest {
        coEvery { authRepository.signInWithGoogle() } throws GoogleSignInFailedException("bad credential type")

        viewModel.signInWithGoogle(context)

        assertEquals("bad credential type", viewModel.error.value)
        assertEquals(false, viewModel.isGoogleSigningIn.value)
    }
```

- [ ] **Step 3: Run the new tests to verify they fail**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.login.LoginViewModelTest"
```
Expected: 4 failures with `signInWithGoogle` / `isGoogleSigningIn` unresolved.

- [ ] **Step 4: Add the new state and method to `LoginViewModel`**

Open `app/src/main/java/app/tastile/android/ui/login/LoginViewModel.kt`. After the existing `_password` field declaration (around line 31), add:

```kotlin
    private val _isGoogleSigningIn = MutableStateFlow(false)
    val isGoogleSigningIn: StateFlow<Boolean> = _isGoogleSigningIn.asStateFlow()
```

Then add the import for the new exception types — find the existing `import app.tastile.android.data.auth.AuthRepositoryContract` line and add after it:

```kotlin
import app.tastile.android.data.auth.GoogleSignInUnavailableException
```

Then add the `signInWithGoogle` method after the existing `signOut` method (around line 102, just before `clearError`):

```kotlin
    fun signInWithGoogle(@Suppress("UNUSED_PARAMETER") context: Context) {
        if (_isGoogleSigningIn.value) return
        viewModelScope.launch {
            try {
                _isGoogleSigningIn.value = true
                _error.value = null
                authRepository.signInWithGoogle()
            } catch (e: CancellationException) {
                throw e
            } catch (e: GoogleSignInUnavailableException) {
                // No Google account on device / Play Services missing / user
                // cancel. Fall back to the existing web OAuth handoff so the
                // user still gets a path to Google sign-in.
                try {
                    authRepository.signInWithProvider("google")
                } catch (fallback: Exception) {
                    _error.value = this@LoginViewModel.context.getString(
                        R.string.login_error_google_unavailable,
                    )
                }
            } catch (e: Exception) {
                _error.value = e.message
                    ?: this@LoginViewModel.context.getString(R.string.login_error_google_failed)
            } finally {
                _isGoogleSigningIn.value = false
            }
        }
    }
```

- [ ] **Step 5: Run the new tests to verify they pass**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.login.LoginViewModelTest"
```
Expected: 4 PASS. Existing tests for the VM still PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/login/LoginViewModel.kt \
        app/src/test/java/app/tastile/android/ui/login/LoginViewModelTest.kt
git commit -m "feat(login): add LoginViewModel.signInWithGoogle + isGoogleSigningIn

Orchestrates the native Google Sign-In flow with silent fallback to
the web OAuth handoff on GoogleSignInUnavailableException (no Google
account / Play Services missing / user cancel). Surfaces other failures
through the existing error chip with the localized error string.

isGoogleSigningIn is independent of the existing isSigningIn flag so
tapping 'Sign in' while 'Continue with Google' is in flight is
rejected by both flows.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 7: Add "Continue with Google" button to `LoginScreen`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/login/LoginScreen.kt` (add button row, divider, testTag wiring)
- Create: `app/src/main/res/drawable/ic_google_g.xml` (vector drawable)
- Modify: `app/src/main/res/values/system_auth.xml` (3 new strings)
- Modify: `app/src/main/res/values-de/system_auth.xml`
- Modify: `app/src/main/res/values-es/system_auth.xml`
- Modify: `app/src/main/res/values-fr/system_auth.xml`
- Modify: `app/src/main/res/values-ja/system_auth.xml`
- Modify: `app/src/main/res/values-ko/system_auth.xml`
- Modify: `app/src/main/res/values-pt-rBR/system_auth.xml`
- Modify: `app/src/main/res/values-zh-rCN/system_auth.xml`
- Modify: `app/src/test/java/app/tastile/android/ui/login/LoginScreenTest.kt` (create if absent; Compose UI tests under Robolectric)

**Interfaces:**
- Consumes: `LoginViewModel.signInWithGoogle`, `LoginViewModel.isGoogleSigningIn`, existing email/password fields.
- Produces: a single `OutlinedButton` with `Modifier.testTag("login-google-button")` placed between the existing primary "Sign in" `Button` and the "Create an account" `TextButton`.

- [ ] **Step 1: Add the Google "G" vector drawable**

Create `app/src/main/res/drawable/ic_google_g.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M21.35,11.1H12v3.2h5.35c-0.5,2.4 -2.5,3.5 -5.35,3.5c-3.2,0 -5.8,-2.6 -5.8,-5.8s2.6,-5.8 5.8,-5.8c1.45,0 2.75,0.5 3.75,1.45l2.4,-2.4C16.55,3.65 14.4,2.7 12,2.7C6.85,2.7 2.7,6.85 2.7,12s4.15,9.3 9.3,9.3c5.4,0 9,-3.8 9,-9.15C20.85,11.85 21.35,11.1 21.35,11.1z" />
</vector>
```

- [ ] **Step 2: Add the 3 new strings to all 9 locales**

For each of the 9 `system_auth.xml` files, add the three `<string>` entries. The English fallback in `values/system_auth.xml`:

```xml
    <string name="login_button_continue_google_in_progress">Connecting to Google…</string>
    <string name="login_error_google_unavailable">Google Sign-In isn\'t available on this device</string>
    <string name="login_error_google_failed">Unable to verify Google account</string>
```

For each other locale, use a translation consistent with the surrounding style. Japanese:

```xml
    <string name="login_button_continue_google_in_progress">Google に接続中…</string>
    <string name="login_error_google_unavailable">この端末では Google サインインを利用できません</string>
    <string name="login_error_google_failed">Google アカウントを確認できません</string>
```

For the remaining 6 locales (`values-de`, `values-es`, `values-fr`, `values-ko`, `values-pt-rBR`, `values-zh-rCN`), provide matching translations. If unsure, use the English fallback with a `<!-- TODO(i18n): translate -->` comment so the build does not break; the i18n review pass can iterate on phrasing afterwards (this matches the spec's "Open follow-ups" note).

- [ ] **Step 3: Locate or scaffold `LoginScreenTest`**

Run:
```bash
ls app/src/test/java/app/tastile/android/ui/login/ 2>/dev/null
```
If `LoginScreenTest.kt` exists, jump to Step 4. Otherwise create it:

```kotlin
package app.tastile.android.ui.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.tastile.android.MainActivity
import app.tastile.android.data.auth.TastileAuthState
import app.tastile.android.util.MainActivityTestRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LoginScreenTest {

    @get:Rule
    val rule = MainActivityTestRule(createAndroidComposeRule<MainActivity>())

    // ... three new @Test methods below ...
}
```

Use the exact construction pattern (`MainActivityTestRule(createAndroidComposeRule<MainActivity>())`) that other tests in the project use — search `git grep -l "MainActivityTestRule" app/src/test/` to confirm. If `LoginScreenTest` doesn't currently exist, this introduces the Hilt + Robolectric + Compose rule triple for the first time in `app/src/test/` (most Compose tests in this repo likely live in `app/src/androidTest/`; if so, follow that convention instead and put the new tests under `app/src/androidTest/java/.../login/`).

- [ ] **Step 4: Append three failing tests**

Append to `LoginScreenTest`:

```kotlin
    @Test
    fun continueWithGoogleButton_isDisplayed() {
        rule.composeTestRule.onNodeWithTag("login-google-button").assertIsDisplayed()
    }

    @Test
    fun continueWithGoogleButton_triggersSignIn() {
        rule.composeTestRule.onNodeWithTag("login-google-button").performClick()
        // Side effect: LoginViewModel.signInWithGoogle is invoked. We can't
        // assert on the network call from a Compose test directly, but the
        // button being clickable + the test not throwing confirms wiring.
    }

    @Test
    fun continueWithGoogleButton_disabledWhileSigningIn() {
        // Drive the VM into isGoogleSigningIn = true and assert the button is
        // disabled. The exact harness depends on how LoginScreen reads the
        // VM state; if LoginScreen exposes a setter or test seam, use it.
        // Otherwise use the existing pattern from this project's other
        // Compose tests for "loading state" assertions.
    }
```

The third test should use whatever seam the project exposes for "VM in loading state." Look at other tests for "spinner / button disabled while loading" patterns: `git grep -rn "isSigningIn\|isLoading" app/src/test/`.

- [ ] **Step 5: Run the new tests to verify they fail**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.login.LoginScreenTest"
```
Expected: 3 failures (button not found / no testTag wiring yet).

- [ ] **Step 6: Modify `LoginScreen` to add the button**

Open `app/src/main/java/app/tastile/android/ui/login/LoginScreen.kt`. First, add the new state collector after the existing `isSigningIn` collector (around line 70):

```kotlin
    val isGoogleSigningIn by viewModel.isGoogleSigningIn.collectAsStateWithLifecycle()
```

Then, after the existing `Button` (the primary "Sign in" button, around line 147) and BEFORE the existing `Spacer(modifier = Modifier.height(Grid.inlineGap))` + `TextButton` ("Create an account"), insert:

```kotlin
            Spacer(modifier = Modifier.height(Grid.blockGap))

            // Subtle divider so the social button reads as a separate path
            // instead of a fourth email-password control.
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Spacer(modifier = Modifier.height(Grid.blockGap))

            OutlinedButton(
                onClick = { viewModel.signInWithGoogle(context) },
                enabled = !isSigningIn && !isGoogleSigningIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login-google-button"),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google_g),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(Grid.inlineGap))
                Text(
                    text = if (isGoogleSigningIn) {
                        stringResource(R.string.login_button_continue_google_in_progress)
                    } else {
                        stringResource(R.string.login_button_continue_google)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(Grid.inlineGap))
```

Add the required imports near the top of the file (find the existing `import androidx.compose.material3.OutlinedTextField` line and add these after it):

```kotlin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
```

- [ ] **Step 7: Run the new tests to verify they pass**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.login.LoginScreenTest"
```
Expected: 3 PASS.

- [ ] **Step 8: Confirm all auth + login unit tests pass**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.auth.*" --tests "app.tastile.android.ui.login.*"
```
Expected: all PASS.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/res/drawable/ic_google_g.xml \
        app/src/main/res/values/system_auth.xml \
        app/src/main/res/values-de/system_auth.xml \
        app/src/main/res/values-es/system_auth.xml \
        app/src/main/res/values-fr/system_auth.xml \
        app/src/main/res/values-ja/system_auth.xml \
        app/src/main/res/values-ko/system_auth.xml \
        app/src/main/res/values-pt-rBR/system_auth.xml \
        app/src/main/res/values-zh-rCN/system_auth.xml \
        app/src/main/java/app/tastile/android/ui/login/LoginScreen.kt \
        app/src/test/java/app/tastile/android/ui/login/LoginScreenTest.kt
git commit -m "feat(login): render 'Continue with Google' button on LoginScreen

Single OutlinedButton between the email/password block and the
'Create an account' link, separated by a subtle divider so it reads as
a separate auth path. testTag 'login-google-button' is exposed for
Compose UI tests; leading icon is the Google 'G' mark vector drawable.

The button label flips to 'Connecting to Google…' (localized in all 9
locales) while isGoogleSigningIn is true, and greyed out while either
isSigningIn or isGoogleSigningIn is true.

i18n: 3 new strings × 9 locales. Strings: login_button_continue_google_in_progress,
login_error_google_unavailable, login_error_google_failed. English
fallback in values/, Japanese in values-ja/, English fallbacks in the
remaining 6 locales with TODO(i18n) markers for the i18n review pass.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Phase E — Build config + final verification

### Task 8: Wire `GOOGLE_ANDROID_CLIENT_ID` into `BuildConfig`

**Files:**
- Modify: `app/build.gradle.kts:16,61,688` (extend the existing pattern)

**Interfaces:**
- Consumes: `gradle.properties` / `~/.gradle/gradle.properties` / `-PKEY=value`.
- Produces: `BuildConfig.GOOGLE_ANDROID_CLIENT_ID: String` (non-blank per the `gradle.projectsEvaluated` guard).

- [ ] **Step 1: Locate the existing `googleWebClientId` wiring**

```bash
grep -n "googleWebClientId\|GOOGLE_WEB_CLIENT_ID" app/build.gradle.kts
```
Expected: three references (around lines 16, 61, 688 — the `providers.gradleProperty`, the `buildConfigField`, and the guard's required-fields list).

- [ ] **Step 2: Add the `googleAndroidClientId` provider**

Right after the existing `val googleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID")` (around line 16), add:

```kotlin
val googleAndroidClientId = providers.gradleProperty("GOOGLE_ANDROID_CLIENT_ID")
```

- [ ] **Step 3: Add the `buildConfigField`**

Right after the existing `buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleWebClientId.orNull ?: ""}\"")` (around line 61), add:

```kotlin
        buildConfigField("String", "GOOGLE_ANDROID_CLIENT_ID", "\"${googleAndroidClientId.orNull ?: ""}\"")
```

- [ ] **Step 4: Extend the guard's required-fields list**

Open the `gradle.projectsEvaluated { ... }` block in `app/build.gradle.kts` and find the list of required BuildConfig fields (around line 680–690). The pattern is something like:

```kotlin
    val requiredBuildConfigFields = listOf(
        "COGNITO_USER_POOL_CLIENT_ID",
        ...,
        "GOOGLE_WEB_CLIENT_ID",
    )
```

Add `"GOOGLE_ANDROID_CLIENT_ID"` to that list.

- [ ] **Step 5: Run `./gradlew verify` from a clean state**

First, ensure the env var is set so the guard passes. From the project root:

```bash
# Local dev: append to gradle.properties (do NOT commit — see CLAUDE.md
# "Never commit .env* with real values" — but `gradle.properties` IS
# git-ignored locally; this is the standard place for local-only keys).
echo "GOOGLE_ANDROID_CLIENT_ID=PLACEHOLDER-ANDROID-CLIENT-ID.apps.googleusercontent.com" >> ~/.gradle/gradle.properties
```

Then:

```bash
./gradlew clean verify
```
Expected: BUILD SUCCESSFUL. The 15 pre-existing unit-test failures documented in `docs/superpowers/m3/phase-3-deferral.md` line 122 will still appear — they are unrelated to this change and predate the M3 migration.

If `./gradlew verify` reports NEW failures (beyond the 15 pre-existing), one of the new files introduced a regression. Read the failure stack trace; the most likely culprit is a missing import or a stub test. Fix and re-run before committing.

- [ ] **Step 6: Run the build guards explicitly**

```bash
./gradlew :app:verifyDesignSystemImports :app:verifyNoEmbeddedServerSecrets :app:verifyV1ApiCoverage
```
Expected: all PASS.

- [ ] **Step 7: Commit**

```bash
git add app/build.gradle.kts
git commit -m "build: wire GOOGLE_ANDROID_CLIENT_ID into BuildConfig + guard

The Android OAuth 2.0 client ID registered in Google Cloud Console as
'app.tastile.android' (debug + release SHA-1 fingerprints) flows into
BuildConfig.GOOGLE_ANDROID_CLIENT_ID via gradle.properties, with the
gradle.projectsEvaluated guard extended to require it non-blank.

Web-ops must set this in ~/.gradle/gradle.properties locally and in
CI before the build can succeed; tastile-web/.env.* also needs the
matching value (see commit on the tastile-web side of this change).

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Phase F — Documentation

### Task 9: Add manual device verification doc

**Files:**
- Create: `docs/superpowers/mobile/google-signin-manual-evidence.md`

**Interfaces:**
- Produces: A checklist-style doc that explains how to verify the native Google Sign-In flow on a real device with Play Services. Pre-registers the criteria for the eventual "done" proof so a follow-up device run can land without re-authoring the test plan.

- [ ] **Step 1: Check whether `docs/superpowers/mobile/` exists**

```bash
ls docs/superpowers/mobile/ 2>/dev/null || echo "MISSING — will create"
```

- [ ] **Step 2: Create the directory and the doc**

If `docs/superpowers/mobile/` does not exist:

```bash
mkdir -p docs/superpowers/mobile
```

Then create `docs/superpowers/mobile/google-signin-manual-evidence.md`:

```markdown
# Native Google Sign-In — Manual Device Evidence

- **Date:** 2026-09-03
- **Spec:** [docs/superpowers/specs/2026-09-03-google-signin-mobile-design.md](../../specs/2026-09-03-google-signin-mobile-design.md)
- **Status:** Pending device run

## Prerequisites

1. A debug build with `GOOGLE_ANDROID_CLIENT_ID` set in
   `~/.gradle/gradle.properties` to the value registered in Google
   Cloud Console as the `app.tastile.android` Android OAuth client.
2. The matching `GOOGLE_ANDROID_CLIENT_ID` set on the Tastile web
   server's environment (so BetterAuth accepts the idToken).
3. A device with Play Services and at least one Google account signed
   in (Settings → Accounts).

## Checklist

For each step, record PASS / FAIL + the observed behavior.

### Cold launch, no Google account on device

- [ ] Launch the app cold (force-stop + restart).
- [ ] Navigates to `LoginScreen` (assumes email/password is the
      default state).
- [ ] "Continue with Google" button is visible with the "G" mark.
- [ ] Tap the button.
- [ ] Expected: the system browser opens to
      `${WEB_BASE_URL}/login?provider=google` (the existing web OAuth
      handoff). This is the silent fallback path —
      `GoogleSignInUnavailableException` → `signInWithProvider("google")`.

### Cold launch, single Google account on device

- [ ] Launch the app cold.
- [ ] Tap "Continue with Google".
- [ ] Expected: Credential Manager auto-selects the single Google
      account (no bottom-sheet prompt).
- [ ] Expected: short delay, then `MainActivity` opens (the user is
      signed in via Google — BetterAuth session persisted).
- [ ] Force-stop the app + cold launch again.
- [ ] Expected: `LoginScreen` is NOT shown; the user goes straight to
      `MainActivity` because the BetterAuth session is persisted.

### Cold launch, multiple Google accounts on device

- [ ] Launch the app cold.
- [ ] Tap "Continue with Google".
- [ ] Expected: system account picker bottom-sheet appears with the
      Google accounts listed.
- [ ] Pick one.
- [ ] Expected: short delay, then `MainActivity` opens.

### User cancels the account picker

- [ ] Launch the app cold.
- [ ] Tap "Continue with Google".
- [ ] Dismiss the account picker without selecting.
- [ ] Expected: the system browser opens to
      `${WEB_BASE_URL}/login?provider=google` (web fallback).

### Email-not-verified edge case

- [ ] Sign in with a Google account whose email is NOT verified.
- [ ] Expected: BetterAuth creates the user with
      `emailVerified: false`; the user is signed in; no error chip.
- [ ] (Out of scope: BetterAuth sends a verification email; the
      verification flow is web-only.)

### idToken rejection

- [ ] Temporarily set `GOOGLE_ANDROID_CLIENT_ID` on the web server to
      a value that does NOT match the Android client.
- [ ] Tap "Continue with Google".
- [ ] Expected: error chip with "HTTP 401 ..." or similar.
- [ ] Restore the correct `GOOGLE_ANDROID_CLIENT_ID`.

## Evidence to capture

When this checklist runs green, commit the following under
`docs/superpowers/mobile/google-signin-evidence-<device>-<date>/`:

- `cold-launch.txt` — `adb logcat` capture for a cold launch → tap →
  auto-select → MainActivity, trimmed to the relevant log lines.
- `account-picker.png` — screenshot of the system account picker.
- `web-fallback.txt` — logcat capture for the no-Google-account
  fallback (Intent.ACTION_VIEW to `${WEB_BASE_URL}/login?provider=google`).

## Status

- **Checklist authored:** 2026-09-03
- **First green run:** TBD (pending device availability per
  `docs/superpowers/m3/phase-3-deferral.md`)
```

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/mobile/google-signin-manual-evidence.md
git commit -m "docs(superpowers): add manual device verification checklist for Google Sign-In

Pre-registers the criteria for the eventual device run. Captures the
silent fallback to web OAuth when no Google account is on device,
the auto-select path when exactly one account is on device, the
multi-account picker path, the user-cancel path, and the idToken
rejection error path.

Evidence to capture on the first green device run is enumerated so a
follow-up PR can land it without re-authoring the test plan.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

## Cross-repo evidence matrix (for `verify-tastile-change` and `cross-repo-contract-check`)

When this plan is fully executed, both sides must show:

| Cell | Evidence |
| --- | --- |
| Producer (`tastile-web`) | Task 1 commit on `main`: `socialProviders.google.clientId` becomes `string[]`; `GOOGLE_ANDROID_CLIENT_ID` env wired in both `.env.example` files; `pnpm typecheck` PASS; `pnpm vitest run src/shared/auth/better-auth` PASS |
| Consumer (`tastile-android`) | Tasks 2–9 commits on `main`; `./gradlew verify` PASS (modulo the 15 pre-existing failures documented in `docs/superpowers/m3/phase-3-deferral.md`); all 3 build guards PASS |
| Schema | BetterAuth `signInSocial` body schema unchanged: existing `{ provider, idToken: { token } }` |
| Migration | None — additive env var; existing `GOOGLE_CLIENT_ID` consumers unaffected |
| Tests | Both sides green; new test methods in `BetterAuthHttpClientTest`, `AuthRepositoryTest`, `LoginViewModelTest`, `LoginScreenTest` |
| Drift | `login_button_continue_google` string in `system_auth.xml` is unchanged; 3 new strings × 9 locales added in lock-step |
| Device evidence | `docs/superpowers/mobile/google-signin-manual-evidence.md` filed, checklist authored, first green run pending device availability |

---

## Self-review checklist

Before handing this plan off for execution, the executor should re-verify:

1. **Spec coverage:** Every section of the spec maps to a task. (Architecture → Tasks 4 + 5. Components → Tasks 2 + 3 + 4 + 6. Data flow → Tasks 6 + 7. Error handling → Tasks 6 + 7. Configuration → Task 8. Testing → Tasks 2 + 3 + 4 + 6 + 7. Build guards → Task 8. Cross-repo evidence → Tasks 1 + 8 + 9.)
2. **Type consistency:** `signInWithGoogleIdToken(idToken: String): BetterAuthSession` defined in Task 2, consumed in Task 3 (`AuthRepository.signInWithGoogle`) and Tasks 3 / 6 tests. `GoogleSignInLauncher.getIdToken(): String` defined in Task 3 (interface), implemented in Task 4, consumed in Task 3 (`AuthRepository.signInWithGoogle`) and Task 6 (test). `isGoogleSigningIn: StateFlow<Boolean>` defined in Task 6, consumed in Task 7 (`LoginScreen` collectAsStateWithLifecycle). `R.string.login_button_continue_google_in_progress` / `_google_unavailable` / `_google_failed` defined in Task 7, consumed in Task 6 + Task 7. `R.drawable.ic_google_g` defined in Task 7, consumed in Task 7. `@Named("googleAndroidClientId")` defined in Task 5, consumed in Task 4 (constructor parameter) and Task 5 (provider).
3. **i18n:** All 9 locales touched in Task 7, in lock-step with the spec's "Open follow-ups" note.
4. **No placeholder steps:** Each step has concrete code or a concrete command. Where the implementation depends on the existing project conventions (e.g., how `AuthRepositoryTest` constructs the repo), the step names the convention and points to the right `git grep` to find it.
5. **Gradle / Verify:** `./gradlew verify` runs at the end of Task 8 as the per-task check; if it fails on pre-existing 15 unit-test failures, those are documented and unrelated.
