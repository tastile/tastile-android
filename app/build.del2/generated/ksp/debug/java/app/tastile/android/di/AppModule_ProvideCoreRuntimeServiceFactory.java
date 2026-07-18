package app.tastile.android.di;

import app.tastile.android.core.CoreCommandStore;
import app.tastile.android.core.CoreRuntimeService;
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
public final class AppModule_ProvideCoreRuntimeServiceFactory implements Factory<CoreRuntimeService> {
  private final Provider<CoreCommandStore> commandStoreProvider;

  private AppModule_ProvideCoreRuntimeServiceFactory(
      Provider<CoreCommandStore> commandStoreProvider) {
    this.commandStoreProvider = commandStoreProvider;
  }

  @Override
  public CoreRuntimeService get() {
    return provideCoreRuntimeService(commandStoreProvider.get());
  }

  public static AppModule_ProvideCoreRuntimeServiceFactory create(
      Provider<CoreCommandStore> commandStoreProvider) {
    return new AppModule_ProvideCoreRuntimeServiceFactory(commandStoreProvider);
  }

  public static CoreRuntimeService provideCoreRuntimeService(CoreCommandStore commandStore) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCoreRuntimeService(commandStore));
  }
}
