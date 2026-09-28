---
name: agent-recovery
description: Use when recovering from context/session/sandbox loss, creating checkpoints, or reconstructing task state from durable sources. Keywords: recovery, checkpoint, context loss, session loss, sandbox loss, durable, reconstruct.
---
> Source: ~/.config/opencode/skills/agent-recovery/SKILL.md vendored on 2026-09-15. Canonical source freshness must be re-checked when reconciling (no explicit version pin).

# Agent Recovery Skill

## Overview

AI agent recovery must NOT depend on conversation resume. The canonical path is fresh agent reconstructing from durable project state.

## Failure Model

Prepare for:
- Model/session context loss
- Agent process crash/cancellation
- IDE/terminal restart
- Parent agent crash while child continues
- Child/subagent crash
- Sandbox/container/VM recreation
- Supervisor restart
- Transient network/provider failure
- Host reboot
- Context-window exhaustion

## Durable Recovery Sources (Priority)

1. GitHub Issue / Project
2. Target release branch
3. Ticket branch / commit graph
4. Draft/Ready PR / review / CI state
5. Committed design / ADR / Skills / docs
6. Immutable worker/subagent results
7. Structured recovery checkpoint

**NOT durable**: native conversation ID, agent ID, Supervisor local DB, shell history, IDE state

## Structured Recovery Checkpoint

Save only externally-state necessary for recovery (NOT private chain-of-thought):

```yaml
schema_version: "1"
issue_id: "123"
target_release: "release-0-2-0"
ticket_branch: "123"
pr_number: 456
base_sha: "abc123"
checkpoint_sha_or_snapshot: "def456"
execution_generation: 1
status: "in_progress"
completed_steps:
  - "implemented feature X"
  - "added unit tests"
next_steps:
  - "run integration tests"
  - "create Draft PR"
pending_validation:
  - "integration tests"
active_children: []
integrated_child_results: []
external_side_effects: []
blockers: []
decision_refs:
  - "ADR-0001"
artifact_refs: []
updated_at: "2026-09-07T00:00:00Z"
```

## Checkpoint Triggers

Consider checkpointing before:
- Meaningful implementation milestone
- Risky refactor/migration
- Child spawn
- Child result integration
- Long validation
- External side effect
- User/external input wait
- Provider TTL/shutdown approaching
- Graceful cancellation/shutdown signal
- Context limit approaching

## Soft vs Hard Checkpoints

- **Soft**: same host/sandbox recovery — local immutable ref, filesystem snapshot, Supervisor journal
- **Hard**: sandbox/provider loss recovery — meaningful state reachable from durable remote infrastructure

## Recovery Algorithm

Fresh agent must:
1. NOT guess at previous conversation
2. Identify Issue/PR/target release
3. Fetch ticket branch/remote commit graph
4. Read latest valid checkpoint
5. Confirm canonical policy/design refs
6. Recreate workspace from checkpoint
7. Re-evaluate completed/pending validation
8. Verify actual remote state of external effects
9. Check stale base / conflicting integration
10. Reconstruct remaining plan
11. Safe minimal verification
12. Update execution generation/lease

Even with native resume success, verify branch/PR/checkpoint consistency first.

## Split-Brain Prevention

- Execution generation/fencing tokens per task
- Recovery checks for duplicate active agents
- No automatic merge of stale child results
