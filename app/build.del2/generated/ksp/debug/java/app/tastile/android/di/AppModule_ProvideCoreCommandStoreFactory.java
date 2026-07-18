package app.tastile.android.di;

import app.tastile.android.core.CoreCommandStore;
import app.tastile.android.core.SharedPreferencesCoreCommandStore;
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
public final class AppModule_ProvideCoreCommandStoreFactory implements Factory<CoreCommandStore> {
  private final Provider<SharedPreferencesCoreCommandStore> storeProvider;

  private AppModule_ProvideCoreCommandStoreFactory(
      Provider<SharedPreferencesCoreCommandStore> storeProvider) {
    this.storeProvider = storeProvider;
  }

  @Override
  public CoreCommandStore get() {
    return provideCoreCommandStore(storeProvider.get());
  }

  public static AppModule_ProvideCoreCommandStoreFactory create(
      Provider<SharedPreferencesCoreCommandStore> storeProvider) {
    return new AppModule_ProvideCoreCommandStoreFactory(storeProvider);
  }

  public static CoreCommandStore provideCoreCommandStore(SharedPreferencesCoreCommandStore store) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCoreCommandStore(store));
  }
}
