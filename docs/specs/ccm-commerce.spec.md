# C3 CCM 음반 + 미리듣기 도메인 구현 스펙 (★차별화 핵심: 30초 HTML5 미리듣기)

> 확장 플랜 §3·§5(`docs/expansion-research-and-plan.md`)의 **C3 CCM 음반 커머스**를 구현한다.
> 기존 `goods`/`order` 도메인(`docs/specs/goods-order.spec.md`)과 `member`(Church/Region) 패턴을 최대 재활용하되,
> **앨범→트랙(1:N) + 트랙별 30초 미리듣기**라는 음반 고유 구조를 신규 엔티티로 추가한다.
> **사용자 결정 제약**: 외부 음원/음반 API 키 없이 **목업/시드**로 구현하되, 추후 실키 주입이 쉽도록 **어댑터 seam**(인터페이스 + Provider + `.env` 플래그)을 둔다. 실제 외부 호출은 하지 않으며, 모든 미리듣기 음원은 기존 SoundHelix 샘플(이미 `DataInitializer.SAMPLE_AUDIOS`에 존재)을 재활용한다. UI에 "데모/테스트" 배지를 정직 표기한다.

---

## 1. 목적/범위 + 기존 자산 재활용 전략

### 1.1 목적
CCM 음반 커머스를 ① 관리자(앨범/트랙 등록·관리), ② 사용자 사이트(앨범 커버 그리드 → 상세 트랙리스트 → **트랙별 30초 미리듣기** → 장바구니/구매 → 오프라인 매장찾기)로 제공한다. 포트폴리오 차별화 스토리는 **"레거시 IE/ActiveX 의존 없는 HTML5 `<audio>` 30초 즉시 미리듣기"**(두란노몰 대비)와 **단일 전역 오디오 인스턴스(한 곡만 재생)**다.

### 1.2 재활용/확장/신규 결정표

| 자산 | 처리 | 이유 |
|---|---|---|
| `v1/order` (ORDERS/ORDER_ITEM/ORDER_RECEIPT, 상태머신, 영수증) | **그대로 재사용** | 앨범 구매/배송/환불은 기존 주문 상태머신(`PLACED→PAID→…`)·재고차감·영수증 로직과 100% 동일. 신규 주문 도메인 불필요. |
| `v1/goods` (GOODS_ITEM/GOODS_CATEGORY, "음반" 카테고리 이미 시드됨) | **브리지 재사용** | 주문은 `ORDER_ITEM.goods_id → GOODS_ITEM`을 FK로 가짐. 앨범을 직접 ORDER에 물리면 order 도메인을 침범. → **앨범 1개당 GOODS_ITEM 1행을 "대표 판매상품"으로 브리지**(category="음반"), `ALBUM.goods_item_id`로 연결. 구매·재고·배송·영수증은 기존 order/goods 경로를 그대로 탐. (§2.6 상세) |
| **ALBUM / TRACK** (1:N) | **신규** | 앨범 메타(아티스트·발매일·레이블·장르) + 트랙(트랙번호·재생시간·`preview_url`)은 goods의 option/image로 표현 불가능한 고유 구조. 깔끔한 분리를 위해 신규 엔티티. |
| **STORE** (오프라인 직영매장) | **신규**(Region/Church 패턴 차용) | "매장찾기"는 C1 교회지도 컴포넌트 재활용 대상. 좌표(lat/lng) 보유 엔티티가 필요한데 Church에는 좌표 컬럼이 없음 → Church와 동일한 구조(`region_id`+name+좌표)의 STORE 신규. C1 지도 컴포넌트가 STORE/CHURCH 양쪽 마커를 동일 인터페이스로 받도록 설계. |
| `StorageService` (S3 업로드/publicUrl) | **그대로 재사용** | 앨범 커버 이미지 업로드. goods 썸네일과 동일(`storageService.upload(file, "album")`). |
| `ActionLogPublisher` | **그대로 재사용** | 앨범 CRUD 감사 로그(`ALBUM_CREATE` 등). |
| `streamhub-user-web/AudioPlayer.tsx` | **참고/확장** | 기존은 콘텐츠 음악용 풀 플레이어. 미리듣기는 **신규 전역 미니 플레이어**(`PreviewPlayerProvider`)로 분리(한 곡만 재생 보장). |
| `DataInitializer.SAMPLE_AUDIOS` (SoundHelix 8곡) | **그대로 재사용** | 트랙 `preview_url`로 결정론적 매핑. 신규 음원 호스트 없음. |
| `v1/pub/PublicController` (`/pub/v1/**`, permitAll) | **확장** | 사용자 사이트용 공개 앨범/매장 엔드포인트를 동일 컨트롤러 패턴으로 추가. |

> **핵심 판단**: 음반은 **신규 ALBUM/TRACK 엔티티**가 깔끔하다(goods 카테고리만으로는 트랙·미리듣기 표현 불가). 단, **커머스(구매/배송/재고/영수증)는 기존 order/goods를 흡수**한다 — 앨범↔GOODS_ITEM 브리지 1:1로 주문 도메인을 한 줄도 수정하지 않는다.

---

## 2. 데이터 모델

> 컨벤션(goods-order.spec.md §2와 동일): 테이블명 **대문자 SNAKE**, 컬럼 **소문자 snake**(`@Column(name=...)`), 인덱스 `idx_{테이블}_{용도}`, enum `@Enumerated(EnumType.STRING)`, PK `@GeneratedValue(IDENTITY)`, FK는 `Long ...Id`, `created_at`/`updated_at`는 엔티티가 소유(`@Builder` 생성자 `LocalDateTime.now()` 기본값), `@NoArgsConstructor(access = PROTECTED)`. `map-underscore-to-camel-case: true`라 MyBatis는 `AS snake_case` alias만 쓰면 자동 매핑.

