---
name: onboarding
description: Use when creating or improving documentation for fresh contributors or new agents, ensuring project knowledge is repository-controlled. Keywords: onboarding, documentation, README, CONTRIBUTING, agent, fresh, bootstrap.
---
> Source: ~/.config/opencode/skills/onboarding/SKILL.md vendored on 2026-09-15. Canonical source freshness must be re-checked when reconciling (no explicit version pin).

# Onboarding Skill

## Overview

A fresh contributor or fresh agent must be able to start development and recover from context loss using only repository-controlled documentation.

## Required Documentation Coverage

From repository docs, a newcomer must be able to reach:

1. **Project purpose / scope** — what this project is, why it exists
2. **Architecture / dependency direction / data flow / trust boundary** — how it's structured
3. **Bootstrap / run / migrate / seed** — how to get started
4. **Worker/integration/release validation** — how to verify changes
5. **Issue / release branch / ticket branch / Draft PR workflow** — how work flows
6. **Decision precedence** — how decisions are made
7. **ADR / design / Agent Skills** — why decisions were made
8. **Troubleshooting** — common problems and solutions
9. **Release/security/recovery workflow** — operational procedures

## Documentation Structure

Scale documentation to project size:

| Project Size | Minimum Docs |
|-------------|-------------|
| Small | README, CONTRIBUTING |
| Medium | + docs/architecture.md, docs/development.md |
| Large | + docs/troubleshooting.md, docs/release.md, docs/security.md |
| Enterprise | + ADR series, role definitions, governance docs |

## Progressive Disclosure

- Root agent file = dispatcher (project identity, boundaries, SoT pointers)
- Detailed docs in `docs/` directory
- Agent Skills for specialized workflows
- Full initialization prompt only for first-time init or major policy changes

## Visualization

Use Mermaid for:
- Architecture diagrams
- Data flow diagrams
- Trust boundary diagrams
- Dependency graphs

## Maintenance

- Architecture/runtime/workflow changes → same ticket includes documentation update
- Documented commands verified in fresh sandbox/CI
- No contradictions between README, AGENTS, Skills, ADR
