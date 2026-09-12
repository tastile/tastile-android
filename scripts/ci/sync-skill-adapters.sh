#!/usr/bin/env bash
# sync-skill-adapters.sh — detect drift between `.claude/skills/` (Claude Code
# adapter stubs) and `.agents/skills/` (canonical Skills). Wired into
# `app/build.gradle.kts:verifySkillAdapterDrift`.
#
# Exits 0 when every Skill under `.agents/skills/` has a matching
# `.claude/skills/<name>/SKILL.md`, every adapter is named identically to its
# canonical Skill, and no orphan adapter exists without a canonical. Exits 1
# when any drift is detected, printing the diff to stdout.
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
    if [[ ! -f "${canonical}" ]] \
      && [[ ! -f "${WORKSPACE_CANONICAL_DIR}/${adapter_name}/SKILL.md" ]]; then
      violations+=("orphan-adapter: ${adapter_name} (.claude/skills/${adapter_name} has no canonical Skill in .agents/skills/ or ../../.agents/skills/)")
    fi
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