### 2.1 ALBUM (음반)
| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| goods_item_id | BIGINT | nullable | FK→GOODS_ITEM(판매 브리지, §2.6). null이면 미판매(소개만) |
| title | VARCHAR(200) | not null | 앨범명 |
| artist | VARCHAR(120) | not null | 아티스트/팀명 |
| label | VARCHAR(120) | nullable | 레이블/기획사 |
| genre | ENUM(AlbumGenre) | not null, length 16 | 장르 |
| release_date | DATE | nullable | 발매일 |
| description | VARCHAR(2000) | nullable | 앨범 소개 |
| cover_key | VARCHAR(300) | nullable | 커버 이미지 스토리지 key |
| status | ENUM(AlbumStatus) | not null, length 10 | `ON_SALE`/`HIDDEN` |
| track_count | INT | not null, default 0 | 트랙수(서비스가 동기화) |
| view_count | BIGINT | not null, default 0 | 조회수 |
| source | ENUM(MusicSource) | not null, length 12 | `SEED`/`EXTERNAL` — 미리듣기 출처(어댑터 seam, §3.5) |
| external_id | VARCHAR(80) | nullable | 실 음원사 연동 시 외부 앨범 ID(현재 null) |
| created_at | DATETIME | not null | |
| updated_at | DATETIME | not null | |

인덱스: `idx_album_status`(status), `idx_album_genre`(genre), `idx_album_goods`(goods_item_id), `idx_album_release`(release_date), `idx_album_created`(created_at)

enum `AlbumGenre { WORSHIP, HYMN, GOSPEL, CCM, CAROL, INSTRUMENTAL, KIDS }` — `v1/album/entity/AlbumGenre.java`
enum `AlbumStatus { ON_SALE, HIDDEN }` — `v1/album/entity/AlbumStatus.java`
enum `MusicSource { SEED, EXTERNAL }` — `v1/album/entity/MusicSource.java`

### 2.2 TRACK (트랙, ALBUM 1:N)
| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| album_id | BIGINT | not null | FK→ALBUM |
| track_no | INT | not null | 트랙 번호(1부터) |
| title | VARCHAR(200) | not null | 곡명 |
| duration_sec | INT | nullable | 곡 전체 길이(초) |
| preview_url | VARCHAR(500) | nullable | **30초 미리듣기 음원 URL**(SoundHelix 샘플) |
| preview_start_sec | INT | not null, default 0 | 미리듣기 시작 오프셋(초) |
| preview_length_sec | INT | not null, default 30 | 미리듣기 길이(초, 기본 30) |
| source | ENUM(MusicSource) | not null, length 12 | `SEED`/`EXTERNAL` |
| external_id | VARCHAR(80) | nullable | 외부 트랙 ID(현재 null) |

인덱스: `idx_track_album`(album_id), `uk_track_album_no`(album_id, track_no) — `@Index(name=..., columnList="album_id,track_no", unique=true)`

> **미리듣기 30초 제어**: `preview_url`은 전체 곡이라도, 프론트 미니 플레이어가 `preview_start_sec`로 `audio.currentTime`을 세팅하고 `preview_length_sec` 경과 시 `pause()`(§4.4). DB는 "어디부터 몇 초"만 가짐 → 실 음원사 연동 시 `previewUrl`만 외부값으로 교체하면 됨.

### 2.3 STORE (오프라인 직영매장 — Church/Region 패턴 차용 + 좌표)
| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| region_id | BIGINT | not null | FK→REGION(기존 재사용) |
| name | VARCHAR(120) | not null | 매장명(예: "두란노 강남직영점" — 데모 가공) |
| address | VARCHAR(300) | nullable | 주소(마스킹/가상) |
| phone | VARCHAR(30) | nullable | 전화(데모 가상번호) |
| lat | DECIMAL(10,7) | nullable | 위도(WGS84) |
| lng | DECIMAL(10,7) | nullable | 경도(WGS84) |
| open_hours | VARCHAR(120) | nullable | 영업시간(예: "평일 10:00–20:00") |
| use_yn | VARCHAR(1) | not null, length 1 | "Y"/"N" |
| created_at | DATETIME | not null | |

인덱스: `idx_store_region`(region_id), `idx_store_use`(use_yn)

> 좌표는 **데모 가공값**(서울/수도권 가상 좌표). 실제 상호/주소/전화 미사용(리서치 §6 PII 가드). C1 지도 컴포넌트(Leaflet/OSM)가 STORE를 `{id,name,lat,lng,...}`로 받아 마커 렌더.

### 2.4 (재사용) GOODS_ITEM / GOODS_CATEGORY
변경 없음. "음반" 카테고리는 `PortfolioSeeder.CATEGORY_NAMES[0]="음반"`으로 이미 시드됨. 앨범 판매 브리지 행은 이 카테고리로 생성.

### 2.5 (재사용) ORDERS / ORDER_ITEM / ORDER_RECEIPT
변경 없음. 앨범 구매 = 기존 주문. `ORDER_ITEM.goods_id`는 앨범의 `goods_item_id`를 가리킴(스냅샷 `goods_name`에 앨범명 저장).

