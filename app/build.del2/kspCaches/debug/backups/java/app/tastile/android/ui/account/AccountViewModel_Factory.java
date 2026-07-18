package app.tastile.android.ui.account;

import app.tastile.android.data.repository.AuthRepository;
import app.tastile.android.data.repository.ProfileRepository;
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
public final class AccountViewModel_Factory implements Factory<AccountViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private AccountViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public AccountViewModel get() {
    return newInstance(authRepositoryProvider.get(), profileRepositoryProvider.get());
  }

  public static AccountViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new AccountViewModel_Factory(authRepositoryProvider, profileRepositoryProvider);
  }

  public static AccountViewModel newInstance(AuthRepository authRepository,
      ProfileRepository profileRepository) {
    return new AccountViewModel(authRepository, profileRepository);
  }
}
