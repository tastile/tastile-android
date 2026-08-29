# v1 OpenAPI Coverage

Tracks the delta between the v1 API contract (`tastile-core/v1/*.md`) and the
utoipa-generated OpenAPI document (`tastile-core/crates-v1/api/src/openapi.rs`),
emitted as the cross-repo canonical YAML at
`../../openapi/openapi.yaml` (the workspace-shell submodule at
`tastile-root/openapi/`). The Android client's auto-generation pipeline
(`./gradlew :app:generateV1Api`) only covers the operations present in the
OpenAPI document; everything else stays on the hand-rolled `V1ApiClient`
(HttpURLConnection).

## How the pipeline works

1. Refresh the submodule pointer (publishes a new spec version produced
   by `tastile-core`'s `dump_openapi` binary, serialized to YAML, and
   committed to the submodule repo):

   ```
   git submodule update --init --remote ../openapi
   ```

   The path `../../openapi/openapi.yaml` (from `tastile-android/`) resolves
   to that submodule. The `openapi.input` Gradle property in
   `gradle.properties` controls the path so CI / local overrides can pin a
   different spec without editing the build script.
2. `./gradlew :app:generateV1Api` — runs openapi-generator
   (`org.openapi.generator` v7.13.0) with `kotlin` / `jvm-retrofit2` to
   produce a Retrofit + Moshi client into
   `app/build/generated/openapi/v1/`.
3. `./gradlew :app:verifyV1ApiCoverage` — drift guard; fails the build if
   the submodule YAML lists an operation whose `operationId` has no
   generated method (or no wrapper in `V1GeneratedApiClient`).

The submodule YAML at `../../openapi/openapi.yaml` is the **input**. The
generated sources under `app/build/generated/openapi/v1/` are the
**output** and are gitignored via the project-root `build/` rule.

## Covered by openapi.json (10 operations, auto-generated)

| Method | Path | operationId | Wrapped in `V1GeneratedApiClient` |
| --- | --- | --- | --- |
| `GET` | `/v1/tiles` | `list_tiles` | `listTiles(...)` |
| `GET` | `/v1/source-tiles` | `list_source_tiles` | `listSourceTiles(...)` |
| `POST` | `/v1/source-tiles` | `create_source_tile` | `createSourceTile(...)` |
| `GET` | `/v1/source-tiles/{id}` | `get_source_tile` | `getSourceTile(...)` |
| `PUT` | `/v1/source-tiles/{id}` | `update_source_tile` | `updateSourceTile(...)` |
| `POST` | `/v1/source-tiles/{id}/cancel` | `cancel_source_tile` | `cancelSourceTile(...)` |
| `POST` | `/v1/source-tiles/{id}/reflow` | `reflow_source_tile` | `reflowSourceTile(...)` |
| `GET` | `/v1/source-tiles/{id}/completion` | `get_source_tile_completion` | `getSourceTileCompletion(...)` |
| `GET` | `/v1/source-tiles/{id}/placements` | `list_source_tile_placements` | `listSourceTilePlacements(...)` |
| `POST` | `/v1/schedule-definitions` | `publish_schedule_definition` | `publishScheduleDefinition(...)` |

The DTOs are the Moshi-annotated classes in
`app.tastile.android.data.api.generated.v1.models.*`. They are NOT mapped to
the hand-written `V1Models.kt` types because the wire shape diverged (UUID
type, additional `relations` field, sealed `oneOf` for discriminated unions).
For the existing hand-rolled paths, `V1ApiClient` (HttpURLConnection) remains
the source of truth.

## Not yet in openapi.json (hand-rolled in `V1ApiClient`, follow-up in core)

The following v1 routes exist in `tastile-core` (per `crates-v1/api/src/handlers/*.rs`
and `tastile-core/v1/*.md`) but lack `#[utoipa::path]` annotations. They are
served by the hand-rolled `V1ApiClient` today. Closing the gap requires
adding the missing `#[utoipa::path]` annotations in `crates-v1/api/src/`
handlers and re-running `dump_openapi` — this is core-side work tracked here.

### Command API (`handlers::commands`)

