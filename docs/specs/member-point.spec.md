# 회원 확장 + 포인트 원장 구현 스펙 (member-point)

> 출처: `streamhub-portfolio-admin-design.md`(§6.1 ERD `member.grade/point_balance`, `point_ledger`, §6.2 상태머신, §10 P3) + `gnuboard5-admin-feature-spec.md`(§2.1 회원관리, §2.5 포인트관리 `point_list.php`).
> 모든 패턴은 실제 `streamhub-admin` 코드(member/statistics 도메인)를 그대로 따른다. 메모리 추정 아님 — 실제 파일 기준.

---

## 1. 목적/범위

기존 `member` 도메인을 레퍼런스 서비스(은혜 포인트·멤버십 등급) 수준으로 확장하고, 포인트 증감의 단일 진실 원천인 **포인트 원장(`POINT_LEDGER`)**을 추가한다. 핵심 어필은 *수동 지급/차감 시 회원 누적 포인트(`MEMBER.point_balance`)를 한 트랜잭션에서 동기화*하고, *만료 포인트를 스케줄 배치로 회수*하는 운영형 원장 로직이다. 기존 `Member` 엔티티/회원관리 화면을 깨지 않고 컬럼 추가(`grade`, `point_balance`) + 화면 확장(등급/포인트 컬럼·포인트 탭)으로 얹는다. 백엔드는 원장 엔티티·수동 지급 API·회원별 원장 조회 API·만료 배치를, 프론트는 포인트 원장 화면(검색+수동지급 폼)과 회원 상세 포인트 탭을 신규로 구현한다.

---

## 2. 데이터 모델

### 2.1 `MEMBER` 확장 (기존 테이블에 컬럼 2개 추가 — 비파괴)

기존 `Member.java`(`v1/member/entity/Member.java`)에 필드 추가. 기존 빌더/`updateProfile`/`changeStatus`는 유지하고, 신규 enum 1개 + 도메인 메서드 1개를 추가한다.

| 필드(Java) | 컬럼(@Column) | 타입 | 제약 | 비고 |
|---|---|---|---|---|
| `grade` | `grade` | `Grade` (enum, `@Enumerated(STRING)`) | `nullable=false, length=20`, default `BRONZE` | 멤버십 등급. 운영 권한 아님(설계 §6.3) |
| `pointBalance` | `point_balance` | `long` | `nullable=false`, default `0` | 누적 포인트. 원장 증감의 캐시된 합계 |

신규 enum `Grade` (`v1/member/entity/Grade.java`):
```java
public enum Grade { BRONZE, SILVER, GOLD, ANGEL }   // 브론즈/실버/골드/후원천사
```

`Member`에 추가할 도메인 메서드 (원장 서비스에서만 호출):
```java
/** 포인트 잔액을 delta만큼 조정한다. 음수 허용, 단 0 미만으로 내려갈 수 없다. */
public void adjustPointBalance(long delta) {
    long next = this.pointBalance + delta;
    if (next < 0) {
        throw new IllegalStateException("point balance cannot go negative");
    }
    this.pointBalance = next;
    this.updatedAt = LocalDateTime.now();
}
```

> 인덱스: 등급별 집계(대시보드 도넛)를 위해 `idx_member_grade(grade)`를 `@Table(indexes=...)`에 추가. 기존 3개 인덱스(`idx_member_church/status/created`)는 유지.

스키마 적용: Hibernate `ddl-auto`가 신규 컬럼을 추가(기존 데이터 보존). default 보장을 위해 시드/마이그레이션에서 기존 행을 `BRONZE`/`0`으로 채운다(§5 참조). **기존 컬럼 삭제·rename 금지.**

### 2.2 신규 테이블 `POINT_LEDGER`

엔티티 `PointLedger` (`v1/member/entity/PointLedger.java`). 기존 `Member`/`WatchHistory` 엔티티 컨벤션(대문자 테이블, snake `@Column`, `@Builder` private 생성자, `@NoArgsConstructor(PROTECTED)`) 그대로.

