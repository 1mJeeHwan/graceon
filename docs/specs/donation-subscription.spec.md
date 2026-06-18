# 정기후원 + 구독 멤버십 도메인 구현 스펙

> 대상 코드베이스: `streamhub-admin/streamhub-api` (Spring Boot 3.4 / Java 21) + `streamhub-admin/streamhub-web` (Next.js 14 App Router).
> 본 스펙은 실제 `content` / `member` / `statistics` 도메인의 패턴을 100% 그대로 따른다. (ResultDTO 래퍼, JPA+MyBatis 혼용, `@PreAuthorize` 클래스 가드, AG Grid v33 Quartz 테마, Orval 훅, ResInfinityList 페이지네이션.)

---

## (1) 목적 / 범위

영카트5 `subscription_admin`(정기결제) 메뉴를 레퍼런스 서비스의 **정기후원 / 멤버십 구독 + 단건 후원** 도메인으로 재해석하여 구현한다. 핵심 어필 포인트는 **빌링키 기반 정기청구 CRON 시뮬레이션**(실 PG 미연동, `next_billing_at` 도래 건을 `@Scheduled` 스캐너가 스캔 → 회차 `donation` + `point_ledger` 자동 생성 → `next_billing_at` 전진)과 **결제일정 캘린더 집계**다. 백엔드는 멤버십 플랜 CRUD, 구독 라이프사이클(`ACTIVE/PAUSED/CANCELED`) 전이, 후원내역 목록(상태·기간·유형 필터), 결제일정 캘린더 월별 집계를, 프론트는 플랜 관리·후원내역·구독 현황·결제일정 캘린더 4개 화면을 제공한다. 모든 청구는 `test_mode`로 고정되어 실제 결제는 발생하지 않으며 빌링키는 마스킹된 데모 값이다.

---

## (2) 데이터 모델

신규 패키지 루트: `org.streamhub.api.v1.donation`. 엔티티 3종 + 포인트 원장 1종. 기존 컨벤션(대문자 테이블, snake_case `@Column`, `@Index(name="idx_{table}_{용도}")`, `@NoArgsConstructor(PROTECTED)` + `@Builder` private 생성자, `created_at`/`updated_at` 직접 필드, `LocalDateTime.now()` 기본값) 준수.

### 2.1 `SUBSCRIPTION_PLAN` (멤버십 플랜)
엔티티: `donation/entity/SubscriptionPlan.java`

| 필드(Java) | @Column | 타입 | 제약 / 비고 |
|---|---|---|---|
| id | id | Long | PK, IDENTITY |
| name | name | String | `nullable=false, length=100` (예: 후원천사) |
| grade | grade | `PlanGrade` | `@Enumerated(STRING)`, `nullable=false, length=20` (BRONZE/SILVER/GOLD/ANGEL) |
| price | price | Long | `nullable=false` (월 청구액, KRW) |
| periodMonths | period_months | Integer | `nullable=false` (청구 주기 개월, 기본 1) |
| pointRate | point_rate | Integer | `nullable=false` (청구액 대비 은혜포인트 적립률 %, 예: 5) |
| benefit | benefit | String | `length=500` (혜택 설명) |
| active | active | String | `nullable=false, length=1` ("Y"/"N") — 기존 `liveYn`/`openYn` 컨벤션 따름 |
| createdAt | created_at | LocalDateTime | `nullable=false` |
| updatedAt | updated_at | LocalDateTime | `nullable=false` |

인덱스: `@Index(name="idx_plan_active", columnList="active")`, `@Index(name="idx_plan_grade", columnList="grade")`.
비즈니스 메서드: `update(name, grade, price, periodMonths, pointRate, benefit, active)`, `deactivate()` — 각각 `updatedAt = LocalDateTime.now()`.
Enum: `donation/entity/PlanGrade.java` → `{ BRONZE, SILVER, GOLD, ANGEL }`.

### 2.2 `SUBSCRIPTION` (구독 = 정기후원 약정)
엔티티: `donation/entity/Subscription.java`

| 필드 | @Column | 타입 | 제약 / 비고 |
|---|---|---|---|
| id | id | Long | PK, IDENTITY |
| memberId | member_id | Long | `nullable=false` (FK → MEMBER) |
| planId | plan_id | Long | `nullable=false` (FK → SUBSCRIPTION_PLAN) |
| billingKeyMasked | billing_key_masked | String | `nullable=false, length=40` (예: `bk_****1234`, 데모 마스킹) |
| status | status | `SubscriptionStatus` | `@Enumerated(STRING)`, `nullable=false, length=20` (ACTIVE/PAUSED/CANCELED) |
| cycleNo | cycle_no | Integer | `nullable=false` (지금까지 청구된 회차, 시작 0) |
| nextBillingAt | next_billing_at | LocalDateTime | `nullable=true` (다음 청구 예정; CANCELED·PAUSED는 null) |
| startedAt | started_at | LocalDateTime | `nullable=false` |
| canceledAt | canceled_at | LocalDateTime | `nullable=true` |
| createdAt | created_at | LocalDateTime | `nullable=false` |
| updatedAt | updated_at | LocalDateTime | `nullable=false` |

