# Spec — 관리자에서 운영(user-web) UI를 유동적으로 개선하는 기능

> 상태: **제안(검토 대기 · 최종 결정 필요)** · 작성 2026-06-18
> 대상: `streamhub-api`(신규 SiteConfig + 배너 공개화), `streamhub-web`(관리자 설정 화면),
> `streamhub-user-web`(운영 — 설정 소비)
> 기반: 기존 `banner` 도메인(BANNER 테이블 + 관리자 CRUD 화면)은 이미 존재하나 **운영에서 미소비** 상태.

---

## 1. 목표
관리자가 **코드 수정·재배포 없이** 운영 사이트의 UI를 바꿀 수 있게 한다. "유동적 UI 개선"을
포트폴리오에서 보여줄 수 있는, **현실적이고 임팩트 있는 범위**로 정의한다.

## 2. 무엇을 유동적으로 바꿀 수 있게 할 것인가 (제안 범위)

### A. 사이트 설정(SiteConfig) — 단일 설정 레코드
| 설정 | 운영 반영 | 비고 |
|---|---|---|
| 기본 테마(light/dark) | 첫 방문자 기본 테마 | 사용자가 토글로 바꾼 선택(localStorage)이 **우선** |
| 액센트 색(primary) | 버튼·강조·가격색 등 전체 | CSS 변수 `--primary` 동적 주입 |
| 공지바(announcement) | 상단 띠 배너(텍스트+링크) on/off | 점검 안내·이벤트 고지 등 |
| 홈 섹션 노출/순서 | 예배실황·최신영상·CCM음반·내주변교회 | 토글 + 드래그 정렬 |
| 추천 음반(featured) | 홈 "추천 음반" 행 고정 노출 | 앨범 id 다중 선택 |

### B. 배너 운영 연동 (기존 자산 활용)
- 이미 만든 **관리자 배너 CRUD**(위치/디바이스/기간/정렬)를 운영에 실제로 노출.
- 신규 공개 엔드포인트 `GET /pub/v1/banners?position=MAIN_TOP`(노출기간·useYn 필터) → user-web 홈 상단/중간 배너 렌더.
- 이로써 "관리자에서 배너 올리면 운영 홈이 바뀐다"는 **즉시 시연 가능한 유동 UI**가 완성됨.

### C. (범위 외 권장) 풀 드래그앤드롭 페이지 빌더
- 블록 단위 자유 편집기. **비권장** — 범위·리스크가 과도하고 데모 가치 대비 비효율. 본 스펙에서 제외.

---

## 3. 아키텍처 / 데이터 흐름

```
[관리자 /site-config 화면] --PUT /v1/site-config--> [SITE_CONFIG (단일행, JSON 컬럼)]
[관리자 /banners 화면(기존)] --CRUD /v1/banner------> [BANNER]
                                                          │
                          GET /pub/v1/site-config  ◄──────┤  (공개, 인증 불요)
                          GET /pub/v1/banners      ◄──────┘
                                   │
                                   ▼
                   [user-web layout(SSR fetch) + home]
                     - 공지바/기본테마/accent 주입
                     - 홈 섹션 순서·토글, 추천음반 행, 배너 렌더
```

### 데이터 모델 (제안)
- `SITE_CONFIG` : `id(고정 1)`, `data(JSON/TEXT)`, `updatedAt`. `data` 예:
  ```json
  {
    "defaultTheme": "dark",
    "accentColor": "#40C1DF",
    "announcement": { "enabled": true, "text": "성탄 특별예배 안내", "link": "/churches" },
    "homeSections": [
      { "key": "worshipLive", "enabled": true },
      { "key": "latestVideos", "enabled": true },
      { "key": "ccmAlbums",    "enabled": true },
      { "key": "nearbyChurch", "enabled": true }
    ],
    "featuredAlbumIds": [2, 5, 9]
  }
  ```
  - 단일 JSON 행 채택 이유: 스키마 변경 없이 설정 항목 추가 용이(데모 친화적). 검증은 서버 DTO로.

### 엔드포인트
- 관리자: `GET /v1/site-config`(현재 설정), `PUT /v1/site-config`(저장) — `@PreAuthorize` SYSTEM.
- 공개: `GET /pub/v1/site-config`(SiteConfigPublic DTO), `GET /pub/v1/banners`(위치별 활성 배너).
- `SecurityConfig`의 `PUBLIC_PATHS`에 이미 `/pub/**` 포함 → 공개 엔드포인트 추가 설정 불요.

---

## 4. 영향 범위 / 작업량

| 레이어 | 작업 | 파일(개략) | 난이도 |
|---|---|---|---|
| 백엔드 | SiteConfig 엔티티/리포/서비스/DTO/관리자+공개 컨트롤러/시드 + 배너 공개 엔드포인트 | ~8 | 중 |
| 관리자(web) | `/site-config` 화면(폼: 테마/색/공지/섹션 토글·정렬/추천음반 멀티선택) + 사이드바 메뉴 + `npm run gen` | ~3 + gen | 중 |
| 운영(user-web) | layout SSR fetch(공지바·기본테마·accent var) + home 섹션 순서/토글/추천행 + 배너 컴포넌트 | ~5 | 중 |

- 총량: **백엔드 배포 → orval gen → 양 프론트** 의 기존 파이프라인을 그대로 탐. 기존 `banner`/`album` 자산 재사용으로 절감.
- 한 세션 내 **MVP(A 핵심 + B 배너연동)** 구현 가능 추정. featured/섹션정렬까지 풀로 하면 분량 증가.

## 5. 리스크 / 결정 필요 사항 (결정 게이트)
1. **테마 우선순위**: 관리자 기본테마 vs 사용자 토글. → 권장: 사용자가 한 번이라도 토글하면 그 선택(localStorage) 우선, 아니면 관리자 기본값. (이미 만든 라이트모드와 충돌 없음)
2. **accent 동적 주입**: `--primary`를 임의 색으로 바꾸면 대비(접근성) 깨질 수 있음. → 사전 정의된 팔레트(예: cyan/violet/rose/green) 중 선택 방식 권장(자유 hex보다 안전).
3. **공지바 노출 위치**: AppBar 아래 고정 띠. 탭/스크롤과 z-index 충돌 점검 필요(낮음).
4. **운영 SSR vs 클라 fetch**: 공지바·테마 깜빡임 줄이려면 layout 서버 컴포넌트에서 fetch 권장(현재 layout은 서버 컴포넌트라 가능).
5. **범위 선택**: (a) A 전체 + B  /  (b) B 배너연동 + 공지바만(가장 작게)  /  (c) A+B 풀(섹션정렬·추천·색팔레트까지).

---

## 6. 권장안
**MVP = A(공지바 + 기본테마 + accent 팔레트 + 홈 섹션 토글) + B(배너 공개 연동)**.
- 이유: 기존 banner/album 자산을 살리고, "관리자에서 바꾸면 운영이 바뀐다"를 가장 분명하게 시연. 섹션 **드래그 정렬**과 자유 hex 색은 후속(리스크/분량 큼)으로 분리.
