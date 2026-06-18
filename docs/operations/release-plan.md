# tastile-android リリースプラン

Production releases are fully automated via
`.github/workflows/release.yml`. This document is the operator-facing
playbook: what the workflow does, what secrets it reads, how to drive a
release, and how to roll back.

---

## 前提

- **Play Console アカウント**: 登録済み (developer fee paid)
- **署名キーストア**: 1 個。base64 で GitHub Secrets (`ANDROID_KEYSTORE_BASE64`)
  に格納。パスワード・alias・key password は別シークレット
- **リリーストラック**: 内部テスト (`internal`) を既定。`beta` / `production`
  への昇格は `workflow_dispatch` の `track` 入力で指定

---

## ワークフロー全体像

`.github/workflows/release.yml` は tag push (`v*`) または `workflow_dispatch`
で動き、3 つのジョブで構成される:

### Job 1: `build-aab` (ubuntu-latest)

1. このリポジトリをチェックアウト
2. **`tastile-core` を sibling として checkout** — `cargo-ndk` が
   `../tastile-core` を参照するため。`CORE_REPO_READ_TOKEN` が必要
3. JDK 17 (Temurin) セットアップ
4. Rust toolchain + `cargo-ndk` インストール、4 つの Android target を追加
5. `ANDROID_KEYSTORE_BASE64` を base64 デコードし `/tmp/keystore.jks` に展開
6. `local.properties` を Secrets から materialize:

   ```properties
   RELEASE_STORE_FILE=/tmp/keystore.jks
   RELEASE_STORE_PASSWORD=${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
   RELEASE_KEY_ALIAS=${{ secrets.ANDROID_KEY_ALIAS }}
   RELEASE_KEY_PASSWORD=${{ secrets.ANDROID_KEY_PASSWORD }}
   GOOGLE_WEB_CLIENT_ID=${{ secrets.GOOGLE_WEB_CLIENT_ID }}
   ```

7. `./gradlew bundleRelease` で AAB ビルド
8. AAB + mapping.txt を `app-release` という artifact にアップロード

### Job 2: `upload-play` (ubuntu-latest)

`r0adkll/upload-google-play@v1` を使い、artifact をダウンロード → Play
Console にアップロード。track は `workflow_dispatch` 入力 or 既定の
`internal`。status は `completed` で即時公開 (内部テストでは審査不要)。

### Job 3: `upload-github-release` (ubuntu-latest)

同じ AAB + mapping.txt を GitHub Release に attach。Release が未作成なら
`gh release create` で自動生成。

---

## 実行手順

### 通常リリース (version bump + tag)

```bash
# 1. version 更新 (build.gradle.kts の versionName / versionCode)
# 2. コミット
git add app/build.gradle.kts
git commit -m "chore: bump version to 1.2.3"
# 3. タグを打って push
git tag v1.2.3
git push origin main
git push origin v1.2.3
```

CI が自動で `build-aab` → `upload-play` (track=internal) →
`upload-github-release` を実行する。

### Ad-hoc リリース (任意の version で再デプロイ)

GitHub の Actions タブから "Release" ワークフローを選び:

- `version` 入力: デプロイしたいバージョン (例: `1.2.3`)
- `track` 入力: `internal` / `beta` / `production` のいずれか

"Run workflow" を押す。

### 内部テストから production への昇格

Play Console で手動昇格する (Gradle Play Publisher は使っていない)。CI の
責務は「正しい track に AAB を置く」まで。昇格判断は人が行う。

---

## 必要な GitHub Secrets

| Secret                              | 用途                                     |
|-------------------------------------|------------------------------------------|
| `CORE_REPO_READ_TOKEN`              | `tastile-core` の sibling checkout 用 PAT |
| `ANDROID_KEYSTORE_BASE64`           | base64 エンコードされた keystore JKS     |
| `ANDROID_KEYSTORE_PASSWORD`         | Keystore password                        |
| `ANDROID_KEY_ALIAS`                 | Key alias                                |
| `ANDROID_KEY_PASSWORD`              | Key password                             |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`  | Play Console service-account JSON        |
| `GOOGLE_WEB_CLIENT_ID`              | buildConfig 用 (Cognito Hosted UI の OAuth) |
| `GITHUB_TOKEN`                      | 自動付与 (GitHub Release 作成)           |

値の出所とローテーション手順は
[tastile-core/docs/production/secrets-and-deploy.md](https://github.com/tastile/tastile-core/blob/main/docs/production/secrets-and-deploy.md)
に集約。

---

## Play Console 初回セットアップ (一度だけ)

アカウント作成・身元確認・$25 支払いなどのブラウザ操作は人手で行う。CI が
必要とするのは以下:

1. **Service Account 作成** (Google Cloud Console → IAM → Service Accounts)
2. **JSON キーをダウンロード** し、`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` の
   生 JSON として Secrets に登録
3. **Play Console で Service Account にアクセス権を付与** (Setup → API access)
4. **アプリ作成** (Play Console → Create app) — パッケージ名 `app.tastile.android`
5. **トラック作成** (Release → Internal testing → Create track)

これ以降は GitHub Actions からトラックへ直接アップロード可能。

---

## ストアリスティング (Play Console)

アセット・説明文・スクリーンショットは Play Console の UI から管理する
(これらは CI の管轄外):

- アイコン 512x512 PNG
- フィーチャーグラフィック 1024x500 PNG
- スクリーンショット (phone) 最低 2 枚
- 短い説明・詳しい説明
- プライバシーポリシー URL: `https://tastile.app/privacy`

---

## Rollback

Android のロールバックは Play Console 経由で行う:

1. Play Console → 該当リリース → **Release > History**
2. 戻したい revision を選び "Release to <track>" で再公開
3. 内部テストトラックなら即時反映。本番の場合は staged rollout を使う

CI 側でロールバックはしない (Play Console の audit trail が正本)。
mapping.txt は新しいリリースで上書きされるが、Play Console の Deobfuscation
は過去の mapping も保持する。

---

## 実行者の区分

- **GitHub Actions が実行**: AAB ビルド、Play アップロード、GitHub Release
  作成のすべて
- **人が実行 (必要時のみ)**: バージョン bump コミット + tag push、初回の
  Play Console セットアップ、production 昇格判断、ロールバック判断
