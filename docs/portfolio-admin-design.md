# StreamHub Admin — 포트폴리오 어드민 설계문서

> **한 줄 정의**: PalmPlus(교회·스트리밍) 운영 어드민을, 그누보드5/영카트5 수준의 기능 폭으로 재구성하여 **"실제 운영 중인 서비스의 관제실"처럼 보이는 포트폴리오**로 만든다.
> **두 개의 입구**: ① 한눈에 보는 **통합 운영 대시보드** ② 만든 것을 전시하는 **기능 카탈로그**.
> **스택**: 기존 `ng-admin-front`(Next.js 14, TS) + `ng-admin-api`(Spring Boot 3.x, Java 21) 재활용 · Colima 인프라.

---

## 1. 목표와 성공 기준 (포트폴리오 관점)

### 1.1 이 사이트가 증명해야 할 것
포트폴리오를 보는 사람(채용담당/PM/클라이언트)에게 **30초 안에** 다음을 전달한다.
1. **운영 가능한 실서비스를 설계·구현할 수 있다** — 단순 CRUD가 아니라 주문 상태머신·정기결제·재고·포인트 원장 같은 *진짜 운영 도메인*.
2. **넓은 기능 폭을 일관된 아키텍처로 묶는다** — 7개 도메인 × 수십 화면을 공통 컴포넌트/레이어드 구조로.
3. **실제 데이터로 돌아간다** — 목업이 아니라 사진·가격·시계열이 살아있는 것처럼.

### 1.2 성공 기준 (측정 가능하게)
| 항목 | 기준 |
|---|---|
| 첫인상 | 대시보드 진입 3초 내 "운영 중인 서비스"로 인지 (실시간 피드 + KPI + 차트) |
| 기능 폭 | 카탈로그에 **20+ 화면**, 그중 **8~10개는 실제 동작**(DB 연동), 나머지는 정교한 목업 |
| 데이터 현실성 | 굿즈 60+종(실 이미지·가격), 회원 800+, 주문/후원 3,000+건이 최근 6개월에 자연 분포 |
| 코드 품질 | 린트/포맷 0 경고, 공통 컴포넌트 재사용률 높음, API 스펙 자동생성(Orval) |
| 데모 안전성 | 실제 PII·저작권 자산 0건, 결제는 테스트 모드, 파괴적 액션은 가드 |

### 1.3 비목표 (명시적으로 안 함)
- 그누보드 58개 기능 **전부** 구현 ❌ → 임팩트 기준 큐레이션.
- 실결제·실발송(문자/메일) ❌ → 전부 시뮬레이션/테스트 모드.
- 멀티테넌시·실사용자 트래픽 ❌ → 단일 데모 조직.

---

## 2. 도메인 매핑 — 영카트/그누보드 → PalmPlus

PalmPlus는 **교회·스트리밍 플랫폼**이다. 커머스 기능을 도메인 언어로 치환한다. (이 표가 설계의 뿌리)

| 그누보드/영카트 개념 | PalmPlus(StreamHub) 개념 | 비고 |
|---|---|---|
| 쇼핑몰 / 상품(`g5_shop_item`) | **굿즈샵 / 굿즈**(음반·도서·교회 굿즈) | 사진·가격·옵션 그대로 살림 |
| 분류(`g5_shop_category`) | 굿즈 카테고리 (음반/도서/의류/소품) | 3단 트리 |
| 상품옵션/재고 | 굿즈 옵션(사이즈·색상)/재고 | 옵션재고 화면 재사용 |
| 주문(`g5_shop_order`) | **주문**(굿즈) + **후원결제** 통합 | 상태머신 공유 |
| 정기결제(구독) | **정기후원 / 멤버십 구독** | 빌링키·회차·캘린더 |
| 회원(`g5_member`) | **구독자/후원자/신도** | 레벨=멤버십 등급 |
| 포인트(`g5_point`) | **은혜 포인트**(적립/사용 원장) | 후원 시 적립 |
| 쿠폰/쿠폰존 | 후원 감사 쿠폰 / 굿즈 할인쿠폰 | 그대로 |
| 게시판/게시글 | **공지 / 말씀나눔 / 기도제목** | 권한·포인트 옵션 |
| 1:1문의(`g5_qa`) | **문의**(굿즈문의 + 일반문의) | |
| 내용관리(`g5_content`) | 정적 페이지(소개·약관·이용안내) | |
| SMS5 | **알림센터**(예배안내·후원영수증·재입고) | 발송 로그만, 실발송 X |
| 배너/이벤트 | 메인 배너 / 캠페인(특별헌금·신간) | |
| 매출현황 | **후원·매출 현황** | 대시보드 핵심 차트 |
| (신규) | **콘텐츠/스트리밍**(설교 영상·음원) | PalmPlus 고유 — 조회수·구독 연계 |

