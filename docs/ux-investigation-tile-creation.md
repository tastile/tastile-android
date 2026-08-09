# Tile Creation UX Gap Analysis (Android vs Web vs v1 Schema)

> Read-only investigation, 2026-08-09. Focus: do the Android tile creation forms
> expose every field present in the v1 source-tile schema and the Web creator?
> No code is modified by this report.

## TL;DR

The Android app has **two parallel tile creation surfaces**, both thin
non-`SourceTile` facades:

1. **Dashboard QuickCreate** (`ui/dashboard/QuickCreateSheet.kt`, ~800 lines).
   Wired to the v0 `tile.create` command via `V1CommandDispatcher`, NOT the v1
   `createSourceTile` endpoint. Models the dashboard in 12 flat UI fields and
   reuses only `next_action` / `done_definition` / `annotation` of the v1
   domain.
2. **Mobile QuickCreate** (`ui/mobile/sheets/QuickCreateSheetMobile.kt` +
   `QuickCreateState.kt` + `quickcreate/QuickCreateSubpanels.kt`). Uses the
   richer draft shape (`window`, `conditions`, `flow`, `placement_rules`,
   `nesting_rules`, `metrics`, `decisions`, `references`, `time_requirements`,
   `tasks`, `frame_rules`, `rules`). Still wired to the v0 thin-create +
   `setPlan` flow (`POST /v1/tiles` + `POST /v1/tiles/{id}/plan`).

The v1 OpenAPI canonical `CreateSourceTilePayload` (used by the Web app at
`POST /v1/source-tiles`) projects **never** used by Android. Every v1
`SourceScheduleDefinition` field beyond `required_duration_ms` is dropped on
the Android wire today.

The Web creator (`tastile-web/src/features/create-tile/ui/QuickCreate.tsx`)
exposes **the full v1 schema** — every SourceTile field is editable, while
sub-panels for `Completion`, `Metrics`, `Flow`, `PlacementRules`,
`References`, `Relation`, `Recurring` (`SourceGeneration`) and `SourceWindow`
exist. The "input widget gap" the user noticed is most visible in the Mobile
QuickCreate: time is captured as HH:MM strings via `TimeInput`, weekday picks
are chip rows, and the only DatePicker is the daily-schedule date range.

## 1. Android current state

### 1.1 Dashboard QuickCreate (legacy)

File: `app/src/main/java/app/tastile/android/ui/dashboard/QuickCreateSheet.kt`
(view model: `DashboardViewModel.kt` line 77-103, payload builder lines 989-1068).

| # | Field name | UI widget | Source line | Maps to v1 payload key |
|---|------------|-----------|-------------|------------------------|
| 1 | `title` | `OutlinedTextField` (singleLine) | `QuickCreateSheet.kt:278-288` | `payload.tile.title` |
| 2 | `tileKind` (work / label) | `SingleChoiceSegmentedButtonRow` | `QuickCreateSheet.kt:292-302` | `payload.annotation.semantic_role` |
| 3 | `objectiveMode` (finish_once / recurring / maximize_within_interval) | `SingleChoiceSegmentedButtonRow` | `QuickCreateSheet.kt:306-323` | `payload.objective.objective_mode` |
| 4 | `useStartAt` (bool) | `Switch` | `QuickCreateSheet.kt:466-472` | `payload.temporal.fixed_start` |
| 5 | `startDate` | `OutlinedTextField` + `DatePickerDialog` | `QuickCreateSheet.kt:474-481` + `DateTimeField` | `payload.temporal.fixed_start` |
| 6 | `startTime` | `OutlinedTextField` + `TimePicker` (m3) | `DateTimeField` line 717-733 | `payload.temporal.fixed_start` |
| 7 | `useEndAt` (bool) | `Switch` | `QuickCreateSheet.kt:484-491` | `payload.temporal.fixed_end` |
| 8 | `endDate` | `DatePicker` | `QuickCreateSheet.kt:493-501` | `payload.temporal.fixed_end` |
| 9 | `endTime` | `TimePicker` | same | `payload.temporal.fixed_end` |
| 10 | `recurrenceFrequency` (daily / weekly / monthly) | `PrimaryTabRow` | `QuickCreateSheet.kt:330-353` | `payload.objective.recurrence.selector` (built from `buildRecurrenceExpression`) |
| 11 | `recurrenceInterval` (numeric) | `OutlinedTextField` (numeric) | `QuickCreateSheet.kt:355-361` | embedded in expression string |
| 12 | `recurrenceWeekdays` (CSV) | `OutlinedTextField` (CSV) | `QuickCreateSheet.kt:363-371` | embedded in expression string |
| 13 | `recurrenceMonthlyWeek` (numeric) | `OutlinedTextField` | `QuickCreateSheet.kt:378-384` | embedded in expression string |
| 14 | `recurrenceMonthlyWeekday` (numeric) | `OutlinedTextField` | `QuickCreateSheet.kt:385-391` | embedded in expression string |
| 15 | `recurrenceStartTime` (HH:MM string) | `OutlinedTextField` (text) | `QuickCreateSheet.kt:395-401` | `payload.objective.recurrence.window.start_offset_min` |
| 16 | `recurrenceEndTime` (HH:MM string) | `OutlinedTextField` (text) | `QuickCreateSheet.kt:403-409` | `payload.objective.recurrence.window.end_offset_min` |
| 17 | `recurrenceValidFrom` enabled switch | `Switch` | `QuickCreateSheet.kt:411-421` | `payload.temporal.release_at` |
| 18 | `recurrenceValidFromDate` | `DateTimeField` (date only) | `QuickCreateSheet.kt:423-433` | `payload.temporal.release_at` |
| 19 | `recurrenceValidTo` enabled switch | `Switch` | `QuickCreateSheet.kt:435-445` | `payload.temporal.due_at` |
| 20 | `recurrenceValidToDate` | `DateTimeField` (date only) | `QuickCreateSheet.kt:447-457` | `payload.temporal.due_at` |
| 21 | `workHours` / `workMinutes` | `DurationInput` (text) | `QuickCreateSheet.kt:509-516` | `payload.objective.target_work_min` |
| 22 | `breakSplitsWork` (bool) | `SingleChoiceSegmentedButtonRow` | `QuickCreateSheet.kt:525-536` | `payload.interruption.break_splits_work` |
| 23 | `project` (label) | `AutoCompleteTextField` | `QuickCreateSheet.kt:544-550` | `payload.annotation.labels` (prefixed `project:`) |
| 24 | `tags` (multi) | `AutoCompleteTextField` + `FilterChip` | `QuickCreateSheet.kt:552-575` | `payload.annotation.labels` |
| 25 | `memo` | `OutlinedTextField` (multi-line) | `QuickCreateSheet.kt:582-588` | `payload.next_action` (re-purposed as memo) |