인덱스: `@Index(name="idx_subscription_member", columnList="member_id")`, `@Index(name="idx_subscription_status", columnList="status")`, `@Index(name="idx_subscription_next_billing", columnList="status, next_billing_at")` (CRON 스캔 복합 인덱스).
Enum: `donation/entity/SubscriptionStatus.java` → `{ ACTIVE, PAUSED, CANCELED }`.
비즈니스 메서드(상태머신 — 서비스가 아니라 엔티티가 전이 책임):
- `pause()`: `status==ACTIVE`만 허용 → `PAUSED`, `nextBillingAt=null`, touch updatedAt. 위반 시 `IllegalStateException`(서비스가 잡아 `ApiException(INVALID_PARAMETER)` 변환).
- `resume(LocalDateTime nextBillingAt)`: `status==PAUSED`만 → `ACTIVE`, `nextBillingAt` 재설정.
- `cancel()`: `CANCELED` 아니면 → `CANCELED`, `nextBillingAt=null`, `canceledAt=now`.
- `advanceCycle(LocalDateTime next)`: 회차 청구 성공 시 `cycleNo++`, `nextBillingAt=next`, touch updatedAt. (CRON 전용)

### 2.3 `DONATION` (후원 내역 = 청구 1건)
엔티티: `donation/entity/Donation.java`

| 필드 | @Column | 타입 | 제약 / 비고 |
|---|---|---|---|
| id | id | Long | PK, IDENTITY |
| memberId | member_id | Long | `nullable=false` |
| subscriptionId | subscription_id | Long | `nullable=true` (단건 후원은 null) |
| type | type | `DonationType` | `@Enumerated(STRING)`, `nullable=false, length=20` (ONCE/SUBSCRIPTION) |
| amount | amount | Long | `nullable=false` (KRW) |
| cycleNo | cycle_no | Integer | `nullable=true` (정기후원 회차; 단건 null) |
| status | status | `DonationStatus` | `@Enumerated(STRING)`, `nullable=false, length=20` (PAID/CANCELED/FAILED) |
| pointAwarded | point_awarded | Long | `nullable=false` (이번 건 적립 포인트, 기본 0) |
| testMode | test_mode | String | `nullable=false, length=1` ("Y" 고정 — 데모 안전 라벨) |
| paidAt | paid_at | LocalDateTime | `nullable=false` |
| createdAt | created_at | LocalDateTime | `nullable=false` |

인덱스: `@Index(name="idx_donation_member", columnList="member_id")`, `@Index(name="idx_donation_subscription", columnList="subscription_id")`, `@Index(name="idx_donation_status_paid", columnList="status, paid_at")`, `@Index(name="idx_donation_type", columnList="type")`.
Enums: `donation/entity/DonationType.java` → `{ ONCE, SUBSCRIPTION }`; `donation/entity/DonationStatus.java` → `{ PAID, CANCELED, FAILED }`.

### 2.4 `POINT_LEDGER` (은혜 포인트 원장)
엔티티: `donation/entity/PointLedger.java` — 후원 적립의 불변 추적(append-only). 설계문서 §6.1 `point_ledger` 대응.

| 필드 | @Column | 타입 | 제약 / 비고 |
|---|---|---|---|
| id | id | Long | PK, IDENTITY |
| memberId | member_id | Long | `nullable=false` |
| delta | delta | Long | `nullable=false` (증감; 적립 +, 사용 −) |
| balanceAfter | balance_after | Long | `nullable=false` (적용 후 잔액) |
| reason | reason | String | `nullable=false, length=200` (예: "정기후원 3회차 적립") |
| donationId | donation_id | Long | `nullable=true` (출처 후원 건) |
| createdAt | created_at | LocalDateTime | `nullable=false` |

인덱스: `@Index(name="idx_point_ledger_member", columnList="member_id")`, `@Index(name="idx_point_ledger_created", columnList="created_at")`.
> 주의: `balanceAfter` 계산은 **회원별 직전 잔액 조회** 필요. CRON과 단건 후원이 동시 기록 시 경쟁 발생 가능 → §7 위험 참고. 본 데모는 단일 스케줄러 스레드 + `@Transactional`로 직렬화한다.

---

## (3) 백엔드

### 3.1 생성할 파일 목록 (정확한 패키지 경로)

루트: `streamhub-api/src/main/java/org/streamhub/api/`

**엔티티 / Enum** (`v1/donation/entity/`)
- `SubscriptionPlan.java`, `PlanGrade.java`
- `Subscription.java`, `SubscriptionStatus.java`
- `Donation.java`, `DonationType.java`, `DonationStatus.java`
- `PointLedger.java`

**Repository** (`v1/donation/repository/`) — JPA, 단순 CRUD + CRON 스캔 쿼리만
- `SubscriptionPlanRepository.java` — `extends JpaRepository<SubscriptionPlan, Long>`
- `SubscriptionRepository.java` — 아래 쿼리 메서드 포함
  ```java
  List<Subscription> findByStatusAndNextBillingAtBefore(SubscriptionStatus status, LocalDateTime now);
  long countByPlanIdAndStatus(Long planId, SubscriptionStatus status);
  ```
