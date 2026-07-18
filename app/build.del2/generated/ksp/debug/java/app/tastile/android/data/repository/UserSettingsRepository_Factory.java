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
public final class UserSettingsRepository_Factory implements Factory<UserSettingsRepository> {
  private final Provider<Context> contextProvider;

  private UserSettingsRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public UserSettingsRepository get() {
    return newInstance(contextProvider.get());
  }

  public static UserSettingsRepository_Factory create(Provider<Context> contextProvider) {
    return new UserSettingsRepository_Factory(contextProvider);
  }

  public static UserSettingsRepository newInstance(Context context) {
    return new UserSettingsRepository(context);
  }
}
