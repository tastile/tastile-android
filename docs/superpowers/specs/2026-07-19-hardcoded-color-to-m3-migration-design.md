# Hardcoded Color → Material 3 Migration (tastile-android)

Date: 2026-07-19
Status: Approved (brainstorm), pending spec review

## Goal

Eliminate all hardcoded color literals in `tastile-android` that bypass the
Material 3 `ColorScheme`. The theme tokens must drive every status tint
(`DONE`, `STARTED`, "now-line", pro-tier accent, etc.) instead of literal
`Color.Red`, `Color.Green`, or `Color(0xFF...)` values. Enable Material You
on Android 12+ by default.

## Scope (strict)

In-scope:
- 5 files covering 10 hardcoded usages (NowIndicator.kt has 2 lines in one
  row) that currently bypass `MaterialTheme.colorScheme`.
- 1 line in `core/designsystem/theme/Theme.kt` to flip the
  `NiaTheme(disableDynamicTheming = ...)` default from `true` to `false`.

Out of scope (left untouched):
- `core/designsystem/theme/Color.kt` (NiA `Purple/Orange/Blue/Green` palette).
- `Color.White` in `Theme.kt` (M3 canonical on-primary text).
- `Color.Black` in `DarkAndroidGradientColors.container` /
  `DarkAndroidBackgroundTheme.color` (Android-brand dark variants).
- `Color.Transparent` and `Color.Unspecified` (idiomatic sentinel/no-fill).
- `core/designsystem/theme/Background.kt` etc. gradient stops
  (`Color.Transparent` is required by the brush construction).

## Semantic → Material 3 token mapping

| File | Line | Current | New token | Rationale |
|---|---|---|---|---|
| `ui/now/NowScreen.kt` | 185 | `Color.Green` (Card border) | `primary` | ACTIVE state, brand-emphasised |
| `ui/now/NowScreen.kt` | 201 | `Color.Green` ("Active" label) | `primary` | ACTIVE label, same as border |
| `ui/now/NowScreen.kt` | 288 | `Color.Green` (Check icon tint) | `tertiary` | "Complete" action = transition to DONE |
| `ui/now/NowScreen.kt` | 317 | `Color.Green to "Done"` | `tertiary` | DONE lifecycle badge surface |
| `ui/mobile/calendar/NowIndicator.kt` | 44, 52 | `Color.Red` (background) | `error` | Alert/attention role for "now" line |
| `ui/prompt/PromptScreen.kt` | 262 | `Color.Green` (contentColor) | `tertiary` | Complete-Task button = DONE action |
| `ui/mobile/tabs/tiles/TilesChangesBody.kt` | 41 + 76 | `Color(0xFF0D8A72)` private const | inline `tertiary` (when `ended`) / `primary` (when active) | Matches `LifecycleBadge` mapping |
| `ui/account/AccountScreen.kt` | 278 | `Color(0xFFFFD700).copy(alpha = 0.2f)` | `primaryContainer.copy(alpha = 0.2f)` | Pro-tier branded surface pattern |
| `ui/account/AccountScreen.kt` | 283 | `Color(0xFFB8860B)` | `onPrimaryContainer` | Coordinated text on Pro-tier surface |

### Tokens used, in order of frequency

- `tertiary` — DONE/COMPLETE, "ended" tiles in dashboard, "Complete" actions
- `primary` — ACTIVE/STARTED states, "in-progress" tiles in dashboard
- `error` — "now" time-line on calendar
- `primaryContainer` + `onPrimaryContainer` — Pro-tier plan badge

## Theme.kt change (one line)

`core/designsystem/theme/Theme.kt`:

```diff
 fun NiaTheme(
     darkTheme: Boolean = isSystemInDarkTheme(),
     androidTheme: Boolean = false,
-    disableDynamicTheming: Boolean = true,
+    disableDynamicTheming: Boolean = false,
```

Effect: Android 12+ users now see Material You `dynamicLightColorScheme` /
`dynamicDarkColorScheme` generated from their wallpaper. Pre-12 keeps the
existing `Purple` palette (`LightDefaultColorScheme` /
`DarkDefaultColorScheme`).

## Why this mapping is M3-compliant

- **`tertiary` for DONE/COMPLETE**: M3 defines `tertiary` as the third accent
  role, conceptually used for "highlight that isn't brand or alert".
  Lifecycle/COMPLETE states are a textbook fit.
- **`primary` for ACTIVE/STARTED**: M3 `primary` is the dominant brand accent.
  Active/started states carry brand weight (calendar block focus, started
  tiles, "in-progress" dashboard row).
- **`error` for the now-line**: M3 `error` is reserved for urgent attention
  signals. The current-time line is the only constantly animated, non-passive
  element on the calendar — high attention, non-brand — so `error` is the
  correct alert role.
- **`primaryContainer` / `onPrimaryContainer` for Pro tier**: M3's
  `primaryContainer` is the standard "branded surface" pattern (also used for
  prominent cards, chips, emphasis backgrounds). The Pro tier badge fits this
  pattern exactly; on Material You the surface reflects the wallpaper's
  primary tone automatically.
- **`color.copy(alpha = 0.2f)` still works**: the existing translucent-surface
  pattern (`Surface(color = tertiary.copy(alpha = 0.2f))`) stays identical in
  structure. The only change is which `ColorScheme` token is sampled.

## Verification

After implementation:

1. `rg -n 'Color\.Red|Color\.Green|Color\.|Color\(0x' app/src/main/java` —
   must only show matches inside `theme/Color.kt` or
   `theme/Theme.kt` (allowed: literal NiA palette tokens + on-primary white
   in scheme builder).
2. `./gradlew :app:compileDebugKotlin` — clean build.
3. `./gradlew :app:assembleDebug` then `adb install -r` (see package memory
   `xiaomi_install_workaround.md` if local device is Xiaomi).
4. Visual smoke test per dark/light variants:
   - Now screen: Active tile border + "Active" label picks up `primary`;
     DONE badge picks up `tertiary`.
   - Calendar: "now" line is `error` (red/orange under dynamic theming).
   - Dashboard changes: "ended" rows show `tertiary` dot.
   - Account: Pro plan badge shows `primaryContainer` wash with
     `onPrimaryContainer` text.
5. (Material You verification) On Android 12+, change wallpaper and
   re-open app — token-driven colours should shift accordingly.

## Non-goals (explicitly deferred)

- Replacing the NiA `Purple` palette with actual Tastile brand colours.
- Adding a custom `LocalStatusColors` / extended M3 `ColorScheme` for
  success/danger; standard M3 tokens suffice.
- Lint rule to ban `Color.X` outside `theme/` (could be a future PR).

## Open risks

- **Risk**: Pro-tier badge identity is lost when wallpapers generate
  low-saturation primary containers (e.g. mostly-grey wallpaper).
  *Mitigation*: `primaryContainer` is still tinted; Pro badge still reads as
  "branded". If legal/design rejects loss of literal gold, this would
  require the deferred `LocalStatusColors` extension — out of scope here.
- **Risk**: User chose "嚴格スコープ" but also "Dynamic Color ON"; this spec
  applies a 1-line Theme.kt default flip that the user implicitly authorised
  via the latter choice.