### 2.6 앨범↔GOODS_ITEM 브리지 (커머스 흡수 방식)
- 앨범 생성 시(판매 활성), 서비스가 GOODS_ITEM 1행을 자동 생성/동기화: `category=음반`, `name=앨범명`, `code=ALB{id}`, `price=앨범가`, `thumbnail_key=cover_key`. `ALBUM.goods_item_id`에 연결.
- **이유**: 주문/재고/배송/영수증/환불 전 로직이 기존 order·goods를 한 줄도 수정 없이 그대로 동작. 사용자 사이트 "장바구니/구매"는 이 GOODS_ITEM을 주문 라인으로 사용.
- 트랙·미리듣기·아티스트·발매일은 ALBUM/TRACK이 소유(goods로는 표현 불가한 부분만 신규).

---

## 3. 백엔드

패키지 루트: `org.streamhub.api.v1.album`, `org.streamhub.api.v1.store`. 관리자 컨트롤러는 `@RequestMapping("/v1/album")`·`@RequestMapping("/v1/store")` + `@PreAuthorize("hasAnyAuthority(T(...).SYSTEM, T(...).CHURCH_MANAGER)")`(GoodsController 패턴 그대로). 공개 조회는 기존 `PublicController`(`/pub/v1`) 확장. 응답은 전부 `ResultDTO<T>`. MyBatis XML은 `src/main/resources/mappers/`.

### 3.1 생성/수정 파일 목록 (정확한 경로)

**album 도메인** (`streamhub-api/src/main/java/org/streamhub/api/v1/album/`)
- `AlbumController.java` (관리자 CRUD)
- `AlbumService.java`
- `entity/Album.java`, `entity/Track.java`, `entity/AlbumGenre.java`, `entity/AlbumStatus.java`, `entity/MusicSource.java`
- `repository/AlbumRepository.java`, `repository/TrackRepository.java`
- `dto/AlbumSearchRequest.java`, `dto/AlbumListItem.java`, `dto/AlbumDetail.java`, `dto/AlbumCreateRequest.java`, `dto/TrackDto.java`, `dto/PreviewResponse.java`
- `mapper/AlbumMapper.java`
- `provider/MusicPreviewProvider.java` (**어댑터 seam 인터페이스**, §3.5)
- `provider/SeedMusicPreviewProvider.java` (**목업 구현 — 활성 기본값**)
- `provider/ExternalMusicPreviewProvider.java` (**실키 주입 지점 — 비활성, NotImplemented 스텁**)
- `provider/MusicProviderConfig.java` (`@ConfigurationProperties` + `@Bean` 플래그 선택)

**store 도메인** (`streamhub-api/src/main/java/org/streamhub/api/v1/store/`)
- `StoreController.java` (관리자), 공개는 PublicController
- `StoreService.java`
- `entity/Store.java`
- `repository/StoreRepository.java`
- `dto/StoreDto.java`, `dto/StoreSearchRequest.java`

**MyBatis XML** (`streamhub-api/src/main/resources/mappers/`)
- `AlbumMapper.xml`

**수정 파일**
- `v1/pub/PublicController.java` — 공개 앨범 목록/상세/미리듣기 + 매장목록 엔드포인트 추가
- `v1/pub/dto/` — 필요 시 공개 DTO 재사용(AlbumListItem/AlbumDetail/StoreDto 공유)
- `base/config/PortfolioSeeder.java` — `seedAlbums()` / `seedStores()` 추가 + repo 주입 + `run()` 등록(§5)
- `application.yml` — `app.music.*` 플래그(§3.5)

### 3.2 DTO 형태 (핵심)

```java
// AlbumSearchRequest (GoodsSearchRequest record 패턴)
public record AlbumSearchRequest(
        Integer pageNumber, Integer pageSize, String keyword,
        AlbumGenre genre, AlbumStatus status) {
    public int pageSizeOrDefault() { return pageSize == null || pageSize <= 0 ? 12 : pageSize; }
    public int offset() { int p = pageNumber == null || pageNumber < 0 ? 0 : pageNumber; return p * pageSizeOrDefault(); }
}

// AlbumCreateRequest (트랙 동적행 포함)
public record AlbumCreateRequest(
        @NotBlank String title,
        @NotBlank String artist,
        String label,
        @NotNull AlbumGenre genre,
        LocalDate releaseDate,
        String description,
        String coverKey,
        AlbumStatus status,
        @NotNull Long price,          // 판매가 → 브리지 GOODS_ITEM.price
        Integer stock,                // 재고 → 브리지 GOODS_ITEM.stock
        @Valid List<TrackDto> tracks) {}  // 동적행

// TrackDto (목록/상세/생성 공용; @Getter @Setter @NoArgsConstructor — MyBatis 매핑 대상)
//   trackNo, title, durationSec, previewUrl, previewStartSec, previewLengthSec
//   previewUrl은 응답에서 서비스가 SeedMusicPreviewProvider로 결정/검증(§3.5)

// StoreSearchRequest (공개 매장찾기: 위치 기반)
public record StoreSearchRequest(Long regionId, Double lat, Double lng) {}
//   lat/lng 주어지면 거리순 정렬(Haversine), 아니면 region/이름순
```

`AlbumListItem`(id·title·artist·genre·coverKey·coverUrl·trackCount·status·price·releaseDate), `AlbumDetail`(전 필드 + `List<TrackDto> tracks` + `coverUrl` + `goodsItemId` + `source`)는 GoodsListItem/GoodsDetail 패턴(`@Getter @Setter @NoArgsConstructor`, MyBatis 매핑, `coverUrl`/`tracks`는 서비스가 채움).

### 3.3 API 엔드포인트 표

