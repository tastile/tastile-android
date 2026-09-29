# Android Offline-first Timeline 設計

- Status: Approved (2026-09-16)
- Date: 2026-09-16
- Scope: `tastile-android` の Timeline read path、Day / Week / Month pager、端末内永続 cache
- Canonical contract: `../tastile-core/v1/10-invariants.md`、`../tastile-core/v1/14-read-model-and-endpoint.md`

## 1. 背景

現在の Timeline は `DashboardViewModel` が単一の `List<CoreTimelineItem>` と直前 range 1 件だけの
memory cache を所有し、Day / Week / Month の全 pager page が同じ list を参照している。選択日や scale が
変わると API range を再取得して list 全体を差し替えるため、次の問題がある。

- 日付、週、月を移動するたびに cache miss し、往復しても再取得する。
- pager の全 page が同じ list identity の変更を受け、非表示 page まで派生 block を再計算する。
- 隣接 page を事前に構成していないため、slow swipe 中に片側だけが描画され、明確な画面切替が見える。
- process restart 後に Timeline を復元できず、offline では既取得データも読めない。
- ID 列だけを比較して StateFlow emit を抑える現行処理は、時刻、title、status などの変更を捨て得る。
- UI state と network request が同じ ViewModel に結合しており、通信 latency が描画 UX に伝播する。

## 2. Goals

1. Day / Week / Month のいずれも、表示中 page と前後 page を local data から同時に描画する。
2. swipe 中に両側の page が完全な内容を保持し、空白、flash、明確な切替を発生させない。
3. UI は local persistent read model のみを監視し、API client や request state を直接参照しない。
4. API 通信は background synchronization として local read model を更新し、描画を待たせない。
5. process restart、network failure、機内 mode でも取得済み期間を読み取れるようにする。
6. 未取得期間と取得済みだが item が 0 件の期間を区別する。
7. 実際に内容が変わった page だけ新しい immutable snapshot を発行する。
8. core の Timeline contract を変更せず、Android を thin client に保つ。

## 3. Non-goals

- offline 中の作成、編集、開始、一時停止、完了などの command 実行
- offline command queue、楽観更新、conflict resolution
- core API / schema / numeric registry の変更
- Web / Desktop の Timeline UI 変更
- 全期間を無制限に端末へ download すること

## 4. Architecture

### 4.1 Read path と sync path の分離

```text
Compose Day / Week / Month
          |
          v
TimelinePageRepository.observePage(key): Flow<TimelinePageSnapshot>
          |
          v
Room local read model  <--- transaction ---  TimelineSyncRepository
                                              |
                                              v
                                      canonical core API
```

Compose と screen state holder は `TimelinePageRepository` の local `Flow` だけを読む。
`TimelineSyncRepository` は API response を検証して Room transaction へ書く producer であり、UI に
network response を直接返さない。同期開始、成功、失敗によって既存 local item を空 list へ戻さない。

UI が同期を要求するときも `requestRefresh(keys)` という intent を送るだけとし、その完了を待って描画
しない。同期状態は DB metadata から導出し、network job の mutable state を UI source of truth にしない。

### 4.2 永続 read model

Room database に次の logical table を置く。実 field 名と index は implementation plan で固定する。

#### TimelineItem

- account ID と scope fingerprint 内で canonical Timeline item ID を一意にする。
- `CoreTimelineItem` の全 canonical field を lossless に保存する。
- `startAt` / `endAt` を UTC instant として index し、期間 overlap を query できるようにする。
- source payload の equality/hash は全表示 field を含み、ID だけで同一判定しない。

#### TimelineDayMembership

- account、scope fingerprint、zone ID、local date、item ID の対応を保存する。
- 日跨ぎ item は overlap するすべての local date に membership を持つ。
- Day / Week / Month は同じ membership を利用し、scale ごとの item 複製を作らない。

#### TimelineCoverage

- account、scope fingerprint、zone ID、local date ごとに `NeverFetched / Available / Stale` を判定できる
  metadata を保存する。
