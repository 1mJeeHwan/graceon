# 기능 카탈로그 페이지 — 구현 스펙 (Feature Catalog)

> 설계문서 5장(입구 ② 기능 카탈로그) 구현 스펙. **프론트 전용** — 백엔드/DB/Orval 불필요.
> 대상 코드베이스: `streamhub-admin/streamhub-web` (Next.js 14 App Router, TS, Tailwind, lucide-react).

---

## 1. 목적 / 범위

기능 카탈로그는 StreamHub Admin 포트폴리오의 **두 번째 입구**다. 7개 도메인(후원·구독 / 굿즈샵 / 콘텐츠 / 회원 / 소통 / 마케팅 / 설정)에 걸친 **20장 이상의 관리 화면을 카드 그리드로 전시**하고, 각 카드에 상태 배지(`live`/`mock`/`wip`)·기술 하이라이트 태그·스크린샷 썸네일·실제 화면 딥링크를 노출하여 viewer가 30초 안에 "이만큼의 운영 도메인을 일관된 아키텍처로 만들 수 있다"를 인지하게 한다. 모든 카드는 **단일 메타 파일** `src/data/features.catalog.ts` 에서 렌더되며, 백엔드 호출 없이 정적 데이터로만 동작한다. 본 스펙은 (1) 카드 타입과 전체 데이터, (2) 카탈로그 페이지 컴포넌트(도메인 필터칩 + 상태 필터 + 검색 + 카드 그리드 + hover 프리뷰 + 정직 배지), (3) 사이드바 메뉴 추가를 다룬다.

> **정직 원칙(설계문서 5.3, 9장):** 전부 `live`인 척하지 않는다. 실제 DB 연동 화면만 `live`, 시드 기반 정교한 목업은 `mock`, 미완성은 `wip`. 실제 코드베이스에서 동작하는 라우트는 `dashboard / member / content / action-log` 4개뿐이므로 **이 4개만 `live`**, 나머지는 설계문서 IA(3장)에 존재하나 미구현이므로 `mock` 또는 `wip`.

---

## 2. 데이터 모델

> 카탈로그는 **백엔드 엔티티가 없다**(프론트 정적 메타). 따라서 DB 테이블/`@Column`/인덱스 정의는 **해당 없음**. 대신 프론트 TypeScript 타입을 데이터 모델로 정의한다. (설계문서 5.2의 `FeatureCard` 타입을 실제 코드베이스 컨벤션에 맞춰 확정.)

### 2.1 타입 정의 (`src/data/features.catalog.ts` 상단)

| 필드 | 타입 | 제약 / 비고 |
|---|---|---|
| `id` | `string` | 고유 슬러그. kebab-case. 예: `orders`, `goods`, `points-ledger`. 중복 금지(런타임 `assertUniqueIds` 가드). |
| `domain` | `Domain` (union) | `'support' \| 'shop' \| 'content' \| 'member' \| 'community' \| 'marketing' \| 'settings'` — 7개 도메인 고정. |
| `title` | `string` | 화면명(한글). 예: `"주문 관리"`. |
| `summary` | `string` | 1줄 설명(한글, ~40자). 카드 본문. |
| `status` | `FeatureStatus` (union) | `'live' \| 'mock' \| 'wip'`. |
| `href` | `string` | 실제 화면 딥링크. `live`는 구현된 라우트(`/dashboard` 등), `mock`/`wip`는 설계상 라우트(`/goods` 등, 미구현이어도 등록). |
| `gnuboard` | `string` | (선택) 기능 출처 — 그누보드 명세 §번호. 예: `"영카트 §4.2 orderlist.php"`. 카드 hover 시 출처로 노출. |
| `repoPath` | `string` | (선택) GitHub 소스 경로. 예: `"streamhub-web/src/app/(protected)/member/page.tsx"`. |
| `thumb` | `string` | 스크린샷 경로. `/catalog/<id>.png`. 실제 캡처 4종은 기존 `docs/screenshots/*` 를 `public/catalog/`로 복사·매핑, 미구현 화면은 플레이스홀더. |
| `highlights` | `string[]` | 기술 어필 태그 2~4개. 예: `["주문 상태머신", "재고/포인트 재계산", "AG Grid 일괄수정"]`. |

