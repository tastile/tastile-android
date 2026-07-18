package app.tastile.android.data.repository;

import app.tastile.android.data.api.V1ApiClient;
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
public final class IntegrationRepository_Factory implements Factory<IntegrationRepository> {
  private final Provider<CurrentUserProvider> currentUserProvider;

  private final Provider<V1ApiClient> v1ApiClientProvider;

  private IntegrationRepository_Factory(Provider<CurrentUserProvider> currentUserProvider,
      Provider<V1ApiClient> v1ApiClientProvider) {
    this.currentUserProvider = currentUserProvider;
    this.v1ApiClientProvider = v1ApiClientProvider;
  }

  @Override
  public IntegrationRepository get() {
    return newInstance(currentUserProvider.get(), v1ApiClientProvider.get());
  }

  public static IntegrationRepository_Factory create(
      Provider<CurrentUserProvider> currentUserProvider,
      Provider<V1ApiClient> v1ApiClientProvider) {
    return new IntegrationRepository_Factory(currentUserProvider, v1ApiClientProvider);
  }

  public static IntegrationRepository newInstance(CurrentUserProvider currentUserProvider,
      V1ApiClient v1ApiClient) {
    return new IntegrationRepository(currentUserProvider, v1ApiClient);
  }
}
