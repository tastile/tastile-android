package app.tastile.android.di;

import app.tastile.android.domain.repository.TileRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideDomainTileRepositoryFactory implements Factory<TileRepository> {
  private final Provider<app.tastile.android.data.repository.TileRepository> tileRepositoryProvider;

  private AppModule_ProvideDomainTileRepositoryFactory(
      Provider<app.tastile.android.data.repository.TileRepository> tileRepositoryProvider) {
    this.tileRepositoryProvider = tileRepositoryProvider;
  }

  @Override
  public TileRepository get() {
    return provideDomainTileRepository(tileRepositoryProvider.get());
  }

  public static AppModule_ProvideDomainTileRepositoryFactory create(
      Provider<app.tastile.android.data.repository.TileRepository> tileRepositoryProvider) {
    return new AppModule_ProvideDomainTileRepositoryFactory(tileRepositoryProvider);
  }

  public static TileRepository provideDomainTileRepository(
      app.tastile.android.data.repository.TileRepository tileRepository) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDomainTileRepository(tileRepository));
  }
}
