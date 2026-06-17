# C2 예배/새가족 온라인 등록 — 구현 스펙

> 출처 플랜: `docs/expansion-research-and-plan.md` §2·§5(C2). 사랑의교회 새가족 등록 폼 필드 표준 차용.
> **데모/테스트 모드**: 외부 API 키 없이 시드/목업으로 구현하되, 우편번호·SMS는 **어댑터 seam**으로 분리해 추후 실키 주입을 1줄 환경변수 토글로 가능하게 한다. 실제 외부 호출은 하지 않는다.
> 컨벤션 100% 준수: 대문자 SNAKE 테이블 · `@Column` snake · enum `@Enumerated(STRING)` · `@NoArgsConstructor(PROTECTED)`+`@Builder` · FK는 `Long ...Id` · `ResultDTO<T>` · 검색 record DTO · MyBatis XML은 `resources/mappers` · orval operationName은 태그+경로+verb 안정명.

---

## 1. 목적/범위 + 기존 자산 재활용 전략

### 목적
특정 교회(`church_id`)에 대한 **새가족/예배 온라인 등록**을 구현한다.
- **공개 신청(인증 불필요)**: 다단계 폼으로 인적·주소·신앙배경·인도자·가족(동적 최대 5)·개인정보 동의를 제출 → `WORSHIP_REGISTRATION` 1건 + `REGISTRATION_FAMILY` N건 생성.
- **관리자 신청관리(SYSTEM/CHURCH_MANAGER)**: 신청 목록(검색/필터/페이지네이션) · 상세 · 상태 전이(`RECEIVED`→`CONTACTED`→`COMPLETED`, 분기 `CANCELED`).

### 범위 밖 (명시)
- 결제(C4)·SMS 실발송(C6)·교회 위치검색(C1)은 별도 도메인. 단 **SMS 알림은 어댑터 seam만 배치**(no-op 목업 구현)하여 상태 전이 시 호출 지점을 미리 마련한다.
- 회원 계정 연동(로그인된 회원과 매칭)은 하지 않는다. 신청자는 비회원 입력으로 받는다.

### 기존 자산 재활용/확장/신규 표

| 분류 | 자산 | 처리 |
|---|---|---|
| **재사용(그대로)** | `base/response/ResultDTO`·`ResInfinityList`·`ResultCode` | 응답/페이지네이션/에러코드 그대로 |
| | `base/exception/ApiException` | `NOT_FOUND`/`INVALID_PARAMETER` 발생 |
| | `member/entity/Church`·`ChurchRepository` | `churchId` FK 검증(존재·`openYn='Y'`)에 사용 |
| | `actionlog/ActionLogPublisher` | 상태 전이 시 `WORSHIP_*` 액션 로그 발행 |
| | `base/security/SecurityConfig` `/pub/**` permitAll | 공개 신청 생성 엔드포인트를 `/pub/v1` 하위에 둠(코드 변경 불필요) |
| **확장(패턴 차용)** | `order` 도메인(상태머신 `TRANSITIONS` 맵 + `changeStatus`) | 등록 상태 전이 로직을 동형 구조로 신규 작성 |
| | `order` 의 1:N 자식 패턴(`Order`↔`OrderItem`) | `WorshipRegistration`↔`RegistrationFamily` 별도 테이블 1:N (JSON 대신 — 아래 §2 근거) |
| | `goods` 의 create/detail + 폼(RHF+Zod+`useFieldArray`) | 신규 다단계 폼·신청관리 화면이 그대로 차용 |
| | `pub/PublicController` | 공개 생성 엔드포인트 추가 위치 후보(별도 `WorshipPublicController` 신규로 분리 권장) |
| **신규** | `v1/worship/**` 백엔드 패키지 전체 | 컨트롤러·서비스·매퍼·엔티티·DTO |
| | `mappers/WorshipMapper.xml` | 목록/상세 MyBatis |
| | 프론트 `(protected)/worship/**` · 공개 `streamhub-user-web/.../register` | 관리자/신청 화면 |
| | `WorshipSeeder`(또는 `PortfolioSeeder` 확장) | 결정론적 신청 시드 |
| | `worship/adapter/**` | 우편번호·SMS 어댑터 seam |