- `DonationRepository.java` — `extends JpaRepository<Donation, Long>`
- `PointLedgerRepository.java`
  ```java
  Optional<PointLedger> findTopByMemberIdOrderByIdDesc(Long memberId); // 직전 잔액
  ```

**DTO** (`v1/donation/dto/`) — record 요청 / `@Getter @Setter @NoArgsConstructor` 응답 (ContentListItem 컨벤션)
- `PlanCreateRequest.java` (record, `@NotBlank`/`@NotNull` 검증)
- `PlanResponse.java` (응답; activeSubscriptionCount 조인 포함)
- `SubscriptionSearchRequest.java` (record, pageNumber/pageSize/keyword/status/planId + `pageSizeOrDefault()`/`offset()`)
- `SubscriptionListItem.java` (조인: memberName, planName, planGrade)
- `SubscriptionDetail.java`
- `SubscriptionStatusRequest.java` (record: `@NotNull SubscriptionStatus status`)
- `DonationSearchRequest.java` (record: pageNumber/pageSize/keyword/type/status/from/to + 헬퍼)
- `DonationListItem.java` (조인: memberName, planName)
- `BillingCalendarRequest.java` (record: `Integer year, Integer month`)
- `BillingCalendarItem.java` (응답: `String date`, `long count`, `long amount`)
- `OnceDonationRequest.java` (record: `@NotNull Long memberId, @NotNull @Positive Long amount`) — 단건 후원 수기 등록

**Mapper** (`v1/donation/mapper/`) — MyBatis(조인/필터/집계). XML은 `resources/mappers/`
- `SubscriptionMapper.java` (`@Mapper`) — `selectList`, `countList`, `selectDetail`
- `DonationMapper.java` (`@Mapper`) — `selectList`, `countList`, `billingCalendar`(ACTIVE 구독의 next_billing_at 월별 집계)

**Mapper XML** (`streamhub-api/src/main/resources/mappers/`)
- `SubscriptionMapper.xml`
- `DonationMapper.xml`

**Service** (`v1/donation/`)
- `SubscriptionPlanService.java` — 플랜 CRUD
- `SubscriptionService.java` — 구독 목록/상세/상태전이
- `DonationService.java` — 후원내역 목록 + 단건 후원 등록 + 캘린더 집계
- `BillingScheduler.java` — `@Scheduled` 정기청구 시뮬레이터 (핵심 어필)
- `BillingService.java` — 회차 청구 1건의 트랜잭션 경계(스케줄러가 호출). `Subscription.advanceCycle` + `Donation` + `PointLedger`를 한 트랜잭션에.

**Controller** (`v1/donation/`)
- `SubscriptionPlanController.java` — `@RequestMapping("/v1/subscription-plan")`
- `SubscriptionController.java` — `@RequestMapping("/v1/subscription")`
- `DonationController.java` — `@RequestMapping("/v1/donation")`

**설정 변경(신규 파일 아님, 수정)**
- `org.streamhub.api.StreamhubApiApplication` 또는 신규 `base/config/SchedulingConfig.java`에 **`@EnableScheduling` 추가** (현재 코드베이스에 없음 — 반드시 추가).
- `base/config/DataInitializer.java` — 시드 메서드 추가 (§5).

> 모든 컨트롤러 클래스 가드는 기존과 동일:
> ```java
> @PreAuthorize("hasAnyAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM, "
>         + "T(org.streamhub.api.base.security.AuthoritiesConstants).CHURCH_MANAGER)")
> ```
> 본 데모는 단일 조직이므로 member/content와 동일하게 **church 스코핑 없이** 두 권한 모두 전체 접근(설계 단순화). 플랜 삭제(`DELETE`)만 SYSTEM 전용으로 메서드 레벨 `@PreAuthorize` 추가 권장(파괴적 액션 가드).

### 3.2 API 엔드포인트 표

