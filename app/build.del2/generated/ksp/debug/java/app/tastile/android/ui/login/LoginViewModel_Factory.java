package app.tastile.android.ui.login;

import app.tastile.android.data.repository.AuthRepositoryContract;
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
public final class LoginViewModel_Factory implements Factory<LoginViewModel> {
  private final Provider<AuthRepositoryContract> authRepositoryProvider;

  private LoginViewModel_Factory(Provider<AuthRepositoryContract> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public LoginViewModel get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static LoginViewModel_Factory create(
      Provider<AuthRepositoryContract> authRepositoryProvider) {
    return new LoginViewModel_Factory(authRepositoryProvider);
  }

  public static LoginViewModel newInstance(AuthRepositoryContract authRepository) {
    return new LoginViewModel(authRepository);
  }
}
