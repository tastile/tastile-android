package app.tastile.android.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter

/**
 * Rule C3: any @Composable wrapper in the design system that accepts a
 * `kotlin.Function*` parameter or a `androidx.compose.ui.graphics.Color`
 * parameter directly must be annotated `@Stable` (or `@Immutable` if it is
 * a data class).
 *
 * Compose can only skip recomposition for "stable" parameters; a
 * `() -> Unit` lambda is itself stable, but a `Color` value is only stable
 * if the wrapper is annotated so that Compose trusts the value's identity
 * will not change under equal inputs.
 */
class WrapperStabilityDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                if (!node.isComposable()) return
                if (!isInDesignSystemPackage(context)) return

                // Skip wrappers already marked stable.
                if (node.hasStableOrImmutableAnnotation()) return

                // Look for parameters whose type is `kotlin.Function*` or
                // `androidx.compose.ui.graphics.Color`.
                val unstableParams = node.uastParameters.filter { it.isUnstableParameter() }
                if (unstableParams.isEmpty()) return

                context.report(
                    ISSUE,
                    node,
                    context.getLocation(node),
                    "L0 C3: @Composable wrapper accepting lambda or Color must be " +
                        "annotated `@Stable` or `@Immutable` " +
                        "(found unstable params: ${unstableParams.joinToString { it.name }})",
                )
            }
        }

    private fun UParameter.isUnstableParameter(): Boolean {
        val typeText = type.canonicalText
        // When the test client compiles with `allowCompilationErrors()`,
        // imports may not resolve and the canonical text collapses to the
        // short name ("Color") instead of the FQN. Match either form.
        return typeText.contains("kotlin.Function") ||
            typeText.contains(".Function") ||
            typeText == "androidx.compose.ui.graphics.Color" ||
            typeText == "Color"
    }

    private fun UMethod.hasStableOrImmutableAnnotation(): Boolean {
        if (annotations.any {
                it.qualifiedName == "androidx.compose.runtime.Stable" ||
                    it.qualifiedName == "androidx.compose.runtime.Immutable"
            }
        ) {
            return true
        }
        // Fallback for unresolved annotations in the test client (when
        // `allowCompilationErrors()` is on, the qualified name may be empty).
        // Walk the raw PSI text for `@Stable` / `@Immutable` markers.
        val psi = sourcePsi ?: return false
        val text = psi.text
        return text.contains("@Stable") || text.contains("@Immutable")
    }

    private fun UMethod.isComposable(): Boolean {
        // First, use UAnnotation.qualifiedName when the annotation resolves.
        if (annotations.any { it.qualifiedName == "androidx.compose.runtime.Composable" }) {
            return true
        }
        // Fallback for unresolved annotations (e.g. when the test client runs
        // with `allowCompilationErrors()`): check the raw PSI text. Kotlin
        // source uses `@Composable(...)`, `@Composable` (no args).
        val psi = this.sourcePsi ?: return false
        return psi.text.contains("@Composable")
    }

    private fun isInDesignSystemPackage(context: JavaContext): Boolean {
        val pkg = context.uastFile?.packageName ?: return false
        return pkg == "app.tastile.android.ui.designsystem" ||
            pkg == "app.tastile.android.ui.mobile.designsystem" ||
            pkg == "app.tastile.android.core.designsystem"
    }

    companion object {
        val ISSUE = Issue.create(
            id = "WrapperStability",
            briefDescription = "Wrapper stability annotation required",
            explanation = "Wrappers that accept a lambda or `Color` directly must be " +
                "annotated `@Stable` (or `@Immutable` if they are a data class). " +
                "Without this annotation, Compose cannot skip recomposition of the wrapper " +
                "when its inputs are equal, leading to unnecessary recompositions and " +
                "degraded performance.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(
                WrapperStabilityDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
