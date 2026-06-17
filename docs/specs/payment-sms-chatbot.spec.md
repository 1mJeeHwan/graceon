# C4·C6·C5 — 결제 · SMS · 챗봇 (목업/어댑터 seam) 구현 스펙

> 범위: 확장 리서치(`docs/expansion-research-and-plan.md`) §4·§5의 **C4 결제 / C6 SMS / C5 챗봇**을
> **외부 API 키 없이 "목업 + 시드"** 로 구현하되, **추후 실키 주입이 쉽도록 어댑터 seam(인터페이스 / Provider 빈 선택 / `.env` 플래그)** 을 둔다.
> 실제 외부 호출은 **하지 않는다**(테스트/데모 배지 명시). 컨벤션은 기존 `v1/order`·`v1/donation`·`v1/actionlog` 100% 준수.
>
> 정직 표기 원칙(리서치 §6): "실거래 / 실발송 아님 — 데모/테스트 모드". 기존 `Donation.testMode="Y"` + `TestModeBadge` 패턴을 그대로 확장한다.

---

## 1. 목적 / 범위 + 기존 자산 재활용 전략

### 1.1 목적
포트폴리오에서 **결제·문자·챗봇 3종 외부연동을 "실연동 직전 단계"까지** 보여준다. 핵심 셀링포인트는 *어댑터 seam 설계* — 목업 Provider를 인터페이스 뒤에 두고, `.env` 한 줄(`app.payment.provider=toss` 등)로 실 샌드박스 구현으로 교체 가능함을 코드로 증명한다.

### 1.2 재활용 / 확장 / 신규 매트릭스

| 영역 | 재사용 (그대로) | 확장 (필드/메서드 추가) | 신규 |
|---|---|---|---|
| 결제 | `ResultDTO`/`ResInfinityList`, `OrderReceipt`(kind=PAY), `OrderStatus` 상태머신, `ActionLogPublisher` | `Order`에 `pay_provider`·`pay_status` 2컬럼 추가, `OrderReceipt`에 `provider`·`txn_id` 추가 | `payment/` 패키지(`PaymentProvider` seam + 목업 4종 + `PaymentService`/`Controller`), 결제 모달 |
| SMS | `ActionLog` 패턴(SQS publish→consume→persist)의 **DB-저장 부분만**, `ResInfinityList` 검색, record 검색 DTO | 주문 상태변경/단건후원 시 자동 발송 로그 1줄(`SmsService.send(...)`) 삽입 | `sms/` 패키지(`SmsSender` seam + 목업 + `SmsMessage` 엔티티/`SmsService`/`Controller`), 커스텀 발송 화면 |
| 챗봇 | `OrderMapper.selectList`(주문조회 도구), `GoodsMapper`(상품문의), `ResultDTO` | — | `chat/` 패키지(`ChatProvider` seam + 룰베이스 목업 + `ChatService`/`Controller`), 플로팅 위젯 |
| 프론트 공통 | `TestModeBadge`, `OnceDonationModal`(모달 골격), `goods/page.tsx`(검색/페이지네이션), `FIELD_CLASS`, `SUCCESS_CODE`, `Sidebar` NAV | Sidebar에 "메시지·챗봇" 섹션 추가 | `payment/`·`sms/` 모달·페이지, `ChatWidget` |

**`.env` 플래그(전부 기본 = 목업)** — `application.yml`의 기존 `app:` 트리 아래 추가:
```yaml
app:
  payment:
    provider: ${PAYMENT_PROVIDER:mock}   # mock | toss | paypal | kakao
    test-mode: ${PAYMENT_TEST_MODE:true}
  sms:
    sender: ${SMS_SENDER:mock}           # mock | aligo | solapi
    from-number: ${SMS_FROM_NUMBER:0212345678}  # 데모용 가상 발신번호
    test-mode: ${SMS_TEST_MODE:true}
  chat:
    provider: ${CHAT_PROVIDER:rule}      # rule | llm
```

---

## 2. 데이터 모델

### 2.1 결제 — `ORDERS` 확장 + `ORDER_RECEIPT` 확장