```ts
export type Domain =
  | "support" | "shop" | "content" | "member"
  | "community" | "marketing" | "settings";

export type FeatureStatus = "live" | "mock" | "wip";

export interface FeatureCard {
  id: string;
  domain: Domain;
  title: string;
  summary: string;
  status: FeatureStatus;
  href: string;
  gnuboard?: string;
  repoPath?: string;
  thumb: string;
  highlights: string[];
}

export interface DomainMeta {
  key: Domain;
  label: string;   // "후원·구독"
  icon: LucideIcon;
}
```

### 2.2 도메인 메타 (필터 칩 라벨/아이콘)

| `key` | `label` | lucide 아이콘 |
|---|---|---|
| `support` | 후원·구독 | `HeartHandshake` |
| `shop` | 굿즈샵 | `ShoppingBag` |
| `content` | 콘텐츠 | `FileVideo` |
| `member` | 회원 | `Users` |
| `community` | 소통 | `MessageSquare` |
| `marketing` | 마케팅 | `Megaphone` |
| `settings` | 설정 | `Settings` |

### 2.3 상태 메타 (정직 배지)

| `status` | 라벨 | 아이콘(이모지 텍스트) | className (StatusBadge 컨벤션 재사용) |
|---|---|---|---|
| `live` | 실동작 | ✅ | `bg-emerald-100 text-emerald-700` |
| `mock` | 목업 | 🟡 | `bg-amber-100 text-amber-700` |
| `wip` | 진행중 | 🔧 | `bg-slate-200 text-slate-600` |

> 색상은 기존 `src/components/member/StatusBadge.tsx` / `action-log` 의 `actionColor` 팔레트와 100% 일치.

### 2.4 전체 카드 데이터 (24장 — 그누보드 58개 기능 큐레이션)

> 출처 표기는 `gnuboard5-admin-feature-spec.md` 절번호. `live` 4장은 실제 라우트, 나머지는 설계문서 3장 IA 기준.

