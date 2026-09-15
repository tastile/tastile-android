---
name: writing-discipline
description: Use when writing durable artifacts (Issues, PRs, ADRs, docs, checkpoints): reader-oriented writing, context-independent reconstruction, Select-Compose-Reread pipeline. Keywords: writing, artifact, documentation, review.
---

# Writing Discipline

## Overview

Durable artifacts (Issues, PRs, ADRs, docs, checkpoints) are read by people
and agents who were not in the room: future maintainers, cold reviewers,
recovery agents reconstructing state after context loss. Reader-oriented
writing means every artifact carries its own context: a stranger can
reconstruct what was decided, why, and what remains without access to the
author's working memory. Anything less is a note to self, not an artifact.

## When to use

- Creating or substantially editing an Issue, PR description, ADR, plan,
  operations doc, or recovery checkpoint.
- Reviewing someone else's (or another agent's) artifact before it lands.
- Converting session output (chat, investigation, spike) into a durable
  record.

Ephemeral chat and in-progress scratch notes are exempt while they stay
ephemeral. The moment text is referenced by other work, it becomes durable
and this skill applies retroactively.

## Procedure

### 1. Select: decide what the reader needs

1. Identify the reader and their task: implementer picking up the Issue,
   reviewer judging the PR, recovery agent rebuilding state, future self
   questioning the ADR.
2. Select the minimal content that lets that reader act: goal, context,
   decision or request, acceptance criteria, pointers to evidence. Exclude
   the investigation travelogue (dead ends, tool output dumps, musings).
3. Choose pointers over copies: reference files by repo-root-relative path
   (`.agents/...`, `docs/...`), commits by hash, Issues by number. The
   artifact stays short and never rots when the source moves.
4. Full-snapshot serialization (dumping entire logs, whole files, full
   environment state) is forbidden unless a reviewer explicitly requested
   it. Leave a pointer; the reader can follow it.

### 2. Compose: build the artifact

1. Structure every artifact the same way: title stating the outcome,
   background in three sentences or fewer, the decision/request/change,
   evidence and pointers, and explicit open items with owners.
2. Acceptance criteria are testable sentences, not aspirations. Prefer
   "alarm fires once per entry; verified by unit test X" over "alarms work
   reliably".
3. Language rules: Issues and PRs in Japanese, commits and source
   (identifiers, comments) in English. Internal durable docs follow the
   repo convention for their type. Never paste secrets, tokens, keystore
   material, or `google-services.json` content into any artifact.
4. Record uncertainty honestly: mark estimates as conditional, unknowns as
   open with owners, rejected options with revival evidence. The artifact
   must not read more certain than the work is.

### 3. Reread: verify before publishing

1. Reread the finished artifact cold: pretend you have no memory of the
   session and check whether each claim is supported by a pointer or a
   criterion in the text.
2. Run the reconstruction test: could a fresh agent implement, review, or
   recover from this artifact alone? If it needs the author's memory, it
   is not done.
3. Check mechanics: links resolve, paths are repo-root-relative (no parent-dir
   chains that break on move), code identifiers match the tree,
   and the language rule from step 2 holds throughout.
4. For checkpoints specifically, confirm alignment with
   `docs/operations/recovery.md`: state, next action, and pointers must let
   a recovery run resume without replaying the session.

## Checklist

- [ ] Reader and their task identified; content selected for them.
- [ ] Title states the outcome; background is three sentences or fewer.
- [ ] Decisions/requests backed by pointers, not pasted snapshots.
- [ ] No unconditional full-snapshot serialization.
- [ ] Acceptance criteria are testable sentences.
- [ ] Issue/PR in Japanese; commit/source references in English.
- [ ] No secrets or credentials anywhere in the text.
- [ ] Cold reread passes the reconstruction test.

## References

- `docs/operations/project-board.md` (Issue writing conventions)
- `docs/operations/release-workflow.md` (PR and release-note writing)
- `docs/operations/recovery.md` (checkpoint writing bar)
- `./gradlew verify` (verification claims cited in artifacts)
