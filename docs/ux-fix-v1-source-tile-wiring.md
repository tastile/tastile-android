# v1 Source-Tile Wiring Fix (Mobile QuickCreate)

> Implementation note, 2026-08-09. Read `docs/ux-investigation-tile-creation.md`
> for the gap analysis that motivated this PR. Scope: make the Mobile
> QuickCreate composer POST to `POST /v1/source-tiles` (via the existing
> `V1ApiClient.createSourceTile`) and surface four high-priority
> `SourceScheduleDefinition` fields in the UI.

## TL;DR

The Mobile QuickCreate was wired to the v0 `POST /v1/tiles` +
`POST /v1/tiles/{id}/plan` flow (see
`app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubmission.kt:92-108`
in the previous revision). Every `SourceScheduleDefinition` field beyond
`required_duration_ms` was dropped. This PR:

1. Routes the Mobile QuickCreate through the canonical `POST /v1/source-tiles`
   endpoint first, with the v0 path as a `NOT_FOUND` fallback.
2. Surfaces four new fields in the UI (priority, split_policy,
   generation.offset_min, generation.excluded_dates).

## Files Changed

| File | Lines | Change |
| --- | --- | --- |
| `app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateState.kt` | 15, 152-176, 185-194, 221 | New `QuickCreatePanel.Schedule` enum value, new `QuickCreateSchedule` data class, `schedule` field on `QuickCreateDraftState`, `updateSchedule` store method. |
| `app/src/main/java/app/tastile/android/data/api/V1CommandPayloads.kt` | 343-356 | Added `offsetMin: Int? = null` to `SourceGenerationPayload`. No `@EncodeDefault` so existing wire-shape contract tests stay green. |
| `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubpanels.kt` | 47, 153, 568-577, 1107-1246 | Added `Icons.Outlined.Tune` import; routed `QuickCreatePanel.Schedule`; added "Schedule" entry in `IntentPanel`; new private `SchedulePanel` composable. |
| `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubmission.kt` | 4-6, 14-24, 28-33, 53, 61, 94-258 | New `createSourceTile` gateway method, primary v1 `submitSourceTile` path with NOT_FOUND fallback to v0, new `buildSourceTileWritePayload` + `buildGenerationPayload` + `timeOfDayOffsetMs` helpers. |
| `app/src/test/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubmissionTest.kt` | 1-89 | Added `createSourceTile` to `FakeGateway`, updated expectations for the v1-first ordering, added explicit `v0 fallback fires when v1 endpoint returns NOT_FOUND` test. |

## New State Fields

`QuickCreateSchedule` (added to `app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateState.kt`):

| Field | Type | Default | Wire target |
| --- | --- | --- | --- |
| `priority` | `Int` | `5` | `schedule.priority` (i32, 0..10) |
| `splitPolicyKind` | `Short` | `0` (unsplit) | `schedule.split_policy.kind` (i16, 0=unsplit / 1=split) |
| `splitPolicyMinSegmentMs` | `Long` | `0L` | `schedule.split_policy.min_segment_ms` (i64) |
| `splitPolicyMaxSegmentMs` | `Long` | `Long.MAX_VALUE` | `schedule.split_policy.max_segment_ms` (i64) |
| `splitPolicyMaxSegments` | `Int` | `1` | `schedule.split_policy.max_segments` (u32) |
| `offsetMin` | `Int` | `0` (UTC) | `schedule.generation.offset_min` (i32 UTC minutes) |
| `excludedDates` | `List<String>` | `emptyList()` | `schedule.generation.excluded_dates` (`Vec<String>`, ISO yyyy-MM-dd) |

A new `schedule` field on `QuickCreateDraftState` (default
`QuickCreateSchedule()`) carries the slice through panel navigation. The
existing `QuickCreateStateStore` mutators all remain compatible — a new
`updateSchedule(schedule: QuickCreateSchedule)` method is added.

## UI Controls

The new `SchedulePanel` composable (`QuickCreateSubpanels.kt:1107-1246`) is
reached from the `IntentPanel` (a "Schedule" entry with `Icons.Outlined.Tune`
now joins the existing Time / References / Meta / Completion targets). It
contains:

1. **Priority** — `LocalNumberField`, value coerced to 0..10 on input.
2. **Split policy** — `FilterChip` row "Unsplit / Split" (`QuickCreatePanel.Schedule`).
   When "Split" is selected, three additional `LocalNumberField` controls appear
   for `Min segment (ms)`, `Max segment (ms)` (empty displays as `Long.MAX_VALUE`),
   and `Max segments (1..N)`.
3. **Calendar offset** — `LocalNumberField` for `Offset (UTC minutes)`, clamped
   to ±720 min (±12h).
4. **Excluded dates** — `LazyRow` of `FilterChip`s (each tag ends in "×" and is
   tappable to remove) plus a "Add excluded date" `NiaFilledTonalButton` that
   opens the existing `DatePickerSheet`. Selected dates are formatted ISO
   `yyyy-MM-dd`.

All controls reuse existing m3 widgets (`OutlinedTextField`,
`KeyboardType.Number`, `FilterChip`, `DatePickerSheet`,
`NiaFilledTonalButton`). No new dependencies. No design-system internals
modified.

## Wire Payload Shape

The dispatcher builds a `SourceTileWritePayload` from the draft state
(`QuickCreateSubmission.kt:156-211`):

