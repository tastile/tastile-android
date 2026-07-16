package app.tastile.android.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod

class WrapperParameterOrderDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                System.err.println("[WPD-DEBUG] enter visitMethod name=${node.name}")
                val isComposable = node.isComposable()
                val inDesignSystem = isInDesignSystemPackage(context)
                System.err.println("[WPD-DEBUG] composable=$isComposable inDesignSystem=$inDesignSystem")
                if (!isComposable) return
                if (!inDesignSystem) return

                val psi = node.sourcePsi as? PsiMethod ?: run {
                    System.err.println("[WPD-DEBUG] psi is not PsiMethod: ${node.sourcePsi}")
                    return
                }
                val params = psi.parameterList.parameters
                System.err.println("[WPD-DEBUG] paramCount=${params.size} paramNames=${params.joinToString { it.name }}")
                if (params.size < 2) return

                // Rule C1: Modifier must be the first parameter.
                val firstParamName = params.firstOrNull()?.name
                if (firstParamName != "modifier") {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "L0 C1: a wrapper's first parameter must be `modifier: Modifier` " +
                            "(found `${firstParamName ?: "<none>"}`)",
                    )
                    return
                }

                // Rule C2: `enabled` must be the LAST optional parameter.
                // If any other parameter appears AFTER `enabled`, that's a C2 violation.
                val enabledIndex = params.indexOfFirst { it.name == "enabled" }
                if (enabledIndex >= 0 && enabledIndex < params.size - 1) {
                    val afterEnabled = params.drop(enabledIndex + 1).firstOrNull()?.name ?: "?"
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

    private fun UMethod.isComposable(): Boolean =
        annotations.any { ann ->
            val q = ann.qualifiedName
            // When the test client runs with `allowCompilationErrors()`, the
            // qualified name for an unresolved annotation comes back as null.
            // Match by qualified name when available, else fall back to the
            // simple-name of the annotation.
            q == "androidx.compose.runtime.Composable" ||
                (q == null && ann.name == "Composable")
        }

    private fun isInDesignSystemPackage(context: JavaContext): Boolean {
        val pkg = context.uastFile?.packageName ?: return false
        return pkg == "app.tastile.android.ui.designsystem" ||
            pkg == "app.tastile.android.ui.mobile.designsystem"
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
