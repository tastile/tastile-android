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

        // Rule 8: raw <N>.dp in uiConsumerRoots (exceptions: 0.dp / 1.dp / 0.5.dp).
        val rawDp = Regex("""(\d+(?:\.\d+)?)\.dp""")
        val exemptDp = setOf("0", "0.0", "0.5", "1", "1.0")
        uiConsumerRoots.forEach { root ->
            root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                file.readText().lines().forEachIndexed { idx, line ->
                    rawDp.findAll(line).forEach { match ->
                        val raw = match.groupValues[1]
                        if (raw !in exemptDp) {
                            violations += "${file.path}:${idx + 1}: raw `${raw}.dp` literal in ui/ (Rule 8)"
                        }
                    }
                }
            }
        }

        // Rule 9: shadowElevation = N.dp in uiConsumerRoots.
        val shadowElevation = Regex("""shadowElevation\s*=\s*(\d+(?:\.\d+)?)\.dp""")
        uiConsumerRoots.forEach { root ->
            root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                file.readText().lines().forEachIndexed { idx, line ->
                    shadowElevation.find(line)?.let { match ->
                        violations +=
                            "${file.path}:${idx + 1}: shadowElevation = ${match.groupValues[1]}.dp in ui/ (Rule 9)"
                    }
                }
            }
        }

        // Rule 10: Color(0xFF...) in uiConsumerRoots, except designsystem/theme/Color.kt.
        val hexColor = Regex("""Color\(\s*0[xX][0-9A-Fa-f]{6,8}""")
        uiConsumerRoots.forEach { root ->
            root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val path = file.path.replace(File.separatorChar, '/')
                if (path.endsWith("core/designsystem/theme/Color.kt") ||
                    path.endsWith("designsystem/theme/Color.kt")
                ) return@forEach
                file.readText().lines().forEachIndexed { idx, line ->
                    hexColor.find(line)?.let {
                        violations += "${file.path}:${idx + 1}: hardcoded Color(0xFF...) literal in ui/ (Rule 10)"
                    }
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

    // Rule 8 is the gate counterpart of the retired NoRawDpInUiRule detector. The
    // detector took the first `.dp` match in a file and returned early when that
    // value was exempt, so a file opening with `0.dp` hid every later violation.
    // These cases pin the gate against the same defect.
    @Test fun `flags raw dp in ui-consumer tree`() {
        val src = tmp.newFolder("src")
        val ui = tmp.newFolder("src/ui/dashboard")
        makeFile(ui, "Bad.kt", "val gap = 16.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(
                srcRoot = src,
                designSystemRoot = File(src, "designsystem"),
                uiConsumerRoots = listOf(ui),
            )
        }
        assert(ex.message!!.contains("16.dp"))
    }

    @Test fun `flags raw dp that follows an exempt value on the same line`() {
        val src = tmp.newFolder("src")
        val ui = tmp.newFolder("src/ui/dashboard")
        makeFile(ui, "Bad.kt", "val a = 0.dp; val b = 16.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(
                srcRoot = src,
                designSystemRoot = File(src, "designsystem"),
                uiConsumerRoots = listOf(ui),
            )
        }
        assert(ex.message!!.contains("16.dp"))
    }

    @Test fun `flags raw dp on a line after an exempt-only line`() {
        val src = tmp.newFolder("src")
        val ui = tmp.newFolder("src/ui/dashboard")
        makeFile(ui, "Bad.kt", "val hairline = 0.5.dp\nval gap = 24.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(
                srcRoot = src,
                designSystemRoot = File(src, "designsystem"),
                uiConsumerRoots = listOf(ui),
            )
        }
        assert(ex.message!!.contains("24.dp"))
    }

    @Test fun `allows the documented raw dp exemptions`() {
        val src = tmp.newFolder("src")
        val ui = tmp.newFolder("src/ui/dashboard")
        makeFile(ui, "Ok.kt", "val a = 0.dp\nval b = 0.5.dp\nval c = 1.dp\n")
        // No exception is the assertion: JUnit @Test passes if no throw.
        checkDesignSystemRules(
            srcRoot = src,
            designSystemRoot = File(src, "designsystem"),
            uiConsumerRoots = listOf(ui),
        )
    }

    @Test fun `allows raw dp outside the ui-consumer tree`() {
        val src = tmp.newFolder("src")
        val other = tmp.newFolder("src/other")
        makeFile(other, "Ok.kt", "val gap = 16.dp\n")
        // No exception is the assertion: JUnit @Test passes if no throw.
        checkDesignSystemRules(
            srcRoot = src,
            designSystemRoot = File(src, "designsystem"),
            uiConsumerRoots = emptyList(),
        )
    }

    @Test fun `flags hardcoded shadowElevation dp in ui-consumer tree`() {
        val src = tmp.newFolder("src")
        val ui = tmp.newFolder("src/ui/dashboard")
        makeFile(ui, "Bad.kt", "val shadowElevation = 3.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(
                srcRoot = src,
                designSystemRoot = File(src, "designsystem"),
                uiConsumerRoots = listOf(ui),
            )
        }
        assert(ex.message!!.contains("shadowElevation"))
    }

    @Test fun `flags hardcoded hex color in ui-consumer tree`() {
        val src = tmp.newFolder("src")
        val ui = tmp.newFolder("src/ui/dashboard")
        makeFile(ui, "Bad.kt", "val brand = Color(0xFF4C6EF5)\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(
                srcRoot = src,
                designSystemRoot = File(src, "designsystem"),
                uiConsumerRoots = listOf(ui),
            )
        }
        assert(ex.message!!.contains("hardcoded Color"))
    }

    @Test fun `allows hardcoded hex color in designsystem theme Color file`() {
        val src = tmp.newFolder("src")
        val theme = tmp.newFolder("src/core/designsystem/theme")
        makeFile(theme, "Color.kt", "val brand = Color(0xFF4C6EF5)\n")
        // No exception is the assertion: JUnit @Test passes if no throw.
        checkDesignSystemRules(
            srcRoot = src,
            designSystemRoot = File(src, "designsystem"),
            uiConsumerRoots = emptyList(),
        )
    }
}