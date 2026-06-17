# 콘텐츠(스트리밍) 확장 — PalmPlus 차별점 구현 스펙

## 1. 목적 / 범위

기존 `content` + `statistics` 도메인을 **확장(delta)** 하여 StreamHub 어드민을 "단순 미디어 CRUD"에서 "스트리밍 운영툴"로 격상한다. 신규 엔티티는 **`ContentCategory`(콘텐츠 카테고리) 단 1개**만 추가하고, 나머지는 기존 `Content` 엔티티에 컬럼 2개(`categoryId`, `accessLevel`)를 더하고 `statistics` 도메인에 집계 메서드 2개를 추가하는 방식으로 처리한다. 핵심 가치 세 가지 — (a) **콘텐츠 카테고리**(설교/찬양/세미나) 분류·필터, (b) **멤버십 전용 콘텐츠**(`accessLevel` = FREE/MEMBERS) 표시·필터, (c) **콘텐츠 통계 위젯**(조회 Top·채널별 시청·카테고리 분포)을 기존 `ContentController`/`ContentService`/`ContentMapper.xml`/`StatController`/`StatMapper.xml`/`DataInitializer`와 기존 프론트 콘텐츠/대시보드 화면에 **그대로 얹는다**. 과도한 신규 도메인 패키지 생성 금지.

---

## 2. 데이터 모델

### 2.1 신규 엔티티 — `CONTENT_CATEGORY` (1개만 추가)

테이블 `CONTENT_CATEGORY`, 패키지 `org.streamhub.api.v1.content.entity.ContentCategory`. 컨벤션은 `Channel` 엔티티와 동일(`@NoArgsConstructor(PROTECTED)` + `@Builder` private 생성자, `created_at`은 `LocalDateTime.now()`).

| 필드 | 컬럼(@Column name) | 타입 | 제약 | 비고 |
|---|---|---|---|---|
| id | (PK) | `Long` | `@GeneratedValue(IDENTITY)` | |
| name | `name` | `String` | `nullable=false, length=50` | "설교", "찬양", "세미나" 등 |
| sortOrder | `sort_order` | `Integer` | `nullable=false` | 카탈로그/필터 정렬용, 기본 0 |
| createdAt | `created_at` | `LocalDateTime` | `nullable=false` | |

```java
@Table(name = "CONTENT_CATEGORY", indexes = {
        @Index(name = "idx_content_category_sort", columnList = "sort_order")
})
```

비즈니스 메서드 없음(읽기 전용 마스터). 신규 `ContentType` 같은 enum 추가 안 함 — 카테고리는 동적 마스터 테이블.

### 2.2 신규 enum — `ContentAccessLevel`

패키지 `org.streamhub.api.v1.content.entity.ContentAccessLevel` (`ContentType`/`ContentStatus`와 동일한 단순 enum 파일).

```java
public enum ContentAccessLevel {
    FREE,      // 누구나
    MEMBERS    // 멤버십(구독) 전용
}
```

### 2.3 기존 `Content` 엔티티 확장 (delta — 컬럼 2개 추가)

`org.streamhub.api.v1.content.entity.Content`에 아래 2필드를 추가한다. 기존 `@Builder` 생성자 시그니처와 `update(...)` 메서드에도 파라미터를 추가한다.

| 추가 필드 | @Column | 타입 | 제약 | 비고 |
|---|---|---|---|---|
| categoryId | `category_id` | `Long` | `nullable=true` | `CONTENT_CATEGORY.id` FK(논리적, JPA 연관 없이 `Long` id 보유 — 기존 `channelId` 패턴 동일) |
| accessLevel | `access_level` | `ContentAccessLevel` | `@Enumerated(STRING)`, `nullable=false, length=10` | 기본값 `FREE` (빌더에서 null이면 FREE) |

`@Table` indexes에 추가:
```java
@Index(name = "idx_content_category", columnList = "category_id"),
@Index(name = "idx_content_access", columnList = "access_level")
```

빌더 내부 기본값 처리(기존 `viewCount` 패턴 그대로):
```java
this.accessLevel = accessLevel != null ? accessLevel : ContentAccessLevel.FREE;
```

