---
name: parallel-orchestration
description: Use when decomposing complex Issues into parallel worker tasks, managing subagent lifecycle, integrating immutable results, or coordinating multi-ticket release sprints. Keywords: parallel, orchestrate, decompose, subagent, worker, coordinate, sprint.
---
> Source: ~/.config/opencode/skills/parallel-orchestration/SKILL.md vendored on 2026-09-15. Canonical source freshness must be re-checked when reconciling (no explicit version pin).

# Parallel Orchestration Skill

## Overview

Decompose non-trivial Issues into dependency graphs, maximize safe parallelization, and coordinate workers through immutable snapshot/result transfers.

## Issue Decomposition

For each Issue, produce a task graph where each node has:
- `issue_id` — GitHub Issue number
- `objective` — what to achieve
- `acceptance_criteria` — verifiable completion conditions
- `prerequisites` — dependency list (Issue IDs)
- `input_snapshot` — base SHA, branch, relevant file list
- `output_contract` — expected result format (commit ref, diff, validation)
- `owner_role` — worker, reviewer, coordinator
- `target_release` — `release-x-y-z`
- `integration_target` — release branch
- `recovery_policy` — checkpoint frequency and strategy

## Parallelization Rules

1. Ready nodes with resolved dependencies → spawn workers in parallel
2. Maximum parallel = min(resource_capacity, WIP_limit, cost_budget)
3. Same file edit is NOT a serializaton requirement in isolated sandboxes
4. Same interface non-compatible change, same generated artifact, same external mutable resource → dependency or additional isolation needed
5. Workers must not share mutable state

## Worker Spawn Protocol

Each worker receives:
- Issue reference + acceptance criteria
- Immutable input snapshot (base SHA, branch state)
- Allowed tools list
- Filesystem/network policy
- Budget/timeout/maximum depth
- Expected result format
- Parent execution generation (for fencing)

## Result Integration

Workers return immutable results:
```
agent_id, issue_id, base_snapshot, execution_generation,
result_commit_or_ref, summary, validation_results,
known_issues, artifacts
```

Coordinator/Supervisor inspects, integrates, rejects, or requests revision.

## Recovery on Worker Failure

1. Check worker last checkpoint
2. Re-spawn with same input snapshot + execution_generation
3. Do not retry stale results
4. Update integration plan if dependency graph changed
