---
name: worktree-workflow
description: Use when managing worktree checkouts as a local operation layer on WSL/Linux: branch base discipline, port allocation, sandbox-local use. Keywords: worktree, worktrunk, checkout, ports, WSL.
---

# Worktree Workflow

## Overview

Git worktrees are a local operation layer: extra checkouts of the same
repository that let one sandbox hold several branches at once (a ticket
checkout beside the sprint integration checkout, for example). They are a
Git implementation detail inside an already-isolated sandbox. A worktree is
not isolation by itself: separate checkouts still share ports, processes,
device state, and databases unless those are separated deliberately. This
skill defines how to use worktrees safely on WSL/Linux without mistaking
them for sandboxes.

## When to use

- Setting up parallel local checkouts for ticket work, sprint integration,
  or review of another branch.
- Choosing branch bases and naming for ticket vs sprint checkouts.
- Allocating ports and local services across multiple live checkouts.
- Working from WSL where filesystem placement affects build performance.

Do not use worktrees to share state between agents or to bypass the
sandbox-per-worker rule. Cross-agent coordination goes through commits and
the project board, never through a shared checkout.

## Procedure

### 1. Treat worktrees as in-sandbox detail, never as isolation

1. A worktree inherits the sandbox it lives in: same network namespace,
   same running processes, same local databases and emulators, same
   hardware-bound state. Creating a worktree creates zero new isolation.
2. Before running anything from a second checkout, separate the runtime
   explicitly: distinct ports, no shared Gradle daemon assumptions, no
   concurrent writes to one emulator or device, no shared local database
   files.
3. Never run `./gradlew verify` (or instrumented tests) concurrently from
   two checkouts against the same emulator, device, or build-cache state
   without proving the separation first. Serial verification is the safe
   default.
4. If the task actually needs isolation (untrusted code, conflicting
   toolchains, parallel device tests), request a real sandbox instead of
   adding worktrees.

### 2. Follow branch base discipline

1. Ticket checkouts track their ticket: branch name contains the Issue
   number (`ticket=<issue-number>`), branched from the agreed base
   (normally `main` or the active release branch per
   `docs/operations/release-workflow.md`).
2. Sprint integration checkouts track the sprint (`sprint=release-x-y-z`),
   branched from the same base the release workflow defines. They
   integrate finished ticket branches; they are not a second place to edit
   ticket code.
3. Never base a ticket checkout on another ticket checkout. Stacked local
   branches hide the true merge base and turn one ticket's delay into two.
   Rebase onto the canonical base instead.
4. Before creating the worktree, fetch and confirm the base ref. A
   worktree cut from a stale base starts life as a merge conflict.

### 3. Allocate ports and local services per checkout

1. Assign each live checkout its own ports for anything that listens:
   local API stubs, debug bridges, preview servers, test orchestrators.
   Keep a tiny written allocation (checkout, service, port) where the
   sandbox owner can see it.
2. Android-specific conflicts to check: ADB server port, emulator console
   ports, and any Metro-like or preview relays if the checkout runs them.
   Two checkouts fighting over one ADB server produce failures that look
   like flaky tests.
3. Environment files (`.env*`, `local.properties`) are per checkout and
   never committed. Verify each checkout has its own valid set before
   running builds; a copied checkout with the wrong local config fails in
   confusing ways.
4. Tear down listeners when a checkout goes idle. A forgotten server on a
   shared port breaks the next checkout that assumes the port is free.

### 4. Prefer the WSL Linux filesystem and keep it clean

1. On WSL, keep worktrees on the Linux filesystem (`~` or the distro
   volume), not under `/mnt/c`. Cross-OS file access slows Gradle builds
   dramatically and breaks file-watching assumptions.
2. Keep the worktree list short: active ticket plus sprint integration is
   the normal maximum. Remove merged or abandoned checkouts with
   `git worktree remove` (after confirming no uncommitted work) and prune
   stale metadata with `git worktree prune`.
3. Never place worktrees inside `reference/`, `.build-logs/`, `.tools/`,
   or generated output directories (`app/src/main/jniLibs/`). Those paths
   are read-only, ignored, or build-produced, and a checkout there invites
   accidental commits of files that must never land.
4. Run `git worktree list` at setup and teardown so orphaned checkouts do
   not accumulate silently across sprints.

## Checklist

- [ ] Worktree purpose named (ticket vs sprint integration).
- [ ] No worktree mistaken for process/port/DB isolation.
- [ ] Runtime separation proven (ports, daemons, devices, DB files).
- [ ] Branch base discipline followed (ticket / sprint naming, canonical base).
- [ ] No ticket checkout stacked on another ticket checkout.
- [ ] Port allocation written down; conflicts checked (ADB, emulator).
- [ ] Per-checkout local config valid; nothing committable leaked.
- [ ] Checkouts on the Linux filesystem (WSL); stale ones removed.

## References

- `docs/operations/release-workflow.md` (branch bases and sprint integration)
- `docs/operations/project-board.md` (ticket identity for checkout naming)
- `docs/operations/recovery.md` (checkout state in recovery)
- `./gradlew verify` (serial-by-default verification across checkouts)