| 필드(Java) | 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|---|
| `id` | `id` | `Long` | `@Id @GeneratedValue(IDENTITY)` | PK |
| `memberId` | `member_id` | `Long` | `nullable=false` | FK(논리). JPA 연관 없이 id 보관(기존 `WatchHistory.memberId` 방식) |
| `delta` | `delta` | `long` | `nullable=false` | 증감량. 음수=차감/사용 (gnuboard `po_point`) |
| `balanceAfter` | `balance_after` | `long` | `nullable=false` | 적용 후 잔액(원장 무결성·감사용) |
| `reason` | `reason` | `String` | `nullable=false, length=200` | 사유 (gnuboard `po_content`) |
| `sourceType` | `source_type` | `LedgerSourceType` (enum, STRING) | `nullable=false, length=20` | `MANUAL`/`EXPIRE`(향후 `DONATION`/`ORDER` 확장 여지) |
| `status` | `status` | `LedgerStatus` (enum, STRING) | `nullable=false, length=20`, default `ACTIVE` | `ACTIVE`/`EXPIRED`. 만료 시 전이 |
| `expireAt` | `expire_at` | `LocalDateTime` | nullable | 만료 시각(없으면 무기한). gnuboard `po_expire_term` |
| `createdAt` | `created_at` | `LocalDateTime` | `nullable=false` | 발생 시각 |

```java
@Table(name = "POINT_LEDGER", indexes = {
        @Index(name = "idx_ledger_member", columnList = "member_id"),
        @Index(name = "idx_ledger_created", columnList = "created_at"),
        @Index(name = "idx_ledger_expire", columnList = "status, expire_at")  // 만료 배치 스캔용 복합
})
```

신규 enum 2개 (`v1/member/entity/LedgerSourceType.java`, `LedgerStatus.java`):
```java
public enum LedgerSourceType { MANUAL, EXPIRE }
public enum LedgerStatus { ACTIVE, EXPIRED }
```

도메인 메서드:
```java
/** 만료 처리: 이 적립 항목을 EXPIRED로 표시한다(회수 원장은 별도 row로 기록). */
public void markExpired() { this.status = LedgerStatus.EXPIRED; }
```

---

## 3. 백엔드

### 3.1 생성/수정 파일 목록

**신규 파일**
| 파일 | 역할 |
|---|---|
| `v1/member/entity/Grade.java` | 등급 enum |
| `v1/member/entity/PointLedger.java` | 원장 엔티티 |
| `v1/member/entity/LedgerSourceType.java` | 원장 출처 enum |
| `v1/member/entity/LedgerStatus.java` | 원장 상태 enum |
| `v1/member/repository/PointLedgerRepository.java` | JPA 리포지토리(만료 스캔 쿼리) |
| `v1/member/mapper/PointLedgerMapper.java` | MyBatis 매퍼 인터페이스(조인 목록/집계) |
| `resources/mappers/PointLedgerMapper.xml` | 원장 목록/카운트 SQL |
| `v1/member/PointController.java` | 포인트 원장 엔드포인트 |
| `v1/member/PointService.java` | 지급/차감 트랜잭션 + 만료 배치 |
| `v1/member/PointExpiryScheduler.java` | `@Scheduled` 만료 트리거 |
| `v1/member/dto/PointLedgerSearchRequest.java` | 원장 검색 DTO(record) |
| `v1/member/dto/PointLedgerListItem.java` | 원장 목록 행 DTO |
| `v1/member/dto/PointGrantRequest.java` | 수동 지급/차감 요청 DTO(record, validation) |

**수정 파일**
| 파일 | 변경 |
|---|---|
| `v1/member/entity/Member.java` | `grade`/`pointBalance` 필드 + `adjustPointBalance` + `idx_member_grade` + 빌더 인자 추가 |
| `v1/member/dto/MemberDetail.java` | `grade`/`pointBalance` 필드 추가 |
| `v1/member/dto/MemberListItem.java` | `grade`/`pointBalance` 필드 추가 |
| `resources/mappers/MemberMapper.xml` | `selectList`/`selectDetail` SELECT에 `m.grade`, `m.point_balance` 추가 |
| `v1/member/dto/MemberUpdateRequest.java` | (선택) 등급 수정 허용 시 `grade` 추가 — 1차 범위에서는 미포함, 원장으로만 포인트 변경 |
| `base/config/DataInitializer.java` | `seedMembers()` 빌더에 `grade` 지정 + 신규 `seedPointLedger()` 추가 + `run()`에 호출 |
| `StreamhubApiApplication.java` | 클래스에 `@EnableScheduling` 추가(현재 미설정 — 만료 배치 필수) |

### 3.2 API 엔드포인트

