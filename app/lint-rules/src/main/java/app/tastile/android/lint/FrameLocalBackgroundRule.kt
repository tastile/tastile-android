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
import org.jetbrains.uast.UFile

/**
 * Rule 4: a `Screen` / `Sheet` / `Frame` / `Dialog` / `Panel` / `Scaffold`
 * composable must import either `LocalBackgroundTheme` or `Surface` (the
 * design-system surface entry points) so the tonal / elevation theme is applied
 * consistently.
 *
 * Without this rule new top-level composables can accidentally render on a
 * transparent background, which breaks:
 *   - the light/dark theme contract (`MaterialTheme.colorScheme.background`
 *     is not applied),
 *   - the elevation-token contract (`Surface` overlays do not pick up the
 *     tonal elevation tint),
 *   - the contrast contract in accessibility settings.
 *
 * The check is per file: the decision only needs the file's import list, and one
 * compliant import clears every frame composable in that file. `UFile` is used
 * purely as the per-file hook lint guarantees once per file, and the
 * declarations are read from the text. That keeps the rule independent of how
 * the UAST visitor descends into top-level declarations, which is what made a
 * top-level `fun FooScreen()` unreachable when the scan was driven from classes.
 *
 * NOTE: this ships as a `:app:lint` warning, not a Gradle gate. The existing
 * 200+ screens do not conform yet, so making it a hard gate would fail the build.
 * It becomes a gate once those screens are migrated.
 */
class FrameLocalBackgroundRule : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UFile::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitFile(node: UFile) {
                val file = node.sourcePsi ?: return
                val text = file.text

                val offending = DECLARATION_PATTERN.findAll(text)
                    .map { it.groupValues[1] }
                    .distinct()
                    .filter { it.matches(FRAME_NAME_REGEX) }
                    .sorted()
                if (offending.isEmpty()) return

                if (text.contains(LOCAL_BACKGROUND_THEME)) return
                if (SURFACE_IMPORT.containsMatchIn(text)) return

                context.report(
                    ISSUE,
                    file,
                    context.getLocation(file),
                    "Rule 4: ${offending.joinToString()} " +
                        "is a Screen/Sheet/Frame/Dialog/Panel/Scaffold composable but this file " +
                        "imports neither `LocalBackgroundTheme` nor `Surface`. Add one of them " +
                        "so the tonal / elevation theme is applied.",
                )
            }
        }

    companion object {
        /** Matches function names ending in Screen/Sheet/Frame/Dialog/Panel/Scaffold. */
        val FRAME_NAME_REGEX = Regex(""".*(Screen|Sheet|Frame|Dialog|Panel|Scaffold)$""")

        /** Any Kotlin declaration whose name could be a frame composable. */
        val DECLARATION_PATTERN =
            Regex("""\b(?:fun|class|object|interface)\s+([A-Za-z_][A-Za-z0-9_]*)""")

        const val LOCAL_BACKGROUND_THEME = "LocalBackgroundTheme"

        /** `\b` keeps `import ... .SurfaceView` from matching. */
        val SURFACE_IMPORT = Regex("""import\s+(\S+\.)?Surface\b""")

        val ISSUE = Issue.create(
            id = "FrameLocalBackground",
            briefDescription = "Screen/Sheet/Frame/Dialog/Panel/Scaffold must use LocalBackgroundTheme or Surface",
            explanation = "Top-level composables whose name ends in `Screen` / `Sheet` / " +
                "`Frame` / `Dialog` / `Panel` / `Scaffold` must import either " +
                "`LocalBackgroundTheme` or `Surface` so the design-system background and " +
                "elevation are applied.",
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