> **REGISTRATION_FAMILY를 별도 테이블로 결정한 근거**: 코드베이스 전역에 JSON 컬럼(`columnDefinition=json`/`@JdbcTypeCode`/`@Convert`) 사용처가 **0건**이며, 1:N은 모두 별도 테이블(`OrderItem`/`GoodsOption`/`GoodsImage`/`ContentHashtag`)로 모델링되어 있다. 컨벤션 일치 + 관리자 상세에서 가족 행을 직접 조회/표시하기 위해 **별도 테이블**을 채택한다. 최대 5행 제약은 서비스단에서 검증.

---

## 2. 데이터 모델

### 2.1 enum (신규)

`v1/worship/entity/RegistrationStatus.java` — `@Enumerated(STRING)`, 전이 맵은 서비스가 단일 출처.
```
RECEIVED   접수
CONTACTED  연락완료
COMPLETED  등록완료
CANCELED   취소
```
전이(서비스 `TRANSITIONS`): `RECEIVED→{CONTACTED,CANCELED}`, `CONTACTED→{COMPLETED,CANCELED}`, `COMPLETED→{}`, `CANCELED→{}`.

`v1/worship/entity/Gender.java` — `MALE`, `FEMALE`.

`v1/worship/entity/RegisterDept.java` (등록부서) — `INFANT`(영아부), `CHILDREN`(아동부), `YOUTH`(중고등부), `YOUNG_ADULT`(청년부), `ADULT`(장년부), `SENIOR`(노년부).

`v1/worship/entity/BaptismType.java` (세례 단계) — `NONE`(없음), `BAPTISM`(세례), `CONFIRMATION`(입교), `INFANT_BAPTISM`(유아세례).

### 2.2 `WORSHIP_REGISTRATION` (엔티티 `WorshipRegistration`)

`@Table(name = "WORSHIP_REGISTRATION", indexes = {...})`

| 컬럼 (snake) | 자바 필드 | 타입/제약 | 비고 |
|---|---|---|---|
| `id` | `id` | `Long` PK IDENTITY | |
| `church_id` | `churchId` | `Long` NOT NULL | FK → CHURCH |
| `reg_no` | `regNo` | `String(30)` NOT NULL UNIQUE | `WR-yyyyMMdd-NNNN` (주문번호 패턴 차용) |
| `status` | `status` | enum STRING NOT NULL len 12 | RegistrationStatus |
| `name` | `name` | `String(50)` NOT NULL | 신청자 이름 |
| `gender` | `gender` | enum STRING NOT NULL len 6 | |
| `birth_date` | `birthDate` | `LocalDate` NOT NULL | 생년월일 |
| `phone` | `phone` | `String(20)` NOT NULL | 연락처 |
| `email` | `email` | `String(120)` | 선택 |
| `zipcode` | `zipcode` | `String(10)` | 우편번호(다음 우편번호/목업) |
| `addr1` | `addr1` | `String(200)` | 기본주소 |
| `addr2` | `addr2` | `String(200)` | 상세주소 |
| `register_dept` | `registerDept` | enum STRING NOT NULL len 16 | 등록부서 |
| `church_experience` | `churchExperience` | `String(1)` NOT NULL | `Y`/`N` 교회경험 유무 |
| `prev_church` | `prevChurch` | `String(100)` | 이전 교회명(경험 Y일 때) |
| `baptism_type` | `baptismType` | enum STRING NOT NULL len 16 | 세례 단계 |
| `leader_name` | `leaderName` | `String(50)` | 인도자 이름 |
| `leader_phone` | `leaderPhone` | `String(20)` | 인도자 연락처 |
| `privacy_agreed` | `privacyAgreed` | `String(1)` NOT NULL | `Y` 필수(개인정보 동의) |
| `memo` | `memo` | `String(500)` | 관리자 메모(상태전이 시 누적 X, 최신값) |
| `test_mode` | `testMode` | `String(1)` NOT NULL | `Y` (데모 표기, donation 패턴 차용) |
| `created_at` | `createdAt` | `LocalDateTime` NOT NULL | 신청일시 |
| `updated_at` | `updatedAt` | `LocalDateTime` NOT NULL | |

