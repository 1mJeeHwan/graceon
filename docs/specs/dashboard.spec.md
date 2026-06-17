# 통합 운영 대시보드 — 구현 스펙 (`/v1/dashboard`)

> 설계문서 4장(입구 ① — 통합 운영 대시보드)의 구현 스펙. 기능 출처는 `gnuboard5-admin-feature-spec.md` 5.1 매출현황 / 메인화면 위젯. 실제 `streamhub-api`(Spring Boot 3.4.1 / Java 21) · `streamhub-web`(Next.js 14) 코드 컨벤션에 100% 맞춰 작성했다.

---

## 1. 목적 / 범위

"운영 중인 관제실"의 첫인상을 만드는 단일 화면. 위에서 아래로 중요도 순: **(a) KPI 스트립 6종**(카운트업 + 전일 대비 ▲▼) · **(b) 후원·매출 추이 90일 스택 차트 + 멤버십/채널 도넛** · **(c) 실시간 활동 피드**(시드 타임라인을 현재 시각 기준 슬라이딩 윈도우로 재생) · **(d) 할 일 큐**(미처리 주문·미답변 문의·재고 경고를 관리화면 필터로 딥링크). 백엔드는 N+1 없는 **집계 전용 읽기 엔드포인트 3종**(`/summary`·`/timeseries`·`/feed`)으로 제공하고, 가장 무거운 `/summary`·`/timeseries`는 기존 `StatService`의 `@Cacheable`(Redis 60s TTL) 패턴을 그대로 재사용한다. 이 도메인은 **신규 엔티티를 만들지 않는다** — 현재 스키마(`MEMBER`/`CONTENT`/`CHANNEL`/`WATCH_HISTORY`/`ACTION_LOG`/`POST`)만으로 집계하며, 향후 커머스 도메인(`GOODS`/`ORDER`/`DONATION`/`INQUIRY`) 추가 시 매퍼 SQL의 서브쿼리만 교체하면 되도록 DTO 필드를 미리 확장 가능한 형태로 설계한다.

> **현실 제약 (중요)**: 설계문서 4.1 KPI 6종은 후원·매출/주문/문의/재고를 가정하지만, 현재 코드베이스에는 해당 엔티티가 **아직 없다**. 이 스펙은 두 단계로 나눈다.
> - **Phase D1 (지금 구현 가능)**: 현재 스키마로 산출 가능한 6 KPI로 매핑(아래 2.1 표 "D1 소스" 열). 매출/주문/문의/재고 자리는 콘텐츠·시청·회원 지표로 채우되, 필드명은 설계문서 도메인 언어를 유지(`todayRevenue` 등)하고 D1에서는 결정론적 시드 파생값으로 산출한다.
> - **Phase D2 (커머스 도메인 착수 후)**: `GOODS`/`ORDER`/`DONATION`/`INQUIRY` 엔티티가 생기면 `DashboardMapper.xml`의 해당 서브쿼리만 실 테이블로 교체. 컨트롤러/DTO/프론트는 무변경.

---

## 2. 데이터 모델

이 도메인은 **자체 테이블이 없다**(집계 전용). 아래는 (a) 집계가 읽는 기존 테이블, (b) Phase D2에서 추가될 테이블의 예약 컬럼이다. 모든 테이블은 기존 컨벤션을 따른다: **대문자 스네이크 테이블명**, **소문자 스네이크 `@Column`**, `created_at`/`updated_at` 직접 보유, `idx_{테이블}_{용도}` 인덱스.

### 2.1 집계가 읽는 기존 테이블 (D1, 변경 없음)

| 테이블 | 읽는 컬럼 | 용도 |
|---|---|---|
| `MEMBER` | `id`, `user_status`, `created_at` | 활성 구독자(=CONFIRMED 회원수), 신규 가입 추이, 회원 증가 |
| `CONTENT` | `id`, `view_count`, `status`, `created_at` | 진행 중 콘텐츠(=DRAFT 수), 조회 Top5, 총 조회수 |
| `CHANNEL` | `id`, `name` | 채널별 시청시간 도넛 |
| `WATCH_HISTORY` | `member_id`, `content_id`, `watched_at`, `watch_seconds` | 일별 시청 추이 스택, 채널별 합계 |
| `ACTION_LOG` | `admin_id`, `admin_name`, `action`, `target_type`, `target_id`, `detail`, `created_at` | 실시간 활동 피드 소스 |
| `POST` | `id`, `status`, `created_at` | 미답변 문의 자리(D1: 최근 PUBLISHED 게시글 수로 대체) |