`PointController`: `@RequestMapping("/v1/point")`, 클래스 `@PreAuthorize` 는 `MemberController`와 동일(SYSTEM, CHURCH_MANAGER). 모든 응답 `ResultDTO<T>`. 회원 스코핑은 `MemberService`와 동일 패턴(`ensureInScope`)을 `PointService`가 수행.

| 메서드 | 경로 | 요청 DTO | 응답 DTO | 권한 | 설명 |
|---|---|---|---|---|---|
| POST | `/v1/point/list` | `PointLedgerSearchRequest`(body) | `ResultDTO<ResInfinityList<PointLedgerListItem>>` | SYSTEM/CHURCH_MANAGER | 원장 목록(검색·페이지). gnuboard `point_list.php` 목록 |
| GET | `/v1/point/member/{memberId}` | `?pageNumber&pageSize`(query) | `ResultDTO<ResInfinityList<PointLedgerListItem>>` | SYSTEM/CHURCH_MANAGER | 회원별 원장(상세 포인트 탭) |
| POST | `/v1/point/grant` | `@Valid PointGrantRequest`(body) | `ResultDTO<PointLedgerListItem>` | SYSTEM/CHURCH_MANAGER | 수동 지급/차감(음수 가능). 생성된 원장 행 반환 |

> 검색 필드 매핑(gnuboard `sfl`=회원아이디/내용 → 본 도메인): `keyword`는 회원 `name`/`email` 또는 `reason` LIKE. `PointController`는 `principal`만 전달, 스코핑은 서비스. 만료 배치는 `@Scheduled` 내부 트리거이므로 공개 엔드포인트 없음.

**DTO 정의**

```java
// PointLedgerSearchRequest (record) — MemberSearchRequest와 동일한 default 헬퍼
public record PointLedgerSearchRequest(
        Integer pageNumber, Integer pageSize,
        String keyword,        // 회원 name/email 또는 reason LIKE
        Long memberId,         // 특정 회원 한정(nullable)
        Long churchId) {       // SYSTEM 전용 필터, CHURCH_MANAGER는 서비스가 강제
    public int pageNumberOrDefault() { return pageNumber == null || pageNumber < 0 ? 0 : pageNumber; }
    public int pageSizeOrDefault()   { return pageSize == null || pageSize <= 0 ? 10 : pageSize; }
    public int offset()              { return pageNumberOrDefault() * pageSizeOrDefault(); }
}

// PointGrantRequest (record + validation) — gnuboard 수동 지급 폼
public record PointGrantRequest(
        @NotNull(message = "회원을 선택하세요") Long memberId,
        @NotNull(message = "포인트를 입력하세요") Long delta,        // 음수 가능(차감)
        @NotBlank(message = "사유를 입력하세요") String reason,
        Integer expireDays) {                                      // 유효기간(일). null이면 무기한
}

// PointLedgerListItem (@Getter @Setter @NoArgsConstructor) — MyBatis 매핑
//   id, memberId, memberName, memberEmail, churchId, delta, balanceAfter,
//   reason, sourceType, status, expireAt, createdAt
```

### 3.3 핵심 로직 (의사코드)

**수동 지급/차감 (`PointService.grant`)** — 단일 트랜잭션에서 원장 기록 + 회원 누적 동기화:
```
@Transactional
grant(PointGrantRequest req, AdminPrincipal principal):
    member = memberRepository.findById(req.memberId)  or throw NOT_FOUND
    ensureInScope(member.churchId, principal)          // MemberService와 동일
    member.adjustPointBalance(req.delta)               // 음수면 0 미만 방지 → IllegalState
    memberRepository.saveAndFlush(member)              // MyBatis 조회 전 flush(기존 update 패턴)
    expireAt = req.expireDays == null ? null
                                      : now().plusDays(req.expireDays)
    ledger = PointLedger.builder()
                .memberId(member.id).delta(req.delta)
                .balanceAfter(member.pointBalance)     // 동기화된 최신 잔액
                .reason(req.reason).sourceType(MANUAL)
                .status(ACTIVE).expireAt(expireAt)
                .createdAt(now()).build()
    pointLedgerRepository.saveAndFlush(ledger)
    actionLogPublisher.publish(                        // best-effort 감사(기존 패턴)
        req.delta >= 0 ? "POINT_GRANT" : "POINT_DEDUCT",
        "MEMBER", String.valueOf(member.id),
        req.delta + "P: " + req.reason)
    return pointLedgerMapper.selectById(ledger.id)     // 조인된 회원명 포함 반환
```
> `IllegalStateException`(잔액 음수)은 `GlobalExceptionHandler`에서 `INVALID_PARAMETER`로 매핑되도록 처리하거나, 서비스에서 사전 검증 후 `throw new ApiException(ResultCode.INVALID_PARAMETER, "포인트 잔액이 부족합니다")`로 변환(권장 — 명시적 메시지).