> **콘텐츠 도메인**은 그누보드엔 없지만 PalmPlus의 정체성이므로 **추가**한다. 이게 "쇼핑몰 클론"이 아니라 "스트리밍 플랫폼 운영툴"로 보이게 하는 차별점.

---

## 3. 정보 구조(IA) & 화면 맵

```
StreamHub Admin
├── 🏠 통합 운영 대시보드  (/admin/dashboard)         ← 입구 ①
├── 🗂️ 기능 카탈로그        (/admin/catalog)           ← 입구 ②
│
├── 후원·구독
│   ├── 정기후원/구독 현황   (/admin/subscription)
│   ├── 후원 내역           (/admin/donation)
│   └── 멤버십 플랜 관리     (/admin/subscription/plans)
├── 굿즈샵
│   ├── 주문 관리           (/admin/orders)            ★실동작
│   ├── 굿즈 관리           (/admin/goods)             ★실동작
│   ├── 카테고리/옵션·재고   (/admin/goods/category|stock)
│   ├── 쿠폰                (/admin/coupons)
│   └── 굿즈문의/후기        (/admin/goods/inquiry|review)
├── 콘텐츠 (스트리밍)
│   ├── 설교/음원 관리       (/admin/contents)          ★실동작
│   └── 콘텐츠 통계         (/admin/contents/stats)
├── 회원
│   ├── 회원 관리           (/admin/members)           ★실동작
│   ├── 포인트 원장         (/admin/points)
│   └── 접속 통계           (/admin/visits)
├── 소통
│   ├── 게시판 관리         (/admin/boards)
│   ├── 공지/나눔/기도제목   (/admin/posts)
│   └── 1:1 문의            (/admin/inquiry)           ★실동작
├── 마케팅
│   ├── 배너                (/admin/banners)
│   ├── 캠페인/이벤트        (/admin/campaigns)
│   └── 알림센터(발송로그)   (/admin/notifications)
└── 설정
    ├── 사이트 설정         (/admin/settings)
    └── 메뉴/정적페이지      (/admin/settings/menu|pages)
```

**★실동작** = DB 연동 + 실제 CRUD. 나머지는 시드 데이터 기반 조회 + 정교한 목업.

---

## 4. 입구 ① — 통합 운영 대시보드 (`/admin/dashboard`)

"운영 중인 관제실"의 첫인상을 만드는 화면. **위에서 아래로 중요도 순.**

### 4.1 상단 KPI 스트립 (6 카드)
| 카드 | 값 | 보조 |
|---|---|---|
| 오늘 후원·매출 | ₩ 합계 | 전일 대비 ▲▼ %, 미니 스파크라인 |
| 신규 구독 | N건 | 이번 달 누적 / 목표 대비 게이지 |
| 진행 중 주문 | N건 | 입금대기·배송준비 분해 뱃지 |
| 미답변 문의 | N건 | SLA 초과 빨강 강조 |
| 재고 경고 | N종 | 통보수량 이하 품목 |
| 활성 구독자 | N명 | MoM 증가율 |

