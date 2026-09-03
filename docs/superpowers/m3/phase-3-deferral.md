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
tasks **remain unfinished** for new reasons, none of which is in the
M3 plan's scope.

### Task 3.2 — new blocker: pre-existing `ExecutionAlarmRescheduleReceiver`
crash on ANY of its registered actions

```
java.lang.IllegalStateException: The component was not created.
    Check that you have added the HiltAndroidRule.
    at dagger.hilt.android.internal.testing.TestApplicationComponentManager.generatedComponent(...)
    at app.tastile.android.notifications.Hilt_ExecutionAlarmRescheduleReceiver.inject(...)
    at app.tastile.android.notifications.ExecutionAlarmRescheduleReceiver.onReceive(...)
```

The receiver at `app/src/main/AndroidManifest.xml` registers for five
actions: `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_CHANGED`,
`TIMEZONE_CHANGED`, and `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
It is annotated `@AndroidEntryPoint`, and its Hilt-generated
`Hilt_ExecutionAlarmRescheduleReceiver.onReceive()` calls `inject()`
before delegating to user code. Whenever any of the five broadcasts is
delivered to a process that has registered this receiver, the call
chain walks down through `BroadcastReceiverComponentManager.generatedComponent`
→ the process's `Application.generatedComponent`. In the test runner
that Application is `HiltTestApplication` (via `TastileTestRunner`); in
the production process it is `TastileApp`. Neither has its Hilt
component initialized at the instant the system delivers the
broadcast, so `Preconditions.checkState` throws and the process
crashes.

When `am instrument` is used to run `QuickCreateSmokeTest`, the test
runner needs to host `MainActivity` from the production package,
which forces the **production process** to start. logcat on the
2026-09-03 attempt shows:

```
08:30:23.898  ActivityManager: Start proc 4560:app.tastile.android/u0a494
    for added application app.tastile.android caller=android
08:30:26.030  BroadcastQueue: BOOT_COMPLETED_BROADCAST_COMPLETION_LATENCY_REPORTED
08:30:26.040  PID 4560: E AndroidRuntime: FATAL EXCEPTION: main
08:30:26.040  PID 4560: E AndroidRuntime: Process: app.tastile.android, PID: 4560
08:30:26.040  PID 4560: E AndroidRuntime: java.lang.RuntimeException:
    Unable to start receiver ExecutionAlarmRescheduleReceiver