**만료 배치 (`PointService.expirePoints`)** — 도래한 적립을 회수하고 회수 원장 기록:
```
@Transactional
expirePoints():
    due = pointLedgerRepository
            .findByStatusAndExpireAtBefore(ACTIVE, now())   // idx_ledger_expire 사용
            .filter(l -> l.delta > 0)                        // 적립만 만료 대상(사용분 제외)
    for each l in due:
        member = memberRepository.findById(l.memberId) (없으면 skip)
        recover = min(l.delta, member.pointBalance)          // 이미 쓴 포인트는 회수 안 함
        if recover > 0:
            member.adjustPointBalance(-recover)
            memberRepository.save(member)
            pointLedgerRepository.save(PointLedger.builder()
                .memberId(member.id).delta(-recover)
                .balanceAfter(member.pointBalance)
                .reason("포인트 만료 회수").sourceType(EXPIRE)
                .status(ACTIVE).expireAt(null).createdAt(now()).build())
        l.markExpired()                                      // 원본 적립 EXPIRED 전이
    log.info("Expired {} ledger entries", due.size())
```

**스케줄러 (`PointExpiryScheduler`)**:
```java
@Component  // @RequiredArgs 대신 기존처럼 명시 생성자
public class PointExpiryScheduler {
    private final PointService pointService;
    @Scheduled(cron = "0 0 4 * * *")   // 매일 04:00 (데모: fixedRate로 바꿔 빠르게 시연 가능)
    public void run() { pointService.expirePoints(); }
}
```

**원장 목록 SQL (`PointLedgerMapper.xml`)** — `MemberMapper.xml` 구조 그대로(공유 `searchWhere`, `LIMIT #{offset},#{size}`):
```xml
<sql id="searchWhere">
  <where>
    <if test="keyword != null and keyword != ''">
      AND (mb.name LIKE CONCAT('%',#{keyword},'%')
           OR mb.email LIKE CONCAT('%',#{keyword},'%')
           OR pl.reason LIKE CONCAT('%',#{keyword},'%'))
    </if>
    <if test="memberId != null"> AND pl.member_id = #{memberId} </if>
    <if test="churchId != null"> AND mb.church_id = #{churchId} </if>
  </where>
</sql>

<select id="selectList" resultType="...PointLedgerListItem">
  SELECT pl.id, pl.member_id AS member_id, mb.name AS member_name,
         mb.email AS member_email, mb.church_id AS church_id,
         pl.delta, pl.balance_after AS balance_after, pl.reason,
         pl.source_type AS source_type, pl.status,
         pl.expire_at AS expire_at, pl.created_at AS created_at
  FROM POINT_LEDGER pl JOIN MEMBER mb ON pl.member_id = mb.id
  <include refid="searchWhere"/>
  ORDER BY pl.created_at DESC, pl.id DESC
  LIMIT #{offset}, #{size}
</select>
<select id="countList" resultType="long"> ... 동일 join + searchWhere ... </select>
<select id="selectById" resultType="...PointLedgerListItem"> ... WHERE pl.id=#{id} </select>
```
매퍼 인터페이스 `@Param` 시그니처는 `MemberMapper`와 동일 규약(`selectList(keyword,memberId,churchId,offset,size)`, `countList(...)`, `selectById(id)`).

**리포지토리 (`PointLedgerRepository`)**:
```java
public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {
    List<PointLedger> findByStatusAndExpireAtBefore(LedgerStatus status, LocalDateTime at);
    long countByMemberId(Long memberId);
}
```

**(선택) 대시보드 등급 분포** — 설계 §4.2 도넛 차트용. `StatMapper.xml`에 `gradeDistribution` 집계 추가 가능(범위 밖이면 P3 후속). 형식:
```sql
SELECT grade AS grade, COUNT(*) AS count FROM MEMBER GROUP BY grade
```

---

## 4. 프론트 (streamhub-web)

### 4.1 생성/수정 파일 목록

