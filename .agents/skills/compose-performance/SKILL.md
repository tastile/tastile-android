---
name: compose-performance
description: Use when investigating Jetpack Compose recomposition cost, compiler stability reports, skippability, unstable parameters, frame-rate State reads, cross-phase snapshot back-writing, or @ReadOnlyComposable contracts.
---

# compose-performance (vendored pointer)

> **Upstream**: `chrisbanes/skills` @ `948acbbd6c44` · Apache-2.0.
> Body: `.agents/upstream-skills/chrisbanes-skills/skills/compose-performance/SKILL.md`
> Read the upstream file in full when this skill fires. Do not edit inside `upstream-skills/`; refresh via `git subtree pull` (see `.agents/upstream-skills/README.md`).

This pointer exists so the canonical-skill surface under `.agents/skills/<name>/SKILL.md` is the single discoverable entry the agent loader matches on. The actual workflow / examples / decision rules are in the upstream file referenced above. `upstream-skills/` is a vendored snapshot pulled in via `git subtree`.

**Tastile note**: compose compiler reports land in `app/build/compose-reports/` and
`app/build/compose-metrics/`. The baseline sits at `docs/superpowers/m3/before-reports/`.