- `fetchedAt`、contract version、最後の sync outcome を保持する。
- item が 0 件でも `Available` を記録し、未取得期間と空期間を区別する。
- API response の item upsert、membership replacement、coverage 更新は同一 transaction で行う。

`scope fingerprint` は owner IDs、Timeline include flags、API contract version を順序正規化して生成する。
表示上の minimum-duration filter は raw cache key に含めず、local query 後の presentation filter とする。

### 4.3 Page key と page snapshot

`TimelinePageKey` は `accountId / scopeFingerprint / zoneId / scale / anchor` を持つ。anchor は次のように
正規化する。

- Day: local date
- Week: locale に依存しない canonical Monday
- Month: month first day。ただし query 範囲は UI grid に表示する前後月の日を含む 5〜6 週

`TimelinePageSnapshot` は immutable collection として次を提供する。

- page key
- page の全表示日に対応する item
- 日単位の coverage
- local data の最終更新時刻
- `isOffline` / `isRefreshing`
- 未取得日があるか

Room の複数 table emission は repository 内で content equality を取り、表示内容と metadata が変わらない
場合は同じ snapshot instance を維持する。Compose は page ごとの snapshot を受け取り、他 page の更新で
再コンポーズしない。

### 4.4 Pager と同時描画

Day / Week / Month の各 `HorizontalPager` は `beyondViewportPageCount = 1` を設定し、現在 page と前後 1 page
を常時構成する。page key は page index ではなく正規化 anchor を表す stable key とする。

各 page composable は自分の `TimelinePageSnapshot` のみを受け取る。screen 全体で単一 timeline list を
collect しない。pager offset、scroll、zoom の frame-rate state は page data の再計算 key に含めず、layout
または draw phase で消費する。

scale 切替時は Day / Week / Month それぞれの pager state と zoom state を保持し、中央 page への強制 reset
を行わない。選択 anchor は共有できるが、各 scale の現在位置を独立して保持する。

### 4.5 Prefetch と stale-while-revalidate

画面表示時に current、previous、next の page key を local DB から即時 observe する。同期 coordinator は
coverage を見て次の順で refresh する。

1. current page の未取得日
2. swipe 方向の adjacent page の未取得日
3. 反対側 adjacent page の未取得日
4. stale な current / adjacent page

取得 range は重複する連続日をまとめ、canonical API の有限 range query とする。同じ account / scope / zone
で重複する in-flight request は単一化する。古い request が新しい request より後に終了しても、generation
または request start time により新しい coverage を上書きしない。

stale data は refresh 中も表示する。成功時は transaction commit による local Flow 更新だけが UI へ届く。
失敗時は既存 item を保持し、coverage metadata に failure を記録する。

### 4.6 Offline read-only behavior

- 明示的 logout 済みでない、端末に最後の認証済み account identity がある場合、network verification が
  できなくても local Timeline の読み取りを許可する。
- security lock が有効なら、offline entry にも同じ unlock rule を適用する。
- offline 中または有効な API credential がない場合、変更 action を disabled にし理由を表示する。
- 未取得 page は空 Timeline として偽装せず、「この期間は offline では未取得」と表示する。
- 明示的 logout、account deletion、別 account への切替時は対象 account の Timeline cache を削除する。
- Android app sandbox を前提とし、token や credential は Timeline DB に保存しない。

### 4.7 Retention

cache は無制限に増やさない。account / scope ごとの最終 access を記録し、最近閲覧した coverage を優先する。
初期 policy は次とする。

- 現在日を含む前後 90 日は保持対象
- それ以外は least-recently-accessed coverage から削除可能
- item はどの retained membership からも参照されなくなったときだけ削除
- pruning は UI request path ではなく background task で実行

容量上限の具体値は実装前に実データ 90 日分の計測から決め、根拠なしの固定 MB 値を設けない。

## 5. State ownership

