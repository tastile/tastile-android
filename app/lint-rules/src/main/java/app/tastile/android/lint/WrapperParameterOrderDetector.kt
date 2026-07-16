package app.tastile.android.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter

class WrapperParameterOrderDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                if (!node.isComposable()) return
                if (!isInDesignSystemPackage(context)) return

                // Use UParameter list (UAST-level) so the detector works on
                // Kotlin source where the PSI element is a KtNamedFunction
                // rather than a Java PsiMethod.
                val uastParams = node.uastParameters
                if (uastParams.size < 2) return
                if (isNonWrapper(uastParams)) return

                // Rule C1: configurable wrapper slots require modifier first.
                val firstParam = uastParams.firstOrNull()
                if (firstParam?.name != "modifier" && firstParam?.hasDefaultValue() == true) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "L0 C1: a wrapper's first parameter must be `modifier: Modifier` " +
                            "(found `${firstParam.name}`)",
                    )
                    return
                }

                // Rule C2: `enabled` must be the last optional parameter.
                val enabledIndex = uastParams.indexOfFirst { it.name == "enabled" }
                if (enabledIndex >= 0 && uastParams.drop(enabledIndex + 1)
                        .any { it.hasDefaultValue() }) {
                    val afterEnabled = uastParams.drop(enabledIndex + 1)
                        .first { it.hasDefaultValue() }.name
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "L0 C2: `enabled` must be the last optional parameter " +
                            "(found `$afterEnabled` after `enabled`)",
                    )
                }
            }
        }

    private fun isNonWrapper(params: List<UParameter>): Boolean =
        params.none { it.name == "modifier" } &&
            params.none { it.name == "enabled" && it.hasDefaultValue() } &&
            params.none { it.hasDefaultValue() }

    private fun UParameter.hasDefaultValue(): Boolean =
        sourcePsi?.text?.contains("=") == true

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
            id = "WrapperParameterOrder",
            briefDescription = "Wrapper parameter order violates L0 conventions",
            explanation = "C1: `modifier: Modifier` must be the first parameter. " +
                "C2: `enabled: Boolean = true` must be the last parameter.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                WrapperParameterOrderDetector::class.java,
                com.android.tools.lint.detector.api.Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
