---
name: openapi-spec-refresh
description: Use when the cross-repo OpenAPI submodule at `../../openapi/openapi.yaml` is bumped and the Android `V1ApiClient` Retrofit client must be regenerated and the v1 coverage guard re-run.
---

canonical Skill は `../.agents/skills/openapi-spec-refresh/SKILL.md` である。発火時にその全文を読み、binding workflow として実行する。この adapter に手順を複製しない。

adapter 自身は `gradle.properties:openapi.input` (submodule path)、`app/build.gradle.kts:316-344` (generateV1Api task)、`app/build.gradle.kts:357-373` (Moshi post-processor)、`app/build.gradle.kts:386-427` (verifyV1ApiCoverage task) を参照する。
