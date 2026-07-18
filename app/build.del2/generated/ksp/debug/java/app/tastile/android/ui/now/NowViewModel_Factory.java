package app.tastile.android.ui.now;

import app.tastile.android.domain.repository.AuthRepository;
import app.tastile.android.domain.repository.TileRepository;
import app.tastile.android.domain.usecase.GetTilesUseCase;
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
public final class NowViewModel_Factory implements Factory<NowViewModel> {
  private final Provider<GetTilesUseCase> getTilesProvider;

  private final Provider<TileRepository> tileRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private NowViewModel_Factory(Provider<GetTilesUseCase> getTilesProvider,
      Provider<TileRepository> tileRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.getTilesProvider = getTilesProvider;
    this.tileRepositoryProvider = tileRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public NowViewModel get() {
    return newInstance(getTilesProvider.get(), tileRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static NowViewModel_Factory create(Provider<GetTilesUseCase> getTilesProvider,
      Provider<TileRepository> tileRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new NowViewModel_Factory(getTilesProvider, tileRepositoryProvider, authRepositoryProvider);
  }

  public static NowViewModel newInstance(GetTilesUseCase getTiles, TileRepository tileRepository,
      AuthRepository authRepository) {
    return new NowViewModel(getTiles, tileRepository, authRepository);
  }
}
