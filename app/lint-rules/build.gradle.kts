plugins {
    `java-library`
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")
    // AGP 9.2.1 ships with lint 32.2.1; align here so the detector classpath matches
    // the runtime that runs `:app:lint`.
    compileOnly("com.android.tools.lint:lint-api:32.2.1")
    compileOnly("com.android.tools.lint:lint-checks:32.2.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.android.tools.lint:lint:32.2.1")
    testImplementation("com.android.tools.lint:lint-tests:32.2.1")
}
