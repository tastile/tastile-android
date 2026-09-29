plugins {
    id("com.android.application")
    // id("org.jetbrains.kotlin.android")  // auto-applied by AGP 9.x
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.openapi.generator")
    jacoco
}

fun configuredValue(name: String) = providers.environmentVariable(name)

val releaseStoreFile = configuredValue("RELEASE_STORE_FILE")
val releaseStorePassword = configuredValue("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = configuredValue("RELEASE_KEY_ALIAS")
val releaseKeyPassword = configuredValue("RELEASE_KEY_PASSWORD")
val googleWebClientId = configuredValue("GOOGLE_WEB_CLIENT_ID")
val googleAndroidClientId = configuredValue("GOOGLE_ANDROID_CLIENT_ID")
val webBaseUrl = configuredValue("WEB_BASE_URL")
val tastileCoreUrl = configuredValue("TASTILE_CORE_URL")
val hasReleaseSigning =
    releaseStoreFile.isPresent &&
        releaseStorePassword.isPresent &&
        releaseKeyAlias.isPresent &&
        releaseKeyPassword.isPresent

extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "app.tastile.android"
    compileSdk = 37
    ndkVersion = "27.1.12297006"

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    defaultConfig {
        applicationId = "app.tastile.android"
        minSdk = 26
        targetSdk = 35
        // Play has already accepted versionCode 31. Keep the checked-in
        // release baseline monotonic; CI must never re-upload that artifact.
        versionCode = 33
        versionName = "0.6.0"

        // R17 (android-archdoc audit 2026-07-16): instrumented UI navigation tests.
        // The runner swaps the production Application for Hilt's HiltTestApplication
        // so per-test Hilt @TestInstallIn modules can swap repositories.
        testInstrumentationRunner = "app.tastile.android.util.TastileTestRunner"

        // R18 (android refactor 2026-07-22): no Kotlin-level fallback defaults.
        // All production values come from Infisical environment variables.
        // Empty strings are validated at the bottom of this file via the
        // requireGradleProperty guard so a partial config fails the build fast
        // instead of silently embedding the wrong environment.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleWebClientId.orNull ?: ""}\"")
        buildConfigField("String", "GOOGLE_ANDROID_CLIENT_ID", "\"${googleAndroidClientId.orNull ?: ""}\"")
        buildConfigField("String", "WEB_BASE_URL", "\"${webBaseUrl.orNull ?: ""}\"")
        buildConfigField("String", "TASTILE_CORE_URL", "\"${tastileCoreUrl.orNull ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // M3 baseline (2026-07-16): enable Compose Compiler Reports so the next
    // successful Kotlin compile drops HTML stability reports under
    // app/build/compose-reports/ and metrics under app/build/compose-metrics/.
    // Captured baseline lives at docs/superpowers/m3/before-reports/.
    // AGP 9.x removed the AndroidExtension.composeOptions DSL; the compose
    // plugin wires these via kotlin.compilerOptions.freeCompilerArgs.
    // (2026-07-23) Re-enabled all 5 disabled lint rules. OldTargetApi stays
    // active; if the API-36 SDK remains unavailable, the warning will surface
    // and must be addressed by either installing the platform or bumping
    // targetSdk down — see app/lint-baseline-old-target-api.md for tracking.
    lint {
        // No `disable +=` block: every lint rule must surface so warnings
        // are root-fixed rather than hidden. Track any unaddressable rule
        // in a tracking doc with a hard BLOCKED rationale, never here.
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    // JaCoCo coverage threshold policy: 80% on lines / branches / methods /
    // instructions. See tasks.register("testDebugUnitTestCoverage") below for
    // the enforcement task. Excluding generated BuildConfig / R / Manifest
    // classes is enforced inside the task via `classDirectories.exclude(...)`.
    testCoverage {
        jacocoVersion = "0.8.15"
    }
}

kotlin {
    compilerOptions {
        // KT-73255: future-proof Hilt qualifier annotations (e.g. @ApplicationContext)
        // so they apply to both the value parameter and the backing field.
        freeCompilerArgs.addAll(
            "-Xannotation-default-target=param-property",
        )
        // M3 baseline (2026-07-16): enable Compose Compiler Reports so the next
        // successful Kotlin compile drops HTML stability reports under
        // app/build/compose-reports/ and metrics under app/build/compose-metrics/.
        // Captured baseline lives at docs/superpowers/m3/before-reports/.
        freeCompilerArgs.addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                project.layout.projectDirectory.dir("build/compose-reports").asFile.absolutePath,
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                project.layout.projectDirectory.dir("build/compose-metrics").asFile.absolutePath,
        )
    }
}

// In AGP 9.0+, Kotlin is integrated.
// We can use the extension if it exists, or just rely on defaults.

val releaseSigningInstructions = """
Release signing is not configured.
Authenticate with the tastile-android Infisical project and run release tasks through `infisical run`.
""".trimIndent()

gradle.taskGraph.whenReady {
    val requestedReleaseBuild =
        allTasks.any { task ->
            task.project == project && (task.name == "assembleRelease" || task.name == "bundleRelease")
        }
    if (requestedReleaseBuild && !hasReleaseSigning) {
        throw GradleException(releaseSigningInstructions)
    }
}

val designSystemGuardRoots = listOf(
    "src/main/java/app/tastile/android/ui/dashboard",
    "src/main/java/app/tastile/android/ui/mobile",
    "src/main/java/app/tastile/android/ui/account",
)
val designSystemGuardFiles: List<File> =
    designSystemGuardRoots.flatMap { root ->
        project.fileTree(root) { include("**/*.kt") }.files
    }

tasks.register("verifyDesignSystemImports") {
    group = "verification"
    description = "Disallow direct Material3 imports and colorScheme references in M3-unified screens; forbid hardcoded RoundedCornerShape(N.dp) outside design-system; ratchet raw dp debt; gate shadowElevation and hardcoded colors"
    doLast {
        val violations = collectDesignSystemViolations(
            designSystemGuardFiles = designSystemGuardFiles,
            uiConsumerRoots = listOf(
                layout.projectDirectory.dir("src/main/java/app/tastile/android/ui/dashboard").asFile,
                layout.projectDirectory.dir("src/main/java/app/tastile/android/ui/mobile").asFile,
                layout.projectDirectory.dir("src/main/java/app/tastile/android/ui/account").asFile,
            ),
            designSystemRoot = layout.projectDirectory
                .dir("src/main/java/app/tastile/android/core/designsystem").asFile,
            allKtRoot = layout.projectDirectory
                .dir("src/main/java/app/tastile/android").asFile,
            dpBaselineFile = layout.projectDirectory
                .file("src/main/assets/design_system_rule8_dp_baseline.json").asFile,
            approvedColorsFile = layout.projectDirectory
                .file("src/main/assets/design_system_rule10_approved_colors.json").asFile,
        )
        check(violations.isEmpty()) { formatDesignSystemViolations(violations) }
    }
}

/**
 * Collect every guard violation across the enforced rules:
 *  - Rule 1: forbidden Material3 imports (uses [designSystemGuardFiles] + the `// m2-allow:` marker)
 *  - Rule 2: `MaterialTheme.colorScheme` references in [uiConsumerRoots] without `// m2-allow:` marker
 *  - Rule 3: hardcoded `RoundedCornerShape(<non-zero-numeric>.dp)` outside [designSystemRoot]
 *  - Rule 8: raw `<N>.dp` literals in `ui/`, as a **per-file ratchet** against
 *    [dpBaselineFile]. A file's count may never rise, and must fall in the same
 *    change that removes the violations, so the debt decreases monotonically.
 *  - Rule 9: `shadowElevation = N.dp` literals in `ui/`. Zero-tolerance; there is
 *    no legitimate existing debt.
 *  - Rule 10: `Color(0xFF...)` literals in `ui/` outside `designsystem/theme/Color.kt`.
 *    Semantic colors approved by `path` + `symbol` + `purpose` in
 *    [approvedColorsFile] are allowed; every other literal fails. `0xFF000000`
 *    on the left of a bitwise `or` is an alpha mask, not a color, and is allowed.
 *
 * Rules 4 (FrameLocalBackground) and 6 (SingleUiState) ship as UAST lint detectors
 * in `:lint-rules` because the existing screens do not conform yet. Rule 7
 * (SingleComposer) ships with the Phase 4 TileComposer work, once the use cases it
 * inspects exist.
 *
 * Exposed at top level so the unit test (`app/src/test/.../buildlogic/VerifyDesignSystemImportsGuardTest.kt`)
 * can re-invoke the same algorithm against synthetic tmp dirs. The test re-implements the body to
 * avoid coupling `:app:test` to the build script classloader (no `buildSrc/` infrastructure exists).
 */
fun collectDesignSystemViolations(
    designSystemGuardFiles: List<File>,
    uiConsumerRoots: List<File>,
    designSystemRoot: File,
    allKtRoot: File,
    dpBaselineFile: File? = null,
    approvedColorsFile: File? = null,
): List<String> {
    val allowMarker = "// m2-allow:"
    val forbiddenPrefix = "import androidx.compose.material3."
    val violations = mutableListOf<String>()

    designSystemGuardFiles.filter { it.exists() }.forEach { file ->
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

    allKtRoot.walkTopDown()
        .filter { it.extension == "kt" && !it.startsWith(designSystemRoot) }
        .forEach { file ->
            file.readText().lines().forEachIndexed { idx, line ->
                val match = Regex("""RoundedCornerShape\(\s*(\d+(?:\.\d+)?)\.dp\s*\)""").find(line)
                if (match != null && match.groupValues[1].toDouble() != 0.0) {
                    violations += "${file.path}:${idx + 1}: hardcoded RoundedCornerShape(<non-zero-numeric>.dp)"
                }
            }
        }

    // Rule 8: raw <N>.dp in ui/ (exceptions: 0.dp / 1.dp / 0.5.dp), enforced as a
    // per-file ratchet. A repository-wide allowance would let new debt appear
    // anywhere while a single file migrates, so the baseline is keyed by file and
    // must move with the code.
    val rawDp = Regex("""(\d+(?:\.\d+)?)\.dp""")
    val exemptDp = setOf("0", "0.0", "0.5", "1", "1.0")
    val uiPrefix = allKtRoot.path.replace(File.separatorChar, '/') + "/"
    val rawDpByFile = linkedMapOf<String, Int>()
    uiConsumerRoots.forEach { root ->
        if (!root.exists()) return@forEach
        root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val lines = file.readText().lines()
            var count = 0
            lines.forEach { line ->
                rawDp.findAll(line).forEach { match ->
                    if (match.groupValues[1] !in exemptDp) count++
                }
            }
            if (count > 0) {
                rawDpByFile[file.path.replace(File.separatorChar, '/').removePrefix(uiPrefix)] = count
            }
        }
    }
    val dpBaseline = if (dpBaselineFile != null && dpBaselineFile.exists()) {
        val slurper = groovy.json.JsonSlurper()
        @Suppress("UNCHECKED_CAST")
        val parsed = slurper.parse(dpBaselineFile) as Map<*, *>
        @Suppress("UNCHECKED_CAST")
        (parsed["files"] as? Map<*, *> ?: emptyMap<String, Any>())
            .mapNotNull { (k, v) -> (k as? String)?.let { it to (v as? Int ?: 0) } }
            .toMap()
    } else {
        emptyMap()
    }
    if (dpBaselineFile != null) {
        (rawDpByFile.keys - dpBaseline.keys).sorted().forEach { path ->
            val count = rawDpByFile.getValue(path)
            violations += "$path: $count new raw `<N>.dp` literal(s) with no Rule 8 baseline entry. " +
                "Route the new code through LocalTastileLayoutTokens; do not add debt."
        }
        dpBaseline.keys.sorted().forEach { path ->
            val allowed = dpBaseline.getValue(path)
            val actual = rawDpByFile[path] ?: 0
            when {
                actual > allowed ->
                    violations += "$path: raw `<N>.dp` count rose from $allowed to $actual. " +
                        "Rule 8 is a ratchet; existing debt may shrink but never grow."
                actual in 1 until allowed ->
                    violations += "$path: raw `<N>.dp` count fell from $allowed to $actual. " +
                        "Lower the entry in ${dpBaselineFile.name} in the same change so the " +
                        "ratchet keeps decreasing."
                actual == 0 && allowed > 0 ->
                    violations += "$path: no raw `<N>.dp` literals remain but the Rule 8 " +
                        "baseline still allows $allowed. Drop the entry from " +
                        "${dpBaselineFile.name}."
            }
        }
    } else {
        rawDpByFile.forEach { (path, count) ->
            violations += "$path: $count raw `<N>.dp` literal(s) (Rule 8; no baseline supplied)"
        }
    }

    // Rule 9: shadowElevation = N.dp in ui/. Zero tolerance; the existing debt was
    // 2 sites and both now read LocalTastileSurfaceElevationTokens.
    val shadowElevation = Regex("""shadowElevation\s*=\s*(\d+(?:\.\d+)?)\.dp""")
    uiConsumerRoots.forEach { root ->
        if (!root.exists()) return@forEach
        root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            file.readText().lines().forEachIndexed { idx, line ->
                shadowElevation.find(line)?.let { match ->
                    val raw = match.groupValues[1]
                    violations += "${file.path}:${idx + 1}: shadowElevation = ${raw}.dp in ui/ (Rule 9)"
                }
            }
        }
    }

    // Rule 10: Color(0xFF...) in ui/, except designsystem/theme/Color.kt.
    //
    // Policy, in order:
    //  1. `0xFF000000` on the left of a bitwise `or` is an alpha mask, not a color.
    //     The RGB comes from parsed user data, so there is no literal to route.
    //  2. A semantic color approved by `path` + `symbol` + `purpose` is allowed.
    //  3. Everything else is a violation.
    //
    // The enclosing symbol is the nearest preceding `val` / `var` / `fun`
    // declaration, so an approved entry follows a rename. An entry that no longer
    // matches a literal is reported so the registry cannot rot.
    val hexColor = Regex("""Color\(\s*0[xX][0-9A-Fa-f]{6,8}""")
    val alphaMask = Regex("""0[xX]FF000000L?\s+or\b""")
    val declaration = Regex(
        """^\s*(?:@\w+\s+)*(?:(?:private|internal|public|protected|override|const|lateinit|open|final|suspend)\s+)*(?:val|var|fun)\s+([A-Za-z_][A-Za-z0-9_]*)"""
    )
    val approved = if (approvedColorsFile != null && approvedColorsFile.exists()) {
        val slurper = groovy.json.JsonSlurper()
        @Suppress("UNCHECKED_CAST")
        val parsed = slurper.parse(approvedColorsFile) as Map<*, *>
        (parsed["approved"] as? List<*> ?: emptyList<Any>())
            .filterIsInstance<Map<*, *>>()
            .mapNotNull { entry ->
                val p = entry["path"] as? String
                val s = entry["symbol"] as? String
                if (p != null && s != null) "$p#$s" else null
            }
            .toSet()
    } else {
        emptySet()
    }
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
                    .mapNotNull { declaration.find(lines[it]) }
                    .map { it.groupValues[1] }
                    .firstOrNull()
                    ?: "<unknown>"
                val key = "$relative#$symbol"
                if (approved.contains(key)) {
                    approvedSeen += key
                } else {
                    violations += "${file.path}:${idx + 1}: hardcoded Color(0xFF...) literal in ui/ " +
                        "(Rule 10) in `$symbol`. Route it through the design-system color layer, or " +
                        "record a reasoned exemption for `$key` in ${approvedColorsFile?.name}."
                }
            }
        }
    }
    if (approvedColorsFile != null) {
        (approved - approvedSeen).sorted().forEach { key ->
            violations += "${approvedColorsFile.path}: approved Rule 10 entry `$key` no longer " +
                "matches any Color(0xFF...) literal. Remove the stale exemption."
        }
    }

    return violations
}

