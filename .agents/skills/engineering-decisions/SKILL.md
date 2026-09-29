---
name: engineering-decisions
description: Use when resolving conflicts between design sources, making architectural choices, or determining whether to escalate to user. Keywords: decision, precedence, architecture, escalate, policy, design, ADR.
---
> Source: ~/.config/opencode/skills/engineering-decisions/SKILL.md vendored on 2026-09-15. Canonical source freshness must be re-checked when reconciling (no explicit version pin).

# Engineering Decisions Skill

## Decision Precedence

When making engineering decisions, check in this order:

1. **Project-wide policy / canonical architecture / invariant**
2. **Design / specification / explicit task instruction**
3. **Coherent existing implementation majority**
4. **Current official framework/runtime/SDK guidance**
5. **Established ecosystem convention**
6. **Local best judgment**

Higher level overrides lower. Same level: more specific + newer canonical source wins.

## Convention Detection

When checking conventions:
- Look at multiple files with the same responsibility
- Exclude generated/vendor/example code and migration-in-progress old patterns
- Do not treat first-found file as project convention

## Auto-Judge Criteria

Proceed independently when:
- Precedence yields unique or effectively unique answer
- Change is reversible and local
- Acceptance criteria unchanged
- Public/external contract not newly determined
- Security/privacy/cost/release scope not materially changed

Do NOT return A/B questions to user when project evidence resolves it.

## User Escalation Required

Escalate when:
- Canonical sources contradict AND product semantics change
- Acceptance criteria ambiguous AND user-visible behavior changes
- Irreversible/destructive operation
- Public/external API contract determination
- Security/privacy/compliance risk acceptance
- Meaningful cost increase
- Release scope/date change
- Explicit design-first approval gate

When escalating: verify facts first, then present options + impact + recommendation.
