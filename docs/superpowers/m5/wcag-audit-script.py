#!/usr/bin/env python3
"""
WCAG AA contrast audit script for the Tastile Android M3 palette.

Read-only audit of the four Material 3 color schemes defined in
app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt, driven
by the named constants in
app/src/main/java/app/tastile/android/core/designsystem/theme/Color.kt.

WCAG 2.1 ratios:
  - Normal text  (< 18pt or < 14pt bold): >= 4.5:1
  - Large text   (>= 18pt or >= 14pt bold): >= 3:1
  - UI components / graphical objects: >= 3:1

Relative luminance:
    L = 0.2126 * R + 0.7152 * G + 0.0722 * B     (linearized sRGB)

sRGB -> linear:
    if c <= 0.03928: linear = c / 12.92
    else:            linear = ((c + 0.055) / 1.055) ** 2.4

Contrast ratio:
    ratio = (L_lighter + 0.05) / (L_darker + 0.05)
"""

import sys


# ------------------------------------------------------------------
# Palette: verbatim copy of the named colors from core/designsystem/theme/Color.kt.
# The "Color.White" entry corresponds to androidx.compose.ui.graphics.Color.White.
# ------------------------------------------------------------------
PALETTE = {
    # Blue
    "Blue10":  0xFF001F28, "Blue20":  0xFF003544, "Blue30":  0xFF004D61, "Blue40":  0xFF006780,
    "Blue80":  0xFF5DD5FC, "Blue90":  0xFFB8EAFF,
    # DarkGreen
    "DarkGreen10": 0xFF0D1F12, "DarkGreen20": 0xFF223526, "DarkGreen30": 0xFF394B3C, "DarkGreen40": 0xFF4F6352,
    "DarkGreen80": 0xFFB7CCB8, "DarkGreen90": 0xFFD3E8D3,
    # DarkGreenGray
    "DarkGreenGray10": 0xFF1A1C1A, "DarkGreenGray20": 0xFF2F312E,
    "DarkGreenGray90": 0xFFE2E3DE, "DarkGreenGray95": 0xFFF0F1EC, "DarkGreenGray99": 0xFFFBFDF7,
    # DarkPurpleGray
    "DarkPurpleGray10": 0xFF201A1B, "DarkPurpleGray20": 0xFF362F30,
    "DarkPurpleGray90": 0xFFECDFE0, "DarkPurpleGray95": 0xFFFAEEEF, "DarkPurpleGray99": 0xFFFCFCFC,
    # Green
    "Green10": 0xFF00210B, "Green20": 0xFF003919, "Green30": 0xFF005227, "Green40": 0xFF006D36,
    "Green80": 0xFF0EE37C, "Green90": 0xFF5AFF9D,
    # GreenGray
    "GreenGray30": 0xFF414941, "GreenGray50": 0xFF727971, "GreenGray60": 0xFF8B938A,
    "GreenGray80": 0xFFC1C9BF, "GreenGray90": 0xFFDDE5DB,
    # Orange
    "Orange10": 0xFF380D00, "Orange20": 0xFF5B1A00, "Orange30": 0xFF812800, "Orange40": 0xFFA23F16,
    "Orange80": 0xFFFFB59B, "Orange90": 0xFFFFDBCF,
    # Purple
    "Purple10": 0xFF36003C, "Purple20": 0xFF560A5D, "Purple30": 0xFF702776, "Purple40": 0xFF8B418F,
    "Purple80": 0xFFFFA9FE, "Purple90": 0xFFFFD6FA,
    # PurpleGray
    "PurpleGray30": 0xFF4D444C, "PurpleGray50": 0xFF7F747C, "PurpleGray60": 0xFF998D96,
    "PurpleGray80": 0xFFD0C3CC, "PurpleGray90": 0xFFEDDEE8,
    # Red
    "Red10": 0xFF410002, "Red20": 0xFF690005, "Red30": 0xFF93000A, "Red40": 0xFFBA1A1A,
    "Red80": 0xFFFFB4AB, "Red90": 0xFFFFDAD6,
    # Teal
    "Teal10": 0xFF001F26, "Teal20": 0xFF02363F, "Teal30": 0xFF214D56, "Teal40": 0xFF3A656F,
    "Teal80": 0xFFA2CED9, "Teal90": 0xFFBEEAF6,

    # Compose runtime constant
    "Color.White": 0xFFFFFFFF,
}


