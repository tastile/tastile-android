package app.tastile.android.data.repository

/**
 * Backwards-compatible typealiases for the data-layer production repositories.
 *
 * Per R27 of the Android architecture audit (`docs/audit/2026-07-16-android-archdoc-baseline.md`):
 * concrete implementations must carry a `Default` kind-prefix so readers can tell at
 * a glance what they are. The renames below (`AccountRepository` -> `DefaultAccountRepository`,
 * etc.) are the canonical names; the unprefixed aliases are kept here so existing
 * call sites in `MainActivity`, `notifications/`, `di/AppModule.kt`, and the data-layer
 * test files continue to compile without churn.
 *
 * New code should import the `Default*` class directly.
 */
typealias AccountRepository = DefaultAccountRepository
typealias AuthRepository = DefaultAuthRepository
typealias EventRepository = DefaultEventRepository
typealias IntegrationRepository = DefaultIntegrationRepository
typealias ProfileRepository = DefaultProfileRepository
typealias TileRepository = DefaultTileRepository
typealias UserSettingsRepository = DefaultUserSettingsRepository
typealias WorkspaceRepository = DefaultWorkspaceRepository