**Total Android dashboard fields exposed: 25.**

The dashboard composer does not use any `date_range_start/end`,
`weekday_mask`, `excluded_dates`, `priority`, `split_policy`,
`offset_min`, `interval_ms`, `preferred_duration`, `plan_role`,
`completion.*`, `metrics`, `decisions`, `placement_rules`, `nesting_rules`,
`flows`, `relations`, `references`, `windows`, `frame_rules`, `rules`,
`tile.description`, `tile.color`, `tile.icon`, `tile.external_id` (the
Web/Mobile draft has `description`/`color`/`icon` in the identity slice but
the dashboard composer never writes them). Note: the dashboard builder
(`DashboardViewModel.buildCreatePayload`) hardcodes `interrupt_penalty=3`,
`resume_penalty=3`, `external_interrupt_only=false`, `prompt_on_start=false`,
`prompt_on_end=true`, `auto_start_allowed=false`, `auto_end_allowed=false`,
`done_rule="manual"`. The user has no UI for these.

### 1.2 Mobile QuickCreate (current)

File: `app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateState.kt`
(plus `quickcreate/QuickCreateBasePanel.kt`, `quickcreate/QuickCreateSubpanels.kt`,
`quickcreate/QuickCreateSubmission.kt`, `quickcreate/QuickCreateSubmissionViewModel.kt`).

The mobile composer mirrors the Web slice types. Each Compose subpanel collects
the right slice; the submit path (`QuickCreateSubmission.kt:78-142`) builds
`CreateTilePayload` + `SetPlanPayload` and posts to `POST /v1/tiles` and
`POST /v1/tiles/{id}/plan` — not the v1 source-tile endpoint.