`update(...)` 메서드: 기존 파라미터 끝에 `Long categoryId, ContentAccessLevel accessLevel` 추가 후 `this.categoryId = categoryId; this.accessLevel = accessLevel;`.

> **마이그레이션 주의**: `access_level`은 `nullable=false`. 기존 시드/DB가 비어있는 개발 환경에서만 동작하므로 문제 없음(데모 리셋 기반). 운영 마이그레이션 계층은 만들지 않는다(CLAUDE.md 규칙).

---

## 3. 백엔드

### 3.1 생성/수정 파일 목록 (정확한 경로)

**신규 생성 (4)**
- `streamhub-api/src/main/java/org/streamhub/api/v1/content/entity/ContentCategory.java`
- `streamhub-api/src/main/java/org/streamhub/api/v1/content/entity/ContentAccessLevel.java`
- `streamhub-api/src/main/java/org/streamhub/api/v1/content/repository/ContentCategoryRepository.java` (`extends JpaRepository<ContentCategory, Long>`)
- `streamhub-api/src/main/java/org/streamhub/api/v1/content/dto/ContentCategoryDto.java` (record `ContentCategoryDto(Long id, String name, Integer sortOrder)` + `static from(ContentCategory)` — `ChannelDto` 패턴)

**기존 수정 (10)**
- `content/entity/Content.java` — 컬럼 2개 + 빌더 + update 시그니처 (§2.3)
- `content/dto/ContentSearchRequest.java` — 필터 2개 추가: `Long categoryId`, `ContentAccessLevel accessLevel`
- `content/dto/ContentCreateRequest.java` — `Long categoryId`, `ContentAccessLevel accessLevel` 추가(둘 다 optional, `@NotNull` 안 붙임)
- `content/dto/ContentListItem.java` — `categoryId`, `categoryName`, `accessLevel` 필드 추가 (Getter/Setter)
- `content/dto/ContentDetail.java` — `categoryId`, `categoryName`, `accessLevel` 필드 추가
- `content/mapper/ContentMapper.java` — `selectList`/`countList`에 `@Param("categoryId") Long`, `@Param("accessLevel") String` 추가
- `content/ContentService.java` — list 필터 전달, create/update에 categoryId·accessLevel 전달, `listCategories()` 추가
- `content/ContentController.java` — `GET /v1/content/categories` 엔드포인트 추가
- `resources/mappers/ContentMapper.xml` — searchWhere에 `category_id`/`access_level` 조건 + select 컬럼/조인 추가
- `statistics/StatController.java` + `StatService.java` + `mapper/StatMapper.java` + `resources/mappers/StatMapper.xml` — 카테고리 분포 + 콘텐츠 요약 위젯 추가 (§3.4)
- `base/config/DataInitializer.java` — `seedContentCategories()` 추가 + `seedContent()` 수정 (§5)
- 신규 stat DTO 2개: `statistics/dto/CategoryDistItem.java`, `statistics/dto/ContentStatSummary.java`

### 3.2 API 엔드포인트 표

권한은 기존과 동일: 클래스 레벨 `@PreAuthorize("hasAnyAuthority(...SYSTEM, ...CHURCH_MANAGER)")`. 모든 응답 `ResultDTO<T>` 래핑.

| 메서드 | 경로 | 요청 | 응답 | 권한 | 상태 |
|---|---|---|---|---|---|
| GET | `/v1/content/categories` | — | `ResultDTO<List<ContentCategoryDto>>` | SYSTEM·CHURCH_MANAGER | **신규** |
| POST | `/v1/content/list` | `ContentSearchRequest`(+categoryId,accessLevel) | `ResultDTO<ResInfinityList<ContentListItem>>` | 동일 | 확장 |
| GET | `/v1/content/{id}` | — | `ResultDTO<ContentDetail>`(+category,access) | 동일 | 확장 |
| POST | `/v1/content` | `ContentCreateRequest`(+categoryId,accessLevel) | `ResultDTO<ContentDetail>` | 동일 | 확장 |
| PUT | `/v1/content/{id}` | `ContentCreateRequest`(+categoryId,accessLevel) | `ResultDTO<ContentDetail>` | 동일 | 확장 |
| GET | `/v1/statistics/content-summary` | — | `ResultDTO<ContentStatSummary>` | 동일 | **신규** |
| GET | `/v1/statistics/category-distribution` | — | `ResultDTO<List<CategoryDistItem>>` | 동일 | **신규** |

