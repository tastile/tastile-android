---
name: quality-gate
description: Use when running quality validation, determining verification levels, setting up CI/CD checks, or assessing test adequacy. Keywords: quality, gate, test, lint, typecheck, verify, CI, check, validation, coverage.
---
> Source: ~/.config/opencode/skills/quality-gate/SKILL.md vendored on 2026-09-15. Canonical source freshness must be re-checked when reconciling (no explicit version pin).

# Quality Gate Skill

## Overview

Quality gates are project-specific, not a fixed global bundle. Detect the project's language/framework/runtime/SDK and compile appropriate verification from current official guidance.

## Verification Taxonomy

| Level | Scope | When Required |
|-------|-------|---------------|
| Unit | Local logic/component behavior | Always for logic changes |
| Smoke/Connectivity | Startup, wiring, DI, critical path entry | Runtime/env/DI changes |
| Integration | Multi-component data flow, transaction, persistence | API/service/DB changes |
| Contract/Schema | API/event/DB interface compatibility | Interface changes |
| E2E/System | User/system critical flow | Release, auth, navigation |
| Manual/Automation | UI/native/hardware gaps | When automation insufficient |

## Risk-Based Verification Mapping

| Change Type | Required Levels |
|-------------|----------------|
| Pure logic | unit |
| API/service | unit + integration |
| DB/schema/migration | integration + schema + smoke |
| Runtime/env/network/DI | smoke + relevant integration |
| User journey/auth/navigation | integration/contract + E2E |
| Build/package/container | build + smoke |
| Release | full integration + critical E2E/smoke + release checks |

## Quality Profile Detection

On project init, detect:
1. Language + version
2. Framework + runtime + SDK versions
3. Test framework + config
4. Linter/formatter/type-checker
5. Build system
6. CI/CD platform
7. Existing quality config

Compile from:
1. Framework/runtime/SDK official quality/testing guidance
2. Official examples/templates/starters
3. Official first-party CI guidance
4. Official language/toolchain guidance
5. Coherent existing configuration
6. Maintained ecosystem tooling
7. Custom tooling

## Validation Rules

- Local gate and CI gate use same deterministic entry point
- Stale validation results are never reused
- Partial green ≠ full pass
- False green is forbidden (skipped tests, `.only`, `|| true`, blanket suppression, CI disabling)
- Coverage is project-specific signal, not blind threshold