fun formatDesignSystemViolations(violations: List<String>): String = buildString {
    if (violations.isEmpty()) return@buildString
    appendLine("verifyDesignSystemImports found ${violations.size} violation(s):")
    violations.forEach { appendLine("  - $it") }
    appendLine()
    appendLine("Use LocalTastileCardRoleTokens.current / LocalTastileStatusTokens.current instead of MaterialTheme.colorScheme.")
    appendLine("Use RoundedCornerShape(LocalTastileShapeTokens.current.<key>) instead of hardcoded <n>.dp shapes.")
    appendLine("Direct Material3 imports require an immediately-preceding `// m2-allow:` marker line.")
    appendLine("Raw `<N>.dp` literals (Rule 8) must route through LocalTastileLayoutTokens.current.*.")
    appendLine("Rule 8 is a per-file ratchet: debt may shrink, never grow. Move the baseline in the same change.")
    appendLine("`shadowElevation = N.dp` (Rule 9) must use Card / Surface or LocalTastileSurfaceElevationTokens.")
    appendLine("Hardcoded `Color(0xFF...)` (Rule 10) is allowed in core/designsystem/theme/Color.kt and for symbols")
    appendLine("carrying a reasoned path+symbol exemption in design_system_rule10_approved_colors.json.")
}

tasks.register("verifyNoEmbeddedServerSecrets") {
    group = "verification"
    description = "Reject server-only bridge credentials from Android sources and BuildConfig."
    doLast {
        val forbidden = listOf(
            "TASTILE_WEB_BRIDGE_" + "SECRET",
            "x-tastile-web-bridge-" + "secret",
        )
        val sources = fileTree("src/main") { include("**/*.kt", "**/*.java") }.files
        val buildScript = layout.projectDirectory.file("build.gradle.kts").asFile
        val offenders = (sources + buildScript).filter { file ->
            val content = file.readText()
            forbidden.any(content::contains)
        }
        check(offenders.isEmpty()) {
            "Server-only bridge credentials must not enter Android artifacts:\n" +
                offenders.joinToString(separator = "\n") { "- ${it.path}" }
        }
    }
}

