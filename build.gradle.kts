plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    // Rust NDK plugin for building core as Android native library
    id("com.github.willir.rust.cargo-ndk-android") version "0.3.4" apply false
}

tasks.register("verify") {
    group = "verification"
    description = "Runs the repository verification suite."
    dependsOn(":app:testDebugUnitTest")
}
