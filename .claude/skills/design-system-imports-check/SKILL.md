---
name: design-system-imports-check
description: Use when adding a new Compose screen under `app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account}/` or when reviewing such a change before commit.
---

canonical Skill は `../.agents/skills/design-system-imports-check/SKILL.md` である。発火時にその全文を読み、binding workflow として実行する。この adapter に手順を複製しない。

adapter 自身は `app/build.gradle.kts:157-195` (verifyDesignSystemImports task) を参照する。