인덱스:
```
idx_worship_reg_church  (church_id)
idx_worship_reg_status  (status)
idx_worship_reg_created (created_at)
idx_worship_reg_no      (reg_no)
```

메서드(엔티티 내, order 패턴): `changeStatus(RegistrationStatus to)`, `updateMemo(String memo)` — 둘 다 `updatedAt` 갱신.

### 2.3 `REGISTRATION_FAMILY` (엔티티 `RegistrationFamily`)

`@Table(name = "REGISTRATION_FAMILY", indexes = { @Index(name="idx_reg_family_reg", columnList="registration_id") })`

| 컬럼 | 필드 | 타입/제약 | 비고 |
|---|---|---|---|
| `id` | `id` | `Long` PK IDENTITY | |
| `registration_id` | `registrationId` | `Long` NOT NULL | FK → WORSHIP_REGISTRATION |
| `name` | `name` | `String(50)` NOT NULL | 가족 이름 |
| `relation` | `relation` | `String(20)` NOT NULL | 관계(배우자/자녀/부모 등 — 자유문자, goods optionType 패턴) |
| `birth_date` | `birthDate` | `LocalDate` | 선택 |
| `sort` | `sort` | `Integer` NOT NULL | 1..5 표시 순서 |

> 최대 5행은 서비스에서 검증(`INVALID_PARAMETER`). `OrderItem`처럼 생성 시 `registrationId` 바인딩 후 `saveAll`.

---

## 3. 백엔드

### 3.1 생성 파일 목록 (정확한 패키지 경로)

신규 패키지 루트: `streamhub-api/src/main/java/org/streamhub/api/v1/worship/`

```
worship/
  WorshipPublicController.java        # 공개 신청 생성 (/pub/v1/worship)
  WorshipAdminController.java         # 관리자 목록/상세/상태/메모 (/v1/worship)
  WorshipService.java                 # 생성 + 상태머신 + 상세조립
  dto/
    WorshipRegisterRequest.java       # record, 공개 생성 입력 (가족 배열 포함, @Valid)
    RegistrationFamilyDto.java        # record, 가족 1행 (요청/응답 공용)
    WorshipRegistrationDetail.java    # @Getter@Setter@NoArgsConstructor, 상세 (가족은 서비스가 채움)
    WorshipRegistrationListItem.java  # @Getter@Setter@NoArgsConstructor, 목록 1행
    WorshipSearchRequest.java         # record, 검색/페이지네이션
    WorshipStatusChangeRequest.java   # record, @NotNull status + memo
    WorshipRegisterResponse.java      # record, 생성 결과(regNo, id) — 공개 응답 최소화
  entity/
    WorshipRegistration.java
    RegistrationFamily.java
    RegistrationStatus.java
    Gender.java
    RegisterDept.java
    BaptismType.java
  repository/
    WorshipRegistrationRepository.java   # extends JpaRepository<WorshipRegistration, Long>; existsByRegNo
    RegistrationFamilyRepository.java     # findByRegistrationIdOrderBySortAscIdAsc
  mapper/
    WorshipMapper.java                   # @Mapper: selectList/countList/selectDetail
  adapter/
    PostcodeProvider.java                # interface (우편번호 seam)
    MockPostcodeProvider.java            # @Profile-less 기본 목업 구현 (no-op/정규화만)
    SmsNotifier.java                     # interface (SMS 알림 seam)
    NoopSmsNotifier.java                 # 기본 목업: 로그만 남기고 실제 발송 안 함
```

리소스: `streamhub-api/src/main/resources/mappers/WorshipMapper.xml` (신규)

### 3.2 수정 파일 목록

| 파일 | 변경 |
|---|---|
| `base/config/PortfolioSeeder.java` **또는 신규** `WorshipSeeder.java`(`@Order(3)`) | 신청 시드. **신규 `WorshipSeeder` 권장**(PortfolioSeeder 600줄 근접 — 경량화 규칙). |
| `application.yml` (`src/main/resources`) | `app.worship.postcode.provider: mock` / `app.worship.sms.provider: noop` 플래그 추가(실키 주입 지점). SecurityConfig는 **무변경**(`/pub/**` 이미 permitAll). |