| 메서드 | 경로 | 요청 DTO | 응답(`ResultDTO<T>`) | 권한 |
|---|---|---|---|---|
| GET | `/v1/subscription-plan/list` | — (전체) | `ResultDTO<List<PlanResponse>>` | SYSTEM/MANAGER |
| GET | `/v1/subscription-plan/{id}` | path id | `ResultDTO<PlanResponse>` | SYSTEM/MANAGER |
| POST | `/v1/subscription-plan` | `@Valid PlanCreateRequest` | `ResultDTO<PlanResponse>` | SYSTEM/MANAGER |
| PUT | `/v1/subscription-plan/{id}` | `@Valid PlanCreateRequest` | `ResultDTO<PlanResponse>` | SYSTEM/MANAGER |
| DELETE | `/v1/subscription-plan/{id}` | path id | `ResultDTO<Void>` | **SYSTEM only** |
| POST | `/v1/subscription/list` | `SubscriptionSearchRequest` | `ResultDTO<ResInfinityList<SubscriptionListItem>>` | SYSTEM/MANAGER |
| GET | `/v1/subscription/{id}` | path id | `ResultDTO<SubscriptionDetail>` | SYSTEM/MANAGER |
| PUT | `/v1/subscription/{id}/status` | `@Valid SubscriptionStatusRequest` | `ResultDTO<SubscriptionDetail>` | SYSTEM/MANAGER |
| POST | `/v1/donation/list` | `DonationSearchRequest` | `ResultDTO<ResInfinityList<DonationListItem>>` | SYSTEM/MANAGER |
| POST | `/v1/donation/once` | `@Valid OnceDonationRequest` | `ResultDTO<DonationListItem>` | SYSTEM/MANAGER |
| POST | `/v1/donation/calendar` | `BillingCalendarRequest` | `ResultDTO<List<BillingCalendarItem>>` | SYSTEM/MANAGER |
| POST | `/v1/donation/run-billing` | — | `ResultDTO<Integer>` (처리 건수) | SYSTEM only — 데모용 수동 트리거(스케줄 대기 없이 즉시 청구 시연) |

> 컨벤션: 목록은 POST `/list` + `@RequestBody SearchRequest`(content/member와 동일), 단건은 GET `/{id}`, 생성 POST 루트, 수정 PUT `/{id}`. `@Operation(summary="한글")` 필수. `MapperScan("org.streamhub.api.**.mapper")`가 신규 매퍼 자동 등록.

### 3.3 핵심 로직 의사코드

#### (a) 구독 상태전이 — `SubscriptionService`
```
@Transactional
SubscriptionDetail changeStatus(Long id, SubscriptionStatusRequest req):
    sub = subscriptionRepository.findById(id)
            .orElseThrow(() -> new ApiException(NOT_FOUND))
    try:
        switch (req.status()):
            PAUSED   -> sub.pause()
            CANCELED -> sub.cancel()
            ACTIVE   -> sub.resume(nextBillingFrom(LocalDateTime.now(), sub.planPeriodMonths()))
                        // resume 시 다음 청구일 = 오늘 + periodMonths
    catch (IllegalStateException e):
        throw new ApiException(INVALID_PARAMETER, "허용되지 않는 상태 전이입니다")
    subscriptionRepository.saveAndFlush(sub)
    actionLogPublisher.publish("SUBSCRIPTION_STATUS", "SUBSCRIPTION", String.valueOf(id), req.status().name())
    return subscriptionMapper.selectDetail(id)   // JPA flush 후 MyBatis 재조회 (content 패턴)
```

#### (b) 정기청구 CRON 시뮬 — `BillingScheduler` + `BillingService` (★핵심)
```
// BillingScheduler — @Component, @Slf4j
@Scheduled(cron = "${app.billing.cron:0 */5 * * * *}")   // 데모: 5분마다 스캔
void runDueBilling():
    now = LocalDateTime.now()
    due = subscriptionRepository.findByStatusAndNextBillingAtBefore(ACTIVE, now)
    if due.isEmpty(): return
    int processed = 0
    for sub in due:
        try:
            billingService.chargeOneCycle(sub.getId(), now)   // 각 건 독립 트랜잭션
            processed++
        catch (RuntimeException e):
            log.warn("Billing failed for subscription {}: {}", sub.getId(), e.getMessage())
            // 한 건 실패가 전체 배치를 막지 않음 (best-effort, ActionLogPublisher 패턴과 동일 철학)
    log.info("Billing run: {} subscriptions charged", processed)

// BillingService.chargeOneCycle — @Transactional (건별 경계, 부분 실패 격리)
@Transactional
void chargeOneCycle(Long subscriptionId, LocalDateTime now):
    sub  = subscriptionRepository.findById(subscriptionId).orElseThrow(...)
    if sub.getStatus() != ACTIVE: return                      // 그 사이 일시정지/해지 방어
    plan = planRepository.findById(sub.getPlanId()).orElseThrow(...)

    // 1) 회차 후원(donation) 생성 — test 모드 고정, 실 PG 미연동
    point = plan.getPrice() * plan.getPointRate() / 100
    donation = Donation.builder()
                 .memberId(sub.getMemberId())
                 .subscriptionId(sub.getId())
                 .type(SUBSCRIPTION)
                 .amount(plan.getPrice())
                 .cycleNo(sub.getCycleNo() + 1)
                 .status(PAID)                                  // 테스트모드 항상 성공
                 .pointAwarded(point)
                 .testMode("Y")
                 .paidAt(now)
                 .build()
    saved = donationRepository.save(donation)

    // 2) 포인트 원장 append (회원별 직전 잔액 기준)
    prev = pointLedgerRepository.findTopByMemberIdOrderByIdDesc(sub.getMemberId())
    balance = (prev == null ? 0 : prev.getBalanceAfter()) + point
    pointLedgerRepository.save(PointLedger.builder()
        .memberId(sub.getMemberId()).delta(point).balanceAfter(balance)
        .reason("정기후원 " + donation.getCycleNo() + "회차 적립")
        .donationId(saved.getId()).build())

    // 3) 구독 회차 전진 + 다음 청구일 = now + plan.periodMonths
    sub.advanceCycle(now.plusMonths(plan.getPeriodMonths()))
    subscriptionRepository.saveAndFlush(sub)

    actionLogPublisher.publish("BILLING_CHARGE", "SUBSCRIPTION", String.valueOf(sub.getId()),
        "회차 " + donation.getCycleNo() + " / ₩" + plan.getPrice())
```
> `POST /v1/donation/run-billing`는 `billingScheduler.runDueBilling()`를 직접 호출하여 데모 시연 시 스케줄 대기 없이 즉시 청구를 보여준다(반환: processed 건수).

