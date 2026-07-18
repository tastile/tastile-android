package app.tastile.android;

import app.tastile.android.data.repository.AuthRepository;
import app.tastile.android.data.repository.UserSettingsRepository;
import app.tastile.android.notifications.ExecutionNotificationCoordinator;
import app.tastile.android.sync.SyncCoordinator;
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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SyncCoordinator> syncCoordinatorProvider;

  private final Provider<ExecutionNotificationCoordinator> executionNotificationCoordinatorProvider;

  private final Provider<UserSettingsRepository> userSettingsRepositoryProvider;

  private MainActivity_MembersInjector(Provider<AuthRepository> authRepositoryProvider,
      Provider<SyncCoordinator> syncCoordinatorProvider,
      Provider<ExecutionNotificationCoordinator> executionNotificationCoordinatorProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.syncCoordinatorProvider = syncCoordinatorProvider;
    this.executionNotificationCoordinatorProvider = executionNotificationCoordinatorProvider;
    this.userSettingsRepositoryProvider = userSettingsRepositoryProvider;
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectAuthRepository(instance, authRepositoryProvider.get());
    injectSyncCoordinator(instance, syncCoordinatorProvider.get());
    injectExecutionNotificationCoordinator(instance, executionNotificationCoordinatorProvider.get());
    injectUserSettingsRepository(instance, userSettingsRepositoryProvider.get());
  }

  public static MembersInjector<MainActivity> create(
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SyncCoordinator> syncCoordinatorProvider,
      Provider<ExecutionNotificationCoordinator> executionNotificationCoordinatorProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider) {
    return new MainActivity_MembersInjector(authRepositoryProvider, syncCoordinatorProvider, executionNotificationCoordinatorProvider, userSettingsRepositoryProvider);
  }

  @InjectedFieldSignature("app.tastile.android.MainActivity.authRepository")
  public static void injectAuthRepository(MainActivity instance, AuthRepository authRepository) {
    instance.authRepository = authRepository;
  }

  @InjectedFieldSignature("app.tastile.android.MainActivity.syncCoordinator")
  public static void injectSyncCoordinator(MainActivity instance, SyncCoordinator syncCoordinator) {
    instance.syncCoordinator = syncCoordinator;
  }

  @InjectedFieldSignature("app.tastile.android.MainActivity.executionNotificationCoordinator")
  public static void injectExecutionNotificationCoordinator(MainActivity instance,
      ExecutionNotificationCoordinator executionNotificationCoordinator) {
    instance.executionNotificationCoordinator = executionNotificationCoordinator;
  }

  @InjectedFieldSignature("app.tastile.android.MainActivity.userSettingsRepository")
  public static void injectUserSettingsRepository(MainActivity instance,
      UserSettingsRepository userSettingsRepository) {
    instance.userSettingsRepository = userSettingsRepository;
  }
}