| Endpoint | Section | Android status |
| --- | --- | --- |
| `POST /v1/tiles` (CREATE_TILE) | v1/14 §2.5 | hand-rolled in `V1ApiClient.createTile` |
| `POST /v1/tiles/{tileId}/plan` (SET_PLAN) | v1/14 §2.5 | hand-rolled in `V1ApiClient.setPlan` |
| `POST /v1/placements` (CREATE_PLACEMENT) | v1/14 §2.5 | hand-rolled in `V1ApiClient.createPlacement` |
| `POST /v1/placements/{id}/changes` (APPEND_CHANGES) | v1/14 §2.5 | **not implemented** |
| `POST /v1/placements/{id}/executions` (START_EXECUTION) | v1/14 §2.5 | hand-rolled in `V1ApiClient.startExecution` |
| `POST /v1/placements/{id}/close` (CLOSE_PLACEMENT) | — | hand-rolled via `V1CommandDispatcher` |
| `POST /v1/placements/{id}/detach` (DETACH_PLACEMENT) | — | **not implemented** |
| `POST /v1/executions/{id}/pause` (PAUSE_EXECUTION) | v1/14 §2.5 | hand-rolled in `V1ApiClient.pauseExecution` |
| `POST /v1/executions/{id}/resume` (RESUME_EXECUTION) | v1/14 §2.5 | hand-rolled in `V1ApiClient.resumeExecution` |
| `POST /v1/executions/{id}/finish` (FINISH_EXECUTION) | v1/14 §2.5 | hand-rolled in `V1ApiClient.finishExecution` |
| `POST /v1/tiles/{id}/memos` (ATTACH_MEMO) | — | hand-rolled in `V1CommandDispatcher.dispatchMemoAttach` |
| `POST /v1/tiles/{id}/update` (UPDATE_TILE) | — | hand-rolled in `V1ApiClient` |
| `POST /v1/tiles/{id}/start` (START_TILE) | — | hand-rolled via `V1CommandDispatcher.dispatchTileStart` |
| `POST /v1/tiles/{id}/complete` (SET_TILE_LIFECYCLE) | — | hand-rolled via `V1CommandDispatcher.dispatchTileComplete` |
| `POST /v1/tiles/{id}/defer` (SET_TILE_LIFECYCLE) | — | hand-rolled via `V1CommandDispatcher.dispatchTileDefer` |
| `POST /v1/tiles/{id}/extend-phase` (SET_TILE_LIFECYCLE) | — | not implemented (`dispatchTileExtend` returns null) |
| `DELETE /v1/tiles/{id}` (ARCHIVE_TILE) | — | hand-rolled in `V1ApiClient` |
| `POST /v1/prompts` | — | hand-rolled in `V1ApiClient` |
| `POST /v1/tick` | — | not implemented |

### Read API (`handlers::read`, per-aggregate)

| Endpoint | Section | Android status |
| --- | --- | --- |
| `GET /v1/tiles/{id}` | v1/14 §9 | hand-rolled in `V1ApiClient.readTile` |
| `GET /v1/tiles/{id}/detail` | — | not implemented |
| `GET /v1/tiles/{id}/editable` | — | not implemented |
| `GET /v1/placements/{id}` | v1/14 §9 | not implemented |
| `GET /v1/placements` | v1/14 §9 | hand-rolled in `V1ApiClient.listPlacements` |
| `GET /v1/candidates` | — | not implemented |
| `GET /v1/executions/{id}` | v1/14 §9 | hand-rolled in `V1ApiClient.readExecution` |
| `GET /v1/executions/{id}/view` | — | not implemented |
| `GET /v1/executions/{id}/basis` | — | not implemented |
| `GET /v1/change-sets/{id}` | — | not implemented |
| `GET /v1/recurring/{id}` | — | not implemented |
| `GET /v1/recurring/{id}/rules` | — | 501 stub |
| `GET /v1/recurring/{id}/frame-rules` | — | 501 stub |
| `GET /v1/plans/{id}` | — | not implemented |
| `GET /v1/conditions/{id}` | — | 501 stub |
| `GET /v1/windows/{id}` | — | not implemented |
| `GET /v1/flows/{id}` | — | 501 stub |
| `GET /v1/decisions/{id}` | — | 501 stub |
| `GET /v1/sessions` | v1/14 §9 | hand-rolled in `V1ApiClient.listPendingSessions` |
| `GET /v1/sessions/{id}` | v1/14 §9 | hand-rolled in `V1ApiClient.readSession` |
| `GET /v1/sessions/{id}/feedback` | — | not implemented |
| `GET /v1/work-items/{id}` | — | 501 stub |
| `GET /v1/labels` | — | not implemented |
| `GET /v1/schedule-reference-catalog` | — | not implemented |

### Sync, Endpoints, Notifications (`handlers::sync` / `endpoints` / `notifications`)

| Endpoint | Section | Android status |
| --- | --- | --- |
| `GET /v1/sync?cursor=...` | v1/14 §6 | not implemented (Android uses CoreRuntimeService daemon bridge, not v1) |
| `POST /v1/endpoints` | v1/14 §5 | hand-rolled in `V1ApiClient.registerEndpoint` |
| `GET /v1/endpoints` | v1/14 §5 | hand-rolled in `V1ApiClient.listEndpoints` |
| `DELETE /v1/endpoints/{id}` | v1/14 §5 | hand-rolled in `V1ApiClient.deleteEndpoint` |
| `GET /v1/deliveries/{id}` | v1/14 §7 | not implemented |
| `POST /v1/deliveries/{id}/delivered` | v1/14 §7 | not implemented |
| `POST /v1/deliveries/{id}/failed` | v1/14 §7 | not implemented |
| `GET /v1/sessions/{id}/deliveries` | v1/14 §7 | not implemented |
| `GET /v1/endpoints/{id}/deliveries` | v1/14 §7 | not implemented |

