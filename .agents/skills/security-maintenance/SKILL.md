---
name: security-maintenance
description: Use when handling framework/runtime security advisories, dependency vulnerabilities, or security-related code changes. Keywords: security, vulnerability, advisory, CVE, dependency, scan, secret.
---
> Source: ~/.config/opencode/skills/security-maintenance/SKILL.md vendored on 2026-09-15. Canonical source freshness must be re-checked when reconciling (no explicit version pin).

# Security Maintenance Skill

## Overview

Framework/runtime/SDK/dependency security information is tracked continuously, tied to actually-used versions.

## Advisory Source Priority

1. Official framework/runtime/SDK security advisory
2. Official release/security announcement
3. Ecosystem official advisory source
4. GitHub Security Advisories / dependency alerts
5. Maintainer patch information
6. Trusted secondary source

## Triage Priority Factors

Beyond severity, evaluate:
- Exploitability
- Project reachability
- External exposure
- Required privilege
- Impact
- Fix availability
- Workaround quality
- Regression risk
- Release timing

## Remediation Workflow

1. Meaningful advisory → GitHub Issue with target release assigned
2. Critical exposed vulnerability → may interrupt current sprint for patch release
3. Apply fix, verify no regression, update dependency, re-run quality gate

## Security Checks on Init

- Dependency review (if available)
- Code scanning (if available)
- Secret scanning (if available)
- Container scanning (if applicable)
- SBOM generation (if applicable)

## Secret Handling

- Never commit secrets to repository, checkpoint, commit, log, or agent result
- Use `.env.example` / `.env.development.example` / `.env.production.example` committed as reference
- Actual `.env`, `.env.production`, lockfiles → gitignore
- Secrets in snapshots/checkpoints/logs → immediate redaction
