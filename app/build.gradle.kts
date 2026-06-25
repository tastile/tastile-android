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
    compileSdk = 35
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
        versionCode = 22
        versionName = "0.2.13"

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleWebClientId.orNull ?: ""}\"")
        buildConfigField("String", "COGNITO_CLIENT_ID", "\"${cognitoClientId.orNull ?: "2b9fkkb4u5di8veelnmjkmnldj"}\"")
        buildConfigField("String", "COGNITO_REGION", "\"${cognitoRegion.orNull ?: "ap-northeast-1"}\"")
        buildConfigField("String", "COGNITO_HOSTED_UI_DOMAIN", "\"${cognitoHostedUiDomain.orNull ?: "tastile-beta"}\"")
        buildConfigField("String", "COGNITO_REDIRECT_URI", "\"${cognitoRedirectUri.orNull ?: "tastile://auth/callback"}\"")
        buildConfigField("String", "COGNITO_WEB_AUTH_BASE_URL", "\"${cognitoWebAuthBaseUrl.orNull ?: "https://app.tastile.app"}\"")
        buildConfigField("String", "TASTILE_CORE_URL", "\"${tastileCoreUrl.orNull ?: "https://api.tastile.app"}\"")
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
    lint {
        // OldTargetApi is suppressed deliberately: this box only has API 35 and 37 installed
        // (commit a2c508c). Bumping targetSdk to 36 is blocked until the missing SDK is
        // restored on the build host.
        disable += setOf(
            "GradleDependency",
            "ObsoleteLintCustomCheck",
            "IconDuplicates",
            "IconLauncherShape",
            "OldTargetApi"
        )
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
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

val designSystemGuardFiles = listOf(
    "app/src/main/java/app/tastile/android/ui/dashboard/ManagementScreens.kt"
)

tasks.register("verifyDesignSystemImports") {
    group = "verification"
    description = "Disallow direct Material3 imports in M3-unified screens"
    doLast {
        val forbidden = "import androidx.compose.material3."
        val offenders = designSystemGuardFiles
            .map { project.file(it) }
            .filter { it.exists() && it.readText().contains(forbidden) }
        check(offenders.isEmpty()) {
            "Direct Material3 imports are not allowed in guarded screens:\n" +
                offenders.joinToString(separator = "\n") { "- ${it.path}" }
        }
    }
}

tasks.named("check").configure {
    dependsOn("verifyDesignSystemImports")
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("io.ktor:ktor-client-okhttp:3.1.3")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Credential Manager / Google Identity
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.robolectric:robolectric:4.14")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