```
SourceTileWritePayload(
  tile = SourceTileDefinitionPayload(
    title = draft.identity.title.trim(),
    description = draft.identity.description,
    color = draft.identity.visual.color,
    icon = draft.identity.visual.icon,
    externalId = draft.identity.externalId,
  ),
  plan = SchedulePlanDefinitionPayloadTyped(
    role = role(draft.plan.role),                  // 0=Executable, 1=Label
    references = emptyList(),
    completion = CompletionSchema(
      root = ConditionRef(ConditionAny(emptyList())),   // minimal placeholder
      time_requirements = emptyList(),
      tasks = emptyList(),
    ),
    planning = SchedulingPlanningDefinitionSchema(
      placement_rules = emptyList(),
      nesting_rules = emptyList(),
    ),
    metrics = emptyList(),
    decisions = emptyList(),
  ),
  flows = emptyList(),
  schedule = SourceSchedulePayload(
    requiredDurationMs = draft.time.durationMinMax.minMs ?: maxMs ?: 0L,
    generation = SourceGenerationPayload(
      kind = recurring.repeatMode → 0 (Once) | 1 (Daily/Weekly/Interval/Condition),
      startsAt = recurring.endDate → "…T00:00:00Z" (Recurring only),
      intervalMs = frameRules[0].generator.value.step (Interval mode only),
      endsAt = recurring.endDate → "…T23:59:59Z",
      weekdayMask = recurring.weekdayMask (Weekly only),
      excludedDates = schedule.excludedDates,
      offsetMin = schedule.offsetMin,
    ),
    window = SourceWindowPayload(
      startOffsetMs = time.timeOfDayStart → nanos,
      endOffsetMs = time.timeOfDayEnd → nanos,
    ),
    splitPolicy = SourceSplitPolicyPayload(
      kind = schedule.splitPolicyKind,
      minSegmentMs = schedule.splitPolicyMinSegmentMs,
      maxSegmentMs = schedule.splitPolicyMaxSegmentMs,
      maxSegments = schedule.splitPolicyMaxSegments,
    ),
    priority = schedule.priority,
  ),
  horizon = PlacementSpanPayload(start, end),
)
```

## Submission Order

`QuickCreateSubmissionDispatcher.submit` (`QuickCreateSubmission.kt:94-118`)
now:

1. Calls `submitSourceTile` (POSTs `SourceTileWritePayload` via
   `V1ApiClient.createSourceTile`).
2. If the v1 endpoint returns `V1Error.Api(kind=NOT_FOUND)` (i.e. the
   backing server has not rolled out the source-tile handler yet), falls
   back to the existing v0 `submitPlacement` / `submitRecurring` path.
3. Any other `V1Error` (validation, conflict, etc.) is surfaced to the
   user — not silently retried against the v0 path.

## Caveats / Punted Work

- **Plan shape on the v1 path is a minimal stub.** The typed
  `SchedulePlanDefinitionPayloadTyped` requires an externally-tagged
  Condition AST (`ConditionAny/All/Not/Term`). The Mobile QuickCreate
  draft stores an internally-tagged `QuickCreateConditionNode` that the
  v0 path round-trips as raw JSON through `SetPlanPayload`. For the v1
  path we emit `Any([])` + empty references / planning / metrics /
  decisions. Rich plan state (references, completion, time requirements,
  tasks, planning rules, metrics, decisions) only survives via the v0
  fallback today. Mapping the internal-tagged tree to the external-tagged
  `ConditionRef` AST is documented as a follow-up.

- **`offsetMin` was added to `SourceGenerationPayload`** to let the new
  field reach the wire. The change is additive (nullable, no
  `@EncodeDefault`) so the existing
  `source_tile_create_and_update_share_the_canonical_payload_shape`
  contract test still asserts the same exact JSON wire shape.

- **The Dashboard QuickCreate is untouched** per the PR scope. It still
  uses `V1CommandDispatcher.dispatchTileCreate` and the v0 thin-create +
  `setPlan` flow. Migration is a separate effort — see
  `docs/ux-investigation-tile-creation.md` §5 priority 4.

- **Only the four fields named in the PR scope are wired.** The full
  ~80-leaf v1 SourceTile gap (relations, flows, metrics, decisions,
  preferred_duration, include/anchor, automation flags, interruption
  flags, windows-rules subeditor, etc.) remains as separate work.

## Compilation Verification

Could not be verified locally — the Android SDK is not installed on this
workstation (`ANDROID_HOME` is unset; Gradle reports "SDK location not
found" before any Kotlin compile runs). The code was instead verified by:

1. Tracing all imports against their definitions (every `import` symbol
   used resolves to a class/object/function that exists in the codebase).
2. Reading the existing wire-shape contract test to confirm that adding
   `offsetMin: Int? = null` (no `@EncodeDefault`) does not change the
   encoded JSON.
3. Confirming that adding `schedule: QuickCreateSchedule = QuickCreateSchedule()`
   as the last parameter of `QuickCreateDraftState` is safe — every
   existing constructor call site uses named arguments.

Run `./gradlew :app:compileDebugKotlin` and `./gradlew :app:testDebugUnitTest --tests "*QuickCreateSubmissionTest"`
on a workstation with the Android SDK to confirm.

## Follow-up

- Wire `V1ApiClient.updateSourceTile(...)` to a new `SourceTileEditSheetMobile`.
- Map internally-tagged `QuickCreateConditionNode` to externally-tagged
  `ConditionRef` so the rich plan state survives the v1 source-tile path.
- Add priority/split_policy UI to the Dashboard QuickCreate (separate
  composer / state shape).
- Decide on a `flow` representation that the `FlowDefinitionSchema` accepts
  (the v0 path round-trips `flows` as raw JSON in `placement_rules`).