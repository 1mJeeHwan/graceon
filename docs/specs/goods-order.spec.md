# 굿즈샵 + 주문 도메인 구현 스펙 (★실동작 핵심)

## 1. 목적/범위

레퍼런스 서비스 굿즈샵(상품·카테고리·옵션·재고·이미지)과 주문(주문·주문상품·입금/환불) 도메인을 영카트5 `itemform`/`orderlist`/`orderform`를 교회 굿즈로 재해석하여 **DB 연동 실동작**으로 구현한다. 포트폴리오의 핵심 어필은 **주문 상태머신**(`PLACED → PAID → READY → SHIPPING → DONE`, 분기 `CANCEL`/`RETURN`)과 상태전이 시 **재고 차감/복원·합계 재계산·입금/환불 영수증 기록**을 한 트랜잭션으로 묶는 점이다. 기존 `streamhub-api`(Spring Boot 3.4.1 / Java 21, JPA+MyBatis 혼용) · `streamhub-web`(Next.js 14, AG Grid, React Query, RHF+Zod) 컨벤션을 100% 그대로 따른다. 굿즈 이미지 업로드는 기존 `StorageService`를 재사용한다.

---

## 2. 데이터 모델

> 컨벤션: 테이블명 **대문자 SNAKE**, 컬럼 **소문자 snake**(`@Column(name=...)`), 인덱스 `idx_{테이블}_{용도}`, enum은 `@Enumerated(EnumType.STRING)`, PK `@GeneratedValue(strategy = IDENTITY)`, `created_at`/`updated_at`는 엔티티가 직접 소유(`@Builder` 생성자에서 `LocalDateTime.now()` 기본값, 변경 메서드에서 `updatedAt` 갱신). `map-underscore-to-camel-case: true`이므로 MyBatis는 `AS snake_case` alias만 쓰면 camelCase DTO로 자동 매핑.

### 2.1 GOODS_CATEGORY (굿즈 카테고리, 3단 트리)
| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| parent_id | BIGINT | nullable | 상위 카테고리(루트는 null) |
| name | VARCHAR(100) | not null | 분류명 |
| depth | INT | not null | 1~3 |
| sort | INT | not null, default 0 | 출력순서 |
| image_key | VARCHAR(300) | nullable | 카테고리 대표 이미지(스토리지 key) |
| use_yn | VARCHAR(1) | not null, length 1 | "Y"/"N" 판매가능 |
| created_at | DATETIME | not null | |
| updated_at | DATETIME | not null | |

인덱스: `@Index(name="idx_goods_category_parent", columnList="parent_id")`, `@Index(name="idx_goods_category_sort", columnList="sort")`

### 2.2 GOODS_ITEM (굿즈 상품)
| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| category_id | BIGINT | not null | FK→GOODS_CATEGORY |
| name | VARCHAR(200) | not null | 상품명 |
| code | VARCHAR(50) | not null, unique | 상품코드(`GD0001` 형태) |
| description | VARCHAR(2000) | nullable | 기본설명 |
| price | BIGINT | not null | 판매가(KRW) |
| list_price | BIGINT | nullable | 시중가 |
| stock | INT | not null, default 0 | 창고재고(옵션 없을 때 기준 재고) |
| noti_qty | INT | not null, default 0 | 재고통보수량(이하면 재고경고) |
| sold_out | VARCHAR(1) | not null | "Y"/"N" 품절 |
| use_yn | VARCHAR(1) | not null | "Y"/"N" 판매 |
| status | ENUM(GoodsStatus) | not null, length 10 | `SELLING`/`PAUSED` |
| sale_count | INT | not null, default 0 | 누적 판매수 |
| view_count | BIGINT | not null, default 0 | 조회수 |
| thumbnail_key | VARCHAR(300) | nullable | 대표 썸네일 |
| badges | VARCHAR(50) | nullable | 콤마조인 노출배지(`HIT,NEW,SALE`) |
| created_at | DATETIME | not null | |
| updated_at | DATETIME | not null | |

인덱스: `idx_goods_item_category`(category_id), `idx_goods_item_status`(status), `idx_goods_item_created`(created_at), `idx_goods_item_code`(code)

enum `GoodsStatus { SELLING, PAUSED }` — `v1/goods/entity/GoodsStatus.java`

### 2.3 GOODS_OPTION (굿즈 옵션, 사이즈·색상)
| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| item_id | BIGINT | not null | FK→GOODS_ITEM |
| name | VARCHAR(100) | not null | 옵션항목(예: "블랙 / L") |
| option_type | VARCHAR(50) | nullable | 옵션유형(예: "색상/사이즈") |
| extra_price | BIGINT | not null, default 0 | 추가금 |
| stock | INT | not null, default 0 | 옵션재고 |
| use_yn | VARCHAR(1) | not null | "Y"/"N" |
| sort | INT | not null, default 0 | |

