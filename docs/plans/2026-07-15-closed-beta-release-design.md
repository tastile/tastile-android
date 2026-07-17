# tastile-android Closed Beta リリース設計 (v0.3.0)

| | |
|---|---|
| 日付 | 2026-07-15 |
| 対象パッケージ | `tastile-android` |
| ブランチ | `2026-07-07-android-parity` |
| 目的 | Google Play **Closed testing** トラックで 12 人 × 14 日 Opt-in 実績を作り、本番昇格要件を満たす |
| スコープ | 最小 — 14 日 Opt-in 維持 + Critical bug 0。テストシナリオ集は作らない |

---

## 1. Context

`app.tastile.android` の Production トラック昇格には、Google Play Console が Closed testing で **14 日連続で 12 人以上のテスター Opt-in** を要求する。新規デベロッパーアカウントの本公開前最低ライン。

現在の `tastile-android`:
- v1 バックエンド移行完了 (2026-07-01)、web と同等の動作
- `2026-07-07-android-parity` ブランチで C1–C4 まで parity 適用済み (2026-07-08 merge)
- Cognito `tastile-v1-users` (ap-northeast-1) は本番構成済み (Hosted UI ACTIVE、`tastile://auth/callback` 登録済)
- CI `release.yml` で `bundleRelease` → `r0adkll/upload-google-play@v1` の流れが既に稼働
- `release.yml` の `track` 選択肢は `internal | beta | production` の3つ。**`beta` を渡せば Play Console の Closed testing トラックにアップロードされる**

## 2. Goals / Non-goals

### Goals
- Closed testing 14 日 Opt-in のカウント開始
- 12 テスター全員が「本番と同じ Hosted UI 自己サインアップ」を通過できることを確認
- ロールバック容易性 (CI 経由での再リリース)

### Non-goals
- C1–C12 全部のパリティ検証シナリオ集は作らない
- 内部テスター (社員/友人) は使わない。テスターは Discord 経由で募る社外ユーザー
- Production トラックへの昇格は本設計の範囲外 (14 日後に別途判断)

## 3. アプローチ: Lean Verified CI

事前検証 → CI release → テスター招待 の 3 段で最短経路を取る。

- 既存の `release.yml` をそのまま使う (新規 CI 追加なし)
- `workflow_dispatch` で `track=beta` を渡し、Closed testing に直接リリース
- 検証で問題が出たら CI 経由で即 `0.3.1` 等をリリース可能

## 4. 配布チャネル

| 項目 | 値 |
|---|---|
| Play Console トラック | **Closed testing** (= `r0adkll/upload-google-play` の `track=beta`) |
| パッケージ名 | `app.tastile.android` |
| テスター母集団 | 社外不特定多数、Discord グループで募集 |
| テスター Opt-in 経路 | Play Console 招待メール → 「Become a tester」リンク → Play Store でインストール |

## 5. バージョン・ビルド

| 項目 | 値 |
|---|---|
| バージョン | `versionCode=31` / `versionName=0.3.0` |
| ブランチ | `2026-07-07-android-parity` (変更なし) |
| コミット | `chore(android): bump versionCode 31 + versionName 0.3.0 for closed testing` |
| ビルド | `./gradlew bundleRelease` (CI ubuntu-latest) |
| 署名 | `ANDROID_KEYSTORE_BASE64` decode → `release` signingConfig (既存の `app/build.gradle.kts` で完結) |
| AAB 出力 | `app/build/outputs/bundle/release/app-release.aab` + `mapping.txt` |
| CI 発火 | GitHub Actions UI の **Run workflow** → `version=0.3.0` `track=beta` |

### 5-1. Play App Signing
アップロード鍵 ≠ アプリ署名鍵の構成は Play Console 側で App Signing を有効にしている場合の挙動。CI アップロード鍵 (`ANDROID_KEYSTORE_*`) と Play Console の Upload key certificate が一致しているか、アップロード前に API で照合する。

## 6. Pre-flight 検証 (リリース前に全件通過)