신규 컨트롤러 메서드 예 (`ContentController`, `channels()` 바로 아래):
```java
@Operation(summary = "콘텐츠 카테고리 목록", description = "등록 폼/필터용 카테고리(설교·찬양·세미나).")
@GetMapping("/categories")
public ResultDTO<List<ContentCategoryDto>> categories() {
    return ResultDTO.ok(contentService.listCategories());
}
```

`StatController` 신규 메서드:
```java
@Operation(summary = "콘텐츠 요약", description = "총/게시/멤버십전용 콘텐츠 수 + 총 조회수.")
@GetMapping("/content-summary")
public ResultDTO<ContentStatSummary> contentSummary() {
    return ResultDTO.ok(statService.getContentSummary());
}

@Operation(summary = "카테고리별 콘텐츠 분포")
@GetMapping("/category-distribution")
public ResultDTO<List<CategoryDistItem>> categoryDistribution() {
    return ResultDTO.ok(statService.getCategoryDistribution());
}
```

### 3.3 핵심 로직 의사코드 — `ContentService`

`list()`는 enum → String 변환 패턴(`request.type() == null ? null : request.type().name()`)을 `accessLevel`에도 적용:
```
list(request):
    type       = request.type()?.name()
    status     = request.status()?.name()
    access     = request.accessLevel()?.name()    // 신규
    keyword    = blankToNull(request.keyword())
    size       = request.pageSizeOrDefault()
    rows  = contentMapper.selectList(keyword, type, status,
                request.channelId(), request.categoryId(), access,   // 신규 2개
                request.offset(), size)
    rows.forEach(r -> r.setThumbnailUrl(storageService.publicUrl(r.getThumbnailKey())))
    total = contentMapper.countList(keyword, type, status,
                request.channelId(), request.categoryId(), access)
    return ResInfinityList.of(rows, total, size)

create(request):
    content = Content.builder()
        ...(기존 필드)...
        .categoryId(request.categoryId())          // 신규
        .accessLevel(request.accessLevel())        // 신규(null이면 빌더가 FREE)
        .build()
    ...(기존: save, applyHashtags, recordThumbnailFile, actionLogPublisher.publish)...

update(id, request):
    content.update(..., request.categoryId(), request.accessLevel())   // 시그니처 확장
    ...(기존)...

listCategories():   // @Transactional(readOnly=true)
    return contentCategoryRepository.findAll(Sort.by("sortOrder"))
        .stream().map(ContentCategoryDto::from).toList()

listPublic(request):   // 기존 — accessLevel은 강제하지 않음(멤버십 콘텐츠도 공개 목록 노출, 상세에서만 게이트 가능)
    동일하게 status=PUBLISHED 강제, 나머지 그대로
```

> **트랜잭션**: 기존 그대로. `listCategories` = `readOnly=true`, create/update = `@Transactional`. saveAndFlush 후속 MyBatis 조회 패턴 유지.

### 3.4 집계 로직 — `StatService` / `StatMapper.xml`

신규 DTO (snake_case alias 자동 매핑):
```java
// statistics/dto/ContentStatSummary.java
@Getter @Setter @NoArgsConstructor
public class ContentStatSummary {
    private Long totalContents;
    private Long publishedContents;
    private Long membersOnlyContents;   // access_level = 'MEMBERS'
    private Long totalViews;
}
// statistics/dto/CategoryDistItem.java
@Getter @Setter @NoArgsConstructor
public class CategoryDistItem {
    private String categoryName;
    private Long contentCount;
    private Long totalViews;
}
```

`StatService` 신규 메서드 (요약은 `@Cacheable`로 캐싱 — 기존 `getSummary` 패턴):
```java
@Cacheable(cacheNames = "contentSummary", key = "'all'")
public ContentStatSummary getContentSummary() {
    log.info("Computing content summary (cache miss)");
    return statMapper.contentSummary();
}

public List<CategoryDistItem> getCategoryDistribution() {
    return statMapper.categoryDistribution();
}
```

