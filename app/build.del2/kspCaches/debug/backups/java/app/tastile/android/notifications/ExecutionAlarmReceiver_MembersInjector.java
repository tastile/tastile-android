package app.tastile.android.notifications;

import app.tastile.android.core.CoreRuntimeService;
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
public final class ExecutionAlarmReceiver_MembersInjector implements MembersInjector<ExecutionAlarmReceiver> {
  private final Provider<ExecutionAlarmScheduler> schedulerProvider;

  private final Provider<CoreRuntimeService> coreRuntimeServiceProvider;

  private final Provider<UserSettingsRepository> userSettingsRepositoryProvider;

  private ExecutionAlarmReceiver_MembersInjector(
      Provider<ExecutionAlarmScheduler> schedulerProvider,
      Provider<CoreRuntimeService> coreRuntimeServiceProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider) {
    this.schedulerProvider = schedulerProvider;
    this.coreRuntimeServiceProvider = coreRuntimeServiceProvider;
    this.userSettingsRepositoryProvider = userSettingsRepositoryProvider;
  }

  @Override
  public void injectMembers(ExecutionAlarmReceiver instance) {
    injectScheduler(instance, schedulerProvider.get());
    injectCoreRuntimeService(instance, coreRuntimeServiceProvider.get());
    injectUserSettingsRepository(instance, userSettingsRepositoryProvider.get());
  }

  public static MembersInjector<ExecutionAlarmReceiver> create(
      Provider<ExecutionAlarmScheduler> schedulerProvider,
      Provider<CoreRuntimeService> coreRuntimeServiceProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider) {
    return new ExecutionAlarmReceiver_MembersInjector(schedulerProvider, coreRuntimeServiceProvider, userSettingsRepositoryProvider);
  }

  @InjectedFieldSignature("app.tastile.android.notifications.ExecutionAlarmReceiver.scheduler")
  public static void injectScheduler(ExecutionAlarmReceiver instance,
      ExecutionAlarmScheduler scheduler) {
    instance.scheduler = scheduler;
  }

  @InjectedFieldSignature("app.tastile.android.notifications.ExecutionAlarmReceiver.coreRuntimeService")
  public static void injectCoreRuntimeService(ExecutionAlarmReceiver instance,
      CoreRuntimeService coreRuntimeService) {
    instance.coreRuntimeService = coreRuntimeService;
  }

  @InjectedFieldSignature("app.tastile.android.notifications.ExecutionAlarmReceiver.userSettingsRepository")
  public static void injectUserSettingsRepository(ExecutionAlarmReceiver instance,
      UserSettingsRepository userSettingsRepository) {
    instance.userSettingsRepository = userSettingsRepository;
  }
}
