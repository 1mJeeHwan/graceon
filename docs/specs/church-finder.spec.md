# C1 교회찾기(위치기반) — 구현 스펙

> 도메인: **내 주변 교회 찾기**. `expansion-research-and-plan.md` Phase **C1**의 구현 명세.
> **핵심 제약(사용자 결정):** 외부 지도/검색 API 키 **미사용**. 좌표·교단·연락처는 **결정론적 시드/목업**으로 채우고, 추후 실키(Kakao Local 등) 주입이 쉽도록 **어댑터 seam**(인터페이스 + Provider + `.env` 플래그)을 둔다. 런타임 외부 호출은 **하지 않으며**, 화면에 **"데모/테스트 데이터" 배지**를 명시한다.
> 지도 렌더링은 **키 불필요**한 **Leaflet + OpenStreetMap** 타일을 사용하되, 추후 Kakao Maps SDK 교체가 가능하도록 `MapProvider` 추상화로 감싼다.

기준 코드 컨벤션은 `goods`/`order`/`content`/`member` 도메인 100% 준수: 대문자 SNAKE 테이블, `@Column(name="snake")`, enum `@Enumerated(EnumType.STRING)`, `@NoArgsConstructor(access=PROTECTED)+@Builder`, FK는 `Long ...Id`, 응답은 `ResultDTO<T>`/`ResInfinityList<T>`, 검색은 record DTO, MyBatis XML은 `resources/mappers/`, orval operationName은 태그+경로+verb 안정명.

---

## 1. 목적 / 범위 + 기존 자산 재활용 전략

### 1.1 목적
- 사용자(streamhub-user-web)가 **현위치 기준으로 가까운 교회**를 거리순·교단·키워드로 찾고, 지도 마커와 상세(예배시간·연락처·길찾기)를 본다.
- 관리자(streamhub-web)가 교회를 **목록/등록/수정**한다(좌표·교단·예배시간 포함).

### 1.2 범위
- **포함:** 기존 `CHURCH` 엔티티 **확장**(좌표/교단/주소/연락처/담임목사/시설), 별도 `WORSHIP_TIME` 테이블, 위치기반 검색 API(Haversine), 공개 조회 API(`/pub/v1/churches/**`), 관리자 CRUD API(`/v1/churches/**`), 관리자 교회관리 화면, 사용자 교회찾기/상세 화면, Leaflet 지도(MapProvider 추상화), `GeocodeProvider` seam(목업), 시드 ~40곳.
- **불포함:** 실제 외부 API 호출(Kakao/Naver/LOCALDATA), 예배신청/새가족 폼(=Phase C2), 매장찾기(=C3), 결제/SMS/챗봇/라이브.

### 1.3 기존 자산 재활용 (재사용 / 확장 / 신규)
| 구분 | 대상 | 비고 |
|---|---|---|
| **확장** | `member/entity/Church.java` | 좌표·교단·주소·전화·담임목사·시설·`useYn` 등 컬럼 추가. 기존 `regionId/name/openYn/createdAt` 유지. |
| **확장** | `member/repository/ChurchRepository.java` | 위치 후보 1차 필터용 derived/`@Query` 메서드 추가(또는 MyBatis로 일임). |
| **확장** | `base/config/DataInitializer.seedOrganization()` | 기존 5개 교회 시드를 **40곳 + 좌표/교단/예배시간**으로 대체(church id 1은 manager 계정용으로 유지). |
| **재사용** | `base/response/ResultDTO`, `ResInfinityList` | 응답 봉투 그대로. |
| **재사용** | `actionlog/ActionLogPublisher` | 관리자 CRUD 감사로그(`CHURCH_CREATE/UPDATE/DELETE`). |
| **재사용(패턴)** | `goods` 컨트롤러·서비스·MyBatis 매퍼·검색 record DTO | `church` 도메인 구조의 본. |
| **재사용(패턴)** | `pub/PublicController` + user-web `lib/api.ts` `request()` | 공개 조회 + fetch 클라이언트 패턴. |
| **재사용(패턴)** | admin-web `goods/page.tsx`(필터+RQ+페이지네이션), `goods/[id]`·`goods/add`(RHF+Zod 폼) | 관리자 화면 본. |
| **재사용** | admin-web `components/layout/Sidebar.tsx` | "커머스/회원·콘텐츠"와 별개로 네비 항목 1개 추가. |
| **신규** | `v1/church/**` 백엔드 패키지, `geo/HaversineDistance`, `external/geocode/**` seam, user-web `app/churches/**`, `components/map/**`(MapProvider) | 아래 상세. |

