package app.tastile.android.buildlogic

import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VerifyDesignSystemImportsGuardTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun makeFile(parent: File, path: String, content: String): File {
        val f = File(parent, path)
        f.parentFile.mkdirs()
        f.writeText(content)
        return f
    }

    /**
     * Re-implements the same algorithm as `app/build.gradle.kts:collectDesignSystemViolations`.
     * The `:app:test` classpath cannot reach the build script's classloader (no `buildSrc/`
     * infrastructure in this repo), so the algorithm lives in two places. If you change the
     * build script's `collectDesignSystemViolations`, mirror the change here. Integration
     * coverage comes from running `:app:verifyDesignSystemImports` against the real source tree.
     */
    private fun checkDesignSystemRules(
        srcRoot: File,
        designSystemRoot: File,
        uiConsumerRoots: List<File>,
    ) {
        val allowMarker = "// m2-allow:"
        val forbiddenPrefix = "import androidx.compose.material3."
        val violations = mutableListOf<String>()

        // Rule 1: forbidden Material3 imports in uiConsumerRoots (mimicking the
        // `designSystemGuardFiles` precomputed list passed into the build script's function).
        uiConsumerRoots.forEach { root ->
            root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val lines = file.readText().lines()
                lines.forEachIndexed { idx, rawLine ->
                    val trimmed = rawLine.trimStart()
                    if (!trimmed.startsWith(forbiddenPrefix)) return@forEachIndexed
                    var i = idx - 1
                    var allowed = false
                    var foundPrev = false
                    while (i >= 0 && !foundPrev) {
                        val prev = lines[i].trim()
                        if (prev.isNotEmpty()) {
                            if (prev.startsWith(allowMarker)) allowed = true
                            foundPrev = true
                        }
                        i--
                    }
                    if (!allowed) violations += "${file.path}:${idx + 1}: forbidden Material3 import"
                }
            }
        }

        // Rule 2: MaterialTheme.colorScheme references in uiConsumerRoots.
        uiConsumerRoots.forEach { root ->
            root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val lines = file.readText().lines()
                lines.forEachIndexed { idx, line ->
                    if (line.contains("MaterialTheme.colorScheme") &&
                        (idx == 0 || !lines[idx - 1].trim().startsWith(allowMarker))) {
                        violations += "${file.path}:${idx + 1}: forbidden MaterialTheme.colorScheme reference"
                    }
                }
            }
        }

        // Rule 3: hardcoded RoundedCornerShape(<non-zero-numeric>.dp) outside designSystemRoot.
        srcRoot.walkTopDown()
            .filter { it.extension == "kt" && !it.startsWith(designSystemRoot) }
            .forEach { file ->
                file.readText().lines().forEachIndexed { idx, line ->
                    val match = Regex("""RoundedCornerShape\(\s*(\d+(?:\.\d+)?)\.dp\s*\)""").find(line)
                    if (match != null && match.groupValues[1].toDouble() != 0.0) {
                        violations += "${file.path}:${idx + 1}: hardcoded RoundedCornerShape(<non-zero-numeric>.dp)"
                    }
                }
            }

        if (violations.isNotEmpty()) {
            throw AssertionError(
                "Guard violations:\n" + violations.joinToString("\n") { "  - $it" }
            )
        }
    }

    @Test fun `flags MaterialTheme colorScheme in ui-dashboard`() {
        val src = tmp.newFolder("src")
        val uiDashboard = tmp.newFolder("src/ui/dashboard")
        makeFile(uiDashboard, "Bad.kt", "val x = MaterialTheme.colorScheme.primary\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(
                srcRoot = src,
                designSystemRoot = File(src, "designsystem"),
                uiConsumerRoots = listOf(uiDashboard),
            )
        }
        assert(ex.message!!.contains("MaterialTheme.colorScheme"))
    }

    @Test fun `allows MaterialTheme colorScheme preceded by m2-allow typography marker`() {
        val src = tmp.newFolder("src")
        val uiDashboard = tmp.newFolder("src/ui/dashboard")
        makeFile(
            uiDashboard,
            "Ok.kt",
            "// m2-allow: typography - reading MaterialTheme.typography.titleMedium\n" +
                "val x = MaterialTheme.colorScheme.primary\n",
        )
        // No exception is the assertion: JUnit @Test passes if no throw.
        checkDesignSystemRules(
            srcRoot = src,
            designSystemRoot = File(src, "designsystem"),
            uiConsumerRoots = listOf(uiDashboard),
        )
    }

    @Test fun `flags hardcoded RoundedCornerShape numeric dp outside designsystem`() {
        val src = tmp.newFolder("src")
        val other = tmp.newFolder("src/other")
        makeFile(other, "Bad.kt", "val x = RoundedCornerShape(12.dp)\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(
                srcRoot = src,
                designSystemRoot = File(src, "designsystem"),
                uiConsumerRoots = emptyList(),
            )
        }
        assert(ex.message!!.contains("RoundedCornerShape"))
    }

    @Test fun `allows RoundedCornerShape with LocalTastileShapeTokens reference anywhere`() {
        val src = tmp.newFolder("src")
        val other = tmp.newFolder("src/other")
        makeFile(other, "Ok.kt", "val x = RoundedCornerShape(LocalTastileShapeTokens.current.m)\n")
        // No exception is the assertion: JUnit @Test passes if no throw.
        checkDesignSystemRules(
            srcRoot = src,
            designSystemRoot = File(src, "designsystem"),
            uiConsumerRoots = emptyList(),
        )
    }
}