| # | Field name | UI widget | Source line | Notes |
|---|------------|-----------|-------------|-------|
| 1 | `identity.title` | `BasicTextField` (underline) | `QuickCreateBasePanel.kt:312-368` | – |
| 2 | `identity.description` | subpanel (`Textarea`? — not implemented) | – | **Mobile has no description field in the UI** |
| 3 | `identity.color` | not exposed | – | `_Visual.color` defaults to `#3b82f6` |
| 4 | `identity.icon` | not exposed | – | `_Visual.icon` defaults to `"check-circle"` |
| 5 | `identity.externalId` | not exposed | – | `null` is always sent (auto-generated server-side) |
| 6 | `plan.role` (Executable / Label) | `Switch` | `QuickCreateBasePanel.kt:191-212` | – |
| 7 | `time.span.start`, `time.span.end` | subpanel `TimePanel` (`NativeDateField`) | `QuickCreateSubpanels.kt:218-219` | Date only (no time-of-day) |
| 8 | `time.whenMode` (None / Day / Range / Reference) | `SegmentedControl` | `QuickCreateSubpanels.kt:190` | – |
| 9 | `time.referenceId`, `referenceLabel` | `ReferencePickerSheet` | `QuickCreateSubpanels.kt:230` | – |
| 10 | `time.timeOfDayMode` (AllDay / Range / Unspecified) | `SegmentedControl` | `QuickCreateSubpanels.kt:234` | – |
| 11 | `time.timeOfDayStart` / `time.timeOfDayEnd` | `TimePickerSheet` (m3) | `QuickCreateSubpanels.kt:266-284` | – |
| 12 | `time.durationMinMax.minMs` / `maxMs` | `DurationPanel` (number text inputs) | `QuickCreateSubpanels.kt:452-477` | – |
| 13 | `windows[]` (kind, bounds.start/end, referenceId, rules) | subpanel `WindowRow` | `QuickCreateSubpanels.kt:346-450` | supports Calendar/LabelSpan/ParentSpan/Gap |
| 14 | `recurring.repeatMode` (Once / Daily / Weekly / Interval / Condition) | `SegmentedControl` | `QuickCreateSubpanels.kt:548` | – |
| 15 | `recurring.weekdayMask` | `LocalWeekdayPicker` (chip row) | `QuickCreateSubpanels.kt:1207-1229` | bit 0 = Mo ... bit 6 = Su |
| 16 | `recurring.endDate` | subpanel `EndDateInput` (date-only) | `QuickCreateSubpanels.kt` (EndDate pattern) | – |
| 17 | `recurring.lifecycleFilter` | not exposed | – | always default JSON |
| 18 | `recurring.frameRules[].generator` | text field "step" + `MaterialTheme` toggle | `QuickCreateSubpanels.kt:531-549` | generic JSON editor, not a structured picker |
| 19 | `plan.references[]` | `ReferencesPanel` (id, target, pick, interval number input) | `QuickCreateSubpanels.kt:481-559` | – |
| 20 | `plan.completion.root` | `ConditionControls` (calendar / moment / relation / gap / requirement / task / fact / metric / life) | `QuickCreateSubpanels.kt:767-` | full boolean tree editor |
| 21 | `plan.completion.timeRequirements[]` | subpanel | `QuickCreateSubpanels.kt:108-112` | per-row `NumberInput` for minMs/maxMs |
| 22 | `plan.completion.tasks[]` (id, title, note, conditions, order) | `TaskDetailSubPanel` | `QuickCreateState.kt:50-56` | – |
| 23 | `plan.planning.placementRules` | `JsonEditor` (raw text) | `QuickCreateSubpanels.kt:1107-1119` | raw JSON, not a structured editor |
| 24 | `plan.planning.nestingRules` | `JsonEditor` (raw text) | `QuickCreateSubpanels.kt:1107-1119` | raw JSON, not a structured editor |
| 25 | `plan.planning.flows` | `JsonEditor` (raw text) | `QuickCreateSubpanels.kt:1107-1119` | raw JSON, not a structured editor |
| 26 | `plan.metrics` | `JsonEditor` | `QuickCreateSubpanels.kt:1107-1119` | raw JSON |
| 27 | `plan.decisions` | `JsonEditor` | `QuickCreateSubpanels.kt:1107-1119` | raw JSON |
| 28 | `meta.ownerSubjectId` (project) | `FilterChip` row | `QuickCreateSubpanels.kt:1047-1061` | – |
| 29 | `meta.tags` | `LazyRow` + `OutlinedTextField` | `QuickCreateSubpanels.kt:1063-1082` | – |
| 30 | `meta.memo` | `OutlinedTextField` | `QuickCreateSubpanels.kt:1083` | – |

**Total Mobile QuickCreate fields exposed: 30.**

But the wired wire shape is the v0 `/v1/tiles` `CreateTilePayload` which only
forwards `kind`, `title`, `description`, `color`, `icon`, `external_id`,
`plan_role`, `owner_subject_id`, `frame_rule`. Every other slice (windows,
conditions, completion, time_requirements, placement_rules, flows, relations,
metrics, decisions, references, recurring.weekday_mask, repeats, endDate,
excluded_dates, offset_min, split_policy, priority) is round-tripped through
`SetPlanPayload` (`POST /v1/tiles/{id}/plan`) but per `v1-openapi-coverage.md`
the integration of `SetPlan` with the v1 plan shape is not part of the v0
openapi spec, so the server-side reaction is unverified outside the
test fixtures.

## 2. v1 schema fields (exhaustive)

Drawn from `tastile-core/crates-v1/api/src/openapi.rs:700-915` and the
generated `app/openapi/v1.json` lines 1378-4343.

### 2.1 `CreateSourceTilePayloadSchema` (top level)

Source: `openapi.rs:895-904`, `v1.json:1378-1423`.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `source_client_local_id` | `uuid?` | no | reconciliation id |
| `tile` | `ScheduleTileDefinitionSchema` | **yes** | see 2.2 |
| `plan` | `SchedulePlanDefinitionSchema` | **yes** | see 2.3 |
| `flows` | `Vec<FlowDefinitionSchema>` | **yes** | see 2.4 |
| `relations` | `Vec<SourceRelationDefinitionPayloadSchema>?` | no | see 2.5 |
| `schedule` | `SourceScheduleDefinitionSchema` | **yes** | see 2.6 |
| `horizon` | `SpanSchema` | **yes** | `{start, end}` ISO-8601 |

### 2.2 `ScheduleTileDefinitionSchema` (`v1.json:3694-3728`)

| Field | Type | Required |
|-------|------|----------|
| `title` | `string` | **yes** |
| `description` | `string?` | no |
| `color` | `string?` | no |
| `icon` | `string?` | no |
| `external_id` | `string?` | no |

### 2.3 `SchedulePlanDefinitionSchema` (`v1.json:3631-3672`)

| Field | Type | Required |
|-------|------|----------|
| `role` | `i16` (PlanRole registry) | **yes** |
| `references` | `Vec<ReferenceDefSchema>` | **yes** |
| `completion` | `CompletionSchema` | **yes** |
| `planning` | `SchedulePlanningDefinitionSchema` | **yes** |
| `metrics` | `Vec<MetricDefSchema>` | **yes** |
| `decisions` | `Vec<DecisionDefSchema>` | **yes** |