#### (c) 단건 후원 등록 — `DonationService.createOnce`
```
@Transactional
DonationListItem createOnce(OnceDonationRequest req):
    member = memberRepository.findById(req.memberId()).orElseThrow(NOT_FOUND)  // memberRepository 주입
    point  = req.amount() / 100                                  // 단건은 1% 적립(정책 상수)
    donation = Donation.builder().memberId(req.memberId()).subscriptionId(null)
                 .type(ONCE).amount(req.amount()).cycleNo(null).status(PAID)
                 .pointAwarded(point).testMode("Y").paidAt(LocalDateTime.now()).build()
    saved = donationRepository.save(donation)
    appendPointLedger(req.memberId(), point, "단건 후원 적립", saved.getId())   // (b)-2와 동일 헬퍼
    actionLogPublisher.publish("DONATION_ONCE", "DONATION", String.valueOf(saved.getId()), "₩" + req.amount())
    return donationMapper.selectDetail(saved.getId())  // 또는 mapper.selectList 단건 재조회
```

#### (d) 결제일정 캘린더 집계 — `DonationMapper.xml`
ACTIVE 구독의 `next_billing_at`을 해당 월 범위로 GROUP BY 하여 날짜별 건수·금액 합계. (StatMapper 집계 패턴 그대로.)
```xml
<select id="billingCalendar" resultType="org.streamhub.api.v1.donation.dto.BillingCalendarItem">
    SELECT
        DATE_FORMAT(s.next_billing_at, '%Y-%m-%d') AS date,
        COUNT(*)                                   AS count,
        COALESCE(SUM(p.price), 0)                  AS amount
    FROM SUBSCRIPTION s
        JOIN SUBSCRIPTION_PLAN p ON s.plan_id = p.id
    WHERE s.status = 'ACTIVE'
      AND s.next_billing_at BETWEEN #{from} AND #{to}
    GROUP BY DATE_FORMAT(s.next_billing_at, '%Y-%m-%d')
    ORDER BY date
</select>
```
서비스: `BillingCalendarRequest(year, month)` → `from = LocalDateTime.of(y, m, 1, 0,0)`, `to = from.plusMonths(1).minusSeconds(1)` 계산해 `@Param("from")`/`@Param("to")` 전달.

#### (e) 후원내역 동적 필터 — `DonationMapper.xml`
```xml
<sql id="searchWhere">
    <where>
        <if test="keyword != null and keyword != ''">
            AND m.name LIKE CONCAT('%', #{keyword}, '%')
        </if>
        <if test="type != null and type != ''">     AND d.type = #{type} </if>
        <if test="status != null and status != ''">  AND d.status = #{status} </if>
        <if test="from != null"> AND d.paid_at &gt;= #{from} </if>
        <if test="to != null">   AND d.paid_at &lt;= #{to} </if>
    </where>
</sql>
```
`selectList`: `FROM DONATION d JOIN MEMBER m ON d.member_id = m.id LEFT JOIN SUBSCRIPTION s ON d.subscription_id = s.id LEFT JOIN SUBSCRIPTION_PLAN p ON s.plan_id = p.id ... <include refid="searchWhere"/> ORDER BY d.paid_at DESC, d.id DESC LIMIT #{offset}, #{size}`. `countList`는 `FROM DONATION d JOIN MEMBER m ... <include refid="searchWhere"/>`만.
Mapper 인터페이스 시그니처는 ContentMapper와 동일하게 `@Param` 나열(`keyword, type, status, from, to, offset, size`).

---

## (4) 프론트

### 4.1 생성할 파일 목록 (정확한 경로)

루트: `streamhub-web/src/`

**자동생성(Orval) — 직접 작성 금지, `npm run gen`으로 생성**
- `apis/query/subscription-plan/subscription-plan.ts`
- `apis/query/subscription/subscription.ts`
- `apis/query/donation/donation.ts`
- `apis/query/streamHubAdminAPI.schemas.ts` 갱신 (신규 타입/enum 추가)

