---
name: jetpack-compose-audit
description: Audit Android Jetpack Compose repositories for performance, animation phase correctness, state management, side effects, composable API quality, and adjacent Android launch UX resource risks such as blurry Android 12+ splash icons. Scans source code, scores each category from 0-10, writes a strict markdown report, and summarizes the most important fixes. Use when reviewing a Compose codebase, rating repository quality, inspecting recomposition/state issues, animation issues, or running a Compose audit.
---

canonical Skill は `../.agents/skills/jetpack-compose-audit/SKILL.md` である。発火時にその全文を読み、binding workflow として実行する。この adapter に手順を複製しない。

adapter 自身は upstream vendored body を `../.agents/upstream-skills/compose-skill/skills/jetpack-compose-audit/SKILL.md` で参照する (supporting references/ と scripts/ 同梱)。