| 메서드 | 경로 | 요청 DTO | 응답 DTO | 권한 |
|---|---|---|---|---|
| POST | `/v1/album/list` | `AlbumSearchRequest` | `ResInfinityList<AlbumListItem>` | SYSTEM·CHURCH_MANAGER |
| GET | `/v1/album/{id}` | — | `AlbumDetail` | 〃 |
| POST | `/v1/album` | `AlbumCreateRequest` | `AlbumDetail` | 〃 |
| PUT | `/v1/album/{id}` | `AlbumCreateRequest` | `AlbumDetail` | 〃 |
| DELETE | `/v1/album/{id}` | — | `Void` | 〃 |
| POST | `/v1/album/upload` | `MultipartFile file` | `UploadResponse`(goods 재사용) | 〃 |
| POST | `/v1/store/list` | `StoreSearchRequest` | `List<StoreDto>` | 〃 |
| POST/PUT/DELETE | `/v1/store`, `/v1/store/{id}` | `StoreDto` | `StoreDto`/`Void` | 〃 (관리자 매장 CRUD) |
| **GET** | **`/pub/v1/albums`** | `?genre&keyword&pageNumber&pageSize` | `ResInfinityList<AlbumListItem>` | **공개**(ON_SALE만) |
| **GET** | **`/pub/v1/albums/{id}`** | — | `AlbumDetail`(트랙+미리듣기 포함) | **공개**(조회수+1) |
| **GET** | **`/pub/v1/albums/{albumId}/tracks/{trackId}/preview`** | — | `PreviewResponse`(`previewUrl,startSec,lengthSec,demo:true`) | **공개** |
| **GET** | **`/pub/v1/stores`** | `?lat&lng&regionId` | `List<StoreDto>`(거리순) | **공개** |

- 업로드: `storageService.upload(file, "album")` → `new UploadResponse(key, storageService.publicUrl(key))`(GoodsController.upload 그대로).
- 공개 상세/미리듣기는 PublicController에 추가(`/pub/**` permitAll, SecurityConfig 기존 설정).

### 3.4 핵심 로직 의사코드

**(A) 앨범 생성/수정 + 브리지 + 트랙 동기화 — `AlbumService.create/update` (`@Transactional`)**
```
create(req):
    album = Album.builder()
        .title(req.title).artist(req.artist).label(req.label).genre(req.genre)
        .releaseDate(req.releaseDate).description(req.description).coverKey(req.coverKey)
        .status(req.status ?: ON_SALE).source(SEED).build()
    saved = albumRepository.save(album)
    syncGoodsBridge(saved, req.price, req.stock)   // (B)
    replaceTracks(saved.id, req.tracks)            // (C): delete-then-reinsert, trackNo 1..N
    saved.syncTrackCount(trackCount)               // 엔티티 메서드: track_count 갱신
    albumRepository.saveAndFlush(saved)
    actionLogPublisher.publish("ALBUM_CREATE", "ALBUM", str(saved.id), req.title)
    return getDetail(saved.id)
```

**(B) 커머스 브리지 — `AlbumService.syncGoodsBridge`** (order 도메인 비침범의 핵심)
```
syncGoodsBridge(album, price, stock):
    if album.goodsItemId == null:
        gi = goodsItemRepository.save(GoodsItem.builder()
            .categoryId(eumbanCategoryId())   // GOODS_CATEGORY name="음반" 조회(캐시)
            .name(album.title).code("ALB"+album.id)
            .price(price).stock(stock ?: 0).notiQty(5)
            .soldOut("N").useYn("Y").status(SELLING)
            .thumbnailKey(album.coverKey).build())
        album.linkGoodsItem(gi.id)
    else:
        gi = goodsItemRepository.findById(album.goodsItemId)
        gi.update(... name=album.title, price=price, thumbnailKey=album.coverKey ...) // 기존 GoodsItem.update 재사용
```
> 재고 차감/복원·주문·영수증은 **전부 기존 `OrderService.changeStatus`가 처리**(이 스펙은 한 줄도 안 건드림). 앨범은 GOODS_ITEM을 통해 주문 흐름에 "참여"만 한다.

**(C) 트랙 + 미리듣기 결정 — `AlbumService.replaceTracks`**
```
replaceTracks(albumId, tracks):
    trackRepository.deleteByAlbumId(albumId)
    no = 1
    for t in tracks (skip blank title):
        previewUrl = musicPreviewProvider.resolvePreviewUrl(albumId, no, t.previewUrl) // (§3.5 seam)
        trackRepository.save(Track.builder()
            .albumId(albumId).trackNo(no++).title(t.title).durationSec(t.durationSec)
            .previewUrl(previewUrl)
            .previewStartSec(t.previewStartSec ?: 0)
            .previewLengthSec(t.previewLengthSec ?: 30)
            .source(SEED).build())
```

**(D) 공개 미리듣기 — `PublicController` → `AlbumService.getPreview`**
```
getPreview(albumId, trackId):
    track = trackRepository.findByIdAndAlbumId(trackId, albumId) ?: throw NOT_FOUND
    url = musicPreviewProvider.resolvePreviewUrl(albumId, track.trackNo, track.previewUrl)
    return new PreviewResponse(url, track.previewStartSec, track.previewLengthSec, /*demo*/ true)
```
> `demo:true` 플래그는 프론트 "데모 미리듣기" 배지 노출 근거(§7).

**(E) 공개 매장 거리순 — `StoreService.listPublic`**
```
listPublic(req):
    stores = storeRepository.findByUseYn("Y")   // 데모 규모 작아 전량 로드
    dtos = stores.map(StoreDto::from)
    if req.lat != null && req.lng != null:
        dtos.sort(by haversine(req.lat, req.lng, s.lat, s.lng))   // 거리 km도 dto.distanceKm에 채움
    else if req.regionId != null:
        dtos = dtos.filter(regionId == req.regionId)
    return dtos
```
Haversine은 `StoreService` private 헬퍼(외부 라이브러리 불필요).

