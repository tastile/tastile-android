package app.tastile.android.data.di;

import app.tastile.android.data.api.V1ApiClient;
import app.tastile.android.data.repository.ApiTokenManager;
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
public final class ApiModule_ProvideV1ApiClientFactory implements Factory<V1ApiClient> {
  private final Provider<ApiTokenManager> apiTokenManagerProvider;

  private ApiModule_ProvideV1ApiClientFactory(Provider<ApiTokenManager> apiTokenManagerProvider) {
    this.apiTokenManagerProvider = apiTokenManagerProvider;
  }

  @Override
  public V1ApiClient get() {
    return provideV1ApiClient(apiTokenManagerProvider.get());
  }

  public static ApiModule_ProvideV1ApiClientFactory create(
      Provider<ApiTokenManager> apiTokenManagerProvider) {
    return new ApiModule_ProvideV1ApiClientFactory(apiTokenManagerProvider);
  }

  public static V1ApiClient provideV1ApiClient(ApiTokenManager apiTokenManager) {
    return Preconditions.checkNotNullFromProvides(ApiModule.INSTANCE.provideV1ApiClient(apiTokenManager));
  }
}
