---
name: github-delivery
description: Use when managing GitHub Issues, Projects, release branches, Draft PRs, ticket branches, or release integration. Keywords: GitHub, Issue, Project, PR, release, branch, ticket, kanban, sprint.
---
> Source: ~/.config/opencode/skills/github-delivery/SKILL.md vendored on 2026-09-15. Canonical source freshness must be re-checked when reconciling (no explicit version pin).

# GitHub Delivery Skill

## Overview

Development proceeds through target-version release sprints centered on GitHub Issues.

## Branch Model

```
main                              # released/integrated source state
└─ release-<major>-<minor>-<patch>  # active sprint integration
   ├─ 123                           # ticket branch (Issue number only)
   ├─ 124
   └─ 125
```

### Rules
- `main` = released state
- `release-x-y-z` = active sprint
- Ticket branch name = Issue number only (no `issue/`, slug, title, type prefix)
- Branch naming explanation responsibility is on Issue/PR, not branch name

## GitHub Issue

Independent, plannable, implementable, reviewable work items → Issue.
- Title/body: Japanese
- Fields: purpose, acceptance criteria, scope/non-scope, dependency, priority, size, area/component, target version, release date
- Short-lived nested subtasks → Supervisor task (not Issue)

## GitHub Projects / Kanban

Minimum columns:
`Backlog → Ready → In Progress → In Review → Done`

Recommended fields:
- Priority, Size, Target Version, Area/Component, Blocked/dependency

WIP limit = actual capacity.

## Ticket Branch & Draft PR

1. One top-level Issue → one durable ticket branch (`<issue-number>`)
2. After meaningful first commit → create Draft PR from ticket branch to `release-x-y-z`
3. PR title/body/review: Japanese

### Draft → Ready Conditions
- Acceptance criteria implemented
- Ticket integration gate passed
- Blocking issues resolved or scope-out documented
- PR description matches current state
- Stale base / conflict resolved
- Latest checkpoint consistent with branch state

### Ticket Done
- Required CI/checks green
- Blocking review resolved
- PR merged into release branch
- Issue closed
- Project status = Done

## Release Integration

After sprint tickets merged to release branch:
1. Run release gate
2. Create release PR: `release-x-y-z → main`
3. PR includes: goal, included Issues/PRs, breaking changes, migration notes, validation results, known limitations, version metadata
4. After merge: `main` = that version's released state
