package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class WrapperParameterOrderDetectorTest : LintDetectorTest() {
    override fun getDetector() = WrapperParameterOrderDetector()
    override fun getIssues() = listOf(WrapperParameterOrderDetector.ISSUE)

    // Compose runtime + Modifier types aren't on the test classpath, so the
    // Lint test client is told to ignore the unresolved imports. The detector
    // only reads parameter NAMES from the raw PSI tree, so unresolved types
    // never affect the C1/C2 assertions.

    fun testModifierFirstReportsPositiveWhenModifierIsNotFirst() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            @Composable
            fun Wrong(text: String, modifier: Modifier = Modifier) {}
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testModifierFirstReportsNegativeWhenModifierIsFirst() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            @Composable
            fun Right(modifier: Modifier = Modifier, text: String) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testEnabledLastReportsPositiveWhenEnabledIsNotLast() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            @Composable
            fun Wrong(modifier: Modifier = Modifier, enabled: Boolean = true, text: String) {}
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testEnabledLastReportsNegativeWhenEnabledIsLast() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            @Composable
            fun Right(modifier: Modifier = Modifier, text: String, enabled: Boolean = true) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testNonDesignSystemPackageIsIgnored() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            @Composable
            fun Wrong(text: String, modifier: Modifier = Modifier) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }
}