Nesting: `CompletionSchema` (root condition + time_requirements + tasks),
`SchedulePlanningDefinitionSchema` (placement_rules + nesting_rules),
`MetricDefSchema` (id + `output` enum + `expression` scalar expr + `limit`),
`DecisionDefSchema` (id + `observe` + `when` + `candidates` + `reuse` +
`dialog`).

### 2.4 `FlowDefinitionSchema` (`openapi.rs:832-837`)

| Field | Type | Required |
|-------|------|----------|
| `observes` | `Vec<ScheduleFlowSignalSchema>` | yes |
| `when` | `ConditionRefSchema?` | no |
| `candidates` | `Vec<ScheduleFlowCandidateDefinitionSchema>` | yes |

### 2.5 `SourceRelationDefinitionPayloadSchema` (`v1.json:3913-3981`)

`client_local_id`, `subject_source_ref`, `referenced_source_ref`, `kind`,
`point`, `offset_ms`, `ordering` (primary/point/direction),
`duration_expression` (Fixed / ReferenceSpan), `split_policy` (Unsplit /
Split), `correlation_scope`, `lifecycle_filter`, `eligible_through_revision`,
`summary_priority`. All fields required.

### 2.6 `SourceScheduleDefinitionSchema` (`v1.json:4066-4094`)

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `required_duration_ms` | `i64` | **yes** | – |
| `priority` | `i32` | **yes** | – |
| `generation` | `SourceGenerationSchema` | **yes** | see 2.7 |
| `window` | `SourceWindowSchema` | **yes** | see 2.8 |
| `split_policy` | `SplitPolicySchema` | **yes** | see 2.9 |

### 2.7 `SourceGenerationSchema` (`v1.json:3781-3860`)

| Field | Type | Required |
|-------|------|----------|
| `kind` | `i16` (0=OneTime, 1=Recurring, 2=DemandDriven) | **yes** |
| `at` | `DateTime?` | no |
| `starts_at` | `DateTime?` | no |
| `interval_ms` | `i64?` | no |
| `ends_at` | `DateTime?` | no |
| `weekday_mask` | `u8?` (Mon=1..Sun=64) | no |
| `date_range_start` | `String?` (ISO date) | no |
| `date_range_end` | `String?` (ISO date) | no |
| `excluded_dates` | `Vec<String>` (default `[]`) | no |
| `offset_min` | `i16?` (UTC offset in minutes) | no |

### 2.8 `SourceWindowSchema` (`v1.json:4275-4291`)

| Field | Type | Required |
|-------|------|----------|
| `start_offset_ms` | `i64` | yes |
| `end_offset_ms` | `i64` | yes |

### 2.9 `SplitPolicySchema` (`v1.json:4309-4343`)

| Field | Type | Required |
|-------|------|----------|
| `kind` | `i16` (0=unsplit, 1=split) | yes |
| `min_segment_ms` | `i64?` | no |
| `max_segment_ms` | `i64?` | no |
| `max_segments` | `u32?` | no |

### 2.10 `CreateSourceTileRequest` (`v1.json:1424-1453`)

| Field | Type | Required |
|-------|------|----------|
| `idempotency_key` | `uuid` | **yes** |
| `expected_revision` | `i64?` | no |
| `occurred_at` | `DateTime?` | no |
| `payload` | `CreateSourceTilePayloadSchema` | **yes** |

### 2.11 Auxiliary (not on the create payload but on the response)

`SourceTileRead` (`v1.json:4127-`): `source_tile_id`, `plan_id`, `owner_id`,
`revision`, `source_state`, `title`, `description?`, `color?`, `icon?`,
`external_id?`, `plan_role`, `schedule`, `created_at`, `updated_at`.

### 2.12 Total field count (payload-nested counts)

By a count of leaf fields under the create payload (counting union members
once), the v1 schema requires **at least 67 fields** to fully express a
SourceTile. Categories:

- Tile (5)
- Plan (5) -> Completion (4) + TimeRequirement (3) + Task (4) + Reference (3)
  + Planning (2) + Metric (4) + Decision (5) + ConditionRef
- Flow (3) -> Candidate (3) -> Output (3 + 3 + 3)
- Relation (12)
- Schedule (5) -> Generation (10) + Window (2) + SplitPolicy (4)
- Horizon (2)

Adding SourceTileRead output fields (10) and the request envelope (4) the
total reaches **~80 fields**.

## 3. Web current state

Primary file: `tastile-web/src/features/create-tile/ui/QuickCreate.tsx` (1249
lines). Sub-panels in the same directory:

