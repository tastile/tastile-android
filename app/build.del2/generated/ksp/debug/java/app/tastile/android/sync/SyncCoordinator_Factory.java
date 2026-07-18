package app.tastile.android.sync;

import app.tastile.android.core.CoreRuntimeService;
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
public final class SyncCoordinator_Factory implements Factory<SyncCoordinator> {
  private final Provider<CoreRuntimeService> coreRuntimeServiceProvider;

  private final Provider<CoreEventSyncService> coreEventSyncServiceProvider;

  private SyncCoordinator_Factory(Provider<CoreRuntimeService> coreRuntimeServiceProvider,
      Provider<CoreEventSyncService> coreEventSyncServiceProvider) {
    this.coreRuntimeServiceProvider = coreRuntimeServiceProvider;
    this.coreEventSyncServiceProvider = coreEventSyncServiceProvider;
  }

  @Override
  public SyncCoordinator get() {
    return newInstance(coreRuntimeServiceProvider.get(), coreEventSyncServiceProvider.get());
  }

  public static SyncCoordinator_Factory create(
      Provider<CoreRuntimeService> coreRuntimeServiceProvider,
      Provider<CoreEventSyncService> coreEventSyncServiceProvider) {
    return new SyncCoordinator_Factory(coreRuntimeServiceProvider, coreEventSyncServiceProvider);
  }

  public static SyncCoordinator newInstance(CoreRuntimeService coreRuntimeService,
      CoreEventSyncService coreEventSyncService) {
    return new SyncCoordinator(coreRuntimeService, coreEventSyncService);
  }
}