인덱스: `idx_goods_option_item`(item_id)

### 2.4 GOODS_IMAGE (굿즈 추가 이미지)
| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| item_id | BIGINT | not null | FK→GOODS_ITEM |
| s3_key | VARCHAR(300) | not null | 스토리지 key (ContentFile 컨벤션과 동일 컬럼명) |
| sort | INT | not null, default 0 | |

인덱스: `idx_goods_image_item`(item_id)

### 2.5 ORDERS (주문 — `ORDER`는 SQL 예약어이므로 테이블명 `ORDERS`)
| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| order_no | VARCHAR(30) | not null, unique | 주문번호(`YYYYMMDD-XXXXXX`) |
| member_id | BIGINT | not null | FK→MEMBER |
| status | ENUM(OrderStatus) | not null, length 12 | 상태머신 |
| ordered_name | VARCHAR(50) | not null | 주문자명 |
| ordered_phone | VARCHAR(20) | nullable | 주문자 전화(마스킹 시드) |
| receiver_name | VARCHAR(50) | not null | 받는분 |
| receiver_phone | VARCHAR(20) | nullable | 받는분 전화 |
| receiver_addr | VARCHAR(300) | nullable | 배송지 |
| goods_total | BIGINT | not null | 상품합계(옵션 추가금 포함) |
| ship_fee | BIGINT | not null, default 0 | 배송비 |
| coupon_discount | BIGINT | not null, default 0 | 쿠폰할인 |
| point_used | BIGINT | not null, default 0 | 포인트 사용 |
| total | BIGINT | not null | 최종합계 = goods_total + ship_fee − coupon_discount − point_used |
| pay_method | VARCHAR(20) | not null | `BANK`/`CARD` |
| tracking_no | VARCHAR(50) | nullable | 운송장번호 |
| ship_company | VARCHAR(50) | nullable | 배송회사 |
| ordered_at | DATETIME | not null | 주문일시 |
| updated_at | DATETIME | not null | |

인덱스: `idx_orders_member`(member_id), `idx_orders_status`(status), `idx_orders_ordered_at`(ordered_at), `idx_orders_order_no`(order_no)

enum `OrderStatus { PLACED, PAID, READY, SHIPPING, DONE, CANCEL, RETURN }` — `v1/order/entity/OrderStatus.java`

### 2.6 ORDER_ITEM (주문상품, cart 스냅샷)
| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| order_id | BIGINT | not null | FK→ORDERS |
| goods_id | BIGINT | not null | FK→GOODS_ITEM(스냅샷이므로 삭제돼도 유지) |
| option_id | BIGINT | nullable | FK→GOODS_OPTION |
| goods_name | VARCHAR(200) | not null | 주문 시점 상품명 스냅샷 |
| option_name | VARCHAR(100) | nullable | 주문 시점 옵션명 스냅샷 |
| unit_price | BIGINT | not null | 단가(옵션 추가금 포함, 주문시점) |
| qty | INT | not null | 수량 |
| line_total | BIGINT | not null | unit_price × qty |

인덱스: `idx_order_item_order`(order_id), `idx_order_item_goods`(goods_id)

### 2.7 ORDER_RECEIPT (입금/환불 영수증)
| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| order_id | BIGINT | not null | FK→ORDERS |
| kind | ENUM(ReceiptKind) | not null, length 8 | `PAY`/`REFUND` |
| amount | BIGINT | not null | 금액 |
| method | VARCHAR(20) | not null | `BANK`/`CARD` |
| memo | VARCHAR(200) | nullable | 비고(예: "입금확인", "취소환불") |
| created_at | DATETIME | not null | 처리일시 |

인덱스: `idx_order_receipt_order`(order_id)

enum `ReceiptKind { PAY, REFUND }` — `v1/order/entity/ReceiptKind.java`

---

## 3. 백엔드

패키지 루트: `org.streamhub.api.v1.goods`, `org.streamhub.api.v1.order`. 컨트롤러는 `@RequestMapping("/v1/goods")`, `@RequestMapping("/v1/order")`. 클래스에 `@PreAuthorize("hasAnyAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM, T(...).CHURCH_MANAGER)")` (ContentController 패턴 그대로). 응답은 전부 `ResultDTO<T>`. MyBatis XML은 `src/main/resources/mappers/`.

### 3.1 생성 파일 목록