> **5개 이상 파일 안전장치**: 본 작업은 신규 파일 다수 + 기존 2개 수정. 자동생성(`src/apis/query`)은 `npm run gen`으로만 변경. 착수 전 사용자 승인 대상.

### 3.3 API 엔드포인트 표

| # | 메서드 | 경로 | 권한 | 입력 DTO | 응답 | orval 훅명 |
|---|---|---|---|---|---|---|
| 1 | POST | `/pub/v1/worship` | permitAll(공개) | `WorshipRegisterRequest` | `ResultDTO<WorshipRegisterResponse>` | `publicWorshipCreate` |
| 2 | GET | `/pub/v1/worship/churches` | permitAll | — | `ResultDTO<List<ChurchOptionDto>>`* | `publicWorshipChurches` |
| 3 | POST | `/v1/worship/list` | SYSTEM·CHURCH_MANAGER | `WorshipSearchRequest` | `ResultDTO<ResInfinityList<WorshipRegistrationListItem>>` | `worshipList` |
| 4 | GET | `/v1/worship/{id}` | SYSTEM·CHURCH_MANAGER | — | `ResultDTO<WorshipRegistrationDetail>` | `worshipDetail` |
| 5 | PATCH | `/v1/worship/{id}/status` | SYSTEM·CHURCH_MANAGER | `WorshipStatusChangeRequest` | `ResultDTO<WorshipRegistrationDetail>` | `worshipStatus` |

\* 공개 폼의 교회 선택용. `Church` 의 `openYn='Y'`만. `ChurchOptionDto(id,name)` record를 `worship/dto`에 신규. (간소화를 위해 기존 멤버 도메인에 두지 않고 worship 내 신규.)

> 훅명 검증: `orval.config.js` 규칙상 `Tag=Public`+route `/pub/v1/worship`+POST → DROP(pub,v1) 후 `worship` 세그먼트 → `publicWorshipCreate`. `/v1/worship/{id}/status` PATCH (Tag `Worship`) → 도메인 중복 세그먼트 drop → `status`는 ACTION_WORDS 포함 → `worshipStatus`. 새 ACTION_WORD 추가 불필요(`status`는 이미 등록됨).

### 3.4 `WorshipService` 핵심 로직 의사코드

```
// 전이 맵 (order 패턴 — 단일 출처)
TRANSITIONS = {
  RECEIVED  : {CONTACTED, CANCELED},
  CONTACTED : {COMPLETED, CANCELED},
  COMPLETED : {},
  CANCELED  : {},
}

@Transactional
create(WorshipRegisterRequest req):
  church = churchRepository.findById(req.churchId).orElseThrow(NOT_FOUND)
  if church.openYn != "Y": throw INVALID_PARAMETER("등록을 받지 않는 교회")
  if req.privacyAgreed != "Y": throw INVALID_PARAMETER("개인정보 동의 필요")
  families = req.families ?? []
  if families.size > 5: throw INVALID_PARAMETER("가족은 최대 5명")
  if req.churchExperience == "Y" and blank(req.prevChurch):
     // 선택 검증 — 경고 수준, 막지 않음(데모 관대)
  regNo = buildRegNo(now)   // WR-yyyyMMdd-NNNN, existsByRegNo 충돌 시 +1
  reg = WorshipRegistration.builder()
          .churchId(req.churchId).regNo(regNo).status(RECEIVED)
          .name(req.name).gender(req.gender).birthDate(req.birthDate)
          .phone(req.phone).email(req.email)
          .zipcode(req.zipcode).addr1(req.addr1).addr2(req.addr2)
          .registerDept(req.registerDept)
          .churchExperience(defaultYn(req.churchExperience))
          .prevChurch(req.prevChurch).baptismType(req.baptismType ?? NONE)
          .leaderName(req.leaderName).leaderPhone(req.leaderPhone)
          .privacyAgreed("Y").testMode("Y").build()
  saved = repo.save(reg)
  boundFamilies = families.indexed().map(f -> RegistrationFamily.builder()
        .registrationId(saved.id).name(f.name).relation(f.relation)
        .birthDate(f.birthDate).sort(index+1).build())
  familyRepo.saveAll(boundFamilies)
  // 알림 seam — 실제 발송 X (NoopSmsNotifier)
  smsNotifier.notifyRegistrationReceived(saved.phone, saved.regNo)
  // 공개 호출이라 SecurityContext 없음 → publishAs(null,...) 형태 사용
  actionLogPublisher.publishAs(null, "신청자", "WORSHIP_RECEIVED", "WORSHIP",
        str(saved.id), saved.name)
  return new WorshipRegisterResponse(saved.id, regNo)

@Transactional(readOnly=true)
list(WorshipSearchRequest req):           // order.list 동형
  rows = worshipMapper.selectList(searchField, keyword, status, churchId,
                                  fromDate, toDate, offset, size)
  total = worshipMapper.countList(...)
  return ResInfinityList.of(rows, total, size)

@Transactional(readOnly=true)
getDetail(id):
  detail = worshipMapper.selectDetail(id) ?: throw NOT_FOUND
  detail.setFamilies(familyRepo.findByRegistrationIdOrderBySortAscIdAsc(id).map(dto))
  return detail

@Transactional
changeStatus(id, WorshipStatusChangeRequest req):
  reg = repo.findById(id).orElseThrow(NOT_FOUND)
  from = reg.status; to = req.status
  if !TRANSITIONS[from].contains(to): throw INVALID_PARAMETER("허용되지 않는 상태 전이")
  if notBlank(req.memo): reg.updateMemo(req.memo)
  reg.changeStatus(to)
  repo.saveAndFlush(reg)
  if to == CONTACTED: smsNotifier.notifyContacted(reg.phone, reg.regNo)  // seam, no-op
  actionLogPublisher.publish("WORSHIP_"+to.name(), "WORSHIP", str(id), reg.regNo)
  return getDetail(id)
```

