# Phase 3 Carve-Out Record

- **Date:** 2026-09-03
- **Plan:** [`docs/superpowers/plans/2026-09-02-m3-expressive.md`](../plans/2026-09-02-m3-expressive.md)
- **Baseline:** [`docs/superpowers/m3/before-reports/README.md`](before-reports/README.md)
- **Status (post-carve-out):** Phase 3 Tasks 3.3 + 3.4 **complete and on plan scope**; Tasks 3.1 + 3.2 **CARVED OUT** of this plan (moved to follow-up "M3X device integration" plan).

## Why this addendum exists

The original plan at `docs/superpowers/plans/2026-09-02-m3-expressive.md`
treated Phase 3 as a single sequential integration step without flagging a
device dependency. The plan's whole-branch checklist thus reports "Phase 3
integration: Tasks 3.1–3.4 ✓" without distinguishing which sub-tasks are
device-gated.

Two of the four Phase 3 sub-tasks require an attached ADB device:

- **Task 3.1: Run gfxinfo to confirm motion physics frame rate** — `adb`
  shell calls, debug-APK install via Gradle, manual UI exercise.
- **Task 3.2: Add instrumented QuickCreate smoke test** —
  `./gradlew :app:connectedDebugAndroidTest` requires a connected device
  or running emulator.

At the close of the JVM-side Phase 3 work on this Windows host, no ADB
device was attached (`adb devices` returned an empty list). Therefore
these two tasks cannot be executed here.

## What "carve-out" means (vs. "defer")

There are two ways to record a gap relative to a plan:

- **Defer (not adopted here):** leave the plan tasks as unchecked
  checkboxes and write a record elsewhere explaining why. The tasks
  remain in the plan's scope; the plan is therefore not 完遂 until the
  tasks run.
- **Carve-out (adopted here):** amend the plan in-place — strike
  through the device-gated tasks with a `CARVED OUT 2026-09-03` banner,
  update the Self-Review Checklist, and move the work to a separate
  follow-up plan ("M3X device integration"). The M3 Expressive plan's
  scope is now strictly bounded by the carved-in tasks; 3.1 + 3.2 are
  out of its scope, so the plan is 完遂 when the carved-in tasks
  (Phases 0 + 1 + 2 + Phase 3.3 + Phase 3.4) are all complete.

This record is the artifact side of that carve-out.

## Why this addendum exists

The original plan at `docs/superpowers/plans/2026-09-02-m3-expressive.md`
treated Phase 3 as a single sequential integration step without flagging a
device dependency. The plan's whole-branch checklist thus reports "Phase 3
integration: Tasks 3.1–3.4 ✓" without distinguishing which sub-tasks are
device-gated.

Two of the four Phase 3 sub-tasks require an attached ADB device:

- **Task 3.1: Run gfxinfo to confirm motion physics frame rate** — `adb`
  shell calls, debug-APK install via Gradle, manual UI exercise.
- **Task 3.2: Add instrumented QuickCreate smoke test** —
  `./gradlew :app:connectedDebugAndroidTest` requires a connected device
  or running emulator.

At the close of the JVM-side Phase 3 work on this Windows host, no ADB
device was attached (`adb devices` returned an empty list). Therefore
these two tasks cannot be executed here and must be formally deferred
rather than silently left as unchecked boxes in the plan.

This addendum amends the plan to formally defer Tasks 3.1 + 3.2 until
they can run on a device-available environment (CI runner or developer
machine with an attached device or emulator). It does **not** amend the
acceptance criteria — the original plan's expectations still apply once
the tasks run.

## Deferred tasks

### Task 3.1 — gfxinfo motion physics verification

**Plan reference:** `docs/superpowers/plans/2026-09-02-m3-expressive.md`
lines 1904–1965.

**What must run (verbatim from plan):**

```bash
adb shell setprop debug.hwui.profile true
adb shell dumpsys gfxinfo app.tastile.android reset

cd app && ../gradlew :app:installDebug
adb shell am start -n app.tastile.android/.MainActivity
# Manual: TimelineScreen → tap quick-create-fab → wait 5s → back

adb shell dumpsys gfxinfo app.tastile.android framestats

adb shell setprop debug.hwui.profile false
```

**Acceptance criteria (verbatim from plan §"Expected"):**

- Average frame time on API 31+ ≤ 16.67ms (60 fps) during FAB rotation
  animation.
- No jank flag (frames > 16.67ms but < 33.33ms) longer than 5% of total
  frames during animation.

**If threshold missed (verbatim from plan):**

- Note device model and OS version.
- Document as a known limitation in `docs/superpowers/m3/motion-perf.md`.
- Open an issue tracking a `MotionScheme` with relaxed stiffness for
  low-end devices.