### 4.2 메인 위젯 (2열 그리드, ApexCharts — 스택 기존 보유)
- **후원·매출 추이** (좌, 큰 영역): 최근 90일 라인+바, 굿즈매출/정기후원/단건후원 스택. 주말·특별헌금일 스파이크가 자연스럽게.
- **실시간 활동 피드** (우): "방금 김OO님이 멤버십 가입", "굿즈 주문 #..." 5초마다 한 줄씩 흐르는 느낌(시드 타임라인 재생).
- **인기 굿즈 Top 5** / **콘텐츠 조회 Top 5**: 썸네일+수치.
- **멤버십 플랜 분포**: 도넛(브론즈/실버/골드/후원천사).
- **할 일 큐**: 미처리 주문·미답변 문의·재고 통보 — 클릭 시 해당 관리화면 필터로 딥링크.
- **회원 증가 추이**: 누적 라인.

### 4.3 "실시간처럼" 보이는 트릭 (5장에서 상세)
- 서버 시각 기준 **상대 시간**("3분 전") + 카운터 애니메이션.
- 활동 피드는 시드에 심어둔 이벤트 타임라인을 현재 시각에 맞춰 슬라이딩 윈도우로 재생.

---

## 5. 입구 ② — 기능 카탈로그 (`/admin/catalog`)

만든 것을 **전시**하는 페이지. 포트폴리오 viewer가 "이만큼 만들 수 있구나"를 한눈에.

### 5.1 레이아웃
- 상단: 도메인 필터 칩(후원·굿즈·콘텐츠·회원·소통·마케팅·설정) + 상태 필터(✅실동작 / 🟡목업 / 🔧진행중) + 검색.
- 본문: **카드 그리드**. 카드 = 아이콘 + 기능명 + 1줄 설명 + 상태 배지 + 스크린샷 썸네일 + `[열기]`/`[코드 보기]`.
- 카드 hover 시 미니 프리뷰(스크린샷 확대) 또는 GIF.

### 5.2 카드 데이터 모델 (정적 메타로 관리)
```ts
type FeatureCard = {
  id: string; domain: Domain; title: string; summary: string;
  status: 'live' | 'mock' | 'wip';
  href: string;            // 실제 화면으로 딥링크
  repoPath?: string;       // GitHub 소스 링크 (선택)
  thumb: string;           // 스크린샷
  highlights: string[];    // "상태머신", "빌링키", "원장" 등 어필 키워드
};
```
> 카탈로그는 **단일 메타 파일**(`features.catalog.ts`)에서 렌더 → 화면 추가 시 카드 한 줄만 등록. 대시보드의 "할 일"·검색과도 공유 가능.

### 5.3 운영 디테일
- 각 카드에 "이 화면에서 보여주는 기술" 태그(예: *AG Grid 인라인 일괄수정*, *주문 상태 전이*, *CRON 정기청구 시뮬*).
- 상태 배지로 **정직하게** 실동작/목업 구분 → 신뢰도 ↑ (전부 live인 척 안 함).

---

## 6. 데이터 모델 (ERD 개요)

`ng-admin-api` 레이어드 구조(`controller → service → repository(JPA) / mapper(MyBatis)`)에 맞춘 핵심 엔티티. 접두어 없이 도메인 패키지로 분리.

