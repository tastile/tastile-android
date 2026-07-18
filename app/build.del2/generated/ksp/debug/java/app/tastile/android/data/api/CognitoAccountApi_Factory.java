package app.tastile.android.data.api;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function1;

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
public final class CognitoAccountApi_Factory implements Factory<CognitoAccountApi> {
  private final Provider<Function1<? super Continuation<? super String>, ?>> tokenProvider;

  private CognitoAccountApi_Factory(
      Provider<Function1<? super Continuation<? super String>, ?>> tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  public CognitoAccountApi get() {
    return newInstance(tokenProvider.get());
  }

  public static CognitoAccountApi_Factory create(
      Provider<Function1<? super Continuation<? super String>, ?>> tokenProvider) {
    return new CognitoAccountApi_Factory(tokenProvider);
  }

  public static CognitoAccountApi newInstance(
      Function1<? super Continuation<? super String>, ?> tokenProvider) {
    return new CognitoAccountApi(tokenProvider);
  }
}
