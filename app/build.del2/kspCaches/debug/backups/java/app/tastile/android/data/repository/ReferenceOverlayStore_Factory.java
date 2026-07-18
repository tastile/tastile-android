package app.tastile.android.data.repository;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ReferenceOverlayStore_Factory implements Factory<ReferenceOverlayStore> {
  private final Provider<Context> contextProvider;

  private ReferenceOverlayStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ReferenceOverlayStore get() {
    return newInstance(contextProvider.get());
  }

  public static ReferenceOverlayStore_Factory create(Provider<Context> contextProvider) {
    return new ReferenceOverlayStore_Factory(contextProvider);
  }

  public static ReferenceOverlayStore newInstance(Context context) {
    return new ReferenceOverlayStore(context);
  }
}