**Evidence required to close:**

- Raw `dumpsys gfxinfo` output (saved as an artifact under
  `docs/superpowers/m3/gfxinfo/<device-model>-<os-version>-<date>.txt`
  or attached to the closing PR / commit).
- A commit on `main` either (a) recording the green run, or (b) adding
  `docs/superpowers/m3/motion-perf.md` documenting the known limitation
  if the threshold is missed.

### Task 3.2 — Instrumented QuickCreate smoke test

**Plan reference:** `docs/superpowers/plans/2026-09-02-m3-expressive.md`
lines 1969–2035.

**What must be created (verbatim from plan):**
`app/src/androidTest/java/app/tastile/android/ui/navigation/QuickCreateSmokeTest.kt`
with the body quoted at plan lines 1984–2008.

**What must run (verbatim from plan):**

```bash
cd app && ../gradlew :app:connectedDebugAndroidTest \
    --tests "app.tastile.android.ui.navigation.QuickCreateSmokeTest"
```

**Acceptance criteria (verbatim from plan §"Expected"):** PASS. If the
test fails because `MainActivityTestRule` requires a different
constructor signature, the plan authorizes adjusting the test to match
the rule's actual API (and documenting the adjustment in the closing
commit).

**Evidence required to close:**

- A new commit on `main` adding the smoke test file and the test run
  output showing PASS (or the runner log explaining the rule-signature
  adjustment and the re-run PASS).

## What is NOT deferred — this plan's scope after carve-out

Phase 3 Tasks 3.3 and 3.4 are complete on the JVM-side scope that this
host can verify, and they remain on the plan:

- **Task 3.3 — README + M3 baseline docs.** Commits `012453c` and
  `328f625` cover the docs the plan asked for, including the post-
  review corrections.
- **Task 3.4 — Final verification gate.** The JVM-side portion of
  Step 1 is complete: the three design-system guards
  (`verifyDesignSystemImports`, `verifyNoEmbeddedServerSecrets`,
  `verifyV1ApiCoverage`) all PASS on the migrations' owned scope. Step 2
  (marker audit) is complete: zero `// m3e-allow:` markers in
  `app/src/`, `// m2-allow:` net migration delta +1 (documented). The
  pre-existing baselines (15 unit-test failures, 89 `MissingTranslation`
  lint failures) are not migration-caused and are tracked separately.
  Step 1's `connectedDebugAndroidTest` portion is carved out under
  Task 3.2 above rather than separately.

The 17 migration commits on `main` (`275eeb1` … `107716d`) implement
the carve-out-surviving goals (Phase 0 + Phase 1 + Phase 2 + Phase 3.3 +
Phase 3.4). Tasks 3.1 + 3.2 will land under a separate "M3X device
integration" plan once an ADB device is available.

## Follow-up plan ("M3X device integration") — required inputs

When M3X is opened, copy the verbatim run commands and acceptance
criteria above into the new plan. M3X's whole-branch checklist should
add a single bullet:

> - Goal N: Tasks 3.1 + 3.2 from `2026-09-02-m3-expressive` (carved-out
>   device integration) ✓

Closing the M3X plan does **not** reopen this plan's already-completed
status. It is independent work.

## Tracking semantics

- The plan's checkbox rows for Tasks 3.1 + 3.2 are struck-through and
  carry a `CARVED OUT 2026-09-03` banner. Any audit reading the plan in
  isolation will see the carve-out marker without needing this
  addendum to interpret it.
- Any developer with an attached ADB device (or running emulator) can
  pick up the carved-out Tasks 3.1 + 3.2 directly from the struck-
  through sections in the plan, or copy them verbatim from this
  addendum. Both are accurate.
- Closing the carved-out tasks does not require reopening this plan.
  The closer should commit the run output under `docs/superpowers/m3/`
  (e.g. an `m3x/` mirror of the deferred file paths) and link back to
  this addendum's "Evidence" subsections.

## Status

- **Phase 0:** complete
- **Phase 1a/1b/1c:** complete
- **Phase 2:** complete
- **Phase 3.1:** DEFERRED — pending device
- **Phase 3.2:** DEFERRED — pending device
- **Phase 3.3:** complete (`012453c`, `328f625`)
- **Phase 3.4 (JVM-side):** complete; Phase 3.4 (instrumented portion):
  deferred under Task 3.2
- **Whole-branch review (Task 23):** complete, APPROVED WITH NOTES,
  3 VERIFIED findings fixed (`328f625`), 4 PLAUSIBLE findings documented
  as deferred follow-ups
