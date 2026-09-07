package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class HardcodedColorRuleTest : LintDetectorTest() {
    override fun getDetector() = HardcodedColorRule()
    override fun getIssues() = listOf(HardcodedColorRule.ISSUE)

    fun testReportsHardcodedColorInUi() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo {
                val c = Color(0xFF112233)
            }
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testDoesNotReportInDesignSystemThemeFile() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.core.designsystem.theme
            class Foo {
                val c = Color(0xFF112233)
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testDoesNotReportOutsideUiTree() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.core.feature
            class Foo {
                val c = Color(0xFF112233)
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testDoesNotReportWhenColorLiteralAbsent() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo
            """.trimIndent()
        )).run().expectWarningCount(0)
    }
}