package app.tastile.android.notifications;

import android.content.Context;
import app.tastile.android.core.CoreRuntimeService;
import app.tastile.android.data.repository.AuthRepository;
import app.tastile.android.data.repository.UserSettingsRepository;
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
public final class ExecutionNotificationCoordinator_Factory implements Factory<ExecutionNotificationCoordinator> {
  private final Provider<Context> contextProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<CoreRuntimeService> coreRuntimeServiceProvider;

  private final Provider<UserSettingsRepository> userSettingsRepositoryProvider;

  private final Provider<ExecutionAlarmScheduler> alarmSchedulerProvider;

  private ExecutionNotificationCoordinator_Factory(Provider<Context> contextProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<CoreRuntimeService> coreRuntimeServiceProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<ExecutionAlarmScheduler> alarmSchedulerProvider) {
    this.contextProvider = contextProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.coreRuntimeServiceProvider = coreRuntimeServiceProvider;
    this.userSettingsRepositoryProvider = userSettingsRepositoryProvider;
    this.alarmSchedulerProvider = alarmSchedulerProvider;
  }

  @Override
  public ExecutionNotificationCoordinator get() {
    return newInstance(contextProvider.get(), authRepositoryProvider.get(), coreRuntimeServiceProvider.get(), userSettingsRepositoryProvider.get(), alarmSchedulerProvider.get());
  }

  public static ExecutionNotificationCoordinator_Factory create(Provider<Context> contextProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<CoreRuntimeService> coreRuntimeServiceProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<ExecutionAlarmScheduler> alarmSchedulerProvider) {
    return new ExecutionNotificationCoordinator_Factory(contextProvider, authRepositoryProvider, coreRuntimeServiceProvider, userSettingsRepositoryProvider, alarmSchedulerProvider);
  }

  public static ExecutionNotificationCoordinator newInstance(Context context,
      AuthRepository authRepository, CoreRuntimeService coreRuntimeService,
      UserSettingsRepository userSettingsRepository, ExecutionAlarmScheduler alarmScheduler) {
    return new ExecutionNotificationCoordinator(context, authRepository, coreRuntimeService, userSettingsRepository, alarmScheduler);
  }
}