`buildRegNo`: `"WR-" + yyyyMMdd + "-" + %04d(seq)`. seq는 당일 카운트 기반(`countByCreatedDate`) 또는 `existsByRegNo` 루프 증가(주문 시드의 일별 seq 패턴 차용, 충돌 회피).

### 3.5 외부연동 어댑터 seam 설계 (핵심 제약)

**우편번호 — `PostcodeProvider`**
```java
public interface PostcodeProvider {
    /** 우편번호 입력값 정규화/검증. 실키 모드에서는 외부 주소 API 보강에 사용. */
    PostcodeResult resolve(String zipcode, String addr1);
}
```
- `MockPostcodeProvider`(기본, `@Component`): 외부 호출 없이 입력값 trim/형식검증만 수행하고 그대로 반환. **프론트의 다음(카카오) 우편번호 스크립트가 zipcode/addr1을 채워 보내므로 서버는 저장만** 한다. seam은 "추후 서버측 주소 정제/좌표 보강"을 위한 자리.
- 실키 주입 지점: `application.yml` `app.worship.postcode.provider`(기본 `mock`). 실 구현 추가 시 `@ConditionalOnProperty(name="app.worship.postcode.provider", havingValue="kakao")`로 `KakaoPostcodeProvider`를 등록. **인터페이스/주입처는 본 스펙에서 확정, 실 구현 클래스는 미작성**(키 없음).

**SMS 알림 — `SmsNotifier`**
```java
public interface SmsNotifier {
    void notifyRegistrationReceived(String phone, String regNo);
    void notifyContacted(String phone, String regNo);
}
```
- `NoopSmsNotifier`(기본, `@Component`): `log.info("[DEMO][SMS-noop] {} → {}", ...)`만. **실제 발송 절대 없음.**
- 실키 주입 지점: `app.worship.sms.provider`(기본 `noop`). 추후 `AligoSmsNotifier`/`SolapiSmsNotifier`를 `@ConditionalOnProperty(havingValue="aligo")` 등으로 교체. 발신번호/템플릿은 그 구현체의 책임. (플랜 §4 — 알리고 `testmode_yn=Y` / SOLAPI 무료포인트.)

> seam 원칙: 비즈니스 로직(`WorshipService`)은 **인터페이스에만 의존**. 기본 빈은 목업. 환경변수 1줄 + 실구현 1클래스 추가로 실연동 전환. CLAUDE.md "마이그레이션/호환계층 금지" 준수 — 목업↔실키는 빈 교체이지 버전 분기가 아님.

### 3.6 `WorshipMapper.xml` 설계 (resources/mappers)