tasks.register("verifySkillAdapterDrift") {
    group = "verification"
    description = "Detect drift between .claude/skills/ (Claude Code adapter stubs) " +
        "and .agents/skills/ (canonical Skills)."
    doLast {
        val script = rootProject.file("scripts/ci/sync-skill-adapters.sh")
        check(script.exists()) {
            "scripts/ci/sync-skill-adapters.sh not found at ${script.path}; " +
                "rebuild from git or restore from upstream."
        }
        // ProcessBuilder rather than providers.exec: the exit code of the
        // exec providers has moved between Gradle versions, and this gate must
        // not depend on which shape the current one exposes.
        val process = ProcessBuilder("bash", script.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitValue = process.waitFor()
        check(exitValue == 0) { "Skill adapter drift detected:\n${output}" }
        logger.lifecycle(output.trim())
    }
}

tasks.named("check").configure {
    dependsOn("verifyDesignSystemImports", "verifyNoEmbeddedServerSecrets", "verifySkillAdapterDrift")
}

// ---------------------------------------------------------------------------
// Phase 1 UI rebuild guards (Issue #11 = plan ticket #102)
//
// Rule 5 (`// m2-allow:` marker ratchet), Rule 8 (raw `.dp` debt ratchet),
// Rule 9 (`shadowElevation = N.dp`, zero tolerance) and Rule 12 (SourceTileRead
// wire-shape contract) all gate `:app:check`; Rule 8, 9 and 10 are enforced
// inside `verifyDesignSystemImports` and Rule 10 additionally allows reasoned
// path+symbol exemptions. Rule 11 (branch name `^\d+$`) gates `:app:check` too.
// Rules 4 and 6 ship as UAST lint detectors in `:lint-rules`; Rule 7 ships with
// the Phase 4 TileComposer work.
// ---------------------------------------------------------------------------

// Rule 5 freezes the existing `// m2-allow:` debt rather than allowing more of it.
// `m2AllowBaseline` is the count that actually exists in the tree today, so the
// limit is the current count: any new marker fails, and removing markers is the
// only way to create room. Lower the baseline in the same change that removes
// markers so the ceiling keeps falling. A raise needs an ADR, not a build edit.
val m2AllowBaseline = 610
val m2AllowLimit = m2AllowBaseline

tasks.register("verifyM2AllowBudget") {
    group = "verification"
    description = "Rule 5: `// m2-allow:` marker ratchet — fail if the count exceeds the " +
        "frozen baseline of $m2AllowBaseline."
    doLast {
        val allowMarker = "// m2-allow:"
        val allKt = fileTree("src/main") { include("**/*.kt") }.files
        val count = allKt.sumOf { file ->
            file.readText().lineSequence().count { it.contains(allowMarker) }
        }
        if (count > m2AllowLimit) {
            throw GradleException(
                "Rule 5: `// m2-allow:` marker count is $count, which exceeds the frozen " +
                    "baseline of $m2AllowBaseline. Existing debt may shrink but never grow; " +
                    "remove marker usage rather than raising the baseline. Raising it " +
                    "requires an ADR.",
            )
        }
        logger.lifecycle(
            "verifyM2AllowBudget: $count markers used, $m2AllowLimit allowed " +
                "(${m2AllowBaseline - count} below the frozen baseline)",
        )
    }
}

tasks.register("verifyBranchName") {
    group = "verification"
    description = "Rule 11: branch name must match `^\\d+$` (ADR-0007)."
    doLast {
        // GitHub Actions checks a `pull_request` out as a detached HEAD, so
        // `git rev-parse --abbrev-ref HEAD` answers `HEAD` and every correctly
        // named ticket branch failed the gate. Prefer the ref GitHub exports
        // and only fall back to git for a local invocation.
        val githubHeadRef = providers.environmentVariable("GITHUB_HEAD_REF").orNull?.trim()
        val githubRefName = providers.environmentVariable("GITHUB_REF_NAME").orNull?.trim()
        val branch = githubHeadRef?.takeIf { it.isNotEmpty() }
            ?: githubRefName?.takeIf { it.isNotEmpty() }
            ?: providers.exec {
                commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
            }.standardOutput.asText.get().trim()

        // The integration branches are not ticket branches, so the rule does
        // not apply to them.
        if (branch == "main" || branch.matches(Regex("""^release-[0-9]+-[0-9]+-[0-9]+$"""))) {
            logger.lifecycle("verifyBranchName: `$branch` is an integration branch — skipped")
            return@doLast
        }

        check(branch.matches(Regex("""^\d+$"""))) {
            "Rule 11: branch name `$branch` does not match `^\\d+$` (ADR-0007). " +
                "Rename the branch to its GitHub Issue number before opening a PR."
        }
        logger.lifecycle("verifyBranchName: branch `$branch` matches `^\\d+$` — OK")
    }
}

tasks.register("verifyWireShapeContract") {
    group = "verification"
    description = "Rule 12: SourceTileRead wire-shape contract gate — every fixture " +
        "under app/src/test/resources/wire_fixtures/source_tile/*.json must match the " +
        "canonical key set in app/src/main/assets/source_tile_canonical_keys.json."
    doLast {
        val canonicalFile = layout.projectDirectory
            .file("src/main/assets/source_tile_canonical_keys.json").asFile
        check(canonicalFile.exists()) {
            "Missing canonical keys file at ${canonicalFile.path}"
        }
        // The canonical file uses a small JSON schema (see app/src/main/assets/source_tile_canonical_keys.json):
        //   { "fields": [ { "wire": "...", ... } ] }
        // Parse it with a real JSON reader rather than a regex: `SourceTileRead`
        // is a nested payload, so a text scan cannot tell a top-level key from
        // one inside `schedule` and would flag every nested key as `extra`.
        val slurper = groovy.json.JsonSlurper()
        @Suppress("UNCHECKED_CAST")
        val canonicalJson = slurper.parse(canonicalFile) as Map<*, *>
        val canonicalFields = canonicalJson["fields"] as? List<*>
        checkNotNull(canonicalFields) { "`fields` missing from $canonicalFile" }
        val canonical = canonicalFields
            .filterIsInstance<Map<*, *>>()
            .mapNotNull { it["wire"] as? String }
            .toSet()
        check(canonical.isNotEmpty()) {
            "No `wire` keys found in $canonicalFile — is the schema valid?"
        }

        val fixturesDir = layout.projectDirectory
            .dir("src/test/resources/wire_fixtures/source_tile").asFile
        if (!fixturesDir.exists()) {
            logger.lifecycle("verifyWireShapeContract: no fixtures directory yet — OK")
            return@doLast
        }
        val fixtures = fixturesDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .toList()
        if (fixtures.isEmpty()) {
            logger.lifecycle("verifyWireShapeContract: no fixtures yet — OK")
            return@doLast
        }
        val violations = mutableListOf<String>()
        fixtures.forEach { fixture ->
            // Compare the **root** key set only. A regex over the raw text also
            // matched keys inside `schedule`, so a correct nested fixture was
            // rejected as `extra`.
            @Suppress("UNCHECKED_CAST")
            val parsed = slurper.parse(fixture) as Map<*, *>
            val keys = parsed.keys.map { it.toString() }.toSet()
            val missing = canonical - keys
            val extra = keys - canonical
            if (missing.isNotEmpty() || extra.isNotEmpty()) {
                violations += buildString {
                    append("${fixture.path}: missing=").append(missing)
                    append(", extra=").append(extra)
                }
            }
        }
        check(violations.isEmpty()) {
            "Rule 12: wire-shape contract drift:\n" +
                violations.joinToString("\n") { "  - $it" }
        }
        logger.lifecycle(
            "verifyWireShapeContract: ${fixtures.size} fixture(s), " +
                "${canonical.size} canonical key(s) — OK",
        )
    }
}

tasks.named("check").configure {
    dependsOn("verifyM2AllowBudget", "verifyBranchName", "verifyWireShapeContract")
    // `:lint-rules` unit tests were unreachable from `:app:check`, so they never
    // ran in CI. That is how nine detectors against a removed lint API sat in
    // the module un-compiled. Keep the detector tests inside the gate.
    dependsOn(":lint-rules:test")
}

// ---------------------------------------------------------------------------
// OpenAPI auto-generation pipeline (cross-repo canonical YAML -> Retrofit client)
// ---------------------------------------------------------------------------
//
// Source of truth: ../../openapi/openapi.yaml (the workspace-shell submodule at
// tastile-root/openapi/). That YAML is regenerated from tastile-core's
// `cargo run --bin dump_openapi` output and published to the submodule by the
// core sync script; consumers (web, android, desktop) read from the submodule
// path directly. The path is parameterised via the `openapi.input` Gradle
// property (see gradle.properties) so CI / local overrides can pin a different
// spec without editing this script.
//
// The generated Retrofit + Moshi client lives under app/build/generated/openapi/v1/
// (gitignored via the project-root `build/` rule) and is wired into the
// `main` Kotlin source set. Existing hand-rolled V1ApiClient stays as a facade
// so the 15+ `mockk<V1ApiClient>()` tests remain untouched.

val openapiInput: java.io.File =
    file(
        (project.findProperty("openapi.input") as? String)
            ?: error(
                "openapi.input is not set in gradle.properties; add " +
                    "`openapi.input=../../openapi/openapi.yaml` (the path is " +
                    "resolved relative to this module's projectDir, i.e. app/).",
            ),
    )

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateV1Api") {
    group = "openapi"
    description = "Generate the v1 Kotlin client from the cross-repo OpenAPI submodule"
    inputSpec.set(openapiInput.toURI().toString())
    outputDir.set(layout.buildDirectory.dir("generated/openapi/v1").get().asFile.absolutePath.replace('\\', '/'))
    generatorName.set("kotlin")
    library.set("jvm-retrofit2")
    apiNameSuffix.set("Api")
    modelNameSuffix.set("")
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "useCoroutines" to "true",
            "enumPropertyNaming" to "UPPERCASE",
            // Disable Moshi's @JsonClass(generateAdapter = true) emission so
            // the generated DTOs decode via the reflection-based
            // KotlinJsonAdapterFactory at runtime. Avoids the requirement
            // to wire moshi-kotlin-codegen (KSP) onto the generated source
            // directory and keeps the v1 client portable.
            "moshiCodeGen" to "false",
        )
    )
    packageName.set("app.tastile.android.data.api.generated.v1")
    skipValidateSpec.set(false)
}

