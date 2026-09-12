package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class SingleComposerRuleTest : LintDetectorTest() {
    override fun getDetector() = SingleComposerRule()
    override fun getIssues() = listOf(SingleComposerRule.ISSUE)

    fun testReportsWhenUseCaseInvokedOutsideComposer() {
        lint().allowCompilationErrors().files(
            kotlin(
                """
                package app.tastile.android.ui.dashboard
                import app.tastile.android.domain.usecase.CreateTileUseCase
                class Foo {
                    fun bar(useCase: CreateTileUseCase) { useCase() }
                }
                """.trimIndent()
            ),
            kotlin(
                """
                package app.tastile.android.domain.usecase
                class CreateTileUseCase { operator fun invoke() {} }
                """.trimIndent()
            ),
        ).run().expectWarningCount(1)
    }

    fun testDoesNotReportWhenInvokedFromComposer() {
        lint().allowCompilationErrors().files(
            kotlin(
                """
                package app.tastile.android.ui.tile
                import app.tastile.android.domain.usecase.CreateTileUseCase
                class TileComposer {
                    fun bar(useCase: CreateTileUseCase) { useCase() }
                }
                """.trimIndent()
            ),
            kotlin(
                """
                package app.tastile.android.domain.usecase
                class CreateTileUseCase { operator fun invoke() {} }
                """.trimIndent()
            ),
        ).run().expectWarningCount(0)
    }

    fun testDoesNotReportWhenFileDoesNotInvokeUseCases() {
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.ui.dashboard
            class Foo
            """.trimIndent()
        )).run().expectWarningCount(0)
    }
}