**goods 도메인** (`streamhub-api/src/main/java/org/streamhub/api/v1/goods/`)
- `GoodsController.java`
- `GoodsService.java`
- `GoodsCategoryService.java`
- `entity/GoodsCategory.java`, `entity/GoodsItem.java`, `entity/GoodsOption.java`, `entity/GoodsImage.java`, `entity/GoodsStatus.java`
- `repository/GoodsCategoryRepository.java`, `repository/GoodsItemRepository.java`, `repository/GoodsOptionRepository.java`, `repository/GoodsImageRepository.java`
- `dto/GoodsSearchRequest.java`, `dto/GoodsListItem.java`, `dto/GoodsDetail.java`, `dto/GoodsCreateRequest.java`, `dto/GoodsOptionDto.java`, `dto/GoodsImageDto.java`, `dto/GoodsCategoryDto.java`, `dto/GoodsBulkUpdateRequest.java`, `dto/UploadResponse.java`(또는 content의 것 재사용)
- `mapper/GoodsMapper.java`

**order 도메인** (`streamhub-api/src/main/java/org/streamhub/api/v1/order/`)
- `OrderController.java`
- `OrderService.java`
- `entity/Order.java`(`@Table(name="ORDERS")`), `entity/OrderItem.java`, `entity/OrderReceipt.java`, `entity/OrderStatus.java`, `entity/ReceiptKind.java`
- `repository/OrderRepository.java`, `repository/OrderItemRepository.java`, `repository/OrderReceiptRepository.java`
- `dto/OrderSearchRequest.java`, `dto/OrderListItem.java`, `dto/OrderDetail.java`, `dto/OrderItemDto.java`, `dto/OrderReceiptDto.java`, `dto/OrderStatusChangeRequest.java`, `dto/OrderTrackingRequest.java`
- `mapper/OrderMapper.java`

**MyBatis XML** (`streamhub-api/src/main/resources/mappers/`)
- `GoodsMapper.xml`, `OrderMapper.xml`

**시드** — `base/config/DataInitializer.java` 수정(아래 §5).

### 3.2 DTO 형태(핵심)

```java
// GoodsSearchRequest (ContentSearchRequest 패턴)
public record GoodsSearchRequest(
        Integer pageNumber, Integer pageSize, String keyword,
        Long categoryId, GoodsStatus status, String soldOut /* "Y"/"N"/null */) {
    public int pageSizeOrDefault() { return pageSize == null || pageSize <= 0 ? 10 : pageSize; }
    public int offset() { int p = pageNumber == null || pageNumber < 0 ? 0 : pageNumber; return p * pageSizeOrDefault(); }
}

// GoodsCreateRequest (옵션/이미지 동적행 포함)
public record GoodsCreateRequest(
        @NotNull Long categoryId,
        @NotBlank String name,
        @NotBlank String code,
        String description,
        @NotNull Long price, Long listPrice,
        Integer stock, Integer notiQty,
        String soldOut, String useYn,
        GoodsStatus status,
        String thumbnailKey,
        List<String> badges,
        List<GoodsOptionDto> options,   // 동적행
        List<GoodsImageDto> images) {}  // 동적행 (s3Key + sort)

// GoodsBulkUpdateRequest (AG Grid 인라인 일괄수정)
public record GoodsBulkUpdateRequest(@NotEmpty List<Row> rows) {
    public record Row(@NotNull Long id, Integer stock, Integer notiQty, Long price,
                      String soldOut, String useYn, Integer sort) {}
}

// OrderSearchRequest (상태/기간/검색필드)
public record OrderSearchRequest(
        Integer pageNumber, Integer pageSize,
        String searchField /* orderNo/orderedName/receiverName/trackingNo */, String keyword,
        OrderStatus status, String payMethod,
        LocalDate fromDate, LocalDate toDate) {
    public int pageSizeOrDefault() { return pageSize == null || pageSize <= 0 ? 10 : pageSize; }
    public int offset() { int p = pageNumber == null || pageNumber < 0 ? 0 : pageNumber; return p * pageSizeOrDefault(); }
}

// OrderStatusChangeRequest
public record OrderStatusChangeRequest(@NotNull OrderStatus status, String memo) {}
// OrderTrackingRequest
public record OrderTrackingRequest(@NotBlank String trackingNo, String shipCompany) {}
```

`GoodsListItem`/`GoodsDetail`/`OrderListItem`/`OrderDetail`는 ContentListItem/ContentDetail 패턴(`@Getter @Setter @NoArgsConstructor`, MyBatis 매핑 대상, 이미지/옵션 등 컬렉션·`thumbnailUrl`은 서비스가 채움).

### 3.3 API 엔드포인트 표

