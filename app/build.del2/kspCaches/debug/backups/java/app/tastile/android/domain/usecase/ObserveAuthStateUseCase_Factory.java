package app.tastile.android.domain.usecase;

import app.tastile.android.domain.repository.AuthRepository;
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
public final class ObserveAuthStateUseCase_Factory implements Factory<ObserveAuthStateUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private ObserveAuthStateUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ObserveAuthStateUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static ObserveAuthStateUseCase_Factory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new ObserveAuthStateUseCase_Factory(authRepositoryProvider);
  }

  public static ObserveAuthStateUseCase newInstance(AuthRepository authRepository) {
    return new ObserveAuthStateUseCase(authRepository);
  }
}