**신규 파일**
| 파일 | 역할 |
|---|---|
| `src/app/(protected)/point/page.tsx` | 포인트 원장 목록 + 수동 지급 폼 |
| `src/components/point/PointLedgerGrid.tsx` | 원장 AG Grid(`MemberGrid` 패턴) |
| `src/components/point/GrantPointForm.tsx` | 수동 지급/차감 폼(RHF+Zod) |
| `src/components/member/MemberPointTab.tsx` | 회원 상세 포인트 탭(회원별 원장 목록) |
| `src/components/member/GradeBadge.tsx` | 등급 배지(`StatusBadge` 패턴) |

**수정 파일**
| 파일 | 변경 |
|---|---|
| `src/components/layout/Sidebar.tsx` | `NAV_ITEMS`에 `{ label:"포인트 원장", href:"/point", icon: Coins }` 추가(lucide `Coins`) |
| `src/components/member/MemberGrid.tsx` | 컬럼 `grade`(GradeBadge), `pointBalance`(우정렬, `toLocaleString()`) 추가 |
| `src/app/(protected)/member/[id]/page.tsx` | 읽기 영역에 등급/포인트 표시 + 하단 `<MemberPointTab memberId={memberId}/>` 마운트 |
| `src/apis/query/...` (Orval 자동생성) | 백엔드 배포 후 `npm run gen` — `usePointList`, `usePointMemberLedger`, `usePointGrant`, 스키마(`PointGrantRequest`, `PointLedgerListItem`, `MemberDetail.grade/pointBalance`) 생성. **수기 편집 금지** |

> CLAUDE.md 안전 규칙: 수정 파일이 5개 이상이고 Orval 재생성으로 import 연쇄가 생기므로 `npm run gen`은 **백엔드 배포 확인 후에만**, 변경 목록 사전 보고 후 진행.

### 4.2 화면별 상세

**포인트 원장 목록 `(protected)/point/page.tsx`** — `member/page.tsx` 구조 복제:
- 상태: committed(`keyword`,`pageNumber`) + draft(`keywordDraft`) 분리, `PAGE_SIZE=10`.
- 데이터: `usePointList()`(POST `/v1/point/list`, `useMutation` — member 목록과 동일하게 mutation 패턴), `useEffect`로 `searchRequest` 변경 시 `fetchList`. UI 1-based → API 0-based(`pageNumber-1`).
- 검색바: 검색어 input(이름/이메일/사유) + [검색] 버튼(`bg-brand`).
- 상단 우측: [포인트 지급] 버튼 → `GrantPointForm` 모달/패널 토글. 성공 시 `setMessage` + `refetch()`.
- 결과: `<PointLedgerGrid rows={rows} />`. 페이지네이션 블록(member와 동일 `이전/다음`).
- 메시지/로딩/에러 UI는 member 페이지 클래스 그대로(`bg-blue-50`, `Loader2`, `h-[560px]`).

**`PointLedgerGrid.tsx`** — `MemberGrid` 복제, `ColDef<PointLedgerListItem>[]`:
| field | headerName | 렌더/포맷 |
|---|---|---|
| `createdAt` | 일시 | `formatDateTime` |
| `memberName` | 회원 | `?? "-"` |
| `memberEmail` | 이메일 | `flex 1.2` |
| `reason` | 사유 | `flex 1.4` |
| `delta` | 증감 | `valueFormatter`: `>0 ? "+"+n : n` 의 `toLocaleString()`, 양수 emerald/음수 red 셀클래스 |
| `balanceAfter` | 잔액 | `toLocaleString()` |
| `sourceType` | 출처 | `MANUAL`→수동, `EXPIRE`→만료 |
| `status` | 상태 | `ACTIVE`→유효, `EXPIRED`→만료(회색 배지) |
| `expireAt` | 만료일 | `formatDate`, null→"무기한" |

`ModuleRegistry.registerModules([AllCommunityModule])`, `themeQuartz.withParams(...)`, `overlayNoRowsTemplate="포인트 내역이 없습니다."` 동일. 선택/일괄 액션은 1차 미사용(읽기+지급만).