# ------------------------------------------------------------------
# Schemes: verbatim copy of lightColorScheme(...) / darkColorScheme(...)
# blocks in core/designsystem/theme/Theme.kt.
# ------------------------------------------------------------------
SCHEMES = {
    "LightDefault": {
        "primary": "Purple40", "onPrimary": "Color.White",
        "primaryContainer": "Purple90", "onPrimaryContainer": "Purple10",
        "secondary": "Orange40", "onSecondary": "Color.White",
        "secondaryContainer": "Orange90", "onSecondaryContainer": "Orange10",
        "tertiary": "Blue40", "onTertiary": "Color.White",
        "tertiaryContainer": "Blue90", "onTertiaryContainer": "Blue10",
        "error": "Red40", "onError": "Color.White",
        "errorContainer": "Red90", "onErrorContainer": "Red10",
        "background": "DarkPurpleGray99", "onBackground": "DarkPurpleGray10",
        "surface": "DarkPurpleGray99", "onSurface": "DarkPurpleGray10",
        "surfaceVariant": "PurpleGray90", "onSurfaceVariant": "PurpleGray30",
        "inverseSurface": "DarkPurpleGray20", "inverseOnSurface": "DarkPurpleGray95",
        "outline": "PurpleGray50",
    },
    "DarkDefault": {
        "primary": "Purple80", "onPrimary": "Purple20",
        "primaryContainer": "Purple30", "onPrimaryContainer": "Purple90",
        "secondary": "Orange80", "onSecondary": "Orange20",
        "secondaryContainer": "Orange30", "onSecondaryContainer": "Orange90",
        "tertiary": "Blue80", "onTertiary": "Blue20",
        "tertiaryContainer": "Blue30", "onTertiaryContainer": "Blue90",
        "error": "Red80", "onError": "Red20",
        "errorContainer": "Red30", "onErrorContainer": "Red90",
        "background": "DarkPurpleGray10", "onBackground": "DarkPurpleGray90",
        "surface": "DarkPurpleGray10", "onSurface": "DarkPurpleGray90",
        "surfaceVariant": "PurpleGray30", "onSurfaceVariant": "PurpleGray80",
        "inverseSurface": "DarkPurpleGray90", "inverseOnSurface": "DarkPurpleGray10",
        "outline": "PurpleGray60",
    },
    "LightAndroid": {
        "primary": "Green40", "onPrimary": "Color.White",
        "primaryContainer": "Green90", "onPrimaryContainer": "Green10",
        "secondary": "DarkGreen40", "onSecondary": "Color.White",
        "secondaryContainer": "DarkGreen90", "onSecondaryContainer": "DarkGreen10",
        "tertiary": "Teal40", "onTertiary": "Color.White",
        "tertiaryContainer": "Teal90", "onTertiaryContainer": "Teal10",
        "error": "Red40", "onError": "Color.White",
        "errorContainer": "Red90", "onErrorContainer": "Red10",
        "background": "DarkGreenGray99", "onBackground": "DarkGreenGray10",
        "surface": "DarkGreenGray99", "onSurface": "DarkGreenGray10",
        "surfaceVariant": "GreenGray90", "onSurfaceVariant": "GreenGray30",
        "inverseSurface": "DarkGreenGray20", "inverseOnSurface": "DarkGreenGray95",
        "outline": "GreenGray50",
    },
    "DarkAndroid": {
        "primary": "Green80", "onPrimary": "Green20",
        "primaryContainer": "Green30", "onPrimaryContainer": "Green90",
        "secondary": "DarkGreen80", "onSecondary": "DarkGreen20",
        "secondaryContainer": "DarkGreen30", "onSecondaryContainer": "DarkGreen90",
        "tertiary": "Teal80", "onTertiary": "Teal20",
        "tertiaryContainer": "Teal30", "onTertiaryContainer": "Teal90",
        "error": "Red80", "onError": "Red20",
        "errorContainer": "Red30", "onErrorContainer": "Red90",
        "background": "DarkGreenGray10", "onBackground": "DarkGreenGray90",
        "surface": "DarkGreenGray10", "onSurface": "DarkGreenGray90",
        "surfaceVariant": "GreenGray30", "onSurfaceVariant": "GreenGray80",
        "inverseSurface": "DarkGreenGray90", "inverseOnSurface": "DarkGreenGray10",
        "outline": "GreenGray60",
    },
}


# Pair definitions for the 12 M3 role text pairs plus outline checks.
PAIR_DEFS = [
    ("primary",            "onPrimary",            "primary / onPrimary",                       "text"),
    ("primaryContainer",   "onPrimaryContainer",   "primaryContainer / onPrimaryContainer",     "text"),
    ("secondary",          "onSecondary",          "secondary / onSecondary",                   "text"),
    ("secondaryContainer", "onSecondaryContainer", "secondaryContainer / onSecondaryContainer", "text"),
    ("tertiary",           "onTertiary",           "tertiary / onTertiary",                     "text"),
    ("tertiaryContainer",  "onTertiaryContainer",  "tertiaryContainer / onTertiaryContainer",   "text"),
    ("error",              "onError",              "error / onError",                           "text"),
    ("errorContainer",     "onErrorContainer",     "errorContainer / onErrorContainer",         "text"),
    ("background",         "onBackground",         "background / onBackground",                 "text"),
    ("surface",            "onSurface",            "surface / onSurface",                       "text"),
    ("surfaceVariant",     "onSurfaceVariant",     "surfaceVariant / onSurfaceVariant",         "text"),
    ("inverseSurface",     "inverseOnSurface",     "inverseSurface / inverseOnSurface",         "text"),
]


