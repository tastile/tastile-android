# Phase M5 — WCAG AA Contrast Audit (baseline)

## Header

| Field | Value |
|---|---|
| Date | 2026-07-16 |
| Branch | `2026-07-07-android-parity` |
| HEAD commit | `4e1a7c36146b14abe7563192632f07bc3a19f1a9` — `feat(android): add domain layer with GetTilesUseCase + ObserveAuthStateUseCase (R23)` |
| Scope | All four M3 schemes built in `app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt` from constants in `Color.kt` |
| Mode | Read-only audit (no `.kt` / `.xml` color resource modified) |

Schemes in scope:

- `LightDefaultColorScheme` — "default" brand (purple / orange / blue) light
- `DarkDefaultColorScheme`  — "default" brand dark
- `LightAndroidColorScheme` — "android" brand (green / dark-green / teal) light
- `DarkAndroidColorScheme`  — "android" brand dark

A fourth runtime mode is `dynamicDarkColorScheme(ctx)` / `dynamicLightColorScheme(ctx)` on Android 12+, which derives its palette from the system wallpaper — that pair is **out of scope** here (wallpaper-controlled and cannot be audited in code).

## Method

WCAG 2.1 AA thresholds:

- Normal text (< 18 pt or < 14 pt bold): contrast ratio >= **4.5 : 1**
- Large text  (>= 18 pt or >= 14 pt bold): contrast ratio >= **3 : 1**
- UI components / graphical objects: contrast ratio >= **3 : 1**

Relative luminance:

```
L = 0.2126 * R + 0.7152 * G + 0.0722 * B          (linearized sRGB)
```

sRGB -> linear:

```
if c <= 0.03928: linear = c / 12.92
else:            linear = ((c + 0.055) / 1.055) ** 2.4
```

Contrast ratio:

```
ratio = (L_lighter + 0.05) / (L_darker + 0.05)
```

Reproduction:

```bash
python docs/superpowers/m5/wcag-audit-script.py        # pretty table
python docs/superpowers/m5/wcag-audit-script.py csv    # CSV for spreadsheets
```

Script verified reproducible — two runs produced byte-identical output
(`diff run1.txt run2.txt` returned empty).

## Results — 56 M3 role pairs

Notation:

- "Background / Foreground" lists the M3 slot names; the script reads the
  resolving tokens from `Color.kt` (verbatim) and the assignment from each
  `lightColorScheme(...)` / `darkColorScheme(...)` block in `Theme.kt`.
- The `bg_token` column is the literal `Color(0x…)` value used as the
  background; `fg_token` is the literal foreground.
- "Normal" applies to body text < 18 pt; "Large / UI" applies to >= 18 pt text
  and to graphical objects (icons, outlines, dividers).

### LightDefault (brand: purple / orange / blue)