| 메서드 | 경로 | 요청 DTO | 응답 DTO | 권한 |
|---|---|---|---|---|
| POST | `/v1/goods/list` | `GoodsSearchRequest` | `ResInfinityList<GoodsListItem>` | SYSTEM·CHURCH_MANAGER |
| GET | `/v1/goods/{id}` | — | `GoodsDetail` | 〃 |
| POST | `/v1/goods` | `GoodsCreateRequest` | `GoodsDetail` | 〃 |
| PUT | `/v1/goods/{id}` | `GoodsCreateRequest` | `GoodsDetail` | 〃 |
| DELETE | `/v1/goods/{id}` | — | `Void` | 〃 |
| PUT | `/v1/goods/bulk` | `GoodsBulkUpdateRequest` | `Integer`(반영행수) | 〃 |
| POST | `/v1/goods/upload` | `MultipartFile file` | `UploadResponse` | 〃 |
| GET | `/v1/goods/categories` | — | `List<GoodsCategoryDto>`(트리) | 〃 |
| POST | `/v1/order/list` | `OrderSearchRequest` | `ResInfinityList<OrderListItem>` | 〃 |
| GET | `/v1/order/{id}` | — | `OrderDetail` | 〃 |
| PATCH | `/v1/order/{id}/status` | `OrderStatusChangeRequest` | `OrderDetail` | 〃 |
| PATCH | `/v1/order/{id}/tracking` | `OrderTrackingRequest` | `OrderDetail` | 〃 |

업로드는 `storageService.upload(file, "goods")` → `new UploadResponse(key, storageService.publicUrl(key))` (ContentController.upload 그대로). `GoodsService.getDetail`는 `item.setThumbnailUrl(storageService.publicUrl(key))`, 이미지 리스트도 동일 변환.

### 3.4 핵심 로직 의사코드

**(A) 주문 상태 전이 — `OrderService.changeStatus` (`@Transactional`)**

허용 전이맵(불법 전이는 `ApiException(INVALID_PARAMETER, "허용되지 않는 상태 전이")`):
```
PLACED   → PAID, CANCEL
PAID     → READY, CANCEL
READY    → SHIPPING, CANCEL
SHIPPING → DONE, RETURN
DONE     → RETURN
CANCEL   → (종료)
RETURN   → (종료)
```

```
changeStatus(orderId, req):
    order = orderRepository.findById(orderId) ?: throw ApiException(NOT_FOUND)
    from = order.status; to = req.status
    if to not in TRANSITIONS[from]: throw ApiException(INVALID_PARAMETER, "허용되지 않는 상태 전이")

    items = orderItemRepository.findByOrderId(orderId)

    // 재고 효과
    if to == PAID:                          // 결제확정 시 재고 차감
        for it in items: decrementStock(it)   // 옵션 있으면 option.stock--, 없으면 goodsItem.stock--; sale_count += qty
        orderReceiptRepository.save(PAY 영수증: amount=order.total, method=order.payMethod, memo=req.memo ?: "입금확인")
    if to in (CANCEL, RETURN):              // 취소·반품 시 재고 복원 + 환불
        if from in (PAID, READY, SHIPPING, DONE):  // 이미 차감된 경우만 복원
            for it in items: restoreStock(it)       // stock += qty; sale_count -= qty (음수 가드)
        orderReceiptRepository.save(REFUND 영수증: amount=order.total, method=order.payMethod, memo=req.memo ?: (to==CANCEL? "주문취소" : "반품환불"))

    order.changeStatus(to)                  // 엔티티 메서드: status 세팅 + updatedAt 갱신
    orderRepository.saveAndFlush(order)
    actionLogPublisher.publish("ORDER_" + to.name(), "ORDER", String.valueOf(orderId), order.getOrderNo())
    return getDetail(orderId)
```
- `decrementStock`/`restoreStock`는 옵션 우선: `option_id != null` → `GoodsOption.stock`, 아니면 `GoodsItem.stock`. JPA 변경감지(엔티티 메서드 `addStock(delta)`/`subtractStock(qty)`)로 처리. 재고 부족 시 `throw ApiException(INVALID_PARAMETER, "재고 부족")`.
- 운송장(`changeTracking`)은 별도 PATCH: `order.setTracking(trackingNo, shipCompany)` + `updatedAt` 갱신, 상태가 `READY`이면 자동으로 `SHIPPING` 승격(선택). ActionLog `ORDER_TRACKING`.

**(B) 합계 재계산 — `Order` 엔티티 `recalcTotal()`** (생성·아이템 변경 시)
```
goodsTotal = sum(orderItem.lineTotal)
total = goodsTotal + shipFee - couponDiscount - pointUsed
if total < 0: total = 0
```
이 스펙의 주문은 시드/조회 중심이라 생성 API는 두지 않지만(주문은 사용자 사이드에서 발생하는 것으로 간주), `recalcTotal()`은 시드 생성기와 향후 확장에서 단일 진실 원천으로 사용.