| # | 検証項目 | 確認方法 | 状態 |
|---|---|---|---|
| 1 | Cognito pool `tastile-v1-users` 存在 | `aws cognito-idp describe-user-pool` | ✅ 確認済 (2026-07-15) |
| 2 | Cognito client `3f14cs42nkc0v3qf6k57gthlfe` の CallbackURL に `tastile://auth/callback` 含む | `describe-user-pool-client` | ✅ 確認済 (2026-07-15) |
| 3 | Hosted UI domain `tastile-v1-app.auth.ap-northeast-1.amazoncognito.com` ACTIVE | `describe-user-pool-domain` | ✅ 確認済 (2026-07-15) |
| 4 | プール `AllowAdminCreateUserOnly: false` (自己サインアップ可) | 同上 | ✅ |
| 5 | CI Secrets 6 件全部存在 | `gh secret list` on `tastile-android` repo | ✅ 確認済 (2026-07-15) |
| 6 | ローカル `build/local-release-upload.jks` SHA-1 ≡ `ANDROID_KEYSTORE_BASE64` decode 結果 | `keytool -list -keystore` × 2 | 未検証 (要実施) |
| 7 | Play Console に `app.tastile.android` アプリ登録済み | Play Developer API (私はアクセス不可な範囲あり) | **ユーザー確認** |
| 8 | Play Console Closed testing トラック設定済み | 同上 | **ユーザー確認** |
| 9 | ストアリスティング (スクリーンショット/説明文/コンテンツレーティング) 整っている | 同上 | **ユーザー確認** |
| 10 | C5–C11 (parity WIP) のコミット | `git status` + `git add` + `git commit` | 未解消 (要実施) — `feat(android): web→android parity sweep (C5–C11)` で同梱 |

## 7. 認証・テスターオンボーディング

### 7-1. テスターが初回ログインで見る流れ
1. アプリ起動 → 「Sign in with Tastile」タップ
2. Custom Tab で `tastile-v1-app.auth.ap-northeast-1.amazoncognito.com` の Hosted UI が開く
3. 「Sign up」タブ → メール + パスワード (12 文字以上、大小英数) を入力
4. 確認メールが届く → リンククリック (Hosted UI 標準の email verification)
5. アプリに戻り再度「Sign in」
6. **MFA SETUP チャレンジが走る** (Cognito MFA=ON) → QR コードを Authenticator アプリ (Google Authenticator / 1Password / Authy) でスキャン → 6 桁コード入力
7. MFA 通過後、id/access/refresh token がアプリに戻り、`EncryptedTokenStorage` に保存
8. ホーム画面到達

### 7-2. ユーザー準備物 (テスター向け Discord 招待文に含めるもの)
- 「Google アカウントに紐づいた Gmail アドレス」(Play Opt-in 用)
- 「Authenticator 系アプリ (Google Authenticator / 1Password / Authy のいずれか)」(MFA TOTP 用)
- 「テスター用 Android スマホ (Android 8.0 / API 26 以上)」(本アプリ minSdk=26)
- 「テスター手順書リンク」(下記 §8 で作成)

## 8. 成果物

| # | ファイル | 役割 |
|---|---|---|
| 1 | `docs/plans/2026-07-15-closed-beta-release-design.md` (本ファイル) | 設計書 (decisions の保管) |
| 2 | `docs/operations/closed-beta-tester-guide.md` | テスター手順書 (Discord 招待文テンプレ / GitHub Issue テンプレ含む) |
| 3 | `app/build.gradle.kts` の `versionCode` / `versionName` bump | コミット `chore(android): bump versionCode 31 + versionName 0.3.0 for closed testing` |
| 4 | C5–C11 (parity WIP) | コミット `feat(android): web→android parity sweep (C5–C11)` — `ui/mobile/account/`, `ui/mobile/panels/{timeline,schedule}/`, `CognitoAccountApi.kt`, `AccountRepository.kt`, Settings/Dashboard/Overlay/SectionPanel 修正, `strings.xml` i18n キー追加 |

## 9. バグ報告チャネル

| 項目 | 値 |
|---|---|
| チャネル | **GitHub Issues** 公開 (本リポジトリ) |
| ラベル | `area: android` / `beta: closed-testing` (本リリースで追加) |
| テンプレ | バグレポート: 再現手順 / Android バージョン / 端末 / スクリーンショット / 期待 vs 実際 |
| トリアージ | プロジェクトオーナー (本リポジトリの admin) |