### 6.1 핵심 엔티티
```
member(id, login_id, name, nickname, email_masked, phone_masked,
       grade[BRONZE|SILVER|GOLD|ANGEL], point_balance, status, joined_at, last_login_at)

subscription_plan(id, name, price, period[MONTH], benefit_json, active)
subscription(id, member_id, plan_id, billing_key_masked, status[ACTIVE|PAUSED|CANCELED],
             cycle_no, next_billing_at, started_at)         ← 정기후원/멤버십
donation(id, member_id, amount, type[ONCE|SUBSCRIPTION], campaign_id, paid_at)

goods_category(id, parent_id, name, depth, sort, image)
goods_item(id, category_id, name, price, list_price, stock, sold_out, status,
           sale_count, view_count, thumb, badges[HIT|NEW|SALE])
goods_option(id, item_id, name, type, extra_price, stock)
goods_image(id, item_id, url, sort)

order(id, order_no, member_id, status[PLACED|PAID|READY|SHIPPING|DONE|CANCEL|RETURN],
      total, ship_fee, coupon_discount, point_used, pay_method, tracking_no, ordered_at)
order_item(id, order_id, goods_id, option_id, qty, price)
order_receipt(id, order_id, kind[PAY|REFUND], amount, method, at)

coupon(id, name, method, type[FLAT|RATE], value, min_amount, max_discount, start, end)
point_ledger(id, member_id, delta, balance_after, reason, expire_at, at)

content(id, category_id, title, type[VIDEO|AUDIO], thumb, duration,
        view_count, published_at, status)                  ← 설교/음원 스트리밍
content_category(id, name, sort)

board(id, code, name, group, list_level, write_level, use_secret, skin, sort)
post(id, board_code, member_id, category, title, body, hit, good, secret, created_at)
inquiry(id, member_id, goods_id?, subject, body, status[OPEN|ANSWERED], answered_at)

banner(id, position, device, image, link, start, end, sort, status)
campaign(id, title, type[OFFERING|NEW_RELEASE|EVENT], banner, period, linked_ids)
notification_log(id, channel[SMS|PUSH|EMAIL], template, target_count,
                 success, fail, sent_at, scheduled_at)      ← 실발송 X, 로그만

site_config(singleton: title, admin_email, org_info_json, theme, point_policy_json …)
menu(id, parent_id, name, link, target, sort, pc_use, mobile_use)
static_page(id, code, title, body, mobile_body)
```

### 6.2 상태머신 (포트폴리오 어필 포인트)
- **주문**: `PLACED → PAID → READY → SHIPPING → DONE`, 분기 `CANCEL/RETURN`. 전이 시 재고·포인트·합계 재계산.
- **구독**: `ACTIVE ⇄ PAUSED → CANCELED`, CRON이 `next_billing_at` 도래 시 회차 청구 시뮬 → `donation` + `point_ledger` 생성.

### 6.3 권한 모델
- `member.grade`(등급) ≠ 운영 권한. 운영 권한은 `admin_user`(별도) + `role`(SUPER/MANAGER/VIEWER).
- 화면/액션별 가드: 설정·정산 = SUPER, 일반 운영 = MANAGER, 카탈로그 데모 = VIEWER 읽기전용.
- **데모 안전**: 파괴적 액션(삭제·환불)은 VIEWER에게 비활성 + 확인 모달.

---

## 7. 시드 데이터 전략 — "실제 운영처럼"

핵심은 **현실적 분포 + 저작권 안전 + 가짜 PII**.

### 7.1 자산(사진·이미지·가격) 확보
| 종류 | 출처 전략 | 안전장치 |
|---|---|---|
| 굿즈 사진 | 무료 라이선스(Unsplash/Pexels) — 음반/책/머그/의류 | 라이선스 표기, 실제 브랜드 로고 회피 |
| 콘텐츠 썸네일 | 자체 생성(타이포+그라디언트 템플릿, OG 이미지 빌더) | 100% 자작 |
| 가격 | KRW 현실 범위(굿즈 8,000~45,000 / 멤버십 5,000~50,000) | 반올림 단위 자연스럽게 |
| 회원 PII | `faker(ko)` 생성 + **마스킹**(데모와 동일: `김O준`, `010-****-1234`) | 실제 개인정보 0 |

