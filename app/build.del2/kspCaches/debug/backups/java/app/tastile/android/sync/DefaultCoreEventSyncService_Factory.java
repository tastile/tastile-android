package app.tastile.android.sync;

import app.tastile.android.core.CoreRuntimeService;
import app.tastile.android.data.repository.EventRepository;
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
public final class DefaultCoreEventSyncService_Factory implements Factory<DefaultCoreEventSyncService> {
  private final Provider<EventRepository> eventRepositoryProvider;

  private final Provider<CoreRuntimeService> coreRuntimeServiceProvider;

  private DefaultCoreEventSyncService_Factory(Provider<EventRepository> eventRepositoryProvider,
      Provider<CoreRuntimeService> coreRuntimeServiceProvider) {
    this.eventRepositoryProvider = eventRepositoryProvider;
    this.coreRuntimeServiceProvider = coreRuntimeServiceProvider;
  }

  @Override
  public DefaultCoreEventSyncService get() {
    return newInstance(eventRepositoryProvider.get(), coreRuntimeServiceProvider.get());
  }

  public static DefaultCoreEventSyncService_Factory create(
      Provider<EventRepository> eventRepositoryProvider,
      Provider<CoreRuntimeService> coreRuntimeServiceProvider) {
    return new DefaultCoreEventSyncService_Factory(eventRepositoryProvider, coreRuntimeServiceProvider);
  }

  public static DefaultCoreEventSyncService newInstance(EventRepository eventRepository,
      CoreRuntimeService coreRuntimeService) {
    return new DefaultCoreEventSyncService(eventRepository, coreRuntimeService);
  }
}
