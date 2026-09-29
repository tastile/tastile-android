#!/usr/bin/env bash
# sync-skill-adapters.sh — detect drift between `.claude/skills/` (Claude Code
# adapter stubs) and `.agents/skills/` (canonical Skills). Wired into
# `app/build.gradle.kts:verifySkillAdapterDrift`.
#
# Exits 0 when every Skill under `.agents/skills/` has a matching
# `.claude/skills/<name>/SKILL.md` and no adapter exists without a canonical
# Skill. Exits 1 when any drift is detected, printing the diff to stdout.
#
# Three adapters intentionally keep a stable Claude Code name that differs from
# their canonical Skill, as recorded in AGENTS.md. They are listed in
# ADAPTER_ALIASES below and are checked against their declared canonical target
# rather than being reported as orphans, so renaming a canonical Skill still
# surfaces here.
#
# Run from repo root:
#   ./scripts/ci/sync-skill-adapters.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${REPO_ROOT}"

CANONICAL_DIR="${REPO_ROOT}/.agents/skills"
ADAPTER_DIR="${REPO_ROOT}/.claude/skills"
WORKSPACE_CANONICAL_DIR="${REPO_ROOT}/../.agents/skills"
WORKSPACE_ADAPTER_DIR="${REPO_ROOT}/../.claude/skills"

# adapter name -> canonical Skill name. Keep in sync with the AGENTS.md table of
# Claude Code adapters that keep their stable names.
ADAPTER_ALIASES=(
  "project-board:github-delivery"
  "recover-task:agent-recovery"
  "release-branch-workflow:github-delivery"
)

violations=()

# Each canonical Skill must have a matching Claude Code adapter stub.
if [[ -d "${CANONICAL_DIR}" ]]; then
  for skill_dir in "${CANONICAL_DIR}"/*/; do
    [[ -d "${skill_dir}" ]] || continue
    skill_name="$(basename "${skill_dir}")"
    [[ "${skill_name}" == "upstream-skills" ]] && continue
    adapter="${ADAPTER_DIR}/${skill_name}/SKILL.md"
    if [[ ! -f "${adapter}" ]]; then
      violations+=("missing-adapter: ${skill_name} (canonical at .agents/skills/${skill_name} has no .claude/skills/${skill_name}/SKILL.md)")
    fi
  done
fi

# No orphan adapter without a canonical Skill.
if [[ -d "${ADAPTER_DIR}" ]]; then
  for adapter_dir in "${ADAPTER_DIR}"/*/; do
    [[ -d "${adapter_dir}" ]] || continue
    adapter_name="$(basename "${adapter_dir}")"
    canonical="${CANONICAL_DIR}/${adapter_name}/SKILL.md"
    if [[ -f "${canonical}" ]] || [[ -f "${WORKSPACE_CANONICAL_DIR}/${adapter_name}/SKILL.md" ]]; then
      continue
    fi

    # A stable adapter name may point at a differently named canonical Skill.
    alias_target=""
    for entry in "${ADAPTER_ALIASES[@]}"; do
      if [[ "${entry%%:*}" == "${adapter_name}" ]]; then
        alias_target="${entry##*:}"
        break
      fi
    done

    if [[ -n "${alias_target}" ]]; then
      if [[ -f "${CANONICAL_DIR}/${alias_target}/SKILL.md" ]]; then
        continue
      fi
      violations+=("stale-alias: ${adapter_name} is declared to alias ${alias_target}, but .agents/skills/${alias_target}/SKILL.md does not exist. Update ADAPTER_ALIASES in $(basename "${BASH_SOURCE[0]}") and the AGENTS.md adapter table.")
      continue
    fi

    violations+=("orphan-adapter: ${adapter_name} (.claude/skills/${adapter_name} has no canonical Skill in .agents/skills/ or ../../.agents/skills/, and is not listed in ADAPTER_ALIASES)")
  done
fi

# Workspace canonical Skills reachable from project via parent workspace
# adapter dir (informational when drift exists).
if [[ -d "${WORKSPACE_CANONICAL_DIR}" ]]; then
  for skill_dir in "${WORKSPACE_CANONICAL_DIR}"/*/; do
    [[ -d "${skill_dir}" ]] || continue
    skill_name="$(basename "${skill_dir}")"
    project_canonical="${CANONICAL_DIR}/${skill_name}/SKILL.md"
    project_adapter="${ADAPTER_DIR}/${skill_name}/SKILL.md"
    workspace_adapter="${WORKSPACE_ADAPTER_DIR}/${skill_name}/SKILL.md"
    if [[ ! -f "${project_canonical}" ]] \
      && [[ ! -f "${project_adapter}" ]] \
      && [[ -f "${workspace_adapter}" ]]; then
      : # workspace adapter exists; project reaches it via parent — no violation
    fi
  done
fi

if (( ${#violations[@]} > 0 )); then
  echo "verifySkillAdapterDrift: ${#violations[@]} drift item(s) detected"
  for v in "${violations[@]}"; do
    echo "  - ${v}"
  done
  exit 1
fi

echo "verifySkillAdapterDrift: ok (.claude/skills/ mirrors .agents/skills/)"
exit 0