| Subpanel | File | Fields exposed |
|----------|------|----------------|
| Schedule (when) | `SchedulePanel.tsx` | whenMode (None/Day/Range/Reference), range or day DatePicker, referenceId+Label, timeOfDayMode (AllDay/Range/Unspecified), timeOfDayStart/End (HH:MM `TimeInput`), windows[] (kind, bounds.start/end `DateTimePicker`, referenceId), quick-pick chips |
| Duration | `DurationSubPanel.tsx` | `time.durationMinMax.minMs` / `maxMs` (NumberInput, in minutes) |
| Recurring (SourceGeneration) | `SourceGenerationPanel.tsx` | `repeatMode` (once/daily/weekly/monthly/interval/condition), `weekdayMask` (chip row, bit 0 = Sunday), `intervalValue` + `intervalUnit` (min/hour/day), `endDate` (DatePickerInput + Switch), `condition` (disabled placeholder) |
| Source Window / Split | `SourceWindowPanel.tsx` | `time.durationMinMax.minMs` / `maxMs`, `source.preferredDurationMinMax.minMs` / `maxMs`, `source.offsetMin`, `source.priority`, `source.excludedDates` (TagsInput), `source.splitPolicy.kind` (Select) + `minSegment/maxSegment/maxSegments` (NumberInputs) |
| Flow | `FlowSequencePanel.tsx` | `observes` (MultiSelect), `when` (ConditionEditor), `candidateWhen`, `minimumGapMs`, `cycle`, `resetOnInterrupt`, `steps[]` (waitBeforeMs, emitDurationMs) |
| Relations | `RelationPanel.tsx` | `referencedSourceTileId` (Select), `kind` (Select), `point`, `offsetMs` (NumberInput), `ordering` (3 ints), `durationKind` (subject/reference/fixed), `fixedDurationMs`, `splitPolicy` (kind, requiredTotalDurationMs, minSegmentMs, maxSegmentMs), `correlationScope`, `lifecycleFilter`, `eligibleThroughRevision`, `summaryPriority` |
| Placement Rules | `PlacementRulesPanel.tsx` | `effect.kind` (Select), `scope`, `span`, `score`, `record`, `when` (Condition) |
| References | `ReferencesSubPanel.tsx` | `target`, `pick`, `when` |
| Completion | `CompletionSubPanel.tsx` | `root` (Condition tree), `timeRequirements[]` (id, scope/source/aggregate/quantifier, min/max), `tasks[]` (id, title, note, complete, order) |
| Task Detail | `TaskDetailSubPanel.tsx` | Task-level `complete`, `show`, `order`, `content` |
| Meta | `MetaSubPanel.tsx` | `ownerSubjectId`, `memo` |
| Identity | `QuickCreate.tsx:630-653` | `title` (TextInput placeholder), `kind` (SegmentedControl Executable/Label) |
| Visual | `QuickCreate.tsx` (popover) | `color`, `icon` (popover based on `essentialRow` `visualOpen` state) |
| External ID | `QuickCreate.tsx:248-252` | auto-generated UUIDv7 (no UI) |

### 3.1 Total Web fields exposed (counted from the stores + subpanels)

Identity (incl. visual): 5
Time: 6 (span.start, span.end, whenMode, timeOfDayMode, timeOfDayStart,
timeOfDayEnd, referenceId, referenceLabel — 8 if we count the day-mode
"only start" branch)
Duration: 2
Windows: 5 (per row × unbounded; times N)
Recurring / SourceGeneration: 6 (repeatMode, weekdayMask, intervalValue,
intervalUnit, endDate, condition)
Source Authoring: 9 (offsetMin, excludedDates, preferredDurationMinMax×2,
splitPolicy.kind, splitPolicy.min/maxSegmentMs, splitPolicy.maxSegments,
priority, include, anchorMode)
Flow: 8 (observes, when, candidateWhen, minimumGapMs, cycle,
resetOnInterrupt, steps[].waitBeforeMs, steps[].emitDurationMs)
Relation: 13 (per row × unbounded)
References: 3 (per row × unbounded)
Completion: 5+ (root compound, timeRequirements 4-6 per row, tasks 5 per row)
Placement/Nesting Rules: 7 per row × unbounded
Metrics/Decisions: Web does not surface; defer to JSON editors
Meta: 2 (ownerSubjectId, memo)

**Total Web fields exposed (recurring rows × N, windows × N, relations × N,
etc): ~80 leaf fields**.

### 3.2 Web subpanel list

Identity, Schedule (when), Duration, Recurring, Source Window / Split,
Condition-Driven Flow, Source Relations, Placement Rules, References,
Completion, Tasks, Meta. Twelve subpanels, each opens over the base panel.

### 3.3 Web widgets (per file)

- `DatePickerInput` / `DateTimePicker` / `TimeInput` (Mantine) for date and
  time-of-day inputs.
- `NumberInput` (with min/max/step) for numeric fields.
- `Select` / `MultiSelect` / `TagsInput` for enumerated fields.
- `Switch` for boolean flags.
- `Chip` rows for weekday masks.
- `ConditionEditor` (recurring tree) for boolean condition trees.
- `SegmentedControl` for short-choice switches.

## 4. Gap matrix

Conventions:
- ✅ = both clients have an input widget that maps to the v1 field
- ⚠️ = Web has the input, Android has it as text input / chip / hidden
- ❌ = Web has the input, Android has no input, server gets a default
- ❔ = unclear — see note