| # | id | domain | title | status | href | gnuboard 출처 | highlights |
|---|---|---|---|---|---|---|---|
| 1 | `dashboard` | support | 통합 운영 대시보드 | **live** | `/dashboard` | 영카트 §5.1 sale1.php | KPI 카드, ApexCharts, Redis 캐시 집계 |
| 2 | `members` | member | 회원 관리 | **live** | `/member` | §2.1 member_list.php | 동적 검색, JPA+MyBatis 하이브리드, 일괄 승인/거부 |
| 3 | `contents` | content | 설교/음원 관리 | **live** | `/content` | (레퍼런스 서비스 고유) | MinIO 업로드(S3 SDK), 해시태그 다대다, 복합 조인 |
| 4 | `action-log` | settings | 감사 로그 | **live** | `/action-log` | §2.3 visit_list.php | SQS 비동기 적재, 액션 색상 배지, SYSTEM 전용 |
| 5 | `orders` | shop | 주문 관리 | mock | `/orders` | 영카트 §4.2 orderlist.php | 주문 상태머신(PLACED→DONE), 합계 재계산, AG Grid |
| 6 | `goods` | shop | 굿즈 관리 | mock | `/goods` | 영카트 §4.5 itemlist.php | 인라인 일괄수정, 옵션/이미지, 파레토 분포 |
| 7 | `goods-category` | shop | 카테고리 관리 | mock | `/goods/category` | 영카트 §4.4 categorylist.php | 3단 트리, 계층 코드, 드래그 정렬 |
| 8 | `goods-stock` | shop | 옵션·재고 관리 | mock | `/goods/stock` | 영카트 §4.9/4.11 stocklist | 재고/통보수량 인라인, 품절 토글, 재입고 알림 |
| 9 | `goods-inquiry` | shop | 굿즈 문의 | mock | `/goods/inquiry` | 영카트 §4.7 itemqalist.php | 답변 상태, 미답변 SLA 강조 |
| 10 | `goods-review` | shop | 굿즈 후기 | mock | `/goods/review` | 영카트 §4.8 itemuselist.php | 평점 인라인, 노출 승인 토글 |
| 11 | `coupons` | shop | 쿠폰 관리 | mock | `/coupons` | 영카트 §4.12 couponlist.php | 정액/정률, 최소주문·최대할인, 절사단위 |
| 12 | `subscription` | support | 정기후원/구독 현황 | mock | `/subscription` | 정기결제 §6.4 paylist.php | 빌링키(마스킹), 회차, 상태(ACTIVE⇄PAUSED) |
| 13 | `subscription-plans` | support | 멤버십 플랜 관리 | wip | `/subscription/plans` | 정기결제 §6.3 itemlist.php | 등급별 혜택 JSON, 가격 정책 |
| 14 | `subscription-calendar` | support | 정기결제 일정 | mock | `/subscription/calendar` | 정기결제 §6.5 calendar.php | CRON 청구 시뮬, 캘린더 UI, 공휴일 이동 |
| 15 | `donation` | support | 후원 내역 | mock | `/donation` | 영카트 §4.2 / §5.1 | 단건/정기 분리, 캠페인 연동, 영수증 |
| 16 | `points` | member | 포인트(은혜) 원장 | mock | `/points` | §2.5 point_list.php | 증감 원장, 누적 동기화 트랜잭션, 만료 배치 |
| 17 | `visits` | member | 접속 통계 | mock | `/visits` | §2.3/2.4 visit_search.php | 기간 검색, IP/UA 파싱, 일별 집계 |
| 18 | `content-stats` | content | 콘텐츠 통계 | mock | `/contents/stats` | §3.8 write_count.php | 조회수 Top N, 채널별 시청시간 집계 |
| 19 | `boards` | community | 게시판 관리 | mock | `/boards` | §3.1/3.2 board_list/form | 레벨 권한(1~10), 그룹/전체 적용 전파 |
| 20 | `posts` | community | 공지/나눔/기도제목 | mock | `/posts` | §3.x write / content | 카테고리, 비밀글, 추천/조회 |
| 21 | `inquiry` | community | 1:1 문의 | mock | `/inquiry` | §3.6 qa_config / qa_content | 답변 상태머신(OPEN→ANSWERED), 미답변 큐 |
| 22 | `banners` | marketing | 배너 관리 | mock | `/banners` | 영카트 §5.6 bannerlist.php | 위치/디바이스/노출기간 필터, 순서 |
| 23 | `campaigns` | marketing | 캠페인/이벤트 | wip | `/campaigns` | 영카트 §5.5 itemevent.php | 특별헌금/신간, 연결상품 일괄, 스파이크 연동 |
| 24 | `notifications` | marketing | 알림센터(발송 로그) | mock | `/notifications` | SMS §7.3 history_list.php | 채널(SMS/PUSH/EMAIL), 성공/실패, **실발송 X** |

> **집계:** live 4 / mock 18 / wip 2 = **24장** (설계 성공기준 "20+ 화면, 8~10 live" 중 화면 수 충족. live는 현 코드베이스 실측 4장으로 정직하게 표기 — viewer 신뢰 우선).

---

## 3. 백엔드

**없음.** 카탈로그는 프론트 정적 메타(`features.catalog.ts`)로만 동작하며 어떤 API도 호출하지 않는다(설계문서 8.3: "카탈로그 메타는 프론트 정적 — 백엔드 불필요"). 생성/수정할 백엔드 파일·엔드포인트·DTO·매퍼는 **0개**.

> 미구현 `mock`/`wip` 카드의 `href`는 라우트가 아직 없으므로 클릭 시 별도 처리 필요 — §4.4 "딥링크 동작" 참조(백엔드가 아닌 프론트 라우팅 이슈).

---

## 4. 프론트

### 4.1 생성할 파일 목록 (정확한 경로)

