# ADR-0014: Linear profile 不採用 (planning control plane は GitHub Projects のみ)

- Status: Accepted
- Date: 2026-09-15
- Scope: `tastile-android/`

## Context

planning control plane を GitHub Projects と optional Linear profile のどちらか
一方に明示し、同じ field を二重 canonical にしてはならない。`linear-release-control`
Skill は Linear 採用時の契約を定義するが、本 repo での採否を決める必要がある。

## Decision

### 1. Linear profile は不採用

本 repo の planning control plane は GitHub Projects のみ (ADR-0009)。
Linear を ticket mirror や status 管理に使わない。

### 2. 将来採用時の契約 (予約)

将来 Linear を optional release planning / health / portfolio control plane と
して併用する場合、`linear-release-control` Skill の契約に従う:

- 同じ field を GitHub Projects と Linear で二重 canonical にしない
- Linear は ticket を全面 mirror しない
- ticket Done 境界は GitHub Issue close のみで成立する
- Linear 側 status の Done/Completed 更新は release 完了時の release-level
  reconciliation で別途行い、個々の ticket Done 境界に含めない

採用時は本 ADR を revise し、対象 field と reconciliation 手順を明示する。
