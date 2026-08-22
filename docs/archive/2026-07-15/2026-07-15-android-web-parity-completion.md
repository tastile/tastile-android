# Android Web Parity Completion Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make Tastile Android present the same v1 API-backed tile creation workflow, project management, and calendar behavior as Tastile Web, while using Compose-native mobile sheets.

**Architecture:** Keep Android a thin client. The canonical wire contract remains tastile-core v1; the Web implementation is the UI/content reference. A typed Android creation draft and command gateway replace the legacy v0-shaped quick-create payload, while the existing Overlay layer hosts a root panel plus navigable subpanels.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, StateFlow, kotlinx.serialization, Ktor/HttpURLConnection v1 client, JVM/Robolectric tests.

---

### Task 1: Freeze the Web parity contract

**Files:**
- Modify: `docs/plans/2026-07-15-android-web-parity-completion.md`
- Test: `app/src/test/java/app/tastile/android/data/api/V1ApiClientTest.kt`

**Step 1: Write failing request-shape tests**
- Assert the Android client sends the same canonical v1 paths, JSON field names, command envelope, and idempotency behavior as the Web command helpers for create, plan, placement, recurring source tile, and timeline reads.

**Step 2: Run the focused tests**
Run: `./gradlew :app:testDebugUnitTest --tests '*V1ApiClientTest*'`
Expected: FAIL for currently unimplemented canonical creation operations.

**Step 3: Add only missing typed payloads/client methods**
- Add typed DTOs and methods for the exact endpoint shapes used by `tastile-web/src/lib/api/v1/tile-commands.ts` and `submit.ts`.
- Do not retain a second Android-only request format.

**Step 4: Re-run focused tests**
Expected: PASS.

**Step 5: Commit**
`git commit -m "feat(android): align tile creation API with web v1 contract"`

### Task 2: Replace the legacy quick-create draft with the Web field model

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardViewModel.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/QuickCreateSheet.kt`
- Create: `app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateState.kt`
- Test: `app/src/test/java/app/tastile/android/ui/mobile/sheets/QuickCreateStateTest.kt`

**Step 1: Write failing reducer/state tests**
- Cover the Web-equivalent fields: identity/visual, plan role/intent, time/span, duration range, recurrence windows/frame rule, references, completion conditions, project/tags/memo, and behavior.
- Cover subpanel back/dismiss transitions without losing edits.

**Step 2: Implement the smallest state holder**
- Use one Compose/StateFlow draft and an explicit panel enum: base, intent, time, duration, recurring, references, completion, meta, behavior.
- Preserve the existing mobile bottom-sheet container only; do not duplicate business rules in UI.

**Step 3: Run state tests**
Expected: PASS.

**Step 4: Commit**
`git commit -m "feat(android): add web-equivalent quick-create state and panels"`

### Task 3: Implement the Quick Create root and subpanels

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateSheetMobile.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/QuickCreateSheet.kt`
- Create: `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateBasePanel.kt`
- Create: `app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/QuickCreateSubpanels.kt`
- Test: `app/src/androidTest/java/app/tastile/android/ui/mobile/sheets/QuickCreateSheetTest.kt`

**Step 1: Write Compose tests**
- Verify the root panel lists the same fields in Web order.
- Verify each root row opens its matching subpanel and Back returns to root with retained values.
- Verify conditional recurrence controls only appear for recurring tiles and validation blocks invalid title/span/duration ranges.

**Step 2: Implement panels**
- Match Web content and ordering: Base; Intent; Time; Duration; Recurring; References; Completion; Meta; Behavior.
- Use Compose-native controls (bottom sheet, dropdown, picker, text fields) but mirror labels, options, conditions, and summaries.

**Step 3: Run UI tests and debug build**
Run: `./gradlew :app:connectedDebugAndroidTest :app:assembleDebug`
Expected: PASS (use the JVM suite if no emulator is available, and report that limitation).

**Step 4: Commit**
`git commit -m "feat(android): mirror web quick-create panel workflow"`

### Task 4: Submit and edit through the canonical command sequence

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardViewModel.kt`
- Modify: `app/src/main/java/app/tastile/android/data/command/V1CommandDispatcher.kt`
- Modify: `app/src/main/java/app/tastile/android/data/api/V1CommandPayloads.kt`
- Test: `app/src/test/java/app/tastile/android/data/command/V1CommandDispatcherTest.kt`

**Step 1: Write failing command-sequence tests**
- One-off: create tile → set plan → create/update placement span.
- Recurring: create recurring source tile/occurrences with the same frame/window semantics as Web.
- Edit: update tile, placement span, and plan; refresh projections after success.
- Assert failure is surfaced and no partial UI success is claimed.

**Step 2: Implement only the canonical sequence**
- Remove/reject the old `CreateTileDraft` v0-shaped payload path once all callers are migrated.
- Send idempotency keys and expected revisions exactly once per command.

**Step 3: Run focused tests**
Expected: PASS.

**Step 4: Commit**
`git commit -m "feat(android): submit quick-create through canonical v1 commands"`

### Task 5: Close projects and calendar parity gaps

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/panels/ProjectsViewModel.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/panels/projects/NewProjectForm.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/MonthCalendarScreen.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/TimelineScreen.kt`
- Test: `app/src/test/java/app/tastile/android/ui/mobile/panels/ProjectsViewModelTest.kt`
- Test: `app/src/test/java/app/tastile/android/ui/dashboard/MonthCalendarScreenTest.kt`

**Step 1: Write failing parity tests**
- Projects: parent selector, name/slug/color validation, create/select/delete, tree ordering, and owner filtering.
- Calendar: previous/next/today navigation, Day/Week/Month range request, all-day/timed block ordering, overflow, selected-day interaction.

**Step 2: Implement missing controls against existing v1 reads**
- Keep workspaces on `/v1/access/*` and calendar/timeline on `/v1/timeline`; never fall back to daemon/legacy endpoints.

**Step 3: Run tests**
Expected: PASS.

**Step 4: Commit**
`git commit -m "feat(android): complete web projects and calendar interactions"`

### Task 6: Final parity audit and repository verification

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-ja/strings.xml`
- Modify: `docs/plans/2026-07-15-android-web-parity-completion.md`

**Step 1: Add missing Web-mirrored quick-create strings**
- Use the established dots-to-underscores resource naming and supply Japanese/English counterparts.

**Step 2: Run all required validation**
Run: `./gradlew verify`
Expected: PASS.

**Step 3: Review the complete diff**
Run: `git diff --check && git status --short`
Expected: no whitespace errors; only planned Android files changed.

**Step 4: Commit**
`git commit -m "test(android): verify web parity workflows"`
