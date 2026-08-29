---
name: openapi-spec-refresh
description: Use when the cross-repo OpenAPI submodule at `../../openapi/openapi.yaml` (the canonical Tastile v1 spec) is bumped and the Android `V1ApiClient` Retrofit client must be regenerated and the v1 coverage guard re-run.
---

The canonical OpenAPI 3.1 spec lives at the workspace-shell submodule
`../../openapi/openapi.yaml` (resolved by `openapi.input` in
`gradle.properties`). That YAML is regenerated from the Rust
`tastile-core` server's utoipa export and published to the submodule by
the core sync script. It is the source of truth for the Android
`V1ApiClient` Retrofit interface and the generated request / response DTOs.
Whenever the spec pointer advances, three things must happen in this order:

1. Make sure the submodule pointer is up to date:

   ```
   git submodule update --init --remote ../openapi
   ```

   Or, if the spec edit happens on a branch of the submodule repo itself,
   `cd ../../openapi && git pull` (or check out the branch directly).
   The `openapi.input` property in `gradle.properties` defaults to
   `../../openapi/openapi.yaml`, so the gradle plugin reads whatever is
   checked out at that path.

2. Regenerate the Kotlin client:

   ```
   ./gradlew :app:generateV1Api
   ```

   The task wires the generated sources into the `main` Kotlin source set
   via `android.sourceSets["main"].kotlin.srcDir(...)` in
   `app/build.gradle.kts` and applies a post-processor that strips the
   `@JsonClass(generateAdapter = ...)` annotations from each generated
   DTO so they decode through `KotlinJsonAdapterFactory` instead of
   requiring `moshi-kotlin-codegen`. Re-run if the spec is touched even
   slightly: the KSP source set compatibility is strict and a stale
   generation silently passes type-checks against an outdated DTO.

3. Run the coverage guard:

   ```
   ./gradlew :app:verifyV1ApiCoverage
   ```

   The guard enumerates every `operationId` in the submodule YAML and
   confirms the generated client has a
   `suspend fun <operationId-as-camelCase>(...)` method. It also asserts
   the generated `apis/` directory exists. A missing method is the most
   common failure mode after a spec edit that renamed an operation
   without regenerating; never hand-edit the generated sources to fix a
   coverage miss, regenerate instead.

4. Run the project verification suite so the guard and the regeneration
   both run in the same configuration:

   ```
   ./gradlew :app:check
   ```

   `:app:check` depends on `verifyV1ApiCoverage` and the
   design-system / server-secret guards. If `check` is green, the spec
   and the client are in sync.

Cross-reference: the dump script that produces the committed YAML lives in
`../tastile-core` (the `dump_openapi` binary, plus the
`scripts/dump-openapi-yaml.py` / `.ts` wrapper that ships the JSON output
through the YAML serializer and into the submodule). The end-to-end
flow is owned by `tastile-core`; Android only consumes the published
YAML. If the spec is intentionally a stand-in for an unmerged backend
change, edit `openapi.input` locally (`-Popenapi.input=...`) rather than
committing a hand-edited copy.

Validation contract for the change:
`./gradlew :app:testDebugUnitTest` must still pass after the
regeneration. The V1 client DTOs are used by unit tests under
`app/src/test/java/app/tastile/android/data/api/`. A regeneration that
changes a DTO shape will surface as a test compile error in those tests.
