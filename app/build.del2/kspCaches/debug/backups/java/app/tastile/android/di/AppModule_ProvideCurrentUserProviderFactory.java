package app.tastile.android.di;

import app.tastile.android.data.repository.AuthRepository;
import app.tastile.android.data.repository.CurrentUserProvider;
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
public final class AppModule_ProvideCurrentUserProviderFactory implements Factory<CurrentUserProvider> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private AppModule_ProvideCurrentUserProviderFactory(
      Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public CurrentUserProvider get() {
    return provideCurrentUserProvider(authRepositoryProvider.get());
  }

  public static AppModule_ProvideCurrentUserProviderFactory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new AppModule_ProvideCurrentUserProviderFactory(authRepositoryProvider);
  }

  public static CurrentUserProvider provideCurrentUserProvider(AuthRepository authRepository) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCurrentUserProvider(authRepository));
  }
}