android.sourceSets["main"].kotlin.srcDir(
    layout.buildDirectory.get().asFile.resolve("generated/openapi/v1/src/main/kotlin")
)

tasks.named("preBuild").configure { dependsOn("generateV1Api") }

// The Kotlin generator emits `@JsonClass(generateAdapter = true)` on every
// data class even when `moshiCodeGen=false` is set. To keep the generated
// DTOs decodable via `KotlinJsonAdapterFactory` (no moshi-codegen KSP on
// the generated source directory), strip the annotation and its import
// after each generation.
//
// Configuration-cache note: the directory is resolved at configuration time
// into a local val above the doLast. Reading `layout.buildDirectory.*` directly
// inside doLast makes the Kotlin compiler emit a non-static inner class that
// captures the build script receiver via a synthetic `$$script_receiver_1`
// field (a DefaultProject reference). Gradle's configuration cache rejects
// that with "cannot serialize object of type DefaultProject" when storing the
// task graph. The local-val form below ensures the doLast action only
// captures a serializable `java.io.File`, breaking the chain to the script
// receiver. See
// https://docs.gradle.org/9.7.1/userguide/configuration_cache_requirements.html#config_cache:requirements:disallowed_types
tasks.named("generateV1Api").configure {
    val generatedModelsDir: File =
        layout.buildDirectory.get().asFile.resolve("generated/openapi/v1/src/main/kotlin")
    doLast {
        generatedModelsDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val original = file.readText()
                val stripped = original
                    .replace(Regex("@JsonClass\\(generateAdapter = true\\)\\s*\n"), "")
                    .replace(Regex("@JsonClass\\(generateAdapter = false\\)\\s*\n"), "")
                if (stripped != original) {
                    file.writeText(stripped)
                }
            }
    }
}

