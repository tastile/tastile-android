---
name: buildconfig-guard-check
description: Use when adding a new `buildConfigField(...)` to `app/build.gradle.kts` or when reviewing such a change before commit.
---

canonical Skill は `../.agents/skills/buildconfig-guard-check/SKILL.md` である。発火時にその全文を読み、binding workflow として実行する。この adapter に手順を複製しない。

adapter 自身は `app/build.gradle.kts:64-70` (buildConfigField) と `app/build.gradle.kts:472-491` (gradle.projectsEvaluated guard) を参照する。
