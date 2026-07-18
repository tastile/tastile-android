package app.tastile.android.data.di;

import app.tastile.android.data.api.CognitoAccountApi;
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
public final class ApiModule_ProvideCognitoAccountApiFactory implements Factory<CognitoAccountApi> {
  private final Provider<ApiTokenManager> apiTokenManagerProvider;

  private ApiModule_ProvideCognitoAccountApiFactory(
      Provider<ApiTokenManager> apiTokenManagerProvider) {
    this.apiTokenManagerProvider = apiTokenManagerProvider;
  }

  @Override
  public CognitoAccountApi get() {
    return provideCognitoAccountApi(apiTokenManagerProvider.get());
  }

  public static ApiModule_ProvideCognitoAccountApiFactory create(
      Provider<ApiTokenManager> apiTokenManagerProvider) {
    return new ApiModule_ProvideCognitoAccountApiFactory(apiTokenManagerProvider);
  }

  public static CognitoAccountApi provideCognitoAccountApi(ApiTokenManager apiTokenManager) {
    return Preconditions.checkNotNullFromProvides(ApiModule.INSTANCE.provideCognitoAccountApi(apiTokenManager));
  }
}
