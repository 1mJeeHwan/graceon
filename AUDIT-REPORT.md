# StreamHub 흠결·위험 감사 보고서 (현재 상태)

> 갱신일 **2026-06-19** · 대상 커밋 `918a7ce` (feat/portfolio-admin-extension) · 범위: streamhub 전체(api · admin-web · user-web · infra)
> 방법: 8개 차원 병렬 코드 감사(보안/재무/동시성/멀티테넌시/외부연동/API품질/프론트/인프라) → 각 발견을 **적대적 검증**(허위양성 제거 + 심각도 교정)
> 결과: **확정 20건 · 기각 1건 · 불확실 0건**

---

## 0. 이전 실사(2026-06-18) 이후 변화

2026-06-18 인수 실사가 지목한 **BLOCKER 5종 + MAJOR 12종 + FAKE 5종**은 커밋 `ca056b1`→`918a7ce` 구간에서 **모두 해소**되었다(쿠폰 적용·포인트 단일화·멀티테넌시 스코핑·재고 동시성·정기결제 멱등성·환불 PG취소·HLS 암호화·CSO 보안 수정 등, 테스트 179개). 본 보고서는 그 수정 **이후 현재 HEAD**에 남아있는 결함만 다룬다 — 즉 *새로 발견되었거나, 수정이 한 도메인에만 적용되어 누락된* 잔여 흠결이다.

