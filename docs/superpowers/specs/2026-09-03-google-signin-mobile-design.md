# Native Google Sign-In for Android — Design Spec

- **Date:** 2026-09-03
- **Status:** Approved (brainstorming sections 1–5 confirmed 2026-09-03)
- **Repos:** `tastile-android` (consumer), `tastile-web` (producer)
- **Workspace contract:** `../../AGENTS.md`
- **Cross-repo skill:** `cross-repo-contract-check`

## Background

The Android app's `LoginScreen` currently exposes only an email + password sign-in path. The string `login_button_continue_google` ("Continue with Google") is already in `app/src/main/res/values/system_auth.xml:9` but is never rendered. `AuthRepository.signInWithProvider("google")` exists as a web-browser handoff to `${WEB_BASE_URL}/login?provider=google`, which is the documented MVP fallback while the native OAuth bridge lands.

The user has asked to add a native Google Sign-In flow to the Android login surface. This spec covers the cross-repo change that delivers that.

## Goal

Add a native Google Sign-In path to `LoginScreen` that:

1. Uses Google's Credential Manager + Google Identity Services to obtain a Google ID token in a single tap.
2. Exchanges that idToken against BetterAuth's `/api/auth/sign-in/social` idToken branch on `tastile-web`.
3. Persists the resulting BetterAuth session identically to the email sign-in path.
4. Falls back transparently to the existing web-OAuth handoff when the device has no Google account, no Play Services, or the user cancels the system account picker.

## Non-goals

- Apple Sign-In (separate OAuth flow, deferred).
- Native Credential Manager flows on `iOS` (out of repo scope).
- New web-side sign-up screens for Google-only users (BetterAuth handles account creation server-side).
- Replacing the email/password sign-in path.
- Adding new analytics events.

## Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│ Tastile Android (Kotlin / Compose / Hilt)                            │
│                                                                      │
│  ┌────────────────┐    ┌──────────────────────┐    ┌──────────────┐ │
│  │  LoginScreen   │───▶│  LoginViewModel      │───▶│ AuthRepo     │ │
│  │  (Compose)     │    │  .signInWithGoogle() │    │ Contract     │ │
│  └────────────────┘    └──────────┬───────────┘    └──────┬───────┘ │
│         ▲                         │                       │         │
│         │ button tap              │                       ▼         │
│         │                         ▼              ┌────────────────┐ │
│         │              ┌──────────────────────┐   │ BetterAuth     │ │
│         │              │ GoogleSignInLauncher │   │ HttpClient     │ │
│         │              │  (Credential Mgr +   │   │ .signInWithGo  │ │
│         │              │   GoogleIdOption)    │   │  ogleIdToken() │ │
│         │              └──────────┬───────────┘   └────────┬───────┘ │
│         │                         │ idToken                │         │
│         │   fallback on failure  ▼                         │         │
│         │              ┌──────────────────────┐            │         │
│         └──────────────│ AuthRepo             │◀───────────┘         │
│            web handoff │ .signInWithProvider  │                      │
│                        │   ("google")         │                      │
│                        └──────────────────────┘                      │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  │ POST /api/auth/sign-in/social
                                  │ { provider: "google", idToken: { token } }
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│ Tastile Web (Next.js / BetterAuth)                                   │
│                                                                      │
│  socialProviders.google = {                                          │
│    clientId: [                                                       │
│      process.env.GOOGLE_CLIENT_ID,         // web OAuth client       │
│      process.env.GOOGLE_ANDROID_CLIENT_ID, // android OAuth client   │
│    ],                                                                │
│    clientSecret: process.env.GOOGLE_CLIENT_SECRET,                   │
│  }                                                                   │
│                                                                      │
│  POST /api/auth/sign-in/social → validates idToken.aud against       │
│  either clientId, mints BetterAuth session cookie.                   │
└──────────────────────────────────────────────────────────────────────┘
```

## Components

### 1. `GoogleSignInLauncher` (new interface + impl)

**File:** `app/src/main/java/app/tastile/android/data/auth/GoogleSignInLauncher.kt`

```kotlin
interface GoogleSignInLauncher {
    /**
     * Returns a Google ID token. Throws [GoogleSignInUnavailableException]
     * when no Google account is available / Play Services missing / user
     * cancels. Throws [GoogleSignInFailedException] for all other failures.
     */
    suspend fun getIdToken(): String
}

class CredentialManagerGoogleSignInLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("googleAndroidClientId") private val serverClientId: String,
) : GoogleSignInLauncher {
    private val credentialManager = CredentialManager.create(context)

    override suspend fun getIdToken(): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)  // first-time users too
            .setAutoSelectEnabled(true)            // auto-pick if single account
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
            throw GoogleSignInFailedException("Unexpected credential type: ${credential::class}")
        }
        return credential.idToken
            ?: throw GoogleSignInFailedException("GoogleIdTokenCredential.idToken is null")
    }
}

class GoogleSignInUnavailableException(cause: Throwable) : RuntimeException(cause)
class GoogleSignInFailedException(message: String) : RuntimeException(message)
```

### 2. `BetterAuthHttpClient.signInWithGoogleIdToken(idToken)` (new method)

**File:** `app/src/main/java/app/tastile/android/data/auth/BetterAuthHttpClient.kt`

```kotlin
suspend fun signInWithGoogleIdToken(idToken: String): BetterAuthSession =
    withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("provider", JsonPrimitive("google"))
            put("idToken", buildJsonObject {
                put("token", JsonPrimitive(idToken))
            })
        }
        val response = postJson("/api/auth/sign-in/social", body)
        val sessionToken = response.cookies.firstNotNullOfOrNull {
            extractSessionToken(listOf(it))
        } ?: throw BetterAuthException("Google sign-in response missing session cookie")
        decodeSession(sessionToken, response.body)
    }
```

Reuses existing `postJson`, `extractSessionToken`, `decodeSession`, `JsonResponse`.

### 3. `AuthRepositoryContract.signInWithGoogle()` (new method)

**File:** `app/src/main/java/app/tastile/android/data/auth/AuthRepositoryContract.kt`

```kotlin
/**
 * Native Google Sign-In via Credential Manager. Exchanges the Google
 * idToken against BetterAuth's /api/auth/sign-in/social idToken branch,
 * then persists the resulting session identically to email sign-in.
 * Throws GoogleSignInUnavailableException when no Google account is
 * available (caller should fall back to signInWithProvider("google")).
 */
suspend fun signInWithGoogle()
```

### 4. `AuthRepository.signInWithGoogle()` (impl)

**File:** `app/src/main/java/app/tastile/android/data/auth/AuthRepository.kt`

```kotlin
override suspend fun signInWithGoogle() {
    val idToken = googleSignInLauncher.getIdToken()  // throws on failure
    val session = httpClient.signInWithGoogleIdToken(idToken)
    persistSession(session)                          // existing
}
```

`GoogleSignInLauncher` is injected as a constructor parameter (added to `@Inject`).

### 5. `LoginViewModel.signInWithGoogle()` (new)

**File:** `app/src/main/java/app/tastile/android/ui/login/LoginViewModel.kt`

```kotlin
private val _isGoogleSigningIn = MutableStateFlow(false)
val isGoogleSigningIn: StateFlow<Boolean> = _isGoogleSigningIn.asStateFlow()

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
            // Fall back to web OAuth handoff.
            try {
                authRepository.signInWithProvider("google")
            } catch (fallback: Exception) {
                _error.value = context.getString(R.string.login_error_google_unavailable)
            }
        } catch (e: Exception) {
            _error.value = e.message ?: context.getString(R.string.login_error_google_failed)
        } finally {
            _isGoogleSigningIn.value = false
        }
    }
}
```

### 6. `LoginScreen` UI changes

**File:** `app/src/main/java/app/tastile/android/ui/login/LoginScreen.kt`

- Add a single "Continue with Google" button between the email/password block and the existing "Create an account" link.
- Outlined style matching the existing email/password inputs; leading edge shows a Google "G" mark vector drawable.
- Button enabled state: `!isSigningIn && !isGoogleSigningIn`.
- New testTag `login-google-button` for Compose UI tests.
- Reuses the existing error chip and `onLoginSuccess` flow.

### 7. Hilt wiring (new)

**File:** `app/src/main/java/app/tastile/android/di/AuthModule.kt` (new)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideGoogleSignInLauncher(
        @ApplicationContext context: Context,
        @Named("googleAndroidClientId") clientId: String,
    ): GoogleSignInLauncher = CredentialManagerGoogleSignInLauncher(context, clientId)

    @Provides
    @Named("googleAndroidClientId")
    fun provideGoogleAndroidClientId(): String =
        BuildConfig.GOOGLE_ANDROID_CLIENT_ID
}
```