`namespace="org.streamhub.api.v1.worship.mapper.WorshipMapper"`. `OrderMapper.xml` 구조 그대로:
- `<sql id="searchWhere">`: `searchField`(`name`/`phone`/`regNo`) + `keyword` LIKE, `status`, `churchId`, `created_at` 범위(`fromDate`/`toDate` `DATE_ADD ... INTERVAL 1 DAY`).
- `selectList` → `WorshipRegistrationListItem` (CHURCH join으로 `church_name`, 가족 수 서브쿼리 `(SELECT COUNT(*) FROM REGISTRATION_FAMILY rf WHERE rf.registration_id = wr.id) AS family_count`). `ORDER BY wr.created_at DESC, wr.id DESC LIMIT #{offset}, #{size}`.
- `countList` → `long`.
- `selectDetail` → `WorshipRegistrationDetail` (CHURCH join `church_name`; 가족은 서비스가 채움).

`WorshipRegistrationListItem` 표시 컬럼: `id, reg_no, church_id, church_name, status, name, gender, register_dept, phone, family_count, test_mode, created_at`.

---

## 4. 프론트엔드

### 4.1 키 불필요 기술 선택 (데모 제약)

| 필요 | 선택 | 키 |
|---|---|---|
| 우편번호 검색 | **다음(카카오) 우편번호 임베드 스크립트** `//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js` (무료, **앱키 불필요**) + 실패/오프라인 시 **목업 수동 입력**(zipcode/addr1 직접 타이핑) 폴백 | 없음 |
| 폼/검증 | RHF + Zod (`goods-form.ts` 패턴) | — |
| 가족 동적배열 | `useFieldArray` (`OptionRows.tsx` 패턴), 최대 5 가드 | — |
| 목록 그리드 | 신청관리는 **단순 테이블**(AG Grid 불필요 — 인라인 편집 없음). 필요 시 `goods/page.tsx`의 React Query+POST list 패턴 차용 | — |

> 다음 우편번호 스크립트는 인증키가 필요 없는 공개 임베드다(C1의 Kakao Local API와 별개). 스크립트 로드 실패 시 즉시 수동 입력 모드로 폴백하며, 두 경로 모두 `zipcode/addr1`을 동일 폼 필드로 채운다.

### 4.2 파일 목록 (정확한 경로)

**관리자 (streamhub-web)**
```
src/app/(protected)/worship/page.tsx              # 신청 목록 (검색/필터/페이지네이션)
src/app/(protected)/worship/[id]/page.tsx         # 신청 상세 + 상태전이 + 가족 표 + 메모
src/components/worship/WorshipStatusBadge.tsx     # 상태 배지 (OrderStatusBadge 패턴)
src/components/worship/WorshipStatusStepper.tsx   # RECEIVED→CONTACTED→COMPLETED 스텝퍼(선택)
src/lib/worship-status.ts                         # STATUS_LABEL/전이 UX 미러 + isDestructive (order-status.ts 패턴)
src/components/layout/Sidebar.tsx                 # (수정) "예배·새가족" 섹션 nav 추가
```

**공개 신청 폼 (streamhub-user-web — 비회원 공개 사이트)**
```
src/app/register/page.tsx                         # 다단계 등록 폼 (RHF+Zod, useFieldArray)
src/components/register/FamilyRows.tsx            # 가족 동적 배열 (OptionRows 패턴, 최대 5)
src/components/register/PostcodeField.tsx         # 다음 우편번호 임베드 + 목업 폴백
src/components/register/DemoBadge.tsx             # "데모/테스트" 배지
src/lib/register-form.ts                          # Zod 스키마 + payload 빌더 (goods-form.ts 패턴)
src/lib/api.ts                                    # (수정) api.registerWorship / api.worshipChurches 추가
src/lib/types.ts                                  # (수정) Worship* 타입 (user-web은 orval 미사용, 수기 타입)
```

> user-web은 orval을 쓰지 않고 `lib/api.ts`의 `request<T>` 래퍼 + `lib/types.ts` 수기 타입을 사용(기존 패턴). 관리자(streamhub-web)는 orval 생성 훅 사용.

### 4.3 화면별 컴포넌트·데이터패칭·폼

