package app.tastile.android.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ProfileRepository_Factory implements Factory<ProfileRepository> {
  @Override
  public ProfileRepository get() {
    return newInstance();
  }

  public static ProfileRepository_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ProfileRepository newInstance() {
    return new ProfileRepository();
  }

  private static final class InstanceHolder {
    static final ProfileRepository_Factory INSTANCE = new ProfileRepository_Factory();
  }
}