// ---------------------------------------------------------------------------
// Drift guard: verify that every operation in the canonical OpenAPI submodule
// has a corresponding method in the generated v1 client. Catches the case
// where the spec gains a new path but the generated sources are stale (e.g.,
// developer bumped the submodule pointer but forgot to re-run
// `./gradlew :app:generateV1Api`).
//
// The spec is YAML (OpenAPI 3.1). The same operationId regex works because
// YAML and JSON share the `operationId: "<id>"` textual form.
// ---------------------------------------------------------------------------

tasks.register("verifyV1ApiCoverage") {
    group = "verification"
    description = "Assert every operationId in the OpenAPI submodule has a generated method"
    dependsOn("generateV1Api")
    doLast {
        val specFile = openapiInput
        check(specFile.exists()) {
            "Missing OpenAPI spec at $specFile — set openapi.input in gradle.properties " +
                "and run `git submodule update --init` at the workspace root."
        }

        val specText = specFile.readText()
        // operationId keys inside paths.*.* blocks. The regex must accept both
        // the JSON form (`"operationId": "name"`) and the YAML form
        // (`operationId: name`, unquoted, as emitted by `serde_yaml`). The
        // OpenAPI spec is machine-generated and well-formed, so a single
        // anchored pattern is enough. Use a negative lookbehind to skip
        // accidental matches of substrings like `myOperationId`.
        val operationIdRegex =
            Regex("""(?:"operationId"|(?<![A-Za-z0-9_])operationId)\s*:\s*"?([A-Za-z_][\w]*)"?""")
        val operationIds = operationIdRegex.findAll(specText).map { it.groupValues[1] }.toList()
        check(operationIds.isNotEmpty()) { "No operationIds found in $specFile — is the spec valid?" }

        val generatedApisDir = layout.buildDirectory
            .get()
            .asFile
            .resolve("generated/openapi/v1/src/main/kotlin/app/tastile/android/data/api/generated/v1/apis")
        check(generatedApisDir.exists()) {
            "Generated apis dir not found at $generatedApisDir — run :app:generateV1Api first"
        }
        val generatedMethodRegex = Regex("""suspend\s+fun\s+(\w+)\s*\(""")
        val generatedMethodNames = generatedApisDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { generatedMethodRegex.findAll(it.readText()).map { m -> m.groupValues[1] } }
            .toSet()

        val missing = operationIds
            .map { snakeToCamel(it) }
            .filter { it !in generatedMethodNames }
        check(missing.isEmpty()) {
            "OpenAPI spec lists operationIds that have no generated method:\n" +
                missing.joinToString("\n") { "  - $it" } +
                "\n\nRe-run `./gradlew :app:generateV1Api` to refresh the client, " +
                "or add a delegation method to V1GeneratedApiClient."
        }

        logger.lifecycle(
            "verifyV1ApiCoverage: ${operationIds.size} operations, " +
                "${generatedMethodNames.size} generated methods — OK"
        )
    }
}