### 2.2 Phase D2 예약 테이블 (이번 스펙 구현 대상 아님 — 참고용 인덱스 정의)

> 커머스 슬라이스(P1) 착수 시 별도 도메인 패키지로 생성. 대시보드는 이 테이블들이 생기면 SQL만 바꿔 끼운다.

| 테이블 | 핵심 컬럼 (타입 · 제약) | 인덱스 |
|---|---|---|
| `ORDER` | `id BIGINT PK`, `order_no VARCHAR(30) NN UNIQUE`, `member_id BIGINT NN`, `status VARCHAR(20) NN`(PLACED/PAID/READY/SHIPPING/DONE/CANCEL/RETURN), `total BIGINT NN`, `ordered_at DATETIME NN` | `idx_order_status(status)`, `idx_order_ordered(ordered_at)`, `idx_order_member(member_id)` |
| `DONATION` | `id BIGINT PK`, `member_id BIGINT NN`, `amount BIGINT NN`, `type VARCHAR(20) NN`(ONCE/SUBSCRIPTION), `paid_at DATETIME NN` | `idx_donation_paid(paid_at)`, `idx_donation_member(member_id)` |
| `GOODS_ITEM` | `id BIGINT PK`, `name VARCHAR(120) NN`, `price BIGINT NN`, `stock INT NN`, `notify_stock INT NN`, `sold_out CHAR(1) NN`, `sale_count INT NN` | `idx_goods_stock(stock)` |
| `INQUIRY` | `id BIGINT PK`, `member_id BIGINT NN`, `status VARCHAR(20) NN`(OPEN/ANSWERED), `created_at DATETIME NN`, `answered_at DATETIME` | `idx_inquiry_status(status)`, `idx_inquiry_created(created_at)` |

---

## 3. 백엔드

### 3.1 생성할 파일 목록 (정확한 패키지 경로)

신규 도메인 패키지: `org.streamhub.api.v1.dashboard` (기존 `statistics` 패키지와 동일 레이아웃). `statistics`는 단순 통계 위젯용으로 유지하고, **대시보드 전용 집계는 별도 패키지**로 분리한다(관제실 KPI/피드/할일 큐 = 합성 응답이라 책임이 다름).

```
streamhub-api/src/main/java/org/streamhub/api/v1/dashboard/
├── DashboardController.java                         # @RestController, /v1/dashboard
├── DashboardService.java                            # @Cacheable 집계 오케스트레이션
├── mapper/DashboardMapper.java                      # @Mapper 인터페이스
└── dto/
    ├── DashboardSummaryResponse.java                # KPI 6종 (값 + 전일/전기간 비교)
    ├── KpiDelta.java                                # 단일 KPI: current/previous/deltaPct/spark
    ├── TimeseriesResponse.java                      # 90일 스택 (categories + 3 series)
    ├── TimeseriesSeriesPoint.java                   # date, goodsRevenue, recurringDonation, onceDonation
    ├── FeedItem.java                                # 활동 피드 한 줄
    ├── MembershipSliceItem.java                     # 멤버십 분포 도넛 (label, count)
    └── TodoQueueResponse.java                       # 할 일 큐 카운트 3종 + 딥링크 메타

streamhub-api/src/main/resources/mappers/
└── DashboardMapper.xml                              # 집계 SQL (서브쿼리/GROUP BY)
```

수정할 기존 파일: **없음**(`CacheConfig`의 60s TTL·`summary` 캐시 매니저를 그대로 사용. 새 캐시 이름 `dashboardSummary`/`dashboardTimeseries`는 동일 매니저가 자동 처리).

