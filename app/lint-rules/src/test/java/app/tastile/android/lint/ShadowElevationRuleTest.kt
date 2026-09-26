package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class ShadowElevationRuleTest : LintDetectorTest() {
    override fun getDetector() = ShadowElevationRule()
    override fun getIssues() = listOf(ShadowElevationRule.ISSUE)

    fun testReportsWhenShadowElevationIsHardcoded() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo {
                val elevation = 8.dp
                fun render() { Modifier.shadowElevation = 8.dp }
            }
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testDoesNotReportWhenAbsent() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo {
                val elevation = 8.dp
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testDoesNotReportWhenFileIsOutsideUiTree() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.core.designsystem
            class Foo {
                fun render() { Modifier.shadowElevation = 4.dp }
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }
}