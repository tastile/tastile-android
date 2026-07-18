package app.tastile.android.data.repository;

import app.tastile.android.data.api.V1ApiClient;
import app.tastile.android.data.command.V1CommandDispatcher;
import app.tastile.android.notifications.ExecutionNotificationCoordinator;
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
public final class TileRepository_Factory implements Factory<TileRepository> {
  private final Provider<ExecutionNotificationCoordinator> executionNotificationCoordinatorProvider;

  private final Provider<EventRepository> eventRepositoryProvider;

  private final Provider<CurrentUserProvider> currentUserProvider;

  private final Provider<V1ApiClient> v1ApiClientProvider;

  private final Provider<V1CommandDispatcher> v1CommandDispatcherProvider;

  private TileRepository_Factory(
      Provider<ExecutionNotificationCoordinator> executionNotificationCoordinatorProvider,
      Provider<EventRepository> eventRepositoryProvider,
      Provider<CurrentUserProvider> currentUserProvider, Provider<V1ApiClient> v1ApiClientProvider,
      Provider<V1CommandDispatcher> v1CommandDispatcherProvider) {
    this.executionNotificationCoordinatorProvider = executionNotificationCoordinatorProvider;
    this.eventRepositoryProvider = eventRepositoryProvider;
    this.currentUserProvider = currentUserProvider;
    this.v1ApiClientProvider = v1ApiClientProvider;
    this.v1CommandDispatcherProvider = v1CommandDispatcherProvider;
  }

  @Override
  public TileRepository get() {
    return newInstance(executionNotificationCoordinatorProvider.get(), eventRepositoryProvider.get(), currentUserProvider.get(), v1ApiClientProvider.get(), v1CommandDispatcherProvider.get());
  }

  public static TileRepository_Factory create(
      Provider<ExecutionNotificationCoordinator> executionNotificationCoordinatorProvider,
      Provider<EventRepository> eventRepositoryProvider,
      Provider<CurrentUserProvider> currentUserProvider, Provider<V1ApiClient> v1ApiClientProvider,
      Provider<V1CommandDispatcher> v1CommandDispatcherProvider) {
    return new TileRepository_Factory(executionNotificationCoordinatorProvider, eventRepositoryProvider, currentUserProvider, v1ApiClientProvider, v1CommandDispatcherProvider);
  }

  public static TileRepository newInstance(
      ExecutionNotificationCoordinator executionNotificationCoordinator,
      EventRepository eventRepository, CurrentUserProvider currentUserProvider,
      V1ApiClient v1ApiClient, V1CommandDispatcher v1CommandDispatcher) {
    return new TileRepository(executionNotificationCoordinator, eventRepository, currentUserProvider, v1ApiClient, v1CommandDispatcher);
  }
}