### 3.2 API 엔드포인트 표

클래스 레벨 가드는 `StatController`와 동일:
`@PreAuthorize("hasAnyAuthority(T(...AuthoritiesConstants).SYSTEM, T(...AuthoritiesConstants).CHURCH_MANAGER)")`. 모든 응답은 `ResultDTO<T>`로 래핑. `principal`은 스코핑이 필요한 D2에서만 사용(D1은 단일 데모 조직이라 전역 집계).

| 메서드 | 경로 | 요청 | 응답 DTO | 권한 | 캐시 |
|---|---|---|---|---|---|
| GET | `/v1/dashboard/summary` | — | `ResultDTO<DashboardSummaryResponse>` | SYSTEM·CHURCH_MANAGER | `@Cacheable("dashboardSummary", key="'all'")` 60s |
| GET | `/v1/dashboard/timeseries` | `@RequestParam(defaultValue="90") int days` | `ResultDTO<TimeseriesResponse>` | 동일 | `@Cacheable("dashboardTimeseries", key="#days")` 60s |
| GET | `/v1/dashboard/feed` | `@RequestParam(defaultValue="20") int limit` | `ResultDTO<List<FeedItem>>` | 동일 | **캐시 없음**(실시간 느낌 — 매 호출 최신) |
| GET | `/v1/dashboard/membership` | — | `ResultDTO<List<MembershipSliceItem>>` | 동일 | `@Cacheable("dashboardMembership", key="'all'")` 60s |
| GET | `/v1/dashboard/todo` | — | `ResultDTO<TodoQueueResponse>` | 동일 | **캐시 없음**(미처리 카운트는 즉시성 필요) |

> Swagger: 각 메서드에 `@Operation(summary="...")` 한글 요약. 클래스에 `@Tag(name="Dashboard", description="통합 운영 대시보드")`.

### 3.3 DTO 정의 (필드 · 타입)

`statistics` DTO 컨벤션(`@Getter @Setter @NoArgsConstructor`, MyBatis가 snake→camel 자동 매핑)을 그대로 따른다.

```java
// DashboardSummaryResponse — KPI 6종. 각 KPI는 KpiDelta로 현재값+비교+스파크라인.
class DashboardSummaryResponse {
    KpiDelta todayRevenue;      // 오늘 후원·매출 (D1: 시드 파생 합계)
    KpiDelta newSubscriptions;  // 신규 구독 (D1: 오늘 신규 CONFIRMED 회원)
    KpiDelta openOrders;        // 진행 중 주문 (D1: DRAFT 콘텐츠 수로 대체)
    KpiDelta unansweredInquiry; // 미답변 문의 (D1: 최근 7일 POST 수)
    KpiDelta lowStock;          // 재고 경고 (D1: 고정 시드 상수 2, "운영 흠집")
    KpiDelta activeSubscribers; // 활성 구독자 (CONFIRMED 회원수)
}

// KpiDelta — 카운트업/▲▼/미니 스파크라인 한 카드.
class KpiDelta {
    long current;               // 현재 값
    long previous;              // 전일(또는 전기간) 값
    double deltaPct;            // (current-previous)/previous*100, previous=0이면 0
    List<Long> spark;          // 최근 7포인트 스파크라인 (없으면 빈 리스트)
}

// TimeseriesResponse — 90일 스택 (ApexCharts categories + 3 series).
class TimeseriesResponse {
    List<String> categories;          // yyyy-MM-dd (90개)
    List<Long> goodsRevenue;          // 굿즈 매출 (D1: WATCH_HISTORY 일별 분 환산 파생)
    List<Long> recurringDonation;     // 정기후원 (D1: 일별 신규회원*상수)
    List<Long> onceDonation;          // 단건후원 (D1: 일별 시청건수*상수)
}

// FeedItem — 활동 피드 한 줄 (ActionLogItem 기반 + 상대시간/카테고리 가공).
class FeedItem {
    Long id;
    String kind;          // MEMBER_JOIN | ORDER | DONATION | CONTENT | LOGIN ... (action에서 파생)
    String message;       // "김O준님이 멤버십 가입" 식 완성 문장
    String actorName;     // adminName 또는 마스킹된 회원명
    LocalDateTime occurredAt;  // ACTION_LOG.created_at (슬라이딩 윈도우 기준점)
}

// MembershipSliceItem — 멤버십 분포 도넛.
class MembershipSliceItem {
    String label;   // BRONZE/SILVER/GOLD/ANGEL (D1: member.user_status 분포로 대체)
    long count;
}

// TodoQueueResponse — 할 일 큐 + 딥링크 메타 (프론트가 href 조립).
class TodoQueueResponse {
    long pendingOrders;     // 미처리 주문 (D1: DRAFT 콘텐츠 수)
    long unansweredInquiry; // 미답변 문의 (D1: 최근 PENDING 회원 수)
    long lowStockItems;     // 재고 경고 (D1: 시드 상수)
}
```

