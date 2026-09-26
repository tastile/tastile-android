---
name: design-refinement
description: Use before planning any non-trivial feature, architecture, or product design: evidence-first design, unknown decomposition, scope-risk adjustment, trade-off documentation. Keywords: design, refinement, trade-off, scope, unknown.
---

# Design Refinement

## Overview

No non-trivial plan starts from a blank page. Design refinement is the
mandatory gate between "we want this" and "here is the plan": gather the
evidence that already exists, separate facts from open decisions, shrink or
re-scope around the unknowns, and record the trade-offs so the plan's readers
never have to guess why an option was rejected. A plan that skips this gate
is a guess with formatting.

## When to use

- Before sprint planning or any weekly-release plan that contains feature,
  architecture, or product-surface work.
- Before writing an ADR, a technical proposal, or a multi-ticket Issue.
- When an existing design meets new evidence (a failed spike, a changed API
  contract, a review objection) and must be re-refined.

The gate may be skipped only for trivial changes: typos, string-resource
fixes, and localized bug fixes whose blast radius is one screen and whose
fix is evident. Everything else passes through it.

## Procedure

### 1. Collect the evidence first

1. Read the directly relevant sources before proposing anything: the
   neighboring implementation files, applicable ADRs, the skills that govern
   the area (design system, navigation, testing, correctness), and the
   official guidance (Material 3, Android documentation, library docs).
2. Note the repo's hard constraints that bind the design: the
   `verifyDesignSystemImports` and `verifyNoEmbeddedServerSecrets` guards,
   the non-blank `BuildConfig` requirements, min/target SDK levels, and the
   `tastile-core` bridge boundary.
3. Summarize what each source actually says in one line. Paraphrase forces
   understanding; pasted quotes preserve ambiguity.
4. Evidence collection stops when further reading no longer changes the
   option set. Record the sources consulted so reviewers can spot gaps.

### 2. Separate facts from open decisions

1. Build two lists: FACTS (verified in code, docs, or contracts) and OPEN
   (undecided, unconfirmed, or contested). Every design input goes in
   exactly one list.
2. For each OPEN item, name the decision-maker and the cheapest experiment
   or question that would close it (spike, prototype, contract check with
   the backend, design sign-off).
3. Material unknowns are never filled with convenient constants. An OPEN
   item stays open until evidence closes it; the plan routes around it.
4. If the OPEN list dominates the FACTS list, the design is premature.
   Schedule the closing experiments first and re-enter the gate after.

### 3. Adjust scope for risk

1. Order candidate scope by risk-adjusted value: user-visible value divided
   by uncertainty. High-uncertainty items go last or into a follow-up.
2. Define the shippable core: the smallest scope that is useful, verifiable
   with `./gradlew verify`, and releasable alone. Everything beyond the
   core is staged, not promised.
3. Attach each deferred item to an explicit re-entry condition ("revisit
   after the core API contract is signed"), not to a vague "later".
4. Check the adjusted scope against the release workflow
   (`docs/operations/release-workflow.md`): it must fit a weekly cadence
   or be honestly staged across weeks.

### 4. Document the trade-offs

1. For every rejected option, record: the option, why it lost, and what
   evidence would revive it. Future agents must not re-litigate settled
   choices without new facts.
2. Name the load-bearing decisions: the two or three choices the whole
   design rests on (data ownership, navigation shape, state boundary).
   These get the most review attention.
3. State the failure modes accepted knowingly (offline gaps, large-screen
   degradation, older-API fallbacks) and who accepted them.
4. Write the outcome as a durable artifact (Issue, ADR, or plan doc) that a
   reader with no prior context can reconstruct the reasoning from. The
   writing bar is set by the writing-discipline skill.

## Checklist

- [ ] Relevant code, ADRs, skills, and official guidance read and cited.
- [ ] Repo hard constraints (guards, SDK, bridge boundary) accounted for.
- [ ] FACTS and OPEN lists built; every input classified.
- [ ] Each OPEN item has an owner and a closing experiment.
- [ ] No unknown filled with an invented value.
- [ ] Shippable core defined; deferred work has re-entry conditions.
- [ ] Rejected options recorded with revival evidence.
- [ ] Outcome written as a context-independent artifact.

## References

- `docs/operations/release-workflow.md` (weekly release planning gate)
- `docs/operations/project-board.md` (tracking OPEN items to closure)
- `docs/operations/recovery.md` (re-refining after a design failure)
- `./gradlew verify` (verifiability bar for the shippable core)
