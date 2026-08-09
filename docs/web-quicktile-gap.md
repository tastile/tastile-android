# Web QuickTileCreate vs. Mobile QuickCreate — Field Gap Analysis

**Document purpose:** Enumerates every field the web QuickTileCreate exposes and identifies which are missing or incomplete in the Android QuickCreate sheet.

**Source commit (web):** QuickCreate.tsx + all subpanels under src/features/create-tile/ui/ (QuickCreate store: src/shared/stores/quick-create-store.ts)

**Source commit (mobile):** c379d7a — QuickCreateSheetMobile + quickcreate package

---

## Section 1: Web QuickTileCreate Field Inventory

The web exposes seven subpanels, each 1:1 with a v1 spec chapter.

### §1 Identity (identity.*)

| Field | Type | Label / Notes |
|---|---|---|
| identity.title | string | Large 1.5rem TextInput, placeholder tile title |
| identity.kind | TileKind (0=Placement/1=Recurring/2=Execution) | Header SegmentedControl: Executable vs Label |
| identity.description | string or null | Not visually surfaced in base panel; stored |
| identity.externalId | string or null | Auto-generated uuidv7 on mount; stored |
| identity.visual.color | string (hex) | VisualEditor chip in BehaviorPreview |
| identity.visual.icon | string | VisualEditor chip in BehaviorPreview |
Subpanels: IntentSubPanel (routing hub), BehaviorPreview (visual preview)

---

### §2 Plan (plan.*)

#### Role

| Field | Type | Notes |
|---|---|---|
| plan.role | PlanRole (EXECUTABLE=0 / LABEL=1) | Header SegmentedControl |

#### References (plan.references[]) — ReferencesSubPanel

| Field | Type | Label |
|---|---|---|
| references[].id | string | Reference ID (free text) |
| references[].target.kind | int (0=Exact/1=Series/2=Filter) | Target kind FilterChip row |
| references[].target.referenceId | string or null | TileReferencePicker |
| references[].target.contextKind | int or null | — |
| references[].target.conditionId | string or null | — |
| references[].pick.kind | int (0=All/1=First/2=Last/3=Before/4=After) | Relation segmented |
| references[].pick.momentId | string (minutes) | Interval stepper |

#### Completion Logic (plan.completion) — CompletionSubPanel

| Field | Type | Label / Notes |
|---|---|---|
| plan.completion.root.kind | int (0=ALL/1=ANY/2=NOT/3=TERM) | Logic FilterChip row |
| plan.completion.root.children[] | ConditionNode[] | Nested AND/ANY/NOT groups |
| plan.completion.root.term | ConditionNode.term or null | Leaf condition |
| plan.completion.tasks[] | TaskDefinition[] | Task list in base + TaskDetailSubPanel |
| plan.completion.tasks[].id | string | — |
| plan.completion.tasks[].content.title | string | Task title inline in base |
| plan.completion.tasks[].content.note | string or null | TaskDetailSubPanel textarea |
| plan.completion.tasks[].show | ConditionNode or null | Not surfaced in base |
| plan.completion.tasks[].complete | ConditionNode | Not surfaced in mobile |
| plan.completion.tasks[].order[] | TaskOrderRule[] | TaskDetailSubPanel: targetTaskId, relation (BEFORE/AFTER) |
| plan.completion.timeRequirements[] | TimeRequirement[] | In CompletionSubPanel |
| plan.completion.timeRequirements[].id | string | — |
| plan.completion.timeRequirements[].required.minMs | long or null | Minutes stepper |
| plan.completion.timeRequirements[].required.maxMs | long or null | Minutes stepper |
| plan.completion.timeRequirements[].observation.scope | int | Scope selector (not surfaced) |
| plan.completion.timeRequirements[].preferred | DurationRange or null | Not surfaced in mobile |

#### Planning — Placement Rules (plan.planning.placementRules[]) — PlacementRulesPanel

