# 시드 생성기 구현 스펙 (P0 — 데이터가 먼저 살아야 함)

> 출처: `streamhub-portfolio-admin-design.md` 7장(시드 데이터 전략) + 6장(ERD) / `gnuboard5-admin-feature-spec.md` 4·6장(영카트 주문·정기결제).
> 대상 코드베이스: `streamhub-admin/streamhub-api`(Spring Boot 3.4.1 / Java 21), `streamhub-admin/streamhub-web`(Next.js 14).
> 본 스펙은 실제 코드 컨벤션(`DataInitializer`, `Content` 엔티티, `StatMapper.xml`, `ContentController/Service`, `orval.config.js`, `Sidebar.tsx`)에 100% 맞춘다.

---

## 1. 목적 / 범위

P0의 핵심 산출물은 **"실제 6개월 운영처럼 보이는 결정론적 시드 생성기"**다. 기존 `DataInitializer`(admin/조직/회원60/콘텐츠24/시청800/감사로그/게시글)를 깨지 않고 그 **뒤에 이어 붙여**, 굿즈(`goods_item` 60+종, 실 이미지·KRW 가격·파레토 판매·일부 품절), 주문(`order`/`order_item`/`order_receipt` 통합), 멤버십 플랜·정기후원 구독(`subscription_plan`/`subscription`), 후원·포인트 원장(`donation`/`point_ledger`)을 생성한다. 주문+후원 합산 3,000건 이상을 **최근 6개월 우상향 + 주말/특별헌금 스파이크 + 상태분포(70 DONE / 15 SHIPPING·READY / 10 PAID / 5 CANCEL·RETURN) + 구독 라이프사이클(ACTIVE/PAUSED/CANCELED 회차 제각각)**로 분포시킨다. 모든 난수는 **고정 seed `RandomGenerator`(또는 `new Random(20260617L)`)** 로 재현 가능하며, 각 seed 메서드는 기존 패턴대로 `if (repo.count() > 0) return;` 멱등 가드를 갖는다. PII는 `faker` 없이 기존 `SURNAMES`/`GIVEN_NAMES` 배열 + 마스킹(`010-****-1234`, `김O준`)으로 충당한다(신규 의존성 추가 회피). 미디어/이미지 URL은 **검증된 호스트만** 사용한다(아래 §7.3). 화면(굿즈/주문 관리)은 P1에서 구현하므로, 본 스펙의 프론트(§4)는 시드 결과를 즉시 검증할 수 있는 **최소 조회 화면**까지만 정의한다.

---

## 2. 데이터 모델

> 컨벤션(실측): 테이블명 **대문자 스네이크**, 컬럼 **소문자 스네이크 + `@Column(name=...)`**, enum은 `@Enumerated(EnumType.STRING)` + `length`, PK `@GeneratedValue(IDENTITY)`, `@NoArgsConstructor(access=PROTECTED)` + `@Builder private 생성자`, `createdAt/updatedAt`는 엔티티가 직접 보유(BaseEntity 없음), 인덱스명 `idx_{테이블}_{용도}`. FK는 **연관 매핑 없이 `Long ...Id` 컬럼**으로만 보유(기존 `Content.channelId` 방식). 금액은 `Long`(KRW, 원 단위).

### 2.1 `GOODS_ITEM` — 굿즈 (패키지 `v1.goods.entity.GoodsItem`)
| 필드 | 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|---|
| id | id | Long | PK, IDENTITY | |
| categoryId | category_id | Long | NOT NULL | `GOODS_CATEGORY.id` (FK 컬럼) |
| name | name | String | NOT NULL, len 200 | |
| price | price | Long | NOT NULL | 판매가 KRW |
| listPrice | list_price | Long | NOT NULL | 시중가(할인 표기용, ≥ price) |
| stock | stock | Integer | NOT NULL | 재고 |
| notiQty | noti_qty | Integer | NOT NULL | 통보수량(재고경고 기준) |
| soldOut | sold_out | String(1) | NOT NULL | `Y`/`N` |
| status | status | enum `GoodsStatus`(SELLING/HIDDEN) | NOT NULL, len 10 | |
| saleCount | sale_count | Long | NOT NULL | 누적 판매수(파레토) |
| viewCount | view_count | Long | NOT NULL | |
| thumbUrl | thumb_url | String | len 500 | 외부 실 이미지 URL(§7.3) |
| badges | badges | String | len 50 | `HIT,NEW,SALE` CSV |
| createdAt | created_at | LocalDateTime | NOT NULL | |
| updatedAt | updated_at | LocalDateTime | NOT NULL | |