`Order` 엔티티에 2컬럼 추가(기존 컨벤션: `@Enumerated(STRING)`, snake `@Column`):

| 필드(Java) | 컬럼 | 타입/길이 | enum/제약 | 비고 |
|---|---|---|---|---|
| `payProvider` | `pay_provider` | VARCHAR(20) | `MOCK`/`TOSS`/`PAYPAL`/`KAKAO`/`CARD` (문자열, 기존 `payMethod`와 동일 스타일) | 결제 시도된 PG |
| `payStatus` | `pay_status` | VARCHAR(12) | enum `PayStatus` | 결제 진행상태 |

`PayStatus` enum(`@Enumerated(STRING)`): `NONE`(기본) → `REQUESTED` → `APPROVED` / `FAILED` → `CANCELED`.

`OrderReceipt`(기존, kind=PAY/REFUND 재사용)에 2컬럼 추가:

| 필드 | 컬럼 | 타입 | 비고 |
|---|---|---|---|
| `provider` | `provider` | VARCHAR(20) | 영수증 발급 PG (MOCK 등) |
| `txnId` | `txn_id` | VARCHAR(60) | 목업 거래번호 `MOCK-{orderNo}-{seq}` (실키 주입 시 PG paymentKey) |

> `Order` 인덱스 추가: `@Index(name="idx_orders_pay_status", columnList="pay_status")`.
> `Order` 생성자/빌더에 `payProvider` 기본 `"MOCK"`, `payStatus` 기본 `PayStatus.NONE` 디폴트화(null-safe, 기존 `goodsTotal` 디폴트 패턴과 동일).

### 2.2 SMS — 신규 `SMS_MESSAGE`

`@Table(name="SMS_MESSAGE")`, `@NoArgsConstructor(PROTECTED)` + `@Builder`, `ActionLog`와 동형:

| 필드 | 컬럼 | 타입/길이 | enum | 비고 |
|---|---|---|---|---|
| `id` | `id` | BIGINT IDENTITY | | |
| `toNumber` | `to_number` | VARCHAR(20) NOT NULL | | **시드/입력 시 마스킹**(`010-1234-****`) |
| `content` | `content` | VARCHAR(2000) NOT NULL | | 본문 |
| `kind` | `kind` | VARCHAR(20) NOT NULL | `SmsKind` `{CUSTOM, ORDER_PAID, ORDER_SHIPPING, DONATION_ONCE}` | 발송 트리거 |
| `channel` | `channel` | VARCHAR(8) NOT NULL | `SmsChannel` `{SMS, LMS}` | 본문 길이 따라 결정(>90byte=LMS) |
| `sender` | `sender` | VARCHAR(20) NOT NULL | | 사용된 어댑터(`MOCK`/`ALIGO`/`SOLAPI`) |
| `status` | `status` | VARCHAR(12) NOT NULL | `SmsStatus` `{QUEUED, SENT, FAILED}` | 목업=항상 `SENT` |
| `testMode` | `test_mode` | CHAR(1) NOT NULL | | 항상 `"Y"`(데모) — `Donation.testMode` 컨벤션 |
| `memberId` | `member_id` | BIGINT NULL | | 연관 회원(있으면) |
| `refType` | `ref_type` | VARCHAR(30) NULL | | `ORDER`/`DONATION` |
| `refId` | `ref_id` | VARCHAR(60) NULL | | 연관 도메인 PK |
| `sentAt` | `sent_at` | DATETIME NOT NULL | | |

인덱스: `idx_sms_sent_at(sent_at)`, `idx_sms_kind(kind)`, `idx_sms_member(member_id)`.

### 2.3 챗봇 — 신규 `CHAT_SESSION` + `CHAT_MESSAGE`

세션/메시지 분리(주문조회 의도 시 회원 컨텍스트 유지용). 데모는 익명도 허용.

`CHAT_SESSION`:

| 필드 | 컬럼 | 타입 | 비고 |
|---|---|---|---|
| `id` | `id` | BIGINT IDENTITY | |
| `sessionKey` | `session_key` | VARCHAR(40) NOT NULL UNIQUE | 프론트 생성 UUID(localStorage) |
| `memberId` | `member_id` | BIGINT NULL | 로그인 시 |
| `provider` | `provider` | VARCHAR(10) NOT NULL | `RULE`/`LLM` |
| `createdAt` | `created_at` | DATETIME NOT NULL | |

