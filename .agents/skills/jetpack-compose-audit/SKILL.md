---
name: jetpack-compose-audit
description: Audit Android Jetpack Compose repositories for performance, animation phase correctness, state management, side effects, composable API quality, and adjacent Android launch UX resource risks such as blurry Android 12+ splash icons. Scans source code, scores each category from 0-10, writes a strict markdown report, and summarizes the most important fixes. Use when reviewing a Compose codebase, rating repository quality, inspecting recomposition/state issues, animation issues, or running a Compose audit.
---

# jetpack-compose-audit (vendored pointer)

> **Upstream**: `hamen/compose_skill` @ `f815c31d6cc1` · Apache-2.0.
> Body: `.agents/upstream-skills/compose-skill/skills/jetpack-compose-audit/SKILL.md`
> Supporting files:
> - `.agents/upstream-skills/compose-skill/skills/jetpack-compose-audit/references/`
>   (scoring, search-playbook, canonical-sources, report-template, diagnostics)
> - `.agents/upstream-skills/compose-skill/skills/jetpack-compose-audit/scripts/`
>   (compose-reports.init.gradle)
>
> Read the upstream file in full when this skill fires. Do not edit inside
> `upstream-skills/`; refresh via `git subtree pull` (see `.agents/upstream-skills/README.md`).

This pointer exists so the canonical-skill surface under `.agents/skills/<name>/SKILL.md` is the single discoverable entry the agent loader matches on. The actual workflow / examples / decision rules are in the upstream file referenced above. `upstream-skills/` is a vendored snapshot pulled in via `git subtree`.

**Tastile note**: Compose compiler reports for this project land in
`app/build/compose-reports/` and `app/build/compose-metrics/`; the baseline is
`docs/superpowers/m3/before-reports/`. When the audit skill asks for a compose
reports init script, point it at the project's standard
`./gradlew :app:assembleDebug` invocation and the on-disk reports path rather
than the vendored init script.
