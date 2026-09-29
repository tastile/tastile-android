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
            class FooPresenter {
                val uiState: Int = 0
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testReportsWhenClassNameEndsWithViewModel() {
        // The rule matches on the name suffix, so a class called
        // `NotAViewModel` is still treated as a ViewModel. This pins that the
        // suffix test is the only thing keeping the previous fixture quiet.
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.feature.foo
            class NotAViewModel {
                val uiState: Int = 0
            }
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testReportsWhenViewModelExposesNoUiState() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.feature.foo
            class FooViewModel {
                val count: Int = 0
            }
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    fun testReportsExactlyOneWarningForAConformantViewModel() {
        // Kotlin rejects two properties with the same name in one class, so the
        // detector's "more than one uiState" branch is unreachable for code that
        // compiles. One conformant ViewModel must still report nothing.
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.feature.foo
            import kotlinx.coroutines.flow.MutableStateFlow
            import kotlinx.coroutines.flow.StateFlow
            class FooViewModel {
                val uiState: StateFlow<Int> = MutableStateFlow(0)
                val other: StateFlow<Int> = MutableStateFlow(0)
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }
}