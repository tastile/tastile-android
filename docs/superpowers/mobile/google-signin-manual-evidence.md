# Native Google Sign-In — Manual Device Evidence

- **Date:** 2026-09-03
- **Spec:** [docs/superpowers/specs/2026-09-03-google-signin-mobile-design.md](../../specs/2026-09-03-google-signin-mobile-design.md)
- **Status:** Pending device run

## Prerequisites

1. A debug build with `GOOGLE_ANDROID_CLIENT_ID` set in
   `~/.gradle/gradle.properties` to the value registered in Google
   Cloud Console as the `app.tastile.android` Android OAuth client.
2. The matching `GOOGLE_ANDROID_CLIENT_ID` set on the Tastile web
   server's environment (so BetterAuth accepts the idToken).
3. A device with Play Services and at least one Google account signed
   in (Settings → Accounts).

## Checklist

For each step, record PASS / FAIL + the observed behavior.

### Cold launch, no Google account on device

- [ ] Launch the app cold (force-stop + restart).
- [ ] Navigates to `LoginScreen` (assumes email/password is the
      default state).
- [ ] "Continue with Google" button is visible with the "G" mark.
- [ ] Tap the button.
- [ ] Expected: the system browser opens to
      `${WEB_BASE_URL}/login?provider=google` (the existing web OAuth
      handoff). This is the silent fallback path —
      `GoogleSignInUnavailableException` → `signInWithProvider("google")`.

### Cold launch, single Google account on device

- [ ] Launch the app cold.
- [ ] Tap "Continue with Google".
- [ ] Expected: Credential Manager auto-selects the single Google
      account (no bottom-sheet prompt).
- [ ] Expected: short delay, then `MainActivity` opens (the user is
      signed in via Google — BetterAuth session persisted).
- [ ] Force-stop the app + cold launch again.
- [ ] Expected: `LoginScreen` is NOT shown; the user goes straight to
      `MainActivity` because the BetterAuth session is persisted.

### Cold launch, multiple Google accounts on device

- [ ] Launch the app cold.
- [ ] Tap "Continue with Google".
- [ ] Expected: system account picker bottom-sheet appears with the
      Google accounts listed.
- [ ] Pick one.
- [ ] Expected: short delay, then `MainActivity` opens.

### User cancels the account picker

- [ ] Launch the app cold.
- [ ] Tap "Continue with Google".
- [ ] Dismiss the account picker without selecting.
- [ ] Expected: the system browser opens to
      `${WEB_BASE_URL}/login?provider=google` (web fallback).

### Email-not-verified edge case

- [ ] Sign in with a Google account whose email is NOT verified.
- [ ] Expected: BetterAuth creates the user with
      `emailVerified: false`; the user is signed in; no error chip.
- [ ] (Out of scope: BetterAuth sends a verification email; the
      verification flow is web-only.)

### idToken rejection

- [ ] Temporarily set `GOOGLE_ANDROID_CLIENT_ID` on the web server to
      a value that does NOT match the Android client.
- [ ] Tap "Continue with Google".
- [ ] Expected: error chip with "HTTP 401 ..." or similar.
- [ ] Restore the correct `GOOGLE_ANDROID_CLIENT_ID`.

## Evidence to capture

When this checklist runs green, commit the following under
`docs/superpowers/mobile/google-signin-evidence-<device>-<date>/`:

- `cold-launch.txt` — `adb logcat` capture for a cold launch → tap →
  auto-select → MainActivity, trimmed to the relevant log lines.
- `account-picker.png` — screenshot of the system account picker.
- `web-fallback.txt` — logcat capture for the no-Google-account
  fallback (Intent.ACTION_VIEW to `${WEB_BASE_URL}/login?provider=google`).

## Status

- **Checklist authored:** 2026-09-03
- **First green run:** TBD (pending device availability per
  `docs/superpowers/m3/phase-3-deferral.md`)
