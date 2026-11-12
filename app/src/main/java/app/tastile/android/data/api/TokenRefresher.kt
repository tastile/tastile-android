package app.tastile.android.data.api

/**
 * Discards a Core-rejected bearer token and returns a replacement minted
 * through `POST /api/mobile/api-token`.
 *
 * Implementations must be single-flight: parallel 401s for the same stale
 * token share one re-mint (success or failure) instead of stampeding the
 * mint endpoint. [app.tastile.android.data.auth.ApiTokenCache] is the
 * production implementation; tests use fakes/mocks.
 */
fun interface TokenRefresher {
    /**
     * @param staleToken the token Core just answered 401 with; it must be
     *   discarded before re-minting.
     * @return the replacement token, or `null` when re-minting failed
     *   (bounded retries with backoff exhausted, or no BetterAuth session).
     *   On `null` the implementation has already transitioned the auth
     *   state to unauthenticated — callers surface [V1Error.Auth] for the
     *   in-flight request and must not retry further.
     *
     * Implementations must keep the work bounded per call: one re-mint
     * round only (the production implementation allows
     * `MAX_REMINT_ATTEMPTS` attempts with linear backoff), shared
     * single-flight across concurrent callers, and fail-fast while the
     * auth state is unauthenticated (android#92).
     */
    suspend fun refreshAfterUnauthorized(staleToken: String): String?
}