| # | 경로 | 역할 |
|---|---|---|
| 1 | `src/data/features.catalog.ts` | 카드 타입(§2.1) + DOMAIN_META(§2.2) + STATUS_META(§2.3) + `FEATURES: FeatureCard[]`(§2.4 24장) + `assertUniqueIds()` 가드. |
| 2 | `src/app/(protected)/catalog/page.tsx` | 카탈로그 페이지(클라이언트 컴포넌트). 필터/검색 상태 + 그리드 렌더. |
| 3 | `src/components/catalog/FeatureCard.tsx` | 단일 카드 컴포넌트(썸네일 + 배지 + 하이라이트 + hover 프리뷰). |
| 4 | `src/components/catalog/CatalogStatusBadge.tsx` | 정직 배지(✅/🟡/🔧). StatusBadge 컨벤션 재사용. |
| 5 | `src/components/catalog/FilterChips.tsx` | 도메인 필터 칩 행 + 상태 필터 칩 행. |
| 6 | `public/catalog/` (디렉토리) | 썸네일 자산. 실측 4종 매핑 + 플레이스홀더 1종. §5 참조. |

> **수정할 파일 1개:** `src/components/layout/Sidebar.tsx` — `NAV_ITEMS`에 카탈로그 항목 추가(§4.5).

### 4.2 화면별 컴포넌트 / 데이터 패칭 / 상태

**데이터 패칭:** **React Query 미사용**(정적 데이터). `FEATURES` 배열을 직접 import 후 `useMemo`로 필터링. 백엔드/Orval 훅 없음.

**`catalog/page.tsx` 상태(action-log 페이지의 committed-vs-draft 패턴 차용, 단 검색은 즉시 반영해도 무방):**

```ts
"use client";
const [domain, setDomain] = useState<Domain | "ALL">("ALL");
const [status, setStatus] = useState<FeatureStatus | "ALL">("ALL");
const [keyword, setKeyword] = useState("");

const visible = useMemo(() => {
  const kw = keyword.trim().toLowerCase();
  return FEATURES.filter((f) =>
    (domain === "ALL" || f.domain === domain) &&
    (status === "ALL" || f.status === status) &&
    (kw === "" ||
      f.title.toLowerCase().includes(kw) ||
      f.summary.toLowerCase().includes(kw) ||
      f.highlights.some((h) => h.toLowerCase().includes(kw))),
  );
}, [domain, status, keyword]);
```

**레이아웃 (action-log 페이지의 헤더/검색바 마크업 컨벤션 재사용):**
- 헤더: `<h1 className="text-xl font-semibold text-slate-900">기능 카탈로그</h1>` + 서브카피 + **"데모 데이터 · 일부 화면은 목업입니다" 정직 안내**(설계문서 9장).
- 필터 영역: `FilterChips`(도메인 칩 + 상태 칩) + 검색 input(`focus:border-brand focus:ring-1 focus:ring-brand` 컨벤션).
- 카운트: `<p className="mb-3 text-sm text-slate-600">총 {visible.length}개 · 실동작 {liveCount}</p>`.
- 그리드: `<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">` → `visible.map((f) => <FeatureCard ... />)`.
- 빈 상태: 필터 결과 0개 시 `"조건에 맞는 화면이 없습니다."`(action-log 빈 상태 톤 일치).

**`FeatureCard.tsx` 마크업 개요:**
- 카드 컨테이너: `rounded-md border border-slate-200 bg-white overflow-hidden transition hover:shadow-md hover:border-brand` (대시보드 카드 컨벤션 `rounded-md border border-slate-200 bg-white` 계승).
- 썸네일: `next/image` 또는 `<img>` — `f.thumb`, `aspect-video object-cover`, hover 시 `scale-105 transition`(프리뷰 확대, 설계문서 5.1 "hover 미니 프리뷰").
- 본문: 도메인 아이콘(lucide, `h-4 w-4 text-slate-400`) + `title`(`text-sm font-semibold text-slate-900`) + `CatalogStatusBadge`.
- `summary`: `text-xs text-slate-500`.
- `highlights`: 칩 행 — `highlights.map((h) => <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] text-slate-600">)` (HashtagChips 톤 차용).
- 액션: `live`/`mock`/`wip`별 `[열기]` 버튼 동작 분기(§4.4). `repoPath` 있으면 `[코드 보기]`(GitHub 링크) 추가.

