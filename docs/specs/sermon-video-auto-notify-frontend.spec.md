# 예배 영상 자동 업로드 + 공지 발송 — 프론트엔드/디자인 스펙

## 0. 배경 / 범위

백엔드 스펙: [`sermon-video-auto-notify-backend.spec.md`](./sermon-video-auto-notify-backend.spec.md) — **반드시 백엔드 배포 확인 후 착수**(CLAUDE.md, `npm run gen` 규칙). 이 문서는 기존 콘텐츠 등록 화면(`content/add/page.tsx`)에 "업로드 후 자동 공지 발송" 옵션 하나를 얹는 최소 변경만 다룬다.

**신규 페이지 없음, 신규 사이드바 메뉴 없음.** 기존 `/content/add` 폼에 체크박스 1개 + 미리보기 텍스트만 추가.

---

## 1. 조사로 확인된 사실

- `content/add/page.tsx`는 React Hook Form + Zod, `useContentChannels()` / `useContentCreate()`(Orval 생성 훅) 사용.
- `ThumbnailUpload.tsx`가 업로드(key/url) 처리, 폼 제출 시 `ContentCreateRequest` payload를 `useContentCreate()`에 전달하는 구조.
- `src/apis/query/`는 자동생성 — 직접 수정 금지. 백엔드에 `notifyOnPublish` 필드가 배포된 뒤 `npm run gen`을 돌려야 `ContentCreateRequest` 타입에 필드가 생긴다.

---

## 2. 화면 변경

### 2.1 생성/수정 파일 목록

**신규 (0)**

**기존 수정 (1)**
- `streamhub-web/src/app/(protected)/content/add/page.tsx` — 체크박스 필드 1개 + zod 스키마 확장 + payload 필드 추가

### 2.2 상세

**zod 스키마 delta**:
```ts
const createSchema = z.object({
  // ...기존 필드 그대로...
  notifyOnPublish: z.boolean().default(false),   // 신규
});
```

**폼 defaultValues delta**:
```ts
defaultValues: {
  // ...기존...
  notifyOnPublish: false,
},
```

**UI — 상태(status) select 바로 아래에 배치** (기존 `FIELD_CLASS` 스타일 재사용):
```tsx
<div className="flex items-center gap-2">
  <input
    type="checkbox"
    id="notifyOnPublish"
    {...register("notifyOnPublish")}
    className="h-4 w-4 rounded border-slate-300"
  />
  <label htmlFor="notifyOnPublish" className="text-sm text-slate-700">
    업로드 후 해당 교회 회원 전체에게 자동 공지 발송
  </label>
</div>
{watch("notifyOnPublish") && (
  <p className="text-xs text-slate-500">
    선택한 채널이 속한 교회의 전체 회원에게 PUSH 알림이 즉시 발송됩니다. 되돌릴 수 없습니다.
  </p>
)}
```
`watch`를 `useForm` 구조분해에 추가해야 함(`const { register, handleSubmit, control, watch, formState: { errors } } = useForm(...)`).

**payload delta** (`onSubmit` 내부, 기존 `payload` 객체에 필드 추가):
```ts
const payload: ContentCreateRequest = {
  // ...기존 필드 그대로...
  notifyOnPublish: values.notifyOnPublish,   // 신규
};
```

**제출 후 안내 메시지**: 기존 성공 메시지에 조건부 분기 추가 —
```ts
setMessage(
  values.notifyOnPublish
    ? "콘텐츠가 등록되고 공지가 발송되었습니다."
    : "콘텐츠가 등록되었습니다.",
);
```

### 2.3 디자인 노트 (신중하게 다룰 이유)

- 체크박스가 기본 **비활성(false)** — "누른 적 없는데 알림 나갔다"는 사고를 막기 위해 옵트인. 롬맵 리뷰에서 지적된 "허접한 로그인 화면" 같은 신뢰 신호 문제를 여기서 반복하지 않는다: 되돌릴 수 없는 액션 앞에는 경고 문구를 명시적으로 노출한다(위 `<p>` 문구).
- 알림 문구("새 영상이 등록되었습니다" + 콘텐츠 제목)는 백엔드에서 고정 생성 — 프론트에서 알림 문구를 커스터마이징하는 입력 필드는 이번 스코프에 없음(YAGNI, 실사용 피드백 나온 뒤 추가 여부 결정).
- 성공/실패 메시지는 기존 `message` state 패턴(`ThumbnailUpload` 옆의 에러 배너와 동일 스타일) 그대로 재사용 — 새 토스트/모달 컴포넌트 추가 안 함.

---

## 3. 구현 순서 체크리스트

- [ ] **백엔드 배포 확인** (`sermon-video-auto-notify-backend.spec.md` 완료 + 배포)
- [ ] `npm run gen` 실행 — `ContentCreateRequest.notifyOnPublish` 타입 반영 확인
- [ ] `content/add/page.tsx` — zod 스키마 + defaultValues + 체크박스 UI + payload + 성공 메시지 분기
- [ ] `npm run build:no-lint` 그린 확인
- [ ] 실기기/브라우저 QA: 체크박스 미체크 시 기존과 동일 동작(회귀 없음) 확인, 체크 시 알림 로그(`/notifications` 화면)에 발송 기록 확인

---

## 4. 위험 / 주의

- **`npm run gen` 타이밍**: 백엔드 미배포 상태에서 실행 금지(CLAUDE.md 명시) — 신규 필드 없이 gen 하면 `notifyOnPublish`가 타입에 없어 컴파일 에러.
- **되돌릴 수 없는 액션**: 알림은 발송 즉시 회원에게 도달(로그 기준)한다 — "임시저장 후 나중에 발송" 같은 예약 기능은 이번 스코프에 없음. 실수로 체크 후 제출하면 취소 불가 — §2.3의 경고 문구가 유일한 방어선.
- **1개 파일 변경**: 매우 작은 변경이라 CLAUDE.md의 "5개 이상 파일" 승인 절차 대상 아님.