| Field | Type | Notes |
|---|---|---|
| plan.planning.placementRules[].id | string | — |
| plan.planning.placementRules[].when | ConditionNode or null | ConditionEditor |
| plan.planning.placementRules[].rank | int | Rank stepper |
| plan.planning.placementRules[].effect.kind | int (0=Permit/1=Deny/2=Limit/3=Score/4=Record) | Effect Select |
| plan.planning.placementRules[].effect.scope | object | Not fully surfaced |
| plan.planning.placementRules[].effect.span | DurationRange or null | Span steppers (kind=2) |
| plan.planning.placementRules[].effect.score | int or null | Score stepper (kind=3) |
| plan.planning.placementRules[].effect.record | int or null | Record select (kind=4) |
#### Planning — Flow Sequences (plan.planning.flows[]) — FlowSequencePanel

| Field | Type | Notes |
|---|---|---|
| plan.planning.flows[].id | string | — |
| plan.planning.flows[].observes[] | string[] | MultiSelect: 7 event types |
| plan.planning.flows[].when | ConditionNode or null | ConditionEditor |
| plan.planning.flows[].candidateWhen | ConditionNode or null | ConditionEditor |
| plan.planning.flows[].minimumGapMs | long | Gap min stepper |
| plan.planning.flows[].rank | int | Rank stepper |
| plan.planning.flows[].cycle | bool | Cycle switch |
| plan.planning.flows[].resetOnInterrupt | bool | Reset switch |
| plan.planning.flows[].steps[] | Step[] | Steps with waitBeforeMs + emitDurationMs |
| plan.planning.nestingRules[] | NestingRule[] | NOT in UI |
| plan.planning.metrics[] | Metric[] | NOT in UI |
| plan.planning.decisions[] | Decision[] | NOT in UI |

#### Source Relations (source.relations[]) — RelationPanel

| Field | Type | Notes |
|---|---|---|
| source.relations[].id | string | — |
| source.relations[].referencedSourceTileId | string | Searchable Select |
| source.relations[].kind | int (0=Inside/1=Before/2=After/3=StartsAt/4=EndsAt/5=SameSpan/6=SameDuration) | 7-option Select |
| source.relations[].point | int | — |
| source.relations[].offsetMs | long | Offset stepper |
| source.relations[].ordering | object | Primary/point/direction |
| source.relations[].durationKind | subject or reference or fixed | 3-option Select |
| source.relations[].fixedDurationMs | long or null | Fixed duration stepper |
| source.relations[].splitPolicy | object | Kind + min/max segment |
| source.relations[].correlationScope | int | Not surfaced |
| source.relations[].lifecycleFilter | int | Not surfaced |
| source.relations[].eligibleThroughRevision | int | Not surfaced |
| source.relations[].summaryPriority | int | Priority stepper |

---

### §3 Time (time.*) — SchedulePanel

| Field | Type | Label / Notes |
|---|---|---|
| time.whenMode | none or day or range or reference | NullCard switch + SegmentedControl |
| time.span.start | string (ISO date) | DatePicker |
| time.span.end | string (ISO date) | DatePicker (range only) |
| time.referenceId | string or null | TileReferencePicker |
| time.referenceLabel | string | Free text |
| time.timeOfDayMode | all-day or range or unspecified | Choice tabs |
| time.timeOfDayStart | string (HH:mm) | TimeInput |
| time.timeOfDayEnd | string (HH:mm) | TimeInput |
| time.durationMinMax.minMs | long or null | DurationSubPanel stepper |
| time.durationMinMax.maxMs | long or null | DurationSubPanel stepper |
---

### §4 Windows (windows[]) — SchedulePanel

| Field | Type | Notes |
|---|---|---|
| windows[].id | string | — |
| windows[].owner | string (self) | — |
| windows[].kind | int (0=Calendar/1=LabelSpan/2=ParentSpan/3=Gap) | Kind FilterChip |
| windows[].bounds.start | string (ISO datetime) | DateTimePicker |
| windows[].bounds.end | string (ISO datetime) | DateTimePicker |
| windows[].referenceId | string or null | TileReferencePicker (kinds 1-3) |
| windows[].rules[] | WindowRule[] | Per-window weekday/time rules |

---

### §5 Recurring (recurring.*) — SourceGenerationPanel

