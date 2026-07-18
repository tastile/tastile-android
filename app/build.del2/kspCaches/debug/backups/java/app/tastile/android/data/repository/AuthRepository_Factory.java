package app.tastile.android.data.repository;

import android.content.Context;
import dagger.Lazy;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AuthRepository_Factory implements Factory<AuthRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<ApiTokenManager> apiTokenManagerProvider;

  private AuthRepository_Factory(Provider<Context> contextProvider,
      Provider<ApiTokenManager> apiTokenManagerProvider) {
    this.contextProvider = contextProvider;
    this.apiTokenManagerProvider = apiTokenManagerProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(contextProvider.get(), DoubleCheck.lazy(apiTokenManagerProvider));
  }

  public static AuthRepository_Factory create(Provider<Context> contextProvider,
      Provider<ApiTokenManager> apiTokenManagerProvider) {
    return new AuthRepository_Factory(contextProvider, apiTokenManagerProvider);
  }

  public static AuthRepository newInstance(Context context, Lazy<ApiTokenManager> apiTokenManager) {
    return new AuthRepository(context, apiTokenManager);
  }
}
