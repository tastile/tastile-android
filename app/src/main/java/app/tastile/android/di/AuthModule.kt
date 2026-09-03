package app.tastile.android.di

import app.tastile.android.data.auth.GoogleSignInLauncher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Stub Hilt binding for `GoogleSignInLauncher`. The real implementation
 * (wrapping Credential Manager + Google Identity Services) lands in
 * Task 5 of the plan `docs/superpowers/plans/2026-09-03-google-signin-mobile.md`
 * and replaces this @Provides body. The stub keeps the Hilt graph valid
 * between Tasks 3 and 5; it throws at runtime if invoked (safe because
 * nothing in main calls signInWithGoogle() until Task 6).
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideGoogleSignInLauncher(): GoogleSignInLauncher =
        error("GoogleSignInLauncher implementation lands in Task 5")
}
