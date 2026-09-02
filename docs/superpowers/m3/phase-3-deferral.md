# Phase 3 Device Deferral Addendum

- **Date:** 2026-09-03
- **Plan:** [`docs/superpowers/plans/2026-09-02-m3-expressive.md`](../plans/2026-09-02-m3-expressive.md)
- **Baseline:** [`docs/superpowers/m3/before-reports/README.md`](before-reports/README.md)
- **Status:** Phase 3 Tasks 3.3 + 3.4 **complete**; Tasks 3.1 + 3.2 **deferred pending device**.

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

## What is NOT deferred

Phase 3 Tasks 3.3 and 3.4 are complete on the JVM-side scope that this
host can verify:

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
  Step 1's `connectedDebugAndroidTest` portion is deferred under Task
  3.2 above rather than separately.

The 16 migration commits on `main` (`275eeb1` … `328f625`) implement
all 7 plan goals; the only remaining work is the two device-gated
integration verifications above.

## Deferral semantics

- The plan's checkbox rows for Tasks 3.1 + 3.2 should be read as
  `[ ]` (deferred) rather than `[x]` (done) in any audit. The original
  plan is unamended; this addendum is the standing reference for
  deferral.
- Any developer with an attached ADB device (or running emulator) can
  execute the deferred tasks directly from the plan text without
  waiting on a planning-cycle update. The plan's "Run with" command
  blocks remain accurate.
- Closing Tasks 3.1 + 3.2 does **not** require a new plan revision;
  one commit per task (or one combined commit) is enough. The closer
  should update this addendum's "Evidence" subsections with the
  artifact path and link the commit SHA, then move Tasks 40 and 41
  from `pending` to `completed` in the task list.

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
