package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class SingleUiStateRuleTest : LintDetectorTest() {
    override fun getDetector() = SingleUiStateRule()
    override fun getIssues() = listOf(SingleUiStateRule.ISSUE)

    fun testReportsWhenUiStateIsNotStateFlow() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.feature.foo
            class FooViewModel {
                val uiState: Int = 0
            }
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testDoesNotReportWhenViewModelHasExactlyOneUiStateStateFlow() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.feature.foo
            import kotlinx.coroutines.flow.MutableStateFlow
            import kotlinx.coroutines.flow.StateFlow
            class FooViewModel {
                val uiState: StateFlow<Int> = MutableStateFlow(0)
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testDoesNotReportWhenClassIsNotAViewModel() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.feature.foo
            class NotAViewModel {
                val uiState: Int = 0
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }
}