### 3.4 매퍼 인터페이스 (`DashboardMapper.java`)

```java
@Mapper
public interface DashboardMapper {
    // D1: 현재 스키마 집계. D2: 서브쿼리만 실 테이블로 교체.
    long countActiveSubscribers();                                  // CONFIRMED 회원수
    long countNewMembersSince(@Param("since") LocalDateTime since); // 오늘/전일 신규
    long countDraftContents();                                      // 진행중 자리
    long countRecentPosts(@Param("since") LocalDateTime since);     // 미답변 문의 자리
    long sumViewCount();                                            // 총 조회수
    List<TrendRow> dailyWatchTrend(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);  // 90일 일별 집계
    List<MembershipSliceItem> membershipDistribution();            // user_status GROUP BY
    List<FeedRow> recentActions(@Param("limit") int limit);        // ACTION_LOG 최근 N
}
```

### 3.5 핵심 로직 의사코드

**(a) `/summary` — KPI 6종 합성 (트랜잭션: readOnly 불필요, MyBatis 단순 SELECT)**

```
@Cacheable("dashboardSummary", key="'all'")
getSummary():
    now      = LocalDateTime.now()
    todayStart = now.toLocalDate().atStartOfDay()
    yesterdayStart = todayStart.minusDays(1)

    activeSubs   = mapper.countActiveSubscribers()
    newToday     = mapper.countNewMembersSince(todayStart)
    newYesterday = mapper.countNewMembersSince(yesterdayStart) - newToday
    draft        = mapper.countDraftContents()
    recentPosts  = mapper.countRecentPosts(now.minusDays(7))

    // D1 결정론적 파생 매출 (시드와 동일 공식 → 데모 재현성 보장)
    todayRevenue = deriveRevenue(newToday, sumViews=mapper.sumViewCount())

    return DashboardSummaryResponse{
      todayRevenue      = kpi(todayRevenue, deriveRevenue(newYesterday,...), spark=last7RevenueSpark()),
      newSubscriptions  = kpi(newToday, newYesterday, spark=last7NewMembers()),
      openOrders        = kpi(draft, draft, spark=[]),               // 비교없음 → delta 0
      unansweredInquiry = kpi(recentPosts, recentPosts, spark=[]),
      lowStock          = kpi(2, 2, spark=[]),                        // "운영 흠집" 고정 상수
      activeSubscribers = kpi(activeSubs, activeSubs, spark=[])
    }

kpi(cur, prev, spark):
    delta = prev==0 ? 0 : (cur-prev)/prev*100.0
    return KpiDelta{current=cur, previous=prev, deltaPct=round1(delta), spark=spark}
```

**(b) `/timeseries` — 90일 스택 (날짜 빈 구멍 채우기 = 핵심)**