| v1 field | Web | Android Dashboard | Android Mobile | Widget gap on Android |
|----------|-----|-------------------|----------------|-----------------------|
| `tile.title` | ✅ | ✅ | ✅ | none |
| `tile.description` | ✅ | ❌ (`next_action` reused for memo) | ❌ (no UI field) | missing in both |
| `tile.color` | ✅ (popover) | ❌ (hardcoded `#3b82f6`) | ❌ (hardcoded `#3b82f6`) | missing |
| `tile.icon` | ✅ (popover) | ❌ (hardcoded `"check-circle"`) | ❌ (hardcoded `"check-circle"`) | missing |
| `tile.external_id` | ✅ (auto) | ❌ (always null) | ❌ (always null) | server-generated |
| `plan.role` | ✅ (Segmented Executable/Label) | ⚠️ (work/label) | ✅ (Switch) | renamed |
| `plan.references` | ✅ (full editor) | ❌ | ⚠️ (id, target, pick, interval number row) | partial |
| `plan.completion.root` | ✅ (Condition tree) | ❌ (defaults to `manual` done_rule) | ✅ (Condition tree) | missing on dashboard |
| `plan.completion.timeRequirements[]` | ✅ (per-row scope/source/aggregate/min/max) | ❌ | ⚠️ (per-row minMs/maxMs only) | partial |
| `plan.completion.tasks[]` | ✅ (full editor) | ❌ | ✅ (full editor) | missing on dashboard |
| `plan.planning.placement_rules` | ✅ (PlacementRulesPanel) | ❌ | ⚠️ (raw JSON editor) | poor UX |
| `plan.planning.nesting_rules` | ✅ (PlacementRulesPanel) | ❌ | ⚠️ (raw JSON editor) | poor UX |
| `plan.planning.flows` | ✅ (FlowSequencePanel) | ❌ | ⚠️ (raw JSON editor) | poor UX |
| `plan.metrics` | ❌ (no UI yet) | ❌ | ⚠️ (raw JSON editor) | none in either |
| `plan.decisions` | ❌ (no UI yet) | ❌ | ⚠️ (raw JSON editor) | none in either |
| `flows[]` (ScheduleFlow) | ❌ (no UI yet) | ❌ | ⚠️ (raw JSON editor) | none in either |
| `relations[]` | ✅ (RelationPanel) | ❌ | ❌ (no UI) | missing entirely |
| `schedule.required_duration_ms` | ✅ (Duration subpanel) | ✅ (workHours/Minutes) | ✅ (DurationPanel) | none |
| `schedule.priority` | ✅ (SourceWindowPanel) | ❌ (server default) | ❌ (server default) | hidden |
| `schedule.generation.kind` | ✅ (repeatMode) | ⚠️ (via objectiveMode) | ✅ (repeatMode) | none on mobile |
| `schedule.generation.at` | ❔ (depends on repeatMode) | ❔ | ❔ | unclear |
| `schedule.generation.starts_at` | ✅ (EndDate -> `ends_at`) | ❌ (only `validFrom`) | ⚠️ (recurring life.dateRange) | partial |
| `schedule.generation.interval_ms` | ✅ (built from intervalValue/Unit) | ❌ (built from frequency × 24*60) | ⚠️ (lifecycleFilter) | partial |
| `schedule.generation.ends_at` | ✅ (EndDate) | ⚠️ (validToDate) | ✅ (endDate) | partial |
| `schedule.generation.weekday_mask` | ✅ (chip row) | ⚠️ (CSV text 0..6) | ✅ (chip row) | poor UX on dashboard |
| `schedule.generation.date_range_start` | ❌ (no UI) | ❌ | ⚠️ (recurring life.dateRange) | unclear |
| `schedule.generation.date_range_end` | ❌ (no UI) | ❌ | ⚠️ (recurring life.dateRange) | unclear |
| `schedule.generation.excluded_dates` | ✅ (TagsInput) | ❌ | ❌ (no UI) | missing |
| `schedule.generation.offset_min` | ✅ (NumberInput) | ❌ (server default) | ❌ (server default) | hidden |
| `schedule.window.start_offset_ms` | ✅ (TimeOfDay) | ⚠️ (HH:MM text) | ✅ (TimePicker) | poor UX on dashboard |
| `schedule.window.end_offset_ms` | ✅ (TimeOfDay) | ⚠️ (HH:MM text) | ✅ (TimePicker) | poor UX on dashboard |
| `schedule.split_policy.kind` | ✅ (Select) | ⚠️ (breakSplitsWork) | ❌ (no UI) | partial |
| `schedule.split_policy.min_segment_ms` | ✅ (NumberInput) | ❌ | ❌ (no UI) | missing |
| `schedule.split_policy.max_segment_ms` | ✅ (NumberInput) | ❌ | ❌ (no UI) | missing |
| `schedule.split_policy.max_segments` | ✅ (NumberInput) | ❌ | ❌ (no UI) | missing |
| `schedule.preferred_duration_min/max` | ✅ (NumberInputs) | ❌ | ❌ (no UI) | missing |
| `schedule.include` / `anchorMode` | ✅ (popover) | ❌ | ❌ (no UI) | missing |
| `horizon` | ❌ (server default) | ❌ (server default) | ❌ (server default) | none |
| `interruption.interrupt_penalty` | ❌ (no UI) | ❌ (hardcoded 3) | ❌ (no UI) | hidden |
| `interruption.resume_penalty` | ❌ (no UI) | ❌ (hardcoded 3) | ❌ (no UI) | hidden |
| `interruption.break_splits_work` | ❌ (no UI) | ✅ (segmented) | ❌ (no UI) | dashboard only |
| `interruption.external_interrupt_only` | ❌ (no UI) | ❌ (hardcoded false) | ❌ (no UI) | hidden |
| `automation.prompt_on_start` | ❌ (no UI) | ❌ (hardcoded false) | ❌ (no UI) | hidden |
| `automation.prompt_on_end` | ❌ (no UI) | ✅ (hardcoded true) | ❌ (no UI) | hidden |
| `automation.auto_start_allowed` | ❌ (no UI) | ❌ (hardcoded false) | ❌ (no UI) | hidden |
| `automation.auto_end_allowed` | ❌ (no UI) | ❌ (hardcoded false) | ❌ (no UI) | hidden |
| `annotation.semantic_role` | ✅ (via identity.kind) | ✅ (work/label) | ⚠️ (via plan.role) | inconsistent |
| `annotation.labels` (project + tags) | ✅ (Meta) | ✅ (project + tags) | ✅ (project + tags) | none |
| `annotation.timed_labels` | ❌ (no UI) | ❌ | ❌ (no UI) | missing |
| `windows[]` | ✅ (SchedulePanel WindowRow) | ❌ | ✅ (WindowRow) | dashboard missing |
| `next_action` / `done_definition` | ⚠️ (root tile test) | ✅ (memo + derived) | ⚠️ (only memo) | inconsistent |