인덱스: `idx_goods_category(category_id)`, `idx_goods_status_created(status, created_at)`, `idx_goods_soldout(sold_out)`.

### 2.2 `GOODS_CATEGORY` — 굿즈 카테고리 (`v1.goods.entity.GoodsCategory`)
| 필드 | 컬럼 | 타입 | 제약 |
|---|---|---|---|
| id | id | Long | PK |
| name | name | String | NOT NULL, len 100 |
| sort | sort | Integer | NOT NULL |

시드 카테고리(4종): `음반`, `도서`, `의류`, `소품`.

### 2.3 `MEMBERSHIP_PLAN` — 멤버십/정기후원 플랜 (`v1.subscription.entity.MembershipPlan`)
| 필드 | 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|---|
| id | id | Long | PK | |
| name | name | String | NOT NULL, len 50 | 브론즈/실버/골드/후원천사 |
| price | price | Long | NOT NULL | 월 KRW |
| grade | grade | enum `MemberGrade`(BRONZE/SILVER/GOLD/ANGEL) | NOT NULL, len 10 | |
| active | active | String(1) | NOT NULL | `Y`/`N` |

### 2.4 `SUBSCRIPTION` — 정기후원 구독 (`v1.subscription.entity.Subscription`)
| 필드 | 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|---|
| id | id | Long | PK | |
| memberId | member_id | Long | NOT NULL | `MEMBER.id` |
| planId | plan_id | Long | NOT NULL | `MEMBERSHIP_PLAN.id` |
| billingKeyMasked | billing_key_masked | String | len 40 | `bill_****1234` |
| status | status | enum `SubscriptionStatus`(ACTIVE/PAUSED/CANCELED) | NOT NULL, len 10 | |
| cycleNo | cycle_no | Integer | NOT NULL | 누적 청구 회차 |
| nextBillingAt | next_billing_at | LocalDateTime | NULL 허용 | CANCELED면 null |
| startedAt | started_at | LocalDateTime | NOT NULL | |

인덱스: `idx_subscription_member(member_id)`, `idx_subscription_status_next(status, next_billing_at)`.

### 2.5 `DONATION` — 후원/정기청구 내역 (`v1.subscription.entity.Donation`)
| 필드 | 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|---|
| id | id | Long | PK | |
| memberId | member_id | Long | NOT NULL | |
| amount | amount | Long | NOT NULL | |
| type | type | enum `DonationType`(ONCE/SUBSCRIPTION) | NOT NULL, len 12 | |
| subscriptionId | subscription_id | Long | NULL | SUBSCRIPTION이면 채움 |
| paidAt | paid_at | LocalDateTime | NOT NULL | 시계열 분포 키 |

인덱스: `idx_donation_member(member_id)`, `idx_donation_paid(paid_at)`, `idx_donation_type(type)`.

### 2.6 `POINT_LEDGER` — 은혜 포인트 원장 (`v1.subscription.entity.PointLedger`)
| 필드 | 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|---|
| id | id | Long | PK | |
| memberId | member_id | Long | NOT NULL | |
| delta | delta | Long | NOT NULL | 증감(음수 가능) |
| balanceAfter | balance_after | Long | NOT NULL | 적립 후 잔액 |
| reason | reason | String | NOT NULL, len 100 | `후원 적립`/`주문 사용` 등 |
| at | created_at | LocalDateTime | NOT NULL | |

인덱스: `idx_point_member(member_id)`, `idx_point_at(created_at)`.

### 2.7 `ORDERS` — 굿즈 주문 (`v1.order.entity.OrderEntity`, 테이블 `ORDERS`)
> ⚠️ `ORDER`는 MySQL 예약어 → 테이블명 **`ORDERS`**, 클래스명 `OrderEntity`.

| 필드 | 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|---|
| id | id | Long | PK | |
| orderNo | order_no | String | NOT NULL, unique, len 20 | `20260617-000123` |
| memberId | member_id | Long | NOT NULL | |
| status | status | enum `OrderStatus`(PLACED/PAID/READY/SHIPPING/DONE/CANCEL/RETURN) | NOT NULL, len 12 | |
| totalAmount | total_amount | Long | NOT NULL | 상품합 + 배송비 − 쿠폰 − 포인트 |
| shipFee | ship_fee | Long | NOT NULL | 기본 3000, 50000 이상 0 |
| couponDiscount | coupon_discount | Long | NOT NULL | |
| pointUsed | point_used | Long | NOT NULL | |
| payMethod | pay_method | enum `PayMethod`(CARD/BANK) | NOT NULL, len 10 | |
| trackingNo | tracking_no | String | len 30 | SHIPPING/DONE만 |
| orderedAt | ordered_at | LocalDateTime | NOT NULL | 시계열 분포 키 |