**`GrantPointForm.tsx`** (RHF + Zod):
```ts
const grantSchema = z.object({
  memberId: z.string().min(1, "회원 ID를 입력하세요")
             .refine(v => Number.isFinite(Number(v)), "숫자만"),
  delta: z.string().min(1, "포인트를 입력하세요")
             .refine(v => Number.isInteger(Number(v)) && Number(v) !== 0, "0이 아닌 정수"),
  reason: z.string().min(1, "사유를 입력하세요"),
  expireDays: z.string().optional(),   // 빈값 → undefined → 무기한
});
```
- 제출: `usePointGrant().mutate({ data: { memberId:Number, delta:Number, reason, expireDays: v?Number(v):undefined } })`.
- `onSuccess`: `resultCode===SUCCESS_CODE` 검사(member 패턴) → 메시지 + 폼 reset + 목록 refetch.
- 음수 delta 입력 시 "차감" 안내 텍스트 표시(gnuboard "음수 가능").

**회원 상세 포인트 탭 `MemberPointTab.tsx`**:
- props `{ memberId:number }`. `usePointMemberLedger(memberId, { pageNumber, pageSize })`(GET, `useQuery` + `enabled: Number.isFinite(memberId)` — member 상세 `useDetail` 패턴).
- 회원의 현재 잔액은 상세 `detail.pointBalance`를 카드로 강조 표시 + 등급 `GradeBadge`.
- 하단에 `PointLedgerGrid`(또는 간단 HTML 테이블 — action-log 페이지 테이블 패턴) 재사용해 해당 회원 원장 표시. 페이지네이션 동일.
- `member/[id]/page.tsx`의 `ReadonlyField` 영역에 `등급`(GradeBadge)·`보유 포인트`(`detail.pointBalance?.toLocaleString()+"P"`) 2칸 추가.

**`GradeBadge.tsx`** — `StatusBadge` 복제. 매핑: `BRONZE`→amber, `SILVER`→slate, `GOLD`→yellow, `ANGEL`→violet. 라벨 `브론즈/실버/골드/후원천사`.

**Sidebar** — `member/[id]` 패턴상 systemOnly 불필요(CHURCH_MANAGER도 자기 교회 회원 포인트 운영). `Coins` 아이콘 import 추가.

---

## 5. 시드 영향

`DataInitializer.java` — 멱등 패턴(`if(repo.count()>0) return;`) 유지.

1. **`seedMembers()` 수정**: 빌더에 `.grade(...)` 추가. 결정론적 분포(파레토 느낌):
   ```java
   Grade[] grades = {Grade.BRONZE, Grade.BRONZE, Grade.BRONZE, Grade.SILVER, Grade.GOLD, Grade.ANGEL};
   .grade(grades[i % grades.length])
   .pointBalance(0L)   // 초기 0, 원장 시드가 채움
   ```
   (`pointBalance`는 빌더 인자 추가하거나 원장 시드에서 `adjustPointBalance`로만 증가 — 후자가 원장-잔액 일관성 보장이라 권장)

2. **신규 `seedPointLedger()` + `run()`에 `seedPointLedger();` 추가** (member 시드 직후). 회원별 2~4건, 현실 분포:
   ```java
   if (pointLedgerRepository.count() > 0) return;
   List<Member> members = memberRepository.findAll();
   LocalDateTime now = LocalDateTime.now();
   String[] reasons = {"가입 축하 포인트","후원 감사 적립","출석 적립","굿즈 구매 사용","이벤트 보상"};
   for (int i=0; i<members.size(); i++) {
     Member m = members.get(i);
     int entries = 2 + (i % 3);                 // 2~4건
     for (int j=0; j<entries; j++) {
       long delta = (j % 4 == 3) ? -(500 + (i*j % 5)*100)   // 일부 사용(음수)
                                 : (1000 + (i*7 + j*13) % 9 * 500);
       m.adjustPointBalance(delta < 0 && m.getPointBalance()+delta < 0 ? 0 : delta);
       LocalDateTime at = now.minusDays((i+j) % 180);        // 최근 6개월 분포(설계 §7.2)
       LocalDateTime expire = (j % 2 == 0) ? at.plusDays(365) : null;
       ledgers.add(PointLedger.builder()
           .memberId(m.getId()).delta(delta).balanceAfter(m.getPointBalance())
           .reason(reasons[(i+j) % reasons.length])
           .sourceType(LedgerSourceType.MANUAL).status(LedgerStatus.ACTIVE)
           .expireAt(expire).createdAt(at).build());
     }
   }
   memberRepository.saveAll(members);            // 동기화된 잔액 저장
   pointLedgerRepository.saveAll(ledgers);
   ```
   - 일부 `expireAt`을 `now.minusDays(...)`(이미 만료)로 심어 **만료 배치 시연** 가능(설계 §9 "운영의 흠집").
   - 결정론적(고정 인덱스 수식) → 데모 리셋 재현(설계 §7.3).