**`CatalogStatusBadge.tsx`:** §2.3 STATUS_META 기반. StatusBadge.tsx와 동일 구조(`Record<FeatureStatus, {label, emoji, className}>`, 알 수 없는 값은 `bg-slate-200 text-slate-600` 폴백).

**`FilterChips.tsx`:** 칩 = `<button>` — 활성 시 `bg-brand text-white`, 비활성 시 `bg-white text-slate-600 border border-slate-200 hover:bg-slate-100`(Sidebar 활성 상태 컨벤션 재사용). "전체" 칩 포함. props: `{ domains, status, selectedDomain, selectedStatus, onDomainChange, onStatusChange }`.

### 4.3 폼 (RHF + Zod)

**없음.** 카탈로그는 조회·필터 전용 화면이라 입력 폼·검증이 없다. 검색 input은 단일 제어 컴포넌트(`useState`)로 충분하며 RHF/Zod 도입 불필요. (콘텐츠 등록 등 다른 스펙과 달리 이 화면은 폼 없음 — 명시.)

### 4.4 딥링크 동작 (live vs mock/wip)

설계문서 5.2 "href는 실제 화면으로 딥링크"이나, 실제 코드베이스에 `mock`/`wip` 라우트는 아직 없다. 정직 원칙에 맞춘 처리:
- `live` 카드 → `<Link href={f.href}>` 정상 이동(`/dashboard`, `/member`, `/content`, `/action-log`).
- `mock`/`wip` 카드 → `[열기]` 버튼을 **비활성(`disabled`, `opacity-50`)** 처리하고 호버 툴팁 `"목업 — 준비 중인 화면"`. 또는(선택) 클릭 시 `PlaceholderPage`로 가는 라우트를 추후 추가. **기본은 비활성** — 깨진 링크(404) 방지가 정직성보다 우선.

> 향후 mock 화면을 `PlaceholderPage`로 채울 경우, 해당 `(protected)/<path>/page.tsx`를 `<PlaceholderPage title=... />`로 생성하면 `live` 외 카드도 클릭 가능해진다(이번 범위 밖).

### 4.5 AG Grid 컬럼 defs

**해당 없음.** 카탈로그는 카드 그리드(CSS grid)이지 데이터 테이블이 아니다. AG Grid를 쓰지 않으며 `ColDef`도 정의하지 않는다. (목록형이 아니라 갤러리형이므로 member/content의 AG Grid 패턴과 의도적으로 다름 — 명시.)

### 4.6 사이드바 메뉴 추가

`src/components/layout/Sidebar.tsx`의 `NAV_ITEMS`에 카탈로그 항목 추가. 대시보드 다음, 두 번째 입구이므로 상단 배치:

```ts
import { LayoutDashboard, LayoutGrid, Users, FileVideo, ScrollText } from "lucide-react";

const NAV_ITEMS: NavItem[] = [
  { label: "대시보드", href: "/dashboard", icon: LayoutDashboard },
  { label: "기능 카탈로그", href: "/catalog", icon: LayoutGrid }, // ← 추가
  { label: "회원관리", href: "/member", icon: Users },
  { label: "콘텐츠관리", href: "/content", icon: FileVideo },
  { label: "감사 로그", href: "/action-log", icon: ScrollText, systemOnly: true },
];
```

> `systemOnly` 미지정 → 모든 운영자에게 노출(카탈로그는 포트폴리오 전시이므로 VIEWER 포함 공개).

---

## 5. 시드 영향

**DB 시드 없음**(백엔드 미사용). 대신 **썸네일 자산**이 필요하다:

| 카드 id | thumb 경로 | 출처 |
|---|---|---|
| `dashboard` | `/catalog/dashboard.png` | 기존 `docs/screenshots/admin-dashboard.png` 복사 |
| `members` | `/catalog/members.png` | 기존 `docs/screenshots/admin-members.png` 복사 |
| `contents` | `/catalog/contents.png` | 기존 `docs/screenshots/admin-content.png` 복사 |
| `action-log` | `/catalog/action-log.png` | (캡처 추가 필요 — 없으면 플레이스홀더) |
| 나머지 20장 | `/catalog/placeholder.png` | 공용 플레이스홀더 1종(타이포+그라디언트, 100% 자작 — 저작권 안전) |