---

## 2. 데이터 모델

### 2.1 enum (신규)
`v1/church/entity/Denomination.java` — `@Enumerated(STRING)` 저장.
```
METHODIST   // 감리교
PCK         // 장로교(통합)  (예장통합)
HAPDONG     // 장로교(합동)  (예장합동)
HOLINESS    // 성결교
GOSPEL      // 순복음
BAPTIST     // 침례교
ETC         // 기타
```
> 한글 라벨은 프론트 매핑 테이블(`DENOMINATION_LABELS`)에서 표시. 백엔드는 enum명만 저장/검색.

`v1/church/entity/WorshipKind.java` — 예배 종류.
```
SUNDAY      // 주일예배
DAWN        // 새벽예배
WEDNESDAY   // 수요예배
FRIDAY      // 금요(철야)예배
YOUTH       // 청년/학생예배
OTHER       // 기타
```

### 2.2 `CHURCH` (확장) — table `CHURCH`
기존 4컬럼(`region_id`,`name`,`open_yn`,`created_at`) 유지 + 추가:

| 필드(Java) | @Column | 타입/길이 | 비고 |
|---|---|---|---|
| id | id | Long PK | 기존 |
| regionId | region_id | Long, NN | 기존 FK→REGION |
| name | name | String(100), NN | 기존 |
| openYn | open_yn | String(1), NN | 기존(공개여부) |
| createdAt | created_at | LocalDateTime, NN | 기존 |
| denomination | denomination | enum STRING(20) | **신규** 교단 |
| latitude | latitude | Double | **신규** WGS84 위도(시드값) |
| longitude | longitude | Double | **신규** WGS84 경도(시드값) |
| address | address | String(300) | **신규** 도로명 주소 |
| addressDetail | address_detail | String(200) | **신규** 상세 |
| zipcode | zipcode | String(10) | **신규** 우편번호 |
| phone | phone | String(30) | **신규** 마스킹 가상번호 |
| pastorName | pastor_name | String(50) | **신규** 담임목사 |
| facilities | facilities | String(200) | **신규** CSV 시설태그(`주차,승강기,영유아실,카페,주차장넓음`) |
| introduction | introduction | String(2000) | **신규** 소개 |
| homepageUrl | homepage_url | String(300) | **신규** |
| thumbnailKey | thumbnail_key | String(300) | **신규** (StorageService.publicUrl 통과; 외부 URL pass-through) |
| dataSource | data_source | String(20), NN | **신규** 출처 표기. 시드=`"SEED"`(데모배지 근거). 추후 실연동시 `"KAKAO"` 등 |
| useYn | use_yn | String(1), NN | **신규** 노출여부(공개 검색 필터) |
| updatedAt | updated_at | LocalDateTime, NN | **신규** |

**인덱스**(`@Table(indexes=...)`):
- `idx_church_region` (`region_id`)
- `idx_church_denom` (`denomination`)
- `idx_church_geo` (`latitude`, `longitude`) — bbox 1차 필터용
- `idx_church_use` (`use_yn`)

엔티티 메서드: `@Builder private Church(...)`, `update(...)`(관리자 수정), `applyGeocode(Double lat, Double lng, String source)`(seam 결과 주입용).
> **거리계산은 컬럼이 아님** — Haversine은 쿼리/서비스에서 계산. (공간인덱스/`POINT` 타입은 데모 단순화를 위해 미사용.)

### 2.3 `WORSHIP_TIME` (신규) — table `WORSHIP_TIME`
교회별 예배시간(1:N). `goods`의 옵션/이미지처럼 **delete-then-reinsert** 교체 전략.

| 필드 | @Column | 타입/길이 | 비고 |
|---|---|---|---|
| id | id | Long PK | |
| churchId | church_id | Long, NN | FK→CHURCH |
| kind | kind | enum STRING(15), NN | WorshipKind |
| dayLabel | day_label | String(20), NN | 표시 요일("주일","수요","매일") |
| startTime | start_time | String(5), NN | `"11:00"` (문자열, 표시 전용) |
| place | place | String(50) | "본당","교육관" |
| target | target | String(30) | "전체","장년","청년" |
| sort | sort | Integer, NN | 표시 순서 |

