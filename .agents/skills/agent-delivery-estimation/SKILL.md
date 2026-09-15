---
name: agent-delivery-estimation
description: Use when estimating release dates, roadmaps, milestones, or capacity effects of adding agents. Keywords: estimation, forecast, work unit, throughput, capacity, milestone.
---

# Agent Delivery Estimation

## Overview

Delivery dates for agent-driven work are derived from evidence, never from
intuition. A date is the output of a calculation whose inputs are: decomposed
Work Units, the Issue dependency graph, measured agent throughput, and the
non-agent waits in the pipeline (human review, CI, external dependencies,
usage limits). If any input is missing, the estimate must say so explicitly
instead of filling the gap with a comfortable constant. Subjective day-count
guesses ("this feels like three days") are forbidden: they hide the critical
path, they do not survive contact with review queues, and they cannot be
re-checked when scope moves.

## When to use

- Committing to a release date, milestone, or roadmap slot.
- Answering "what happens to the date if we add scope / agents / tickets".
- Sizing a weekly sprint plan before tickets are assigned.
- Challenging an existing estimate that has no recorded basis.

Do not use this skill for in-flight status updates ("are we on track"). That is
progress tracking, not estimation, and it follows `docs/operations/project-board.md`.

## Procedure

### 1. Decompose scope into Work Units

1. Break the scope into Work Units: each unit is one Issue-sized piece of
   work with its own acceptance criteria and its own verification step
   (normally `./gradlew verify` or a stated subset for `:app` changes).
2. A Work Unit is atomic for estimation: it is either done (acceptance
   criteria met on `main`) or not done. No percentage-complete credit.
3. Size each unit in the smallest honest grain available: completed
   vs remaining. Never convert "complexity points" into days with a
   fixed multiplier unless that multiplier was measured on this repo.
4. Record unknowns per unit (missing spec, unconfirmed API contract,
   no local reproduction). Unknowns stay attached to the unit; they are
   resolved by evidence, not averaged away.

### 2. Build the Issue dependency graph

1. List every Work Unit as a node with its blocking edges: "B cannot start
   (or cannot be verified) until A lands".
2. Identify the critical path: the longest dependency chain. The critical
   path sets the floor of the schedule; parallelizable units only fill
   around it.
3. Flag fan-in nodes (many units blocked on one review, one contract, one
   external approval). Fan-ins are schedule risks even when each unit is
   small.
4. Parallel-agent capacity can only shorten non-critical-path work. Adding
   agents never shortens a strictly serial chain. Assuming linear speedup
   from agent count is forbidden.

### 3. Measure throughput, do not assume it

1. Throughput is Work Units completed per sprint (or per day), measured from
   the project board history (`docs/operations/project-board.md`), not from
   vendor benchmarks or model-speed claims.
2. Use the trailing observed rate, discounted for review overhead: every
   agent-produced unit still costs human review time and a `verify` run.
3. If there is no local history (new repo, new workflow), report throughput
   as unknown and mark the whole estimate conditional (see step 5).
4. Re-measure after each sprint. An estimate older than one sprint is stale
   and must be recomputed, not defended.

### 4. Add non-agent waits and hard limits

1. Human review latency: reviewers are a serial resource. Count open PRs waiting on the same reviewers as queue time on the critical path.
2. CI and verification time: `./gradlew verify` from a clean state plus instrumented-test or device-farm waits where the plan requires them.
3. External waits: store review, backend contract sign-off, design sign-off,
   security review. Each gets an owner and a latest-answer date.
4. Usage and capacity limits: model usage caps, seat limits, device-lab
   availability. A plan that exceeds a known cap is infeasible, not
   "stretch".

### 5. Report the verdict honestly

Every estimate ends in exactly one of three states:

- `complete`: all inputs evidenced (units, graph, measured throughput,
  waits bounded). A date range may be stated with its assumptions listed.
- `conditional`: stated as "date X IF conditions A, B hold". List each
  condition with its owner and the re-check date.
- `unavailable`: material inputs are unknown (no throughput history,
  undecided scope, unconfirmed external dependency). Say what evidence
   would unlock the estimate and who provides it.

Never plug an arbitrary conservative number into an unknown to force a
`complete` verdict. A padded guess is still a guess, and it converts an
honest unknown into a false commitment.

## Sprint-cadence note

The one-week sprint is a planning cadence: a rhythm for re-measuring
throughput and re-computing the forecast. It is not a duration guarantee
for any ticket. "One sprint of work" means "revisit after one week of
measured progress", not "done in seven days".

## Checklist

- [ ] Scope decomposed into acceptance-criteria-sized Work Units.
- [ ] Dependency graph drawn; critical path identified.
- [ ] Throughput taken from board history or marked unknown.
- [ ] Review, CI, external, and cap waits added to the path.
- [ ] No linear agent-count speedup assumed.
- [ ] Verdict is one of complete / conditional / unavailable.
- [ ] No unknown was filled with an invented conservative value.

## References

- `docs/operations/release-workflow.md` (release planning entry point)
- `docs/operations/project-board.md` (throughput source of truth)
- `docs/operations/recovery.md` (re-planning after a missed estimate)
- `./gradlew verify` (per-unit verification cost included in the plan)