- 실측 4종은 `cp docs/screenshots/admin-*.png streamhub-web/public/catalog/<id>.png`.
- `public/` 디렉토리가 현재 비어 있으므로 `public/catalog/`를 신규 생성.
- 플레이스홀더는 단색/그라디언트 + 화면명 텍스트 SVG/PNG 1장으로 충분(설계문서 7.1: 콘텐츠 썸네일은 자작 OG 빌더 — 동일 원칙).

---

## 6. 구현 순서 체크리스트 (파일 단위, 의존 순서)

1. [ ] `public/catalog/` 생성 + 실측 4종 복사(`docs/screenshots/admin-{dashboard,members,content}.png` → `public/catalog/{dashboard,members,contents}.png`) + `placeholder.png` 1장 자작.
2. [ ] `src/data/features.catalog.ts` — 타입(§2.1) → DOMAIN_META(§2.2) → STATUS_META(§2.3) → `FEATURES`(§2.4 24장) → `assertUniqueIds()` 호출. **(의존 없음, 최우선)**
3. [ ] `src/components/catalog/CatalogStatusBadge.tsx` — STATUS_META 소비. (의존: §2.3)
4. [ ] `src/components/catalog/FilterChips.tsx` — DOMAIN_META + STATUS_META 소비. (의존: 2번)
5. [ ] `src/components/catalog/FeatureCard.tsx` — CatalogStatusBadge + `next/link` + 썸네일. (의존: 2,3번)
6. [ ] `src/app/(protected)/catalog/page.tsx` — 필터 상태 + FilterChips + FeatureCard 그리드. (의존: 2~5번)
7. [ ] `src/components/layout/Sidebar.tsx` — `NAV_ITEMS`에 카탈로그 추가(§4.6). (의존: 6번 라우트 존재)
8. [ ] 검증: `npm run build` 그린, `/catalog` 진입 → 필터/검색 동작, live 4장 딥링크 이동, mock 카드 비활성 확인.

---

## 7. 위험 / 주의

- **깨진 딥링크:** `mock`/`wip` 카드의 `href`는 미구현 라우트 → `<Link>`로 직접 걸면 404. 반드시 §4.4대로 `live`만 이동, 나머지는 `disabled`. (404는 "운영 중" 첫인상을 깬다.)
- **정직성 유지:** live를 부풀리지 말 것. 실측 4장만 `live`. viewer가 클릭해서 동작 안 하면 신뢰 하락 — 배지와 실제 동작이 일치해야 한다.
- **자동생성 디렉토리 금지구역:** `src/apis/query/`(Orval)·`src/components/ui/`(shadcn)는 손대지 않는다. 카탈로그는 이들과 무관한 신규 파일만 생성.
- **5개 파일 이상 변경:** 본 작업은 신규 5파일 + Sidebar 1수정 = 6파일 → 프로젝트 규칙(5개 이상 사전 보고)에 따라 구현 착수 전 사용자 승인 1회 필요.
- **이미지 저작권:** 플레이스홀더는 100% 자작(타이포+그라디언트). 실측 스크린샷은 자체 캡처이므로 안전. 외부 이미지 반입 금지.
- **`next/image` 설정:** `public/catalog/` 로컬 자산은 `next/image` 추가 도메인 설정 불필요. 외부 URL 썸네일을 쓸 경우에만 `next.config` 도메인 허용 필요(이번 범위는 전부 로컬).
- **id 중복:** `assertUniqueIds()`로 빌드/렌더 시 중복 id를 즉시 throw — 카드 추가 시 회귀 방지(메모리의 `assertUniqueValue` 게이트 원칙 준수).
- **라우트 그룹:** 카탈로그는 `(protected)` 그룹 안에 둔다 → 자동으로 Sidebar+Header 셸과 NextAuth 가드 적용. 설계문서의 `/admin/catalog` 경로는 현 코드베이스 라우팅(`/catalog`)으로 매핑(실제 라우트는 `/dashboard`처럼 prefix 없음).
