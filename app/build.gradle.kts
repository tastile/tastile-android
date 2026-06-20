plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

val releaseStoreFile = providers.gradleProperty("RELEASE_STORE_FILE")
val releaseStorePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD")
val supabaseUrl = providers.gradleProperty("SUPABASE_URL")
val supabaseAnonKey = providers.gradleProperty("SUPABASE_ANON_KEY")
val googleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID")
val cognitoClientId = providers.gradleProperty("COGNITO_CLIENT_ID")
val cognitoRegion = providers.gradleProperty("COGNITO_REGION")
val cognitoHostedUiDomain = providers.gradleProperty("COGNITO_HOSTED_UI_DOMAIN")
val cognitoRedirectUri = providers.gradleProperty("COGNITO_REDIRECT_URI")
val cognitoWebAuthBaseUrl = providers.gradleProperty("COGNITO_WEB_AUTH_BASE_URL")
val hasReleaseSigning =
    releaseStoreFile.isPresent &&
        releaseStorePassword.isPresent &&
        releaseKeyAlias.isPresent &&
        releaseKeyPassword.isPresent
val hasSupabaseConfig = supabaseUrl.isPresent && supabaseAnonKey.isPresent

android {
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
        versionCode = 20
        versionName = "0.2.11"

        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl.orNull ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseAnonKey.orNull ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleWebClientId.orNull ?: ""}\"")
        buildConfigField("String", "COGNITO_CLIENT_ID", "\"${cognitoClientId.orNull ?: "2b9fkkb4u5di8veelnmjkmnldj"}\"")
        buildConfigField("String", "COGNITO_REGION", "\"${cognitoRegion.orNull ?: "ap-northeast-1"}\"")
        buildConfigField("String", "COGNITO_HOSTED_UI_DOMAIN", "\"${cognitoHostedUiDomain.orNull ?: "tastile-beta"}\"")
        buildConfigField("String", "COGNITO_REDIRECT_URI", "\"${cognitoRedirectUri.orNull ?: "tastile://auth/callback"}\"")
        buildConfigField("String", "COGNITO_WEB_AUTH_BASE_URL", "\"${cognitoWebAuthBaseUrl.orNull ?: "https://app.tastile.app"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        disable += setOf(
            "GradleDependency",
            "ObsoleteLintCustomCheck",
            "IconDuplicates",
            "IconLauncherShape"
        )
    }
}

val releaseSigningInstructions = """
Release signing is not configured.
Add RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, and RELEASE_KEY_PASSWORD
to your user-level ~/.gradle/gradle.properties or pass them as -P properties when running release tasks.
""".trimIndent()

val missingSupabaseInstructions = """
Supabase client configuration is missing.
Define SUPABASE_URL and SUPABASE_ANON_KEY in gradle.properties or ~/.gradle/gradle.properties before installing debug builds on a device.
""".trimIndent()

gradle.taskGraph.whenReady {
    val requestedReleaseBuild =
        allTasks.any { task ->
            task.project == project && (task.name == "assembleRelease" || task.name == "bundleRelease")
        }
    val requestedDeviceInstall =
        allTasks.any { task ->
            task.project == project && (task.name == "installDebug" || task.name.startsWith("connected"))
        }
    if (requestedReleaseBuild && !hasReleaseSigning) {
        throw GradleException(releaseSigningInstructions)
    }
    if (requestedDeviceInstall && !hasSupabaseConfig) {
        throw GradleException(missingSupabaseInstructions)
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

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.3"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:compose-auth")
    implementation("io.ktor:ktor-client-okhttp:3.1.3")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.56.2")
    ksp("com.google.dagger:hilt-compiler:2.56.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Credential Manager / Google Identity
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.12")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
