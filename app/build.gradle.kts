plugins {
    id("com.android.application")
    // id("org.jetbrains.kotlin.android")  // auto-applied by AGP 9.x
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
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
        versionCode = 31
        versionName = "0.3.0"

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

dependencies {
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

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Date/Time
    // Pinned at 0.6.1: 0.8.0 promoted `kotlinx.datetime.Instant` arithmetic APIs to
    // `@ExperimentalTime`, which breaks `ExecutionAlarmPlanner` and
    // `ExecutionStateProjector`. Track the opt-in migration in
    // docs/plans/2026-07-23-datetime-08-optin.md before bumping.
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

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
