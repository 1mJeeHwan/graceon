# Spec — 음반 데이터 동기화 점검 & 운영 앨범 페이징 버그 수정

> 상태: **제안(검토 대기)** · 작성 2026-06-18 · 대상: `streamhub-user-web`(운영), `streamhub-api`(공개 API)
> 관련 코드: `streamhub-user-web/src/lib/albums.ts`, `.../components/AlbumListView.tsx`,
> `streamhub-api/.../v1/album/AlbumService.java`, `.../v1/pub/PublicController.java`

---

## 1. 음반 구매 페이지 ↔ 관리자 데이터 동기화 (점검 결과)

### 결론: **이미 단일 소스로 동기화되어 있음** (수정 불필요, 캐시만 보완 권장)

데이터 흐름을 추적한 결과:

```
[ALBUM 테이블 (MySQL/RDS)]
        │  albumMapper.selectList(keyword, genre, status, offset, size)
        ├──────────────► AlbumService.list()        ← 관리자 /v1/album  (status 무관 전체)
        └──────────────► AlbumService.listPublic()  ← 공개 /pub/v1/albums (status=ON_SALE 강제)
```

- 관리자(`list`)와 운영(`listPublic`)은 **동일한 MyBatis 매퍼·동일 테이블**을 읽는다. `listPublic`은 내부적으로 `list`를 호출하되 `status=ON_SALE`만 강제한다(`AlbumService.java:87`).
- 따라서 관리자에서 음반을 등록/수정/가격변경/상태변경하면 **그대로 운영에 반영된다.** 별도 복제/동기화 계층 없음 = 구조적으로 어긋날 수 없음.
- 가격(`price`)·재고(`stock`)는 앨범↔`GOODS_ITEM` 브리지를 통해 주문 도메인과도 일관 유지된다(기존 설계).

### 의도된 차이 (버그 아님)
| 항목 | 관리자 | 운영(공개) |
|---|---|---|
| `HIDDEN` 상태 음반 | 보임 | **숨김**(노출 차단, 상세 404) |
| 노출 정렬 | 최신순 | 최신순 동일 |

### 보완 권장 (선택)
- 운영의 React Query `staleTime`이 **30초**(`providers.tsx:14`)라, 관리자 변경이 운영 화면에 **최대 30초** 지연 반영된다(또는 새로고침/재진입 시 즉시). 데모로는 무해하나, "관리자 저장 즉시 반영" 체감을 높이려면 앨범 목록/상세 쿼리의 `staleTime`을 낮추거나(예: 5s) 상세 진입 시 `refetch`를 둘 수 있다. **필수 아님.**

---

## 2. 운영 앨범 페이지 페이징 버그 (원인 확정 · 수정안)

### 증상
운영 `/albums`에서 2페이지 이상으로 이동해도 **항상 1페이지(첫 12장)와 동일한 목록**이 표시됨.

### 근본 원인 — **쿼리 파라미터 이름 불일치**
- 백엔드 `GET /pub/v1/albums`는 `pageNumber`, `pageSize`를 받는다 (`PublicController.java:135`, `AlbumSearchRequest` 레코드 필드 = `pageNumber/pageSize`).
- 운영 프론트(`albums.ts`)는 **`page`, `size`** 라는 이름으로 전송한다:
  ```ts
  // albums.ts:117-120  (BUG)
  export interface AlbumListParams { genre?; keyword?; page?: number; size?: number; }
  list: (p) => request(`/pub/v1/albums${query({ ...p })}`)  // → ?page=1&size=12
  ```
- 백엔드는 `page/size`를 인식하지 못해 `pageNumber=null → offset 0`으로 처리 → **요청한 페이지와 무관하게 항상 첫 페이지** 반환. `totalPage`는 `totalCount/size`로 정상 계산되므로 UI엔 여러 페이지가 보이지만, 어느 페이지를 눌러도 내용이 같다.

### 검증 포인트 (회귀 방지)
- 같은 운영 코드의 **교회/콘텐츠/글 목록은 `pageNumber/pageSize`를 올바르게 사용**(`churches.ts:25`, `api.ts:81/87`) → 정상. **버그는 앨범 목록에 국한**.
- 관리자 앨범 목록은 orval 생성 클라이언트라 DTO 필드명(`pageNumber/pageSize`)을 그대로 보냄 → 정상.

### 수정안 (소규모 · 프론트 단독)
`streamhub-user-web/src/lib/albums.ts` 와 `AlbumListView.tsx`만 수정 (백엔드 무변경):

1. `AlbumListParams`의 `page/size` → **`pageNumber/pageSize`** 로 통일(다른 도메인과 일치).
2. `AlbumListView`에서 상태/전달 변수명을 맞춤(`page` 상태는 유지하되 요청 시 `pageNumber: page, pageSize: PAGE_SIZE`로 매핑).
3. (선택) `Pagination` 컴포넌트가 `totalPage<=1`이면 숨기도록 가드 확인.

**영향 파일**: 2개. **난이도**: 낮음. **리스크**: 낮음(파라미터명 정합만). **검증**: 운영 `/albums`에서 1→2페이지 이동 시 서로 다른 음반 표시 + `pageNumber=1&pageSize=12` 호출 확인(Playwright/네트워크).

---

## 작업 합산
| 항목 | 변경 | 난이도 | 비고 |
|---|---|---|---|
| #1 동기화 | (점검만) 0 / 캐시 보완은 선택 | – | 이미 동기화됨 |
| #2 페이징 | 프론트 2파일 | 낮음 | 파라미터명 정합 |