```
@Cacheable("dashboardTimeseries", key="#days")
getTimeseries(days):
    range = days<=0 ? 90 : days
    to   = LocalDateTime.now()
    from = to.minusDays(range).toLocalDate().atStartOfDay()

    rows = mapper.dailyWatchTrend(from, to)        // {date, watchSeconds, watchCount}
    byDate = rows.indexBy(r -> r.date)

    categories=[], goods=[], recurring=[], once=[]
    for d in [from.date .. to.date]:               // 모든 날짜 순회 (빈 날 0 채움)
        key = d.format("yyyy-MM-dd")
        row = byDate[key] or ZERO
        categories.add(key)
        goods.add( row.watchSeconds / 60 * 17 )    // 결정론 환산 (매출 느낌)
        recurring.add( row.watchCount * 4900 )     // 정기후원 단가 파생
        once.add( row.watchCount * 1300 )          // 단건 파생
    return TimeseriesResponse{categories, goods, recurring, once}
```
> 빈 날짜를 0으로 채우지 않으면 ApexCharts x축이 들쭉날쭉 → "목업 티". 반드시 from~to 연속 채움.

**(c) `/feed` — 실시간 활동 피드 (서버는 원천만, 슬라이딩 재생은 프론트)**

```
getFeed(limit):
    rows = mapper.recentActions(limit<=0 ? 20 : limit)   // ORDER BY created_at DESC LIMIT n
    return rows.map(r -> FeedItem{
        id=r.id,
        kind=classify(r.action),                          // action 접두/접미로 분류
        message=humanize(r.action, r.targetType, r.detail), // "콘텐츠 등록: {detail}"
        actorName=r.adminName ?? "시스템",
        occurredAt=r.createdAt
    })

classify(action):
    if action.endsWith("CREATE") return "CREATE"
    if action.endsWith("APPROVE"|"DENY") return "MEMBER"
    if action == "LOGIN" return "LOGIN"
    return "OTHER"
```
> **상대시간/슬라이딩 윈도우 재생은 프론트 책임**(4.3 참조). 서버는 절대시각(`occurredAt`)만 내려준다 — 캐시 없이 항상 최신.

**(d) `DashboardMapper.xml` 집계 SQL 예시 (StatMapper.xml 스타일)**

```xml
<select id="dailyWatchTrend" resultType="org.streamhub.api.v1.dashboard.dto.TrendRow">
    SELECT DATE_FORMAT(watched_at, '%Y-%m-%d') AS date,
           COALESCE(SUM(watch_seconds),0)       AS watch_seconds,
           COUNT(*)                              AS watch_count
    FROM WATCH_HISTORY
    WHERE watched_at BETWEEN #{from} AND #{to}
    GROUP BY DATE_FORMAT(watched_at, '%Y-%m-%d')
    ORDER BY date
</select>

<select id="membershipDistribution" resultType="org.streamhub.api.v1.dashboard.dto.MembershipSliceItem">
    SELECT user_status AS label, COUNT(*) AS count
    FROM MEMBER GROUP BY user_status ORDER BY count DESC
</select>

<select id="recentActions" resultType="org.streamhub.api.v1.dashboard.dto.FeedRow">
    SELECT id, admin_id, admin_name, action, target_type, target_id, detail, created_at
    FROM ACTION_LOG ORDER BY created_at DESC, id DESC LIMIT #{limit}
</select>

<select id="countActiveSubscribers" resultType="long">
    SELECT COUNT(*) FROM MEMBER WHERE user_status = 'CONFIRMED'
</select>
```
> XML에서 `&gt;=`/`&lt;` 이스케이프는 기존 StatMapper.xml과 동일하게 처리.

---

## 4. 프론트

### 4.1 생성할 파일 목록 (정확한 경로)

기존 대시보드(`/dashboard`)는 회원/콘텐츠/시청 위젯이 이미 있다. 이 스펙은 **그 페이지를 관제실로 확장**한다 — 새 위젯 컴포넌트를 추가하고 `dashboard/page.tsx`를 재구성.

