package app.tastile.android.di;

import app.tastile.android.data.repository.AuthRepository;
import app.tastile.android.data.repository.AuthRepositoryContract;
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
public final class AppModule_ProvideAuthRepositoryContractFactory implements Factory<AuthRepositoryContract> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private AppModule_ProvideAuthRepositoryContractFactory(
      Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public AuthRepositoryContract get() {
    return provideAuthRepositoryContract(authRepositoryProvider.get());
  }

  public static AppModule_ProvideAuthRepositoryContractFactory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new AppModule_ProvideAuthRepositoryContractFactory(authRepositoryProvider);
  }

  public static AuthRepositoryContract provideAuthRepositoryContract(
      AuthRepository authRepository) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAuthRepositoryContract(authRepository));
  }
}