`CHAT_MESSAGE`:

| 필드 | 컬럼 | 타입/길이 | enum | 비고 |
|---|---|---|---|---|
| `id` | `id` | BIGINT IDENTITY | | |
| `sessionId` | `session_id` | BIGINT NOT NULL | | FK→CHAT_SESSION |
| `role` | `role` | VARCHAR(10) NOT NULL | `ChatRole` `{USER, BOT}` | |
| `intent` | `intent` | VARCHAR(20) NULL | `ChatIntent` `{PRODUCT_INQUIRY, ORDER_LOOKUP, FAQ, FALLBACK}` | BOT 응답에만 기록 |
| `content` | `content` | VARCHAR(2000) NOT NULL | | |
| `createdAt` | `created_at` | DATETIME NOT NULL | | |

인덱스: `idx_chat_msg_session(session_id, created_at)`, `CHAT_SESSION.session_key` unique.

---

## 3. 백엔드

### 3.1 생성 / 수정 파일 목록 (정확한 패키지경로)

베이스: `streamhub-api/src/main/java/org/streamhub/api/`

**결제 — `v1/payment/`**
- `PaymentController.java`
- `PaymentService.java`
- `adapter/PaymentProvider.java` *(seam 인터페이스)*
- `adapter/PaymentRequest.java`, `adapter/PaymentResult.java` *(seam DTO record)*
- `adapter/MockPaymentProvider.java` *(@Component, 기본)*
- `adapter/TossPaymentProvider.java`, `adapter/PayPalPaymentProvider.java`, `adapter/KakaoPaymentProvider.java` *(스텁 — `@ConditionalOnProperty`, 본문 = "실키 주입 지점" 주석 + `UnsupportedOperationException`)*
- `adapter/PaymentProviderRouter.java` *(`app.payment.provider`로 빈 선택)*
- `dto/PayRequestCommand.java`, `dto/PayApproveCommand.java`, `dto/PaymentResultDto.java` *(record)*

**수정**: `v1/order/entity/Order.java`(+2컬럼/빌더), `v1/order/entity/PayStatus.java`(신규 enum, order 패키지 내), `v1/order/entity/OrderReceipt.java`(+provider/txnId), `v1/order/dto/OrderDetail.java`(+payProvider/payStatus 노출), `resources/mappers/OrderMapper.xml`(selectDetail에 `pay_provider`,`pay_status` 컬럼 추가).

**SMS — `v1/sms/`**
- `SmsController.java`, `SmsService.java`
- `adapter/SmsSender.java` *(seam)*, `adapter/SmsSendCommand.java`, `adapter/SmsSendResult.java`
- `adapter/MockSmsSender.java`(@Component 기본 — DB 로그만), `adapter/AligoSmsSender.java`, `adapter/SolapiSmsSender.java`(스텁), `adapter/SmsSenderRouter.java`
- `entity/SmsMessage.java`, `entity/SmsKind.java`, `entity/SmsChannel.java`, `entity/SmsStatus.java`
- `repository/SmsMessageRepository.java`
- `mapper/SmsMapper.java` + `resources/mappers/SmsMapper.xml`
- `dto/SmsSearchRequest.java`(record), `dto/SmsSendRequest.java`(record), `dto/SmsListItem.java`

**수정**: `v1/order/OrderService.java`(`changeStatus` PAID/SHIPPING 분기에서 `smsService.sendForOrder(...)` 호출 1줄), `v1/donation/DonationService.java`(`createOnce`에 `smsService.sendForDonation(...)`). → 둘 다 **선택적 의존성**: `SmsService`는 `MockSmsSender`만 호출하므로 외부 호출 없음.

