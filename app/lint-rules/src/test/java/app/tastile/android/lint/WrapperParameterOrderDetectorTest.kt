package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class WrapperParameterOrderDetectorTest : LintDetectorTest() {
    override fun getDetector() = WrapperParameterOrderDetector()
    override fun getIssues() = listOf(WrapperParameterOrderDetector.ISSUE)

    fun testCoreDesignSystemWrapperReportsWhenModifierIsNotFirst() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.core.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            @Composable
            fun Wrong(text: String = "", modifier: Modifier = Modifier) {}
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testCoreDesignSystemScreenIsIgnoredWhenItHasOnlyRequiredParameters() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.core.designsystem
            import androidx.compose.runtime.Composable
            @Composable
            fun MyScreen(required: Int, onAction: () -> Unit) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
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

    fun testModifierFirstDoesNotReportWhenFirstParameterIsRequired() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            @Composable
            fun Screen(text: String, modifier: Modifier = Modifier) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testEnabledReportsWhenDefaultedParameterFollows() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            @Composable
            fun Wrong(modifier: Modifier = Modifier, enabled: Boolean = true, selected: Boolean = false) {}
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testEnabledDoesNotReportWhenRequiredParameterFollows() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            @Composable
            fun Right(modifier: Modifier = Modifier, enabled: Boolean = true, text: String) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
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
            fun Wrong(text: String = "", modifier: Modifier = Modifier) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }
}