### 8. Web-side changes (`tastile-web`)

**File:** `src/shared/auth/better-auth/server.ts`

```diff
 function socialProviders() {
   const providers: Record<string, unknown> = {};
   const googleClientId = process.env.GOOGLE_CLIENT_ID?.trim();
   const googleClientSecret = process.env.GOOGLE_CLIENT_SECRET?.trim();
+  const googleAndroidClientId = process.env.GOOGLE_ANDROID_CLIENT_ID?.trim();
   if (googleClientId && googleClientSecret) {
-    providers.google = { clientId: googleClientId, clientSecret: googleClientSecret };
+    const clientIds = googleAndroidClientId
+      ? [googleClientId, googleAndroidClientId]
+      : [googleClientId];
+    providers.google = { clientId: clientIds, clientSecret: googleClientSecret };
   }
   // ...
 }
```

BetterAuth's `GoogleOptions.clientId: string | string[]` already accepts an array (confirmed in `@better-auth/core/dist/social-providers/google.d.mts`).

## Data flow

### Happy path (returning Google user, single tap)

```
LoginScreen → user taps "Continue with Google"
  → LoginViewModel.signInWithGoogle(context) sets _isGoogleSigningIn = true
  → AuthRepository.signInWithGoogle()
  → GoogleSignInLauncher.getIdToken()
      (CredentialManager shows system account picker; user picks account)
  → BetterAuthHttpClient.signInWithGoogleIdToken(idToken)
      POST /api/auth/sign-in/social { provider: "google", idToken: { token } }
      Set-Cookie: better-auth.session_token=...
  → AuthRepository.persistSession(session)
      EncryptedTokenStorage writes session_token / user_id / email
      apiTokenCache.invalidate()
      _authState.value = Authenticated(userId, email)
  → LoginViewModel collects authState → Authenticated
  → LoginScreen.onLoginSuccess() → MainActivity
```

### Fallback path (no Google account / Play Services missing / cancel)

```
LoginScreen → user taps "Continue with Google"
  → LoginViewModel.signInWithGoogle(context)
  → GoogleSignInLauncher.getIdToken()
      throws GetCredentialException → wrapped as GoogleSignInUnavailableException
  → VM catches → authRepository.signInWithProvider("google")
      opens Intent.ACTION_VIEW to ${WEB_BASE_URL}/login?provider=google
  → BetterAuth web OAuth flow handles the rest in Chrome.
```

User-cancel is treated as fallback to web OAuth (user said "no, not via Google" → offer the alternative path). Silent fallback: no error chip.

### Server-error path

```
BetterAuthHttpClient.signInWithGoogleIdToken()
  POST returns non-2xx (e.g. 401)
  throws BetterAuthException("POST ... failed: HTTP 401 ...")
→ VM catches → _error.value = message
→ LoginScreen renders error chip
→ user can retry
```

## Error handling matrix

