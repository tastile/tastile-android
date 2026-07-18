package app.tastile.android.di;

import app.tastile.android.domain.repository.AuthRepository;
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
public final class AppModule_ProvideDomainAuthRepositoryFactory implements Factory<AuthRepository> {
  private final Provider<app.tastile.android.data.repository.AuthRepository> authRepositoryProvider;

  private AppModule_ProvideDomainAuthRepositoryFactory(
      Provider<app.tastile.android.data.repository.AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public AuthRepository get() {
    return provideDomainAuthRepository(authRepositoryProvider.get());
  }

  public static AppModule_ProvideDomainAuthRepositoryFactory create(
      Provider<app.tastile.android.data.repository.AuthRepository> authRepositoryProvider) {
    return new AppModule_ProvideDomainAuthRepositoryFactory(authRepositoryProvider);
  }

  public static AuthRepository provideDomainAuthRepository(
      app.tastile.android.data.repository.AuthRepository authRepository) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDomainAuthRepository(authRepository));
  }
}