**인덱스:** `idx_worship_church` (`church_id`).

---

## 3. 백엔드

### 3.1 생성/수정 파일 목록 (정확한 패키지 경로)
신규 패키지 루트: `streamhub-api/src/main/java/org/streamhub/api/v1/church/`

**생성**
- `v1/church/ChurchController.java` — 관리자 CRUD (SYSTEM/CHURCH_MANAGER)
- `v1/church/PublicChurchController.java` — 공개 조회/위치검색 (`/pub/v1/churches`)
- `v1/church/ChurchService.java` — 목록/상세/CRUD/위치검색 오케스트레이션
- `v1/church/entity/Denomination.java`, `entity/WorshipKind.java`, `entity/WorshipTime.java`
- `v1/church/repository/WorshipTimeRepository.java`
- `v1/church/mapper/ChurchMapper.java`
- `v1/church/dto/ChurchSearchRequest.java`(관리자 record), `dto/ChurchNearbyRequest.java`(공개 위치검색 record), `dto/ChurchListItem.java`, `dto/ChurchNearbyItem.java`(+`distanceKm`), `dto/ChurchDetail.java`, `dto/WorshipTimeDto.java`, `dto/ChurchUpsertRequest.java`(등록/수정 공용, `@Valid`)
- `v1/church/geo/HaversineDistance.java` — 순수 유틸(거리 km)
- `base/external/geocode/GeocodeProvider.java` — **seam 인터페이스**
- `base/external/geocode/SeedGeocodeProvider.java` — 목업 구현(기본)
- `base/external/geocode/KakaoGeocodeProvider.java` — **실키 주입 지점(스텁, 비활성)**
- `base/external/geocode/GeocodeResult.java` — record(lat,lng,source,demo)
- `resources/mappers/ChurchMapper.xml`

**수정**
- `member/entity/Church.java`(확장), `member/repository/ChurchRepository.java`(필요시 bbox 메서드)
- `base/config/DataInitializer.java`(`seedOrganization` 확장 + `WorshipTimeRepository` 주입)
- `application.yml`(geocode seam 플래그)

> **패키지 배치 결정:** `CHURCH` 테이블/엔티티는 `member` 도메인 소속이라 `Church.java`는 그대로 두고, 신규 조회/검색/CRUD 로직만 `v1/church/`로 분리한다(굿즈/주문이 별 패키지인 것과 동일 결).

### 3.2 API 엔드포인트 표

**관리자 (`/v1/churches`, `@PreAuthorize` SYSTEM 또는 CHURCH_MANAGER — GoodsController와 동일 표현식)** · 태그 `Church`
| 메서드 | 경로 | Body/Param | 응답 | orval name |
|---|---|---|---|---|
| POST | `/v1/churches/list` | `ChurchSearchRequest`(keyword,regionId,denomination,useYn,page) | `ResInfinityList<ChurchListItem>` | `churchList` |
| GET | `/v1/churches/{id}` | path id | `ChurchDetail` | `churchDetail` |
| POST | `/v1/churches` | `@Valid ChurchUpsertRequest` | `ChurchDetail` | `churchCreate` |
| PUT | `/v1/churches/{id}` | `@Valid ChurchUpsertRequest` | `ChurchDetail` | `churchUpdate` |
| DELETE | `/v1/churches/{id}` | path id | `Void` | `churchDelete` |
| GET | `/v1/churches/denominations` | – | `List<CodeLabel>` (enum→라벨) | `churchDenominations` |
| POST | `/v1/churches/upload` | multipart file | `UploadResponse` | `churchUpload` |

**공개 (`/pub/v1/churches`, permitAll — PublicController와 동일 정책)** · 태그 `Public`
| 메서드 | 경로 | Param | 응답 | user-web `api.*` |
|---|---|---|---|---|
| GET | `/pub/v1/churches` | lat,lng,radiusKm,denomination,keyword,regionId,page | `ResInfinityList<ChurchNearbyItem>`(거리순) | `churchesNearby(p)` |
| GET | `/pub/v1/churches/{id}` | path id | `ChurchDetail`(예배시간 포함) | `church(id)` |

