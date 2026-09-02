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

## Device-attempt follow-up (2026-09-03)

After the JVM-side work landed, a developer (XIG03, Android 15, API 35)
attached the device and re-attempted Tasks 3.1 + 3.2 directly. Both
tasks **remain unfinished** for new reasons, neither of which is in the
M3 plan's scope:

### Task 3.2 — new blocker: pre-existing `ExecutionAlarmRescheduleReceiver`
crash on `MY_PACKAGE_REPLACED`

```
java.lang.IllegalStateException: The component was not created.
    Check that you have added the HiltAndroidRule.
    at dagger.hilt.android.internal.testing.TestApplicationComponentManager.generatedComponent(...)
    at app.tastile.android.notifications.Hilt_ExecutionAlarmRescheduleReceiver.inject(...)
    at app.tastile.android.notifications.ExecutionAlarmRescheduleReceiver.onReceive(...)
```

The receiver at `app/src/main/AndroidManifest.xml` is registered for
`android.intent.action.MY_PACKAGE_REPLACED`. Every `adb install` of
`app.tastile.android` re-fires that broadcast. The receiver attempts to
call Hilt's generated `inject()` before the `HiltAndroidRule` has had a
chance to initialize in the **test** process — but the same broadcast is
delivered to the **production** process, where the Hilt graph is not
yet built. This crashes the production app process before the
instrumentation can hand off to the test runner, so
`adb shell am instrument … TastileTestRunner` reports
`Process crashed before executing the test(s)` regardless of which
single class is targeted.

Three workarounds were tried and all failed:

1. `./gradlew :app:connectedDebugAndroidTest` — fails the same way
   (install gate + receiver crash on install).
2. Pre-install both APKs with `adb install -r -d` then re-run — Gradle
   still re-installs the production APK on every run, retriggering the
   receiver crash.
3. `pm disable` the receiver — Android refuses:
   `Shell cannot change component state for ComponentInfo{...} to 2`.

**Required to unblock Task 3.2:** either (a) fix the receiver to defer
Hilt work until `Application.onCreate()` returns, or (b) move the
receiver off `MY_PACKAGE_REPLACED` so it does not fire during a
re-install. Both fixes are out of scope for the M3 migration.

### Task 3.1 — new blocker: auth gate blocks TimelineScreen

The plan's gfxinfo path assumes the user can navigate to TimelineScreen
on a fresh install and tap `quick-create-fab`. On this device, the cold
launch lands on `LoginScreen` (`サインイン`) — no signed-in session exists
because `pm clear` was issued to drain the broadcast queue. The
`MainActivityAuthGateTest` baseline confirms this is the expected
post-clear state. Without a real Cognito sign-in there is no path to
TimelineScreen, and therefore no path to the FAB that the gfxinfo run
is supposed to measure.

`docs/superpowers/m3/gfxinfo/XIG03-Android-15-2026-09-03.txt` was
captured anyway as a **toolchain-evidence artifact**, not a motion-
physics verdict:

- **Total frames rendered:** 5 (cold launch only)
- **Janky frames:** 4 (80.00%)
- **50th percentile:** 150ms, **90th percentile:** 1150ms

These numbers are from the cold-launch auth-gate render and do **not**
satisfy the plan's "average frame time on API 31+ ≤ 16.67ms during FAB
rotation animation" acceptance criterion. The file is committed so
that a follow-up runner with a signed-in session can re-capture
in-place.

**Required to unblock Task 3.1:** a signed-in session on the device
(real Cognito creds) so the run can reach TimelineScreen → FAB → 5s
animation. Out of scope for the M3 migration.

### What was committed as evidence

- `app/src/androidTest/java/app/tastile/android/ui/navigation/QuickCreateSmokeTest.kt`
  — new instrumented smoke test, adapted to the actual
  `MainActivityTestRule` API per plan §Step 3 authorization. Test
  does **not** currently run green; it is committed as the agreed
  shape so the follow-up unblock (receiver fix + signed-in session)
  can land it without further authoring work.
- `docs/superpowers/m3/gfxinfo/XIG03-Android-15-2026-09-03.txt` —
  cold-launch gfxinfo capture (LoginScreen render only).

### Re-deferred status

- **Phase 3.1:** re-DEFERRED — pending (a) signed-in session on a
  device or emulator, AND (b) the receiver-crash fix above. The
  `docs/superpowers/m3/gfxinfo/` directory now exists with the
  cold-launch capture to be overwritten once the unblock lands.
- **Phase 3.2:** re-DEFERRED — pending (a) the receiver-crash fix
  above, AND (b) a connected device. The new smoke test file is in
  place and ready to run green once (a) lands.