fun snakeToCamel(snake: String): String =
    snake.split("_").mapIndexed { idx, part ->
        if (idx == 0) part
        else part.replaceFirstChar { ch -> ch.uppercaseChar() }
    }.joinToString("")

// ---------------------------------------------------------------------------
// JaCoCo coverage for JVM unit tests.
//
// Gradle 9.x's JaCoCo plugin splits the legacy single task into two:
//   - `JacocoReport`               — generates HTML + XML reports
//   - `JacocoCoverageVerification` — enforces violation rules
// Both extend `JacocoReportBase` and share `executionData` / `classDirectories` /
// `sourceDirectories` configuration.
//
// Hooks into `:app:check` so `./gradlew verify` (default pre-push gate per
// AGENTS.md) enforces the 80% threshold policy. Reports land at
// app/build/reports/jacoco/testDebugUnitTestCoverageReport/{html,xml}/.
//
// Threshold policy (mirrors Vitest 80% rule in project agent policy):
//   - INSTRUCTION  >= 0.80  (proxy for "statements covered")
//   - BRANCH       >= 0.80
//   - LINE         >= 0.80
//   - METHOD       >= 0.80  (proxy for "functions covered")
//
// Excluded from `classDirectories`:
//   - BuildConfig / BuildConfig$*:  generated by AGP from gradle.properties
//   - R / R$*:                       generated resource IDs
//   - Manifest*:                     generated manifest wrappers
// These are AGP-generated, not meaningful to unit-test, and excluding them
// is consistent with meta-prompt §28 ("narrow exclusion...generated/vendor
// code"). Generated OpenAPI DTOs under app/build/generated/openapi/v1/ live
// outside the classDirectories include paths and are therefore also
// excluded automatically.
//
// On first `./gradlew verify` after this lands, run
// `./gradlew :app:testDebugUnitTestCoverageVerification --info` and inspect
// app/build/reports/jacoco/testDebugUnitTestCoverageReport/html/index.html
// to see the actual gap. The threshold is not silently lowered to make the
// build pass; bring coverage to >= 80% by adding tests, or document a
// per-class removal in this task with a hard BLOCKED rationale (meta-prompt
// §29).
// ---------------------------------------------------------------------------