> 공개 검색은 GET 쿼리스트링(PublicController가 GET+`@RequestParam` 패턴). `lat/lng` 미제공 시 거리계산 생략하고 `regionId`/`denomination`만으로 정렬(createdAt desc) → "위치 거부" fallback과 정합.

### 3.3 핵심 로직 의사코드

**위치기반 검색(서비스, MyBatis 1차 bbox + 서비스단 Haversine 정밀정렬)**
```
nearby(req):
  useYn = "Y"; demoOnly publish
  if req.lat != null && req.lng != null && req.radiusKm != null:
     # 1) bbox 사전필터(인덱스 활용, 위경도 ± delta). 1km≈위도0.009도.
     latDelta = req.radiusKm / 111.0
     lngDelta = req.radiusKm / (111.0 * cos(toRadians(req.lat)))
     rows = churchMapper.selectInBox(minLat,maxLat,minLng,maxLng,
                                     denomination, keyword, regionId, useYn)  # LIMIT 넉넉히
     # 2) 정밀 거리 + 반경 컷 + 정렬(서비스단)
     for r in rows: r.distanceKm = Haversine(req.lat, req.lng, r.lat, r.lng)
     hits = rows.filter(distanceKm <= req.radiusKm).sortBy(distanceKm)
     page = paginate(hits, req.offset, req.size)   # 메모리 페이징(후보 수 적음)
     return ResInfinityList.of(page, hits.size, size)
  else:
     # 좌표 없음(거부 fallback): 거리 없이 지역/교단/키워드 필터 + createdAt desc + DB 페이징
     items = churchMapper.selectList(keyword, regionId, denom, useYn, offset, size)
     return ResInfinityList.of(items, churchMapper.countList(...), size)
```

**Haversine 유틸**
```
HaversineDistance.km(lat1,lng1,lat2,lng2):
  R=6371.0; dLat=rad(lat2-lat1); dLng=rad(lng2-lng1)
  a = sin(dLat/2)^2 + cos(rad(lat1))*cos(rad(lat2))*sin(dLng/2)^2
  return R * 2 * atan2(sqrt(a), sqrt(1-a))
```

**관리자 create/update (GoodsService 결 그대로)**
```
create(req):
  c = Church.builder()...build(); saved = churchRepository.save(c)
  replaceWorshipTimes(saved.id, req.worshipTimes)   # delete-then-reinsert
  actionLogPublisher.publish("CHURCH_CREATE","CHURCH", id, req.name)
  return getDetail(saved.id)
```

### 3.4 외부연동 어댑터 seam 설계 (지오코딩/좌표 보강)

목적: 시드는 좌표를 **이미 보유**하지만, "주소→좌표"가 필요한 실연동 지점(관리자 신규 등록시 주소만 입력)을 **막힘 없이 교체**하도록 인터페이스를 둔다. **기본 구현은 외부 호출 0회**.

```java
// base/external/geocode/GeocodeProvider.java
public interface GeocodeProvider {
    /** 주소 → 좌표. 외부키 없을 땐 데모용 결정론 좌표를 반환(demo=true). */
    GeocodeResult geocode(String address);
}
// record GeocodeResult(double latitude, double longitude, String source, boolean demo)
```
- `SeedGeocodeProvider`(기본, `@ConditionalOnProperty(name="church.geocode.provider", havingValue="seed", matchIfMissing=true)`):
  - 주소 문자열 **해시 → 서울/경기 bbox 내 결정론 좌표** 생성, `source="SEED"`, `demo=true`. **네트워크 호출 없음.**
- `KakaoGeocodeProvider`(**실키 주입 지점**, `havingValue="kakao"`): `@Value("${church.geocode.kakao-rest-key:}")`로 키 주입, `KakaoAK` 헤더로 Local API 호출 → `source="KAKAO"`, `demo=false`. **본 스펙에선 호출부 미구현 스텁**(키 주입 자리만; 활성화 금지).
- `application.yml`:
  ```yaml
  church:
    geocode:
      provider: ${CHURCH_GEOCODE_PROVIDER:seed}   # seed(기본) | kakao
      kakao-rest-key: ${KAKAO_REST_KEY:}          # 실키 주입 지점(비어있음)
  ```
