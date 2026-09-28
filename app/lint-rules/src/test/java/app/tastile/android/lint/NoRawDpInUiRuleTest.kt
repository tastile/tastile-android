package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class NoRawDpInUiRuleTest : LintDetectorTest() {
    override fun getDetector() = NoRawDpInUiRule()
    override fun getIssues() = listOf(NoRawDpInUiRule.ISSUE)

    fun testReportsWhenRawDpAboveOne() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo {
                val padding = 16.dp
            }
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testDoesNotReportWhenZeroDp() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo {
                val padding = 0.dp
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testDoesNotReportWhenOneDp() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo {
                val stroke = 1.dp
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testDoesNotReportWhenHalfDp() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo {
                val hairline = 0.5.dp
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testDoesNotReportWhenFileIsOutsideUiTree() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.core.designsystem
            class Foo {
                val padding = 16.dp
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testReportsRawDpAfterAnExemptLeadingValue() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo {
                val padding = 0.dp
                val gap = 16.dp
            }
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testReportsEveryNonExemptOccurrence() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo {
                val hairline = 0.5.dp
                val stroke = 1.dp
                val padding = 16.dp
                val margin = 24.dp
            }
            """.trimIndent()
        )).run().expectWarningCount(2)
    }

    fun testReportsFractionalDpBelowOneThatIsNotExempt() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo {
                val offset = 0.25.dp
            }
            """.trimIndent()
        )).run().expectWarningCount(1)
    }
}