package app.tastile.android.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

/**
 * Rule 6: ViewModels must publish exactly one top-level `val uiState: StateFlow<*>`.
 *
 * The Tastile state-projection convention (R22, single-state-holder) is that
 * one ViewModel maps to one StateFlow so the UI can hoist the read into a
 * single `collectAsStateWithLifecycle()` site. Multiple `uiState`-named
 * properties (or non-StateFlow types named `uiState`) indicate accidental
 * split state that has to be migrated before Phase 4.
 *
 * NOTE: Phase 1 ships the rule but does NOT enable it via
 * `verifyDesignSystemImports` (existing screens do not conform yet).
 * `:app:lint` still surfaces the warning so editors see it. Phase 5
 * turns it into a hard gate.
 */
class SingleUiStateRule : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitClass(node: UClass) {
                if (!node.name.orEmpty().endsWith("ViewModel")) return

                val uiStateProperties = node.fields.filter { it.name == "uiState" }
                if (uiStateProperties.size > 1) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "Rule 6: ViewModel exposes ${uiStateProperties.size} `uiState` properties " +
                            "(expected exactly 1). Merge the redundant states into a single holder.",
                    )
                    return
                }

                val single = uiStateProperties.firstOrNull() ?: return
                val rawType = single.sourcePsi?.text ?: return
                val isStateFlow = rawType.contains("StateFlow")
                if (!isStateFlow) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "Rule 6: `uiState` must be `StateFlow<*>` " +
                            "(found `${rawType.substringAfter(':').substringBefore('=').trim()}`).",
                    )
                }
            }
        }

    companion object {
        val ISSUE = Issue.create(
            id = "SingleUiState",
            briefDescription = "ViewModel must publish exactly one `uiState: StateFlow<*>`",
            explanation = "R22: a ViewModel exposes a single `val uiState: StateFlow<*>` " +
                "so the UI can hoist the read into one `collectAsStateWithLifecycle()` site. " +
                "Multiple `uiState` properties (or non-StateFlow types named `uiState`) " +
                "indicate accidental split state that must be migrated before Phase 4.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(
                SingleUiStateRule::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}