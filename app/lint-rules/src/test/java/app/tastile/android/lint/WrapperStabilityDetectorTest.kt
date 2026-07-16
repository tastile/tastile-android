package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class WrapperStabilityDetectorTest : LintDetectorTest() {
    override fun getDetector() = WrapperStabilityDetector()
    override fun getIssues() = listOf(WrapperStabilityDetector.ISSUE)

    fun testCoreDesignSystemLambdaParamReports() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.core.designsystem
            import androidx.compose.runtime.Composable
            @Composable
            fun Wrapper(text: String, onClick: () -> Unit) {}
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testCoreDesignSystemColorParamReports() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.core.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.graphics.Color
            @Composable
            fun Wrapper(color: Color, modifier: Modifier = Modifier) {}
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testUiDesignSystemLambdaParamReports() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.designsystem
            import androidx.compose.runtime.Composable
            @Composable
            fun Wrapper(text: String, onClick: () -> Unit) {}
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testStableAnnotationIsIgnored() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.core.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.Stable
            @Stable
            @Composable
            fun Wrapper(text: String, onClick: () -> Unit) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testImmutableAnnotationIsIgnored() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.Immutable
            @Immutable
            @Composable
            fun Wrapper(text: String, onClick: () -> Unit) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testNoLambdaOrColorDoesNotReport() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.core.designsystem
            import androidx.compose.runtime.Composable
            @Composable
            fun Wrapper(text: String, count: Int) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testNonDesignSystemPackageIsIgnored() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.mobile.tabs
            import androidx.compose.runtime.Composable
            @Composable
            fun Wrapper(text: String, onClick: () -> Unit) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testNonComposableIsIgnored() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.core.designsystem
            fun Wrapper(text: String, onClick: () -> Unit) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }
}