인덱스: `idx_orders_member(member_id)`, `idx_orders_status_ordered(status, ordered_at)`, `idx_orders_ordered(ordered_at)`.

### 2.8 `ORDER_ITEM` — 주문 라인 (`v1.order.entity.OrderItem`)
| 필드 | 컬럼 | 타입 | 제약 |
|---|---|---|---|
| id | id | Long | PK |
| orderId | order_id | Long | NOT NULL |
| goodsId | goods_id | Long | NOT NULL |
| qty | qty | Integer | NOT NULL |
| price | price | Long | NOT NULL (주문 시점 단가 스냅샷) |

인덱스: `idx_orderitem_order(order_id)`, `idx_orderitem_goods(goods_id)`.

### 2.9 `ORDER_RECEIPT` — 결제/환불 영수 (`v1.order.entity.OrderReceipt`)
| 필드 | 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|---|
| id | id | Long | PK | |
| orderId | order_id | Long | NOT NULL | |
| kind | kind | enum `ReceiptKind`(PAY/REFUND) | NOT NULL, len 8 | |
| amount | amount | Long | NOT NULL | |
| method | method | String | NOT NULL, len 10 | CARD/BANK |
| at | created_at | LocalDateTime | NOT NULL | |

인덱스: `idx_receipt_order(order_id)`.

---

## 3. 백엔드

### 3.1 생성할 파일 목록 (정확한 패키지 경로)

```
streamhub-api/src/main/java/org/streamhub/api/v1/
├── goods/
│   ├── GoodsController.java                 # /v1/goods (목록/상세)
│   ├── GoodsService.java
│   ├── entity/GoodsItem.java
│   ├── entity/GoodsCategory.java
│   ├── entity/GoodsStatus.java              # enum SELLING/HIDDEN
│   ├── dto/GoodsListItem.java               # @Getter @Setter @NoArgsConstructor (조인 carrier)
│   ├── dto/GoodsDetail.java
│   ├── dto/GoodsSearchRequest.java          # record + pageNumberOrDefault/pageSizeOrDefault/offset
│   ├── mapper/GoodsMapper.java              # @Mapper selectList/countList/selectDetail
│   ├── repository/GoodsItemRepository.java
│   └── repository/GoodsCategoryRepository.java
├── order/
│   ├── OrderController.java                  # /v1/order (목록/상세/상태전이)
│   ├── OrderService.java
│   ├── entity/OrderEntity.java              # @Table(name="ORDERS")
│   ├── entity/OrderItem.java
│   ├── entity/OrderReceipt.java
│   ├── entity/OrderStatus.java              # enum 7-state
│   ├── entity/PayMethod.java
│   ├── entity/ReceiptKind.java
│   ├── dto/OrderListItem.java
│   ├── dto/OrderDetail.java                 # 라인+영수 포함
│   ├── dto/OrderSearchRequest.java          # keyword/status/payMethod/from/to + 페이징
│   ├── dto/OrderStatusChangeRequest.java    # record(@NotNull OrderStatus status)
│   ├── mapper/OrderMapper.java
│   ├── repository/OrderRepository.java
│   ├── repository/OrderItemRepository.java
│   └── repository/OrderReceiptRepository.java
└── subscription/
    ├── SubscriptionController.java           # /v1/subscription (목록), /v1/donation, /v1/point
    ├── SubscriptionService.java
    ├── entity/MembershipPlan.java
    ├── entity/MemberGrade.java               # enum BRONZE/SILVER/GOLD/ANGEL
    ├── entity/Subscription.java
    ├── entity/SubscriptionStatus.java
    ├── entity/Donation.java
    ├── entity/DonationType.java
    ├── entity/PointLedger.java
    ├── dto/SubscriptionListItem.java
    ├── dto/DonationListItem.java
    ├── dto/PointLedgerItem.java
    ├── dto/SubscriptionSearchRequest.java
    ├── mapper/SubscriptionMapper.java
    ├── repository/MembershipPlanRepository.java
    ├── repository/SubscriptionRepository.java
    ├── repository/DonationRepository.java
    └── repository/PointLedgerRepository.java
```

**MyBatis XML**(`streamhub-api/src/main/resources/mappers/`): `GoodsMapper.xml`, `OrderMapper.xml`, `SubscriptionMapper.xml`. 네임스페이스 = 매퍼 인터페이스 FQN. `<sql id="searchWhere">` + `selectList`(LIMIT `#{offset},#{size}`) + `countList`. 집계는 §3.4 참조.

