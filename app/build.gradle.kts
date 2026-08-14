plugins {
    id("com.android.application")
    // id("org.jetbrains.kotlin.android")  // auto-applied by AGP 9.x
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.openapi.generator")
}

val releaseStoreFile = providers.gradleProperty("RELEASE_STORE_FILE")
val releaseStorePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD")
val googleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID")
val cognitoClientId = providers.gradleProperty("COGNITO_CLIENT_ID")
val cognitoRegion = providers.gradleProperty("COGNITO_REGION")
val cognitoHostedUiDomain = providers.gradleProperty("COGNITO_HOSTED_UI_DOMAIN")
val cognitoRedirectUri = providers.gradleProperty("COGNITO_REDIRECT_URI")
val cognitoWebAuthBaseUrl = providers.gradleProperty("COGNITO_WEB_AUTH_BASE_URL")
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
        buildConfigField("String", "COGNITO_CLIENT_ID", "\"${cognitoClientId.orNull ?: ""}\"")
        buildConfigField("String", "COGNITO_REGION", "\"${cognitoRegion.orNull ?: ""}\"")
        buildConfigField("String", "COGNITO_HOSTED_UI_DOMAIN", "\"${cognitoHostedUiDomain.orNull ?: ""}\"")
        buildConfigField("String", "COGNITO_REDIRECT_URI", "\"${cognitoRedirectUri.orNull ?: ""}\"")
        buildConfigField("String", "COGNITO_WEB_AUTH_BASE_URL", "\"${cognitoWebAuthBaseUrl.orNull ?: ""}\"")
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
    description = "Disallow direct Material3 imports in M3-unified screens"
    doLast {
        val forbiddenPrefix = "import androidx.compose.material3."
        val allowMarker = "// m2-allow:"
        val offenders = designSystemGuardFiles.filter { f ->
            if (!f.exists()) return@filter false
            val lines = f.readText().lines()
            // A file is an offender only when it contains a forbidden import
            // whose immediately preceding non-blank line is NOT an m2-allow marker.
            lines.withIndex().any { (idx, rawLine) ->
                val trimmed = rawLine.trimStart()
                if (!trimmed.startsWith(forbiddenPrefix)) return@any false
                var i = idx - 1
                while (i >= 0) {
                    val prev = lines[i].trim()
                    if (prev.isEmpty()) { i--; continue }
                    return@any !prev.startsWith(allowMarker)
                }
                true
            }
        }
        check(offenders.isEmpty()) {
            "Direct Material3 imports are not allowed in guarded screens:\n" +
                offenders.joinToString(separator = "\n") { "- ${it.path}" }
        }
    }
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

tasks.named("check").configure { dependsOn("verifyV1ApiCoverage") }

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
// Every BuildConfig.* field that ships into runtime (Cognito client/region/hosted-ui,
// web-auth base, TASTILE_CORE_URL, Google web client ID) MUST be supplied by
// gradle.properties — empty strings cause silent auth breakage on a release build.
// Set them in:
//   - gradle.properties (CI / shared values), or
//   - ~/.gradle/gradle.properties (local-dev override), or
//   - -PKEY=value on the gradle command line.
gradle.projectsEvaluated {
    val requiredProps = listOf(
        "GOOGLE_WEB_CLIENT_ID",
        "COGNITO_CLIENT_ID",
        "COGNITO_REGION",
        "COGNITO_HOSTED_UI_DOMAIN",
        "COGNITO_REDIRECT_URI",
        "COGNITO_WEB_AUTH_BASE_URL",
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