| Field | Type | Notes |
|---|---|---|
| recurring.repeatMode | once or daily or weekly or interval or condition | 5-way SegmentedControl |
| recurring.weekdayMask | int (bitmask 0-127) | Weekday chip row (weekly mode) |
| recurring.endDate | string (ISO datetime) | DatePickerInput with toggle |
| recurring.intervalValue | int | NumberInput (interval mode) |
| recurring.intervalUnit | min or hour or day | SegmentedControl (interval mode) |
| recurring.condition | ConditionNode or null | DISABLED placeholder (E1b) |
| recurring.life.active.startDate | string | Life date range |
| recurring.life.active.endDate | string | Life date range |
| recurring.life.state | int | Not surfaced |
| recurring.frameRules[] | FrameRule[] | Not surfaced |
| recurring.rules[] | RecurringRule[] | Not surfaced |

---

### §6 Source Schedule (source.* + schedule.* on wire) — SourceWindowPanel + mobile SchedulePanel

| Field | Type | Notes |
|---|---|---|
| source.priority | int (0-10) | Stepper |
| source.splitPolicy.kind | int (0=Unsplit/1=Split) | FilterChip + min/max/maxSegments |
| source.splitPolicy.minSegmentMs | long or null | Stepper (Split mode) |
| source.splitPolicy.maxSegmentMs | long or null | Stepper (Split mode) |
| source.splitPolicy.maxSegments | int or null | Stepper (Split mode) |
| source.offsetMin | int (UTC minutes, +/-840) | Stepper |
| source.excludedDates[] | string[] (ISO dates) | TagsInput |
| source.preferredDurationMinMax.minMs | long or null | NOT in mobile |
| source.preferredDurationMinMax.maxMs | long or null | NOT in mobile |
| source.include | INCLUDED or EXCLUDED | NOT in UI |
| source.anchorMode | FIXED or FLOATING | NOT in UI |

---

### §7 Meta (meta.*) — MetaSubPanel

| Field | Type | Notes |
|---|---|---|
| meta.ownerSubjectId | string or null | Project FilterChip catalog |
| meta.tags | string[] | Tag FilterChip suggestions + free-text add |
| meta.memo | string | Textarea (6 rows) |
---

## Section 2: Mobile QuickCreate Current Coverage

Mobile state data classes (QuickCreateState.kt):

- QuickCreateIdentity — kind, title, description, externalId, visual
- QuickCreatePlan — role, references, completion, planning, metrics, decisions
- QuickCreateTime — span, durationMinMax, whenMode, timeOfDayMode, timeOfDayStart, timeOfDayEnd, referenceId, referenceLabel
- QuickCreateWindow — id, owner, kind, bounds, rules, referenceId
- QuickCreateRecurring — life, frameRules, rules, repeatMode, weekdayMask, endDate
- QuickCreateSchedule — priority, splitPolicyKind, splitPolicyMinSegmentMs, splitPolicyMaxSegmentMs, splitPolicyMaxSegments, offsetMin, excludedDates
- QuickCreateMeta — ownerSubjectId, tags, memo

### Base Panel (QuickCreateBasePanel.kt)

| Field | Mobile Coverage | Notes |
|---|---|---|
| identity.title | YES — EditableTitleField with auto-focus | |
| identity.description | NO — not in UI | Stored in state but not surfaced |
| identity.visual.color | NO | Stored in state but not surfaced |
| identity.visual.icon | NO | Stored in state but not surfaced |
| plan.role | YES — Label/Executable Switch | |
| time.whenMode | YES — EssentialRow chip summary | |
| time.span.start/end | YES — EssentialRow chip summary | |
| time.durationMinMax | YES — EssentialRow chip summary | |
| plan.completion.root.kind | PARTIAL — shown as summary text (ALL/ANY/NOT) | Logic FilterChips in Completion subpanel |
| plan.completion.tasks[] | PARTIAL — read-only list of task titles | No add/edit per task |
| meta.ownerSubjectId | PARTIAL — shown as project FilterChip | Project picker only in Meta subpanel |
| meta.tags | PARTIAL — shown as tag FilterChips | Add/remove only in Meta subpanel |

