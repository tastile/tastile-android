# Android push endpoint registration

## Goal

Connect the Android client to Core's owner-scoped `/v1/endpoints` contract so
an installed device can register an opaque push token, expose its delivery
capabilities, and unregister that exact endpoint on sign-out or token rotation.

## Scope

1. Add typed endpoint request/response models and authenticated API methods.
2. Add an endpoint repository that makes registration idempotent at the client
   boundary and removes superseded endpoint IDs only after a replacement is
   accepted by Core.
3. Initialize FCM from injected BuildConfig values and acquire a token through
   an injected token provider. Firebase project configuration and actual token
   issuance remain external deployment inputs; the repository must be testable
   with a fake provider.
4. Cover request serialization, replacement ordering, and unregister behavior
   with unit tests.

## Non-goals

This change cannot create a Firebase project or send a production push. Release
requires `FIREBASE_APPLICATION_ID`, `FIREBASE_PROJECT_ID`, `FIREBASE_API_KEY`,
and `FIREBASE_GCM_SENDER_ID` GitHub Secrets. The Core delivery provider and
Android runtime FCM configuration will be verified once those values and the
provider endpoint are available.
