package app.tastile.android.notifications;

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
public final class ExecutionAlarmRescheduleReceiver_MembersInjector implements MembersInjector<ExecutionAlarmRescheduleReceiver> {
  private final Provider<ExecutionAlarmScheduler> schedulerProvider;

  private ExecutionAlarmRescheduleReceiver_MembersInjector(
      Provider<ExecutionAlarmScheduler> schedulerProvider) {
    this.schedulerProvider = schedulerProvider;
  }

  @Override
  public void injectMembers(ExecutionAlarmRescheduleReceiver instance) {
    injectScheduler(instance, schedulerProvider.get());
  }

  public static MembersInjector<ExecutionAlarmRescheduleReceiver> create(
      Provider<ExecutionAlarmScheduler> schedulerProvider) {
    return new ExecutionAlarmRescheduleReceiver_MembersInjector(schedulerProvider);
  }

  @InjectedFieldSignature("app.tastile.android.notifications.ExecutionAlarmRescheduleReceiver.scheduler")
  public static void injectScheduler(ExecutionAlarmRescheduleReceiver instance,
      ExecutionAlarmScheduler scheduler) {
    instance.scheduler = scheduler;
  }
}
