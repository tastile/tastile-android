package app.tastile.android.di;

import app.tastile.android.data.repository.MemoTileRepository;
import app.tastile.android.data.repository.TileRepository;
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
public final class AppModule_ProvideMemoTileRepositoryFactory implements Factory<MemoTileRepository> {
  private final Provider<TileRepository> tileRepositoryProvider;

  private AppModule_ProvideMemoTileRepositoryFactory(
      Provider<TileRepository> tileRepositoryProvider) {
    this.tileRepositoryProvider = tileRepositoryProvider;
  }

  @Override
  public MemoTileRepository get() {
    return provideMemoTileRepository(tileRepositoryProvider.get());
  }

  public static AppModule_ProvideMemoTileRepositoryFactory create(
      Provider<TileRepository> tileRepositoryProvider) {
    return new AppModule_ProvideMemoTileRepositoryFactory(tileRepositoryProvider);
  }

  public static MemoTileRepository provideMemoTileRepository(TileRepository tileRepository) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMemoTileRepository(tileRepository));
  }
}