**라우트 (App Router, `app/(protected)/`)**
- `app/(protected)/subscription-plan/page.tsx` — 멤버십 플랜 관리(목록 + 등록/수정 모달 또는 인라인)
- `app/(protected)/subscription/page.tsx` — 구독 현황 목록(상태 전이 액션)
- `app/(protected)/subscription/[id]/page.tsx` — 구독 상세(상태 전이 + 회차 이력)
- `app/(protected)/donation/page.tsx` — 후원 내역 목록(필터 + 단건 후원 등록)
- `app/(protected)/billing-calendar/page.tsx` — 결제일정 캘린더

**컴포넌트 (`src/components/`)**
- `components/subscription/SubscriptionGrid.tsx` — AG Grid v33
- `components/subscription/SubscriptionStatusBadge.tsx` — StatusBadge 패턴 복제
- `components/donation/DonationGrid.tsx`
- `components/donation/DonationTypeBadge.tsx`
- `components/billing-calendar/BillingCalendar.tsx` — 월 그리드(7열 CSS Grid, 날짜별 셀에 건수·금액)
- `components/plan/PlanForm.tsx` — RHF + Zod (등록/수정 공용)

### 4.2 화면별 설계

#### 멤버십 플랜 관리 (`subscription-plan/page.tsx`)
- **데이터패칭**: `useQuery({ queryKey: ["plan-list"], queryFn: ({signal}) => list(signal) })` — GET 전체 목록(페이지네이션 없음, 소수). 카드 그리드 또는 단순 테이블.
- **폼(`PlanForm`, RHF + Zod)**:
  ```ts
  const planSchema = z.object({
    name: z.string().min(1, "플랜명을 입력하세요."),
    grade: z.enum(["BRONZE","SILVER","GOLD","ANGEL"]),
    price: z.coerce.number().int().positive("금액은 0보다 커야 합니다."),
    periodMonths: z.coerce.number().int().positive().default(1),
    pointRate: z.coerce.number().int().min(0).max(100).default(5),
    benefit: z.string().optional(),
    active: z.enum(["Y","N"]).default("Y"),
  });
  ```
  제출: `createMutation`/`updateMutation` → `onSuccess` 시 `resultCode === SUCCESS_CODE` 확인 후 `refetch()`.
- 삭제 버튼: VIEWER/MANAGER 비활성, 확인 모달(데모 안전). `isSystem(session?.user?.role)`로 가드.

#### 구독 현황 (`subscription/page.tsx`)
- **상태관리**: member/page.tsx와 동일 Committed/Draft 분리 + `pageNumber`(1-based UI → `pageNumber-1` 전송), `PAGE_SIZE = 10`.
- **데이터패칭**: content/page.tsx 패턴 — `useQuery({ queryKey: ["subscription-list", searchRequest], queryFn: ({signal}) => list(searchRequest, signal), placeholderData: keepPreviousData })`. (목록 POST가 Orval에서 useMutation으로 떨어지면 member 패턴인 `useEffect + fetchList` 사용 — 실제 생성 결과에 맞춤.)
- **필터**: 상태 `STATUS_OPTIONS = [ALL, ACTIVE, PAUSED, CANCELED]` + keyword(회원명).
- **상태 전이 액션**: 행 또는 상세에서 일시정지/재개/해지 → `useChangeStatus().mutate({ id, data: { status } })` → `onSuccess` refetch. 해지는 확인 모달.
- **AG Grid `SubscriptionGrid` columnDefs** (`ColDef<SubscriptionListItem>[]`):
  | field | headerName | 비고 |
  |---|---|---|
  | memberName | 회원 | flex 1 |
  | planName | 플랜 | flex 1 |
  | planGrade | 등급 | cellRenderer: grade 배지 |
  | status | 상태 | cellRenderer `<SubscriptionStatusBadge status={value}/>` |
  | cycleNo | 회차 | minWidth 80 |
  | nextBillingAt | 다음 청구 | valueFormatter `formatDate` |
  | startedAt | 시작일 | valueFormatter `formatDate` |
  | (액션) | 상세 | cellRenderer 버튼 → `router.push('/subscription/'+data.id)` |
  - 테마: `themeQuartz.withParams({ accentColor:"#2563eb", borderColor:"#e2e8f0", headerBackgroundColor:"#f8fafc", fontSize:13, rowHeight:48 })`, `ModuleRegistry.registerModules([AllCommunityModule])`, `overlayNoRowsTemplate="조회된 구독이 없습니다."`. `dynamic(..., { ssr:false })`로 import.

#### 후원 내역 (`donation/page.tsx`)
- **데이터패칭**: 위와 동일 useQuery + keepPreviousData. `searchRequest`: `{ pageNumber:pageNumber-1, pageSize:10, keyword, type, status, from, to }`.
- **필터**: 유형 `[ALL, ONCE, SUBSCRIPTION]`, 상태 `[ALL, PAID, CANCELED, FAILED]`, 기간 `from`/`to`(date input, ISO 변환).
- **단건 후원 등록**: 상단 우측 버튼 → 모달(RHF+Zod: memberId select, amount number) → `useCreateOnce().mutate({data})` → refetch.
- **`DonationGrid` columnDefs** (`ColDef<DonationListItem>[]`): memberName / planName(없으면 "-") / type(`<DonationTypeBadge/>`) / amount(`formatNumber` + "원") / cycleNo(`valueFormatter: v ?? "-"`) / status 배지 / pointAwarded(`formatNumber`) / paidAt(`formatDateTime`). 우측 `testMode="Y"` 표시용 "테스트" 배지 컬럼.

