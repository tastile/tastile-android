import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")
    // AGP 9.4.0 ships with lint 32.4.0; align here so the detector classpath matches
    // the runtime that runs `:app:lint`.
    compileOnly("com.android.tools.lint:lint-api:32.4.0")
    compileOnly("com.android.tools.lint:lint-checks:32.4.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.android.tools.lint:lint:32.4.0")
    testImplementation("com.android.tools.lint:lint-tests:32.4.0")
}