### Subpanels

#### Time (TimePanel)

| Field | Coverage |
|---|---|
| time.whenMode | Full — None/Day/Range/Reference FilterChip row |
| time.span.start | Full — NativeDateField |
| time.span.end | Full — NativeDateField (range only) |
| time.referenceId | Full — ReferencePickerSheet |
| time.referenceLabel | Full — OutlinedTextField |
| time.timeOfDayMode | Full — AllDay/Range/Unspecified FilterChip row |
| time.timeOfDayStart | Full — TimePickerSheet |
| time.timeOfDayEnd | Full — TimePickerSheet |
| Quick ranges (morning/midday/night) | Full |
| windows[] | Full — add/remove/edit Window rows with kind/start/end/referenceId |

#### Duration (DurationPanel)

| Field | Coverage |
|---|---|
| No-duration FilterChip | Full |
| Duration stepper (sets both minMs=maxMs) | Full |
| Use for completion link | DISCONNECTED — onClick is empty |

#### References (ReferencesPanel)

| Field | Coverage |
|---|---|
| Add reference button | Full |
| Reference ID text field | Full |
| Target reference ID | Full |
| Target kind (Exact/Series/Filter) | Full FilterChip row |
| Relation (Touch/Inside/Overlap/Before/After) | Full FilterChip row |
| Interval stepper | Full |
| Remove reference | Full |

#### Completion (CompletionPanel)

| Field | Coverage |
|---|---|
| Logic ALL/ANY/NOT FilterChip row | Full |
| ConditionControls (TERM builder) | Full — 9 term types with full field editors |
| Add task/relation/metric chips | Full |
| Add time requirement button | Full |
| Time requirement minutes editor | Full |
| Remove time requirement | Full |
| Clear completion button | Full |
| Task list display | READ-ONLY — no add/edit per task |

#### Meta (MetaPanel)

| Field | Coverage |
|---|---|
| Project FilterChip catalog | Full — with No project option |
| Tag suggestions + selected tags | Full |
| Add tag free-text + button | Full |
| Memo OutlinedTextField | Full |
| Clear/Cancel/Apply buttons | Full |

#### Schedule (SchedulePanel) — added in recent commits

| Field | Coverage |
|---|---|
| Priority (0-10) | Full |
| Split policy kind (Unsplit/Split) | Full |
| Split min/max segment ms | Full |
| Split max segments | Full |
| Offset minutes UTC | Full |
| Excluded dates chips + DatePicker | Full |

#### Intent (IntentPanel)

| Field | Coverage |
|---|---|
| Routing buttons to Time/References/Schedule/Meta/Completion | Full — 5 buttons |

---

## Section 3: Gap Table (Time / Windows / Recurring / Source Schedule / Meta)


### §3 Time (time.*)

| Field | Web | Mobile | Gap Type | Suggested Fix |
|---|---|---|---|---|
| time.span.start | Full | Full | - | - |
| time.span.end | Full | Full | - | - |
| time.whenMode | Full | Full | - | - |
| time.timeOfDayMode | Full | Partial | UI Only | DurationPanel already reads start/end; expose mode toggle |
| time.timeOfDayStart | Full | Full | - | - |
| time.timeOfDayEnd | Full | Full | - | - |
| time.referenceId | Full | Missing | Absent | IntentPanel Reference button not wired; wire to ReferencesSubPanel |
| time.referenceLabel | Full | Missing | Absent | IntentPanel Reference button not wired |

**Gap summary §3:** 2 absent fields. DurationPanel exists; timeOfDayMode toggle needs UI surface.


---

### §4 Windows (windows[])