**챗봇 — `v1/chat/`**
- `ChatController.java`, `ChatService.java`
- `adapter/ChatProvider.java` *(seam)*, `adapter/ChatReply.java`(record)
- `adapter/RuleChatProvider.java`(@Component 기본 — 룰베이스), `adapter/LlmChatProvider.java`(스텁 — "ChatProvider seam으로 LLM 주입" 주석)
- `adapter/IntentClassifier.java`(룰베이스 의도분류 유틸), `adapter/ChatProviderRouter.java`
- `entity/ChatSession.java`, `entity/ChatMessage.java`, `entity/ChatRole.java`, `entity/ChatIntent.java`
- `repository/ChatSessionRepository.java`, `repository/ChatMessageRepository.java`
- `dto/ChatSendRequest.java`(record), `dto/ChatReplyDto.java`, `dto/ChatHistoryItem.java`

**공통 수정**: `resources/application.yml`(§1.2 `app.payment/sms/chat` 트리), `base/config/PortfolioSeeder.java`(SMS/챗봇 시드 스텝, §5).

### 3.2 API 엔드포인트 표

Result: 전부 `ResultDTO<T>` 래핑. orval operationName 안정명 = 태그 + 경로 + verb.

| 메서드 | 경로 | 요청 DTO | 응답 | 권한 | operation |
|---|---|---|---|---|---|
| POST | `/v1/payment/request` | `PayRequestCommand{orderId, provider}` | `PaymentResultDto` | SYSTEM·CHURCH_MANAGER | `paymentRequest` |
| POST | `/v1/payment/approve` | `PayApproveCommand{orderId, txnId, cardNo?}` | `PaymentResultDto` | SYSTEM·CHURCH_MANAGER | `paymentApprove` |
| GET | `/v1/payment/{orderId}/receipt` | — | `OrderReceiptDto` | SYSTEM·CHURCH_MANAGER | `paymentReceipt` |
| POST | `/v1/sms/list` | `SmsSearchRequest` | `ResInfinityList<SmsListItem>` | SYSTEM·CHURCH_MANAGER | `smsList` |
| POST | `/v1/sms/send` | `SmsSendRequest{toNumber, content, memberId?}` | `SmsListItem` | SYSTEM·CHURCH_MANAGER | `smsSend` |
| POST | `/v1/chat/send` | `ChatSendRequest{sessionKey, message}` | `ChatReplyDto` | **permitAll**(공개 위젯) | `chatSend` |
| GET | `/v1/chat/{sessionKey}/history` | — | `List<ChatHistoryItem>` | permitAll | `chatHistory` |

> `/v1/chat/**`는 `SecurityConfig`의 `permitAll` 화이트리스트에 추가(공개 사용자 위젯 — 데모). 단, 주문조회 의도는 **회원 식별값(주문번호+이름)을 본문에서 받아 검증**하고 임의 조회 불가(보안 §3.5).

### 3.3 핵심 로직 의사코드

**PaymentService.request(orderId, provider)** — 결제요청(가짜):
```
order = orderRepo.findById(orderId) or NOT_FOUND
if order.payStatus in {APPROVED} -> INVALID_PARAMETER("이미 결제됨")
adapter = providerRouter.resolve(provider)          // 기본 MockPaymentProvider
result = adapter.requestPayment(PaymentRequest(order.orderNo, order.total, provider))
order.applyPayRequest(provider, result.txnId)       // payProvider=provider, payStatus=REQUESTED
orderRepo.saveAndFlush(order)
return PaymentResultDto.of(result, testMode=app.payment.test-mode)   // 항상 testMode 노출
```

**PaymentService.approve(orderId, txnId, cardNo)** — 가짜 승인 + 영수증 + 상태전이:
```
order = findById or NOT_FOUND
if order.payStatus != REQUESTED -> INVALID_PARAMETER
adapter = providerRouter.resolve(order.payProvider)
result = adapter.approve(PaymentRequest(...), txnId, maskCard(cardNo))  // MOCK: 200 OK, 항상 승인
order.applyPayApprove()                              // payStatus=APPROVED
// 기존 주문 상태머신 재사용: PLACED -> PAID 전이 + 재고차감 + PAY 영수증
orderService.changeStatus(orderId, new OrderStatusChangeRequest(PAID, "결제승인(MOCK)"))
receipt = orderReceiptRepo.latestPay(orderId); receipt.setProviderTxn(order.payProvider, txnId)
smsService.sendForOrder(order, ORDER_PAID)           // 자동 SMS 로그
actionLogPublisher.publish("PAYMENT_APPROVE","ORDER",orderId,order.payProvider)
return PaymentResultDto.approved(...)
```
> 카드번호는 **저장하지 않음** — `maskCard()`로 `**** **** **** 1234`만 영수증 memo에 기록(PCI 회피, 데모 명시).

