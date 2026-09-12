package app.tastile.android.lint

import com.android.tools.lint.client.api.JavaContext
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner

/**
 * Rule 7: tile mutation use-cases must be invoked from `ui/tile/TileComposer.kt` only.
 *
 * The Tastile composition boundary keeps `CreateTileUseCase` / `UpdateTileUseCase` /
 * `DuplicateTileUseCase` behind a single composer so the UI layer has one
 * side-effect funnel for any change that hits the wire shape. Direct
 * invocation from sheets / dialogs / view-models bypasses the funnel and
 * breaks the undo / audit hooks the composer wires in.
 *
 * NOTE: Phase 1 ships the rule but does NOT enable it via
 * `verifyDesignSystemImports`. Phase 4 (composer migration) turns it on.
 */
class SingleComposerRule : Detector(), SourceCodeScanner {

    override fun visitSourceCode(context: JavaContext, source: com.android.tools.lint.client.api.SourceFile) {
        val path = source.relativePath.orEmpty()
        // Allow the composer itself (and the use-case declarations) to mention the names.
        if (path.endsWith("ui/tile/TileComposer.kt")) return
        if (path.endsWith("/usecase/CreateTileUseCase.kt")) return
        if (path.endsWith("/usecase/UpdateTileUseCase.kt")) return
        if (path.endsWith("/usecase/DuplicateTileUseCase.kt")) return

        val text = source.readText()
        val violations = USE_CASES.count { name ->
            Regex("""\b${Regex.escape(name)}\b""").containsMatchIn(text)
        }
        if (violations > 0) {
            context.report(
                ISSUE,
                context.getLocation(source),
                "Rule 7: $violations tile-mutation use-case(s) invoked from `$path`. " +
                    "All Create/Update/Duplicate tile mutations must go through " +
                    "`ui/tile/TileComposer.kt`.",
            )
        }
    }

    companion object {
        private val USE_CASES = listOf("CreateTileUseCase", "UpdateTileUseCase", "DuplicateTileUseCase")

        val ISSUE = Issue.create(
            id = "SingleComposer",
            briefDescription = "Tile mutation use-cases must route through TileComposer",
            explanation = "R7: `CreateTileUseCase` / `UpdateTileUseCase` / " +
                "`DuplicateTileUseCase` are invoked from one place — `ui/tile/TileComposer.kt` — " +
                "so the UI layer has a single side-effect funnel for any change that hits " +
                "the wire shape. Direct calls from sheets / dialogs / view-models bypass " +
                "the composer and break the undo / audit hooks.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(
                SingleComposerRule::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}