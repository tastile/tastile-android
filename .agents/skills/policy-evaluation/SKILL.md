---
name: policy-evaluation
description: Use when evaluating agent policy changes: execution profile, cold review, deterministic vs latent eval, context-budget model, policy regression guard. Keywords: policy, evaluation, regression, context budget.
---

# Policy Evaluation

## Overview

An agent policy (role definitions, workflow rules, quality gates, tool permissions) is load-bearing infrastructure: every agent run executes under it. Changing a policy without evaluation trades a known behavior for an
unknown one across all future runs. This skill defines how to evaluate a
policy change before adopting it, using execution profiles, cold review,
deterministic and latent evals, a context-budget model, and a regression
guard that keeps old guarantees intact.

## When to use

- Editing any rule-like durable text: `AGENTS.md`, skills, ADRs that
  constrain agent behavior, CI quality gates, tool-permission config.
- Widening what agents may do without asking (permissions, auto-approve
  scopes, irreversible operations).
- Tightening a policy after an incident and needing proof the hole closed.
- Retiring or merging policies where overlapping rules may conflict.

Do not use this skill for single-ticket implementation choices; those are
governed by the policy, not evaluations of it.

## Procedure

### 1. Write the execution profile

1. State the policy change in one paragraph: what agents must now do (or may no longer do), and in which situations it fires.
2. List the affected execution paths: which roles, which workflow stages,
   which tools. A policy with no enumerated blast radius is not ready.
3. Predict the observable delta: fewer escalations, slower runs, more
   verification steps, changed artifact shape. Predictions are checked in
   step 4, so write them down before measuring.
4. Record the rollback: the exact revert (commit, previous text) if the
   evaluation fails. No policy change lands without a known undo.

### 2. Run a cold review

1. Ask a reviewer (human or a different agent) who has not seen the
   authoring discussion to read the new policy text alone and paraphrase
   what it requires. Divergent paraphrase means ambiguous text: fix the
   wording, not the reader.
2. The cold reviewer hunts for conflicts with existing canonical rules
   (`AGENTS.md`, sibling skills, ADRs) and for silent permission widening
   (words like "usually", "when convenient", "at your discretion" attached
   to irreversible or externally visible actions).
3. Cold review also checks language rules: source and identifiers stay
   English, durable prose stays reader-oriented, no secrets or credentials
   embedded in examples.
4. Findings block adoption until resolved or explicitly accepted with a
   recorded rationale.

### 3. Separate deterministic evals from latent evals

1. Deterministic evals are machine-checkable and run in CI or locally:
   `./gradlew verify`, lint, guard tasks (`verifyDesignSystemImports`,
   `verifyNoEmbeddedServerSecrets`), permission-config parsing, link
   checks on referenced paths. They must pass on a clean tree.
2. Latent evals are behavioral and need judgment: does the policy produce
   better plans, fewer bad escalations, cleaner artifacts? Evaluate these
   on a fixed sample of past tickets (before/after comparison) with the
   sample and the scoring notes recorded.
3. Never substitute a passing deterministic suite for a missing latent
   eval, or vice versa. A green build does not prove agents escalate less;
   a good review anecdote does not prove the build still passes.
4. For permission-widening changes, add an adversarial latent case: an
   explicit attempt to misuse the new latitude, showing the policy (or a
   surviving guard) still blocks it.

### 4. Model the context budget

1. Estimate the token and attention cost of the policy: new required reads,
   longer procedures, extra verification steps per run.
2. Policies compete for a fixed context budget. A policy that fires rarely
   but costs a full file read on every run must justify itself against
   cheaper triggers (tighter `description`, narrower scope).
3. Prefer pointers over duplication: reference canonical sources by
   repo-root-relative path (`.agents/...`, `docs/...`) instead of copying
   their content into the policy. Duplicated rules drift; pointers do not.
4. If the change grows the budget, name what shrinks to pay for it, or
   record why the growth is acceptable.

### 5. Set the policy regression guard

1. Convert the guarantees that must survive into durable checks: existing
   CI gates keep running, plus any new deterministic check the change
   requires (new guard task, new lint assertion, new review checklist).
2. Record the guard in the policy text or its ADR: what is checked, where
   it runs, and what failure looks like. An unevaluated policy is a policy
   without a guard.
3. After landing, watch the first runs under the new policy for the
   predicted delta from step 1. Missing delta means the policy is inert;
   wrong-direction delta means it is harmful. Either outcome re-opens the
   evaluation.
4. Log the decision (adopted / rejected / adopted-with-conditions) with its
   evidence in the project board or the relevant ADR so a future agent can
   reconstruct why the policy reads the way it does.

## Checklist

- [ ] Execution profile written: change, blast radius, predicted delta.
- [ ] Rollback path recorded before landing.
- [ ] Cold review paraphrase matches author intent.
- [ ] No conflict with existing canonical rules.
- [ ] Deterministic evals pass on a clean tree.
- [ ] Latent evals scored on a recorded ticket sample.
- [ ] Adversarial case tried for permission-widening changes.
- [ ] Context-budget cost modeled and paid for.
- [ ] Regression guard recorded and wired into CI or review.

## References

- `docs/operations/release-workflow.md` (policy landing and rollout)
- `docs/operations/project-board.md` (recording eval outcomes)
- `docs/operations/recovery.md` (rollback when a policy harms runs)
- `./gradlew verify` (deterministic eval baseline)