**시드 생성기**: 기존 `base/config/DataInitializer.java`를 그대로 확장(신규 클래스 분리 대신 `run()` 끝에 메서드 추가 — 기존 컨벤션 유지). 비대화 방지를 위해 **시드 로직만 별도 컴포넌트** `base/config/PortfolioSeeder.java`(`CommandLineRunner`, `@Order(2)` — 기존 `DataInitializer`는 `@Order(1)`로 지정)로 분리하는 것을 권장. 두 방식 중 §6 체크리스트는 **`PortfolioSeeder` 분리** 방식을 기준으로 한다(600줄 룰 회피·기존 파일 보호).

### 3.2 API 엔드포인트 표

> 모든 컨트롤러: `@RestController` + `@RequestMapping("/v1/{domain}")` + 클래스 레벨 `@PreAuthorize("hasAnyAuthority(T(...AuthoritiesConstants).SYSTEM, T(...AuthoritiesConstants).CHURCH_MANAGER)")`. 응답은 `ResultDTO<T>`. 목록은 `POST /list` + `@RequestBody SearchRequest`(콘텐츠 패턴 동일). 권한 스코핑은 본 포트폴리오에서 단일 조직이므로 `AdminPrincipal` 전달만 하고 굿즈/주문/후원은 전체 조회 허용(콘텐츠와 동일하게 principal 미사용 가능).

| 메서드 | 경로 | 요청 DTO | 응답 DTO | 권한 |
|---|---|---|---|---|
| POST | `/v1/goods/list` | `GoodsSearchRequest` | `ResInfinityList<GoodsListItem>` | SYSTEM/MANAGER |
| GET | `/v1/goods/{id}` | — | `GoodsDetail` | SYSTEM/MANAGER |
| GET | `/v1/goods/categories` | — | `List<GoodsCategory>` | SYSTEM/MANAGER |
| POST | `/v1/order/list` | `OrderSearchRequest` | `ResInfinityList<OrderListItem>` | SYSTEM/MANAGER |
| GET | `/v1/order/{id}` | — | `OrderDetail` | SYSTEM/MANAGER |
| PUT | `/v1/order/{id}/status` | `OrderStatusChangeRequest` | `OrderDetail` | SYSTEM/MANAGER |
| POST | `/v1/subscription/list` | `SubscriptionSearchRequest` | `ResInfinityList<SubscriptionListItem>` | SYSTEM/MANAGER |
| POST | `/v1/donation/list` | `SubscriptionSearchRequest` | `ResInfinityList<DonationListItem>` | SYSTEM/MANAGER |
| POST | `/v1/point/list` | `SubscriptionSearchRequest` | `ResInfinityList<PointLedgerItem>` | SYSTEM/MANAGER |

> P0 범위에서 **CRUD 쓰기는 주문 상태전이 1개만** 실제 동작으로 구현(상태머신 어필). 굿즈/구독/후원/포인트는 시드 데이터 **조회 전용**으로 충분. 굿즈 등록·수정은 P1로 미룬다.

### 3.3 주문 상태전이 핵심 로직 (의사코드)

기존 `ContentService.update` 패턴(JPA 변경감지 → `saveAndFlush` → `getDetail` 재조회) + `ActionLogPublisher.publish` 동일.

```
@Transactional
OrderDetail changeStatus(Long id, OrderStatus next, AdminPrincipal principal):
    OrderEntity order = orderRepository.findById(id) ?? throw ApiException(NOT_FOUND)
    if !ALLOWED_TRANSITIONS.get(order.status).contains(next):
        throw ApiException(INVALID_PARAMETER, "허용되지 않은 상태 전이")
    // 환불/취소면 재고 복구 + 환불 영수 + 포인트 환원
    if next in (CANCEL, RETURN):
        for line in orderItemRepository.findByOrderId(id):
            goodsItemRepository.findById(line.goodsId).ifPresent(g -> g.restock(line.qty))   // saleCount/stock 복구
        orderReceiptRepository.save(OrderReceipt.builder()
            .orderId(id).kind(REFUND).amount(order.totalAmount).method(order.payMethod.name()).build())
        if order.pointUsed > 0:
            pointLedgerRepository.save(PointLedger 적립 환원 행)   // delta=+pointUsed
    order.changeStatus(next)                                       // 엔티티 비즈니스 메서드 + updatedAt 갱신
    orderRepository.saveAndFlush(order)
    actionLogPublisher.publish("ORDER_STATUS", "ORDER", id, next.name())
    return getDetail(id)

ALLOWED_TRANSITIONS (Map<OrderStatus, Set<OrderStatus>>):
    PLACED   -> {PAID, CANCEL}
    PAID     -> {READY, CANCEL, RETURN}
    READY    -> {SHIPPING, CANCEL}
    SHIPPING -> {DONE, RETURN}
    DONE     -> {RETURN}
    CANCEL   -> {}        // 종료
    RETURN   -> {}        // 종료
```

