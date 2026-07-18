package app.tastile.android.notifications;

import android.content.Context;
import app.tastile.android.core.CoreRuntimeService;
import app.tastile.android.data.repository.AuthRepository;
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
public final class ExecutionAlarmScheduler_Factory implements Factory<ExecutionAlarmScheduler> {
  private final Provider<Context> contextProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<CoreRuntimeService> coreRuntimeServiceProvider;

  private ExecutionAlarmScheduler_Factory(Provider<Context> contextProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<CoreRuntimeService> coreRuntimeServiceProvider) {
    this.contextProvider = contextProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.coreRuntimeServiceProvider = coreRuntimeServiceProvider;
  }

  @Override
  public ExecutionAlarmScheduler get() {
    return newInstance(contextProvider.get(), authRepositoryProvider.get(), coreRuntimeServiceProvider.get());
  }

  public static ExecutionAlarmScheduler_Factory create(Provider<Context> contextProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<CoreRuntimeService> coreRuntimeServiceProvider) {
    return new ExecutionAlarmScheduler_Factory(contextProvider, authRepositoryProvider, coreRuntimeServiceProvider);
  }

  public static ExecutionAlarmScheduler newInstance(Context context, AuthRepository authRepository,
      CoreRuntimeService coreRuntimeService) {
    return new ExecutionAlarmScheduler(context, authRepository, coreRuntimeService);
  }
}
