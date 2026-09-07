plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
    id("org.openapi.generator") version "7.25.0" apply false
}

tasks.register("verify") {
    group = "verification"
    description = "Runs the repository verification suite."
    dependsOn(":app:check")
}