### Owner profile + Avatar upload (`handlers::owner` / `upload_avatar` / `owner_scope_profile`)

| Endpoint | Section | Android status |
| --- | --- | --- |
| `GET /v1/owners/{kind}/{id}/profile` | v1/15 §3 | not implemented (`ProfileRepository` is a local stub) |
| `PATCH /v1/owners/{kind}/{id}/profile` | v1/15 §3 | not implemented |
| `GET /v1/scopes/{kind}/{id}/members/{actor_kind}/{actor_id}/profile` | v1/15 §4 | not implemented (server returns 501) |
| `PUT /v1/scopes/.../profile-override` | v1/15 §4 | not implemented (server returns 501) |
| `DELETE /v1/scopes/.../profile-override` | v1/15 §4 | not implemented (server returns 501) |
| `POST /v1/uploads/avatar` | v1/15 §3 | not implemented |
| `POST /v1/uploads/avatar/{upload_id}/commit` | v1/15 §3 | not implemented |

### Tile edit (`handlers::tile_edit`)

| Endpoint | Section | Android status |
| --- | --- | --- |
| `PATCH /v1/tiles/{id}` (with `If-Match`) | — | not implemented (separate from `POST /v1/tiles/{id}/update`) |

### Schedule drafts (`handlers::schedule_drafts`)

| Endpoint | Section | Android status |
| --- | --- | --- |
| `POST /v1/schedule-drafts` | — | not implemented |
| `GET /v1/schedule-drafts/{id}` | — | not implemented |
| `POST /v1/schedule-drafts/{id}/operations` | — | not implemented |

### Auth + access (`handlers::auth` / `handlers::access`)

| Endpoint | Section | Android status |
| --- | --- | --- |
| `POST /v1/auth/signup` | — | not implemented (uses Cognito) |
| `POST /v1/auth/signin` | — | not implemented (uses Cognito) |
| `POST /v1/auth/signout` | — | not implemented |
| `GET /v1/auth/session` | — | not implemented |
| `POST /v1/auth/session/restore` | — | not implemented |
| `GET /v1/auth/oauth/status` | — | not implemented |
| `POST /v1/api-tokens` | — | hand-rolled in `V1ApiClient.mintApiTokenViaWeb` |
| `GET /v1/api-tokens` | — | not implemented |
| `PATCH /v1/api-tokens/{id}` | — | not implemented |
| `DELETE /v1/api-tokens/{id}` | — | not implemented |
| `GET /v1/health` | — | not implemented |
| `GET /v1/ready` | — | not implemented |
| `GET /v1/version` | — | not implemented |
| `GET /v1/access/capabilities` | — | not implemented |
| `POST /v1/access/subjects` | — | not implemented |
| `GET /v1/access/subjects` | — | hand-rolled in `V1ApiClient.listWorkspaces` (filters `kind=1`) |
| `GET /v1/access/subjects/{id}` | — | not implemented |
| `PATCH /v1/access/subjects/{id}` | — | hand-rolled in `V1ApiClient.updateWorkspace` |
| `DELETE /v1/access/subjects/{id}` | — | hand-rolled in `V1ApiClient.deleteWorkspace` |
| `POST /v1/access/workspaces` | — | hand-rolled in `V1ApiClient.createWorkspace` |
| `GET /v1/access/subjects/by-external` | — | not implemented |
| `POST /v1/access/offers` / `requests` / `grants*` / `notifications*` | — | not implemented |

### Misc

| Endpoint | Section | Android status |
| --- | --- | --- |
| `GET /v1/active-tile` | v1/14 §9 | hand-rolled in `V1ApiClient.getActiveTile` |
| `GET /v1/timeline?start=...&end=...` | v1/14 §4 | hand-rolled in `V1ApiClient.getTimeline` |
| `GET /v1/runtime/paths` | — | hand-rolled in `V1ApiClient.listRuntimePaths` |

## Closing the gap

1. Add `#[utoipa::path]` to the unannotated handlers in
   `tastile-core/crates-v1/api/src/handlers/*.rs`. Many can be derived from
   the existing inline doc comments.
2. Update the `ApiDoc` `#[openapi(... paths(...))]` list at
   `tastile-core/crates-v1/api/src/openapi.rs` to include the newly annotated
   paths.
3. Re-run `cargo run -p api --bin dump_openapi` and let the core sync
   script publish the new YAML to the `openapi` submodule repo.
4. Bump the submodule pointer in the workspace root
   (`git submodule update --remote ../openapi`) and re-run
   `./gradlew :app:generateV1Api` to regenerate the client. New
   `operationId`s must be added to `V1GeneratedApiClient` as wrapper methods,
   or `verifyV1ApiCoverage` will fail the build.

Each iteration: one `#[utoipa::path]` annotation + one operationId wrapper.
No Kotlin refactor needed.
