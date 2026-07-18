package app.tastile.android.data.repository;

import android.content.Context;
import app.tastile.android.data.api.V1ApiClient;
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
public final class ApiTokenManager_Factory implements Factory<ApiTokenManager> {
  private final Provider<Context> contextProvider;

  private final Provider<V1ApiClient> v1ApiClientProvider;

  private final Provider<CurrentUserProvider> currentUserProvider;

  private ApiTokenManager_Factory(Provider<Context> contextProvider,
      Provider<V1ApiClient> v1ApiClientProvider,
      Provider<CurrentUserProvider> currentUserProvider) {
    this.contextProvider = contextProvider;
    this.v1ApiClientProvider = v1ApiClientProvider;
    this.currentUserProvider = currentUserProvider;
  }

  @Override
  public ApiTokenManager get() {
    return newInstance(contextProvider.get(), DoubleCheck.lazy(v1ApiClientProvider), currentUserProvider.get());
  }

  public static ApiTokenManager_Factory create(Provider<Context> contextProvider,
      Provider<V1ApiClient> v1ApiClientProvider,
      Provider<CurrentUserProvider> currentUserProvider) {
    return new ApiTokenManager_Factory(contextProvider, v1ApiClientProvider, currentUserProvider);
  }

  public static ApiTokenManager newInstance(Context context, Lazy<V1ApiClient> v1ApiClient,
      CurrentUserProvider currentUser) {
    return new ApiTokenManager(context, v1ApiClient, currentUser);
  }
}
