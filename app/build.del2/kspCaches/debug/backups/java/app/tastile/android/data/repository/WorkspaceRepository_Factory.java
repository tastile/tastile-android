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
public final class WorkspaceRepository_Factory implements Factory<WorkspaceRepository> {
  private final Provider<V1ApiClient> v1ApiClientProvider;

  private WorkspaceRepository_Factory(Provider<V1ApiClient> v1ApiClientProvider) {
    this.v1ApiClientProvider = v1ApiClientProvider;
  }

  @Override
  public WorkspaceRepository get() {
    return newInstance(v1ApiClientProvider.get());
  }

  public static WorkspaceRepository_Factory create(Provider<V1ApiClient> v1ApiClientProvider) {
    return new WorkspaceRepository_Factory(v1ApiClientProvider);
  }

  public static WorkspaceRepository newInstance(V1ApiClient v1ApiClient) {
    return new WorkspaceRepository(v1ApiClient);
  }
}