#### 결제일정 캘린더 (`billing-calendar/page.tsx`)
- **상태**: `year`/`month`(기본 현재월), 이전/다음 월 네비게이션.
- **데이터패칭**: `useQuery({ queryKey:["billing-calendar", year, month], queryFn:({signal}) => calendar({ year, month }, signal) })` → `BillingCalendarItem[]`(date/count/amount).
- **`BillingCalendar` 컴포넌트**: 7열 CSS Grid(`grid-cols-7`), 월 1일~말일 셀 렌더. 각 셀에 날짜 + (해당 date 매칭 시) `N건 / ₩금액` 뱃지(brand 색). 데이터는 `Map<string, BillingCalendarItem>`로 인덱싱. ApexCharts 불필요(순수 div 그리드 — action-log의 HTML 테이블처럼 경량 자작).
- 상단 요약: 이번 달 총 청구 예정 건수·금액(items 합계). "데모 데이터 · 테스트 모드" 라벨 표기(설계 §9).

### 4.3 사이드바 메뉴 추가
`src/components/layout/Sidebar.tsx`의 `NAV_ITEMS`에 추가(아이콘은 `lucide-react`에서 import):
```ts
import { LayoutDashboard, Users, FileVideo, ScrollText,
         HeartHandshake, CreditCard, CalendarClock, Gem } from "lucide-react";
// ...기존 항목 뒤에:
{ label: "멤버십 플랜", href: "/subscription-plan", icon: Gem },
{ label: "구독 현황", href: "/subscription", icon: CreditCard },
{ label: "후원 내역", href: "/donation", icon: HeartHandshake },
{ label: "결제일정", href: "/billing-calendar", icon: CalendarClock },
```
`isActive`는 `pathname.startsWith(\`${item.href}/\`)`가 이미 상세 라우트를 커버한다.

---

## (5) 시드 영향

`base/config/DataInitializer.java`에 신규 Repository 4종 주입 + 시드 메서드 추가. **`run()` 호출 순서**는 `seedMembers()` 이후(FK memberId 의존), 결정론적·멱등(`if (repo.count() > 0) return;`).

```
run():
    seedAdmins(); seedOrganization(); seedMembers(); seedContent();
    seedWatchHistory(); seedActionLogs(); seedPosts();
    seedSubscriptionPlans();   // 추가 — FK 없음(플랜 정의)
    seedSubscriptions();       // 추가 — memberId, planId FK
    seedDonations();           // 추가 — memberId, subscriptionId FK (과거 청구 이력 + point_ledger)
```

- **`seedSubscriptionPlans()`** — 4개 등급 플랜 고정 등록:
  `브론즈(BRONZE, ₩5,000, 적립 3%)`, `실버(SILVER, ₩10,000, 5%)`, `골드(GOLD, ₩30,000, 7%)`, `후원천사(ANGEL, ₩50,000, 10%)`. 모두 `active="Y"`, `periodMonths=1`.
- **`seedSubscriptions()`** — 회원 60명 중 약 절반에 구독 부여(결정론적 인덱싱). 분포(설계 §7.2 라이프사이클 다양성):
  - `i % 5 == 4` → CANCELED(`nextBillingAt=null`, `canceledAt` 과거)
  - `i % 5 == 3` → PAUSED(`nextBillingAt=null`)
  - 그 외 → ACTIVE. ACTIVE의 `nextBillingAt`는 `now.plusDays(i % 28)`로 분산 → **캘린더가 이번 달에 자연 분포**. 일부는 `now.minusMinutes(...)`(과거)로 두어 **CRON 첫 스캔이 즉시 청구**하도록 1~2건 심어 데모 효과.
  - `cycleNo`는 `i % 12`(가입 후 경과 회차), `startedAt = now.minusMonths(i % 12)`, `billingKeyMasked = String.format("bk_****%04d", i)`, `planId = plans.get(i % 4).getId()`.
- **`seedDonations()`** — 각 구독의 과거 회차를 채워 후원 내역 충실화 + 단건 후원 일부:
  - 구독별 `cycleNo` 만큼 과거 `DONATION`(type=SUBSCRIPTION, status=PAID, `paidAt = startedAt.plusMonths(n)`, `pointAwarded = price*pointRate/100`) 생성.
  - 단건 후원(type=ONCE) 30여 건을 최근 6개월에 `paidAt` 분산(`now.minusDays(i*5)`), `amount`는 `10000 + (i%5)*5000`.
  - 각 PAID 건마다 `POINT_LEDGER` append(회원별 누적 `balanceAfter` 계산 — 시드는 회원별 Map으로 잔액 누적 후 일괄 saveAll). 상태 분포: 일부 `CANCELED`/`FAILED`를 섞어 "운영의 흠집" 연출(설계 §9).
