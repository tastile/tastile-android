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

val releaseStoreFile = providers.gradleProperty("RELEASE_STORE_FILE")
val releaseStorePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD")
val googleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID")
val webBaseUrl = providers.gradleProperty("WEB_BASE_URL")
val tastileCoreUrl = providers.gradleProperty("TASTILE_CORE_URL")
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
        versionName = "0.4.0"

        // R17 (android-archdoc audit 2026-07-16): instrumented UI navigation tests.
        // The runner swaps the production Application for Hilt's HiltTestApplication
        // so per-test Hilt @TestInstallIn modules can swap repositories.
        testInstrumentationRunner = "app.tastile.android.util.TastileTestRunner"

        // R18 (android refactor 2026-07-22): no Kotlin-level fallback defaults.
        // All production values must come from gradle.properties (committed
        // blank for CI override) or `~/.gradle/gradle.properties` for local dev.
        // Empty strings are validated at the bottom of this file via the
        // requireGradleProperty guard so a partial config fails the build fast
        // instead of silently embedding the wrong environment.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleWebClientId.orNull ?: ""}\"")
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
Add RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, and RELEASE_KEY_PASSWORD
to your user-level ~/.gradle/gradle.properties or pass them as -P properties when running release tasks.
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
    description = "Disallow direct Material3 imports and colorScheme references in M3-unified screens; forbid hardcoded RoundedCornerShape(N.dp) outside design-system"
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
        )
        check(violations.isEmpty()) { formatDesignSystemViolations(violations) }
    }
}

/**
 * Collect every guard violation across the three rules:
 *  - Rule 1: forbidden Material3 imports (uses [designSystemGuardFiles] + the `// m2-allow:` marker)
 *  - Rule 2: `MaterialTheme.colorScheme` references in [uiConsumerRoots] without `// m2-allow:` marker
 *  - Rule 3: hardcoded `RoundedCornerShape(<non-zero-numeric>.dp)` outside [designSystemRoot]
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

tasks.named("check").configure {
    dependsOn("verifyDesignSystemImports", "verifyNoEmbeddedServerSecrets")
}

// ---------------------------------------------------------------------------
// OpenAPI auto-generation pipeline (tastile-core utoipa doc -> Retrofit client)
// ---------------------------------------------------------------------------
//
// Source of truth: app/openapi/v1.json (committed, refreshed by
// `cd ../tastile-core/crates-v1 && cargo run -p api --bin dump_openapi`).
// The generated Retrofit + Moshi client lives under app/build/generated/openapi/v1/
// (gitignored via the project-root `build/` rule) and is wired into the
// `main` Kotlin source set. Existing hand-rolled V1ApiClient stays as a facade
// so the 15+ `mockk<V1ApiClient>()` tests remain untouched.

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateV1Api") {
    group = "openapi"
    description = "Generate the v1 Kotlin client from app/openapi/v1.json"
    inputSpec.set(file("openapi/v1.json").toURI().toString())
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
tasks.named("generateV1Api").configure {
    doLast {
        val generatedModelsDir =
            layout.buildDirectory.get().asFile.resolve("generated/openapi/v1/src/main/kotlin")
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
// Drift guard: verify that every operation in app/openapi/v1.json has a
// corresponding method in the generated v1 client. Catches the case where
// the openapi.json gains a new path but the generated sources are stale
// (e.g., developer forgot to re-run `./gradlew :app:generateV1Api`).
// ---------------------------------------------------------------------------

tasks.register("verifyV1ApiCoverage") {
    group = "verification"
    description = "Assert every operationId in app/openapi/v1.json has a generated method"
    dependsOn("generateV1Api")
    doLast {
        val specFile = file("openapi/v1.json")
        check(specFile.exists()) { "Missing openapi spec at $specFile" }

        val specText = specFile.readText()
        // operationId: "..." inside paths.*.* blocks. A simple regex is enough
        // because the openapi.json is machine-generated and well-formed.
        val operationIdRegex = Regex("\"operationId\"\\s*:\\s*\"([^\"]+)\"")
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
            "openapi.json lists operationIds that have no generated method:\n" +
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
                value = "coveredratio"
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            element = "BUNDLE"
            limit {
                counter = "BRANCH"
                value = "coveredratio"
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "coveredratio"
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            element = "BUNDLE"
            limit {
                counter = "METHOD"
                value = "coveredratio"
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

dependencies {
    // appcompat 1.6.1+ required for AppCompatDelegate.setApplicationLocales
    // compat shim (the runtime-locale-switch path called by
    // DashboardViewModel.setLocale). 1.6.1 covers the
    // `LocaleListCompat.forLanguageTags` API on minSdk=26+ devices.
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-alpha24")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")

    implementation("io.ktor:ktor-client-okhttp:3.5.1")

    // OpenAPI auto-generation pipeline (see `generateV1Api` task above).
    // The generator emits a Retrofit interface + Moshi-backed DTOs, plus an
    // `infrastructure/ApiClient.kt` that imports
    // `retrofit2.converter.scalars.ScalarsConverterFactory` to serialize
    // `String`/`Int`/`Boolean` path / query params that aren't declared via
    // `@Query` annotations. Pin the same 2.11.0 line as the core Retrofit.
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.moshi:moshi-adapters:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

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
    // kotlinx-coroutines-test pinned at 1.9.0 to match the runtime
    // kotlinx-coroutines version pulled in transitively by the Hilt+KSP
    // toolchain; bumping to 1.11.0 surfaces a `kotlin.time.ExperimentalTime`
    // opt-in requirement in test dispatchers. Track opt-in migration in
    // docs/plans/2026-07-23-coroutines-1-11-migration.md.
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("org.robolectric:robolectric:4.16.1")
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
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("io.mockk:mockk-android:1.14.11")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.60.1")
    androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")

    // Custom lint rules (M2-T4): WrapperParameterOrderDetector (L0 C1 + C2).
    lintChecks(dependencyFactory.createProjectDependency(":lint-rules"))
}

// R18 (android refactor 2026-07-22): fail-fast guard.
// Every BuildConfig.* field that ships into runtime (web base URL,
// TASTILE_CORE_URL, Google web client ID) MUST be supplied by
// gradle.properties — empty strings cause silent auth breakage on a release build.
// Set them in:
//   - gradle.properties (CI / shared values), or
//   - ~/.gradle/gradle.properties (local-dev override), or
//   - -PKEY=value on the gradle command line.
gradle.projectsEvaluated {
    val requiredProps = listOf(
        "GOOGLE_WEB_CLIENT_ID",
        "WEB_BASE_URL",
        "TASTILE_CORE_URL",
    )
    requiredProps.forEach { name ->
        val value = providers.gradleProperty(name).orNull
        if (value.isNullOrBlank()) {
            throw GradleException(
                "Missing required gradle property '$name'. Set it in gradle.properties " +
                    "(or ~/.gradle/gradle.properties for local dev, or pass -P$name=… on " +
                    "the gradle command line). See README for the contract."
            )
        }
    }
}