### 3.5 외부연동 어댑터 seam 설계 (★실키 주입 쉬운 구조)

**인터페이스** `provider/MusicPreviewProvider.java`
```java
/** 트랙 미리듣기 음원 URL을 해결한다. 구현 교체로 시드↔실 음원사 전환. */
public interface MusicPreviewProvider {
    /**
     * @param albumId 앨범 id, @param trackNo 트랙 번호(1부터),
     * @param storedUrl DB에 저장된 previewUrl(없을 수 있음)
     * @return 재생 가능한 미리듣기 URL
     */
    String resolvePreviewUrl(Long albumId, int trackNo, String storedUrl);
    /** UI 배지용: 실제 외부 음원이 아니라 데모 샘플이면 true. */
    boolean isDemo();
}
```

**목업 구현(기본 활성)** `SeedMusicPreviewProvider`
```java
@Component
@ConditionalOnProperty(name = "app.music.provider", havingValue = "seed", matchIfMissing = true)
public class SeedMusicPreviewProvider implements MusicPreviewProvider {
    // DataInitializer.SAMPLE_AUDIOS와 동일한 SoundHelix 8곡(상수 공유 또는 복제)
    public String resolvePreviewUrl(Long albumId, int trackNo, String storedUrl) {
        if (StringUtils.hasText(storedUrl)) return storedUrl;        // 시드가 이미 박아둔 값 우선
        int idx = (int) ((albumId * 7 + trackNo) % SAMPLES.length);  // 결정론적 매핑
        return SAMPLES[idx];
    }
    public boolean isDemo() { return true; }
}
```

**실키 주입 지점(비활성 스텁)** `ExternalMusicPreviewProvider`
```java
@Component
@ConditionalOnProperty(name = "app.music.provider", havingValue = "external")
public class ExternalMusicPreviewProvider implements MusicPreviewProvider {
    private final String apiKey;   // @Value("${app.music.external.api-key:}")
    private final String baseUrl;  // @Value("${app.music.external.base-url:}")
    // 추후 멜론/벅스/Spotify Preview 등 실 연동 지점. 현재는:
    public String resolvePreviewUrl(Long albumId, int trackNo, String storedUrl) {
        throw new ApiException(ResultCode.NOT_IMPLEMENTED, "외부 음원 연동 미구현(데모 모드 사용)");
    }
    public boolean isDemo() { return false; }
}
```

**`application.yml` 플래그**
```yaml
app:
  music:
    provider: ${MUSIC_PROVIDER:seed}        # seed(기본) | external
    external:
      api-key: ${MUSIC_API_KEY:}            # 실키 주입 시에만 채움
      base-url: ${MUSIC_BASE_URL:}
```
> **전환 시나리오**: `.env`에 `MUSIC_PROVIDER=external` + `MUSIC_API_KEY=...` 주입 → `ExternalMusicPreviewProvider`가 활성화(현재는 실제 호출부만 채우면 됨). 그 외 모든 코드(엔티티·DTO·컨트롤러·프론트)는 무변경. `PreviewResponse.demo`는 `provider.isDemo()`를 반영해 자동으로 배지가 사라짐.

---

## 4. 프론트

> 음반 커머스는 **사용자 사이트(`streamhub-user-web`)**가 주 무대(커버그리드·상세·미리듣기·매장찾기), **관리자(`streamhub-web`)**는 앨범 등록/관리만. 사용자 사이트는 손코딩 타입(`lib/types.ts`)+`lib/api.ts`+React Query(`lib/queries.ts`) 패턴(Orval 아님 — 공개 표면이 작아 수동 동기화). 관리자 사이트는 Orval(`npm run gen`)·AG Grid·RHF+Zod 패턴.

### 4.1 생성/수정 파일 목록 (정확한 경로)

**사용자 사이트 (`streamhub-user-web/src/`)** — 미리듣기·매장찾기·구매 UX 주역
- `app/albums/page.tsx` — 앨범 목록(커버 그리드)
- `app/albums/[id]/page.tsx` — 앨범 상세(트랙리스트 + 재생버튼 + 장바구니)
- `app/stores/page.tsx` — 오프라인 매장찾기(지도 + 거리순 리스트)
- `components/AlbumCard.tsx` — 커버 카드(ContentCard 패턴)
- `components/AlbumGrid.tsx` — 반응형 커버 그리드(ContentGrid 패턴)
- `components/TrackRow.tsx` — 트랙 1행(번호·곡명·길이·▶미리듣기 버튼)
- `components/preview/PreviewPlayerProvider.tsx` — **전역 단일 오디오 컨텍스트**(한 곡만 재생, §4.4)
- `components/preview/MiniPreviewPlayer.tsx` — **하단 고정 미니 플레이어**(현재곡·진행바·30초 카운트·데모 배지)
- `components/store/StoreMap.tsx` — **Leaflet/OSM 지도**(키 불필요, §4.5), STORE 마커
- `components/store/StoreList.tsx` — 거리순 매장 리스트(전화 CTA)
- `components/DemoBadge.tsx` — "데모 미리듣기" 배지(관리자 TestModeBadge 미러)
- `lib/types.ts` **수정** — `AlbumListItem`·`AlbumDetail`·`TrackDto`·`StoreDto`·`PreviewResponse` 추가
- `lib/api.ts` **수정** — `albums`/`album`/`preview`/`stores` 메서드 추가
- `lib/queries.ts` **수정** — `useAlbums`/`useAlbum`/`useStores` 훅 추가
- `components/AppBar.tsx` 또는 `TabBar.tsx` **수정** — "음반"/"매장" 진입 추가
- `app/providers.tsx` **수정** — `<PreviewPlayerProvider>`로 트리 래핑(전역 1개)
- `app/layout.tsx` **수정** — Leaflet CSS `<link>` 추가(또는 dynamic import)

