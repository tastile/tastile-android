package app.tastile.android.ui.dashboard;

import app.tastile.android.data.repository.AuthRepository;
import app.tastile.android.data.repository.ProfileRepository;
import app.tastile.android.data.repository.ReferenceOverlayStore;
import app.tastile.android.data.repository.TileRepository;
import app.tastile.android.data.repository.UserSettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<TileRepository> tileRepositoryProvider;

  private final Provider<UserSettingsRepository> userSettingsRepositoryProvider;

  private final Provider<ReferenceOverlayStore> referenceOverlayStoreProvider;

  private DashboardViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<TileRepository> tileRepositoryProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<ReferenceOverlayStore> referenceOverlayStoreProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.tileRepositoryProvider = tileRepositoryProvider;
    this.userSettingsRepositoryProvider = userSettingsRepositoryProvider;
    this.referenceOverlayStoreProvider = referenceOverlayStoreProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(authRepositoryProvider.get(), profileRepositoryProvider.get(), tileRepositoryProvider.get(), userSettingsRepositoryProvider.get(), referenceOverlayStoreProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<TileRepository> tileRepositoryProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<ReferenceOverlayStore> referenceOverlayStoreProvider) {
    return new DashboardViewModel_Factory(authRepositoryProvider, profileRepositoryProvider, tileRepositoryProvider, userSettingsRepositoryProvider, referenceOverlayStoreProvider);
  }

  public static DashboardViewModel newInstance(AuthRepository authRepository,
      ProfileRepository profileRepository, TileRepository tileRepository,
      UserSettingsRepository userSettingsRepository, ReferenceOverlayStore referenceOverlayStore) {
    return new DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore);
  }
}