```
streamhub-web/src/app/(protected)/dashboard/
└── page.tsx                          # [수정] KPI 스트립 → 추이/도넛 → 피드/할일 순으로 재배치

streamhub-web/src/components/dashboard/
├── KpiStrip.tsx                      # [신규] 6 KPI 카드 (카운트업 + ▲▼ + 스파크라인)
├── KpiCard.tsx                       # [신규] 단일 카드 (CountUp + 미니 스파크 svg/apex)
├── RevenueTrendChart.tsx             # [신규] 90일 스택 area/bar (goods/recurring/once)
├── MembershipDonutChart.tsx          # [신규] 멤버십 분포 도넛 (WatchByChannelChart 복제)
├── ActivityFeed.tsx                  # [신규] 실시간 피드 (시드 타임라인 슬라이딩 재생)
├── TodoQueue.tsx                     # [신규] 할 일 큐 3카드 + 관리화면 딥링크
└── (기존) SummaryCards/MemberTrendChart/TopContentsChart/WatchByChannelChart/ChartCard/ApexChart 재사용

streamhub-web/src/lib/
└── relative-time.ts                  # [신규] "3분 전" 상대시간 포매터 (서버 occurredAt 기준)

streamhub-web/src/apis/query/dashboard/
└── dashboard.ts                      # [자동생성] Orval — useDashboardSummary/useTimeseries/useFeed/useMembership/useTodo
```

> **주의(프로젝트 규칙)**: `dashboard.ts`·`streamHubAdminAPI.schemas.ts`는 백엔드 배포 후 `npm run gen`으로만 생성. 직접 수정 금지. gen 전 백엔드 배포 여부 사용자 확인 필수.

### 4.2 화면별 컴포넌트 · 데이터패칭 · 차트

**`page.tsx` 레이아웃 (위→아래 중요도 순)**
```
<h1>통합 운영 대시보드</h1> <p>"데모 데이터 · 테스트 모드" 명시 배지</p>
<KpiStrip/>                                  // 6 카드
<div grid lg:grid-cols-3>
  <RevenueTrendChart className="lg:col-span-2"/>   // 좌 큰 영역
  <ActivityFeed/>                            // 우 피드
</div>
<div grid lg:grid-cols-2>
  <TopContentsChart/> <MembershipDonutChart/>
</div>
<TodoQueue/>
```

**데이터패칭 (React Query, Orval 훅)** — `SummaryCards`와 동일 패턴 `const { data, isPending, isError } = useDashboardSummary(); const summary = data?.resultObject;`
- `KpiStrip`: `useDashboardSummary()`
- `RevenueTrendChart`: `useTimeseries({ days: 90 })`
- `MembershipDonutChart`: `useMembership()`
- `ActivityFeed`: `useFeed({ limit: 30 }, { query: { refetchInterval: 15000 } })` — 15초마다 폴링(실시간 느낌)
- `TodoQueue`: `useTodo({ query: { refetchInterval: 30000 } })`

**KpiCard 카운트업**: `react-countup` 미사용 시 `useEffect`+`requestAnimationFrame`로 0→current 이징(약 800ms). 스파크라인은 `KpiDelta.spark`를 `ApexChart type="line"`(sparkline:{enabled:true}, 높이 36)로. ▲▼는 `deltaPct` 부호로 `text-emerald-600`/`text-red-600` + `ArrowUp`/`ArrowDown`(lucide).

**RevenueTrendChart** (`WatchByChannelChart.tsx` 차트 옵션 컨벤션 따름): `type="bar"` stacked, `series=[{name:"굿즈매출"},{name:"정기후원"},{name:"단건후원"}]`, `colors=["#2563eb","#60a5fa","#bfdbfe"]`, `xaxis.categories=resultObject.categories`, `tickAmount:8`. `ChartCard`로 로딩/에러/빈 상태 래핑.

**MembershipDonutChart**: `WatchByChannelChart.tsx`를 그대로 복제 — `labels=items.map(label)`, `series=items.map(count)`, donut size 62%.

**ActivityFeed** (관제실 핵심 연출):
- `useFeed`로 받은 `FeedItem[]`을 `occurredAt` 기준 정렬.
- **슬라이딩 윈도우 재생**: 마운트 시 전체를 한 번에 안 그리고, 가장 오래된 것부터 5초 간격으로 한 줄씩 위에서 슬라이드-인(`useState` 큐 + `setInterval`, `framer-motion` 또는 CSS `animate-in`). 큐가 비면 `useFeed` 재폴링분으로 다시 채움.
- 각 줄: 좌측 `kind`별 색 점, 가운데 `message`, 우측 `relativeTime(occurredAt)`("3분 전") + `title`에 절대시각 툴팁(`formatDateTime`).
- `kind` 색상 함수는 `action-log/page.tsx`의 `actionColor` 패턴 재사용.

