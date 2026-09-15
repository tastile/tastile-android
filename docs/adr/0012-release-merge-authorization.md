# ADR-0012: release PR merge の human authorization 境界

- Status: Accepted
- Date: 2026-09-15
- Scope: `tastile-android/`

## Context

`release-x-y-z -> main` の merge は protected `main` への唯一の正規 delivery
path であり、副作用 (tag、Play upload、GitHub Release) を伴う。agent が自律的に
merge してはならない境界を明示する必要がある。

## Decision

### 1. Agent は ready-to-merge で停止する

Agent は release-wide verification 完了 + release gate green + ready-to-merge
状態まで進めた時点で停止し、現在状態 (head SHA / required checks / outstanding
review conversations) を report する。authorization 取得のためだけに追加の質問
を行わない (permission 確認は user 側の発火に委ねる)。

### 2. Merge 権限は user が保持する

`release-x-y-z -> main` を含む PR merge は explicit user authorization 境界と
する。reviewer / CODEOWNERS approval は merge の前提条件だが、merge を実行する
権限そのものは user が保持する。merge そのものは user が明示的に authorization
した時にのみ実行する。

### 3. 対象範囲

本境界は release PR merge に適用する。通常 ticket PR の release branch への
merge は本境界の対象外だが、irreversible/destructive operation 全般には
`engineering-decisions` Skill の user escalation policy を適用する。