기각 1건: **S3 업로드 확장자/Content-Type 화이트리스트 부재** → 코드 관찰은 사실이나 미디어 버킷이 완전 비공개(block public + hls/* 만 CloudFront 서빙)라 stored-XSS 경로가 성립하지 않음 → **실제 결함 아님**(방어심화 권고로만).

---

## 1. HIGH (5건) — 실 서비스에서 데이터 무결성/보안에 직접 영향

| # | 결함 | 위치 | 핵심 |
|---|------|------|------|
| H1 | **환불/취소 시 쿠폰 미환원** | `OrderService.java:192-204`, `CouponService.java:135-165` | 환불해도 `used_count`가 안 줄고 `COUPON_REDEMPTION` 행이 남아 → 글로벌 한도 영구 잠식 + 해당 회원 동일 쿠폰 **재사용 영구 차단**. Order에 `coupon_id` 컬럼조차 없어 역추적 불가. |
| H2 | **실 PG 결제서 쿠폰 1단계 영구 소진** | `MemberOrderService.java:158-237` | `prepare`가 쿠폰을 redeem·커밋 → PG 창 닫힘/거절/금액불일치로 `confirm` 실패 시 쿠폰만 날아가고 환원 보상 없음. (실 Toss 배포에서만 노출, 데모는 단일 트랜잭션이라 무영향) |
| H3 | **Content 도메인 교회 스코핑 부재 (크로스테넌트 IDOR)** | `ContentController.java:52-80`, `ContentService.java:74-163` | Order/Worship/Payment엔 적용된 `ensureInScope` 패턴이 Content에만 누락. `CHURCH_MANAGER`가 타 교회 콘텐츠 조회·수정·**삭제(S3 객체까지)** 가능. Channel은 church_id NOT NULL로 명백히 교회 소유 모델. |
| H4 | **SMS 알림 @Transactional이 부모 트랜잭션 오염** | `SmsService.java:76,86` | `sendForOrder/sendForDonation`이 REQUIRED 전파로 주문/후원 트랜잭션에 합류 → 실 SMS 어댑터(aligo/solapi) throw 시 호출부 try/catch 무력, `UnexpectedRollbackException`으로 **결제 승인 전체 롤백**. "알림은 주문을 깨지 않는다"는 독스트링과 정반대. |
| H5 | **RDS 미암호화 + 자동백업 0일** | `deploy/terraform/main.tf:179-195` | `storage_encrypted`/`backup_retention_period` 미설정 → 기본값(미암호화·retention 0). 라이브 tfstate에서 `false`/`0` 확인. PII·금융 데이터 평문 저장 + 복구 수단 없음(skip_final_snapshot=true와 결합 시 삭제 시 영구 소실). ⚠️**in-place 전환 불가 — 스냅샷 복원 마이그레이션 필요**. |

## 2. MEDIUM (7건) — 운영 신뢰성/남용 방어

| # | 결함 | 위치 | 핵심 |
|---|------|------|------|
| M1 | **Member.pointBalance lost-update** | `Member.java:67`, `PointService.java:84-118`, `PointLedgerWriter.java:44-66` | @Version/원자적 UPDATE 없는 read-modify-write → 동시 적립(수동지급↔후원) 시 캐시 잔액이 원장 합계와 영구 분기. 재고·쿠폰은 막혔는데 포인트만 누락. |
| M2 | **애널리틱스 배치 인제스트 무제한 증폭(DoS)** | `AnalyticsPublicController.java:50-57` | 무인증 `/pub/v1/events/batch`에 리스트 크기 상한·@Valid 없음 → 레이트리밋 1토큰으로 단일 트랜잭션 수만 INSERT 강제. 단건용 토큰버킷이 배치에서 무력화. |
| M3 | **레이트리미터 XFF leftmost 신뢰 우회** | `AnalyticsPublicController.java:63-72` | `X-Forwarded-For` 최좌측(클라 제어값)을 IP 키로 사용 → 매 요청 회전으로 매번 새 60토큰 버킷 → 레이트리밋 완전 우회. Caddy/CloudFront가 append라 위조값이 leftmost로 잔존. |
| M4 | **worship/chat 공개 쓰기 레이트리밋 부재** | `WorshipPublicController.java:37-40`, `ChatController.java:44-47` | 무인증 쓰기(worship 호출당 최대 6 INSERT+SMS, chat 세션+2 INSERT)에 빈도 제한 전무. 동일 위협모델인 애널리틱스엔 방어를 뒀는데 더 무거운 이 경로엔 누락. |
| M5 | **Kakao/PayPal cancel 스텁 → 실키 시 환불 500** | `KakaoPaymentProvider.java:109`, `PayPalPaymentProvider.java:118` | `UnsupportedOperationException`(ApiException 아님)이 `handleUnexpected`로 떨어져 불투명 500. 실키 배포 시 해당 PG 환불 불가(데이터 무결성은 보존, 데모는 Mock 폴백이라 무영향). |
| M6 | **GlobalExceptionHandler DataIntegrity 핸들러 부재** | `GlobalExceptionHandler.java:52-57` | 중복키/NOT NULL/FK 위반 전용 핸들러 없어 catch 누락 경로가 raw 500. 클라가 '서버 오류'와 '입력 오류'를 구분 불가. (개별 서비스는 catch하지만 안전망 없음) |
| M7 | **EC2 루트 EBS 미암호화** | `deploy/terraform/main.tf:279-310` | `root_block_device { encrypted = true }` 없음 → 라이브 `encrypted:false`. user_data가 평문 기록한 `/etc/streamhub/api.env`(DB비번·JWT시크릿)가 미암호화 디스크에 잔존. ⚠️재생성 필요. |

## 3. LOW (8건) — 위생/정직성/접근성

| # | 결함 | 위치 | 핵심 |
|---|------|------|------|
| L1 | DeliverySyncScheduler self-invocation | `OrderService.java:269-287` | 스케줄 배송동기화가 self-call `changeStatus` → `@CacheEvict` 무력화 → 대시보드 KPI ≤60s stale(TTL로 한정). |
| L2 | TossPaymentProvider 독스트링 과장 | `TossPaymentProvider.java:18-22` | "test_sk_ 기본키로 무설정 동작" ↔ 실제 빈 기본값(미설정 시 INVALID_PARAMETER로 clean 실패). 정직성만 문제. |
| L3 | BoardDto @Valid 무력 + code 중복 가드 없음 | `BoardDto.java:16-24`, `BoardService.java:36-58` | @Valid 붙었으나 필드 제약 0, existsByCode 사전체크 없음 → null/중복 시 raw 500(관리자 전용, 무결성 보존). |
| L4 | ContentService 해시태그 비원자적 check-then-insert | `ContentService.java:180-182` | 동일 신규 태그명 동시 저장 시 unique 위반 → 콘텐츠 생성 트랜잭션 통째 500(저확률). |
| L5 | user-web viewport 핀치줌 차단 | `streamhub-user-web/src/app/layout.tsx:16-21` | `userScalable:false`+`maximumScale:1` → 저시력 사용자 확대 불가(WCAG 1.4.4). |
| L6 | PlaceholderPage 데드코드 | `streamhub-web/src/components/common/PlaceholderPage.tsx` | 어디서도 import 안 됨. CLAUDE.md '데드코드 금지' 위반. 삭제 권장. |
| L7 | ECR lifecycle policy 부재 | `deploy/terraform/main.tf:132-139` | 매 push마다 sha 태그 영구 적재 → 스토리지 단조 증가. 비용/위생. |
| L8 | 미사용 api_ingress_cidr + tfvars.example SSH 0.0.0.0/0 | `deploy/terraform/variables.tf:74-78` | 데드 변수 + example을 그대로 복사하면 SSH 전세계 개방(라이브는 /32로 잠김). |

---

## 4. 기존 인지 잔여(설계상 유지 또는 별도 트랙)
- **무료구매 데모 동작**: `app.payment.test-mode=true` 데모 한정(real 배포는 FORBIDDEN) — 의도된 체험용.
- **배포 IAM 정적키 → OIDC 미마이그레이션**(MEDIUM): 파이프라인 파손 위험으로 별도 트랙.
- **멤버 JWT localStorage**(XSS 잠재): 완화됨, 신규 결함 아님.
- **admin order/payment search churchId optional → orval 재생성 대기**: 옵셔널이라 현재 무영향.

## 5. 권고 수정 순서
1. **H1·H2·H3·H4** (앱 레벨, ddl-auto=update로 스키마 추가 안전): 쿠폰 환원·PG 쿠폰 지연·Content 스코핑·SMS 트랜잭션 분리
2. **M1~M6** (앱 레벨): 포인트 원자적 UPDATE, 레이트리밋 3종, PG cancel 에러 매핑, 전역 DataIntegrity 핸들러
3. **L1~L6** (앱/프론트): 캐시 무효화·독스트링·검증·해시태그 원자화·viewport·데드코드
4. **H5·M7·L7·L8** (인프라): ⚠️RDS/EBS 암호화는 **리소스 재생성 강제** — 라이브 DB 스냅샷 마이그레이션 전략 필요(자동 apply 금지, 사용자 결정 사항)

*검증 한계: 로컬 서버 미가동으로 정적 분석 + 적대적 코드 검증. M3·M5 등 일부는 라이브 확인 권장(needsLiveCheck).*

---

## 6. 수정 완료 (2026-06-19, 커밋 `b87e8b3` — feat/portfolio-admin-extension)

**확정 20건 전부 수정 완료. 백엔드 테스트 179→191 통과, BUILD SUCCESS, Terraform validate Success, user-web tsc 통과.**

| 항목 | 처리 |
|---|---|
| H1 쿠폰 미환원 | Order에 `couponId` 컬럼 + `CouponService.releaseRedemption`(멱등, redemption 삭제 + `decrementUsedCount` 원자적) → `OrderService.changeStatus` CANCEL/RETURN 분기에서 호출(단일 seam, 환불도 changeStatus 경유). 단위테스트 2건. |
| H2 PG 쿠폰 1단계 소진 | `prepare`는 `previewDiscount`만, 실제 redeem은 `confirm` 성공 트랜잭션으로 지연(`pendingCouponCode` 보관). 데모 단일트랜잭션 경로 원자성 유지. |
| H3 Content 크로스테넌트 IDOR | ContentController에 AdminPrincipal 주입, ContentService `isSystem()` 분기 + 매퍼 `church_id` 필터 + 소유 검증(list/detail/update/delete/listChannels). |
| H4 SMS 트랜잭션 오염 | `sendForOrder/sendForDonation` → `REQUIRES_NEW`로 독립 트랜잭션 분리(부모 결제 롤백 차단). |
| H5 RDS 미암호화/백업 | `storage_encrypted=true`, `backup_retention_period=7`, `deletion_protection=true`, `skip_final_snapshot=false` (코드만, 라이브 미적용 — 재생성 강제 주석). |
| M1 포인트 lost-update | `MemberRepository.adjustBalance` 원자적 조건부 UPDATE(`WHERE pointBalance+delta>=0`), grant/append/expire가 사용. 테스트 추가. |
| M2 배치 DoS | `EventIngestBatchRequest`(@Size 캡) + 이벤트당 토큰 비례 차감. |
| M3 XFF 우회 | 신뢰 프록시 기반 `clientIpResolver`로 교체(leftmost 무신뢰). |
| M4 worship/chat 레이트리밋 | 두 공개 쓰기에 `PublicIngestRateLimiter` 게이트(429), worship은 보수적 한도. |
| M5 PG cancel 500 | Kakao/PayPal cancel이 `ApiException(INVALID_PARAMETER)`로 명확 실패(500→4xx). |
| M6 전역 DataIntegrity 핸들러 | GlobalExceptionHandler에 DataIntegrity/Optimistic/MessageNotReadable/TypeMismatch/MissingParam → INVALID_PARAMETER(원문은 log만). |
| M7 EBS 미암호화 | `root_block_device{encrypted=true}` (코드만). |
| L1 DeliverySync 캐시 | 양 `syncDelivery`에 `@CacheEvict`(프록시 진입점). |
| L2 Toss 독스트링 | 실제(빈 기본키, 미설정 시 clean 실패)로 정정. |
| L3 BoardDto 검증 | @NotBlank/@Size/@Min/@Max + `existsByCode` 중복 가드. 테스트 5건. |
| L4 해시태그 비원자 | 충돌 catch 후 재조회(get-or-create). |
| L5 viewport 핀치줌 | `maximumScale/userScalable` 제거. |
| L6 데드코드 | PlaceholderPage.tsx 삭제. |
| L7 ECR lifecycle | `aws_ecr_lifecycle_policy`(untagged 만료 + 최근 10개). |
| L8 미사용 변수/SSH example | `api_ingress_cidr` 삭제, tfvars.example SSH를 `/32` 플레이스홀더로. |

기각 1건(S3 화이트리스트): 비공개 버킷이라 무영향 — 수정 안 함.
잔여(별도 트랙): 배포 IAM OIDC 마이그레이션, 멤버 JWT localStorage, 인프라 암호화 라이브 적용(스냅샷 마이그레이션 결정 대기).

## 7. 확인 재감사 라운드 (2026-06-19, 커밋 b87e8b3 대상 → 6c4a4cc, 테스트 191→195)

수정 커밋을 영역별로 적대적 재감사 → **11건 발견**(회귀/미완/엣지). 기능 결함 전부 수정·검증:

- **HIGH(회귀) H2 후속**: confirm에서 PG 캡처 *후* 쿠폰 redeem이 실패하면 confirm 트랜잭션 전체 롤백 → 외부 PG 캡처는 안 돌아가 "고객 청구·주문 소멸". → `CouponService.redeemInNewTransaction`(REQUIRES_NEW) + `consumePendingCoupon`이 실패 swallow(결제 PAID 유지·쿠폰 미적용·couponId null로 환불 안전).
- **HIGH(미완) M3 후속**: `ClientIpResolver` off-by-one(기본 hops 2 → 2엔트리 헤더서 index −1 → getRemoteAddr 폴백 → 전 클라 한 버킷 공유, 레이트리밋 붕괴). → 기본 hops **1**(스푸핑 3엔트리에서도 real-client 정확).
- **MED**: 레이트리미터 엔드포인트 간 버킷 공유 → analytics/chat/worship 키 네임스페이스 분리. 배치 과소청구 → `MAX_EVENTS` 200→60(capacity 정렬). 해시태그 충돌 catch가 같은 트랜잭션이라 복구 불가 → `HashtagWriter`(REQUIRES_NEW). 인프라 `deletion_protection` 하드코딩 → `protect_database` 토글(기본 false, demo destroy 가능; 암호화/백업은 무관하게 유지).
- **LOW**: Content list 비SYSTEM+null churchId → FORBIDDEN(fail-open IDOR 재개방 차단). `BoardDto.useYn @Pattern`. **`ContentServiceTest` 신규**(H3 IDOR 거부 + null-church + 매니저 스코핑 — 0 커버리지였던 HIGH 수정 회귀 방지).

수정 불요로 판정한 잔여: SMS 고아 알림 윈도우(문서화된 수용 트레이드오프), `api_ingress_cidr` 미사용 변수의 gitignore된 로컬 tfvars/README 잔재(라이브 무영향·문서 정리만).
