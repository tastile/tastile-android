package app.tastile.android.notifications;

import app.tastile.android.data.repository.UserSettingsRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class ExecutionAlarmActivity_MembersInjector implements MembersInjector<ExecutionAlarmActivity> {
  private final Provider<UserSettingsRepository> userSettingsRepositoryProvider;

  private ExecutionAlarmActivity_MembersInjector(
      Provider<UserSettingsRepository> userSettingsRepositoryProvider) {
    this.userSettingsRepositoryProvider = userSettingsRepositoryProvider;
  }

  @Override
  public void injectMembers(ExecutionAlarmActivity instance) {
    injectUserSettingsRepository(instance, userSettingsRepositoryProvider.get());
  }

  public static MembersInjector<ExecutionAlarmActivity> create(
      Provider<UserSettingsRepository> userSettingsRepositoryProvider) {
    return new ExecutionAlarmActivity_MembersInjector(userSettingsRepositoryProvider);
  }

  @InjectedFieldSignature("app.tastile.android.notifications.ExecutionAlarmActivity.userSettingsRepository")
  public static void injectUserSettingsRepository(ExecutionAlarmActivity instance,
      UserSettingsRepository userSettingsRepository) {
    instance.userSettingsRepository = userSettingsRepository;
  }
}
