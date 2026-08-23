---
name: buildconfig-guard-check
description: Use when adding a new `buildConfigField(...)` to `app/build.gradle.kts` or when reviewing such a change before commit.
---

`app/build.gradle.kts` declares Cognito / API-URL / Google-Web-Client-Id values as `buildConfigField` strings on line 64-70. They are read at runtime as `BuildConfig.GOOGLE_WEB_CLIENT_ID`, `BuildConfig.COGNITO_CLIENT_ID`, etc. A separate `gradle.projectsEvaluated` block at line 472-491 enforces that every one of these fields is supplied by `gradle.properties` / `~/.gradle/gradle.properties` / `-PKEY=value` at configuration time; a blank value throws `GradleException` and fails the build fast.

The reason for the guard: an empty BuildConfig string is silent at compile time, then breaks the corresponding auth / API call at runtime with a vague failure ("user not authenticated" / "404 from API"). Failing at build time gives the developer an actionable error pointing at the missing gradle property.

Use this Skill BEFORE adding a new buildConfigField. The flow:

1. Decide whether the new value belongs in BuildConfig at all. BuildConfig is for values that:
   - Are referenced from Kotlin source as `BuildConfig.<NAME>`.
   - Vary between local dev / staging / production environments.
   - Must NOT be in `BuildConfig.DEFAULT_VALUE` (the gradle.properties
     fallback at line 41-46), since defaults defeat the fail-fast
     guard.

2. After deciding the field belongs in BuildConfig, add it to the
   list of required properties inside the `gradle.projectsEvaluated`
   block (app/build.gradle.kts:482-490). The order in the list does
   not matter, but the entry must include the property name in
   `requiredProps` so the guard throws when blank.

3. Add the corresponding `buildConfigField(...)` to the
   `defaultConfig` block (line 64-70). Use the form:

   ```kotlin
   buildConfigField("String", "<NAME>", "\"${<gradle-property>.orNull ?: ""}\"")
   ```

   The `?: ""` keeps the same `""`-on-missing default as the existing
   fields, so the build still compiles when the property is blank;
   the guard then fails the configuration phase.

4. Add an empty-comment reminder line to `gradle.properties`
   (line 33-39) so the next reader knows the property is required
   and where to put a real value. The `verify-tastile-change` Skill
   in the workspace also checks this contract; preserving the
   comment style is part of the wire contract.

5. Validate:

   ```
   ./gradlew :app:assembleDebug -P<NAME>=verify-value
   ```

   should succeed, and:

   ```
   ./gradlew :app:assembleDebug
   ```

   without the property should fail with the message from
   `gradle.projectsEvaluated` quoting the missing property name.

6. Update AGENTS.md "Build-Time Hard Requirements" with the new
   property name so the documentation stays in sync with the guard.

The guard fires for both `:app:assembleRelease` and `:app:bundleRelease`
even with the new property present, because the same `gradle.projectsEvaluated`
block runs at configuration time regardless of which task triggered
the build. So the new property is enforced everywhere without a
per-task wiring.
