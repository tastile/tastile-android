package app.tastile.android.ui.mobile;

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
public final class OverlayViewModel_Factory implements Factory<OverlayViewModel> {
  @Override
  public OverlayViewModel get() {
    return newInstance();
  }

  public static OverlayViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static OverlayViewModel newInstance() {
    return new OverlayViewModel();
  }

  private static final class InstanceHolder {
    static final OverlayViewModel_Factory INSTANCE = new OverlayViewModel_Factory();
  }
}
