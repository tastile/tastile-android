---
name: sandbox-runtime
description: Use when setting up isolated execution environments, managing worker runtime isolation, or configuring cross-platform portability. Keywords: sandbox, runtime, isolation, container, docker, nix, devcontainer, portability.
---
> Source: ~/.config/opencode/skills/sandbox-runtime/SKILL.md vendored on 2026-09-15. Canonical source freshness must be re-checked when reconciling (no explicit version pin).

# Sandbox Runtime Skill

## Overview

Each implementation worker gets an isolated mutable runtime. Immutable/cacheable state is shared; mutable state is isolated.

## Isolation Requirements

Per worker, isolate:
- Checkout / writable workspace
- Process boundary
- Network namespace or port mapping
- Database state
- Redis/cache/queue state
- Application local state
- Test artifacts
- Mutable build output

Same internal port across different sandboxes is fine.

## Shared (Read-Only/Immutable)

- Read-only base image
- Immutable Nix store
- Package download cache
- Cargo registry cache
- OCI layer cache
- Read-only toolchain cache

## NOT Shared

- Writable application DB
- Concurrently-mutated dependency/build directory
- Generated runtime files
- Git index / working tree
- Host Docker socket
- Shared dev-server process

## Cross-Platform Portability

No vendor mandatory. First-class local targets:
- macOS / Apple Silicon
- Windows 11 + WSL2 / WSL Containers
- Linux

Evaluate for provider selection:
- Create/destroy cost
- Snapshot persistence
- Suspend/resume
- Provider-loss recovery
- Remote artifact durability

## Container Definitions

Use `Containerfile` (not `Dockerfile`) for new definitions.

## Worker Spawn Checklist

Before spawning worker:
1. Verify sandbox is clean (or restore from checkpoint)
2. Inject credentials via Supervisor (not directly to worker)
3. Set resource/WIP/cost budgets
4. Confirm execution generation/fencing
5. Verify no fork bomb / unbounded cost paths