| Source | Caught as | UX |
| --- | --- | --- |
| No Google account on device | `GoogleSignInUnavailableException` | Silent fallback to web OAuth — no error chip |
| User cancels Credential Manager | `GoogleSignInUnavailableException` | Silent fallback to web OAuth |
| Play Services missing | `GoogleSignInUnavailableException` | Silent fallback to web OAuth |
| Network error during POST | `BetterAuthException` | Error chip with message |
| Server returns 401 | `BetterAuthException("HTTP 401 ...")` | Error chip: "Unable to verify Google account" |
| Server returns 500 | `BetterAuthException` | Error chip with default "Unable to sign in" |
| Email not verified | BetterAuth session created; verification email sent by web | Authenticated state; verification flow is web-only and out of scope |
| HTTP timeout (15s) | `IOException` → generic `catch (e: Exception)` | Error chip |
| BuildConfig field blank | `gradle.projectsEvaluated` guard fails the build | Build-time, not runtime |

### New strings — all 9 locales

The existing `system_auth.xml` is translated across 9 locales: `values/` (English fallback), `values-de/`, `values-es/`, `values-fr/`, `values-ja/`, `values-ko/`, `values-pt-rBR/`, `values-zh-rCN/`. The new strings must land in **all 9** in the same commit, otherwise the `verifyV1ApiCoverage`-style guard or `aapt2` may warn on missing translations.

Add to all 9 files:

```xml
<string name="login_error_google_unavailable">Google Sign-In isn\'t available on this device</string>
<string name="login_error_google_failed">Unable to verify Google account</string>
<string name="login_button_continue_google_in_progress">Connecting to Google…</string>
```

English fallbacks (above) go in `values/system_auth.xml`. The other 8 locales get translations per the existing convention in each file. Japanese example:

```xml
<string name="login_error_google_unavailable">この端末では Google サインインを利用できません</string>
<string name="login_error_google_failed">Google アカウントを確認できません</string>
<string name="login_button_continue_google_in_progress">Google に接続中…</string>
```

The existing `login_button_continue_google` is already in all 9 locales (verified: `"Continue with Google"` in English, `"Google で続ける"` in Japanese) and is reused.

### Idempotency on retry

- `_isGoogleSigningIn` rejects the second tap (existing pattern from email sign-in).
- After a failure, `_isGoogleSigningIn` is reset to `false` in the `finally` block; user can retry.
- `_isSigningIn` (existing email/password) is independent; only one path active at a time.

## Configuration

### `app/build.gradle.kts`

```diff
 val googleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID")
+val googleAndroidClientId = providers.gradleProperty("GOOGLE_ANDROID_CLIENT_ID")
 ...
         buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleWebClientId.orNull ?: ""}\"")
+        buildConfigField("String", "GOOGLE_ANDROID_CLIENT_ID", "\"${googleAndroidClientId.orNull ?: ""}\"")
 ...
-        "GOOGLE_WEB_CLIENT_ID",
+        "GOOGLE_WEB_CLIENT_ID",
+        "GOOGLE_ANDROID_CLIENT_ID",
```

Both fields are guarded by the existing `gradle.projectsEvaluated` build-time hard requirement (per CLAUDE.md).

### Required env / Gradle properties

- `gradle.properties` (CI) or `~/.gradle/gradle.properties` (local dev) or `-PKEY=value`:
  - `GOOGLE_ANDROID_CLIENT_ID` — Android OAuth 2.0 client ID registered in Google Cloud Console as `app.tastile.android` with debug + release SHA-1 fingerprints.
- `tastile-web` env (`tastile-web/.env.development`, `tastile-web/.env.production`):
  - `GOOGLE_ANDROID_CLIENT_ID` — same value.

### GCP registration (operational, not part of this PR)

Documented for web-ops / infra owner:

1. Open Google Cloud Console → APIs & Services → Credentials.
2. Create Credentials → OAuth 2.0 Client IDs → Application type: **Android**.
3. Package name: `app.tastile.android`.
4. SHA-1 certificate fingerprint (debug):
   - Run: `./gradlew :app:signingReport` and copy the SHA1 of `debug` variant.
5. SHA-1 certificate fingerprint (release): copy from the release keystore used for `bundleRelease` (per `docs/operations/release-plan.md`).
6. Copy the resulting Client ID into `gradle.properties` (local + CI) and `tastile-web/.env.*`.

## Testing

