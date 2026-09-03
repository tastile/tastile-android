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