**SmsService.send(cmd)** / **sendForOrder/sendForDonation**:
```
channel = content.byteLenEUCKR() > 90 ? LMS : SMS
sender = senderRouter.resolve()                      // 기본 MockSmsSender
res = sender.send(SmsSendCommand(maskNumber(to), content, channel))  // MOCK: 외부호출 X, 즉시 SENT
save SmsMessage(to=masked, content, kind, channel, sender=res.sender, status=SENT, testMode="Y", ...)
return SmsListItem.from(saved)
```
- 자동 알림 본문 템플릿(상수): `ORDER_PAID` = `"[StreamHub] {orderNo} 결제가 완료되었습니다. (테스트발송)"`, `ORDER_SHIPPING` = 운송장 안내, `DONATION_ONCE` = 후원 영수.
- **OrderService/DonationService에서의 호출은 best-effort**(try/catch 후 log.warn) — `ActionLogPublisher`와 동일 철학: 알림 실패가 본 거래를 깨지 않음.

**ChatService.send(sessionKey, message)** — 룰베이스 목업:
```
session = sessionRepo.findByKey(sessionKey) ?? create(sessionKey, provider=RULE)
save ChatMessage(USER, message)
provider = chatRouter.resolve()                      // 기본 RuleChatProvider
intent = IntentClassifier.classify(message)          // 키워드 룰: "주문/배송/조회"->ORDER_LOOKUP, "상품/가격/재고"->PRODUCT_INQUIRY, FAQ사전 매칭->FAQ, else FALLBACK
reply = switch(intent):
  ORDER_LOOKUP   -> 주문번호 패턴(YYYYMMDD-…) 추출 시 orderMapper로 상태 1건 조회, 없으면 "주문번호와 주문자명을 알려주세요"
  PRODUCT_INQUIRY-> goodsMapper 키워드 top3 카드 텍스트(상품명/가격/재고)
  FAQ            -> FAQ_TABLE에서 best-match 답변
  FALLBACK       -> 안내 + 빠른답변 버튼 3종(quickReplies)
save ChatMessage(BOT, reply.text, intent)
return ChatReplyDto(reply.text, intent, reply.quickReplies, testMode=true)  // "데모 챗봇" 표기
```
> `IntentClassifier`는 소문자화 + 키워드 셋 매칭(테이블 기반 — 복잡 비즈니스로직이므로 단위테스트 작성). `LlmChatProvider`는 동일 `ChatProvider` 인터페이스만 구현하면 `app.chat.provider=llm`으로 교체.

### 3.4 외부연동 어댑터 seam 설계

**공통 패턴(3종 동일)**: `interface XxxProvider` → `MockXxx`(@Component, 기본·외부호출 X) + 실 Provider 스텁(`@ConditionalOnProperty(name="app.X.provider", havingValue="...")`) → `XxxRouter`가 `Map<String,XxxProvider>` 주입받아 `app.X.provider` 값으로 선택. **실키 주입 지점 = 스텁 클래스의 `// TODO(실키): ...` 1곳 + `application-prod.yml` 환경변수.**

