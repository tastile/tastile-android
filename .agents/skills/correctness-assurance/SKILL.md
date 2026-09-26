---
name: correctness-assurance
description: Use when deciding how much assurance is enough: invariants, pre/post-conditions, types, static analysis, runtime assertions, tests, property/differential testing, review. Keywords: correctness, invariant, assurance, property testing, verification.
---

# Correctness Assurance

## Overview

This skill answers one question: for this change, what is the minimum set of
assurance mechanisms sufficient to trust it? Assurance is layered. The layers, from cheapest to most expensive, are: precise types, static analysis, runtime assertions, unit tests, property-based and differential tests, integration tests, and human review. The skill starts from the premise that the change
itself already computes the right answer; the work is choosing evidence that
the answer stays right under realistic inputs, refactors, and concurrency.

Over-assurance wastes review and CI budget; under-assurance ships defects.
The output is a deliberate, recorded choice of layers per risk, not "add more
tests just in case".

## When to use

- Before implementing any change with persistent state, money-adjacent
  logic, auth/token handling, scheduling, or cross-process contracts.
- When a reviewer asks "how do we know this is correct" and there is no
  written answer yet.
- When deciding whether a bug fix needs a regression test, an assertion, a
  type change, or all three.
- When flaky or tautological tests suggest the assurance layers are wrong,
  not just the test code.

Trivial localized fixes (typo, string resource, layout padding) need only
the standard gate (`./gradlew verify`); running the full procedure below is
optional for those.

## Procedure

### 1. Extract the invariants and contracts

1. Write down the invariants: statements that must hold before, during, and
   after the change (for example, "an expired token is never attached to a
   request", "an alarm fires at most once per schedule entry").
2. For each touched function or component, state its pre-conditions (what
   the caller guarantees) and post-conditions (what the callee guarantees).
3. Mark which invariants cross a boundary: process, thread/coroutine
   context, persistence, or the `tastile-core` native bridge. Boundary
   invariants are the highest-risk items and drive the rest of the plan.
4. If an invariant cannot be stated plainly, the design is not ready.
   Return to design before choosing assurance layers.

### 2. Push guarantees into types first

1. Prefer making illegal states unrepresentable: sealed classes, non-null
   types, value classes, exhaustive `when`. A compile error is cheaper
   than any test.
2. Check that the chosen types actually constrain callers (a `String` typed
   as an ID is not a guarantee; a dedicated value type is).
3. Record which invariants are now compiler-enforced so later layers do not
   re-prove them with brittle tests.

### 3. Apply static analysis and lint

1. Run the repo's static gates (`./gradlew verify` includes lint and the
   project guard tasks). No lint rule is disabled to make a change pass;
   unaddressable rules go to a tracking doc with a BLOCKED rationale.
2. Treat analyzer warnings on touched files as findings to disposition
   (fix, suppress with justification, or file), not as noise to ignore.
3. For Compose UI, include the compiler stability reports where
   recomposition cost is part of the correctness story.

### 4. Add runtime assertions at trust boundaries

1. Place `check` / `require` at points where data crosses a boundary you do
   not control: intent extras, deep links, notification payloads, native
   bridge DTOs, persisted state read back after upgrade.
2. Assertions must fail fast with a message naming the violated invariant,
   never silently coerce bad input into a plausible wrong value.
3. Do not assert on performance-sensitive hot paths without measuring;
   prefer debug-only or sampled checks there.

### 5. Choose tests by risk, minimum sufficient first

1. Unit tests for pure logic and state reducers: given inputs, assert exact
   outputs and the extracted post-conditions from step 1.
2. Property-based tests where the input space is large and the invariant is
   simple (round-trips, idempotence, ordering, serialization symmetry).
   State the property in one sentence per test.
3. Differential tests where a migration or second implementation exists
   (for example, behavior moving behind `tastile-core`): run old and new
   paths over the same inputs and assert identical observable results.
4. Integration tests (Hilt + Espresso, `connectedDebugAndroidTest`) only for
   behavior that cannot be observed below the framework boundary.
5. Each test names the invariant it protects. A test that passes when the
   invariant is broken is deleted or rewritten, not kept for coverage.

### 6. Send the remainder to human review

1. Whatever no cheaper layer covers (product judgment, security trade-offs,
   public-contract wording) becomes an explicit review question, not an
   implicit hope.
2. The review request lists: invariants, layers applied, layers deliberately
   skipped and why. Reviewers check the reasoning, not just the diff.

## Checklist

- [ ] Invariants, pre-conditions, and post-conditions written down.
- [ ] Boundary-crossing invariants identified and prioritized.
- [ ] Compiler-enforced guarantees chosen before tests.
- [ ] Static gates run with no new suppressions unexplained.
- [ ] Runtime assertions guard untrusted-boundary inputs.
- [ ] Every test names the invariant it protects.
- [ ] Property/differential testing considered and dispositioned.
- [ ] Residual risks converted into explicit review questions.

## References

- `docs/operations/release-workflow.md` (where assurance gates run)
- `docs/operations/project-board.md` (tracking assurance follow-ups)
- `docs/operations/recovery.md` (what happens when assurance missed)
- `./gradlew verify` (unit tests, lint, and guard tasks)