`StatMapper.xml` 신규 쿼리 (서브쿼리 요약 + LEFT JOIN 분포):
```xml
<select id="contentSummary" resultType="...ContentStatSummary">
    SELECT
        (SELECT COUNT(*) FROM CONTENT)                                          AS total_contents,
        (SELECT COUNT(*) FROM CONTENT WHERE status = 'PUBLISHED')               AS published_contents,
        (SELECT COUNT(*) FROM CONTENT WHERE access_level = 'MEMBERS')           AS members_only_contents,
        (SELECT COALESCE(SUM(view_count), 0) FROM CONTENT)                      AS total_views
</select>

<select id="categoryDistribution" resultType="...CategoryDistItem">
    SELECT
        cc.name                            AS category_name,
        COUNT(c.id)                        AS content_count,
        COALESCE(SUM(c.view_count), 0)     AS total_views
    FROM CONTENT_CATEGORY cc
        LEFT JOIN CONTENT c ON c.category_id = cc.id
    GROUP BY cc.id, cc.name
    ORDER BY content_count DESC, cc.sort_order ASC
</select>
```

### 3.5 `ContentMapper.xml` delta

`searchWhere` `<sql>` 블록에 조건 2개 추가:
```xml
<if test="categoryId != null">
    AND c.category_id = #{categoryId}
</if>
<if test="accessLevel != null and accessLevel != ''">
    AND c.access_level = #{accessLevel}
</if>
```

`selectList`/`selectDetail` SELECT에 컬럼 추가 + `LEFT JOIN`(카테고리 null 허용이므로 INNER 아님):
```sql
c.category_id   AS category_id,
cc.name         AS category_name,
c.access_level  AS access_level,
...
FROM CONTENT c
    JOIN CHANNEL ch       ON c.channel_id = ch.id
    JOIN CHURCH chu       ON ch.church_id = chu.id
    LEFT JOIN CONTENT_CATEGORY cc ON c.category_id = cc.id
```
`countList`는 `FROM CONTENT c` 단독 유지하되, searchWhere가 `c.category_id`/`c.access_level`만 참조하므로 조인 불필요(기존과 동일).

---

## 4. 프론트엔드

### 4.1 생성/수정 파일 목록

**신규 (3)**
- `streamhub-web/src/components/dashboard/CategoryDistChart.tsx` — 카테고리별 콘텐츠 분포 도넛(ApexChart, `WatchByChannelChart` 패턴)
- `streamhub-web/src/components/content/AccessBadge.tsx` — `FREE`/`MEMBERS` 컬러 배지 (`ContentBadges.tsx` 패턴)
- (선택) `streamhub-web/src/components/dashboard/ContentSummaryCards.tsx` — 콘텐츠 요약 4카드 (`SummaryCards` 패턴). 미도입 시 대시보드 카드 생략 가능.

**기존 수정 (5)**
- `src/app/(protected)/content/page.tsx` — 카테고리 select 필터 + accessLevel select 필터 추가 (기존 type/status draft 패턴 복제)
- `src/components/content/ContentGrid.tsx` — `categoryName` 컬럼 + `accessLevel` 배지 컬럼 추가
- `src/app/(protected)/content/add/page.tsx` — 카테고리 select + accessLevel select 필드 + zod 스키마 확장
- `src/app/(protected)/content/[id]/page.tsx` — 상세/수정 폼에 카테고리·accessLevel 추가
- `src/app/(protected)/dashboard/page.tsx` — `CategoryDistChart` 위젯 그리드에 추가
- `src/components/content/ContentBadges.tsx` — (선택) AccessBadge를 여기 합쳐도 됨

> **자동생성 주의(CLAUDE.md)**: `src/apis/query/`는 직접 수정 금지. 백엔드 배포 후 `npm run gen`으로 재생성 → `ContentSearchRequest`/`ContentCreateRequest`/`ContentListItem`/`ContentDetail` 타입에 신규 필드 + `ContentSearchRequestAccessLevel`/`ContentCreateRequestAccessLevel` enum + `useCategories`/`useContentSummary`/`useCategoryDistribution` 훅이 생성된다. 그 전에는 프론트 작업 착수 금지.

### 4.2 화면별 상세