### 7.2 시계열 자연스럽게 만들기 (가장 중요)
단순 랜덤이면 "목업 티"가 난다. **운영 패턴을 모델링**한다.
- **베이스라인 + 추세**: 최근 6개월 우상향, 월초/주말 피크.
- **이벤트 스파이크**: 특별헌금일·신간 발매일에 후원/주문 급증(캠페인과 연동).
- **상태 분포**: 주문의 70% DONE, 15% SHIPPING/READY, 10% PAID 대기, 5% CANCEL/RETURN.
- **재고 현실성**: 일부 품절·통보수량 이하(재고경고 KPI가 0이 아니게).
- **구독 라이프사이클**: 신규/유지/일시정지/해지가 섞이고 회차가 제각각.
- **롱테일**: 인기 굿즈 소수 + 판매 0 다수(파레토).

### 7.3 구현 방식
- `data.sql` 정적 시드 + **시드 생성기**(Java `CommandLineRunner` 또는 별도 `seed` 프로파일 스크립트).
- 생성기는 **결정론적**(고정 seed)으로 재현 가능 — 데모 리셋해도 동일 데이터.
- 기준일을 "오늘"로 잡고 상대 날짜로 생성 → 언제 봐도 "최근 데이터".

---

## 8. 기술 아키텍처 (기존 스택에 얹기)

### 8.1 프론트 (`ng-admin-front`, Next.js 14 App Router)
- 라우트: `src/app/admin/(portfolio)/...` 그룹으로 격리(기존 어드민과 충돌 방지).
- 상태: 서버데이터 **React Query**, 전역 UI **Zustand** (기존 규약).
- 목록 화면: **AG Grid Enterprise**(기존 보유)로 검색+전체선택+인라인 일괄수정+페이징 = *3장의 공통 리스트 패턴*을 1개 `AdminListGrid` 래퍼로.
- 차트: **ApexCharts**(기존). 폼: **React Hook Form + Zod**.
- API 클라이언트: **Orval**로 OpenAPI에서 자동생성(`npm run gen`) — 단, **백엔드 배포 확인 후에만**(프로젝트 규칙).
- 디자인: shadcn/ui + Tailwind, Atomic Design. 카탈로그/대시보드는 별도 `widgets/` 컴포넌트군.

### 8.2 백엔드 (`ng-admin-api`, Spring Boot 3.x / Java 21)
- 패키지: `domain/{member,subscription,goods,order,content,board,inquiry,marketing,config}` × `controller/service/repository`.
- 단순 조회는 **JPA + QueryDSL**, 통계/그리드 복합쿼리는 **MyBatis 매퍼**(기존 혼용 규약).
- 캐시: 대시보드 KPI/차트는 **Redis** 캐시(짧은 TTL) → "빠른 관제실" 느낌.
- 인증: 기존 **Spring Security(JWT/세션)** 재사용, 데모용 시드 관리자 계정.
- 정기청구 시뮬: **스케줄러**(`@Scheduled`)가 `next_billing_at` 스캔 → 회차 생성. (실 PG 미연동, 테스트 응답 고정)

### 8.3 API 설계 원칙
- 대시보드 전용 집계 엔드포인트 `/api/v1/dashboard/summary`, `/timeseries`, `/feed` — N+1 없이 한 번에.
- 목록은 공통 페이지네이션/검색 DTO(`sfl`/`stx` → `searchField`/`keyword`)로 통일.
- 카탈로그 메타는 프론트 정적(`features.catalog.ts`) — 백엔드 불필요.

---

## 9. "실제 운영처럼" 연출 디테일 체크리스트
- [ ] 모든 날짜는 **상대시간**("어제", "3시간 전") + 절대시간 툴팁.
- [ ] 숫자 KPI **카운트업 애니메이션** + 전기간 대비 ▲▼.
- [ ] 활동 피드 **실시간 재생**(시드 타임라인 슬라이딩).
- [ ] 빈 상태가 아니라 **항상 데이터가 차 있음**(빈 테이블 금지).
- [ ] 일부러 **운영의 흠집**을 남김: 미답변 문의 3건, 품절 2종, 환불 1건 → 진짜 운영처럼.
- [ ] 푸터/헤더에 "데모 데이터 · 테스트 모드" **명시**(신뢰·정직).
- [ ] 반응형: 데스크톱 관제실 + 모바일 요약 카드.
- [ ] 다크모드(선택) — 관제실 감성.

