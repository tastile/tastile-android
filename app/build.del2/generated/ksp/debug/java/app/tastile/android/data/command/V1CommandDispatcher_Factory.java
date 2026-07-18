package app.tastile.android.data.command;

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
public final class V1CommandDispatcher_Factory implements Factory<V1CommandDispatcher> {
  private final Provider<V1ApiClient> v1ApiClientProvider;

  private V1CommandDispatcher_Factory(Provider<V1ApiClient> v1ApiClientProvider) {
    this.v1ApiClientProvider = v1ApiClientProvider;
  }

  @Override
  public V1CommandDispatcher get() {
    return newInstance(v1ApiClientProvider.get());
  }

  public static V1CommandDispatcher_Factory create(Provider<V1ApiClient> v1ApiClientProvider) {
    return new V1CommandDispatcher_Factory(v1ApiClientProvider);
  }

  public static V1CommandDispatcher newInstance(V1ApiClient v1ApiClient) {
    return new V1CommandDispatcher(v1ApiClient);
  }
}
