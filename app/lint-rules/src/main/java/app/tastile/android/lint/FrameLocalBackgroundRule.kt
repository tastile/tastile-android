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
 * NOTE: Phase 1 ships the rule but does NOT enable it via
 * `verifyDesignSystemImports` (existing 200+ screens do not conform yet).
 * The Gradle task `:app:verifyFrameLocalBackground` is added so the rule
 * can be turned on in isolation. Phase 5 migrates the screens and turns
 * the rule into a hard gate.
 */
class FrameLocalBackgroundRule : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitClass(node: UClass) {
                val fileText = node.sourcePsi?.containingFile?.text ?: return
                // Helper: name match across both top-level `fun FooScreen()`
                // and methods declared in a class.
                val methods = buildList {
                    if (node.name.orEmpty().matches(FRAME_NAME_REGEX)) add(node)
                    addAll(node.methods.filter { method ->
                        method.name.orEmpty().matches(FRAME_NAME_REGEX)
                    })
                }
                if (methods.isEmpty()) return

                val hasLocalBg = fileText.contains("LocalBackgroundTheme")
                // Use `\b` so an import like `import androidx.compose.material3.Surface`
                // (which is normally terminated by `\n` or `;`) still matches.
                // Without `\b`, `import ... .SurfaceView` would false-positive.
                val hasSurface = Regex("""import\s+(\S+\.)?Surface\b""").containsMatchIn(fileText)

                if (hasLocalBg || hasSurface) return

                for (target in methods) {
                    context.report(
                        ISSUE,
                        target,
                        context.getLocation(target),
                        "Rule 4: `${target.name}` is a Screen/Sheet/Frame/Dialog/Panel/Scaffold " +
                            "composable but the file does not import `LocalBackgroundTheme` " +
                            "or `Surface`. Add one of those imports so the tonal / elevation " +
                            "theme is applied.",
                    )
                }
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