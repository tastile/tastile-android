# tastile-android リリースプラン (内部テスト)

## 前提
- Google Play Console アカウント: **未作成** (ユーザーが手動で $25 支払い・登録)
- 署名キーストア: **新規作成**
- リリーストラック: **内部テスト** (最大100人、審査不要、即時配信)

---

## Step 1: 署名キーストア作成 + Release ビルド設定

### 1.1 キーストア生成
```bash
keytool -genkeypair -v \
  -keystore /secure/path/tastile-upload-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias tastile \
  -storepass <STORE_PASSWORD> \
  -keypass <KEY_PASSWORD> \
  -dname "CN=Tastile, OU=Mobile, O=Tastile, L=Tokyo, ST=Tokyo, C=JP"
```
- 出力先: repo 外の安全な場所
- **重要**: キーストアとパスワードを repo に置かないこと

### 1.2 ユーザー環境の `~/.gradle/gradle.properties` に署名情報追加
```properties
RELEASE_STORE_FILE=/secure/path/tastile-upload-key.jks
RELEASE_STORE_PASSWORD=<STORE_PASSWORD>
RELEASE_KEY_ALIAS=tastile
RELEASE_KEY_PASSWORD=<KEY_PASSWORD>
```

### 1.3 `app/build.gradle.kts` に signingConfigs 追加
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("RELEASE_STORE_FILE") as String)
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(...)
        }
    }
}
```

### 1.4 AAB (Android App Bundle) ビルド
```bash
./gradlew bundleRelease
```
出力: `app/build/outputs/bundle/release/app-release.aab`

### 1.5 ビルド検証
```bash
# AAB が生成されたか確認
ls -la app/build/outputs/bundle/release/app-release.aab
```

---

## Step 2: ストアリスティング準備

### 2.1 必要なアセット
- [ ] アプリアイコン 512x512 PNG (ストア用ハイレゾアイコン)
- [ ] フィーチャーグラフィック 1024x500 PNG (任意だが推奨)
- [ ] スクリーンショット 最低2枚 (phone)
  - 内部テストでは最低限でOK

### 2.2 ストア情報
```
アプリ名: Tastile
短い説明: Execution control for intentional work
詳しい説明: Tastile helps you focus on one task at a time with
  execution control. Create tiles, start working, and complete
  tasks with intention. Not a task manager — an execution system.
カテゴリ: 仕事効率化
メールアドレス: <developer email>
プライバシーポリシー URL: https://tastile.app/privacy
```

---

## Step 3: Google Play Console 設定 (ユーザー手動)

### 3.1 アカウント作成
1. https://play.google.com/console にアクセス
2. Google アカウントでログイン
3. $25 の登録料を支払い
4. 本人確認 (数日かかる場合あり)

### 3.2 アプリ作成
1. 「アプリを作成」
2. アプリ名: Tastile
3. デフォルト言語: 日本語
4. アプリ / ゲーム: アプリ
5. 無料 / 有料: 無料

### 3.3 内部テスト設定
1. テスト > 内部テスト > 「新しいリリースを作成」
2. AAB をアップロード
3. テスター追加 (メールアドレスリスト)
4. リリースを公開

---

## Step 4: 仕上げ

### 4.1 .gitignore 確認
```
*.jks
local.properties
```

### 4.2 CLAUDE.md 更新
- リリースビルドコマンド追記
- 署名設定の説明追記

### 4.3 versionCode / versionName 確認
- 現在: versionCode=1, versionName="0.1.0"
- 内部テストとして妥当

---

## 実行者の区分

**Claude が実行**: Step 1 (署名キー生成、Gradle 設定、AAB ビルド)、Step 4
**ユーザーが手動実行**: Step 2 (アセット準備)、Step 3 (Play Console)
