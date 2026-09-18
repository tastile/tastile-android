package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class BranchNameRuleTest : LintDetectorTest() {
    override fun getDetector() = BranchNameRule()
    override fun getIssues() = listOf(BranchNameRule.ISSUE)

    fun testNoOpFileScanDoesNotReport() {
        // BranchNameRule is a Gradle-task gate; the lint stub is a no-op
        // to keep the rule registered in IssueRegistry. Lock the
        // no-op behaviour so any future regression that surfaces per-file
        // reports trips a test.
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testPatternIsDigitsOnly() {
        // Lock the regex against accidental relaxation.
        assert(BranchNameRule.PATTERN == "^\\d+\$")
        assert(Regex(BranchNameRule.PATTERN).matches("102"))
        assert(Regex(BranchNameRule.PATTERN).matches("1"))
        assert(!Regex(BranchNameRule.PATTERN).matches("release-0-6-0"))
        assert(!Regex(BranchNameRule.PATTERN).matches("feature/x"))
        assert(!Regex(BranchNameRule.PATTERN).matches(""))
    }
}