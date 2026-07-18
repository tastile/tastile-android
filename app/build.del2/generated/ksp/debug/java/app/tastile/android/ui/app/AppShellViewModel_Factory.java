package app.tastile.android.ui.app;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class AppShellViewModel_Factory implements Factory<AppShellViewModel> {
  @Override
  public AppShellViewModel get() {
    return newInstance();
  }

  public static AppShellViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AppShellViewModel newInstance() {
    return new AppShellViewModel();
  }

  private static final class InstanceHolder {
    static final AppShellViewModel_Factory INSTANCE = new AppShellViewModel_Factory();
  }
}
