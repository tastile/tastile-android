package app.tastile.android.ui.mobile.sheets.quickcreate;

import app.tastile.android.data.api.V1ApiClient;
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
public final class QuickCreateSubmissionViewModel_Factory implements Factory<QuickCreateSubmissionViewModel> {
  private final Provider<V1ApiClient> clientProvider;

  private QuickCreateSubmissionViewModel_Factory(Provider<V1ApiClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public QuickCreateSubmissionViewModel get() {
    return newInstance(clientProvider.get());
  }

  public static QuickCreateSubmissionViewModel_Factory create(
      Provider<V1ApiClient> clientProvider) {
    return new QuickCreateSubmissionViewModel_Factory(clientProvider);
  }

  public static QuickCreateSubmissionViewModel newInstance(V1ApiClient client) {
    return new QuickCreateSubmissionViewModel(client);
  }
}
