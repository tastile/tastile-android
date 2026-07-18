package app.tastile.android.ui.prompt;

import app.tastile.android.data.repository.CurrentUserProvider;
import app.tastile.android.data.repository.PromptTileRepository;
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
public final class PromptViewModel_Factory implements Factory<PromptViewModel> {
  private final Provider<PromptTileRepository> tileRepositoryProvider;

  private final Provider<CurrentUserProvider> currentUserProvider;

  private PromptViewModel_Factory(Provider<PromptTileRepository> tileRepositoryProvider,
      Provider<CurrentUserProvider> currentUserProvider) {
    this.tileRepositoryProvider = tileRepositoryProvider;
    this.currentUserProvider = currentUserProvider;
  }

  @Override
  public PromptViewModel get() {
    return newInstance(tileRepositoryProvider.get(), currentUserProvider.get());
  }

  public static PromptViewModel_Factory create(
      Provider<PromptTileRepository> tileRepositoryProvider,
      Provider<CurrentUserProvider> currentUserProvider) {
    return new PromptViewModel_Factory(tileRepositoryProvider, currentUserProvider);
  }

  public static PromptViewModel newInstance(PromptTileRepository tileRepository,
      CurrentUserProvider currentUserProvider) {
    return new PromptViewModel(tileRepository, currentUserProvider);
  }
}
