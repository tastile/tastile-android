---
name: material-3
description: >
  Implement Google's Material Design 3 (Material You) UI system. Primary: Jetpack
  Compose Material3 (MaterialTheme, components, adaptive layout). Also Flutter and
  limited web (@material/web, maintenance mode). Covers tokens, 30+ components, layout,
  theming, M3 Expressive (platform matrix), and accessibility. Use when: "material
  design", "MD3", "material you", "Jetpack Compose", "MaterialTheme", "material
  component", "md3 button".
---

# material-3 (vendored pointer)

> **Upstream**: `hamen/material-3-skill` @ `14385f2bf380` · Apache-2.0.
> Body: `.agents/upstream-skills/material-3-skill/skills/material-3/SKILL.md`
> Supporting references: `.agents/upstream-skills/material-3-skill/skills/material-3/references/`
> (color-system, component-catalog, theming-and-dynamic-color, typography-and-shape,
> navigation-patterns, layout-and-responsive)
>
> Read the upstream file in full when this skill fires. Do not edit inside
> `upstream-skills/`; refresh via `git subtree pull` (see `.agents/upstream-skills/README.md`).

This pointer exists so the canonical-skill surface under `.agents/skills/<name>/SKILL.md` is the single discoverable entry the agent loader matches on. The actual workflow / examples / decision rules are in the upstream file referenced above. `upstream-skills/` is a vendored snapshot pulled in via `git subtree`.

**Tastile note**: this project is M3-unified with a single design system under
`app/src/main/java/app/tastile/android/core/designsystem/`. Before applying raw
Material 3 changes, cross-check `app/src/main/java/app/tastile/android/core/designsystem/`
for the existing component wrappers, and confirm any new token/component is added to
the design system rather than imported directly. The
`design-system-imports-check` build guard (`./gradlew :app:verifyDesignSystemImports`)
forbids direct `androidx.compose.material3.*` imports under `ui/{dashboard,mobile,account}/`
without an `// m2-allow:` marker.