> seam 패턴 근거: 기존 `StorageService`가 `@Value`로 endpoint를 바꿔 MinIO/S3를 교체하는 것과 동일 사상 — 호출부는 인터페이스만 의존.
> `ChurchDetail.demoData=true`(시드 `dataSource="SEED"`일 때) 플래그를 응답에 실어 프론트 배지를 켠다.

---

## 4. 프론트엔드

### 4.1 사용자 사이트 (streamhub-user-web — fetch + `lib/api.ts`, orval 아님)
**생성**
- `src/app/churches/page.tsx` — 교회찾기(좌측 필터 + 거리순 카드리스트 + 우측 지도)
- `src/app/churches/[id]/page.tsx` — 교회 상세(예배시간 테이블·연락처·길찾기·지도)
- `src/components/ChurchFinderView.tsx` — 클라이언트 컴포넌트(필터·Geolocation·RQ)
- `src/components/ChurchCard.tsx` — 거리 배지 + 교단/주소/전화
- `src/components/map/MapProvider.tsx` — **지도 추상화**(props: `center,markers,selectedId,onSelect`)
- `src/components/map/LeafletMap.tsx` — 기본 구현(react-leaflet + OSM 타일, **키 불필요**)
- `src/components/DemoBadge.tsx` — "데모 데이터" 배지(공용)
- `src/lib/geolocation.ts` — `getCurrentPosition()` Promise 래퍼 + 서울시청 fallback 상수
- `src/lib/churchTypes.ts` — `ChurchNearbyItem/ChurchDetail/WorshipTimeDto` 타입, `DENOMINATION_LABELS`, `FACILITY_LABELS`

**수정**
- `src/lib/api.ts` — `churchesNearby(p)`, `church(id)` 추가(아래)
- `src/lib/types.ts` — church 타입 export
- 사용자 사이트 상단 네비/메뉴에 "교회찾기" 추가(기존 영상/음악/게시글 옆)

`lib/api.ts` 추가:
```ts
export interface ChurchNearbyParams {
  lat?: number; lng?: number; radiusKm?: number;
  denomination?: string; keyword?: string; regionId?: number;
  pageNumber?: number; pageSize?: number;
}
export const api = {
  /* ...기존... */
  churchesNearby: (p: ChurchNearbyParams = {}) =>
    request<InfinityList<ChurchNearbyItem>>(`/pub/v1/churches${query({ ...p })}`),
  church: (id: number) => request<ChurchDetail>(`/pub/v1/churches/${id}`),
};
```

**교회찾기 화면 데이터/UX**
- mount 시 `lib/geolocation.getCurrentPosition()` 호출 → 성공: 좌표 사용 / **거부·실패: 서울시청(37.5663,126.9779) fallback** + "현위치 사용 안내" 토스트.
- 필터: 지역(select, regionId) · 교단(select, Denomination) · 검색(상품명 결 키워드) · 반경 슬라이더(기본 5km). `goods/page.tsx`의 draft→commit 패턴 차용.
- React Query: `queryKey:["churches-nearby", params]`, `queryFn: api.churchesNearby`, `placeholderData: keepPreviousData`.
- 결과: 좌측 거리순 카드(거리 km 배지), 카드 hover/click → 우측 `MapProvider` 마커 강조(`selectedId`). 지도 마커 클릭 → 카드 스크롤.
- 상단에 `<DemoBadge/>`("데모 데이터 · 실제 교회 정보 아님").

**MapProvider 추상화(키 불필요 → 추후 Kakao 교체)**
```tsx
// MapProvider.tsx — 구현 선택 단일 지점
export interface MapMarker { id:number; lat:number; lng:number; label:string; }
export interface MapViewProps {
  center:{lat:number;lng:number}; markers:MapMarker[];
  selectedId?:number; onSelect?(id:number):void;
}
// 기본: LeafletMap. 추후 KakaoMap.tsx로 교체(같은 props 계약).
export default function MapProvider(props: MapViewProps){ return <LeafletMap {...props}/>; }
```
- `LeafletMap.tsx`는 `next/dynamic`로 `ssr:false` 로드(goods 페이지의 AG Grid 동적로딩과 동일). OSM 타일 `https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png` (**키·계정 불필요**). attribution 표기 필수.

