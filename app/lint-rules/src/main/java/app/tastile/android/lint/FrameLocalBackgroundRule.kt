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
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod

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
                val fileText = node.sourcePsi?.containingFile?.text ?: return
                reportFrameComposables(
                    context = context,
                    fileText = fileText,
                    candidates = buildList {
                        add(node.name to node.sourcePsi)
                        node.methods.forEach { add(it.name to it.sourcePsi) }
                    },
                )
            }

            override fun visitMethod(node: UMethod) {
                // Class members are already covered by visitClass; only top-level
                // functions need handling here.
                if (node.uastParent is UClass) return
                val fileText = node.sourcePsi?.containingFile?.text ?: return
                reportFrameComposables(
                    context = context,
                    fileText = fileText,
                    candidates = listOf(node.name to node.sourcePsi),
                )
            }
        }

    /**
     * Reports every frame-shaped declaration in [candidates] when the declaring
     * file imports neither `LocalBackgroundTheme` nor `Surface`. The check is
     * per file, so one compliant import clears every frame composable in it.
     */
    private fun reportFrameComposables(
        context: JavaContext,
        fileText: String,
        candidates: List<Pair<String?, PsiElement?>>,
    ) {
        val targets = candidates.filter { (name, psi) ->
            name.orEmpty().matches(FRAME_NAME_REGEX) && psi != null
        }
        if (targets.isEmpty()) return

        val hasLocalBackgroundTheme = fileText.contains("LocalBackgroundTheme")
        // `\b` so an import like `import androidx.compose.material3.Surface`
        // (normally terminated by `\n` or `;`) still matches, while
        // `import ... .SurfaceView` does not false-positive.
        val hasSurface = Regex("""import\s+(\S+\.)?Surface\b""").containsMatchIn(fileText)
        if (hasLocalBackgroundTheme || hasSurface) return

        for ((name, psi) in targets) {
            // Report against the PSI node: a UElement scope is ambiguous
            // between the PsiElement and UElement overloads of report(), and
            // getLocation is ambiguous for the same reason.
            val target = psi ?: continue
            context.report(
                ISSUE,
                target,
                context.getLocation(target),
                "Rule 4: `$name` is a Screen/Sheet/Frame/Dialog/Panel/Scaffold " +
                    "composable but the file does not import `LocalBackgroundTheme` " +
                    "or `Surface`. Add one of those imports so the tonal / elevation " +
                    "theme is applied.",
            )
        }
    }

    companion object {
        // Matches function names ending in Screen/Sheet/Frame/Dialog/Panel/Scaffold.
        // There is deliberately no `\b` before the suffix group: the suffix is
        // part of the same camelCase word as the character in front of it, so
        // "FooScreen" has no word boundary between "o" and "S" and a `\b` here
        // made the pattern unmatchable. `matches()` already anchors both ends.
        val FRAME_NAME_REGEX = Regex(""".*(Screen|Sheet|Frame|Dialog|Panel|Scaffold)$""")

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