**(C) 굿즈 인라인 일괄수정 — `GoodsService.bulkUpdate` (`@Transactional`)**
```
bulkUpdate(req):
    ids = req.rows.map(Row::id)
    items = goodsItemRepository.findAllByIdIn(ids)   // MemberRepository.findAllByIdIn 패턴
    byId = items.toMap(id)
    affected = 0
    for row in req.rows:
        item = byId[row.id]; if item == null: continue
        item.applyInlineEdit(row.stock, row.notiQty, row.price, row.soldOut, row.useYn, row.sort) // null 필드는 기존값 유지
        affected++
    actionLogPublisher.publish("GOODS_BULK_UPDATE", "GOODS", String.join(",", ids), affected+"건")
    return affected   // JPA dirty checking이 flush
```

**(D) 굿즈 생성/수정 옵션·이미지 동기화 — `GoodsService.create/update`**
ContentService.applyHashtags/recordThumbnailFile 패턴: 옵션·이미지는 `deleteByItemId` 후 재삽입(replace), 삭제 시 이미지 `s3_key`는 `storageService.delete(...)` 후 행 삭제, 썸네일도 동일.

### 3.5 MyBatis 매퍼

**GoodsMapper.xml** — `<sql id="searchWhere">`에 `keyword`(name LIKE / code LIKE), `categoryId`, `status`, `soldOut` 동적필터(ContentMapper.xml 패턴). `selectList`는 `GOODS_ITEM gi JOIN GOODS_CATEGORY gc ON gi.category_id = gc.id`, 옵션수는 서브쿼리 `(SELECT COUNT(*) FROM GOODS_OPTION o WHERE o.item_id = gi.id) AS option_count`, 배지는 컬럼 그대로. `ORDER BY gi.created_at DESC, gi.id DESC LIMIT #{offset},#{size}`. `countList` 동일 where. `selectDetail`은 단건. 인터페이스 `GoodsMapper`는 `selectList(@Param... offset,size)`, `countList(...)`, `selectDetail(@Param("id"))` (ContentMapper.java 시그니처 패턴).

**OrderMapper.xml** — `<sql id="searchWhere">`에:
- `searchField`+`keyword` 조합: `<choose>`로 `orderNo`→`o.order_no LIKE`, `orderedName`→`o.ordered_name LIKE`, `receiverName`→`o.receiver_name LIKE`, `trackingNo`→`o.tracking_no LIKE`
- `status`(`o.status = #{status}`), `payMethod`, 기간 `o.ordered_at BETWEEN #{fromDate} AND #{toDate}` (각 `<if>` null 가드)

`selectList`: `ORDERS o JOIN MEMBER m ON o.member_id = m.id`, 주문상품수 `(SELECT COUNT(*) FROM ORDER_ITEM oi WHERE oi.order_id = o.id) AS item_count`, `m.name AS member_name`. `ORDER BY o.ordered_at DESC, o.id DESC LIMIT #{offset},#{size}`. `selectDetail`은 주문 단건; `OrderService`가 `orderItemRepository.findByOrderId`/`orderReceiptRepository.findByOrderId`로 `items`/`receipts` 채움(ContentService.loadFiles 패턴).

---

## 4. 프론트

라우트 그룹 `src/app/(protected)/`, AG Grid·React Query·RHF+Zod·brand(`#2563eb`)·`FIELD_CLASS`·`SUCCESS_CODE` 그대로. 목록은 `useQuery + keepPreviousData`(content 패턴), 일괄작업/상태변경은 mutation. **모든 API 훅은 `npm run gen`(Orval)으로 생성** — 백엔드 배포 후에만 실행(프로젝트 규칙). 아래 훅명은 Orval 생성 결과(operationId 기반) 가정.

### 4.1 생성 파일 목록

**굿즈** (`streamhub-web/src/`)
- `app/(protected)/goods/page.tsx` — 굿즈 목록(AG Grid 인라인 일괄수정)
- `app/(protected)/goods/add/page.tsx` — 굿즈 등록폼(옵션/이미지 동적행)
- `app/(protected)/goods/[id]/page.tsx` — 굿즈 상세/수정
- `components/goods/GoodsGrid.tsx` — AG Grid (editable 셀 + 선택)
- `components/goods/GoodsStatusBadge.tsx` — 상태/품절 배지
- `components/goods/OptionRows.tsx` — 옵션 동적행(useFieldArray)
- `components/goods/ImageRows.tsx` — 이미지 동적행(ThumbnailUpload 재사용)
- `lib/goods-form.ts` — Zod 스키마 + badges/options 파싱 헬퍼

**주문** (`streamhub-web/src/`)
- `app/(protected)/order/page.tsx` — 주문 목록(상태/기간 필터)
- `app/(protected)/order/[id]/page.tsx` — 주문 상세(상태변경·운송장)
- `components/order/OrderGrid.tsx` — AG Grid
- `components/order/OrderStatusBadge.tsx` — 7상태 색상 배지
- `components/order/OrderStatusStepper.tsx` — 상태머신 시각화 + 전이 버튼
- `lib/order-status.ts` — 상태 라벨/색상/허용전이 맵(프론트 가드)