```

`am instrument` then reports `Process crashed before executing the
test(s)` regardless of which single class is targeted.

**The MY_PACKAGE_REPLACED framing was incomplete.** The 2026-09-03
attempt showed the production process crashing on `BOOT_COMPLETED`,
which the system had been queuing since device boot — the receiver
crashes on ANY of its five subscribed actions, not just on package
replace.

**Workarounds attempted (all failed):**

1. `./gradlew :app:connectedDebugAndroidTest` — install gate + receiver
   crash on install.
2. Pre-install both APKs with `adb install -r -d` then re-run — Gradle
   still re-installs the production APK on every run.
3. `pm disable` the receiver — Android refuses:
   `Shell cannot change component state for ComponentInfo{...} to 2`.
4. `am instrument` directly — production process crashes on
   `BOOT_COMPLETED` when the system starts it to host MainActivity.
5. Add `app/src/androidTest/AndroidManifest.xml` with
   `tools:node="remove"` on the receiver (commit `a10aa20`) — verified
   by `aapt2 dump xmltree` on the built APK that the receiver is gone
   from the test APK's binary manifest, but the **production** APK
   still has it, so the production process crash is unaffected. The
   test-only manifest change is kept as a forward-looking partial
   unblock: once the production bug is fixed, the test APK will also
   be clean.

**Required to unblock Task 3.2:** a fix in production code, out of M3
scope. The minimal change is one of:

- Make the receiver tolerate Hilt-not-yet-initialized by removing
  `@AndroidEntryPoint` and resolving `ExecutionAlarmScheduler` via a
  non-Hilt singleton (or by deferring the work until the next
  foreground launch via `goAsync` + a static flag).
- Narrow the receiver's subscribed actions so it does not fire on
  `BOOT_COMPLETED` (which the system queues for every package at
  device boot and delivers to whatever process is started).

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
  shape so the follow-up unblock (production-receiver fix + signed-in
  session) can land it without further authoring work.
- `app/src/androidTest/AndroidManifest.xml` — test-only manifest that
  uses `tools:node="remove"` to drop the
  `ExecutionAlarmRescheduleReceiver` from the test APK's merged
  manifest. Verified by `aapt2 dump xmltree` on the built APK that the
  receiver is gone from the test APK's binary manifest. This is a
  forward-looking partial unblock; the production manifest is
  unchanged.
- `docs/superpowers/m3/gfxinfo/XIG03-Android-15-2026-09-03.txt` —
  cold-launch gfxinfo capture (LoginScreen render only).

### Re-deferred status

- **Phase 3.1:** re-DEFERRED — pending a signed-in session on a device
  or emulator. The auth gate prevents any path from cold launch to
  TimelineScreen → FAB → 5s animation. The production-receiver fix
  below does not change this. The `docs/superpowers/m3/gfxinfo/`
  directory now exists with the cold-launch capture to be overwritten
  once the unblock lands.
- **Phase 3.2:** test FILE and test-only manifest are committed (per
  the prior block). Test GREEN-RUN still re-DEFERRED — pending a
  signed-in session on a device or emulator. The production-receiver
  fix below unblocked the test runner from crashing on
  `BOOT_COMPLETED`; the test now starts cleanly but hangs on
  `LoginScreen` waiting for `quick-create-fab` to render, which it
  cannot until auth is signed in.

### Production-receiver fix (2026-09-03, late)

The 2026-09-03 device-attempt above identified
`ExecutionAlarmRescheduleReceiver` as the second blocker alongside
the auth gate. The user authorized a production-code fix to that
receiver **specifically to let Task 3.2 land without bypassing
auth**; the fix is out of M3 plan scope but is the only path that
does not require bypassing the auth gate.

The fix is committed as a separate change:

- `app/src/main/java/app/tastile/android/notifications/ExecutionAlarmRescheduleReceiver.kt`:
  removed `@AndroidEntryPoint` and the Hilt `@Inject` field. The
  Hilt-generated wrapper calls `inject()` before delegating to user
  `onReceive`, which crashes when the system delivers one of the
  receiver's subscribed broadcasts (notably `BOOT_COMPLETED`,
  queued since device boot) **before** `Application.onCreate()` has
  finished initializing the Hilt graph. The fix uses
  `EntryPointAccessors.fromApplication(...)` wrapped in a try/catch
  on `IllegalStateException`. If Hilt is not ready, the broadcast is
  dropped; the reschedule is retried on the next foreground launch
  through the normal MainActivity path. No reschedule is lost: the
  same five triggers remain subscribed.

Verification on the 2026-09-03 device-attempt (XIG03 / Android 15 /
API 35):

- Before the fix: `am instrument` reports
  `Process crashed before executing the test(s)`. logcat shows the
  production process dying on `BOOT_COMPLETED` with
  `Unable to start receiver ExecutionAlarmRescheduleReceiver`.
- After the fix: `am instrument` starts the test cleanly.
  logcat shows
  `TestRunner: started: timelineQuickCreateFab_opensSheet(...)`
  with no `Process crashed` line. The test then **hangs** waiting
  for `quick-create-fab` to render — the auth gate shows
  `LoginScreen` and the FAB testTag does not exist on that
  surface. This is the expected state under the user's
  "no auth bypass" choice.

Net effect on Task 3.2:

- Test file: committed (`9df0a5b`).
- Test-only manifest with `tools:node="remove"` on the receiver:
  committed (`a10aa20`).
- Production-receiver fix: committed (this commit).
- Green-run: still requires a signed-in session on a device or
  emulator. The auth-gate unblock is out of M3 plan scope.

The plan's "Run with" command (line 2009) remains accurate and can
be executed as soon as the auth-gate unblock lands.