**상세 화면**
- `api.church(id)` → 예배시간 테이블(`kind/dayLabel/startTime/place/target`), 전화 `tel:` 링크, 시설 태그 배지(`FACILITY_LABELS`), 단일 마커 지도, **길찾기 링크**(키 불필요: `https://map.kakao.com/link/to/{name},{lat},{lng}` 또는 OSM `https://www.openstreetmap.org/directions?...` — 외부 SDK 미탑재, 단순 링크).
- `demoData` true면 `<DemoBadge/>`.

### 4.2 관리자 사이트 (streamhub-web — orval + RHF/Zod)
**생성**
- `src/app/(protected)/church/page.tsx` — 목록(필터: 지역·교단·노출 + 검색 + 페이지네이션) + "교회 등록"
- `src/app/(protected)/church/add/page.tsx` — 등록 폼
- `src/app/(protected)/church/[id]/page.tsx` — 수정 폼
- `src/components/church/ChurchForm.tsx` — RHF + Zod 공용 폼(예배시간 **동적 배열** `useFieldArray`, 좌표 입력, 교단 select, 시설 멀티)
- `src/components/church/ChurchTable.tsx` — 목록 테이블(AG Grid 또는 단순 테이블; 인라인 일괄수정은 불필요 → 단순 테이블 권장)
- `src/lib/denomination.ts` — `DENOMINATION_LABELS` 매핑

**수정**
- `src/components/layout/Sidebar.tsx` — `NAV_SECTIONS`에 신규 섹션/항목 추가:
  ```ts
  { title: "교회", items: [{ label: "교회관리", href: "/church", icon: Church }] } // lucide 'Church'
  ```
- `npm run gen` 후 자동생성 `src/apis/query/church/church.ts` 사용(직접 수정 금지). **gen 전 백엔드 배포 확인 필수**(프로젝트 규칙).

**관리자 데이터/폼**
- 목록: `churchList`(POST search) → `useQuery(["church-list", req])`, goods 페이지 패턴.
- 폼: Zod 스키마(name 필수, latitude/longitude 숫자·범위, denomination enum, phone 정규식, worshipTimes 배열). 제출 → `churchCreate`/`churchUpdate` mutation → 성공시 `/church` 이동.
- 썸네일: `churchUpload`(multipart) → key 저장(goods upload 패턴).

### 4.3 키 불필요 기술 선택(정직 표기)
| 용도 | 선택 | 키 필요 |
|---|---|---|
| 지도 렌더링 | **Leaflet + react-leaflet + OpenStreetMap 타일** | ❌ |
| 현위치 | 브라우저 **HTML5 Geolocation API** + 서울시청 fallback | ❌ |
| 길찾기 | 외부 지도 사이트 **딥링크(단순 URL)** | ❌(SDK 미탑재) |
| 좌표 데이터 | **시드(SeedGeocodeProvider)** | ❌ |
| (추후) 정밀 검색/지오코딩 | KakaoGeocodeProvider 스텁 → 실키 주입 | ✅(미사용) |
- 신규 npm 의존성(streamhub-user-web): `leaflet`, `react-leaflet`(+ `@types/leaflet`). admin-web은 신규 지도 의존성 없음(좌표 숫자 입력만).

---

## 5. 시드 (결정론적 · 마스킹 · 가상)

`DataInitializer.seedOrganization()` 확장(고정 시드 배열, idempotent: `churchRepository.count()` 가드 유지하되 5→40 확장 데이터로 교체). `WorshipTimeRepository` 주입 추가.