`OrderEntity.changeStatus(next)`는 `this.status=next; this.updatedAt=now()`만 수행(전이 검증은 서비스 책임). `OrderDetail`은 `@Getter @Setter @NoArgsConstructor` carrier에 `List<OrderItemLine> items`, `List<OrderReceiptLine> receipts`를 세팅(콘텐츠 `setFiles/setHashtags` 패턴).

### 3.4 대시보드 집계 (StatMapper 확장 — P2 선행 준비)

`StatMapper.java` + `StatMapper.xml`에 메서드 추가(기존 `summary/memberTrend/topContents/watchByChannel` 옆). `StatService`는 `@Cacheable`(60s, 기존 `CacheConfig`)로 래핑.

```sql
<!-- 후원·매출 일별 추이: 주문 매출 + 후원 합산 -->
<select id="revenueTrend" resultType="...statistics.dto.TrendPoint">
  SELECT DATE_FORMAT(d, '%Y-%m-%d') AS date, SUM(amt) AS count FROM (
    SELECT ordered_at AS d, total_amount AS amt FROM ORDERS
      WHERE status NOT IN ('CANCEL','RETURN') AND ordered_at BETWEEN #{from} AND #{to}
    UNION ALL
    SELECT paid_at AS d, amount AS amt FROM DONATION WHERE paid_at BETWEEN #{from} AND #{to}
  ) t GROUP BY DATE_FORMAT(d, '%Y-%m-%d') ORDER BY date
</select>

<!-- 인기 굿즈 Top5 -->
<select id="topGoods" resultType="...statistics.dto.TopGoodsItem">
  SELECT id, name, sale_count AS sale_count, thumb_url AS thumb_url
  FROM GOODS_ITEM ORDER BY sale_count DESC, id ASC LIMIT #{limit}
</select>

<!-- 멤버십 플랜 분포(도넛) -->
<select id="planDistribution" resultType="...statistics.dto.PlanShareItem">
  SELECT p.name AS plan_name, COUNT(s.id) AS cnt
  FROM SUBSCRIPTION s JOIN MEMBERSHIP_PLAN p ON s.plan_id = p.id
  WHERE s.status = 'ACTIVE' GROUP BY p.id, p.name ORDER BY cnt DESC
</select>
```
> P0에서는 XML/매퍼 메서드까지 작성하고, KPI 카드 확장(`SummaryResponse`에 `totalRevenue`/`activeSubscriptions`/`pendingOrders`/`stockWarnings`)은 P2에서 결선한다. `map-underscore-to-camel-case: true`로 alias 자동 매핑.

---

## 4. 프론트 (검증용 최소 화면)

> 실제 굿즈/주문 관리 UI는 P1. P0에서는 **시드가 살아있음을 즉시 확인**하는 조회 화면만 만든다. 기존 컨벤션: `(protected)` 라우트 그룹, AG Grid Community 동적 import(`ssr:false`), `useQuery + placeholderData: keepPreviousData`(콘텐츠 목록 방식), `MemberSearchRequest`식 1-based UI → 0-based API 변환.

### 4.1 생성할 파일 목록
```
streamhub-web/src/
├── app/(protected)/goods/page.tsx          # 굿즈 목록(검색 키워드 + 카테고리/상태 필터 + AG Grid)
├── app/(protected)/order/page.tsx          # 주문 목록(상태/결제수단/기간 필터 + AG Grid + 상세 링크)
├── app/(protected)/order/[id]/page.tsx     # 주문 상세(라인·영수 + 상태 전이 버튼: PUT /v1/order/{id}/status)
├── app/(protected)/subscription/page.tsx   # 정기후원/후원/포인트 탭 조회(HTML 테이블, action-log 패턴)
├── components/goods/GoodsGrid.tsx          # ColDef<GoodsListItem>[]
├── components/order/OrderGrid.tsx          # ColDef<OrderListItem>[]
└── apis/query/{goods,order,subscription}/* # ⚠️ Orval 자동생성 — 백엔드 배포 후 npm run gen
```
> `apis/query/*`와 `streamHubAdminAPI.schemas.ts`는 **직접 작성 금지**. 백엔드가 `/v3/api-docs`에 뜬 뒤 `npm run gen`(orval, `tags-split`, `react-query`, mutator=`custom-instance.ts`)으로만 생성(프로젝트 안전규칙). 백엔드 배포 확인 전 `npm run gen` 금지.