### 4.1 Summary counts

| Bucket | v1 schema fields | Web UI fields | Android Dashboard | Android Mobile |
|--------|------------------|---------------|-------------------|----------------|
| Identity | 5 | 5 | 1 | 1 |
| Time | 8 | 8 | 6 | 8 |
| Duration | 2 | 2 | 1 | 2 |
| Source Schedule | 5 | 5 | 0 | 1 |
| Generation | 10 | 9 | 5 | 5 |
| Window | 2 | 2 | 2 | 2 |
| Split policy | 4 | 4 | 1 | 0 |
| Preferred duration | 2 | 2 | 0 | 0 |
| Include / Anchor | 2 | 2 | 0 | 0 |
| Horizon | 2 | 0 | 0 | 0 |
| Plan / Completion | 6 | 5 | 0 | 6 |
| References | 3 | 3 | 0 | 1 |
| Metrics / Decisions / Flows | 18 | 0 | 0 | 0 |
| Relations | 13 | 13 | 0 | 0 |
| Placement / Nesting rules | 14 | 14 | 0 | 0 |
| Windows (per row) | 5 | 5 | 0 | 5 |
| Annotation | 3 | 3 | 3 | 2 |
| Interruption | 4 | 0 | 1 | 0 |
| Automation | 4 | 0 | 0 | 0 |
| **Total** | **~112** | **~80** | **~20** | **~33** |

(Duplicate windows/relations/rules are counted once per "kind" of row.)

## 5. Priority recommendations

Order suggestions based on user pain (the user explicitly cited the input
volume gap; secondary signals are the time-input widget claim):

### Priority 1 — Surface the canonical v1 path

The Android composer still POSTs to `POST /v1/tiles` + `POST /v1/tiles/{id}/plan`
(v0 command path; see `QuickCreateSubmission.kt:92-108`). The v1
`POST /v1/source-tiles` path (`V1ApiClient.createSourceTile` already exists
at `V1ApiClient.kt:220-226`) is unused. Migrating the mobile submission path
to the v1 typed envelope is the prerequisite for any field-level parity work
because otherwise the `relations`, `flows`, `frame_rules`, `rules`, `metrics`,
`decisions`, `placement_rules`, `nesting_rules`, `time_requirements`,
`tasks`, `references`, `windows`, `split_policy`, `priority`,
`weekday_mask`, `excluded_dates`, `offset_min` slices would all round-trip
through `SetPlanPayload` whose server-side reaction is unverified.

### Priority 2 — Widget parity in the Mobile QuickCreate

While the move is in progress, add the missing fields as plain Compose inputs
(no schema port required). Highest-impact picks:

1. `tile.description` (TextField). Currently never written.
2. `tile.color` / `tile.icon` (ColorPicker + IconPicker). Currently hardcoded.
3. `schedule.required_duration_ms` (work duration) — already in `DurationPanel`,
   but no min/max — only a single NumberInput.
4. `schedule.priority` (NumberInput).
5. `schedule.generation.offset_min` (NumberInput; sunset for v1 UTC).
6. `schedule.generation.excluded_dates` (TagsInput).
7. `schedule.split_policy.kind`, `min/max_segment_ms`, `max_segments`
   (Select + NumberInputs).
8. `schedule.preferred_duration_min/max` (NumberInputs).
9. `schedule.include` / `anchorMode` (SegmentedControl).
10. `interruption.break_splits_work` (Switch).
11. `windows[].rules` (weekdayMask, timeStart, timeEnd, holidayKind)
    subeditor.

### Priority 3 — Widget upgrades

The user reports time inputs as text-only. There are two text-only inputs in
the dashboard composer:

- `OutlinedTextField` for `recurrenceStartTime` / `recurrenceEndTime`
  (`QuickCreateSheet.kt:395-409`). They expect `HH:MM` strings; validation in
  `parseTimeToMinutes` is silent. Wire it to the existing `TimePicker`
  (already used by `DateTimeField`) — same shape `m3-allow:` permission.
- `OutlinedTextField` for `recurrenceWeekdaysCsv` (`QuickCreateSheet.kt:364-370`).
  Replace with a weekday chip row (the Mobile QuickCreate already has
  `LocalWeekdayPicker` in `QuickCreateSubpanels.kt:1207-1229`).

### Priority 4 — Tile edit / detail

