package app.tastile.android.ui.mobile.panels;

import app.tastile.android.data.repository.WorkspaceRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
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
public final class ProjectsViewModel_Factory implements Factory<ProjectsViewModel> {
  private final Provider<WorkspaceRepository> workspaceRepositoryProvider;

  private ProjectsViewModel_Factory(Provider<WorkspaceRepository> workspaceRepositoryProvider) {
    this.workspaceRepositoryProvider = workspaceRepositoryProvider;
  }

  @Override
  public ProjectsViewModel get() {
    return newInstance(workspaceRepositoryProvider.get());
  }

  public static ProjectsViewModel_Factory create(
      Provider<WorkspaceRepository> workspaceRepositoryProvider) {
    return new ProjectsViewModel_Factory(workspaceRepositoryProvider);
  }

  public static ProjectsViewModel newInstance(WorkspaceRepository workspaceRepository) {
    return new ProjectsViewModel(workspaceRepository);
  }
}
