---
name: interaction-discipline
description: Use when deciding agent ownership, presenting blockers, escalating with a single question, deferring tangents, routing persistent prose. Keywords: interaction, escalation, blocker, ownership.
---

# Interaction Discipline

## Overview

Every interruption of the user costs attention that could have shipped the
release. This skill governs the agent-user boundary: what the agent owns
outright, how blockers are presented, how escalation asks exactly one
question, when tangents are deferred, and where persistent prose belongs.
The default stance is ownership: the agent resolves everything resolvable
from project evidence and interrupts only for decisions that genuinely
require the user.

## When to use

- Deciding whether to ask the user or proceed autonomously.
- Reporting a blocker, a scope conflict, or a failed assumption.
- Tempted to surface a finding, idea, or side-quest mid-task.
- Writing anything the user is expected to keep (decisions, instructions,
  durable rationale) versus chat that answers and disappears.

## Procedure

### 1. Own what project evidence can decide

1. The agent owns any choice decidable from repo sources: `AGENTS.md`,
   ADRs, skills, existing implementation patterns, operation docs, and
   official platform guidance. Looking it up is faster than asking.
2. Never return a trivially decidable judgment to the user as a question.
   "Which file should this go in" is answered by reading the neighbors,
   not by polling the user.
3. When the agent decides autonomously, it states the decision and its
   evidence in one line so the user can correct it cheaply if wrong.
4. Autonomy stops at the escalation conditions in step 3. Everything short
   of those is owned.

### 2. Present blockers as evidence, not as helplessness

1. A blocker report contains: what was attempted, what the evidence says,
   which alternatives were ruled out and why, and what is needed to
   proceed. "It failed, what should I do" is not a blocker report.
2. Distinguish hard blockers (no legitimate path forward without input)
   from soft blockers (a path exists but carries a trade-off the user
   should ratify). Soft blockers propose a default: "proceeding with X
   unless told otherwise by <time>".
3. Attach pointers (failing test, guard output, contract text) so the user
   verifies in seconds. Never paste a full log when three quoted lines
   plus a pointer suffice.
4. If the blocker dissolves under project evidence while writing the
   report, delete the report and proceed. Writing it down often answers it.

### 3. Escalate with a single question, only on the eight conditions

Escalation is allowed only when at least one of these holds:

1. Canonical contradiction: two binding sources disagree and no precedence
   rule resolves it.
2. Acceptance criteria admit multiple readings with different scope or
   verification outcomes.
3. The next action is irreversible (data loss, published release, key
   rotation, force-push, external send).
4. A public contract is being fixed: API shape, permission model, pricing
   or privacy-visible behavior.
5. Security or privacy impact beyond the already-guarded baseline.
6. Cost increase: new paid service, device farm, seat, or quota usage.
7. Release scope or date change versus the committed plan.
8. The design-first gate fires: non-trivial design work with no refined
   design on record.

The escalation asks exactly one question, states the recommended option
with evidence, names the cost of waiting, and stops all dependent work
until answered. Independent work continues.

### 4. Defer tangents and route persistent prose

1. A tangent is anything valuable but outside the current ticket's
   acceptance criteria. Log it in one line (board note, Issue comment, or
   follow-up ticket) and return to the task. Never chase it inline.
2. Persistent prose (decisions, how-tos, rationale the team must keep)
   belongs in the durable artifact for its type (Issue, ADR, operations
   doc, checkpoint per `docs/operations/recovery.md`), not in chat.
   Chat answers and disappears; artifacts persist.
3. When the user drops a free-form idea mid-sprint, acknowledge in one
   line, file it where it will survive, and continue the committed work.
   Re-scoping mid-sprint requires the release workflow, not chat consent.
4. End each owned stretch with a compact status: done, evidence (tests,
   commit ref), and the single next step. No open-ended "let me know".

## Checklist

- [ ] Choice checked against project evidence before asking.
- [ ] Blocker report has attempts, evidence, ruled-out options, need.
- [ ] Escalation cites one of the eight conditions.
- [ ] Escalation asks exactly one question with a recommendation.
- [ ] Tangents logged in one line, not pursued inline.
- [ ] Persistent prose routed to its durable artifact.
- [ ] Status close-out states done, evidence, and next step.

## References

- `docs/operations/project-board.md` (where tangents and follow-ups land)
- `docs/operations/release-workflow.md` (re-scoping authority)
- `docs/operations/recovery.md` (blocker state for checkpoints)
- `./gradlew verify` (evidence attached to blocker reports)