**공통**
- `components/layout/Sidebar.tsx` 수정 — 메뉴 추가
- (Orval 생성) `apis/query/goods/goods.ts`, `apis/query/order/order.ts`, `apis/query/streamHubAdminAPI.schemas.ts` 갱신

### 4.2 화면별 상세

**굿즈 목록 `goods/page.tsx`**
- 데이터: `useQuery({ queryKey:["goods-list", searchRequest], queryFn:({signal})=>list?(searchRequest,signal), placeholderData:keepPreviousData })` (content/page.tsx 그대로).
- 필터: 검색어(상품명/코드), 카테고리 select(`useGoodsCategories`), 상태(`SELLING`/`PAUSED`/ALL), 품절(Y/N/ALL). committed vs draft 상태 분리.
- 상단우측 `<Link href="/goods/add">` 굿즈 등록(Plus 아이콘, content 패턴).
- AG Grid 인라인 일괄수정: 변경된 행을 추적해 `[일괄수정]` 버튼 클릭 시 `useGoodsBulkUpdate().mutate({ data:{ rows:dirtyRows } })`, 성공 시 `setMessage("N건 수정됨")` + `listQuery.refetch()`.

**GoodsGrid.tsx 컬럼 defs** (`ColDef<GoodsListItem>[]`, MemberGrid 패턴 — `ModuleRegistry.registerModules([AllCommunityModule])`, `themeQuartz.withParams`, `rowSelection:{mode:"multiRow"}`):
| field | headerName | 비고 |
|---|---|---|
| (썸네일) | 이미지 | cellRenderer `<img src={thumbnailUrl}>` 40px |
| code | 상품코드 | |
| name | 상품명 | flex 1.4 |
| categoryName | 분류 | |
| price | 판매가 | `editable:true`, valueFormatter `toLocaleString` |
| listPrice | 시중가 | valueFormatter |
| stock | 재고 | `editable:true` |
| notiQty | 통보수량 | `editable:true` |
| soldOut | 품절 | `editable:true`, Y/N select(cellEditor `agSelectCellEditor`) |
| useYn | 판매 | `editable:true`, Y/N |
| saleCount | 판매수 | |
| viewCount | 조회 | |
| (상세) | 상세 | cellRenderer 버튼 → `router.push('/goods/'+id)` |
- 편집 추적: `onCellValueChanged`에서 변경 행 id를 부모로 보고(`onRowEdited(id)`), 부모가 dirty set 관리. `defaultColDef:{ sortable, resizable, suppressMovable }`.

**굿즈 등록폼 `goods/add/page.tsx`** (content/add 패턴 + 동적행)
- Zod(`lib/goods-form.ts`): `name`(min1), `code`(min1), `categoryId`(refine 숫자), `price`(coerce number ≥0), `listPrice` optional, `stock`/`notiQty` optional, `soldOut`/`useYn` enum("Y","N"), `status` enum("SELLING","PAUSED"), `badges` string(쉼표), `options: z.array({name, optionType?, extraPrice, stock, useYn})`, `images: z.array({s3Key, sort})`.
- 동적행: `useFieldArray`로 옵션/이미지 행 add/remove. 이미지 행은 `ThumbnailUpload`(content 재사용) → 업로드 후 `s3Key` 필드 채움. 대표 썸네일은 별도 `ThumbnailUpload` 1개.
- 제출: payload 구성 후 `useGoodsCreate().mutate({data})`, 성공 시 `router.push('/goods/'+newId)`.
- 수정 `goods/[id]/page.tsx`: `useGoodsDetail(id,{query:{enabled:Number.isFinite(id)}})` → `reset(buildDefaults(detail))`(member/[id] 패턴), 수정모드 토글, `useGoodsUpdate().mutate`, 삭제 `useGoodsDelete()`.

**주문 목록 `order/page.tsx`**
- 데이터: `useQuery + keepPreviousData`.
- 필터: 검색필드 select(주문번호/주문자/받는분/운송장) + 검색어, 상태 select(7상태+전체), 결제수단(무통장/카드/전체), 기간 `fromDate~toDate`(`<input type="date">` 2개). committed vs draft.
- OrderGrid 컬럼: 주문번호 / 주문자(orderedName) / 받는분(receiverName) / 상품수(itemCount) / 주문합계(total, toLocaleString) / 상태(OrderStatusBadge cellRenderer) / 결제수단 / 운송장(trackingNo, "-") / 주문일시(formatDateTime) / 상세버튼. (선택 없이 read-only 그리드 + 상세이동.)