| Field | Web | Mobile | Gap Type | Suggested Fix |
|---|---|---|---|---|
| windows[].id | Full | Full | - | - |
| windows[].owner | Full | Full | - | - |
| windows[].kind | Full | Full | - | - |
| windows[].bounds.start | Full | Full | - | - |
| windows[].bounds.end | Full | Full | - | - |
| windows[].rules[].id | Full | Missing | Absent | WindowRule rows not modeled; add rule data class + editor |
| windows[].rules[].weekdayMask | Full | Missing | Absent | WindowRule weekday mask; add to WindowRule |
| windows[].rules[].timeStart | Full | Missing | Absent | WindowRule time window; add |
| windows[].rules[].timeEnd | Full | Missing | Absent | WindowRule time window; add |
| windows[].rules[].holidayKind | Full | Missing | Absent | WindowRule holiday filter; add |
| windows[].rules[].dateRange | Full | Missing | Absent | WindowRule date range; add |
| windows[].rules[].when | Full | Missing | Absent | WindowRule ConditionNode; add |
| windows[].referenceId | Full | Missing | Absent | IntentPanel Reference not wired |

**Gap summary §4:** 8 absent WindowRule fields. The QuickCreateWindow data class exists but WindowRule fields are all absent from the state store and UI.


---

### §5 Recurring (recurring.*)

| Field | Web | Mobile | Gap Type | Suggested Fix |
|---|---|---|---|---|
| recurring.life.active.startDate | Full | Full | - | - |
| recurring.life.active.endDate | Full | Full | - | - |
| recurring.life.state | Full | Full | - | - |
| recurring.repeatMode | Full | **Missing** | **Absent** | Add repeatMode to QuickCreateRecurring; TimePanel UI toggle |
| recurring.weekdayMask | Full | **Missing** | **Absent** | Add weekdayMask (Mon-Sun checkboxes) to TimePanel |
| recurring.endDate | Full | **Missing** | **Absent** | Add endDate to recurring section in TimePanel |
| recurring.frameRules | Full | Missing | Absent | frameRules[] not modeled; add QuickCreateFrameRule |
| recurring.rules | Full | **Missing** | **Absent** | recurring.rules[] not modeled; add QuickCreateRecurringRule |
| recurring.frameRules[].generator.kind | Full | Missing | Absent | Frame generator kind |
| recurring.frameRules[].generator.value | Full | Missing | Absent | Frame generator value JSON |
| recurring.frameRules[].active | Full | Missing | Absent | Frame active ConditionNode |
| recurring.rules[].when | Full | Missing | Absent | RecurringRule when ConditionNode |
| recurring.rules[].rank | Full | Missing | Absent | RecurringRule rank |
| recurring.rules[].outputs | Full | Missing | Absent | RecurringRule outputs JsonArray |

**Gap summary §5:** 11 absent fields. QuickCreateRecurring data class exists with repeatMode/weekdayMask/endDate defaults, but hydrateForEdit never populates it, no UI writes to it, and no submission payload references it. Top Priority 1.


---

### §6 Source Schedule (schedule.*)

| Field | Web | Mobile | Gap Type | Suggested Fix |
|---|---|---|---|---|
| schedule.priority | Full | Full | - | - |
| schedule.splitPolicyKind | Full | Full | - | - |
| schedule.splitPolicyMinSegmentMs | Full | Full | - | - |
| schedule.splitPolicyMaxSegmentMs | Full | Full | - | - |
| schedule.splitPolicyMaxSegments | Full | Full | - | - |
| schedule.offsetMin | Full | Full | - | - |
| schedule.excludedDates | Full | Full | - | - |

**Gap summary §6:** No gaps. Fully covered by SchedulePanel and QuickCreateSubmission.


---

### §7 Meta (meta.*)

| Field | Web | Mobile | Gap Type | Suggested Fix |
|---|---|---|---|---|
| meta.ownerSubjectId | Full | Full | - | - |
| meta.tags | Full | Partial | UI Hint | Tags stored but not surfaced in MetaPanel UI (label chip row missing) |
| meta.memo | Full | Full | - | - |

**Gap summary §7:** 1 partial field. Tags present in data model, stored on submit, but MetaPanel does not render a tag-input chip row.


---

## Section 4: Top 10 Priorities Ranked by Mobile UX Impact

Ranked by: (a) user-visible on first open, (b) blocks core recurring tile workflows, (c) enables cross-tile intelligence.

