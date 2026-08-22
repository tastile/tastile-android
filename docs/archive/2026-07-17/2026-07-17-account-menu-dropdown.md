# Avatar Account Menu: Bottom Sheet → Dropdown Menu

Date: 2026-07-17
Branch: `2026-07-07-android-parity`
Scope: C7-followup (avatar trigger presentation only — composition unchanged)

## Goal

Replace the `ModalBottomSheet`-based `AccountMenuSheet` (4 rows) with a Material 3
`DropdownMenu` anchored directly to the top-bar avatar button. Same 4 items,
same i18n keys, same confirm-dialog gating for sign-out. The web equivalent
(`src/app/app/account-menu.tsx`) wraps the avatar in a Radix `FloatingMenu` —
this change brings Android to the same anchor-on-trigger pattern, matching the
in-file precedent set by `ScaleDropdown` (`MobileTopBar.kt:121`).

Composition (R1 from `2026-07-07-android-content-parity.md` §2) is **not**
changed: 4 rows + email header + sign-out confirm dialog carry over verbatim.

## File changes

| File | Action | Notes |
|---|---|---|
| `app/src/main/java/app/tastile/android/ui/mobile/AccountDropdownMenu.kt` | **NEW** | Composable hosting the M3 `DropdownMenu`. Injects `OverlayViewModel` + `DashboardViewModel` via `hiltViewModel()` (same pattern as the deleted `AccountMenuSheet`). |
| `app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt` | MODIFY | `TopBarAvatarAction` gains local `menuOpen: Boolean` state, wraps its `IconButton` in a `Box`, and renders `AccountDropdownMenu` underneath. Drop the `onAvatar` callback parameter from `MobileTopBar`. Add `stateDescription` to the avatar button for screen-reader state. |
| `app/src/main/java/app/tastile/android/ui/mobile/MobileScaffold.kt` | MODIFY | Drop the `onAvatar = { … }` argument from the `MobileTopBar(...)` call (the menu is local to the top bar now). |
| `app/src/main/java/app/tastile/android/ui/mobile/OverlayState.kt` | MODIFY | Delete `data object AccountMenu : Overlay`. |
| `app/src/main/java/app/tastile/android/ui/mobile/OverlayLayer.kt` | MODIFY | Remove the `AccountMenuSheet(...)` line and its import. |
| `app/src/main/java/app/tastile/android/ui/mobile/sheets/AccountMenuSheet.kt` | **DELETE** | Replaced by `AccountDropdownMenu.kt`. |
| `app/src/test/java/app/tastile/android/ui/mobile/sheets/AccountMenuSheetTest.kt` | **DELETE** | Replaced. |
| `app/src/test/java/app/tastile/android/ui/mobile/AccountDropdownMenuTest.kt` | **NEW** | Same 5 cases rewritten against `expanded`/`onDismiss` plumbing. |

## Composition (preserved)

`AccountDropdownMenu` content, in order:

1. **Header** (non-clickable) — `email.ifBlank { stringResource(R.string.shell_account_signed_in) }`
2. **Profile** → `Overlay.AccountSettings`
3. **Subscription** → `Overlay.Subscription`
4. **Access Tokens** → `Overlay.Tokens`
5. **Sign out** → opens confirm `AlertDialog` → `viewModel.signOut()`

i18n keys: `nav_account_profile`, `nav_account_subscription`,
`nav_account_tokens`, `nav_account_sign_out`, `shell_account_signed_in`,
`shell_account_sign_out_confirm`, `dashboard_tiles_delete_dialog_cancel`.

Test tags preserved (renamed only the file): `account_menu_profile`,
`account_menu_subscription`, `account_menu_tokens`, `account_menu_sign_out`,
`account_menu_sign_out_confirm`, `account_menu_sign_out_cancel`.

## Menu geometry & a11y

- Anchor: `Box` wrapping the avatar `IconButton`. M3 `DropdownMenu` anchors
  itself to the parent content automatically (same pattern as `ScaleDropdown`).
- Width: unpinned — items size to content. Tight menu visually distinguishes
  it from a navigation drawer.
- Dismissal: `onDismissRequest = { menuOpen = false }` (M3 default handles
  outside-click). System back is not separately intercepted because the menu
  is anchored to the top bar, not registered with `Overlay` state — the
  existing `BackHandler` in `MobileScaffold` checks `Overlay.Hidden` and is a
  no-op here, which is acceptable.
- a11y: avatar button keeps its existing `clearAndSetSemantics` block; add
  `stateDescription = if (menuOpen) "Open" else "Closed"` so TalkBack
  announces the menu state when focused.

## Implementation steps

1. **Add `AccountDropdownMenu.kt`** — new composable with `DropdownMenu`
   + 4 `DropdownMenuItem`s + sign-out confirm `AlertDialog`. Injects
   `OverlayViewModel` and `DashboardViewModel` via `hiltViewModel()`.
2. **Modify `MobileTopBar.kt`** — convert `TopBarAvatarAction` to own local
   `menuOpen` state, wrap the avatar `IconButton` in a `Box`, render
   `AccountDropdownMenu` next to it. Drop the `onAvatar` parameter from
   `MobileTopBar`. Add `stateDescription` for a11y.
3. **Modify `MobileScaffold.kt`** — drop the `onAvatar = …` argument from
   the `MobileTopBar(...)` call.
4. **Modify `OverlayState.kt`** — delete the `AccountMenu` data object.
5. **Modify `OverlayLayer.kt`** — remove the `AccountMenuSheet(...)` line
   and the `AccountMenuSheet` import.
6. **Delete + replace tests** — delete `AccountMenuSheet.kt` and
   `AccountMenuSheetTest.kt`; create `AccountDropdownMenuTest.kt` with the
   same 5 cases ported to the new plumbing.
7. **Verify** — `gradle :app:compileDebugKotlin :app:testDebugUnitTest`
   per `2026-07-07-android-content-parity.md` §2 R6.

## Verification

- Compile + unit tests green (R6).
- Manual: tap avatar → menu opens anchored under it → tap Profile → menu
  closes, `AccountSettings` sheet appears → tap avatar → Sign out →
  confirm dialog → sign out.
- Existing `AccountSettings` / `Subscription` / `Tokens` sheet tests
  untouched and must stay green.
- `rg -n "AccountMenu"` over `app/src/main` returns 0 hits after step 6.

## Out of scope (deferred)

- Plan-aware header (Pro/Free badge like web) — composition-level, not
  part of this presentation fix.
- Removing `onMenu` / drawer wiring — separate concern.
- Refresh of i18n keys — names already match web's `nav.account.*` namespace.