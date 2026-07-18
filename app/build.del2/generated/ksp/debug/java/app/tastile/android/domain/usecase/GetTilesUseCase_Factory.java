package app.tastile.android.domain.usecase;

import app.tastile.android.domain.repository.TileRepository;
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
public final class GetTilesUseCase_Factory implements Factory<GetTilesUseCase> {
  private final Provider<TileRepository> tileRepositoryProvider;

  private GetTilesUseCase_Factory(Provider<TileRepository> tileRepositoryProvider) {
    this.tileRepositoryProvider = tileRepositoryProvider;
  }

  @Override
  public GetTilesUseCase get() {
    return newInstance(tileRepositoryProvider.get());
  }

  public static GetTilesUseCase_Factory create(Provider<TileRepository> tileRepositoryProvider) {
    return new GetTilesUseCase_Factory(tileRepositoryProvider);
  }

  public static GetTilesUseCase newInstance(TileRepository tileRepository) {
    return new GetTilesUseCase(tileRepository);
  }
}