**(A) 공개 다단계 등록 폼 `register/page.tsx`** — 데모 배지 상단 고정
- 단계 구성(1 폼, 섹션 스텝): ① 인적사항(이름·성별 라디오·생년월일·연락처·이메일) ② 주소(`PostcodeField` — 우편번호검색/수동) ③ 등록부서(select) + 신앙배경(교회경험 Y/N → Y면 이전교회 표시, 세례단계 select) ④ 인도자(이름·연락처) ⑤ 가족(`FamilyRows` 동적, +추가 버튼 5개까지 disabled) ⑥ 개인정보 동의 체크(필수) + 제출.
- 검증: `register-form.ts`의 `registerFormSchema`(goods 패턴 — 숫자/날짜 string 유지 후 submit 시 변환). `privacyAgreed` refine 필수, `families` `z.array(...).max(5)`, `churchExperience==='Y'`일 때 `prevChurch` superRefine 권장(데모는 경고).
- 데이터: `api.worshipChurches()`로 교회 select 채움 → 제출은 `api.registerWorship(payload)`(`POST /pub/v1/worship`). 성공 시 `regNo` 노출하는 완료 화면.

**(B) 관리자 목록 `(protected)/worship/page.tsx`** — `goods/page.tsx` 동형
- 검색바: 검색어(이름/연락처/신청번호) · 상태(전체/RECEIVED/CONTACTED/COMPLETED/CANCELED) · 교회 select(SYSTEM만; CHURCH_MANAGER는 자기 교회 고정) · 기간.
- 패칭: `useQuery({queryKey:["worship-list", req], queryFn:()=>worshipList(req), placeholderData:keepPreviousData})`.
- 표: 신청번호·교회·신청자·부서·연락처·가족수·상태배지·신청일 + 행 클릭 → 상세. "데모" 배지 헤더.

**(C) 관리자 상세 `(protected)/worship/[id]/page.tsx`** — `order/[id]/page.tsx` 동형
- `useWorshipDetail(id)`로 로드. 인적/주소/신앙/인도자 읽기 필드 + 가족 표(REGISTRATION_FAMILY 행).
- 상태전이: 현재 상태에서 허용 전이 버튼만 노출(`worship-status.ts` UX 미러), 메모 입력 → `useWorshipStatus().mutate({id, data:{status,memo}})`. `CANCELED`는 destructive 스타일.

### 4.4 사이드바/네비 추가 (`Sidebar.tsx` 수정)

`NAV_SECTIONS`에 신규 섹션 1개 추가(커머스 아래):
```ts
{
  title: "교회",
  items: [
    { label: "예배·새가족 신청", href: "/worship", icon: ClipboardCheck },
  ],
},
```
아이콘은 `lucide-react`의 `ClipboardCheck`(또는 `UserPlus`) import 추가.

---

## 5. 시드 데이터 (결정론적·마스킹·가상)

신규 `WorshipSeeder implements CommandLineRunner` `@Order(3)`(PortfolioSeeder 600줄 근접 → 경량화). `DataInitializer`(`@Order(1)`)가 만든 CHURCH 이후 실행. 멱등(`worshipRegistrationRepository.count()>0`이면 skip), 고정시드 `Random(2001L)`.

- **건수**: ~36건. 교회별 분산(`churchRepository.findAll()`의 `openYn='Y'` 교회에 라운드로빈).
- **상태 분포**: 50% COMPLETED, 25% CONTACTED, 15% RECEIVED, 10% CANCELED (최근 3일 신청은 RECEIVED/CONTACTED만 — 리드타임 현실성, order `resolveStatus` 패턴).
- **시간 분포**: 최근 120일, `1 - sqrt(u)` 가중 우상향(PortfolioSeeder `distributedDateTime` 패턴 재사용 가능 — 동일 헬퍼를 worship용으로 복제).
- **인적/마스킹**: 이름 = `SURNAMES`+`GIVEN_NAMES`(기존 상수 차용, 가상). 연락처 = `010-****-%04d`(주문 시드 마스킹 패턴). email = `applicant%02d@streamhub.test`. 주소 = `"서울특별시 강남구 데모로 N길 M"`, zipcode = `"0%04d"`. 모든 행 `testMode="Y"`.
- **부서/성별/세례**: enum 라운드로빈으로 고르게.
- **가족**: 각 신청 0~3행(`relation`은 `{"배우자","자녀","자녀","부모"}`에서 순차), 이름 가상, `sort` 1..N.
- **인도자**: 50%만 채움(가상 이름 + 마스킹 번호).
- 로그: `log.info("Seeded {} worship registrations ({} family rows)", ...)`.