**TodoQueue** (딥링크):
- 3카드: 미처리 주문 → `<Link href="/content?status=DRAFT">`, 미답변 문의 → `/member?status=PENDING`, 재고 경고 → `/goods?lowStock=1`(D2). 각 카드에 카운트 + "처리하러 가기" CTA.
- 카운트 0이면 회색/비활성, >0이면 `bg-amber-50 text-amber-700` 강조(설계문서 9장 "운영 흠집").

### 4.3 폼 (RHF + Zod)

대시보드는 **읽기 전용 화면** → 폼 없음. (할 일 큐의 딥링크가 각 관리화면의 기존 필터 폼으로 연결됨.)

### 4.4 AG Grid

대시보드에 그리드 없음. 활동 피드는 `action-log/page.tsx`처럼 **순수 HTML 리스트**(AG Grid 아님). 딥링크 대상 화면(member/content)이 기존 AG Grid 컬럼defs를 재사용.

### 4.5 사이드바 메뉴

`streamhub-web/src/components/layout/Sidebar.tsx`의 `NAV_ITEMS`에서 기존 "대시보드"(`/dashboard`) 항목을 **레이블만 "통합 운영 대시보드"로 변경** — 라우트/아이콘(`LayoutDashboard`) 유지. 신규 메뉴 추가 없음.

---

## 5. 시드 영향

대시보드 자체는 신규 시드 테이블이 없지만, **차 있는 관제실**(설계 9장 "빈 테이블 금지")을 위해 기존 `DataInitializer`의 시드가 충분해야 한다. 현재 `seedWatchHistory`(800건)·`seedActionLogs`·`seedMembers`(60건)가 이미 있으므로 D1은 즉시 동작한다. 단, 다음을 확인/보강:

- **`seedActionLogs` 다양성**: 활동 피드가 단조롭지 않도록 `action` 종류(MEMBER_APPROVE/CONTENT_CREATE/LOGIN 등)와 `created_at`이 최근 N시간에 **분 단위로 분산**돼야 한다. 현재 시드가 충분치 않으면 최근 6시간에 30~50건을 결정론적으로 분산 추가(`now.minusMinutes(i*7)` 식, 기존 `seedWatchHistory` 분산 공식과 동일 철학).
- **"운영 흠집" 시드**(설계 9장): 미답변 문의 3건·품절 2종·환불 1건이 KPI에서 0이 아니게. D1에서는 `lowStock` KPI를 시드 상수 2로 고정, `unansweredInquiry`는 최근 PENDING 회원/POST가 존재하도록 시드 분포 유지.
- **결정론성**: 모든 파생값(매출 환산 계수 17·4900·1300 등)은 `StatService`/`DataInitializer`와 **동일 상수**로 묶어 데모 리셋 시 동일 숫자 재현. 상수는 `DashboardService` 내 `private static final` 또는 별도 `DashboardConstants`로 한 곳에 모은다.
- Phase D2 착수 시 `seedOrders`/`seedDonations`/`seedGoods`/`seedInquiries` 추가(설계 7.2 분포: 주문 70% DONE·15% SHIPPING·10% PAID·5% CANCEL, 파레토 롱테일) → 대시보드 SQL만 실 테이블로 교체.

---

## 6. 구현 순서 체크리스트 (의존 순서, 파일 단위)