The dashboard composer is the only way to mutate tile metadata today, and
`DashboardViewModel.updateTileTitle` only patches `title`
(`DashboardViewModel.kt:851-866`). A v1 edit page that uses
`PUT /v1/source-tiles/{id}` (`V1ApiClient.updateSourceTile` at line 228) is
missing. The Web has an edit mode built into the same `QuickCreate` sheet
(`QuickCreate.tsx:113-198`) — copy that flow into a new
`SourceTileEditSheetMobile` once priority 1 is done.

## 6. Cross-repo pointers

Web (tastile-web) — primary creator:

- `src/features/create-tile/ui/QuickCreate.tsx` lines 90-1248 (main panel)
- `src/features/create-tile/ui/SchedulePanel.tsx` lines 1-590 (when +
  time-of-day + windows)
- `src/features/create-tile/ui/SourceGenerationPanel.tsx` lines 1-388
  (repeatMode tabs + weekdayMask + interval + endDate)
- `src/features/create-tile/ui/SourceWindowPanel.tsx` lines 1-125 (priority +
  offsetMin + excludedDates + splitPolicy)
- `src/features/create-tile/ui/FlowSequencePanel.tsx` lines 1-100 (multi-row
  flow editor)
- `src/features/create-tile/ui/RelationPanel.tsx` lines 1-110 (relations
  editor)
- `src/features/create-tile/ui/PlacementRulesPanel.tsx` (placement rules)
- `src/features/create-tile/ui/CompletionSubPanel.tsx` lines 1-100 (condition
  + timeRequirements + tasks)
- `src/features/create-tile/ui/TaskDetailSubPanel.tsx` (task-level editor)
- `src/features/create-tile/ui/ReferencesSubPanel.tsx` (references)
- `src/features/create-tile/ui/MetaSubPanel.tsx` lines 1-65 (memo + project)
- `src/shared/stores/quick-create-store.ts` lines 66-185 (slice types)
- `src/shared/api/v1/submit.ts` (submission path, v1 source-tile write)

tastile-core (v1 contract):

- `crates-v1/api/src/openapi.rs` lines 700-1412 (canonical schema)
- `crates-v1/api/src/handlers/source_tiles.rs` (CreateSourceTile handler)
- `crates-v1/domain/src/command.rs` (SourceTile domain types)
- `crates-v1/storage/src/source_tile_repo.rs` (SourceTile writes)
- `docs/plans/2026-08-01-api-usecase-test-results.md` (discovered payload
  shape with concrete example)

Android (tastile-android) — current state:

- `app/src/main/java/app/tastile/android/ui/dashboard/QuickCreateSheet.kt`
  (lines 1-813, dashboard composer)
- `app/src/main/java/app/tastile/android/ui/dashboard/DashboardViewModel.kt`
  lines 77-1068 (CreateTileDraft + buildCreatePayload)
- `app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateSheetMobile.kt`
  (mobile composer shell)
- `app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateState.kt`
  (draft shape)
- `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt`
  (base composition)
- `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubpanels.kt`
  (TimePanel, DurationPanel, ReferencesPanel, CompletionPanel, MetaPanel,
  JsonEditor, LocalWeekdayPicker)
- `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubmission.kt`
  lines 78-162 (submit path)
- `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubmissionViewModel.kt`
- `app/src/main/java/app/tastile/android/data/api/V1ApiClient.kt` lines 220-238
  (createSourceTile / updateSourceTile exist but unused)
- `app/src/main/java/app/tastile/android/data/api/V1CommandPayloads.kt`
  lines 23-156 (v1 typed payload shapes)
- `app/src/main/java/app/tastile/android/data/command/V1CommandDispatcher.kt`
  lines 85-115 (dispatchTileCreate uses `POST /v1/tiles` directly)
- `app/openapi/v1.json` lines 1378-4343 (in-repo OpenAPI mirror)
- `docs/v1-openapi-coverage.md` (gap between v1 endpoints and Android
  coverage)
- `app/src/main/java/app/tastile/android/ui/mobile/components/picker/TimePickerSheet.kt`
  (existing m3 TimePicker — reuse for dashboard upgrade)

## 7. Unclear / needs integration test

- `schedule.generation.at` semantics on Recurring (k=1) per `openapi.rs:728`
  "0=OneTime, 1=Recurring, 2=DemandDriven" — the field is described but the
  registry contract was not exhaustively read. The Web UI only emits `at` on
  OneTime mode; Android never emits it. Recommend testing with a Recurring
  tile and a `kind=0` (OneTime) tile to confirm the wire shape.
- `plan.completion.timeRequirements[]` server reaction when posted via
  `SetPlanPayload` (`POST /v1/tiles/{id}/plan`). It is not in the v0 openapi
  spec and the Android `snakeCase` helper (`QuickCreateSubmission.kt:158-162`)
  may not match the v1 snake_case shape (e.g. `timeRequirements` →
  `time_requirements` looks fine, but `observation` nested fields remain
  unverified).
- The `lifecycleFilter` field on `recurring.frameRules[].active` is never
  shown in the UI (Web or Android). Need to confirm what the server expects.
- `external_interrupt_only`, `auto_start_allowed`, `auto_end_allowed`,
  `prompt_on_start` are accepted by the v1 schema but never surfaced. This
  is consistent across Web and Android, so it is a server-side protocol gap,
  not a parity gap.
- The Horizon field is not displayed in any UI. The web store leaves it
  empty; the server may auto-derive it. Verification test recommended.
