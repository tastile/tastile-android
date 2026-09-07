<!--
Release sprint workflow (ADR-0007) — 4 marker を必ず残す。
`release-branch-workflow` Skill と `verify-tastile-change` Skill を
pre-merge に発火する。
-->

## Summary

- 何を / なぜ / どう変更したか (1 段落)

## Issue

- `resolves #<n>` または `part of #<n>`

## Target Release

- `release-x-y-z` または `-` (main 直 commit)

## Branch

- `git rev-parse --abbrev-ref HEAD` の結果
- canonical pattern: `^(?:[0-9]+|release-[0-9]+-[0-9]+-[0-9]+|main)$`

## Execution Generation

- `1` (default) または recovery 後の increment 値 (ADR-0008)

## Validation

- [ ] `./gradlew verify` exited `0`
- [ ] `./gradlew assembleDebug` exited `0` (artifact 変更時のみ)
- [ ] `./gradlew lintDebug` exited `0`
- [ ] `./gradlew testDebugUnitTest --tests <focal>` を focal に実行した
- [ ] `./gradlew connectedDebugAndroidTest` exited `0` (UI / native 変更時)

## Project fields (ADR-0009)

- `priority` / `size` / `target_version` / `area` を Issue 側で更新した
- Status を `In Review` に進めた

## Checks

- [ ] `verify-tastile-change` Skill を発火した
- [ ] `tastile-precommit-review` Skill を発火した
- [ ] 関連 ADR / Skill を更新した (この PR が workflow / canonical contract を変える場合)
- [ ] `docs/HARNESS.md` §n を更新した (この PR が architecture / 手順を変える場合)
- [ ] `SECURITY.md` の報告経路に影響する変更ではない

## Notes

- 関連 Issue / PR / commit への cross-link
- 残存 risk / rollback 手順
- cross-repo 影響 (`tastile-core` / `tastile-web` / `tastile-desktop`)
