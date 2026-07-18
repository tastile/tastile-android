package app.tastile.android.data.repository;

import app.tastile.android.data.api.CognitoAccountApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class AccountRepository_Factory implements Factory<AccountRepository> {
  private final Provider<CognitoAccountApi> accountApiProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private AccountRepository_Factory(Provider<CognitoAccountApi> accountApiProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.accountApiProvider = accountApiProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public AccountRepository get() {
    return newInstance(accountApiProvider.get(), authRepositoryProvider.get());
  }

  public static AccountRepository_Factory create(Provider<CognitoAccountApi> accountApiProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new AccountRepository_Factory(accountApiProvider, authRepositoryProvider);
  }

  public static AccountRepository newInstance(CognitoAccountApi accountApi,
      AuthRepository authRepository) {
    return new AccountRepository(accountApi, authRepository);
  }
}