- `TimelineScreen`: scale 選択、pager state、zoom など一時 UI state
- `TimelinePageViewModel` または専用 screen state holder: visible page keys と page snapshots の購読、refresh intent
- `TimelinePageRepository`: local DB query、immutable snapshot、coverage 判定
- `TimelineSyncRepository`: API fetch、response validation、transactional persistence
- `TimelineCacheDatabase`: item、membership、coverage、migration、pruning
- `DashboardViewModel`: Timeline item list と network request の所有を終了し、他 dashboard concern のみ保持

## 6. Error and correctness rules

- API の 401/403 は cache 削除理由にしない。UI は local data を保持し、再認証を要求する。
- validation failure や decode failure は transaction を開始せず、最後の正常 snapshot を保持する。
- empty success response は coverage を `Available` にし、その fetched range の membership を transaction 内で
  空に置換する。
- item 内容比較は全 canonical display field を対象とする。
- timezone 変更時は別 zone key として membership を再構築する。旧 zone cache は retention 対象とする。
- DST の 23 / 25 時間日でも local date coverage と UTC overlap query を混同しない。
- core API が要求しない alias、dual-read、wire compatibility shim は追加しない。

## 7. API contract

producer は canonical core Timeline endpoint の有限 `TimelineQuery.range` を使う。`TimelineInclude`、
`TimelineItem`、UTC instant、numeric registry の意味を Android 側で再定義しない。

この変更は API/schema/auth protocol を変更しない。Android local tables は transport schema ではなく cache
implementation detail であり、core business logic を複製しない。Day / Week / Month の grouping と coverage は
presentation/read optimization に限定する。

## 8. Migration

既存 version には永続 Timeline DB がないため、初回起動時は空 DB を作成する。旧 memory cache の移行は
行わない。database schema は version 1 から開始し、destructive migration fallback は設定しない。

現在の単一 `_timeline` / `_timelineCache` path は新 repository が実 API と同じ表示結果を返すことを test で
確認してから削除する。dual-read を production compatibility として残さない。

## 9. Verification

### Automated

- Room DAO: overlap、日跨ぎ、empty coverage、transaction replacement、account isolation
- repository: Day / Week / Month composition、immutable snapshot reuse、content change detection
- sync: range coalescing、in-flight dedupe、stale result rejection、失敗時 cache preservation
- ViewModel: visible 3 page の購読、swipe direction prefetch、scale ごとの位置保持
- Compose: adjacent pages の同時存在、stable page key、未取得/offline/refresh state
- migration: fresh install と将来 migration gate
- compiler reports: affected page composable が skippable で、unchanged page の unstable identity churn がないこと

### Device and real API

Day / Week / Month それぞれについて同じ実機、同じ account、同じ transition を計測する。

1. online 初回表示で current と adjacent page を取得する。
2. slow swipe 中に両側 page の item が同時表示されることを録画または screenshot sequence で確認する。
3. 前後へ往復し、追加 API request なしで local cache から表示されることを network log で確認する。
4. process kill / restart 後、network request 完了前に同じ page が表示されることを確認する。
5. 機内 mode で Day / Week / Month を既取得範囲内移動できることを確認する。
6. 未取得期間へ移動し、空表示ではなく未取得表示になることを確認する。
7. online 復帰後、表示を消さず background refresh されることを確認する。
8. `dumpsys gfxinfo` と Compose recomposition evidence を変更前後で比較する。

実 API の origin 到達性は 2026-09-16 に `https://api.tastile.app` で確認済み。認証済み Timeline response の
検証は端末で login した実 account を使い、credential を log や成果物へ保存しない。

## 10. Completion criteria

- Day / Week / Month の slow swipe 中に両側が完全描画される。
- 既取得 page の表示は API response を待たず local DB から開始する。
- 同じ page への再訪で content 未変更なら page snapshot identity と描画 subtree が維持される。
- process restart と機内 mode で既取得範囲を読み取れる。
- offline 中の mutation は実行されず、read-only 状態が明示される。
- account isolation、logout purge、未取得と empty の区別が test で証明される。
- canonical core API contract に drift がない。
- Android の applicable unit、lint、instrumented、実機 gate が成功する。