---

## 10. 구현 로드맵 (Phase)

> 원칙: **세로로 얇게, 임팩트 순으로**. 한 도메인을 끝까지(시드→API→화면) 완성하고 다음으로.

| Phase | 산출물 | 어필 포인트 | 상태 |
|---|---|---|---|
| **P0 기반** | 라우트 그룹·공통 `AdminListGrid`·시드 생성기 골격·관리자 인증 | 아키텍처 토대 | 백엔드 검증 완료(메모리) |
| **P1 굿즈+주문** | 굿즈/카테고리/옵션재고 + 주문 상태머신 (★실동작) | 재고·상태전이·합계계산 | |
| **P2 대시보드** | KPI·차트·실시간 피드·할 일 큐 | "관제실" 첫인상 | |
| **P3 후원·구독** | 멤버십 플랜·정기청구 CRON 시뮬·후원내역·포인트 원장 | 빌링·원장 = 고급 도메인 | |
| **P4 콘텐츠** | 설교/음원 관리·조회수 통계 | PalmPlus 차별점 | |
| **P5 카탈로그** | 기능 카탈로그 + 목업 화면들(쿠폰·배너·문의·알림로그) | 기능 폭 전시 | |
| **P6 마감** | 연출 디테일·반응형·정직 배지·README·스크린샷 | 완성도·신뢰 | |

**큐레이션 기준**: "운영 도메인의 깊이를 보여주는 것"(주문 상태머신·정기결제·재고·포인트 원장)은 **실동작**으로, "폭을 보여주는 것"(배너·이벤트·알림로그·접속통계)은 **목업+카탈로그 전시**로.

---

## 11. 리스크 & 가드
| 리스크 | 대응 |
|---|---|
| 저작권(이미지/브랜드) | 무료 라이선스만, 실 브랜드/로고 회피, 출처 표기 |
| 가짜 PII 오해 | 전부 `faker` + 마스킹, "데모 데이터" 명시 |
| 결제 오인 | 테스트 모드 고정, PG 미연동, "실결제 아님" 라벨 |
| 데모 파손 | 결정론적 시드 + 원클릭 리셋, 파괴적 액션 VIEWER 차단 |
| 범위 폭주 | 58개 전부 금지 — P1~P4 실동작 외엔 목업/카탈로그로 |
| 기존 어드민 충돌 | `(portfolio)` 라우트 그룹·별도 DB 스키마/프로파일로 격리 |

---

## 12. 다음 액션 (이 문서 승인 후)
1. **도메인 매핑 표(2장)** 확정 — 용어(후원/구독/굿즈/콘텐츠) 최종 픽스.
2. **ERD를 실제 엔티티 클래스**로 — `ng-admin-api`에 도메인 패키지 스캐폴딩.
3. **시드 생성기** 먼저(P0) — 화면 없어도 데이터가 있어야 "현실성" 검증 가능.
4. P1(굿즈+주문) 세로 슬라이스 완성 → 대시보드(P2)로.

> 각 Phase 진입 전 `make fmt && test && lint` 그린 확인(프로젝트 규칙), 5개 이상 파일 변경 시 사전 보고.

---
*본 설계는 demo.sir.kr 그누보드5/영카트5 관리자의 실측 기능 명세(`gnuboard5-admin-feature-spec.md`)를 PalmPlus 도메인으로 재해석한 포트폴리오 구현 설계다. 기능 폭은 그누보드에서, 도메인 언어와 차별점(스트리밍 콘텐츠)은 PalmPlus에서 가져온다.*
