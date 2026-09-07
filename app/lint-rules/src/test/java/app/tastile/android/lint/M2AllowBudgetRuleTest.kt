package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class M2AllowBudgetRuleTest : LintDetectorTest() {
    override fun getDetector() = M2AllowBudgetRule()
    override fun getIssues() = listOf(M2AllowBudgetRule.ISSUE)

    fun testReportsWhenMarkerPresent() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            // m2-allow: typography - reading MaterialTheme.typography
            import androidx.compose.material3.Typography
            class Foo
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testDoesNotReportWhenMarkerAbsent() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testBudgetConstantsAreConsistent() {
        // Lock the budget arithmetic against accidental regression.
        assert(M2AllowBudgetRule.BASELINE == 522)
        assert(M2AllowBudgetRule.BUDGET_DELTA == 50)
        assert(M2AllowBudgetRule.LIMIT == 572)
    }
}