**관리자 사이트 (`streamhub-web/src/`)** — 앨범 관리
- `app/(protected)/album/page.tsx` — 앨범 목록(AG Grid)
- `app/(protected)/album/add/page.tsx` — 앨범 등록(트랙 동적행 + 커버 업로드)
- `app/(protected)/album/[id]/page.tsx` — 앨범 상세/수정
- `components/album/AlbumGrid.tsx` — AG Grid(`ColDef<AlbumListItem>[]`)
- `components/album/AlbumStatusBadge.tsx` — ON_SALE/HIDDEN 배지
- `components/album/TrackRows.tsx` — 트랙 동적행(`useFieldArray`)
- `components/album/GenreBadge.tsx` — 장르 배지
- `lib/album-form.ts` — Zod 스키마 + 트랙 파싱 헬퍼(goods-form.ts 패턴)
- `components/layout/Sidebar.tsx` **수정** — "커머스" 섹션에 "음반관리"·"매장관리" 추가
- (Orval 생성) `apis/query/album/album.ts`, `apis/query/store/store.ts`, `streamHubAdminAPI.schemas.ts` 갱신

### 4.2 화면별 컴포넌트·데이터패칭·폼

**[사용자] 앨범 목록 `app/albums/page.tsx`**
- 데이터: `useAlbums({genre, keyword, pageNumber})` → `api.albums(...)`(`useContents` 패턴, `placeholderData:(prev)=>prev`).
- 레이아웃: `AlbumGrid`(2~4열 반응형 커버), 상단 장르 필터칩(`FilterChips` 차용)·검색바(`SearchBar` 재사용).
- 카드 클릭 → `/albums/[id]`.

**[사용자] 앨범 상세 `app/albums/[id]/page.tsx`**
- 데이터: `useAlbum(id)` → `api.album(id)`(`useContent` 패턴, `enabled: id>0`).
- 영역: 커버 + 메타(아티스트·발매일·레이블·장르) + 설명, **트랙리스트**(`TrackRow[]`), "장바구니 담기"(브리지 GOODS_ITEM 기준)·"구매" 버튼, 하단 **"이 음반 판매 매장 보기"** → `/stores`.
- 각 `TrackRow`의 ▶ 버튼 → `PreviewPlayerProvider.play(track)` 호출(전역 미니 플레이어가 받아 재생).

**[사용자] 매장찾기 `app/stores/page.tsx`**
- 데이터: `useStores({lat, lng})` — 브라우저 `navigator.geolocation`로 좌표 획득(거부 시 기본 좌표=서울시청, 리서치 §C1 fallback 정신). `StoreMap`(Leaflet/OSM) + `StoreList`(거리순, 전화 CTA `tel:`).
- 지도/리스트 동일 데이터 바인딩, 마커 클릭 ↔ 리스트 하이라이트.

**[관리자] 앨범 목록 `app/(protected)/album/page.tsx`**
- `useAlbumList`(Orval) + `keepPreviousData`(content/goods 패턴). 필터: 검색어(앨범/아티스트)·장르 select·상태(ON_SALE/HIDDEN/ALL). 우상단 `<Link href="/album/add">`.
- `AlbumGrid` 컬럼: 커버(img 40px) / 앨범명 / 아티스트 / 장르(GenreBadge) / 트랙수 / 판매가(toLocaleString) / 상태(AlbumStatusBadge) / 발매일 / 상세버튼.

**[관리자] 앨범 등록/수정 `album/add`·`album/[id]`**
- Zod(`lib/album-form.ts`): `title`/`artist`(min1), `genre` enum, `price`(coerce ≥0), `releaseDate` optional date, `status` enum, `tracks: z.array({trackNo?, title(min1), durationSec?, previewUrl?, previewStartSec?, previewLengthSec?})`.
- 트랙 동적행: `useFieldArray`(OptionRows.tsx 패턴) add/remove, 드래그 없이 trackNo 자동 부여. 커버는 `ThumbnailUpload`(content 재사용) → `coverKey`.
- 제출: `useAlbumCreate().mutate({data})` → `router.push('/album/'+id)`. 수정은 `useAlbumDetail(id)` → `reset(buildDefaults)`.

### 4.3 사이드바/네비 추가
**관리자 `components/layout/Sidebar.tsx`** — "커머스" 섹션(이미 굿즈/주문 존재)에 추가:
```ts
import { Disc3, MapPin } from "lucide-react";
// "커머스" items에:
{ label: "음반관리", href: "/album", icon: Disc3 },
{ label: "매장관리", href: "/store", icon: MapPin },
```
**사용자 `components/TabBar.tsx`/`AppBar.tsx`** — "음반"(/albums)·"매장"(/stores) 진입 추가(기존 영상/음악/게시글 탭 옆).

### 4.4 전역 단일 오디오 미리듣기 (★한 곡만 재생)
`PreviewPlayerProvider.tsx` (React Context + `useRef<HTMLAudioElement>` **앱당 1개**):
```
상태: current(track+albumId), isPlaying, elapsed
play(track):
    audio.src = track.previewUrl
    audio.currentTime = track.previewStartSec ?? 0
    audio.play()
    // 30초 컷오프: timeupdate에서 (currentTime - startSec) >= previewLengthSec(기본30) 이면 audio.pause()
    setCurrent(track)
stop(): audio.pause(); setCurrent(null)
```
- 단일 인스턴스이므로 새 트랙 `play()` 시 자동으로 이전 곡 교체(다중 재생 불가 — 요구사항 충족).
- `MiniPreviewPlayer`는 `current != null`일 때만 하단 고정 표시: 커버 썸네일·곡명·진행바(elapsed/30)·일시정지·닫기 + **`DemoBadge`("데모 미리듣기 · 30초")**.
- 기존 `AudioPlayer.tsx`(콘텐츠 풀 플레이어)와 분리 — 미리듣기는 30초 컷·전역 1곡 제약이 다름.

