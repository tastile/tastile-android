package app.tastile.android.util

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Minimal Hilt entry-point host for instrumented interaction tests that need
 * the REAL production graph (real ViewModels, real dispatchers) without the
 * MainActivity navigation/auth gate.
 *
 * Lives in `src/debug` (never ships in release) and is declared in
 * `src/debug/AndroidManifest.xml`. Use with
 * `createAndroidComposeRule<HiltTestActivity>()` + [HiltAndroidRule] so
 * `@TestInstallIn` modules (e.g. the canary network backend) are honoured
 * under [TastileTestRunner].
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