def hex_to_rgb(v):
    return ((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF)


def srgb_to_linear(c):
    cs = c / 255.0
    if cs <= 0.03928:
        return cs / 12.92
    return ((cs + 0.055) / 1.055) ** 2.4


def relative_luminance(v):
    r, g, b = hex_to_rgb(v)
    return 0.2126 * srgb_to_linear(r) + 0.7152 * srgb_to_linear(g) + 0.0722 * srgb_to_linear(b)


def contrast(v1, v2):
    L1 = relative_luminance(v1)
    L2 = relative_luminance(v2)
    Lmax, Lmin = max(L1, L2), min(L1, L2)
    return (Lmax + 0.05) / (Lmin + 0.05)


def evaluate(ratio):
    """Verdict string for normal vs large/UI text."""
    if ratio >= 7.0:
        return "PASS (AAA)", "PASS"
    if ratio >= 4.5:
        return "PASS", "PASS"
    if ratio >= 3.0:
        return "EDGE-large (fail normal text)", "PASS"
    return "FAIL", "FAIL"


def run_audit():
    rows = []
    for scheme_name, scheme in SCHEMES.items():
        # 12 role pairs: bg = <role>, fg = on<Role>
        for bg_key, fg_key, label, kind in PAIR_DEFS:
            bg = PALETTE[scheme[bg_key]]
            fg = PALETTE[scheme[fg_key]]
            r = contrast(bg, fg)
            normal, large = evaluate(r)
            rows.append({
                "scheme": scheme_name,
                "pair": label,
                "bg_token": scheme[bg_key],
                "fg_token": scheme[fg_key],
                "ratio": r,
                "normal": normal,
                "large_or_ui": large,
                "kind": kind,
            })
        # outline vs background/surface: only the UI-component 3:1 threshold applies.
        for bg_key in ("background", "surface"):
            bg = PALETTE[scheme[bg_key]]
            out = PALETTE[scheme["outline"]]
            r = contrast(bg, out)
            rows.append({
                "scheme": scheme_name,
                "pair": f"outline / {bg_key}",
                "bg_token": scheme["outline"],
                "fg_token": scheme[bg_key],
                "ratio": r,
                "normal": "n/a (UI component)",
                "large_or_ui": "PASS" if r >= 3.0 else "FAIL",
                "kind": "outline",
            })
    return rows


def main(argv):
    fmt = argv[1] if len(argv) > 1 else "table"
    rows = run_audit()

    if fmt == "csv":
        print("scheme,pair,bg_token,fg_token,ratio,normal,large_or_ui")
        for r in rows:
            print(f"{r['scheme']},{r['pair']},{r['bg_token']},{r['fg_token']},"
                  f"{r['ratio']:.4f},{r['normal']},{r['large_or_ui']}")
        return 0

    # Default: pretty table
    fails = [r for r in rows if r["large_or_ui"] == "FAIL"]
    edges_normal = [r for r in rows if r["normal"].startswith("EDGE-large")]

    print(f"TOTAL_PAIRS: {len(rows)}")
    print(f"FAIL (large/UI threshold 3:1): {len(fails)}")
    print(f"EDGE (normal text 4.5 borderline): {len(edges_normal)}")
    print()
    print("--- All pairs ---")
    hdr = f"{'Scheme':14s} {'Pair':48s} {'BG':18s} {'FG':18s} {'Ratio':>7s} {'Normal':30s} {'Large/UI':10s}"
    print(hdr)
    print("-" * len(hdr))
    for r in sorted(rows, key=lambda x: (x["scheme"], x["pair"])):
        print(f"{r['scheme']:14s} {r['pair']:48s} {r['bg_token']:18s} "
              f"{r['fg_token']:18s} {r['ratio']:>7.2f} {r['normal']:30s} {r['large_or_ui']:10s}")

    print()
    print("--- FAIL (sorted by ratio, worst first) ---")
    for r in sorted(fails, key=lambda x: x["ratio"]):
        print(f"  {r['scheme']:14s} {r['pair']:48s} ratio={r['ratio']:.2f}  "
              f"bg={r['bg_token']}  fg={r['fg_token']}")

    print()
    print("--- EDGE / borderline (3.0 <= ratio < 4.5) ---")
    for r in sorted(edges_normal, key=lambda x: x["ratio"]):
        print(f"  {r['scheme']:14s} {r['pair']:48s} ratio={r['ratio']:.2f}  "
              f"bg={r['bg_token']}  fg={r['fg_token']}")

    # Exit non-zero if any FAIL to make CI integration easy.
    return 0 if not fails else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
