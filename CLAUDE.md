# CLAUDE.md

Claude Code (claude.ai/code) 向け harness-specific adapter。
canonical contract は `AGENTS.md` および workspace root の `../AGENTS.md`
を参照すること。本ファイルは重複を避け、Claude Code 固有の anchor のみを
保持する。

## Canonical Contract (harness-specific)

- Build / Verify / Architecture / Toolchain / Working Rules は `AGENTS.md` を
  canonical として参照する。Codex と Claude Code 双方で同じ内容を読む
  必要がある。
- workspace-wide contract は `../AGENTS.md` を参照する (cross-repo change の前に
  必ず読む)。

## Recovery Anchor (Claude Code 固有)

context 消失 / session expiry / sandbox recreation 時に発火する。
canonical Skill: `../../.agents/skills/recover-task/SKILL.md` (workspace root)。
本 repo の adapter: `.claude/skills/recover-task/SKILL.md` (canonical への pointer のみ)。

## Skill Adapter Resolution

Claude Code の Skill は `.claude/skills/<name>/SKILL.md` から解決される。
canonical Skill は `.agents/skills/<name>/SKILL.md` または workspace の
`../../.agents/skills/<name>/SKILL.md`。
adapter は pointer stub のみで workflow 本文を持たない。
drift 検出は `scripts/ci/sync-skill-adapters.sh` で enforce (`:app:check` に配線)。

## `.claude/` 配下の責務 (harness-specific)

- `settings.json`: PreToolUse:Bash hook (`bun .claude/hooks/git-guard.mjs`) +
  permissions.deny / ask (Build-Time Hard Requirements invariants mirror)。
- `hooks/git-guard.mjs`: 破壊的 command の検出。
- `skills/`: canonical Skill への pointer adapter。

Build-Time Hard Requirements (BuildConfig 非空 / no `disable +=` /
no embedded secrets / `m2-allow:` marker 必須) は Gradle 側で enforce
(`verifyDesignSystemImports` / `verifyNoEmbeddedServerSecrets` /
`verifySkillAdapterDrift`) されており、Claude Code harness は冗長に同じ
invariant を permissions 経由で mirror している。

## Cross-references

- `AGENTS.md` (canonical)
- `../AGENTS.md` (workspace contract)
- `.claude/skills/` (Claude Code adapter)
- `.agents/skills/` (canonical Skills)
- `scripts/ci/sync-skill-adapters.sh` (adapter drift 検出)