**주문 상세 `order/[id]/page.tsx`**
- `useOrderDetail(id)`. 영역: 주문자/수령자 정보(ReadonlyField, member/[id] 패턴), 주문상품 라인(items 테이블: 상품명·옵션·단가·수량·합계), 결제·환불 영수증(receipts 테이블: kind 배지·금액·방법·일시), 합계 요약(상품합/배송비/쿠폰/포인트/최종).
- **상태변경**: `OrderStatusStepper`가 현재 status 강조 + `lib/order-status.ts`의 `ALLOWED_TRANSITIONS[status]`만 버튼 노출. 버튼 클릭 → 확인 모달 → `useOrderChangeStatus().mutate({ id, data:{status, memo} })`, 성공 시 `detailQuery.refetch()` + 메시지. CANCEL/RETURN 버튼은 빨강(파괴적), 확인 모달 필수(설계문서 §6.3 데모 가드).
- **운송장**: READY/SHIPPING 상태에서 운송장 입력폼(trackingNo, shipCompany) → `useOrderUpdateTracking().mutate`.

**order-status.ts**
```ts
export const STATUS_LABEL = { PLACED:"주문", PAID:"입금", READY:"배송준비", SHIPPING:"배송중", DONE:"완료", CANCEL:"취소", RETURN:"반품" };
export const STATUS_COLOR = { PLACED:"bg-slate-100 text-slate-700", PAID:"bg-blue-100 text-blue-700", READY:"bg-amber-100 text-amber-700", SHIPPING:"bg-indigo-100 text-indigo-700", DONE:"bg-emerald-100 text-emerald-700", CANCEL:"bg-red-100 text-red-700", RETURN:"bg-red-100 text-red-700" };
export const ALLOWED_TRANSITIONS = { PLACED:["PAID","CANCEL"], PAID:["READY","CANCEL"], READY:["SHIPPING","CANCEL"], SHIPPING:["DONE","RETURN"], DONE:["RETURN"], CANCEL:[], RETURN:[] };
```
(백엔드 전이맵과 1:1 일치 — 단일 진실은 백엔드, 프론트는 UX 가드.)

### 4.3 사이드바 메뉴 추가 (`components/layout/Sidebar.tsx`)
`lucide-react`에서 `ShoppingBag`, `ClipboardList` import 추가 후 `NAV_ITEMS`에:
```ts
{ label: "굿즈관리", href: "/goods", icon: ShoppingBag },
{ label: "주문관리", href: "/order", icon: ClipboardList },
```
(콘텐츠관리 아래에 배치. systemOnly 불필요 — CHURCH_MANAGER도 운영.)

---

## 5. 시드 영향 (`base/config/DataInitializer.java`)

`run()` 호출 순서 끝에 `seedGoods()` → `seedOrders()` 추가(주문은 굿즈·회원 FK 필요). 각 메서드는 `if (goodsItemRepository.count() > 0) return;` 멱등 가드. 신규 Repository 6개를 `private final`로 주입.

- **seedGoodsCategories()**(seedGoods 내부): 1단 루트 4개(음반/도서/의류/소품), 각 2단 약간. depth/sort 결정론적.
- **seedGoods()**: 굿즈 60종(설계문서 §1.2 "굿즈 60+종"). 가격 8,000~45,000(반올림 단위), `i % 카테고리`, `code=String.format("GD%04d", i+1)`, 배지 `i%5`로 HIT/NEW/SALE 분포, **재고 현실성**: 일부 `stock <= notiQty`(재고경고), 약 2종 `soldOut="Y"`(설계문서 §7.2). 썸네일 key는 시드용 더미(`goods/thumb-XX`) — 실 이미지 없이 publicUrl만. 일부 상품에 옵션 2~3행(`GOODS_OPTION`), 이미지 1~2행. 판매0 롱테일(파레토) 위해 `sale_count`/`view_count` 편중 분포(`(i*137)%5000` 식).
- **seedOrders()**: 주문 약 200건(포트폴리오 데모 적정 — 설계문서의 3,000건은 전체 도메인 합산 목표). `memberRepository.findAll()`에서 결정론적 회원 선택(`(i*13)%size`, watchHistory 패턴), 굿즈 1~3개를 ORDER_ITEM 스냅샷으로. **상태 분포**(설계문서 §7.2): 70% DONE, 15% SHIPPING/READY, 10% PAID, 5% CANCEL/RETURN — `OrderStatus[] dist` 배열 인덱싱으로 구현. `ordered_at = now.minusDays(i % 180).minus...`(최근 6개월 우상향). DONE/SHIPPING 주문엔 `tracking_no` 채움. 상태에 맞춰 ORDER_RECEIPT(PAID 이상 PAY 영수증, CANCEL/RETURN REFUND 영수증) 생성. `total`은 `recalcTotal()` 결과로 정합성 보장. 재고는 시드에서 차감하지 않고 독립값으로 둠(데모 리셋 일관성).

