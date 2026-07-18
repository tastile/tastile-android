package app.tastile.android.data.di;

import app.tastile.android.data.api.V1ApiClient;
import app.tastile.android.data.command.V1CommandDispatcher;
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
public final class ApiModule_ProvideV1CommandDispatcherFactory implements Factory<V1CommandDispatcher> {
  private final Provider<V1ApiClient> v1ApiClientProvider;

  private ApiModule_ProvideV1CommandDispatcherFactory(Provider<V1ApiClient> v1ApiClientProvider) {
    this.v1ApiClientProvider = v1ApiClientProvider;
  }

  @Override
  public V1CommandDispatcher get() {
    return provideV1CommandDispatcher(v1ApiClientProvider.get());
  }

  public static ApiModule_ProvideV1CommandDispatcherFactory create(
      Provider<V1ApiClient> v1ApiClientProvider) {
    return new ApiModule_ProvideV1CommandDispatcherFactory(v1ApiClientProvider);
  }

  public static V1CommandDispatcher provideV1CommandDispatcher(V1ApiClient v1ApiClient) {
    return Preconditions.checkNotNullFromProvides(ApiModule.INSTANCE.provideV1CommandDispatcher(v1ApiClient));
  }
}
