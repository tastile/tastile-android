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
public final class EventRepository_Factory implements Factory<EventRepository> {
  @Override
  public EventRepository get() {
    return newInstance();
  }

  public static EventRepository_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static EventRepository newInstance() {
    return new EventRepository();
  }

  private static final class InstanceHolder {
    static final EventRepository_Factory INSTANCE = new EventRepository_Factory();
  }
}
