---
ticket: A04
issue: https://github.com/tastile/tastile-android/issues/7
target_release: 2026-09-19 Free Web + Android
planning_baseline: release-0-6-0 @ 001ab7c0aff885afe351045ce2e9a9650530c4d5
canonical_spec_sha: b0c781dc18111645324e2abc38621b1564d4c518
canonical_spec_tag: v1.0.0
date: 2026-09-12
---

# A04: Android CI で固定 OpenAPI を生成検証する

## 目的

`./gradlew verify` を **R01 由来の canonical OpenAPI v1.0.0** で必ず実行する。`ref: main`
のように動く branch HEAD を checkout せず、凍結 SHA を直接 checkout することで
「ビルドが指す OpenAPI = 配布された OpenAPI」を一致させる。

## 受入条件 (Issue #7)

- [ ] R01 の正本SHA を clean CI で取得する
- [ ] codegen と Verify を developer 固有 path なしで通す
- [ ] 同時に未実装 API を追加しない

## 触るファイル (2 件のみ)

| File | Before | After |
| --- | --- | --- |
| `.github/workflows/verify.yml` | `ref: main` (line 52) | `ref: b0c781dc18111645324e2abc38621b1564d4c518` |
| `.github/workflows/release.yml` | `ref: main` (line 135) | `ref: b0c781dc18111645324e2abc38621b1564d4c518` |

## 触らないファイル

- `app/build.gradle.kts` — generator 設定は固定しない (gradle property `openapi.input`
  の path を CI 側で差し替える方針を維持)
- `app/gradle.properties` — Android 側 canonical は `../../openapi/openapi.yaml`
  (workspace submodule)。CI では `../tastile-openapi/openapi.yaml` を使用。
- `.gitmodules` — workspace shell submodule は触らない
- `tastile-openapi/openapi.yaml` — spec 内容には触らない (C07 凍結済み)

## 依存と確定状況

- R01 (root #1, status:ready) — canonical SHA `b0c781d` 確定、根 submodule にも同期済み
- C07 (private milestone) — `tastile-openapi main @ b0c781d` を v1.0.0 として freeze 確定、
  19 paths / 133 schemas、zero-diff 検証済み (Issue #7 comment)
- Issue #21 (`./gradlew verify` fails at `:app:generateV1Api`) は **PR #22 で path
  解決済み** (tastile-openapi を CI で別途 checkout)。A04 スコープ外。
- Issue #18 (`GOOGLE_ANDROID_CLIENT_ID`) — PR #17 closed 済み。A04 スコープ外。

## 検証

- 1 commit = `fix(ci): pin OpenAPI spec to canonical v1.0.0 SHA`
- ブランチ `7` → Draft PR → `release-0-6-0` base
- CI: `./gradlew verify` が `:app:verifyV1ApiCoverage` まで到達 (21 operationIds / 21 methods — canonical OpenAPI v1.0.0 実数)
- ローカル host 検証は不要 (CI が `ubuntu-latest + JDK 21 + Android SDK` を provision)

## 影響評価

- 既存の main 追従は失われるが、これは **意図した fix** (A04 の主旨)。b0c781d 以降の
  `tastile-openapi` main 変更は本 release では採用せず、次 release で再評価する
- Generate output (`app/build/generated/openapi/v1/`) は同一 pinned spec から再生成される
  (input spec のみ固定であり、byte-for-byte 比較は A04 AC にないため未実施)
- release.yml の validate 順序 (keystore → tag reachability → submodule checkout →
  verify → bundleRelease) は変えない

## 想定される follow-up

- SHA bump policy を docs/release.md に追記 (次 release 着手時)
- Android 側 OpenAPI source の二重管理 (`app/gradle.properties` の `../../openapi/...`
  vs CI の `../tastile-openapi/...`) 解消は別 ticket