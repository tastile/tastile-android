package app.tastile.android.data.auth

/**
 * Narrow, dependency-free read contract for "who is the current user" in the
 * data layer. Replaces the previous Cognito-typed accessor surface (id_token /
 * access_token) with the post-Cognito-migration shape: a BetterAuth session
 * token plus the parsed `sub` and `email` claims.
 *
 * The token is the bootstrap credential used by `ApiTokenCache` to mint a
 * Tastile API token. The `userId` / `email` accessors feed the UI shell and
 * the v1 read-model `owner_ids` filters.
 */
interface CurrentUserProvider {
    fun currentUserId(): String?
    fun currentEmail(): String?

    /**
     * Returns the BetterAuth session token used to mint / refresh the v1
     * API token. Returning `null` is the supported way to signal
     * "the user is not signed in".
     */
    fun currentSessionToken(): String?
}
