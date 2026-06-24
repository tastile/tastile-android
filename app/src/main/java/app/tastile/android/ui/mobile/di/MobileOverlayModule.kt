package app.tastile.android.ui.mobile.di

import app.tastile.android.ui.mobile.OverlayViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object MobileOverlayModule {

    @Provides
    @ActivityRetainedScoped
    fun provideOverlayViewModel(): OverlayViewModel = OverlayViewModel()
}
