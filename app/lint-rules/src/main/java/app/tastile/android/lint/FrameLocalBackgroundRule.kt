package app.tastile.android.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile

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

    // The rule is a whole-file check: one compliant import clears every frame
    // composable in the file, and the file text is what the check reads. Driving
    // it from UFile also means a top-level `fun FooScreen()` — the main case —
    // is reached reliably, which a UClass/UMethod scan did not guarantee.
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UFile::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitFile(node: UFile) {
                val fileText = node.sourcePsi?.containingFile?.text ?: return
                reportFrameComposables(
                    context = context,
                    fileText = fileText,
                    candidates = frameCandidates(node),
                )
            }
        }

    /**
     * Collects every named declaration in [file] that could be a frame
     * composable: top-level functions, classes, class members, and members of
     * nested classes. Names are resolved here so the reporter needs no named
     * UAST interface — `UElement` does not carry one.
     */
    private fun frameCandidates(file: UFile): List<Pair<String?, PsiElement?>> =
        buildList {
            fun UClass.collect() {
                add(name to sourcePsi)
                methods.forEach { add(it.name to it.sourcePsi) }
                classes.forEach { it.collect() }
            }
            file.methods.forEach { add(it.name to it.sourcePsi) }
            file.classes.forEach { it.collect() }
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