**콘텐츠 목록 (`content/page.tsx`)** — 기존 `useQuery(["content-list", searchRequest], list1, keepPreviousData)` 그대로. 추가:
- 카테고리 드래프트 state `categoryDraft`(string id 또는 `"ALL"`), `useCategories()`로 옵션 채움.
- accessLevel 드래프트 `accessDraft`(`"ALL" | "FREE" | "MEMBERS"`).
- `searchRequest`에 `categoryId: categoryDraft === "ALL" ? undefined : Number(categoryDraft)`, `accessLevel: accessDraft === "ALL" ? undefined : accessDraft` 병합.
- 검색바에 select 2개를 type/status select와 동일 마크업으로 추가.

**ContentGrid 컬럼 delta** (`ColDef<ContentListItem>[]`에 추가):
```ts
{ field: "categoryName", headerName: "카테고리", minWidth: 100, flex: 1,
  valueFormatter: (p) => p.value ?? "-" },
{ field: "accessLevel", headerName: "접근", minWidth: 90, sortable: false,
  cellRenderer: (p) => <AccessBadge value={p.value ?? undefined} /> },
```
`AccessBadge`: `FREE` → "전체" `bg-slate-100 text-slate-600`, `MEMBERS` → "멤버십" `bg-violet-100 text-violet-700` (BADGE_BASE 재사용).

**콘텐츠 등록/수정 (`add/page.tsx`, `[id]/page.tsx`)** — RHF + Zod:
- zod 스키마에 `categoryId: z.string().optional()`, `accessLevel: z.enum(["FREE","MEMBERS"])` 추가(기본 `"FREE"`).
- 카테고리 `<select>`: `useCategories()` 결과 map (채널 select와 동일, 단 optional이므로 "선택 안함" 옵션 포함).
- accessLevel `<select>`: 전체(FREE)/멤버십(MEMBERS).
- payload: `categoryId: values.categoryId ? Number(values.categoryId) : undefined`, `accessLevel: values.accessLevel`.

**대시보드 (`dashboard/page.tsx`)** — 기존 그리드에 위젯 추가:
```tsx
<div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
  <TopContentsChart />
  <WatchByChannelChart />
</div>
<div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
  <CategoryDistChart />        {/* 신규 도넛 */}
  {/* (선택) ContentSummaryCards 등 */}
</div>
```
`CategoryDistChart`: `useCategoryDistribution()` → `chart.type="donut"`, labels=categoryName, series=contentCount, `ChartCard`로 로딩/에러/빈 처리. 색상은 brand 계열(`#2563eb` 외 슬레이트 팔레트). `WatchByChannelChart` 구조 복제.

### 4.3 사이드바
기존 `콘텐츠관리`(`/content`) 항목에 모두 흡수되므로 **신규 메뉴 추가 불필요**. 콘텐츠 통계 위젯은 기존 `대시보드`에 포함. (별도 `/content/stats` 라우트를 원하면 `Sidebar.tsx` `NAV_ITEMS`에 `{ label: "콘텐츠 통계", href: "/content/stats", icon: BarChart3 }` 추가 가능하나, 본 스펙 범위는 대시보드 통합.)

---

## 5. 시드 영향 (`DataInitializer.java`)

`run()` 호출 순서에 `seedContentCategories()`를 `seedContent()` **앞**에 삽입 (FK 의존):
```java
seedAdmins();
seedOrganization();
seedMembers();
seedContentCategories();   // 신규 — seedContent보다 먼저
seedContent();             // 수정 — categoryId/accessLevel 부여
seedWatchHistory();
...
```

**`seedContentCategories()`** (멱등 — `if (contentCategoryRepository.count() > 0) return;`):
```java
String[] names = {"설교", "찬양", "세미나", "기도회", "특별집회"};
for (int i = 0; i < names.length; i++) {
    contentCategoryRepository.save(ContentCategory.builder()
        .name(names[i]).sortOrder(i).build());
}
```

**`seedContent()` delta** — 루프 내에서 결정론적으로 카테고리·accessLevel 부여 (기존 `i % ...` 패턴):
```java
List<Long> categoryIds = contentCategoryRepository.findAll().stream()
        .map(ContentCategory::getId).toList();
...
// 빌더에 추가:
.categoryId(categoryIds.get(i % categoryIds.size()))
.accessLevel(i % 5 == 0 ? ContentAccessLevel.MEMBERS : ContentAccessLevel.FREE)
// → 24건 중 ~5건이 멤버십 전용(category-distribution / content-summary가 0이 아니게)
```

