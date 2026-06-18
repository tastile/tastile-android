# tastile-android 実装プラン

> **Historical note (2026-06):** This document is the original
> Supabase-based implementation plan. The Supabase project was stopped
> on 2026-06-18 and AWS (Cognito + `tastile-core` API) is now the sole
> backend. The Supabase-shaped steps below are kept for history; the
> current architecture is described in
> [`tastile-core/docs/architecture.md`](https://github.com/tastile/tastile-core/blob/main/docs/architecture.md).
>
> The current release/deploy process is automated via
> `.github/workflows/release.yml` — see
> [`docs/operations/release-plan.md`](../operations/release-plan.md)
> for the live flow.

## 現状

プロジェクト骨格は存在する:
- `settings.gradle.kts`, `build.gradle.kts` (root + app) 済み
- `MainActivity.kt` (空の Compose Activity)
- `Theme.kt` (Dynamic Color 対応済み、ただしライトのみ)
- `AndroidManifest.xml` (INTERNET + POST_NOTIFICATIONS)
- Supabase SDK は `3.0.3` (古い) → BOM `3.1.4` に更新必要
- Room / WorkManager は不要 (プランでは使わない) → 削除
- Hilt 未導入 → 追加必要
- `TastileApp.kt` (Application class) 未作成

## ゴール

Web 版 (Next.js) と同じ Supabase DB / Auth / RLS を使い、Android からフル機能を提供する

---

## Step 1: プロジェクト骨格 + Supabase 接続

### 1.1 root `build.gradle.kts` 更新
- Hilt plugin 追加: `id("com.google.dagger.hilt.android") version "2.56.2" apply false`
- KSP plugin 追加: `id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false`

### 1.2 `app/build.gradle.kts` 更新
- plugins に `id("com.google.dagger.hilt.android")` と `id("com.google.devtools.ksp")` 追加
- Supabase 依存を BOM 方式に変更:
  ```kotlin
  implementation(platform("io.github.jan-tennert.supabase:bom:3.1.4"))
  implementation("io.github.jan-tennert.supabase:postgrest-kt")
  implementation("io.github.jan-tennert.supabase:auth-kt")
  implementation("io.github.jan-tennert.supabase:compose-auth")
  implementation("io.ktor:ktor-client-okhttp:3.1.3")
  ```
- Hilt 依存追加:
  ```kotlin
  implementation("com.google.dagger:hilt-android:2.56.2")
  ksp("com.google.dagger:hilt-compiler:2.56.2")
  implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
  ```
- Lifecycle ViewModel Compose 追加:
  ```kotlin
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  ```
- Room / WorkManager の依存を削除
- `buildConfigField` で Supabase URL / Key を設定:
  ```kotlin
  defaultConfig {
      buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL") ?: ""}\"")
      buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("SUPABASE_ANON_KEY") ?: ""}\"")
  }
  buildFeatures {
      compose = true
      buildConfig = true
  }
  ```

### 1.3 `gradle.properties` にキー追加 (.gitignore 対象)
```properties
SUPABASE_URL=<REDACTED — historical Supabase URL>
SUPABASE_ANON_KEY=<REDACTED — historical Supabase anon key>
```
Note: Web版の `NEXT_PUBLIC_SUPABASE_URL` / `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` と同じ値を使う

### 1.4 `data/SupabaseClient.kt` 作成
```kotlin
package app.tastile.android.data

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import app.tastile.android.BuildConfig

object SupabaseClientFactory {
    fun create() = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}
```

### 1.5 `TastileApp.kt` 作成
```kotlin
@HiltAndroidApp
class TastileApp : Application()
```

### 1.6 `di/AppModule.kt` 作成
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideSupabaseClient() = SupabaseClientFactory.create()
}
```

### 1.7 `AndroidManifest.xml` 更新
- `android:name=".TastileApp"` を application に追加

### 1.8 `MainActivity.kt` に `@AndroidEntryPoint` 追加

### 1.9 ビルド確認
```bash
cd tastile-android && ./gradlew assembleDebug
```

---

## Step 2: 認証フロー (Google OAuth)

### 2.1 `data/repository/AuthRepository.kt` 作成
```kotlin
@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient
) {
    val currentSession get() = client.auth.currentSessionOrNull()
    val sessionStatus get() = client.auth.sessionStatus

    suspend fun signInWithGoogle() {
        client.auth.signInWith(Google) {
            // Uses Chrome Custom Tabs
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun handleDeepLink(intent: Intent) {
        client.handleDeeplinks(intent)
    }
}
```

### 2.2 `ui/login/LoginViewModel.kt` 作成
- `sessionStatus` を StateFlow で公開
- `signInWithGoogle()` を呼ぶ関数

### 2.3 `ui/login/LoginScreen.kt` 作成
- ミニマルなログイン画面
- Google Sign-In ボタン1つ
- ロゴ + "Sign in to continue" テキスト

### 2.4 Deep link 設定
`AndroidManifest.xml` に追加:
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="tastile" android:host="auth" android:pathPrefix="/callback" />
</intent-filter>
```

### 2.5 `MainActivity.kt` で deep link をハンドル
```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    // AuthRepository.handleDeepLink(intent)
}
```

### 2.6 認証ガード
- `sessionStatus` を監視
- Authenticated → Now 画面
- NotAuthenticated → Login 画面

---

## Step 3: Now 画面 (タイル CRUD + ライフサイクル)

### 3.1 `data/model/Tile.kt` 作成
```kotlin
@Serializable
data class Tile(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("local_tile_id") val localTileId: String = "",
    val title: String = "",
    @SerialName("next_action") val nextAction: String? = null,
    @SerialName("done_definition") val doneDefinition: String? = null,
    val lifecycle: String = "Ready",
    @SerialName("annotation_conditions") val annotationConditions: JsonObject? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)
```

### 3.2 `data/model/Profile.kt` 作成
```kotlin
@Serializable
data class Profile(
    val id: String = "",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val plan: String = "free",
)
```

### 3.3 `data/repository/TileRepository.kt` 作成
- `getTiles(userId)`: select, filter deleted_at is null, order by updated_at desc, limit 20
- `createTile(userId, title)`: insert with lifecycle=Ready
- `startTile(tileId)`: update lifecycle=Started, updated_at=now
- `completeTile(tileId)`: update lifecycle=Done, updated_at=now
- `deleteTile(tileId)`: update deleted_at=now (soft delete)

### 3.4 `ui/now/NowViewModel.kt` 作成
- `tiles: StateFlow<List<Tile>>`
- `isLoading: StateFlow<Boolean>`
- `loadTiles()`, `createTile(title)`, `startTile(id)`, `completeTile(id)`

### 3.5 `ui/now/NowScreen.kt` 作成
- アクティブタイル (lifecycle=Started) をカード上部にハイライト (green border)
- "Complete" ボタン付き
- タイル一覧 LazyColumn
- 各タイルに lifecycle バッジ (Ready=gray, Started=blue, Done=green)
- Start / Complete ボタン
- 新規作成: 下部に TextField + "Add" ボタン
- Pull-to-refresh

---

## Step 4: Memo + Prompt 画面

### 4.1 `ui/memo/MemoViewModel.kt` 作成
- `recentTiles: StateFlow<List<Tile>>` (直近5件)
- `selectedTileId: StateFlow<String?>`
- `saveMemo(tileId, note)`: annotation_conditions.note に保存

### 4.2 `ui/memo/MemoScreen.kt` 作成
- タイル選択ドロップダウン (直近5件)
- テキスト入力 (auto-focus)
- Save ボタン
- "Saved!" スナックバー

### 4.3 `ui/prompt/PromptViewModel.kt` 作成
- `activeTile: StateFlow<Tile?>` (Started かつ 25分以上経過)
- `elapsedMinutes: StateFlow<Int>`
- `continueTile()`, `takeBreak()`, `completeTile()`

### 4.4 `ui/prompt/PromptScreen.kt` 作成
- 25分以上のアクティブタイル表示
- "You've been working for X minutes" テキスト
- 3つのアクションボタン: Continue / Take Break / Complete
- アクティブタイルがない場合: "No active tiles" メッセージ

---

## Step 5: Account + Settings 画面

### 5.1 `data/repository/ProfileRepository.kt` 作成
- `getProfile(userId)`: profiles テーブルから取得
- `updateDisplayName(userId, name)`: display_name 更新

### 5.2 `ui/account/AccountViewModel.kt` 作成
- `profile: StateFlow<Profile?>`
- `loadProfile()`, `updateDisplayName(name)`, `signOut()`

### 5.3 `ui/account/AccountScreen.kt` 作成
- アバター (avatar_url) - イニシャルアイコンのフォールバック
- メールアドレス (session から)
- 表示名 (編集可能)
- プランバッジ (Free / Pro)
- "Manage Billing" ボタン (→ Billing 画面)
- "Sign Out" ボタン

---

## Step 6: Billing (WebView)

### 6.1 `ui/billing/BillingScreen.kt` 作成
- WebView で `https://tastile.app/pricing` を表示
- URL に `?access_token=XXX` を付与して認証を引き継ぐ
- 戻るボタン対応 (WebView の goBack)
- Pro ユーザーの場合は Stripe Portal URL を取得して開く

---

## Step 7: ナビゲーション + 仕上げ

### 7.1 `navigation/TastileNavGraph.kt` 作成
BottomNavigation 構成:
- Now (メイン、ホームアイコン)
- Prompt (通知アイコン)
- Memo (メモアイコン)
- Account (人物アイコン)

### 7.2 `MainActivity.kt` 更新
- NavHost + BottomNavigation 統合
- Deep link handling
- Auth state に応じた画面切り替え

### 7.3 `ui/theme/Theme.kt` 更新
- ダークテーマ対応追加
- `isSystemInDarkTheme()` に基づく自動切り替え

### 7.4 `ui/theme/Color.kt` 作成
- ライフサイクルカラー定義:
  - Ready: Gray
  - Started: Blue
  - Done: Green

### 7.5 `ui/theme/Type.kt` 作成
- Material 3 Typography カスタマイズ

### 7.6 CLAUDE.md 更新
- 最新の構造を反映

### 7.7 ビルド・動作確認
```bash
./gradlew assembleDebug
```

---

## 依存関係フロー

```
Step 1 (骨格) → Step 2 (認証) → Step 3 (Now) → Step 4 (Memo/Prompt)
                                               → Step 5 (Account)
                                               → Step 6 (Billing)
                              Step 3 + 4 + 5 + 6 → Step 7 (ナビゲーション統合)
```

Step 3-6 は Step 2 完了後に並列実行可能 (ただし Step 3 のモデル/リポジトリは 4-6 が依存)

## ファイル作成リスト

```
app/src/main/java/app/tastile/android/
├── TastileApp.kt                          # Step 1
├── MainActivity.kt                        # Step 1 (更新), Step 7 (更新)
├── navigation/
│   └── TastileNavGraph.kt                 # Step 7
├── data/
│   ├── SupabaseClient.kt                  # Step 1
│   ├── repository/
│   │   ├── AuthRepository.kt              # Step 2
│   │   ├── TileRepository.kt              # Step 3
│   │   └── ProfileRepository.kt           # Step 5
│   └── model/
│       ├── Tile.kt                        # Step 3
│       └── Profile.kt                     # Step 3
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                       # Step 7 (更新)
│   │   ├── Color.kt                       # Step 7
│   │   └── Type.kt                        # Step 7
│   ├── login/
│   │   ├── LoginScreen.kt                 # Step 2
│   │   └── LoginViewModel.kt              # Step 2
│   ├── now/
│   │   ├── NowScreen.kt                   # Step 3
│   │   └── NowViewModel.kt               # Step 3
│   ├── memo/
│   │   ├── MemoScreen.kt                  # Step 4
│   │   └── MemoViewModel.kt              # Step 4
│   ├── prompt/
│   │   ├── PromptScreen.kt               # Step 4
│   │   └── PromptViewModel.kt            # Step 4
│   ├── account/
│   │   ├── AccountScreen.kt              # Step 5
│   │   └── AccountViewModel.kt           # Step 5
│   └── billing/
│       └── BillingScreen.kt              # Step 6
└── di/
    └── AppModule.kt                       # Step 1
```

## 検証チェックリスト

各ステップ完了時に確認:
- [x] `./gradlew assembleDebug` 成功
- [x] コンパイルエラーなし
- [x] 全ファイルが正しいパッケージに配置されている

最終検証:
- [ ] エミュレータで起動 → ログイン画面表示
- [ ] Google OAuth → Now 画面遷移
- [ ] タイル CRUD 動作
- [ ] Memo 保存
- [ ] Prompt 表示 (25分経過タイル)
- [ ] Account 画面表示
- [ ] Billing WebView 表示
- [ ] ダークテーマ切り替え

---

## Step 8: レビューフィードバック修正

Step 1-7 の実装完了後のコードレビューで発見された問題を修正する

### 8.1 [BUG] `PromptViewModel.takeBreak()` がタイルを削除している
**ファイル**: `ui/prompt/PromptViewModel.kt`
**問題**: `takeBreak()` 内で `tileRepository.deleteTile(tileId)` を呼んでおり、タイルがソフトデリートされる
**期待動作**: "Take a Break" は作業の一時中断。タイルの lifecycle を `Ready` に戻す
**修正**:
```kotlin
fun takeBreak() {
    viewModelScope.launch {
        try {
            val tileId = _activeTile.value?.id ?: return@launch
            tileRepository.pauseTile(tileId)  // lifecycle → Ready
            _activeTile.value = null
            _elapsedMinutes.value = 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
```
**TileRepository に追加**:
```kotlin
suspend fun pauseTile(tileId: String) {
    client.from("tiles").update({
        set("lifecycle", "Ready")
        set("updated_at", Clock.System.now().toString())
    }) { filter { eq("id", tileId) } }
}
```

### 8.2 [BUG] `Instant.parse()` のフォーマット不一致でクラッシュ
**ファイル**: `ui/prompt/PromptViewModel.kt`
**問題**: `Instant.parse(updatedAt)` が ISO 8601 以外のフォーマットでパース失敗 → アプリクラッシュ
**修正**: try-catch でラップし、パース失敗時はタイルをスキップ
```kotlin
val updatedTime = try {
    Instant.parse(updatedAt)
} catch (e: Exception) {
    return@mapNotNull null  // skip unparseable tiles
}
```

### 8.3 [HARDENING] Memo/Prompt/Account 画面のエラー表示を統一
**ファイル**: `ui/memo/MemoScreen.kt`, `ui/prompt/PromptScreen.kt`, `ui/account/AccountScreen.kt`
**問題**: NowScreen にはエラー表示があるが、他画面にはない
**修正**: 各 ViewModel に `error: StateFlow<String?>` を追加し、各 Screen で Snackbar 表示
- `MemoViewModel`: saveMemo 失敗時のエラー表示
- `PromptViewModel`: タイル操作失敗時のエラー表示
- `AccountViewModel`: プロフィール更新失敗時のエラー表示

### 8.4 [CONFIG] ProGuard ルール追加
**ファイル**: `app/proguard-rules.pro`
**問題**: Release ビルドで Supabase/Ktor/Serialization のクラスが strip される
**追加内容**:
```proguard
# Supabase
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.tastile.android.data.model.**$$serializer { *; }
-keepclassmembers class app.tastile.android.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class app.tastile.android.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
```

### 8.5 [CONFIG] Supabase URL の確認

> **Removed 2026-06-18:** Supabase is no longer the backend. The URL
> mismatch this entry flagged is moot — `gradle.properties` no longer
> holds any `SUPABASE_*` keys. See the top-of-file historical note.
