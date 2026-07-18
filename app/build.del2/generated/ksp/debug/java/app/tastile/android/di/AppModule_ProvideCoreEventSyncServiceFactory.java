package app.tastile.android.di;

import app.tastile.android.sync.CoreEventSyncService;
import app.tastile.android.sync.DefaultCoreEventSyncService;
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
public final class AppModule_ProvideCoreEventSyncServiceFactory implements Factory<CoreEventSyncService> {
  private final Provider<DefaultCoreEventSyncService> serviceProvider;

  private AppModule_ProvideCoreEventSyncServiceFactory(
      Provider<DefaultCoreEventSyncService> serviceProvider) {
    this.serviceProvider = serviceProvider;
  }

  @Override
  public CoreEventSyncService get() {
    return provideCoreEventSyncService(serviceProvider.get());
  }

  public static AppModule_ProvideCoreEventSyncServiceFactory create(
      Provider<DefaultCoreEventSyncService> serviceProvider) {
    return new AppModule_ProvideCoreEventSyncServiceFactory(serviceProvider);
  }

  public static CoreEventSyncService provideCoreEventSyncService(
      DefaultCoreEventSyncService service) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCoreEventSyncService(service));
  }
}