val coverageClassDirs = fileTree(layout.buildDirectory) {
    include("intermediates/javac/debug/classes/**")
    include("tmp/kotlin-classes/debug/**")
    exclude("**/BuildConfig.class")
    exclude("**/BuildConfig\$*.class")
    exclude("**/R.class")
    exclude("**/R\$*.class")
    exclude("**/Manifest.class")
    exclude("**/Manifest\$*.class")
}

val coverageExecData = fileTree(layout.buildDirectory) {
    include("jacoco/testDebugUnitTest.exec")
}

tasks.register<org.gradle.testing.jacoco.tasks.JacocoReport>("testDebugUnitTestCoverageReport") {
    group = "verification"
    description = "Generate JaCoCo HTML + XML coverage report for JVM unit tests."
    dependsOn("testDebugUnitTest")

    executionData.setFrom(coverageExecData)
    classDirectories.setFrom(coverageClassDirs)
    sourceDirectories.setFrom(files("src/main/java"))

    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.register<org.gradle.testing.jacoco.tasks.JacocoCoverageVerification>("testDebugUnitTestCoverageVerification") {
    group = "verification"
    description = "Verify JVM unit test coverage meets 80% threshold (lines/branches/methods/instructions)."
    dependsOn("testDebugUnitTest")

    executionData.setFrom(coverageExecData)
    classDirectories.setFrom(coverageClassDirs)
    sourceDirectories.setFrom(files("src/main/java"))

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                // JaCoCo's `Limit.value` is an enum (CounterValue). The previous
                // String `"coveredratio"` (lowercase) triggered `No enum constant
                // ICounter.CounterValue.coveredratio` — the canonical enum
                // constant is `COVEREDRATIO`. Pass the uppercase form.
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            element = "BUNDLE"
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            element = "BUNDLE"
            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.named("check").configure {
    dependsOn(
        "verifyV1ApiCoverage",
        "testDebugUnitTestCoverageReport",
        "testDebugUnitTestCoverageVerification",
    )
}

val roomVersion = "2.8.5"

dependencies {
    // appcompat 1.6.1+ required for AppCompatDelegate.setApplicationLocales
    // compat shim (the runtime-locale-switch path called by
    // DashboardViewModel.setLocale). 1.8.0 covers the
    // `LocaleListCompat.forLanguageTags` API on minSdk=26+ devices.
    implementation("androidx.appcompat:appcompat:1.8.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-alpha27")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigation:navigation-compose:2.10.0")

    implementation("io.ktor:ktor-client-okhttp:3.5.2")

    // OpenAPI auto-generation pipeline (see `generateV1Api` task above).
    // The generator emits a Retrofit interface + Moshi-backed DTOs, plus an
    // `infrastructure/ApiClient.kt` that imports
    // `retrofit2.converter.scalars.ScalarsConverterFactory` to serialize
    // `String`/`Int`/`Boolean` path / query params that aren't declared via
    // `@Query` annotations. Pin the same 2.12.0 line as the core Retrofit.
    implementation("com.squareup.retrofit2:retrofit:2.12.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.12.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("com.squareup.moshi:moshi-adapters:1.15.2")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Persistent timeline read model
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.5.1")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Date/Time
    // Pinned at 0.6.1: 0.8.0 promoted `kotlinx.datetime.Instant` arithmetic APIs to
    // `@ExperimentalTime`, which breaks `ExecutionAlarmPlanner` and
    // `ExecutionStateProjector`. Track the opt-in migration in
    // docs/plans/2026-07-23-datetime-08-optin.md before bumping.
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Pin okhttp to 4.12.0. mockwebserver 4.12.0 references `okhttp3.internal.Util`
    // (a class relocated in 5.x); if anything else (ktor-okhttp's flexible
    // range, ksp-android) upgrades okhttp to 5.x, MockWebServer's constructor
    // throws NoClassDefFoundError at runtime. The ktor-okhttp engine and the
    // generated v1 client both target okhttp 4.x APIs anyway.
    configurations.all {
        resolutionStrategy {
            force("com.squareup.okhttp3:okhttp:4.12.0")
            force("com.squareup.okhttp3:mockwebserver:4.12.0")
        }
    }

    // Hilt
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.security:security-crypto:1.1.0")

    // Credential Manager / Google Identity
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // R17: instrumented UI navigation tests (audit 2026-07-16).
    // Hilt-testing lives in androidTest only so the unit-test source set stays
    // Robolectric-only and avoids dragging the Hilt test-application into the
    // `test` classpath (which would conflict with @HiltAndroidTest subclasses
    // that try to use HiltTestApplication).
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    // QuickCreateGestureCanaryTest: true platform pointer input (UiDevice.click
    // at display coordinates) to prove dragHandle-vs-Button gesture arbitration
    // that Compose semantics performClick() bypasses by design.
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("io.mockk:mockk-android:1.14.11")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.60.1")
    // Hilt test codegen for the androidTest source set (generates
    // Hilt_HiltTestActivity for QuickCreateGestureCanaryTest's host activity
    // and wires @TestInstallIn modules). Without this, @AndroidEntryPoint
    // classes in androidTest fail at runtime with ClassNotFoundException.
    kspAndroidTest("com.google.dagger:hilt-compiler:2.60.1")
    // HiltTestActivity lives in src/debug: its @AndroidEntryPoint wrapper is
    // generated when compiling the debug variant.
    kspDebug("com.google.dagger:hilt-compiler:2.60.1")
    androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")

    // Custom lint rules (M2-T4): WrapperParameterOrderDetector (L0 C1 + C2).
    lintChecks(dependencyFactory.createProjectDependency(":lint-rules"))
}

// A failing unit test must report the frames that threw. The default short
// format collapsed a message-less AssertionError to a single line and hid the
// throw site, which is what made Issue #54 expensive to diagnose.
tasks.withType<Test>().configureEach {
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
    }
}

// R18 (android refactor 2026-07-22): fail-fast guard.
// Every BuildConfig.* field that ships into runtime (web base URL,
// TASTILE_CORE_URL, Google web client ID) MUST be supplied by
// Infisical environment variables — empty strings cause silent auth breakage
// on a release build. Run local builds through `infisical run`; the release
// workflow authenticates to Infisical with GitHub OIDC.
gradle.projectsEvaluated {
    val requiredProps = listOf(
        "GOOGLE_WEB_CLIENT_ID",
        "GOOGLE_ANDROID_CLIENT_ID",
        "WEB_BASE_URL",
        "TASTILE_CORE_URL",
    )
    requiredProps.forEach { name ->
        val value = configuredValue(name).orNull
        if (value.isNullOrBlank()) {
            throw GradleException(
                "Missing required value '$name'. Authenticate with Infisical and run the build " +
                    "through `infisical run`. See CONTRIBUTING.md for the contract."
            )
        }
    }
}