セキュリティ・個人情報のクラッシュレポートは Issue ではなくオーナーの DM 経由 (招待文に記載)。

## 10. 14 日モニタリング

| Day | アクション |
|---|---|
| 0 | CI release 発火 + Play Console アップロード成功確認 + ユーザー: テスター 12 件追加 + Discord 招待 |
| 1 | テスター招待メールの到達確認、最初の Opt-in / Install 状況確認 |
| 2–13 | GitHub Issues への報告を週次で確認 (Critical 発生時は緊急リリース) |
| 14 | Opt-in 12 件以上残っているか Play Console で確認 |

Play Console の Opt-in カウントは **release が Closed testing トラックに公開された日 (Day 0) から起算**される。テスターが追加されるのは Day 0 より後で OK (Opt-in の継続日数はあくまでトラック公開日基準)。

## 11. ロールバック

| 状況 | 対応 |
|---|---|
| Critical bug 発見 (Crash / Login broken / Data loss) | CI 再 trigger で `versionCode=32 / versionName=0.3.1` を release。Play Console で `0.3.0` を「アーカイブ」(テスターは自動的に `0.3.1` に upgrade) |
| 14 日未満で撤回 | Play Console でリリース取消 + Discord でアナウンス (カウントは Day 0 からリセット) |
| Cognito 側の問題 | ロールバック不可 (Cognito 状態は変えない) — アプリ側で回避 (例: 一時的に AuthRepository を Hosted UI ではなく別経路に切替) |

## 12. リスクと対策

| リスク | 対策 |
|---|---|
| テスターがメール確認リンクを見落とす | Discord 招待文に「Gmail の迷惑メールも確認」と明記、初日アナウンスで周知 |
| テスターが Authenticator アプリを持っていない | 招待文に「事前に Google Authenticator 等をインストール」と明記 |
| Pool MFA=ON による初回ログイン摩擦 | §7-1 の流れをテスター手順書に明記、Discord で質問受付 |
| アップロード鍵 ≠ Play App Signing key | §6-6 で SHA-1 照合、不一致なら鍵差替 (Play Console で upload key reset) |
| 未コミット差分 (V1ApiClientTest.kt) 混入 | §6-10 で解消してからコミット |
| C5–C11 WIP 機能が v0.3.0 に同梱され、未完成画面 (Settings, Account, Timeline panel, Schedule panel) をテスターが踏む | Critical 報告発生時は v0.3.1 で個別修正。Discord で「WIP 機能は未完成あり」と周知 |
| C5–C11 内に Web とのパリティ崩れが残っている可能性 | テスター手順書で「コア (Tiles / Timeline tab / Dashboard / Login) にフォーカス」と明記、Issue ラベル `parity: c5-c11` で優先度管理 |
| `AutoVerifiedAttributes: null` で新規ユーザーが確認メール必須 | Hosted UI の標準動作。テスター手順書に明記 |

## 13. 14 日後の選択肢

| 選択肢 | 条件 |
|---|---|
| Production 昇格 (`track=production`) | 12+ Opt-in 14日 + Critical 0件 |
| Closed testing 継続 (期間延長) | Critical が複数出て追加検証必要 |
| ロールバック / 中止 | Product 判断 (本設計外) |

Production 昇格する場合は別途 `versionName=0.3.0` を bump せず AAB を再アップロード (versionCode 不変)。Play Console 上で Closed testing リリースを「Promote to Production」する選択肢もある。

## 14. 関連ドキュメント

- `docs/operations/release-plan.md` — 既存の内部テスト向けリリースプラン (本設計と並列に参照)
- `docs/operations/closed-beta-tester-guide.md` — 本設計 §8 で作成
- `tastile-core/HARNESS.md` §7 — Cognito 認証 design intent (email+password+emailOTP)
- メモリ: `Cognito email OTP misconfigured (2026-07-06)` / `Cognito MFA=ON forbids EMAIL_OTP first factor`
- parity plan: `docs/plans/2026-07-07-android-content-parity.md` — C5–C11 のスコープ詳細はそちらを参照