| Priority | Gap | Impact | Why |
|---|---|---|---|
| 1 | **Recurring repeatMode + weekday/end-date** | Critical | identity.kind = Recurring tiles cannot be created on mobile. repeatMode=Once/Daily/Weekly/Interval/Condition defaults to Once but is never changeable. Weekly weekday mask (Mon-Sun) is also absent. Blocks all recurring-schedule use cases. |
| 2 | **Placement rules (plan.placementRules)** | High | PlacementRulesPanel on web is the primary scheduling-authoring UX. Absent on mobile means users must use the web to configure any Permit/Deny/Limit/Score/Record placement effect. |
| 3 | **Identity enrichment (description, color, icon)** | High | Description, color picker, and icon picker exist in identity.visual but the base panel only shows a title. Users cannot set tile color or icon from mobile at all. |
| 4 | **Tags UI (meta.tags chip row)** | Medium | Tags are stored and submitted but no UI to add or remove them. Tags are the primary mobile organization primitive for Projects. |
| 5 | **Window rules (windows[].rules[])** | Medium | TimePanel allows picking a window but the rule-level fields (weekday mask, time-of-day window, holiday filter, date range, when-condition) are all absent. |
| 6 | **Reference picker wiring (time.referenceId)** | Medium | The IntentPanel shows a References button but it navigates nowhere. Wiring it to ReferencesSubPanel would give time-span tiles a way to anchor to another tile state. |
| 7 | **Task note + order editing (tasks[])** | Medium-Low | TaskDetailSubPanel exists in web for editing per-task note and order array. On mobile, the task tap handler is a no-op placeholder. Full task editing UX requires the subpanel. |
| 8 | **Flow sequences (plan.flows)** | Low | Flows are displayed read-only but editing rank, cycle, resetOnInterrupt, steps requires the full FlowSequencePanel. Not a v1 launch blocker. |
| 9 | **Frame rules (recurring.frameRules)** | Low | Generator-based frame rules are advanced v2 semantics. Lower priority than recurring.rules[]. |
| 10 | **ConditionNode builder (completion root)** | Low | CompletionSubPanel exists on web with full ALL/ANY/NOT/TERM builder. Mobile has ConditionControls composable with a read-only display. Full builder is a larger effort. |

| 4 | **Tags UI (meta.tags chip row)** | Addressed | Mobile MetaPanel renders suggestions and selected removable chips, plus free-text add. |
| 5 | **Window rules (windows[].rules[])** | Deferred | State shapes exist, but per-window rule editing and source-tile wire mapping remain. |
| 6 | **Reference picker wiring (time.referenceId)** | Addressed | TimePanel reference mode opens the reference picker and stores the selected ID. |
| 7 | **Task note + order editing (tasks[])** | Deferred | Task rows remain read-only in the base panel. |
| 8 | **Flow sequences (plan.flows)** | Deferred | Full FlowSequencePanel editing remains web-only. |
| 9 | **Frame rules (recurring.frameRules)** | Deferred | Advanced frame generator authoring remains unsurfaced. |
| 10 | **ConditionNode builder (completion root)** | Partial | Existing ConditionControls is reused for placement-rule `when`; full nested completion editing remains deferred. |



| Category | Web Fields | Mobile Fields | Gap |
|---|---|---|---|
| §1 Identity | 7 | 5 | 2 |
| §2 Plan -- Role | 1 | 1 | 0 |
| §2 Plan -- References | 8 | 0 | 8 |
| §2 Plan -- Completion | 10 | 5 | 5 |
| §2 Plan -- Placement Rules | 10 | 0 | 10 |
| §2 Plan -- Flow Sequences | 12 | 0 | 12 |
| §3 Time | 8 | 6 | 2 |
| §4 Windows | 9 | 2 | 7 |
| §5 Recurring | 13 | 3 | 10 |
| §6 Source Schedule | 7 | 7 | 0 |
| §7 Meta | 3 | 2 | 1 |
| **Total** | **~95** | **~40** | **~55** |

> Mobile Fields reflects fields that have a data class slot and are either fully rendered or partially rendered in UI. Many fields are present in QuickCreateState.kt data classes but not yet surfaced in any composable.

---

*End of gap analysis. Next step: implement Top 3 priorities in order (recurring repeatMode -> placement rules -> identity enrichment).*