추가 시드 데이터 없음 — 기존 24 콘텐츠·800 watch-history 재사용. 카테고리 5종만 신규.

---

## 6. 구현 순서 체크리스트 (파일 단위, 의존 순서)

**백엔드 (배포 전 완결)**
- [ ] `ContentAccessLevel.java` enum 생성
- [ ] `ContentCategory.java` 엔티티 + `ContentCategoryRepository.java` 생성
- [ ] `ContentCategoryDto.java` (record + from)
- [ ] `Content.java` — `categoryId`/`accessLevel` 필드·빌더·update·@Index 확장
- [ ] `ContentSearchRequest.java` / `ContentCreateRequest.java` 필드 추가
- [ ] `ContentListItem.java` / `ContentDetail.java` 필드 추가
- [ ] `ContentMapper.java` (@Param) + `ContentMapper.xml` (searchWhere·select·LEFT JOIN) 수정
- [ ] `ContentService.java` — list 필터 전달, create/update 확장, `listCategories()` 추가
- [ ] `ContentController.java` — `GET /categories` 추가
- [ ] `CategoryDistItem.java` / `ContentStatSummary.java` DTO 생성
- [ ] `StatMapper.java` + `StatMapper.xml` — `contentSummary`/`categoryDistribution` 추가
- [ ] `StatService.java`(@Cacheable) + `StatController.java` 엔드포인트 추가
- [ ] `DataInitializer.java` — `seedContentCategories()` + `seedContent()` delta + run() 순서
- [ ] `./mvnw test` 그린 + Colima 기동 후 `/v3/api-docs` 신규 스펙 확인 → **배포**

**프론트 (백엔드 배포 확인 후)**
- [ ] `npm run gen` (백엔드 배포 확인 필수 — CLAUDE.md) → 신규 타입·훅 생성 검증
- [ ] `AccessBadge.tsx` (또는 `ContentBadges.tsx`에 추가)
- [ ] `ContentGrid.tsx` — 카테고리·접근 컬럼 추가
- [ ] `content/page.tsx` — 카테고리·accessLevel 필터 추가
- [ ] `content/add/page.tsx` + `content/[id]/page.tsx` — 폼 필드·zod 확장
- [ ] `CategoryDistChart.tsx` 생성 + `dashboard/page.tsx` 위젯 추가
- [ ] `npm run build:no-lint` 그린 + 화면 QA

---

## 7. 위험 / 주의

- **`access_level` NOT NULL**: 기존 CONTENT 행이 있는 DB에 적용 시 NULL 위반. 데모는 결정론적 리셋 기반이므로 OK이나, 마이그레이션 호환 계층은 만들지 않는다(CLAUDE.md). 개발 DB는 드롭/리시드 전제.
- **`npm run gen` 타이밍**: 백엔드 미배포 상태에서 실행 금지(CLAUDE.md 명시). 신규 enum/훅이 없으면 프론트 컴파일 실패하므로 순서 엄수.
- **카테고리 LEFT JOIN**: `categoryId`가 null인 콘텐츠도 목록/상세에 나와야 하므로 `CONTENT_CATEGORY`는 반드시 `LEFT JOIN`(INNER 금지 — 누락 발생).
- **캐시 무효화**: `contentSummary`는 `@Cacheable(60s TTL)`. 콘텐츠 등록 직후 대시보드 숫자가 최대 60초 지연될 수 있음 — 데모상 허용(기존 `summary`와 동일 특성). 실시간성이 필요하면 캐시 미적용으로 전환.
- **5개 이상 파일 변경**: 본 작업은 백엔드 12+·프론트 6 파일 변경. CLAUDE.md "다수 파일 변경 안전장치"에 따라 착수 전 사용자에게 변경 대상 목록 보고·승인 필요.
- **`listPublic` accessLevel 미강제**: 멤버십 전용 콘텐츠를 공개 목록에서 숨길지(필터) vs 노출 후 상세에서 게이트할지 정책 미정. 본 스펙은 어드민 범위만 다루며 공개 게이트는 후속 결정 사항으로 남김.