---

## 6. 구현 순서 체크리스트 (의존 순서)

**백엔드**
1. [ ] `goods/entity/*` (GoodsStatus → GoodsCategory → GoodsItem → GoodsOption → GoodsImage)
2. [ ] `goods/repository/*` (4개, `findAllByIdIn`/`findByItemId`/`deleteByItemId` 선언)
3. [ ] `order/entity/*` (OrderStatus/ReceiptKind → Order(@Table ORDERS, recalcTotal/changeStatus/setTracking) → OrderItem → OrderReceipt)
4. [ ] `order/repository/*` (3개, `findByOrderId` 선언)
5. [ ] `goods/dto/*`, `order/dto/*`
6. [ ] `goods/mapper/GoodsMapper.java` + `resources/mappers/GoodsMapper.xml`
7. [ ] `order/mapper/OrderMapper.java` + `resources/mappers/OrderMapper.xml`
8. [ ] `GoodsCategoryService`, `GoodsService`(list/detail/create/update/delete/bulkUpdate/categories), `OrderService`(list/detail/changeStatus/changeTracking) — `ActionLogPublisher` 주입
9. [ ] `GoodsController`, `OrderController` (`@PreAuthorize`, `ResultDTO` 래핑)
10. [ ] `DataInitializer` 수정: repo 주입 + `seedGoods()`/`seedOrders()` + `run()` 등록
11. [ ] `./mvnw test`(있으면) → `./mvnw clean package` 그린 확인, Swagger `/v3/api-docs`에 신규 스펙 노출 확인

**프론트** (백엔드 배포 후)
12. [ ] `npm run gen` (Orval) — `apis/query/goods/`, `apis/query/order/`, schemas 생성
13. [ ] `lib/order-status.ts`, `lib/goods-form.ts`
14. [ ] `components/goods/{GoodsStatusBadge,GoodsGrid,OptionRows,ImageRows}.tsx`
15. [ ] `components/order/{OrderStatusBadge,OrderGrid,OrderStatusStepper}.tsx`
16. [ ] `app/(protected)/goods/{page,add/page,[id]/page}.tsx`
17. [ ] `app/(protected)/order/{page,[id]/page}.tsx`
18. [ ] `components/layout/Sidebar.tsx` 메뉴 2개 추가
19. [ ] `npm run build:no-lint` / lint 그린 확인

---

## 7. 위험/주의

- **`ORDER` 예약어**: 테이블명은 반드시 `ORDERS`(`@Table(name="ORDERS")`), MyBatis SQL도 `ORDERS o`. 엔티티 클래스명은 `Order`로 두되 매핑만 ORDERS.
- **상태 전이 단일 진실**: 허용 전이맵은 백엔드(`OrderService.TRANSITIONS`)가 권위. 프론트 `ALLOWED_TRANSITIONS`는 UX용 복제 — 두 곳을 항상 동기화(불일치 시 백엔드가 `INVALID_PARAMETER`).
- **재고 음수/중복 차감**: `PAID` 진입 시에만 차감, `CANCEL/RETURN`은 "이미 차감된 상태(from ∈ PAID·READY·SHIPPING·DONE)"일 때만 복원. `PLACED→CANCEL`은 차감 전이므로 복원 없음. `sale_count` 복원 시 음수 가드.
- **트랜잭션 경계**: `changeStatus`는 재고변경+영수증생성+상태변경을 한 `@Transactional`로. JPA 변경감지 의존 시 `saveAndFlush` 후 detail 재조회(ContentService 패턴).
- **`npm run gen` 전 배포 확인**(CLAUDE.md 규칙) — 백엔드 미배포 상태에서 절대 실행 금지. Orval 생성 디렉토리(`apis/query/`) 직접 수정 금지.
- **5개 이상 파일 변경 사전보고**(CLAUDE.md): 이 도메인은 다수 파일 생성이므로 단계(백엔드 엔티티→매퍼→서비스→컨트롤러→시드, 이후 프론트)별로 진행·검증.
- **AG Grid 인라인 편집 검증**: 음수/빈값 방지(cellEditor 제약 + 백엔드 `bulkUpdate`에서 null=무시·범위검증). 변경 행만 전송(전체 PUT 금지).
- **데모 안전**(설계문서 §6.3): CANCEL/RETURN 등 파괴적 액션은 확인 모달 필수. 결제는 시뮬(실 PG 미연동), 영수증은 기록만.
- **시드 결정론**: 고정 패턴(`i % n`, 소수 배수)만 사용, `Random` 금지 — 데모 리셋 시 동일 데이터 보장.