> 모든 PII는 **가공/마스킹/가상**(플랜 §6 준수). 실제 교회/개인 정보 미사용.

---

## 6. 구현 순서 체크리스트

- [ ] **B1 enum 5종** (`RegistrationStatus`/`Gender`/`RegisterDept`/`BaptismType` — Gender 포함) 작성.
- [ ] **B2 엔티티** `WorshipRegistration`·`RegistrationFamily` (`@Builder`/`PROTECTED`/snake/index) 작성.
- [ ] **B3 repository** 2종 (`existsByRegNo`, `findByRegistrationIdOrderBySortAscIdAsc`).
- [ ] **B4 DTO** record/POJO 7종 + `ChurchOptionDto`.
- [ ] **B5 어댑터 seam** `PostcodeProvider`+`MockPostcodeProvider`, `SmsNotifier`+`NoopSmsNotifier`.
- [ ] **B6 service** `WorshipService` (create/list/getDetail/changeStatus + TRANSITIONS + buildRegNo).
- [ ] **B7 mapper** `WorshipMapper.java` + `WorshipMapper.xml`.
- [ ] **B8 controller** `WorshipPublicController`(/pub/v1/worship) + `WorshipAdminController`(/v1/worship).
- [ ] **B9 application.yml** `app.worship.postcode.provider=mock` / `app.worship.sms.provider=noop` 추가.
- [ ] **B10 seeder** `WorshipSeeder` `@Order(3)` + 검증 `./mvnw test` / 부팅 시 시드 로그 확인.
- [ ] **F0 백엔드 배포 확인 후** `npm run gen` (streamhub-web) → `worship*`/`publicWorship*` 훅 생성 확인. **배포 전 gen 금지(CLAUDE.md).**
- [ ] **F1 관리자** `worship-status.ts` + `WorshipStatusBadge` + `(protected)/worship/page.tsx`(목록).
- [ ] **F2 관리자 상세** `(protected)/worship/[id]/page.tsx` (상태전이+가족표+메모).
- [ ] **F3 Sidebar.tsx** nav 항목 추가.
- [ ] **F4 공개 폼** user-web `lib/types.ts`+`lib/api.ts` 확장, `register-form.ts`, `PostcodeField`, `FamilyRows`, `DemoBadge`, `register/page.tsx`.
- [ ] **검증** `./mvnw fmt test lint` 류 통과(녹색) + 프론트 build:no-lint + 수동 e2e(공개 신청→관리자 목록 노출→상태 COMPLETED 전이).

---

## 7. "데모/테스트 모드" 정직 표기 위치

| 위치 | 표기 |
|---|---|
| **DB** | 모든 신청 행 `WORSHIP_REGISTRATION.test_mode = 'Y'` (donation 패턴). |
| **공개 폼 상단** | `DemoBadge` — "데모/테스트 신청입니다. 실제 등록·알림이 발송되지 않습니다." |
| **SMS seam** | `NoopSmsNotifier` 로그 `"[DEMO][SMS-noop] ..."`. 실제 발송 없음 — 코드/주석에 명시. |
| **우편번호** | `PostcodeField`에 "다음 우편번호(무료) / 키 없이 동작" 주석, 폴백 입력은 "수동 입력(데모)". |
| **관리자 목록/상세** | `test_mode='Y'` 표시 배지(헤더 "데모 데이터" 라벨). |
| **Swagger** | `@Tag(name="Worship", description="예배·새가족 신청 (데모/테스트 모드, 실알림 미발송)")`. |
| **시드 데이터** | 이름/연락처/주소 전부 가상·마스킹. 시드 로그가 데모 데이터임을 드러냄. |
| **스펙/플랜** | 본 문서 §3.5 seam 설계가 "실키 없음, 추후 주입" 정직 표기. |
