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
 * The "more than one" branch is defensive only: Kotlin rejects two properties
 * with the same name in a class, so it cannot trigger for code that compiles.
 * The load-bearing checks are the zero-property and non-StateFlow cases.
 *
 * NOTE: this ships as a `:app:lint` warning, not a Gradle gate. The existing
 * screens do not conform yet, so making it a hard gate would fail the build.
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
                        context.getLocation(node.sourcePsi ?: return),
                        "Rule 6: ViewModel exposes ${uiStateProperties.size} `uiState` properties " +
                            "(expected exactly 1). Merge the redundant states into a single holder.",
                    )
                    return
                }

                // "exactly one" includes zero. A ViewModel that publishes no
                // `uiState` at all was passing silently, so the rule never
                // checked the existence half of its own contract.
                val single = uiStateProperties.firstOrNull()
                if (single == null) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node.sourcePsi ?: return),
                        "Rule 6: ViewModel exposes no `uiState` property (expected exactly 1). " +
                            "Publish a single `val uiState: StateFlow<*>` so the UI has one " +
                            "hoisted read site.",
                    )
                    return
                }

                val rawType = single.sourcePsi?.text ?: return
                val isStateFlow = rawType.contains("StateFlow")
                if (!isStateFlow) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node.sourcePsi ?: return),
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