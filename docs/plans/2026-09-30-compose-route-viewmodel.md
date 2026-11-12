# Compose の ViewModel 解決を navigation route に置く

Issue: #82。release-0-7-0 の実 CI が TimelineScreen と TilesScreen の `runCatching { hiltViewModel() }` を compile error として検出した。

## 最終設計

既存 MobileScaffold の timeline composable route で直接 Hilt TimelinePageViewModel を取得し、TimelineScreen の explicit nullable seam に渡す。screen 自体は Hilt を解決せず、null の plain test host には既存の deterministic frame/FAB rendering を維持する。TilesScreen は現行 production caller がなく、non-LIST の test は explicit projection を渡すため、内部 Hilt fallback を削除して explicit seam のみを使う。未使用 route や新しいnavigation経路は追加しない。

Hilt fail は production ではそのまま fail closed とし、例外を握りつぶすfallbackは作らない。business logic / API / auth / gesture / pager state は変更しない。root ADR-0007 branch policy、ADR-0021 binding verificationに従う。

## Ownership / 検証

対象: TimelineScreen.kt、TilesScreen.kt、MobileScaffold.kt。本plan以外のdocument/skill修正は#80に保持する。

Compose compileとfull gradlew verify、既存TimelineScreenFabTest / TimelineScreenLoadingTest / TilesScreenTestを実行する。productionHilt routeは対象device/emulatorで検証し、証跡がなければ未検証として保持する。SDKはofficialdownloadのchecksumを照合してlocalignored .tools/へ配置し、configurationは既存Infisicalからprocessへ取得する。test timeout/skip/guardを変更しない。
local full gateでは既存adapter drift taskがWindows absolute pathをWSL Bashへ渡して失敗した。app/build.gradle.ktsのProcessBuilderを明示repository working directoryと相対script pathへ変更し、同じscript/exit-code checkをLinuxとWindowsで実行する。guard内容やskip条件は変更しない。
