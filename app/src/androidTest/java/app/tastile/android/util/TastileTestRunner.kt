package app.tastile.android.util

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * R17 (audit 2026-07-16): replaces [app.tastile.android.TastileApp] with
 * [HiltTestApplication] for instrumented UI tests.
 *
 * Wire this runner via `android.defaultConfig.testInstrumentationRunner` in
 * [app.tastile.android.app.build.gradle.kts]. Without this swap, tests
 * annotated with [dagger.hilt.android.testing.HiltAndroidTest] would resolve
 * the production Hilt graph and fail to honour `@TestInstallIn` overrides.
 */
class TastileTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