**백엔드 (먼저)**
1. `dto/KpiDelta.java`, `dto/TrendRow.java`(internal), `dto/FeedRow.java`(internal) — 의존 없는 값 객체.
2. `dto/DashboardSummaryResponse.java`, `TimeseriesResponse.java`, `FeedItem.java`, `MembershipSliceItem.java`, `TodoQueueResponse.java`.
3. `mapper/DashboardMapper.java` 인터페이스.
4. `resources/mappers/DashboardMapper.xml` — SQL (StatMapper.xml 스타일, `&gt;=` 이스케이프).
5. `DashboardService.java` — `@Cacheable` + 집계 합성 + 결정론 파생 상수.
6. `DashboardController.java` — 5 엔드포인트, `@Tag`/`@Operation`/`@PreAuthorize`.
7. `seedActionLogs` 다양성 보강(필요 시) — `DataInitializer.java`.
8. `./mvnw test`(있으면 `DashboardMapperTest`/`DashboardServiceTest`) → `make fmt && test && lint` 그린 확인.
9. 로컬 기동(Colima 인프라) 후 `/swagger-ui`에서 5 엔드포인트 200 + `resultObject` 확인.

**프론트 (백엔드 배포 후)**
10. 사용자에게 "백엔드 배포됐나요?" 확인 → `npm run gen` → `apis/query/dashboard/dashboard.ts` 생성 확인.
11. `lib/relative-time.ts`.
12. `components/dashboard/KpiCard.tsx` → `KpiStrip.tsx`.
13. `components/dashboard/RevenueTrendChart.tsx`, `MembershipDonutChart.tsx`(WatchByChannelChart 복제).
14. `components/dashboard/ActivityFeed.tsx`(슬라이딩 재생), `TodoQueue.tsx`(딥링크).
15. `app/(protected)/dashboard/page.tsx` 재구성.
16. `Sidebar.tsx` 레이블 변경.
17. `npm run lint && npm run build:no-lint` 그린 + 브라우저 육안 확인(카운트업·피드 슬라이딩·딥링크 동작).

> 5개 이상 파일 변경 작업이므로 착수 전 변경 대상 목록을 사용자에게 보고하고 승인받는다(프로젝트 규칙).

---

## 7. 위험 / 주의

- **존재하지 않는 도메인 의존**: KPI 6종 중 4종(매출/주문/문의/재고)은 실 엔티티가 없다. D1 매핑(콘텐츠·회원·시청 파생)을 정직하게 구현하고, 필드명은 도메인 언어 유지 + 코드 주석에 "D1 파생, D2에서 실테이블 교체" 명시. **실제 매출인 척 과장 금지**(설계 9장 정직 원칙·푸터 "데모 데이터" 배지).
- **캐시 스탬피드 / 신선도 충돌**: `/summary`·`/timeseries`는 60s 캐시인데 활동 피드는 15s 폴링이라 KPI와 피드가 미세하게 어긋나 보일 수 있음 → 의도된 동작(관제실은 위젯별 갱신 주기 다른 게 자연스러움). 단 KPI "오늘" 경계(자정)에서 캐시가 60s 동안 전일 값을 보일 수 있으니 `key`에 날짜 미포함 + TTL 60s로 수용.
- **타임존**: `LocalDateTime.now()`는 서버 TZ 기준. 상대시간을 프론트에서 계산하면 클라이언트 TZ와 어긋날 수 있음 → 서버가 항상 절대 `occurredAt`(서버 TZ) 내려주고, 프론트 `relative-time.ts`는 `Date.now()` 대비 단순 차이만 사용(데모 단일 조직이라 TZ 통일 가정).
- **빈 날짜 구멍**: `/timeseries`에서 from~to 연속 채우기를 빠뜨리면 차트가 깨짐(3.5b 강조). 반드시 날짜 루프로 0 채움.
- **ActionLog 비동기 지연**: 피드 원천인 `ACTION_LOG`는 SQS Consumer가 비동기 저장 → 방금 한 액션이 즉시 피드에 안 보일 수 있음. 데모에선 시드 타임라인이 메인 소스라 영향 적음(실시간 액션은 부차적).
- **Orval gen 타이밍**: 백엔드 미배포 상태에서 `npm run gen` 절대 금지(프로젝트 규칙). 프론트 작업은 반드시 백엔드 배포·스펙 확인 후.
- **성능**: D2에서 ORDER/DONATION이 수천 건 되면 `/summary` 서브쿼리가 무거워질 수 있음 → 이미 60s 캐시로 흡수. 단 `idx_order_ordered`/`idx_donation_paid` 인덱스 필수(2.2 정의).
