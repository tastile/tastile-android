---
name: openapi-spec-refresh
description: Use when `app/openapi/v1.json` is changed and the generated Retrofit client must be refreshed and the v1 coverage guard re-run.
---

canonical Skill は `../.agents/skills/openapi-spec-refresh/SKILL.md` である。発火時にその全文を読み、binding workflow として実行する。この adapter に手順を複製しない。

adapter 自身は `app/build.gradle.kts:226-289` (generateV1Api task) と `app/build.gradle.kts:308-349` (verifyV1ApiCoverage task) を参照する。
