# Android Settings Navigation Design

Date: 2026-07-17

## Goal

Make the Android settings page a clearly separated destination without changing the main-screen shell. The settings route must own its App bar so its top content is never hidden behind the shell or status-bar layout.

## Current Problem

`MobileScaffold` suppresses the shell App bar for the `settings` route, but `SettingsScreen` currently renders only its scrollable body. The route therefore has no App bar, and its content does not have a route-owned top inset.

The navigation drawer also places the settings destination in the same flat list as the main destinations, so it is not visually distinguishable as a separate settings area.

## Design

### Screen boundary

`MobileScaffold` remains responsible for the main application shell:

- Timeline
- Tasks
- Projects
- References

The existing `settings` route remains outside the shell drawer and shell App bar. `SettingsScreen` becomes a self-contained Material 3 `Scaffold`.

The settings Scaffold owns a `CenterAlignedTopAppBar` with:

- a leading back action wired to the existing `onBack` callback;
- the title `Setting`;
- a test tag for the settings App bar.

The settings body consumes the Scaffold `innerPadding` and fills the available size before applying its vertical scroll. This keeps the first setting section below the App bar while preserving the existing controls and state flow.

### Drawer grouping

The modal drawer keeps the primary destinations together:

1. Timeline
2. Tasks
3. Projects
4. References

A divider and section label then introduce a separate settings group:

- section label: `Setting`;
- destination: `Setting`.

The settings destination keeps its existing route and test tag, and selection remains based on the current navigation route. The label is exposed through Android string resources and is intentionally the English `Setting` text in both resource sets because that exact label is part of the requested navigation design.

### Navigation behavior

- Tapping `Setting` navigates to the existing `settings` route and closes the drawer.
- The settings App bar back action pops the current route through the existing callback.
- The system back action continues to use the NavHost back stack.
- The settings route does not reopen or display the main drawer.

## Scope

Modify only:

- `app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt`
- `app/src/main/java/app/tastile/android/ui/mobile/SidePanelDrawerContent.kt`
- the English and Japanese Android string resources
- focused Android UI tests for the drawer grouping and settings App bar

Do not refactor `MobileScaffold`, change settings state management, or alter the existing setting controls.

## Verification

1. Run focused JVM and Android UI tests covering settings navigation and App bar semantics.
2. Build the debug app with the repository's Gradle verification task.
3. On an emulator/device, open the drawer, confirm `Setting` is in its own group, open it, confirm the App bar is visible, and confirm the first setting section starts below it.
4. Press the App bar back action and confirm return to the previous main route.

The Android repository currently contains unrelated uncommitted QuickCreate and Timeline changes. Those changes must remain untouched while implementing this design.