### 4.2 화면별 개요
- **굿즈 목록** (`GoodsGrid`): ColDef — `thumbUrl`(이미지 `cellRenderer`), `name`(flex 1.4), `categoryName`(조인), `price`(KRW `valueFormatter` `toLocaleString`), `stock`, `soldOut`(배지), `saleCount`. `useQuery(["goods-list", req])` + `keepPreviousData`. 검색 draft/committed 분리(회원 목록 패턴).
- **주문 목록** (`OrderGrid`): ColDef — `orderNo`, `memberName`(조인), `totalAmount`(KRW), `status`(StatusBadge `cellRenderer`: DONE=emerald / SHIPPING·READY=blue / PLACED·PAID=amber / CANCEL·RETURN=red), `payMethod`, `orderedAt`(`formatDateTime`), 상세 버튼 → `router.push('/order/${id}')`. 필터: 상태 select(전체+7상태), 결제수단, 기간(`fr/to`).
- **주문 상세**: `useDetail(id, { query: { enabled: Number.isFinite(id) } })`. 상품 라인 테이블 + 영수(PAY/REFUND) 테이블 + **상태 전이 버튼군**(허용 전이만 활성, `ALLOWED_TRANSITIONS` 프론트 미러). 전이 = `useMutation`(PUT) → `onSuccess: resultCode === SUCCESS_CODE` 확인 후 `detailQuery.refetch()` (회원 수정 패턴).
- **후원/구독/포인트**: HTML 테이블 3탭(AG Grid 불필요). 상대시간(`format.ts`) + KRW. 페이지네이션은 이전/다음 버튼.

> P0 폼(RHF+Zod)은 주문 상태전이의 단일 select 하나뿐 — 본격 `createSchema`(굿즈 등록)는 P1. RHF+Zod 규약은 콘텐츠 등록 폼(`content/add/page.tsx`) 재사용.

### 4.3 사이드바 메뉴 추가
`streamhub-web/src/components/layout/Sidebar.tsx`의 `NAV_ITEMS`에 추가(import: `lucide-react`의 `ShoppingBag`, `Package`, `HeartHandshake`):
```ts
{ label: "굿즈관리", href: "/goods", icon: Package },
{ label: "주문관리", href: "/order", icon: ShoppingBag },
{ label: "후원·구독", href: "/subscription", icon: HeartHandshake },
```
(콘텐츠관리 다음, 감사 로그 앞. `systemOnly` 불필요 — 단일 조직.)

---

## 5. 시드 영향 (이 도메인이 필요로 하는 시드)

`PortfolioSeeder.run()` 호출 순서(FK 의존순) — 각 단계 `if (repo.count() > 0) return;` 멱등, `Random RND = new Random(20260617L)` 고정 seed, 기준일 `LocalDateTime now = LocalDateTime.now()`:

1. **seedGoodsCategories()** — 음반/도서/의류/소품 4행.
2. **seedGoods()** — **64종**(카테고리당 16). `price` KRW 8,000~45,000(1,000원 반올림), `listPrice = price * (1.0~1.4)` 반올림. `stock`: 대부분 양수, **인덱스 `i%24==0`·`i%37==0`은 stock=0·soldOut=Y**(품절 2종 이상 보장), `notiQty=5`이고 `i%11==0`은 `stock<notiQty`(재고경고). `saleCount`는 **파레토**: 상위 12종은 80~400, 나머지는 0~15(롱테일 + 판매0 다수). `thumbUrl`은 §7.3 굿즈 풀에서 `i % POOL.length`. `badges`: saleCount 상위=`HIT`, 최근7일 생성=`NEW`, listPrice>price=`SALE`. `createdAt = now.minusDays(i * 2)`(우상향 등록 추세). status 90% SELLING.
3. **seedMembershipPlans()** — 4행: 브론즈 5,000 / 실버 15,000 / 골드 30,000 / 후원천사 50,000.
4. **seedSubscriptions()** — 회원의 ~40%(약 24명)에 구독 부여. status 분포: **65% ACTIVE / 20% PAUSED / 15% CANCELED**. `cycleNo = 1~7`(`RND`). `startedAt = now.minusMonths(cycleNo).minusDays(RND%28)`. ACTIVE/PAUSED는 `nextBillingAt = startedAt.plusMonths(cycleNo+1)`(미래), CANCELED는 null. `billingKeyMasked = "bill_****" + format(%04d)`.
5. **seedDonations()** — **1,400건**. 60%는 SUBSCRIPTION(구독 회차 시뮬: 각 구독의 `cycleNo`만큼 월별 행, `subscriptionId` 채움, amount=plan.price), 40%는 ONCE(amount KRW 5,000~100,000). `paidAt`은 §7.2 시계열 분포 함수 적용. ONCE 후원마다 `point_ledger` 적립(delta=amount*0.01).
6. **seedOrders()** — **1,700건**(주문+후원 합산 3,000+ 충족). 각 주문 1~3개 `order_item`(굿즈 `saleCount` 가중 선택 — 인기 굿즈가 더 자주). `orderedAt`은 §7.2 분포. **status 분포 70/15/10/5** 규칙(§7.2). `total = Σ(price*qty) + shipFee − coupon − point`. shipFee: 상품합 50,000↑ 0, 아니면 3,000. DONE/SHIPPING은 `trackingNo` 채움. 각 주문에 `order_receipt`(PAY 1행, CANCEL/RETURN이면 REFUND 추가). `orderNo = yyyyMMdd(orderedAt) + "-" + %06d(seq)`.
7. **seedPointLedgerFromOrders()** — 일부 주문의 `pointUsed`(0~잔액)와 적립(DONE 시 total*0.005)을 `point_ledger`에 `balanceAfter` 누적 계산하여 기록.