```java
// v1/payment/adapter/PaymentProvider.java
public interface PaymentProvider {
    String code();                                   // "MOCK" | "TOSS" | ...
    PaymentResult requestPayment(PaymentRequest req);
    PaymentResult approve(PaymentRequest req, String txnId, String maskedCard);
}

// MockPaymentProvider — 외부 호출 없음. 200 응답 즉시 생성.
@Component
public class MockPaymentProvider implements PaymentProvider {
    public String code() { return "MOCK"; }
    public PaymentResult requestPayment(PaymentRequest r) {
        return PaymentResult.requested("MOCK-" + r.orderNo() + "-" + seq());   // 가짜 txnId
    }
    public PaymentResult approve(PaymentRequest r, String txnId, String maskedCard) {
        return PaymentResult.approved(txnId, r.amount(), "MOCK 승인(실거래 아님)");
    }
}

// TossPaymentProvider — 실키 주입 지점(스텁).
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "toss")
public class TossPaymentProvider implements PaymentProvider {
    @Value("${app.payment.toss.secret-key:}") private String secretKey;  // ← 실 test_ck_ 주입점
    public String code() { return "TOSS"; }
    public PaymentResult approve(...) {
        // TODO(실키): RestClient POST https://api.tosspayments.com/v1/payments/confirm
        //   Authorization: Basic base64(secretKey:) — 샌드박스 test_ck_ 키로 즉시 동작.
        throw new UnsupportedOperationException("실 PG 미연동(데모) — app.payment.provider=mock 사용");
    }
}
```
- **SMS seam**: `SmsSender{ send(SmsSendCommand) → SmsSendResult }`. `AligoSmsSender`/`SolapiSmsSender` 스텁의 TODO에 각 API 엔드포인트(`https://apis.aligo.in/send/`, SOLAPI `/messages/v4/send`)와 **발신번호 사전등록 필수** 주석.
- **Chat seam**: `ChatProvider{ reply(ChatContext) → ChatReply }`. `LlmChatProvider` TODO에 "Anthropic/OpenAI Tool Calling으로 orderMapper/goodsMapper를 tool 노출" 주석.

### 3.5 보안 / 검증 (CLAUDE.md 준수)
- 모든 입력 검증: `@Valid` + record + Bean Validation(`@NotBlank toNumber`, `@Positive amount`는 결제엔 없음 — total은 서버계산).
- **금액은 클라이언트 신뢰 금지**: 결제 금액은 항상 `order.total`(서버) 사용, 요청 본문엔 금액 없음.
- 챗봇 주문조회: `orderNo + orderedName` 동시 일치만 1건 노출(타 회원 주문 열람 차단). 본문은 `MyBatis #{}` 파라미터 바인딩(문자열 연결 금지).
- 카드번호: 미저장, 마스킹만. SMS 수신번호: 마스킹 저장.
- `crypto`/난수 불요(목업 txnId는 시퀀스+orderNo 결정값).

---

## 4. 프론트 (streamhub-web)

베이스: `streamhub-web/src/`

### 4.1 파일 목록

**결제**
- `app/(protected)/order/[id]/page.tsx` *(수정)* — "결제하기" 버튼 + `PaymentModal` 연결, `payProvider`/`payStatus` 표시.
- `components/payment/PaymentModal.tsx` *(신규)* — 수단 선택 라디오(MOCK/카드/토스/PayPal/카카오) + 테스트 카드번호 입력 + `TestModeBadge`.
- `components/payment/PayStatusBadge.tsx` *(신규)*.
- `lib/payment-form.ts` *(신규)* — Zod 스키마(`provider`, `cardNo` 16자리 `4242 4242 4242 4242` 테스트값 placeholder).

**SMS**
- `app/(protected)/sms/page.tsx` *(신규)* — 발송내역 목록(검색/페이지네이션, `goods/page.tsx` 골격 재사용) + "문자 발송" 버튼.
- `components/sms/SmsSendModal.tsx` *(신규)* — 수신자(회원 선택 or 직접입력)·본문(글자수/SMS·LMS 표시)·`TestModeBadge`.
- `components/sms/SmsGrid.tsx` *(신규, AG Grid)*, `components/sms/SmsKindBadge.tsx`.
- `lib/sms-form.ts` *(신규)* — Zod(`toNumber` 정규식, `content` 1~2000).

**챗봇**
- `components/chat/ChatWidget.tsx` *(신규)* — 우하단 플로팅 버튼+패널.
- `components/chat/ChatBubble.tsx`, `components/chat/QuickReplies.tsx` *(신규)*.
- `lib/chat-session.ts` *(신규)* — `sessionKey` localStorage 생성/조회(`crypto.randomUUID()`).
- `app/(protected)/layout.tsx` *(수정)* — `<ChatWidget />` 마운트(보호 셸 전역). 데모 상품문의/주문조회/FAQ.

