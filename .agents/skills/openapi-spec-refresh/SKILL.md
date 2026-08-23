---
name: openapi-spec-refresh
description: Use when `app/openapi/v1.json` is changed (by hand or by the tastile-core dump script) and the generated Retrofit client must be refreshed and the v1 coverage guard re-run.
---

`app/openapi/v1.json` is the committed utoipa export from the Rust `tastile-core` server. It is the source of truth for the Android `V1ApiClient` Retrofit interface and the generated request / response DTOs. Whenever the spec changes, three things must happen in this order:

1. Regenerate the Kotlin client:

   ```
   ./gradlew :app:generateV1Api
   ```

   The task wires the generated sources into the `main` Kotlin source set via `android.sourceSets["main"].kotlin.srcDir(...)` (app/build.gradle.kts:263-265) and applies a post-processor that strips the `@JsonClass(generateAdapter = ...)` annotations from each generated DTO so they decode through `KotlinJsonAdapterFactory` instead of requiring `moshi-kotlin-codegen` (line 274-289). Re-run if the spec is touched even slightly: the KSP source set compatibility is strict and a stale generation silently passes type-checks against an outdated DTO.

2. Run the coverage guard:

   ```
   ./gradlew :app:verifyV1ApiCoverage
   ```

   The guard enumerates every `operationId` in `app/openapi/v1.json` and confirms the generated client has a `suspend fun <operationId-as-camelCase>(...)` method (line 308-349). It also asserts the generated `apis/` directory exists. A missing method is the most common failure mode after a spec edit that renamed an operation without regenerating; never hand-edit the generated sources to fix a coverage miss, regenerate instead.

3. Run the project verification suite so the guard and the regeneration both run in the same configuration:

   ```
   ./gradlew :app:check
   ```

   `:app:check` depends on `verifyV1ApiCoverage` (line 351) and the design-system / server-secret guards. If `check` is green, the spec and the client are in sync.

Cross-reference: the dump script that produces the committed spec lives in `../tastile-core` at `crates-v1/src/bin/dump_openapi.rs` (per the comment on line 226-227 of `app/build.gradle.kts`). Run that script, copy the new JSON over `app/openapi/v1.json`, then run the three steps above. Do not edit `app/openapi/v1.json` by hand unless the spec is intentionally a stand-in for an unmerged backend change.

Validation contract for the change: `./gradlew :app:testDebugUnitTest` must still pass after the regeneration. The V1 client DTOs are used by unit tests under `app/src/test/java/app/tastile/android/data/api/`. A regeneration that changes a DTO shape will surface as a test compile error in those tests.