`ActionLog`: 시드 단계에서는 발행하지 않음(SQS 의존 회피) — 기존 `seedActionLogs()`로 충분. 실제 상태전이(런타임)에서만 `ORDER_STATUS` 발행.

---

## 6. 구현 순서 체크리스트 (의존 순서)

```
백엔드 — 엔티티/리포지토리 (스키마 ddl-auto=update가 자동 생성)
[ ] v1/goods/entity/{GoodsStatus, GoodsCategory, GoodsItem}.java
[ ] v1/goods/repository/{GoodsItemRepository, GoodsCategoryRepository}.java
[ ] v1/subscription/entity/{MemberGrade, SubscriptionStatus, DonationType,
        MembershipPlan, Subscription, Donation, PointLedger}.java
[ ] v1/subscription/repository/{MembershipPlan, Subscription, Donation, PointLedger}Repository.java
[ ] v1/order/entity/{OrderStatus, PayMethod, ReceiptKind, OrderEntity(@Table ORDERS), OrderItem, OrderReceipt}.java
[ ] v1/order/repository/{Order, OrderItem, OrderReceipt}Repository.java

시드 (화면 없이 먼저 — 설계문서 12장 3번 원칙)
[ ] base/config/PortfolioSeeder.java (@Component, CommandLineRunner, @Order(2))
[ ] 기존 DataInitializer에 @Order(1) 부여 (실행 순서 보장)
[ ] ./mvnw spring-boot:run → 로그로 seeded 카운트 확인 (goods 64 / subs 24 / donation 1400 / orders 1700)
[ ] 재기동 후 멱등성 확인 (count>0 스킵, 중복 생성 없음)

백엔드 — DTO/매퍼/서비스/컨트롤러 (조회 + 주문 상태전이)
[ ] goods: dto/mapper(GoodsMapper.xml)/GoodsService/GoodsController
[ ] order: dto(OrderStatusChangeRequest 포함)/mapper(OrderMapper.xml)/OrderService(상태머신)/OrderController
[ ] subscription: dto/mapper(SubscriptionMapper.xml)/SubscriptionService/SubscriptionController
[ ] StatMapper(.java/.xml)에 revenueTrend/topGoods/planDistribution 추가
[ ] ./mvnw test && /v3/api-docs 에 신규 엔드포인트 노출 확인

프론트 (백엔드 배포 확인 후에만)
[ ] (백엔드 배포 확인) → npm run gen → apis/query/{goods,order,subscription} 생성 검증
[ ] components/{goods/GoodsGrid, order/OrderGrid}.tsx
[ ] app/(protected)/{goods/page, order/page, order/[id]/page, subscription/page}.tsx
[ ] Sidebar.tsx NAV_ITEMS 3항목 추가
[ ] npm run build:no-lint 통과 + 화면에서 시드 데이터 렌더 확인

검증 (프로젝트 규칙)
[ ] 백엔드 ./mvnw clean package (fmt/test/lint 그린)
[ ] 프론트 lint/format 0 경고
```

> 5개 이상 파일 변경 작업이므로(엔티티만 20+) **단계별로 사용자 보고 후 진행**(CLAUDE.md 대량 변경 안전규칙). 엔티티 → 시드 → API → 프론트 4구간으로 끊어 커밋.

---

## 7. 위험 / 주의