### 4.5 키 불필요 기술 선택 (외부 키 0개)
| 용도 | 선택 | 키 필요? | 비고 |
|---|---|---|---|
| 미리듣기 재생 | **HTML5 `<audio>`**(네이티브) | ✕ | 기존 `AudioPlayer` 검증된 방식. SoundHelix mp3. |
| 매장 지도 | **Leaflet + OpenStreetMap 타일** | ✕ | Kakao/Google 키 불필요. `react-leaflet` + OSM 타일 URL. (리서치 C1은 Kakao 권장이나 **키 없는 데모 우선** 결정 → OSM, 추후 Kakao 교체 가능) |
| 현위치 | **`navigator.geolocation`** | ✕ | 거부 시 기본 좌표(서울시청) fallback. |
| 주소 검색(관리자 매장 등록) | **다음(카카오) 우편번호 위젯**(`postcode.v2.js`) | ✕(무료 스크립트) | C2 패턴 재사용. 선택 기능. |
> 지도 라이브러리만 신규 의존성(`leaflet`, `react-leaflet`). 그 외 전부 브라우저 내장.

---

## 5. 시드 (`base/config/PortfolioSeeder.java` 확장 — 결정론·마스킹·가상)

`run()` 끝에 `seedAlbums(now)` → `seedStores(now)` 추가. 신규 repo 4개(`AlbumRepository`, `TrackRepository`, `StoreRepository` + 기존 `GoodsCategoryRepository`/`GoodsItemRepository` 재사용)를 생성자 주입. 각 메서드 `if (albumRepository.count() > 0) return;` 멱등 가드. **`Random` 대신 고정 패턴(`i % n`, 소수 배수)** 또는 기존 `new Random(SEED_*)` 패턴(파일이 이미 사용 중) — 결정론 보장.

- **상수**: `SAMPLE_AUDIOS`(SoundHelix 8곡 — DataInitializer와 동일값 복제 또는 공통 상수 추출), `ALBUM_TITLES`/`ARTISTS`(가상 CCM 팀명, 예: "은혜워십"·"새벽기도밴드" — 실명 미사용), `AlbumGenre[]` 순환.
- **seedAlbums()**: 앨범 약 **24장**. 각 앨범:
  - `genre = AlbumGenre.values()[i % 7]`, `artist = ARTISTS[i % ...]`, `releaseDate = now.minusDays((i*23)%900)`.
  - **커버**: 기존 시드의 license-safe Unsplash(워십/음반) URL 패턴 또는 Picsum `seed` URL 재사용(PortfolioSeeder가 이미 사용) — 신규 호스트 없음.
  - **브리지 GOODS_ITEM**: `seedGoods`가 만든 "음반" 카테고리 행 중 일부에 1:1 매핑하거나, 앨범 전용 GOODS_ITEM을 `code="ALB%03d"`로 생성(price 9,000~22,000, 1,000 단위). `ALBUM.goods_item_id` 연결.
  - **트랙**: 앨범당 **5~10곡**(`5 + i%6`). `track_no=1..N`, `preview_url = SAMPLE_AUDIOS[(albumId*7 + trackNo) % 8]`(결정론), `preview_start_sec = (trackNo*15) % 60`, `preview_length_sec = 30`, `duration_sec = 180 + (trackNo*20)%120`. `source=SEED`.
  - 일부(`i%9==0`) `status=HIDDEN`(미노출 데모).
- **seedStores()**: 매장 약 **8개**(수도권 가상). `region_id`는 기존 REGION에서 결정론 선택. **좌표는 서울/경기 가상 좌표 고정 배열**(예: `{37.566,126.978}` 등 — 실제 상호 좌표 아님). `name`은 가상("○○직영점"), `phone`은 `02-0000-00NN`(데모 가상), `address`는 가공. `use_yn="Y"`(1개는 "N" 비노출).
- **정직성**: 모든 매장/앨범은 데모 가공값(실 상호·실 전화·실 음원사 데이터 아님). `source=SEED`로 마킹되어 프론트 `demo` 배지 근거가 됨.

---

## 6. 구현 순서 체크리스트 (의존 순서)

**백엔드**
1. [ ] `album/entity/*` (AlbumGenre/AlbumStatus/MusicSource → Album(linkGoodsItem/syncTrackCount/update) → Track)
2. [ ] `store/entity/Store.java`
3. [ ] `album/repository/*`(AlbumRepository, TrackRepository: `findByAlbumIdOrderByTrackNo`/`deleteByAlbumId`/`findByIdAndAlbumId`), `store/repository/StoreRepository`(`findByUseYn`)
4. [ ] `album/provider/*` — `MusicPreviewProvider` 인터페이스 + `SeedMusicPreviewProvider`(@ConditionalOnProperty seed, matchIfMissing) + `ExternalMusicPreviewProvider`(external 스텁)
5. [ ] `album/dto/*`, `store/dto/*` (record 검색 DTO + @Getter/@Setter 매핑 DTO)
6. [ ] `album/mapper/AlbumMapper.java` + `resources/mappers/AlbumMapper.xml`(GoodsMapper.xml 패턴: 동적 where genre/status/keyword, count, detail, 트랙은 서비스 로드)
7. [ ] `AlbumService`(list/detail/create/update/delete/getPreview + syncGoodsBridge/replaceTracks), `StoreService`(listPublic/CRUD/haversine) — `StorageService`·`ActionLogPublisher`·`MusicPreviewProvider` 주입
8. [ ] `AlbumController`, `StoreController`(`@PreAuthorize`, `ResultDTO`)
9. [ ] `PublicController` 확장: `/pub/v1/albums`, `/albums/{id}`, `/albums/{albumId}/tracks/{trackId}/preview`, `/stores`
10. [ ] `application.yml` `app.music.*` 플래그
11. [ ] `PortfolioSeeder` 수정: repo 주입 + `seedAlbums()`/`seedStores()` + `run()` 등록
12. [ ] `./mvnw test` → `./mvnw clean package` 그린, Swagger `/v3/api-docs`에 Album/Store 노출 확인

