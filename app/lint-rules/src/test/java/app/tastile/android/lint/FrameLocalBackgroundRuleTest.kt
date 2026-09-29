package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class FrameLocalBackgroundRuleTest : LintDetectorTest() {
    override fun getDetector() = FrameLocalBackgroundRule()
    override fun getIssues() = listOf(FrameLocalBackgroundRule.ISSUE)

    fun testReportsWhenScreenComposableMissingBackgroundImport() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            import androidx.compose.runtime.Composable
            @Composable
            fun FooScreen() {}
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testDoesNotReportWhenLocalBackgroundThemeImported() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            import androidx.compose.runtime.Composable
            import app.tastile.android.core.designsystem.LocalBackgroundTheme
            @Composable
            fun FooScreen() {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testDoesNotReportWhenSurfaceImported() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            import androidx.compose.material3.Surface
            import androidx.compose.runtime.Composable
            @Composable
            fun FooSheet() {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testDoesNotReportWhenFunctionNameDoesNotMatchFrameSuffixes() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            import androidx.compose.runtime.Composable
            @Composable
            fun FrameLabelBadge() {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testReportsWhenNameMerelyContainsTheSuffixWord() {
        // The suffix test is the only criterion, so a name like `NotAFrame`
        // still ends in "Frame" and is reported. Pinned here because the
        // previous fixture used exactly that name and expected silence.
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            import androidx.compose.runtime.Composable
            @Composable
            fun NotAFrame() {}
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testReportsAcrossAllFrameSuffixes() {
        lint().allowCompilationErrors().files(
            kotlin(
                """
                package app.tastile.android.ui.dashboard
                import androidx.compose.runtime.Composable
                @Composable
                fun FooDialog() {}
                """.trimIndent()
            ),
            kotlin(
                """
                package app.tastile.android.ui.dashboard
                import androidx.compose.runtime.Composable
                @Composable
                fun BarPanel() {}
                """.trimIndent()
            ),
            kotlin(
                """
                package app.tastile.android.ui.dashboard
                import androidx.compose.runtime.Composable
                @Composable
                fun BazScaffold() {}
                """.trimIndent()
            ),
        ).run().expectWarningCount(3)
    }
}