### 7.1 스키마·예약어·정합성
- **`ORDER`는 MySQL 예약어** → 테이블명 반드시 `ORDERS`. (`@Table(name = "ORDERS")`)
- 금액은 전부 `Long`(원 단위). `double` 사용 금지(반올림 오차). 가격 계산은 정수 산술.
- `ddl-auto=update`라 신규 엔티티는 자동 생성되지만, **기존 테이블 변경(컬럼 추가)은 위험** — 본 스펙은 전부 신규 테이블이므로 안전. `Member`에 `grade`/`pointBalance`를 추가하고 싶다면 별도 합의(기존 엔티티 변경은 5파일 룰·정합성 영향).

### 7.2 결정론·현실성 분포 (목업 티 제거)
- 단일 `Random(20260617L)`을 **메서드 간 공유하면 순서 의존성**이 생긴다. 재현성 위해 시드 메서드별 독립 `Random(고정상수)` 권장(예: goods=1001, orders=1002...). 어느 쪽이든 `run()` 호출 순서가 고정이면 재현 가능.
- **시계열 분포 함수**(주문/후원 공통):
  - 기준: 최근 **180일**. `daysAgo`를 균등이 아니라 **우상향 가중**(`180 - sqrt(RND.nextDouble())*180` → 최근일수록 밀도↑).
  - **주말 스파이크**: 생성된 날짜가 토/일이면 추가 행 1.5배 확률로 더 찍음.
  - **특별헌금일 스파이크**: 매월 둘째 주일·말일 등 고정 날짜 셋에 후원 급증(해당 날짜 ±1일 가중 3배).
  - 시각: `withHour(9~22 RND).withMinute(RND%60)`.
- **상태 분포 70/15/10/5**: `int r = RND.nextInt(100); status = r<70?DONE : r<85?(coin?SHIPPING:READY) : r<95?PAID : (coin?CANCEL:RETURN)`. 단, **최근 3일 주문은 DONE 금지**(배송 리드타임 현실성 → PLACED/PAID/READY로).
- **파레토**: 굿즈 `saleCount` 상위 소수에 집중, `order_item` 굿즈 선택을 `saleCount` 가중 → 인기 굿즈가 주문에도 더 자주.

### 7.3 미디어/이미지 URL — 검증된 호스트만 (⚠️핵심)
- **금지**: Google `gtv-videos-bucket`(현재 **403**). 기존 `DataInitializer` 주석에도 명시됨.
- **콘텐츠 영상/음원**(기존 사용·정상): `media.w3.org`, `test-videos.co.uk`, `download.blender.org`, `www.soundhelix.com`. 신규 콘텐츠 추가 시 이 풀 재사용.
- **굿즈 썸네일**(신규): 핫링크 허용·라이선스 안전 호스트만.
  - **Unsplash Source/이미지 CDN**: `https://images.unsplash.com/photo-{id}?w=400&q=80` 형태의 **고정 photo id** URL(랜덤 `source.unsplash.com`은 리다이렉트·불안정 → 회피, 직접 photo id 명시).
  - **Picsum**: `https://picsum.photos/seed/{seed}/400/400`(결정론적 seed 지원, 안정적) — 음반/도서/굿즈 플레이스홀더로 적합.
  - 권장: **Picsum `seed` URL을 기본 풀**로(완전 결정론적·항상 200), 일부를 큐레이션된 Unsplash 고정 photo id로 섞어 현실감↑.
  - 실제 브랜드 로고/상표 들어간 이미지 회피(설계문서 11장 가드). 푸터에 "데모 데이터" 명시(별도 P6).
- 운영 배포 시 외부 핫링크가 막히면 굿즈 썸네일이 깨질 수 있음 → 장기적으로는 `StorageService`(MinIO/S3)로 사전 업로드하는 마이그레이션 고려(P1+). P0는 외부 URL 직참조로 충분.

### 7.4 시드 비용·트랜잭션
- 3,000+ 행은 `saveAll`(배치)로 저장. 건별 `save` 루프는 피한다(콘텐츠 시드처럼 즉시 id가 필요한 경우만 개별 save).
- `order` → `order_item`/`order_receipt`는 부모 id 필요 → 부모 `saveAll` 후 id 매핑하여 자식 `saveAll`. 또는 부모 개별 save 후 자식 일괄(콘텐츠-해시태그 패턴).
- 시드 전체를 단일 트랜잭션으로 묶지 말 것(대량 → 메모리/롤백 비용). 단계별 `saveAll`로 충분.

### 7.5 프론트 안전
- `npm run gen`은 **백엔드 배포 확인 후에만**(CLAUDE.md). 자동생성 디렉토리 직접 수정 금지.
- Orval 메서드 이름 충돌: 기존 `useList/useDetail/useUpdate`에 더해 신규는 `useList3`, `useDetail2` 등 **suffix 증가**로 생성될 수 있음(content가 `useList1`인 전례) → 페이지 import 시 생성된 실제 이름 확인 필수.
