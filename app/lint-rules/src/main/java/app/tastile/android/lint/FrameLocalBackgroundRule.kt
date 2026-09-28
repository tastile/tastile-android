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
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UNamedElement

/**
 * Rule 4: any `@Composable fun` whose name ends in `Screen` / `Sheet` /
 * `Frame` / `Dialog` / `Panel` / `Scaffold` must import either
 * `LocalBackgroundTheme` or `Surface` (the design-system surface entry
 * points) so the tonal / elevation theme is applied consistently.
 *
 * Without this rule new top-level composables can accidentally render on
 * a transparent background, which breaks:
 *   - the light/dark theme contract (`MaterialTheme.colorScheme.background`
 *     is not applied),
 *   - the elevation-token contract (`Surface` overlays do not pick up the
 *     tonal elevation tint),
 *   - the contrast contract in accessibility settings.
 *
 * NOTE: this ships as a `:app:lint` warning, not a Gradle gate. The existing
 * 200+ screens do not conform yet, so making it a hard gate would fail the build.
 * It becomes a gate once those screens are migrated.
 */
class FrameLocalBackgroundRule : Detector(), Detector.UastScanner {

    // Both types are needed. A frame composable is usually a top-level
    // `fun FooScreen()`, which never appears in a UClass, so scanning classes
    // alone silently missed the main case this rule exists for.
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UClass::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {

            override fun visitClass(node: UClass) {
                checkTargets(context, listOf(node) + node.methods.toList())
            }

            override fun visitMethod(node: UMethod) {
                // Class members are already covered by visitClass; only top-level
                // functions need handling here.
                if (node.uastParent is UClass) return
                checkTargets(context, listOf(node))
            }
        }

    /**
     * Reports every frame-shaped declaration in [candidates] when the declaring
     * file imports neither `LocalBackgroundTheme` nor `Surface`. The check is
     * per file, so one compliant import clears every frame composable in it.
     */
    private fun checkTargets(
        context: JavaContext,
        candidates: List<out UNamedElement>,
    ) {
        val targets = candidates.filter { it.name.orEmpty().matches(FRAME_NAME_REGEX) }
        if (targets.isEmpty()) return

        val fileText = targets.first().sourcePsi?.containingFile?.text ?: return
        val hasLocalBackgroundTheme = fileText.contains("LocalBackgroundTheme")
        // `\b` so an import like `import androidx.compose.material3.Surface`
        // (normally terminated by `\n` or `;`) still matches, while
        // `import ... .SurfaceView` does not false-positive.
        val hasSurface = Regex("""import\s+(\S+\.)?Surface\b""").containsMatchIn(fileText)
        if (hasLocalBackgroundTheme || hasSurface) return

        for (target in targets) {
            // Report against the PSI node: a UElement scope is ambiguous
            // between the PsiElement and UElement overloads of report(), and
            // getLocation is ambiguous for the same reason.
            val psi = target.sourcePsi ?: continue
            context.report(
                ISSUE,
                psi,
                context.getLocation(psi),
                "Rule 4: `${target.name}` is a Screen/Sheet/Frame/Dialog/Panel/Scaffold " +
                    "composable but the file does not import `LocalBackgroundTheme` " +
                    "or `Surface`. Add one of those imports so the tonal / elevation " +
                    "theme is applied.",
            )
        }
    }

    companion object {
        // Matches function names whose last suffix is Screen/Sheet/Frame/Dialog/Panel/Scaffold.
        val FRAME_NAME_REGEX = Regex(""".*\b(Screen|Sheet|Frame|Dialog|Panel|Scaffold)$""")

        val ISSUE = Issue.create(
            id = "FrameLocalBackground",
            briefDescription = "Screen/Sheet/Frame/Dialog/Panel/Scaffold must use LocalBackgroundTheme or Surface",
            explanation = "Top-level composables whose name ends in `Screen` / `Sheet` / " +
                "`Frame` / `Dialog` / `Panel` / `Scaffold` must import either " +
                "`LocalBackgroundTheme` or `Surface` so the tonal / elevation theme " +
                "is applied consistently. Without one of those imports the surface " +
                "renders transparent and breaks the theme contract.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(
                FrameLocalBackgroundRule::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}