### JVM unit tests (no device required)

| Test | Verifies |
| --- | --- |
| `LoginViewModelTest.signInWithGoogle_launchesAuthRepo` | VM calls `authRepository.signInWithGoogle()`; state transitions |
| `LoginViewModelTest.signInWithGoogle_unavailableFallback` | VM catches `GoogleSignInUnavailableException` → calls `signInWithProvider("google")` |
| `LoginViewModelTest.signInWithGoogle_serverError` | VM catches `BetterAuthException` → error chip set |
| `LoginViewModelTest.signInWithGoogle_cancelFallsBack` | Cancel → fallback to web OAuth |
| `BetterAuthHttpClientTest.signInWithGoogleIdToken_postsCorrectBody` | POST body shape via MockWebServer |
| `BetterAuthHttpClientTest.signInWithGoogleIdToken_401Throws` | Non-2xx → `BetterAuthException` |
| `AuthRepositoryTest.signInWithGoogle_persistsSession` | launcher → http → persistSession in order |

### Compose UI tests (Robolectric, no device)

| Test | Verifies |
| --- | --- |
| `LoginScreenTest.continueWithGoogleButton_isDisplayed` | Button visible with string + testTag `login-google-button` |
| `LoginScreenTest.continueWithGoogleButton_triggersSignIn` | Tap → `LoginViewModel.signInWithGoogle()` invoked |
| `LoginScreenTest.continueWithGoogleButton_disabledWhileSigningIn` | Button disabled when `_isGoogleSigningIn.value == true` |

### Manual device verification

Captured as a follow-up, not a CI gate. Per the project's Phase 3 deferral pattern (commit `c8e73e5`), the credential flow cannot be exercised in CI; the evidence file documents what was run.

- `docs/superpowers/mobile/google-signin-manual-evidence.md` — checklist of what to test on a real device with the new `GOOGLE_ANDROID_CLIENT_ID`.

### Test seams

- `GoogleSignInLauncher` is an interface; unit tests inject a fake returning a canned idToken.
- `BetterAuthHttpClient` is tested against `MockWebServer` (already in `testImplementation`).
- `AuthRepository` is tested with both `GoogleSignInLauncher` and `BetterAuthHttpClient` faked.

## Build guards

- `verifyDesignSystemImports` — new `LoginScreen` content uses `OutlinedButton` + `Icon` from the design system; no direct `androidx.compose.material3.*` imports outside the `core/designsystem/` boundary.
- `verifyNoEmbeddedServerSecrets` — Android OAuth client ID is public, no secret material introduced.
- `verifyV1ApiCoverage` — N/A (auth, not v1 API).
- `./gradlew verify` — must pass before claiming DONE.

## Cross-repo evidence matrix

| Cell | Required evidence |
| --- | --- |
| Producer (`tastile-web`) | Diff that changes `clientId: string` → `clientId: string[]`; reads `GOOGLE_ANDROID_CLIENT_ID`; `pnpm typecheck` + relevant Vitest pass |
| Consumer (`tastile-android`) | New `GOOGLE_ANDROID_CLIENT_ID` BuildConfig wired; sign-in flow + JVM unit tests pass; `./gradlew verify` green |
| Schema | BetterAuth `signInSocial` body schema is unchanged (existing `{ provider, idToken: { token } }`) |
| Migration | None — additive env var; existing `GOOGLE_CLIENT_ID` consumers unaffected |
| Tests | Both sides green |
| Drift | `login_button_continue_google` string in `app/src/main/res/values/system_auth.xml` stays English-only until `values-ja/system_auth.xml` is updated (file may not exist — checked during plan phase) |

## Open follow-ups (out of scope)

- Apple Sign-In — separate OAuth provider; deferred.
- Telemetry — no Android telemetry per CLAUDE.md.
- Saving the Google account to `EncryptedTokenStorage` for auto-reauth on next launch — out of scope; user re-taps the button each time.
- A second pass on the new error strings for tone / phrasing per locale (initial translations land in this PR; the project's i18n review can iterate afterwards).
