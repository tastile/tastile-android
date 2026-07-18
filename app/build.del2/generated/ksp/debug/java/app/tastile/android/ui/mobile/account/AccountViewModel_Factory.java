package app.tastile.android.ui.mobile.account;

import app.tastile.android.data.repository.AccountRepository;
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
  private final Provider<AccountRepository> accountRepositoryProvider;

  private AccountViewModel_Factory(Provider<AccountRepository> accountRepositoryProvider) {
    this.accountRepositoryProvider = accountRepositoryProvider;
  }

  @Override
  public AccountViewModel get() {
    return newInstance(accountRepositoryProvider.get());
  }

  public static AccountViewModel_Factory create(
      Provider<AccountRepository> accountRepositoryProvider) {
    return new AccountViewModel_Factory(accountRepositoryProvider);
  }

  public static AccountViewModel newInstance(AccountRepository accountRepository) {
    return new AccountViewModel(accountRepository);
  }
}