- **40곳**: 서울 28 + 경기 12. 각 행 = `{name, denomination, regionId, lat, lng, address, phone, pastorName, facilities}`의 **고정 배열**(랜덤 아님, 재기동 동일).
- **좌표**: 실제 **동 단위 근사 좌표**(예 강남역 37.4979/127.0276, 수원시청 37.2636/127.0286 등)에 `±0.002`도 결정론 오프셋(index 기반) — 실주소 핀포인트 회피.
- **상호(가상)**: `{지명}+{형용}+교회` 조합 — 예 "역삼은혜교회","수원소망제일교회". 실교회명 회피.
- **전화(마스킹)**: `02-***-{4자리}` / `031-***-{4자리}` (PortfolioSeeder의 `010-****-%04d` 마스킹 결).
- **담임목사(가상)**: `{성}{외자}목사` 결정론.
- **교단 분포**: METHODIST/PCK/HAPDONG/HOLINESS/GOSPEL/BAPTIST/ETC를 라운드로빈 + 가중(장로교 계열 다수) → 필터 데모가 비지 않게.
- **시설**: `facilities` CSV를 index%로 조합("주차,승강기,영유아실,카페" 등).
- **예배시간**: 교회당 3~4행(SUNDAY 11:00 본당 / DAWN 05:30 매일 / WEDNESDAY 19:30 / 일부 YOUTH 14:00) 결정론.
- **dataSource**: 전부 `"SEED"`, `useYn="Y"`, `openYn="Y"`(1~2곳 `useYn="N"`으로 노출제외 데모).
- **썸네일**: 기존 DataInitializer가 쓰는 **검증된 외부 이미지 URL**(Unsplash 교회/예배 사진) 재사용 — StorageService.publicUrl pass-through.
- **church id 1**: manager 계정 매핑 유지(첫 시드 교회).

---

## 6. 구현 순서 체크리스트

```
백엔드
- [ ] Denomination / WorshipKind enum, WorshipTime 엔티티 + repository
- [ ] Church.java 확장(컬럼/인덱스/builder/update/applyGeocode)
- [ ] ChurchMapper.java + ChurchMapper.xml(selectList/countList/selectInBox/selectDetail)
- [ ] dto: ChurchSearchRequest/ChurchNearbyRequest(record), ChurchListItem/ChurchNearbyItem/ChurchDetail/WorshipTimeDto/ChurchUpsertRequest/CodeLabel
- [ ] HaversineDistance 유틸 + 단위 테스트(테이블 기반: 알려진 두 좌표 거리)
- [ ] GeocodeProvider seam + SeedGeocodeProvider + KakaoGeocodeProvider 스텁 + yml 플래그
- [ ] ChurchService(list/detail/create/update/delete/nearby + replaceWorshipTimes + demoData 플래그)
- [ ] ChurchController(관리자) + PublicChurchController(공개)
- [ ] DataInitializer.seedOrganization 40곳 + 예배시간 확장, WorshipTimeRepository 주입
- [ ] 검증: ./mvnw test, swagger /v3/api-docs에 church/Public 엔드포인트 노출 확인

프론트(관리자)
- [ ] (백엔드 배포 확인 후) npm run gen → apis/query/church 생성
- [ ] Sidebar 네비 "교회관리" 추가
- [ ] church/page.tsx(목록·필터·페이지네이션), ChurchForm(RHF+Zod+동적 예배시간), add/[id]
- [ ] denomination 라벨 매핑

프론트(사용자)
- [ ] npm i leaflet react-leaflet @types/leaflet
- [ ] lib/api.ts churchesNearby/church, lib/geolocation, churchTypes
- [ ] MapProvider/LeafletMap(ssr:false), ChurchCard, DemoBadge
- [ ] churches/page.tsx(필터+거리순+지도), churches/[id]/page.tsx(예배시간·길찾기)
- [ ] 네비 "교회찾기" 추가

검증
- [ ] make/lint/test green, 위치 거부 fallback 동작, 데모배지 표시 확인
```

---

## 7. "데모/테스트 모드" 정직 표기 위치

| 위치 | 표기 |
|---|---|
| `Church.dataSource` 컬럼 | 시드=`"SEED"` (실연동시 `"KAKAO"`로 구분) |
| `ChurchDetail.demoData` 응답 필드 | `dataSource=="SEED"` → `true` |
| `GeocodeResult.demo` | SeedGeocodeProvider 결과 `true` |
| user-web `<DemoBadge/>` | 교회찾기/상세 상단 "데모 데이터 · 실제 교회 정보 아님" |
| `application.yml` 주석 | `church.geocode.provider=seed`가 외부 호출 없음을 명시, `kakao-rest-key` 빈 값 = 실키 주입 지점 |
| 시드 데이터 자체 | 상호/전화/담임목사 전부 가상·마스킹, 좌표는 동 단위 근사 |
| 스펙/리서치 문서 | "공식 교단 데이터 연동 불가 → 시드 근사"는 `expansion-research-and-plan.md` §6과 정합 |