**공통**
- `components/layout/Sidebar.tsx` *(수정)* — `NAV_SECTIONS`에 섹션 추가:
  ```
  { title: "메시지·결제", items: [
      { label: "문자 발송", href: "/sms", icon: MessageSquare },
  ]}
  ```
  (결제는 주문상세 내 모달이므로 별도 nav 없음, 챗봇은 플로팅.)

> orval 자동생성: 백엔드 배포 후 `npm run gen` 으로 `src/apis/query/payment/`·`sms/`·`chat/` 생성(직접 작성 금지 — CLAUDE.md 대량변경 안전규칙).

### 4.2 화면별 컴포넌트 · 데이터패칭 · 폼

| 화면 | 컴포넌트 | 데이터패칭 | 폼 |
|---|---|---|---|
| 주문상세 결제 | `PaymentModal` | `usePaymentRequest`/`usePaymentApprove`(mutation), 성공 시 `useOrderDetail` invalidate | RHF+Zod(`payment-form.ts`), 수단 라디오·테스트카드 |
| 문자 목록 | `SmsGrid`(AG Grid, `ssr:false` dynamic) | `useQuery(["sms-list", req], smsList, keepPreviousData)` — `goods/page.tsx` 동일 패턴 | 검색바(kind/기간/keyword) |
| 문자 발송 | `SmsSendModal` | `useSmsSend` mutation → 목록 refetch | RHF+Zod, `memberList`로 수신자 picker(`OnceDonationModal` 재사용) |
| 챗봇 위젯 | `ChatWidget` | `useChatSend` mutation + `useChatHistory`(GET) | 단순 input(엔터 전송), `QuickReplies` 버튼 |

### 4.3 키 불필요 기술 선택
- **결제**: 외부 PG SDK 미로드(목업) — 순수 React 모달. 테스트 카드번호는 토스 공식 테스트값(`4242…`) placeholder만, 실호출 없음.
- **SMS 글자수/채널 판정**: 클라 EUC-KR 바이트 추정(`new Blob` + 한글 2byte 근사)로 SMS(≤90)·LMS 라벨 — 외부 라이브러리 불요.
- **챗봇**: 외부 위젯/Dialogflow 불요 — 자체 `ChatWidget` + 백엔드 룰베이스. (실키 단계에서만 LLM.)
- (참고: 본 도메인엔 지도/오디오/우편번호 불필요. 해당 키-불요 기술 Leaflet·HTML5 audio·다음우편번호는 C1~C3 스펙 소관.)

---

## 5. 시드 (결정론적 · 마스킹 · 가상)

`PortfolioSeeder`에 스텝 추가(기존 패턴: 고정시드 `Random`, 멱등 — 테이블 비었을 때만, `@Order(2)` 이후 실행).

- **SMS_MESSAGE (~120건)** `SEED_SMS=1006L`:
  - 기존 시드 주문(PAID/SHIPPING)·단건후원에서 결정론적으로 파생: `ORDER_PAID`/`ORDER_SHIPPING`/`DONATION_ONCE` kind 분배.
  - 추가로 `CUSTOM` 공지 ~20건(예: "[StreamHub] 부활절 특별예배 안내 (테스트발송)").
  - **수신번호 전부 마스킹 가상값**: `010-{seed3}-****`. `testMode="Y"`, `sender="MOCK"`, `status="SENT"`, `sentAt`은 연관 주문 `orderedAt` 기준.
- **결제 반영(시드 주문)**: 기존 PAID 이상 주문에 `payProvider="MOCK"`, `payStatus=APPROVED`, 해당 PAY 영수증에 `provider="MOCK"`, `txnId="MOCK-{orderNo}-1"` 백필(기존 주문 시드 루프 내 1줄).
- **CHAT_SESSION/CHAT_MESSAGE (~8세션, 세션당 4~6턴)** `SEED_CHAT=1007L`:
  - 데모 대화 3시나리오 결정론 생성: 상품문의("찬양 앨범 재고 있나요?"→PRODUCT_INQUIRY), 주문조회(시드 `orderNo` 사용→ORDER_LOOKUP), FAQ("배송비 얼마예요?"→FAQ). `provider="RULE"`.