| Pair | bg_token | fg_token | Ratio | Normal (>=4.5) | Large / UI (>=3) |
|---|---|---|---:|---|---|
| primary / onPrimary | Purple40 (#8B418F) | Color.White (#FFFFFF) | 6.46 | PASS | PASS |
| primaryContainer / onPrimaryContainer | Purple90 (#FFD6FA) | Purple10 (#36003C) | 13.30 | PASS (AAA) | PASS |
| secondary / onSecondary | Orange40 (#A23F16) | Color.White | 6.44 | PASS | PASS |
| secondaryContainer / onSecondaryContainer | Orange90 (#FFDBCF) | Orange10 (#380D00) | 13.29 | PASS (AAA) | PASS |
| tertiary / onTertiary | Blue40 (#006780) | Color.White | 6.46 | PASS | PASS |
| tertiaryContainer / onTertiaryContainer | Blue90 (#B8EAFF) | Blue10 (#001F28) | 13.25 | PASS (AAA) | PASS |
| error / onError | Red40 (#BA1A1A) | Color.White | 6.46 | PASS | PASS |
| errorContainer / onErrorContainer | Red90 (#FFDAD6) | Red10 (#410002) | 13.26 | PASS (AAA) | PASS |
| background / onBackground | DarkPurpleGray99 (#FCFCFC) | DarkPurpleGray10 (#201A1B) | 16.71 | PASS (AAA) | PASS |
| surface / onSurface | DarkPurpleGray99 | DarkPurpleGray10 | 16.71 | PASS (AAA) | PASS |
| surfaceVariant / onSurfaceVariant | PurpleGray90 (#EDDEE8) | PurpleGray30 (#4D444C) | 7.22 | PASS (AAA) | PASS |
| inverseSurface / inverseOnSurface | DarkPurpleGray20 (#362F30) | DarkPurpleGray95 (#FAEEEF) | 11.54 | PASS (AAA) | PASS |
| outline / background | PurpleGray50 (#7F747C) | DarkPurpleGray99 | 4.36 | n/a (UI only) | PASS |
| outline / surface | PurpleGray50 | DarkPurpleGray99 | 4.36 | n/a (UI only) | PASS |

### DarkDefault (brand: purple / orange / blue)

| Pair | bg_token | fg_token | Ratio | Normal (>=4.5) | Large / UI (>=3) |
|---|---|---|---:|---|---|
| primary / onPrimary | Purple80 (#FFA9FE) | Purple20 (#560A5D) | 7.74 | PASS (AAA) | PASS |
| primaryContainer / onPrimaryContainer | Purple30 (#702776) | Purple90 (#FFD6FA) | 7.25 | PASS (AAA) | PASS |
| secondary / onSecondary | Orange80 (#FFB59B) | Orange20 (#5B1A00) | 7.75 | PASS (AAA) | PASS |
| secondaryContainer / onSecondaryContainer | Orange30 (#812800) | Orange90 (#FFDBCF) | 7.28 | PASS (AAA) | PASS |
| tertiary / onTertiary | Blue80 (#5DD5FC) | Blue20 (#003544) | 7.78 | PASS (AAA) | PASS |
| tertiaryContainer / onTertiaryContainer | Blue30 (#004D61) | Blue90 (#B8EAFF) | 7.27 | PASS (AAA) | PASS |
| error / onError | Red80 (#FFB4AB) | Red20 (#690005) | 7.72 | PASS (AAA) | PASS |
| errorContainer / onErrorContainer | Red30 (#93000A) | Red90 (#FFDAD6) | 7.24 | PASS (AAA) | PASS |
| background / onBackground | DarkPurpleGray10 (#201A1B) | DarkPurpleGray90 (#ECDFE0) | 13.22 | PASS (AAA) | PASS |
| surface / onSurface | DarkPurpleGray10 | DarkPurpleGray90 | 13.22 | PASS (AAA) | PASS |
| surfaceVariant / onSurfaceVariant | PurpleGray30 (#4D444C) | PurpleGray80 (#D0C3CC) | 5.50 | PASS | PASS |
| inverseSurface / inverseOnSurface | DarkPurpleGray90 | DarkPurpleGray10 | 13.22 | PASS (AAA) | PASS |
| outline / background | PurpleGray60 (#998D96) | DarkPurpleGray10 | 5.39 | n/a (UI only) | PASS |
| outline / surface | PurpleGray60 | DarkPurpleGray10 | 5.39 | n/a (UI only) | PASS |

### LightAndroid (brand: green / dark-green / teal)

| Pair | bg_token | fg_token | Ratio | Normal (>=4.5) | Large / UI (>=3) |
|---|---|---|---:|---|---|
| primary / onPrimary | Green40 (#006D36) | Color.White | 6.48 | PASS | PASS |
| primaryContainer / onPrimaryContainer | Green90 (#5AFF9D) | Green10 (#00210B) | 13.27 | PASS (AAA) | PASS |
| secondary / onSecondary | DarkGreen40 (#4F6352) | Color.White | 6.48 | PASS | PASS |
| secondaryContainer / onSecondaryContainer | DarkGreen90 (#D3E8D3) | DarkGreen10 (#0D1F12) | 13.30 | PASS (AAA) | PASS |
| tertiary / onTertiary | Teal40 (#3A656F) | Color.White | 6.42 | PASS | PASS |
| tertiaryContainer / onTertiaryContainer | Teal90 (#BEEAF6) | Teal10 (#001F26) | 13.31 | PASS (AAA) | PASS |
| error / onError | Red40 | Color.White | 6.46 | PASS | PASS |
| errorContainer / onErrorContainer | Red90 | Red10 | 13.26 | PASS (AAA) | PASS |
| background / onBackground | DarkGreenGray99 (#FBFDF7) | DarkGreenGray10 (#1A1C1A) | 16.73 | PASS (AAA) | PASS |
| surface / onSurface | DarkGreenGray99 | DarkGreenGray10 | 16.73 | PASS (AAA) | PASS |
| surfaceVariant / onSurfaceVariant | GreenGray90 (#DDE5DB) | GreenGray30 (#414941) | 7.23 | PASS (AAA) | PASS |
| inverseSurface / inverseOnSurface | DarkGreenGray20 (#2F312E) | DarkGreenGray95 (#F0F1EC) | 11.56 | PASS (AAA) | PASS |
| outline / background | GreenGray50 (#727971) | DarkGreenGray99 | 4.37 | n/a (UI only) | PASS |
| outline / surface | GreenGray50 | DarkGreenGray99 | 4.37 | n/a (UI only) | PASS |

### DarkAndroid (brand: green / dark-green / teal)

| Pair | bg_token | fg_token | Ratio | Normal (>=4.5) | Large / UI (>=3) |
|---|---|---|---:|---|---|
| primary / onPrimary | Green80 (#0EE37C) | Green20 (#003919) | 7.69 | PASS (AAA) | PASS |
| primaryContainer / onPrimaryContainer | Green30 (#005227) | Green90 (#5AFF9D) | 7.26 | PASS (AAA) | PASS |
| secondary / onSecondary | DarkGreen80 (#B7CCB8) | DarkGreen20 (#223526) | 7.69 | PASS (AAA) | PASS |
| secondaryContainer / onSecondaryContainer | DarkGreen30 (#394B3C) | DarkGreen90 (#D3E8D3) | 7.24 | PASS (AAA) | PASS |
| tertiary / onTertiary | Teal80 (#A2CED9) | Teal20 (#02363F) | 7.72 | PASS (AAA) | PASS |
| tertiaryContainer / onTertiaryContainer | Teal30 (#214D56) | Teal90 (#BEEAF6) | 7.21 | PASS (AAA) | PASS |
| error / onError | Red80 | Red20 | 7.72 | PASS (AAA) | PASS |
| errorContainer / onErrorContainer | Red30 | Red90 | 7.24 | PASS (AAA) | PASS |
| background / onBackground | DarkGreenGray10 | DarkGreenGray90 (#E2E3DE) | 13.29 | PASS (AAA) | PASS |
| surface / onSurface | DarkGreenGray10 | DarkGreenGray90 | 13.29 | PASS (AAA) | PASS |
| surfaceVariant / onSurfaceVariant | GreenGray30 (#414941) | GreenGray80 (#C1C9BF) | 5.49 | PASS | PASS |
| inverseSurface / inverseOnSurface | DarkGreenGray90 | DarkGreenGray10 | 13.29 | PASS (AAA) | PASS |
| outline / background | GreenGray60 (#8B938A) | DarkGreenGray10 | 5.42 | n/a (UI only) | PASS |
| outline / surface | GreenGray60 | DarkGreenGray10 | 5.42 | n/a (UI only) | PASS |

## Summary

- **FAIL (any threshold):** 0 / 56 pairs
- **EDGEs (normal text 4.5 borderline):** 0 / 56 pairs
- **Marginal pairs** (lowest ratio still PASS): the eight `outline / …` rows at
  4.36 – 5.42. They clear the 3:1 UI threshold but are *close*; see "Notes".

The lowest three ratios across all 56 audited pairs (all still PASS the 3:1
UI threshold):

| Rank | Scheme | Pair | Ratio |
|---:|---|---|---:|
| 1 | LightDefault  | outline / background | 4.36 |
| 1 | LightDefault  | outline / surface    | 4.36 |
| 3 | LightAndroid  | outline / background | 4.37 |
| 3 | LightAndroid  | outline / surface    | 4.37 |

The next tier (4.5 – 5.5 ratio band):

| Scheme | Pair | Ratio |
|---|---|---:|
| DarkDefault  | outline / background | 5.39 |
| DarkDefault  | outline / surface    | 5.39 |
| DarkAndroid  | outline / background | 5.42 |
| DarkAndroid  | outline / surface    | 5.42 |
| DarkAndroid  | surfaceVariant / onSurfaceVariant | 5.49 |
| DarkDefault  | surfaceVariant / onSurfaceVariant | 5.50 |

## Notes for M5 Task 2

- **What was inspected.** `Color.kt` (62 named `Color(0x…)` constants) and the
  four `lightColorScheme(...)` / `darkColorScheme(...)` blocks in `Theme.kt`
  (`LightDefaultColorScheme`, `DarkDefaultColorScheme`,
  `LightAndroidColorScheme`, `DarkAndroidColorScheme`). The
  `Background.kt` / `Tint.kt` / `Gradient.kt` files only declare container
  composition-local types and supply no foreground colors, so they do not
  participate in pairwise contrast.
- **`values-night/` does not exist.** All dark-mode color logic lives in
  Compose (`isSystemInDarkTheme()` -> `darkColorScheme(...)` in `Theme.kt`).
  The only XML color resource is `app/src/main/res/values/colors.xml` with two
  entries: `tastile_launch_bg = #FF171717` (splash background) and
  `tastile_brand_accent = #FF2563EB` (unused as a foreground). Neither is in
  an M3 text pair, so neither is in the audit table.
- **`BrandColors.kt` referenced by M5 plan does not exist** in the current
  source tree. The pre-merge plan was written against the Now-in-Android
  sample app structure; the live file is `Color.kt`. Task 2 should treat
  `Color.kt` as the canonical token source. (`ui/designsystem/BrandColors.kt`
  does not exist; `find . -name 'BrandColors.kt'` returns only the upstream
  reference file under `reference/nowinandroid/.../ThemeBrand.kt`, which is
  not compiled into the APK.)
- **No tone shifts needed for the four fallback schemes.** All 56 pairs clear
  AA at the threshold they apply to. Task 2's "conservative token shift" step
  from the M5 plan therefore has no targets in the fallback schemes.
- **`outline` (PurpleGray50 / GreenGray50) is the only role to keep an eye
  on.** It is the only token sitting within 0.5 of the 3:1 UI floor
  (`4.36–4.37` in the light schemes). It is safe for its current role (icon
  outlines, dividers, chip borders) but **must not** be repurposed as a text
  foreground; if a future component uses `outline` to color
  normal-size text, that surface will FAIL normal-text AA. The four dark-scheme
  outline values (PurpleGray60, GreenGray60) sit at 5.39–5.42, which is safe
  for both UI and normal text.
- **All `*Container` pairs score >= 7**, comfortably AAA. No risk surface.
- **All on-`*` text-on-white pairs in light schemes** score 6.42–6.48, a tight
  band all driven by luminance just above the 4.5 floor. Functional but no
  excess margin — if Phase M5 adds a feature that swaps `Color.White` for a
  warmer neutral or a tinted background, re-run the script before merging.
- **Dynamic color path not audited.** `dynamicLightColorScheme(ctx)` /
  `dynamicDarkColorScheme(ctx)` (Android 12+, wallpaper-derived) cannot be
  statically audited; QA needs to spot-check the dynamic scheme on at least
  three OEM wallpapers per scheme per launch context.
- **Reproduction.** `python docs/superpowers/m5/wcag-audit-script.py` emits
  the same 56 rows on every run; verified by `diff` across two invocations.
  Re-run after any edit to `Color.kt` or `Theme.kt`.
