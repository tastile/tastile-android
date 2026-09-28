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
        dpBaseline: Map<String, Int>? = null,
        approvedColors: Set<String> = emptySet(),
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

        // Rule 8: raw <N>.dp in uiConsumerRoots, as a per-file ratchet against
        // `dpBaseline`. Mirrors `app/build.gradle.kts`.
        val rawDp = Regex("""(\d+(?:\.\d+)?)\.dp""")
        val exemptDp = setOf("0", "0.0", "0.5", "1", "1.0")
        // `src/main/java/app/tastile/android/ui/dashboard` -> `dashboard/`, so the
        // baseline keys match the repository asset file.
        val uiPrefix = uiConsumerRoots.firstOrNull()?.parentFile?.path
            ?.replace(File.separatorChar, '/')?.plus("/").orEmpty()
        val rawDpByFile = linkedMapOf<String, Int>()
        uiConsumerRoots.forEach { root ->
            if (!root.exists()) return@forEach
            root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val count = file.readText().lines().sumOf { line ->
                    rawDp.findAll(line).count { it.groupValues[1] !in exemptDp }
                }
                if (count > 0) {
                    rawDpByFile[
                        file.path.replace(File.separatorChar, '/').removePrefix(uiPrefix)
                    ] = count
                }
            }
        }
        if (dpBaseline == null) {
            rawDpByFile.forEach { (path, count) ->
                violations += "$path: $count raw `<N>.dp` literal(s) (Rule 8; no baseline supplied)"
            }
        } else {
            (rawDpByFile.keys - dpBaseline.keys).sorted().forEach { path ->
                violations += "$path: ${rawDpByFile.getValue(path)} new raw `<N>.dp` literal(s) with no Rule 8 baseline entry."
            }
            dpBaseline.keys.sorted().forEach { path ->
                val allowed = dpBaseline.getValue(path)
                val actual = rawDpByFile[path] ?: 0
                when {
                    actual > allowed -> violations += "$path: raw `<N>.dp` count rose from $allowed to $actual."
                    actual in 1 until allowed ->
                        violations += "$path: raw `<N>.dp` count fell from $allowed to $actual. Lower the baseline."
                    actual == 0 && allowed > 0 ->
                        violations += "$path: no raw `<N>.dp` literals remain but the Rule 8 baseline still allows $allowed."
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
        // `0xFF000000 or x` is an alpha mask, not a color. Otherwise a literal is
        // allowed only for a symbol listed in `approvedColors` as `path#symbol`.
        val hexColor = Regex("""Color\(\s*0[xX][0-9A-Fa-f]{6,8}""")
        val alphaMask = Regex("""0[xX]FF000000L?\s+or\b""")
        val declaration = Regex(
            "^\\s*(?:@\\w+\\s+)*(?:(?:private|internal|public|protected|override|const|lateinit|open|final|suspend)\\s+)*(?:val|var|fun)\\s+([A-Za-z_][A-Za-z0-9_]*)"
        )
        val approvedSeen = mutableSetOf<String>()
        uiConsumerRoots.forEach { root ->
            if (!root.exists()) return@forEach
            root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val path = file.path.replace(File.separatorChar, '/')
                if (path.endsWith("core/designsystem/theme/Color.kt") ||
                    path.endsWith("designsystem/theme/Color.kt")
                ) return@forEach
                val relative = path.removePrefix(uiPrefix)
                val lines = file.readText().lines()
                lines.forEachIndexed { idx, line ->
                    if (!hexColor.containsMatchIn(line)) return@forEachIndexed
                    if (alphaMask.containsMatchIn(line)) return@forEachIndexed
                    val symbol = (idx downTo 0)
                        .mapNotNull { declaration.matchEntire(lines[it]) }
                        .map { it.groupValues[1] }
                        .firstOrNull()
                        ?: "<unknown>"
                    val key = "$relative#$symbol"
                    if (approvedColors.contains(key)) {
                        approvedSeen += key
                    } else {
                        violations += "${file.path}:${idx + 1}: hardcoded Color(0xFF...) literal in ui/ (Rule 10) in `$symbol`."
                    }
                }
            }
        }
        (approvedColors - approvedSeen).sorted().forEach { key ->
            violations += "approved Rule 10 entry `$key` no longer matches any Color(0xFF...) literal."
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
    // These cases pin the gate against the same defect, and pin the ratchet.
    @Test fun `flags raw dp in ui-consumer tree`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Bad.kt", "val gap = 16.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(ui, ui, listOf(ui), dpBaseline = mapOf("dashboard/Bad.kt" to 1))
        }
        assert(ex.message!!.contains("count rose from 1 to 1") || ex.message!!.contains("no Rule 8 baseline entry"))
    }

    @Test fun `flags raw dp that follows an exempt value on the same line`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Bad.kt", "val a = 0.dp; val b = 16.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(ui, ui, listOf(ui))
        }
        assert(ex.message!!.contains("Rule 8"))
    }

    @Test fun `flags raw dp on a line after an exempt-only line`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Bad.kt", "val hairline = 0.5.dp\nval gap = 24.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(ui, ui, listOf(ui))
        }
        assert(ex.message!!.contains("Rule 8"))
    }

    @Test fun `allows the documented raw dp exemptions`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Ok.kt", "val a = 0.dp\nval b = 0.5.dp\nval c = 1.dp\n")
        checkDesignSystemRules(ui, ui, listOf(ui))
    }

    @Test fun `allows raw dp outside the ui-consumer tree`() {
        val src = tmp.newFolder("src")
        val other = tmp.newFolder("src/other")
        makeFile(other, "Ok.kt", "val gap = 16.dp\n")
        checkDesignSystemRules(src, File(src, "designsystem"), emptyList())
    }

    @Test fun `ratchet lets existing per-file debt stand`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Bad.kt", "val a = 16.dp\nval b = 24.dp\n")
        // No exception: the per-file count matches the baseline exactly.
        checkDesignSystemRules(ui, ui, listOf(ui), dpBaseline = mapOf("dashboard/Bad.kt" to 2))
    }

    @Test fun `ratchet fails when a file gains debt`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Bad.kt", "val a = 16.dp\nval b = 24.dp\nval c = 32.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(ui, ui, listOf(ui), dpBaseline = mapOf("dashboard/Bad.kt" to 2))
        }
        assert(ex.message!!.contains("count rose from 2 to 3"))
    }

    @Test fun `ratchet fails when debt shrinks without lowering the baseline`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Bad.kt", "val a = 16.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(ui, ui, listOf(ui), dpBaseline = mapOf("dashboard/Bad.kt" to 2))
        }
        assert(ex.message!!.contains("count fell from 2 to 1"))
    }

    @Test fun `ratchet fails when debt is fully removed without dropping the entry`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Ok.kt", "val a = LocalTastileLayoutTokens.current.gutter\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(ui, ui, listOf(ui), dpBaseline = mapOf("dashboard/Ok.kt" to 1))
        }
        assert(ex.message!!.contains("no raw `<N>.dp` literals remain"))
    }

    @Test fun `ratchet fails when a new file adds debt`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "New.kt", "val a = 16.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(ui, ui, listOf(ui), dpBaseline = mapOf("dashboard/Other.kt" to 1))
        }
        assert(ex.message!!.contains("no Rule 8 baseline entry"))
    }

    @Test fun `ratchet keeps a clean file clean`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Ok.kt", "val a = 0.dp\n")
        checkDesignSystemRules(ui, ui, listOf(ui), dpBaseline = emptyMap())
    }

    @Test fun `flags hardcoded shadowElevation dp in ui-consumer tree`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Bad.kt", "val shadowElevation = 3.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(ui, ui, listOf(ui))
        }
        assert(ex.message!!.contains("shadowElevation"))
    }

    @Test fun `flags hardcoded hex color that has no exemption`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Bad.kt", "private val brand = Color(0xFF4C6EF5)\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(ui, ui, listOf(ui))
        }
        assert(ex.message!!.contains("Rule 10") && ex.message!!.contains("brand"))
    }

    @Test fun `allows hex color for a symbol carrying a reasoned exemption`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Ok.kt", "private val StatusGreen = Color(0xFF0D8A72)\n")
        checkDesignSystemRules(
            ui, ui, listOf(ui),
            approvedColors = setOf("dashboard/Ok.kt#StatusGreen"),
        )
    }

    @Test fun `flags a stale Rule 10 exemption`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(ui, "Ok.kt", "val a = 0.dp\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(
                ui, ui, listOf(ui),
                approvedColors = setOf("dashboard/Gone.kt#Legacy"),
            )
        }
        assert(ex.message!!.contains("no longer matches"))
    }

    @Test fun `allows 0xFF000000 used as an alpha mask`() {
        val ui = tmp.newFolder("ui/dashboard")
        makeFile(
            ui, "Ok.kt",
            "private fun parseHex(hex: String): Color {\n" +
                "    val v = hex.toLong(16)\n" +
                "    return Color(0xFF000000 or v)\n" +
                "}\n",
        )
        checkDesignSystemRules(ui, ui, listOf(ui))
    }

    @Test fun `allows hardcoded hex color in designsystem theme Color file`() {
        val src = tmp.newFolder("src")
        val theme = tmp.newFolder("src/core/designsystem/theme")
        makeFile(theme, "Color.kt", "val brand = Color(0xFF4C6EF5)\n")
        checkDesignSystemRules(src, File(src, "designsystem"), emptyList())
    }
}