- **FAQ 테이블**: DB 아님 — `RuleChatProvider` 내 정적 상수 배열(질문 키워드→답변). 배송/환불/회원/예배시간 등 8개.

---

## 6. 구현 순서 체크리스트

- [ ] **B1** `application.yml`에 `app.payment/sms/chat` 플래그 추가.
- [ ] **B2** `Order`(+payProvider/payStatus/`PayStatus` enum/인덱스), `OrderReceipt`(+provider/txnId), `OrderDetail`/`OrderMapper.xml` 갱신. → `mvn test` 기존 주문 테스트 그린 확인.
- [ ] **B3** `v1/payment/` seam(인터페이스+Mock+스텁3+Router) → `PaymentService`(request/approve) → `PaymentController`. 단위테스트: approve가 PAID 전이+영수증 발급.
- [ ] **B4** `v1/sms/` 엔티티/enum/repo/mapper+XML → `SmsSender` seam+Mock → `SmsService`(send/sendForOrder/sendForDonation) → `SmsController`. 단위테스트: channel 판정(SMS/LMS) 테이블테스트.
- [ ] **B5** `OrderService.changeStatus`·`DonationService.createOnce`에 best-effort SMS 호출 삽입(try/catch).
- [ ] **B6** `v1/chat/` 엔티티/repo → `IntentClassifier`(테이블테스트) → `RuleChatProvider`/`LlmChatProvider` 스텁 → `ChatService` → `ChatController`. `SecurityConfig` `/v1/chat/**` permitAll.
- [ ] **B7** `PortfolioSeeder` SMS/챗봇/결제백필 스텝(멱등·결정론).
- [ ] **검증** `make fmt && make test && make lint`(전부 그린) → API 서버 기동 → Swagger 확인.
- [ ] **F1** 백엔드 배포 확인 후 `npm run gen`(orval).
- [ ] **F2** `lib/payment-form.ts` + `PaymentModal`/`PayStatusBadge` + 주문상세 결제 버튼.
- [ ] **F3** `app/(protected)/sms/page.tsx` + `SmsGrid`/`SmsSendModal`/`SmsKindBadge` + Sidebar nav.
- [ ] **F4** `ChatWidget`/`ChatBubble`/`QuickReplies` + `lib/chat-session.ts` + `(protected)/layout.tsx` 마운트.
- [ ] **F5** `npm run build:no-lint` + 데모 QA(결제 모달 승인→영수증, 문자발송→목록, 챗봇 3시나리오).

---

## 7. "데모/테스트 모드" 정직 표기 위치

| 위치 | 표기 |
|---|---|
| `PaymentModal` 헤더 | `<TestModeBadge />` + "실 PG 미연동 — 가짜 승인(테스트)" 캡션 |
| `PaymentResultDto.testMode` | API 응답에 `testMode:true` 항상 포함 |
| 주문상세 결제 영역 / `PayStatusBadge` | "MOCK" provider 시 배지 색 amber + "테스트" 라벨 |
| 영수증 memo | `"MOCK 승인(실거래 아님)"`, 카드 마스킹 |
| `SmsSendModal` 헤더 & 발송 버튼 | `<TestModeBadge />` + "실제 발송되지 않습니다(로그만 저장)" |
| `SmsMessage.testMode="Y"` / `SmsGrid` 컬럼 | 모든 행에 "테스트" 배지 |
| `ChatWidget` 헤더 | "데모 챗봇 · 룰베이스(실 LLM 미연동)" 서브타이틀 |
| `ChatReplyDto.testMode` | 응답에 `testMode:true` |
| 코드 주석 | 각 실Provider 스텁의 `// TODO(실키)` + `UnsupportedOperationException("…데모…")` |
| `expansion-research-and-plan.md` §6 | 본 스펙이 그 정직표기 원칙의 구현체임을 상호참조 |