3. `DataInitializer` 생성자에 `PointLedgerRepository` 주입 추가.

---

## 6. 구현 순서 체크리스트 (의존 순서, 파일 단위)

**백엔드 (먼저, 배포까지)**
- [ ] `Grade.java`, `LedgerSourceType.java`, `LedgerStatus.java` (enum 3종)
- [ ] `Member.java` 수정 — 필드/메서드/인덱스/빌더 인자
- [ ] `PointLedger.java` 엔티티
- [ ] `PointLedgerRepository.java` (JPA + 만료 스캔 쿼리)
- [ ] `MemberDetail.java` / `MemberListItem.java` 필드 추가
- [ ] `MemberMapper.xml` SELECT 컬럼 추가(`grade`,`point_balance`)
- [ ] DTO: `PointLedgerSearchRequest`, `PointLedgerListItem`, `PointGrantRequest`
- [ ] `PointLedgerMapper.java` + `PointLedgerMapper.xml`
- [ ] `PointService.java` (grant + expirePoints)
- [ ] `PointController.java` (list/member/grant)
- [ ] `StreamhubApiApplication.java` 에 `@EnableScheduling`
- [ ] `PointExpiryScheduler.java`
- [ ] `DataInitializer.java` — seedMembers grade + seedPointLedger + 주입
- [ ] `./mvnw test` (PointService 트랜잭션/만료 단위 테스트 권장: 지급/차감/잔액음수방지/만료회수 테이블 기반)
- [ ] 백엔드 배포 → `/v3/api-docs` 스펙 확인

**프론트 (백엔드 배포 후)**
- [ ] `npm run gen` (Orval 재생성, 사전 보고)
- [ ] `GradeBadge.tsx`
- [ ] `PointLedgerGrid.tsx`
- [ ] `GrantPointForm.tsx`
- [ ] `(protected)/point/page.tsx`
- [ ] `MemberPointTab.tsx`
- [ ] `MemberGrid.tsx` 컬럼 추가
- [ ] `member/[id]/page.tsx` 등급/포인트/탭 추가
- [ ] `Sidebar.tsx` 메뉴 추가
- [ ] `npm run build:no-lint` + 화면 검수(목록·지급·탭·만료 흔적)

---

## 7. 위험/주의

- **잔액-원장 정합성**: 잔액은 항상 `adjustPointBalance` → 원장 기록 순서로 한 트랜잭션 안에서만 변경. 직접 `pointBalance` setter 금지. `balanceAfter`는 flush 후 잔액을 기록해 감사 추적 보장.
- **음수 잔액 방지**: 차감/만료 회수 시 `min(delta, balance)`로 이미 사용된 포인트를 음수로 만들지 않음. 사전 검증 실패는 `ApiException(INVALID_PARAMETER, "포인트 잔액이 부족합니다")`로 명시 매핑.
- **`@EnableScheduling` 누락**: 현재 `StreamhubApiApplication`에 미설정 — 추가하지 않으면 만료 배치가 동작하지 않음. 데모 시연용으로 cron 대신 짧은 `fixedRate` 분기(프로파일) 고려.
- **MyBatis flush 타이밍**: grant에서 `saveAndFlush` 후 매퍼 조회(기존 `MemberService.update` 패턴 동일) — 누락 시 stale read.
- **기존 화면 비파괴**: `MemberDetail/MemberListItem`에 필드 추가는 하위호환(프론트 미사용 시 무시됨). Orval 재생성 전까지 프론트 타입 불일치 주의 — 백엔드 배포·gen 완료 전 프론트 머지 금지.
- **스코핑**: CHURCH_MANAGER는 타 교회 회원 포인트 지급 불가(`ensureInScope`). `grant`의 `memberId`가 스코프 밖이면 `FORBIDDEN`.
- **감사 로그 best-effort**: `actionLogPublisher.publish`는 실패해도 트랜잭션에 영향 없음(기존 패턴). 포인트 변경 자체는 원장이 진실 원천.
- **만료 동시성**: 배치와 수동 지급이 동시에 같은 회원을 건드릴 때 잔액 경쟁 가능 — 데모 규모에선 무시 가능하나, 운영 확장 시 `@Version` 낙관락 또는 원자적 UPDATE(`ContentRepository.incrementViewCount` 식) 고려.
