package app.tastile.android.ui.memo;

import app.tastile.android.data.repository.CurrentUserProvider;
import app.tastile.android.data.repository.MemoTileRepository;
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
public final class MemoViewModel_Factory implements Factory<MemoViewModel> {
  private final Provider<MemoTileRepository> tileRepositoryProvider;

  private final Provider<CurrentUserProvider> currentUserProvider;

  private MemoViewModel_Factory(Provider<MemoTileRepository> tileRepositoryProvider,
      Provider<CurrentUserProvider> currentUserProvider) {
    this.tileRepositoryProvider = tileRepositoryProvider;
    this.currentUserProvider = currentUserProvider;
  }

  @Override
  public MemoViewModel get() {
    return newInstance(tileRepositoryProvider.get(), currentUserProvider.get());
  }

  public static MemoViewModel_Factory create(Provider<MemoTileRepository> tileRepositoryProvider,
      Provider<CurrentUserProvider> currentUserProvider) {
    return new MemoViewModel_Factory(tileRepositoryProvider, currentUserProvider);
  }

  public static MemoViewModel newInstance(MemoTileRepository tileRepository,
      CurrentUserProvider currentUserProvider) {
    return new MemoViewModel(tileRepository, currentUserProvider);
  }
}
