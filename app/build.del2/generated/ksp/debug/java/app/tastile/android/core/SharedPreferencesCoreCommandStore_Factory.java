package app.tastile.android.core;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class SharedPreferencesCoreCommandStore_Factory implements Factory<SharedPreferencesCoreCommandStore> {
  private final Provider<Context> contextProvider;

  private SharedPreferencesCoreCommandStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SharedPreferencesCoreCommandStore get() {
    return newInstance(contextProvider.get());
  }

  public static SharedPreferencesCoreCommandStore_Factory create(
      Provider<Context> contextProvider) {
    return new SharedPreferencesCoreCommandStore_Factory(contextProvider);
  }

  public static SharedPreferencesCoreCommandStore newInstance(Context context) {
    return new SharedPreferencesCoreCommandStore(context);
  }
}
