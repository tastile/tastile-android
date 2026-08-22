# Execution / Session P0

## Scope

- Add wire-compatible Execution and workflow Session DTOs and API calls.
- Add an Execution repository that restores paused executions by persisted execution ID.
- Add Execution and Decision view models.
- Refresh the timeline after successful Session feedback.
- Keep identifiers and feedback changes generic; do not encode prompt-specific behavior.

FCM, delivery endpoints, notification actions, and UI composition are out of scope.

## Contract

The current core implementation exposes open sessions at `GET /v1/sessions`,
session detail at `GET /v1/sessions/{id}`, active execution discovery at
`GET /v1/active-tile`, and authoritative execution state at
`GET /v1/executions/{id}`. Commands use the v1 command envelope.

## Verification

Add API contract, repository restoration, and decision refresh unit tests, then
run the repository `verify` task in WSLC.