- **WATCH_HISTORY 시계열 패턴(소수 곱 `(i*13)%n`)**을 동일 차용해 자연 분포 유지.

> 신규 Repository 주입은 생성자에 4개 파라미터 추가 → `this.xxxRepository = xxxRepository`. (DataInitializer 기존 패턴 그대로.)

---

## (6) 구현 순서 체크리스트 (파일 단위, 의존 순서)

```
[ B1] @EnableScheduling 추가 (StreamhubApiApplication 또는 SchedulingConfig.java)
[ B2] Enum: PlanGrade, SubscriptionStatus, DonationType, DonationStatus
[ B3] 엔티티: SubscriptionPlan → Subscription → Donation → PointLedger (상태머신 메서드 포함)
[ B4] Repository 4종 (CRON 스캔/직전잔액 쿼리 메서드 포함)
[ B5] DTO: 요청 record(검증) + 응답 클래스 (Plan/Subscription/Donation/Calendar/Once)
[ B6] Mapper 인터페이스 + XML: SubscriptionMapper, DonationMapper (검색/조인/캘린더 집계)
[ B7] Service: SubscriptionPlanService, SubscriptionService, DonationService
[ B8] BillingService(@Transactional 건별) → BillingScheduler(@Scheduled + 수동 트리거)
[ B9] Controller 3종 (@PreAuthorize 클래스 가드, 플랜 DELETE만 SYSTEM)
[B10] DataInitializer 시드 3메서드 + run() 순서 + 생성자 주입
[B11] ./mvnw test 통과 + 앱 기동 → /v3/api-docs 에 신규 스펙 노출 확인
--- 백엔드 배포/기동 확인 후에만 ↓ (프로젝트 규칙: npm run gen 전 배포 확인 필수) ---
[F1] npm run gen → apis/query/{subscription-plan,subscription,donation} + schemas 갱신
[F2] Badge: SubscriptionStatusBadge, DonationTypeBadge (StatusBadge 복제)
[F3] Grid: SubscriptionGrid, DonationGrid (AG Grid v33 Quartz)
[F4] PlanForm (RHF+Zod) → subscription-plan/page.tsx
[F5] subscription/page.tsx + subscription/[id]/page.tsx
[F6] donation/page.tsx (필터 + 단건 후원 모달)
[F7] BillingCalendar.tsx → billing-calendar/page.tsx
[F8] Sidebar.tsx NAV_ITEMS 4개 추가
[F9] npm run build:no-lint + lint 0 경고 확인
```

---

## (7) 위험 / 주의

1. **`@EnableScheduling` 미존재**: 현재 코드베이스에 스케줄링이 전혀 없다. B1을 빠뜨리면 `@Scheduled`가 조용히 무시되어 CRON이 안 돈다 — 최우선 확인.
2. **CRON 청구 멱등성**: `chargeOneCycle`은 `advanceCycle`로 `next_billing_at`을 전진시켜 같은 건 재청구를 막는다. 단, 트랜잭션 커밋 전 스케줄러 재실행이 겹치면 중복 가능 → 데모는 `cron` 간격(5분)이 트랜잭션보다 충분히 길어 안전. `findByStatusAndNextBillingAtBefore`에 `status, next_billing_at` **복합 인덱스** 필수.
3. **포인트 원장 동시성**: `balanceAfter`는 직전 잔액 읽기-증가-쓰기라 경쟁 위험. 단일 스케줄러 스레드 + 건별 `@Transactional` + 단건 후원도 같은 트랜잭션 직렬화로 데모 범위에선 충분. 운영급은 회원 락/원자 증분이 필요하나 **본 데모 비범위**.
4. **상태 전이 검증은 엔티티에서**: `pause/resume/cancel`이 `IllegalStateException`을 던지고 서비스가 `ApiException(INVALID_PARAMETER)`로 변환. 컨트롤러/프론트에서 임의 상태 점프(예: CANCELED→ACTIVE)는 거부됨 — 프론트 버튼도 현재 상태에 따라 비활성 처리.
5. **테스트 모드 라벨 필수**: 모든 `donation.testMode="Y"`, 빌링키 마스킹. 프론트에 "테스트 모드" 배지 노출(설계 §9, §11 결제 오인 방지).
6. **church 스코핑 단순화**: member/content와 달리 본 도메인은 단일 데모 조직 가정으로 church 필터를 생략한다. 추후 멀티 조직 확장 시 `member_id → church_id` 조인 스코핑 추가 필요(현재는 명시적 비범위).
7. **`npm run gen` 게이트**: 백엔드가 실제 기동/배포되어 `/v3/api-docs`에 신규 엔드포인트가 떠야만 Orval 실행(프로젝트 CLAUDE.md 대량 변경 안전 규칙). 미배포 상태 실행 금지.
8. **5개 이상 파일 변경 보고**: 본 도메인은 다수 파일을 신규 생성하므로, 구현 착수 전 위 체크리스트를 사용자에게 보고하고 승인받는다(프로젝트 규칙).