**프론트** (백엔드 배포 후)
13. [ ] (관리자) `npm run gen`(Orval) — `apis/query/album/`, `apis/query/store/`, schemas
14. [ ] (관리자) `lib/album-form.ts`, `components/album/{AlbumStatusBadge,GenreBadge,AlbumGrid,TrackRows}.tsx`
15. [ ] (관리자) `app/(protected)/album/{page,add/page,[id]/page}.tsx`, Sidebar 메뉴 2개
16. [ ] (사용자) `lib/types.ts`/`api.ts`/`queries.ts` 수정(Album/Track/Store/Preview)
17. [ ] (사용자) `components/preview/{PreviewPlayerProvider,MiniPreviewPlayer}.tsx`, `DemoBadge.tsx`, `providers.tsx` 래핑
18. [ ] (사용자) `components/{AlbumCard,AlbumGrid,TrackRow}.tsx`, `app/albums/{page,[id]/page}.tsx`
19. [ ] (사용자) `leaflet`/`react-leaflet` 설치, `components/store/{StoreMap,StoreList}.tsx`, `app/stores/page.tsx`, `layout.tsx` Leaflet CSS, TabBar/AppBar 진입
20. [ ] (관리자) `npm run build:no-lint` · (사용자) `npm run build` 그린 확인

---

## 7. "데모/테스트 모드" 정직 표기 위치

| 위치 | 표기 | 근거 |
|---|---|---|
| `PreviewResponse.demo` (백엔드) | `true`(SeedMusicPreviewProvider.isDemo) | 미리듣기가 실 음원사 아닌 SoundHelix 데모 샘플임을 API가 명시 |
| `ALBUM.source` / `TRACK.source` = `SEED` | DB 마킹 | 시드/외부 구분. external 전환 시 자동 변경 |
| 사용자 `MiniPreviewPlayer` | **`DemoBadge`**: "데모 미리듣기 · 30초" | 재생 중 항상 노출 |
| 사용자 `app/albums/[id]` 상단 | "샘플 음원(SoundHelix)으로 재생되는 데모입니다" 안내 | 미리듣기 클릭 전 고지 |
| 사용자 `app/stores` 상단 | "데모용 가상 매장 정보입니다(실제 상호·연락처 아님)" | STORE 가공값 정직 표기(리서치 §6 PII 가드) |
| 관리자 앨범 등록폼 미리듣기 필드 | "미리듣기 음원은 데모 샘플로 자동 매핑됩니다(외부 음원사 미연동)" | 어댑터 seed 모드 안내 |
| (구매 플로우) | 기존 order 도메인의 `OrderStatusStepper` 결제 시뮬 가드 그대로 | 실 PG 미연동(기존 §데모 안전 상속) |

> 어댑터 전환(`MUSIC_PROVIDER=external` + 실키) 시 `isDemo()=false`가 되어 `demo` 배지/고지가 **코드 수정 없이 자동 소거**된다. 이것이 seam 설계의 정직성·확장성 핵심.

---

## 8. 위험/주의
- **order 도메인 비침범**: 앨범 커머스는 반드시 GOODS_ITEM 브리지를 경유. ORDERS/ORDER_ITEM/OrderService를 직접 수정하지 말 것(상태머신·재고·영수증 단일 진실 유지).
- **브리지 정합성**: 앨범 삭제 시 연결된 GOODS_ITEM 처리 정책 명시 — 미판매 전환(`useYn="N"`) 권장(과거 주문의 `goods_id` 스냅샷 무결성 보호, 물리삭제 금지).
- **미리듣기 30초 컷오프**: `timeupdate` 리스너 누수 방지 — `play()` 시 이전 리스너 해제, 단일 audio 인스턴스 재사용. 모바일 자동재생 정책상 사용자 제스처(▶ 클릭) 내에서만 `play()`.
- **Leaflet SSR**: Next.js에서 `react-leaflet`은 `dynamic(() => ..., { ssr:false })`로 클라이언트 전용 로드(window 참조).
- **`npm run gen` 전 배포 확인**(CLAUDE.md 규칙): 관리자 Orval은 백엔드 배포 후에만. 사용자 사이트는 수동 타입이라 무관.
- **5개 이상 파일 변경 사전보고**(CLAUDE.md): 단계(엔티티→provider→매퍼→서비스→컨트롤러→시드, 이후 관리자 프론트, 사용자 프론트)별 진행·검증.
- **시드 결정론**: 고정 패턴/고정 시드만. 미리듣기 매핑·좌표·트랙수 모두 재현 가능해야 데모 리셋 시 동일.
- **좌표 PII**: STORE 좌표/주소/전화는 전부 가상. 실제 직영점 데이터 사용 금지(리서치 §6).
