# 그누보드5(영카트5) 관리자 — 기능 구현 명세서

> 출처: `https://demo.sir.kr/gnuboard5/adm/` 관리자 데모를 화면 단위로 정밀 스캔(폼 필드 / 테이블 컬럼 / 액션 버튼 / 검색조건)하여 작성.
> 목적: 데모 사이트에 동일 기능을 **재구현**하기 위한 화면·데이터·동작 명세.
> 범위: 7개 주메뉴 / 58개 기능 화면.

---

## 0. 공통 규약 (모든 화면 공통)

### 0.1 권한 모델
- 회원 권한 레벨 `mb_level` = **1~10** 정수. 10이 최고. 게시판·분류별로 "보기/쓰기/댓글/다운로드" 최소 레벨을 따로 지정.
- **최고관리자(super admin)**: 환경설정의 단일 지정 계정. 기본환경설정(`config_form.php`), 게시판그룹 추가(`boardgroup_form.php`) 등 일부 화면은 최고관리자 전용 → 그 외 관리자는 `"최고관리자만 접근 가능합니다."` 차단.
- 데모 로그인 계정 `youngcart5`는 일반 관리자 → 위 2개 화면만 차단되고 나머지는 모두 접근 가능.

### 0.2 공통 리스트 화면 패턴
거의 모든 목록 화면이 동일 골격을 가짐. 구현 시 **공통 컴포넌트 1개**로 재사용 권장:
1. **검색 폼**: `검색대상(select, sfl)` + `검색어(text, stx)` + `[검색]` 버튼. (날짜 범위형은 `fr_date`/`to_date`.)
2. **목록 테이블**: 맨 앞 열에 `전체선택 체크박스(chkall)` + 행별 `chk[]`.
3. **인라인 일괄수정**: 행마다 입력/셀렉트가 들어가 `[선택수정]`/`[일괄수정]`으로 한번에 저장(배열 전송 `name[]`).
4. **일괄삭제**: 체크 후 `[선택삭제]`.
5. **페이징**: 페이지당 행 수 설정값 기준.

### 0.3 주요 DB 테이블(그누보드 표준, 접두어 `g5_`)
| 도메인 | 테이블 |
|---|---|
| 회원 | `g5_member`, `g5_member_social_profiles` |
| 포인트 | `g5_point` |
| 게시판 설정 | `g5_board`, `g5_group`, `g5_board_group_member` |
| 게시글 | `g5_write_{bo_table}` (게시판별 동적 테이블), 댓글도 동일 테이블 |
| 설정 | `g5_config`, `g5_content`, `g5_menu`, `g5_poll`, `g5_poll_etc` |
| 접속로그 | `g5_visit`, `g5_visit_sum`, `g5_popular` |
| QA | `g5_qa_config`, `g5_qa_content` |
| 쇼핑몰 | `g5_shop_item`, `g5_shop_category`, `g5_shop_item_option`, `g5_shop_order`, `g5_shop_cart`, `g5_shop_order_receipt`, `g5_shop_coupon`, `g5_shop_couponzone`, `g5_shop_sendcost`, `g5_shop_item_qa`, `g5_shop_item_use`, `g5_shop_event`, `g5_shop_banner` |
| SMS | `g5_sms5_*` (config/list/log/group/form/book) |

---

# 1. 환경설정

## 1.1 기본환경설정 `config_form.php` ⚠️최고관리자 전용
- **목적**: 사이트 전역 설정. (데모 계정 권한으로 폼 미노출 — 그누보드 표준 `g5_config` 기준으로 구현)
- **주요 항목(표준 스키마)**: 사이트 제목(`cf_title`), 관리자 계정(`cf_admin`), 회원가입 정책(가입축하 포인트 `cf_register_point`·추천 포인트 `cf_recommend_point`·메일인증 `cf_use_email_certify`), 첨부 제한(`cf_filter`·`cf_image_extension`), 글쓰기 포인트 기본값, 에디터 선택, 캡차, 가입 약관(`cf_stipulation`/`cf_privacy`), 메일발송 SMTP, 외부 로그인(소셜), 헤더/푸터/메인 HTML.
- **저장 단위**: `g5_config` 단일 행 UPDATE.
- **구현 노트**: 권한 가드 필수(최고관리자만). 다른 모든 화면이 이 설정값을 참조하므로 1순위 구현.

## 1.2 테마설정 `theme.php`
- **목적**: 설치된 테마 목록을 보여주고 선택·적용. 테마 스크린샷·정보 표시, [적용]/[미리보기].
- **DB**: `g5_config.cf_theme`.
- **구현 노트**: `/theme/{name}/theme.info` 메타 파싱하여 카드형으로 출력.

## 1.3 메뉴설정 `menu_list.php`
- **목적**: 사이트 상단/메인 메뉴를 1·2depth로 구성.
- **필드(행 배열)**: `me_name[]`(메뉴명·필수), `me_link[]`(링크·필수), `me_target[]`(새창: 사용안함/사용함), `me_order[]`(순서), `me_use[]`(PC사용), `me_mobile_use[]`(모바일사용).
- **테이블 컬럼**: 메뉴 / 링크 / 새창 / 순서 / PC사용 / 모바일사용 / 관리.
- **액션**: [확인](일괄저장). DB: `g5_menu`.
- **구현 노트**: 부모-자식(me_code 2자리+2자리) 구조. 드래그 정렬 + 인라인 추가/삭제.

## 1.4 메일 테스트 `sendmail_test.php`
- **필드**: `email`(받는 메일주소·필수). **액션**: [발송].
- **목적**: SMTP 설정 정상 여부를 테스트 메일로 확인. 결과 메시지 표시.

## 1.5 Browscap 업데이트 `browscap.php` / 접속로그 변환 `browscap_convert.php`
- **목적**: 브라우저/OS 통계용 browscap 데이터 갱신 및 기존 접속로그 재분석.
- **구현 노트**: 데모 클론에서는 우선순위 낮음(통계 부가기능). UA 파서 라이브러리로 대체 가능.

## 1.6 부가서비스 `service.php`
- **목적**: 외부 연동 부가서비스 안내/관리(정보성 화면).

---

# 2. 회원관리

## 2.1 회원관리 `member_list.php`
- **검색**: `sfl`(회원아이디/닉네임/이름/권한/E-MAIL/전화번호/휴대폰번호/포인트/가입일시/IP/추천인) + `stx`.
- **목록 인라인 수정 필드(행별)**: `mb_certify[]`(본인확인 radio), `mb_open[]`(정보공개), `mb_mailling[]`(메일수신), `mb_sms[]`(SMS수신), `mb_adult[]`(성인인증), `mb_intercept_date[]`(접근차단), `mb_level[]`(권한 1~10 select).
- **테이블 컬럼**: 전체선택 / 아이디 / 이름 / 닉네임 / 본인확인 / 메일인증 / 정보공개 / 광고성이메일수신 / 광고성SMS수신 / 상태 / 성인인증 / 접근차단 / 휴대폰 / 권한 / 전화번호 / 최종접속 / 가입일 / 접근그룹 / 포인트 / 관리.
- **액션**: [검색] [선택수정] [선택삭제]. DB: `g5_member`.
- **상세/수정 화면**(`member_form.php`): 아이디·비밀번호·이름·닉네임·이메일·전화/휴대폰·주소(우편번호+카카오주소)·생일·성별·홈페이지·서명·자기소개·추천인·여분필드(mb_1~10)·메일/SMS수신·차단일·탈퇴일·포인트.
- **구현 노트**: 비번은 해시(그누보드는 자체 password_hash). 탈퇴=`mb_leave_date`, 차단=`mb_intercept_date` 세팅(삭제 아님).

## 2.2 회원메일발송 `mail_list.php`
- **목록**: 번호 / 제목 / 작성일시 / [테스트] / [보내기] / [미리보기], 행별 `chk[]`, [선택삭제].
- **작성 화면**: 수신대상(회원레벨/메일수신동의자 필터), 제목, 내용(에디터), 첨부. 대량 발송은 큐/배치 처리.
- **구현 노트**: 광고성 메일은 `mb_mailling=1` 동의자만. 발송 이력 보관.

## 2.3 접속자집계 `visit_list.php`
- **검색**: `fr_date`~`to_date`(기간).
- **컬럼**: IP / 접속경로(referer) / 브라우저 / OS / 접속기기 / 일시. DB: `g5_visit`.

## 2.4 접속자검색 `visit_search.php`
- **검색**: `sfl`(IP/접속경로/날짜) + `stx`. 컬럼은 2.3과 동일.

## 2.5 포인트관리 `point_list.php`
- **검색**: `sfl`(회원아이디/내용) + `stx`.
- **수동 지급 폼**: `mb_id`(회원아이디·필수), `po_content`(내용·필수), `po_point`(증감 포인트·필수, 음수 가능), `po_expire_term`(유효기간).
- **컬럼**: 전체선택 / 회원아이디 / 이름 / 닉네임 / 포인트 내용 / 포인트 / 일시 / 만료일 / 포인트합. [선택삭제] [확인(지급)]. DB: `g5_point`(+ `g5_member.mb_point` 합계 동기화).
- **구현 노트**: 지급/차감 시 회원 누적포인트 갱신 트랜잭션. 만료 포인트 배치.

## 2.6 투표관리 `poll_list.php`
- **검색**: `sfl`(제목) + `stx`.
- **컬럼**: 전체선택 / 번호 / 제목 / 투표권한 / 투표수 / 기타의견 / 사용 / 관리. [선택삭제].
- **추가/수정 화면**(`poll_form.php`): 제목, 투표권한 레벨, 보기항목 po_poll1~9, 기타의견 사용여부. DB: `g5_poll`, `g5_poll_etc`.

---

# 3. 게시판관리

## 3.1 게시판관리 `board_list.php`
- **검색**: `sfl`(TABLE/제목/그룹ID) + `stx`.
- **인라인 수정 필드(행별)**: `bo_skin[]`/`bo_mobile_skin[]`(스킨 select), `bo_subject[]`(제목), `bo_read_point[]`/`bo_write_point[]`/`bo_comment_point[]`/`bo_download_point[]`(포인트), `bo_use_sns[]`, `bo_use_search[]`, `bo_order[]`(출력순서), `bo_device[]`(모두/PC/모바일).
- **컬럼**: 전체선택 / 그룹 / TABLE / 스킨 / 모바일스킨 / 제목 / 읽기·쓰기·댓글·다운 포인트 / SNS사용 / 검색사용 / 출력순서 / 접속기기 / 관리. [검색] [선택수정]. DB: `g5_board`.

## 3.2 게시판 추가/수정 `board_form.php` (핵심·대형 폼)
한 게시판의 모든 설정. **그룹적용/전체적용 체크박스**(`chk_grp_*`/`chk_all_*`)가 거의 모든 항목에 붙어 "이 값을 같은 그룹/전체 게시판에 일괄 복사" 기능 제공.

- **기본**: `bo_table`(영문 테이블ID·필수), `gr_id`(그룹 select·필수), `bo_subject`/`bo_mobile_subject`(제목), `bo_device`(PC·모바일/PC전용/모바일전용), `bo_category_list`+`bo_use_category`(분류 사용), `bo_admin`(게시판 관리자).
- **권한(1~10 select)**: `bo_list_level`(목록), `bo_read_level`(읽기), `bo_write_level`(쓰기), `bo_reply_level`(답변), `bo_comment_level`(댓글), `bo_link_level`(링크), `bo_upload_level`(업로드), `bo_download_level`(다운로드), `bo_html_level`(HTML).
- **기능 토글(checkbox)**: `bo_count_modify`/`bo_count_delete`(원글 수정·삭제 가능 댓글수), `bo_use_sideview`(글쓴이 사이드뷰), `bo_use_secret`(비밀글: 사용안함/체크박스/무조건), `bo_use_dhtml_editor`+`bo_select_editor`(에디터), `bo_use_rss_view`, `bo_use_good`(추천), `bo_use_nogood`(비추천), `bo_use_name`(실명), `bo_use_signature`(서명), `bo_use_ip_view`, `bo_use_list_content`(목록에 내용), `bo_use_list_file`, `bo_use_list_view`, `bo_use_email`, `bo_use_cert`(본인확인: 사용안함/회원전체/성인만), `bo_use_sns`, `bo_use_search`, `bo_use_captcha`.
- **업로드**: `bo_upload_count`(개수), `bo_upload_size`(용량 byte), `bo_use_file_content`(파일설명).
- **글수 제한**: `bo_write_min`/`bo_write_max`, `bo_comment_min`/`bo_comment_max`.
- **디자인/양식**: `bo_skin`/`bo_mobile_skin`(스킨), `bo_insert_content`(글쓰기 기본내용 textarea), `bo_subject_len`/`bo_mobile_subject_len`(제목 길이), `bo_page_rows`/`bo_mobile_page_rows`(페이지당 목록 수), `bo_gallery_cols`/`bo_gallery_width`/`bo_gallery_height`(+모바일), `bo_table_width`, `bo_image_width`, `bo_new`(새글 아이콘 시간), `bo_hot`(인기글 기준 조회수), `bo_reply_order`(답변 정렬), `bo_sort_field`(리스트 정렬 필드: wr_datetime/wr_hit/wr_comment/wr_good/wr_subject asc·desc 등 다수).
- **포인트**: `bo_read_point`/`bo_write_point`/`bo_comment_point`/`bo_download_point`.
- **여분필드**: `bo_1_subj`/`bo_1` ~ `bo_10_subj`/`bo_10`(라벨+값 10쌍).
- **액션**: [확인]. **DB**: `g5_board` 1행 + 신규 시 `g5_write_{bo_table}` 동적 테이블 CREATE.
- **구현 노트**: "그룹적용/전체적용" 처리가 핵심 — 체크된 항목만 다른 게시판으로 UPDATE 전파. 신규 게시판은 글 저장용 테이블을 동적 생성(또는 단일 테이블+bo_table 컬럼으로 대체 설계 가능).

## 3.3 게시판그룹관리 `boardgroup_list.php` (+추가 `boardgroup_form.php` ⚠️최고관리자)
- **검색**: `sfl`(제목/ID/그룹관리자) + `stx`.
- **인라인 필드**: `gr_subject[]`(제목), `gr_use_access[]`(접근회원 사용), `gr_order[]`(메인메뉴 출력순서), `gr_device[]`.
- **컬럼**: 전체선택 / 그룹아이디 / 제목 / 그룹관리자 / 게시판수 / 접근사용 / 접근회원수 / 출력순서 / 접속기기 / 관리. [검색][선택수정][선택삭제]. DB: `g5_group`.

## 3.4 인기검색어관리 `popular_list.php`
- **검색**: `sfl`(검색어/등록일) + `stx`. **컬럼**: 전체선택 / 검색어 / 등록일 / 등록IP. DB: `g5_popular`.

## 3.5 인기검색어순위 `popular_rank.php`
- **검색**: `fr_date`~`to_date`. **컬럼**: 순위 / 검색어 / 검색회수.

## 3.6 1:1문의설정 `qa_config.php`
- **필드**: `qa_title`(타이틀), `qa_category`(분류·줄바꿈 구분), `qa_skin`/`qa_mobile_skin`(스킨), `qa_use_email`+`qa_req_email`(이메일 보이기/필수), `qa_use_hp`+`qa_req_hp`(휴대폰), `qa_use_sms`+`qa_send_number`+`qa_admin_hp`(SMS알림), `qa_admin_email`, `qa_use_editor`, `qa_subject_len`/`qa_mobile_subject_len`, `qa_page_rows`/`qa_mobile_page_rows`, `qa_image_width`, `qa_upload_size`, `qa_include_head`/`qa_include_tail`(상·하단 파일경로), `qa_content_head`/`qa_content_tail`(+모바일, textarea), `qa_insert_content`, 여분필드 `qa_1_subj`/`qa_1` ~ `qa_5`.
- **액션**: [확인]. DB: `g5_qa_config` 1행. 실제 문의글은 `g5_qa_content`.

## 3.7 내용관리 `contentlist.php` (+ `content_form.php`)
- **목록 컬럼**: ID / 제목 / 관리. 약관·이용안내 등 정적 페이지(CMS) 관리.
- **추가/수정**: `co_id`(영문ID), `co_subject`(제목), `co_content`(PC 내용 에디터), `co_mobile_content`, 상·하단 include, 스킨. DB: `g5_content`. 프론트는 `/bbs/content.php?co_id=` 로 출력.

## 3.8 글,댓글 현황 `write_count.php`
- **목적**: 게시판별 글/댓글 작성 통계(기간별 집계 표/그래프). 읽기 전용.

---

# 4. 쇼핑몰관리 (영카트)

## 4.1 쇼핑몰설정 `shop_admin/configform.php` (대형 폼, `g5_shop_default` 1행)
- **사업자정보**: `de_admin_company_name`(회사명), `de_admin_company_saupja_no`(사업자등록번호), `de_admin_company_owner`(대표자), `de_admin_company_tel`/`de_admin_company_fax`, `de_admin_tongsin_no`(통신판매업 신고번호), `de_admin_buga_no`(부가통신 사업자번호), `de_admin_company_zip`/`de_admin_company_addr`(사업장주소), `de_admin_info_name`/`de_admin_info_email`(정보관리책임자).
- **스킨설정**: `de_shop_skin`(PC), `de_shop_mobile_skin`(모바일).
- **초기화면 설정(PC 5블록·모바일 3블록)**: 블록마다 `de_type{n}_list_use`(출력 여부), `de_type{n}_list_skin`(메인 스킨 main.10~50.skin.php), `de_type{n}_list_mod`(1줄당 이미지 수), `de_type{n}_list_row`(줄 수), `de_type{n}_img_width`/`de_type{n}_img_height`. 모바일은 `de_mobile_type{n}_*`. 블록은 히트/추천/신상품/인기/할인 상품군에 매핑.
- **결제설정**: 무통장입금 사용, 계좌정보, PG사 연동키(이니시스/KCP/나이스/토스 등), 가상계좌, 적립금/예치금 사용.
- **배송설정**: 배송업체, 기본 배송비, 무료배송 기준금액, 지역별 추가배송비 사용, 도서산간.
- **기타설정**: 관련상품 출력, 재고관리 방식, 상품평/문의 사용, 미성년 결제 등.
- **SMS설정**: 주문/입금/배송 알림 SMS 사용 여부 및 템플릿.
- **액션**: [확인]. **구현 노트**: 항목이 매우 많아 **탭/아코디언 섹션**으로 분리 권장(사업자/스킨/메인노출/결제/배송/기타/SMS).

## 4.2 주문내역 `shop_admin/orderlist.php` (커머스 핵심)
- **검색**: `sel_field`(주문번호/회원ID/주문자/주문자전화/주문자핸드폰/받는분/받는분전화/받는분핸드폰/입금자/운송장번호) + `search`.
- **상태 필터(radio)**: `od_status`(전체/주문/입금/준비/배송/완료/취소/반품/품절), `od_settle_case`(결제수단: 전체/무통장/카드/...).
- **부가 필터(checkbox)**: `od_misu`(미수금), `od_cancel_price`(반품·품절), `od_refund_price`(환불), `od_receipt_point`(포인트주문), `od_coupon`(쿠폰). + 기간 `fr_date`~`to_date`.
- **컬럼**: 전체선택 / 주문번호 / 회원ID / 주문자 / 주문자전화 / 받는분 / 주문상품수 / 누적주문수 / 주문합계(선불배송비포함) / 입금합계 / 주문상태 / 결제수단 / 주문취소 / 쿠폰 / 미수금 / 운송장번호 / 배송회사 / 배송일시 / [보기].
- **상세 화면**(`orderform.php`): 주문자/수령자 정보, 주문 상품 라인(상태별 수량 변경), 결제·환불 처리, 운송장 입력, 메모, 상태 일괄변경. DB: `g5_shop_order`(주문) + `g5_shop_order_receipt`(입금/환불) + 주문상품은 cart 스냅샷.
- **구현 노트**: 주문상태 전이(주문→입금→준비→배송→완료, 취소/반품/품절) 상태머신. 합계 = 상품합 + 배송비 − 쿠폰 − 적립.

## 4.3 개인결제관리 `shop_admin/personalpaylist.php`
- **검색**: `sfl`(개인결제번호/이름/주문번호) + `stx`.
- **컬럼**: 전체선택 / 제목 / 주문번호 / 주문금액 / 입금금액 / 미수금액 / 입금방법 / 입금일 / 사용 / 관리. [선택삭제].
- **목적**: 비정형(맞춤) 결제건을 관리자가 생성→링크로 결제 유도.

## 4.4 분류관리 `shop_admin/categorylist.php`
- **검색**: `sfl`(분류명/분류코드/회원아이디) + `stx`.
- **인라인 필드(행별, 다단)**: `ca_name[]`(분류명), `ca_use[]`(판매 가능), `ca_cert_use[]`(본인인증), `ca_adult_use[]`(성인인증), `ca_img_width[]`/`ca_img_height[]`(출력이미지), `ca_list_mod[]`/`ca_list_row[]`(1줄 이미지수/줄수), `ca_mobile_list_mod[]`/`ca_mobile_list_row[]`, `ca_skin_dir[]`/`ca_skin[]`(PC스킨 폴더/파일), `ca_mobile_skin_dir[]`/`ca_mobile_skin[]`.
- **컬럼**: 분류코드 / 분류명 / 상품수 / 본인인증 / 판매가능 / 성인인증 / 이미지 폭·높이 / 1행이미지수 / 이미지 행수 / 모바일 동일 / PC·모바일스킨지정 / 관리회원아이디 / 관리. [검색][일괄수정]. DB: `g5_shop_category`.
- **구현 노트**: `ca_id`는 자릿수 누적 코드(2자리×depth)로 부모-자식 표현(최대 3~4단).

## 4.5 상품관리 `shop_admin/itemlist.php`
- **검색**: `sca`(분류 트리 select) + `sfl`(상품명/상품코드/제조사/원산지/판매자 e-mail) + `stx`.
- **인라인 필드(행별)**: `ca_id[]`/`ca_id2[]`/`ca_id3[]`(분류 1~3차), `it_order[]`(순서), `it_use[]`(판매), `it_soldout[]`(품절), `it_name[]`(상품명), `it_price[]`(판매가), `it_cust_price[]`(시중가), `it_skin[]`/`it_mobile_skin[]`(스킨), `it_stock_qty[]`(재고).
- **컬럼**: 전체선택 / 이미지 / 상품코드 / 상품명 / 분류 / 판매가격 / 시중가격 / 포인트 / 순서 / 판매 / 품절 / 재고 / 조회 / PC·모바일스킨 / 관리. [검색][선택수정]. DB: `g5_shop_item`.

## 4.6 상품 추가/수정 `shop_admin/itemform.php` (대형 폼)
영카트 상품의 전체 속성. 많은 항목에 "분류적용(`chk_ca_*`)/전체적용(`chk_all_*`)" 일괄전파 체크박스.
- **분류/스킨**: `ca_id`/`ca_id2`/`ca_id3`(분류), `it_skin`/`it_mobile_skin`(스킨), `it_class_num`(쇼핑몰/정기결제 사용 구분 radio), `it_id`(상품코드).
- **기본정보**: `it_name`(상품명), `it_basic`(기본설명), `it_order`(출력순서), 노출유형 `it_type1~5`(히트/추천/신상품/인기/할인 checkbox), `it_maker`(제조사), `it_origin`(원산지), `it_brand`, `it_model`, `it_tel_inq`(전화문의), `it_use`(판매가능), `it_nocoupon`(쿠폰적용 안함), `ec_mall_pid`(네이버쇼핑 상품ID).
- **상품요약정보**: `it_info_gubun`(상품군 select: 의류/구두/가방/가전 등) → 군별 `ii_value[]` 고시정보 입력행 동적 변경.
- **가격·재고**: 판매가/시중가/공급가/포인트, 옵션1·추가1 등(섹션 "가격 및 재고 입력").
- **옵션**: 상품선택옵션(`g5_shop_item_option`, 조합형/단독형), 상품추가옵션(추가금 옵션).
- **배송비**: 배송비 유형(무료/조건부/유료/착불), 개별 배송비.
- **이미지**: 대표+추가 이미지 업로드(이미지1~N), 확대이미지.
- **상세설명**: 상단/본문/하단 내용(`it_explan` PC, `it_mobile_explan` 모바일, textarea+에디터), 판매자 이메일 `it_sell_email`, 상점메모 `it_shop_memo`.
- **여분필드**: 여분필드1~N.
- **액션**: [확인]. **DB**: `g5_shop_item`(+옵션/이미지 테이블).
- **구현 노트**: 가장 복잡한 화면. 섹션 분리 + 옵션/이미지 동적 행 추가. "분류적용/전체적용"은 해당 분류 또는 전체 상품으로 값 복사.

## 4.7 상품문의 `shop_admin/itemqalist.php`
- **검색**: `sca`(분류) + `sfl`(상품명/상품코드) + `stx`.
- **컬럼**: 전체선택 / 상품명 / 질문 / 이름 / 답변여부 / 관리. [선택삭제]. 답변 작성. DB: `g5_shop_item_qa`.

## 4.8 사용후기 `shop_admin/itemuselist.php`
- **검색**: `sca` + `sfl`(상품명/상품코드/이름) + `stx`.
- **인라인 필드**: `is_score[]`(평점: 매우만족~매우불만), `is_confirm[]`(확인=노출 승인).
- **컬럼**: 전체선택 / 상품명 / 이름 / 제목 / 평점 / 확인 / 관리. [선택수정][선택삭제]. DB: `g5_shop_item_use`.

## 4.9 상품재고관리 `shop_admin/itemstocklist.php`
- **검색**: `sel_ca_id`(분류) + `sel_field`(상품명/상품코드) + `search`.
- **인라인 필드**: `it_stock_qty[]`(재고수정), `it_noti_qty[]`(통보수량), `it_use[]`(판매), `it_soldout[]`(품절), `it_stock_sms[]`(재입고 알림).
- **컬럼**: 상품코드 / 상품명 / 창고재고 / 주문대기 / 가재고 / 재고수정 / 통보수량 / 판매 / 품절 / 재입고알림 / 관리. [일괄수정].

## 4.10 상품유형관리 `shop_admin/itemtypelist.php`
- **검색**: `sca` + `sfl`(상품명/상품코드) + `stx`.
- **인라인 필드**: `it_type1[]`~`it_type5[]`(히트/추천/신규/인기/할인 일괄 토글).
- **컬럼**: 상품코드 / 상품명 / 히트 / 추천 / 신규 / 인기 / 할인 / 관리. [일괄수정].

## 4.11 상품옵션재고관리 `shop_admin/optionstocklist.php`
- **검색**: `sel_ca_id` + `sel_field` + `search`.
- **인라인 필드**: `io_stock_qty[]`(재고), `io_noti_qty[]`(통보수량), `io_use[]`(판매).
- **컬럼**: 상품명 / 옵션항목 / 옵션타입 / 창고재고 / 주문대기 / 가재고 / 재고수정 / 통보수량 / 판매 / 관리. DB: `g5_shop_item_option`.

## 4.12 쿠폰관리 `shop_admin/couponlist.php` (+ `couponform.php`)
- **목록 검색**: `sfl`(회원아이디/쿠폰이름/쿠폰코드) + `stx`. **컬럼**: 전체선택 / 쿠폰종류 / 쿠폰코드 / 쿠폰이름 / 적용대상 / 회원아이디 / 사용기한 / 사용회수 / 관리. [선택삭제].
- **발급 폼(`couponform.php`)**: `cp_subject`(쿠폰이름), `cp_method`(개별상품할인/카테고리할인/주문금액할인/배송비할인/정기결제할인), `cp_target`(적용상품/카테고리), `mb_id`+`chk_all_mb`(대상 회원/전체회원), `cp_start`~`cp_end`(사용기간), `cp_type`(정액원/정률%), `cp_price`(할인값), `cp_trunc`(절사단위 1/10/100/1000원), `cp_minimum`(최소주문금액), `cp_maximum`(최대할인금액), `cp_sms_send`/`cp_email_send`(발급 알림). [확인]. DB: `g5_shop_coupon`.

## 4.13 쿠폰존관리 `shop_admin/couponzonelist.php`
- **검색**: `stx`. **컬럼**: 전체선택 / 쿠폰이름 / 쿠폰종류 / 적용대상 / 쿠폰금액 / 쿠폰사용기한 / 다운로드 / 사용기한 / 관리. [선택삭제].
- **목적**: 회원이 직접 받아가는 다운로드형 쿠폰존. DB: `g5_shop_couponzone`.

## 4.14 추가배송비관리 `shop_admin/sendcostlist.php`
- **등록 폼**: `sc_name`(지역명), `sc_zip1`~`sc_zip2`(우편번호 시작~끝), `sc_price`(추가배송비).
- **목록**: 전체선택 / 지역명 / 우편번호 / 추가배송비. [선택삭제][확인]. DB: `g5_shop_sendcost`.

## 4.15 미완료주문 `shop_admin/inorderlist.php`
- **검색**: `sfl`(주문번호) + `stx`. **컬럼**: 전체선택 / 주문번호 / PG / 주문자 / 주문자전화 / 받는분 / 주문금액 / 결제방법 / 주문일시 / 관리. [선택삭제].
- **목적**: 결제 진입 후 미완료(장바구니 이탈/결제 실패) 주문 추적.

---

# 5. 쇼핑몰현황/기타

## 5.1 매출현황 `shop_admin/sale1.php`
- **검색**: 일별(`date` 하루 또는 `fr_date`~`to_date`), 월별(`fr_month`~`to_month`), 연별(`fr_year`~`to_year`).
- **출력**: 기간별 매출 집계 표(주문수/매출액/취소/순매출). [확인].

## 5.2 상품판매순위 `shop_admin/itemsellrank.php`
- **검색**: `sel_ca_id`(분류) + `fr_date`~`to_date`.
- **컬럼**: 순위 / 상품명 / 쇼핑 / 주문 / 입금 / 준비 / 배송 / 완료 / 취소 / 반품 / 품절 / 합계(상태별 판매 수량).

## 5.3 주문내역출력 `shop_admin/orderprint.php`
- **필드**: `csv`(출력형식: 화면/MS엑셀 XLS radio), `ct_status`(출력대상: 주문/입금/준비/배송/완료/취소/반품/품절/전체), 기간 `fr_date`~`to_date`, 주문번호 구간 `fr_od_id`~`to_od_id`. [출력(새창)].
- **목적**: 주문/송장 데이터 인쇄·엑셀 다운로드.

## 5.4 재입고SMS알림 `shop_admin/itemstocksms.php`
- **검색**: `sel_field`(상품코드/휴대폰번호) + `search`.
- **컬럼**: 전체선택 / 상품명 / 휴대폰번호 / SMS전송 / SMS전송일시 / 등록일시. [선택SMS전송].
- **목적**: 품절 상품 재입고 신청자에게 일괄 SMS.

## 5.5 이벤트관리 `shop_admin/itemevent.php` (+ 일괄처리 `itemeventlist.php`)
- **이벤트 목록**: 이벤트번호 / 제목 / 연결상품 / 사용 / 관리. 추가/수정(제목, 기간, 배너이미지, 연결상품). DB: `g5_shop_event`.
- **일괄처리(`itemeventlist.php`)**: `ev_id`(이벤트 선택) + `sel_ca_id`(분류) + `sel_field`(상품명/상품코드) + `search` → 상품 검색 후 `ev_chk[]`로 이벤트에 일괄 연결. [이동][검색][일괄수정].

## 5.6 배너관리 `shop_admin/bannerlist.php`
- **검색(select)**: `bn_position`(위치: 전체/메인/왼쪽), `bn_device`(PC와모바일/PC/모바일), `bn_time`(시간: 전체/진행중/종료).
- **컬럼**: ID / 이미지 / 접속기기 / 위치 / 시작일시 / 종료일시 / 출력순서 / 조회 / 관리. 추가/수정(이미지, 링크, 위치, 노출기간, 순서). DB: `g5_shop_banner`.

## 5.7 보관함현황 `shop_admin/wishlist.php`
- **검색**: `sel_ca_id`(분류) + `fr_date`~`to_date`. **컬럼**: 순위 / 상품명 / 건수. 회원 위시리스트(찜) 통계.

## 5.8 가격비교사이트 `shop_admin/price.php`
- **목적**: 네이버쇼핑 등 가격비교 사이트로 상품 EP(상품 피드) 생성/연동 설정.

---

# 6. 정기결제관리 (구독)

## 6.1 정기결제설정 `subscription_admin/configform.php`
- **CRON/일반**: `api_holiday_data_go_key`(공휴일 OpenAPI 키), `cron_night_block`(야간 결제 제외), `su_auto_payment_lead_days`(자동결제 선행일), `su_subscription_content_first`/`su_subscription_content_end`(약관 textarea).
- **배송주기 옵션(행 배열)**: `opt_chk[]`/`opt_input[]`/`opt_date_format[]`(일/주/월/년)/`opt_etc[]`(요일)/`opt_print[]`/`opt_use[]` — 사용자에게 보여줄 결제주기 보기 구성. 출력형식 `su_output_display_type`(셀렉트박스/버튼식).
- **사용자 직접배송일**: `su_chk_user_delivery`(직접입력 허용), `su_user_delivery_default_day`/`su_user_delivery_minimum`, `su_user_delivery_title`/`su_user_select_title`(라벨). 첫희망배송 `su_hope_date_use`/`su_hope_date_after`.
- **PG 연동키**: 이니시스(`su_inicis_mid`/`su_inicis_sign_key`/`su_inicis_iniapi_key`/`su_inicis_iniapi_iv`), KCP(`su_kcp_mid`/`su_kcp_site_key`/`su_kcp_group_id`/`su_kcp_cert_info`), 나이스페이(`su_nicepay_mid`/`su_nicepay_key`), 토스페이먼츠(`su_tosspayments_api_clientkey`/`su_tosspayments_api_secretkey`), 테스트/실결제 `su_card_test`.
- **액션**: [확인]. **구현 노트**: 빌링키(정기결제 토큰) 기반. CRON으로 결제예정일에 자동 청구.

## 6.2 분류관리 `subscription_admin/categorylist.php`
- 쇼핑몰 분류관리(4.4)와 동일 구조(분류명/스킨/이미지/판매여부 인라인). 정기결제 전용 분류.

## 6.3 상품관리 `subscription_admin/itemlist.php`
- 쇼핑몰 상품관리(4.5)와 동일 구조. 캡션만 "정기구독 상품관리". 구독 상품 등록(결제주기·배송주기 연계).

## 6.4 정기결제내역 `subscription_admin/paylist.php`
- **검색**: `sel_field`(주문번호/회원ID/주문자/연락처/받는분/입금자/운송장) + `search`, 상태 `py_status`(radio), 필터 `py_misu`/`py_cancel_price`/`py_refund_price`, 기간 `fr_date`~`to_date`.
- **컬럼**: 전체선택 / 주문상품 / 주문날짜 / 주문번호 / 주문자 / 받는분 / 주문합계 / 입금합계 / 주문취소 / 미수금 / 회원ID / 주문상품수 / 결제수단(PG사) / **정기결제회차** / 주문상태 / 운송장번호 / 배송회사 / 배송일시 / [보기]. DB: 정기결제 주문 테이블.

## 6.5 정기결제일정 `subscription_admin/subscription_calendar.php`
- **목적**: 결제 예정 건을 달력(캘린더 UI)으로 표시. 날짜별 청구 예정 건수/금액.

## 6.6 공휴일설정 `subscription_admin/subscription_holidays.php`
- **목적**: 공휴일 등록/조회(공공데이터 OpenAPI 연동). 결제예정일이 공휴일이면 처리 규칙(전/후 영업일 이동).

---

# 7. SMS 관리 (문자 발송)

> 그누보드 SMS5(아이코드 게이트웨이) 기준. DB 접두어 `g5_sms5_*`.

## 7.1 SMS 기본설정 `sms_admin/config.php`
- **필드**: `cf_sms_type`(전송유형 SMS/LMS), `cf_icode_id`(아이코드 회원아이디·구버전), `cf_icode_pw`(비밀번호·구버전), `cf_icode_token_key`(토큰키·JSON버전), `cf_phone`(회신번호). [확인].

## 7.2 문자 보내기 `sms_admin/sms_write.php`
- **이모티콘 선택**: `fg_no`(이모티콘 그룹) + `st`(제목+이모티콘/제목/이모티콘) + `sv`(검색).
- **메시지**: `wr_message`(내용 textarea, 바이트 카운트), `wr_reply`(회신번호).
- **수신자**: `hp_list`(받는사람 목록 select), `hp_name`/`hp_number`(직접 추가), 그룹/주소록에서 불러오기.
- **예약전송**: `wr_booking`(사용) + `wr_by`/`wr_bm`/`wr_bd`/`wr_bh`/`wr_bi`(년/월/일/시/분).
- **액션**: [검색][전송]. SMS(90byte)/LMS 자동 분기.

## 7.3 전송내역-건별 `sms_admin/history_list.php`
- **검색**: `sv`. **컬럼**: 번호 / 메세지 / 회신번호 / 전송일시 / 예약 / 총건수 / 성공 / 실패 / 중복 / 재전송 / 관리.

## 7.4 전송내역-번호별 `sms_admin/history_num.php`
- **검색**: `st`(이름/휴대폰번호/고유번호) + `sv`. **컬럼**: 번호 / 그룹 / 이름 / 회원ID / 전화번호 / 전송일시 / 예약 / 전송 / 메세지 / 관리.

## 7.5 이모티콘 그룹 `sms_admin/form_group.php`
- **필드**: `fg_name`(그룹명 추가), 인라인 `fg_name[]`(그룹명 수정), `fg_member[]`(회원 그룹 여부), `select_fg_no[]`(이동 그룹).
- **컬럼**: 전체선택 / 그룹명 / 이모티콘수 / 이동 / 보기. [추가][선택수정][선택삭제][선택비우기].

## 7.6 이모티콘 관리 `sms_admin/form_list.php`
- **검색**: `fg_no`(그룹) + `st`(제목+이모티콘/제목/이모티콘) + `sv`. 행별 `fo_no[]`. [검색][선택이동][선택삭제]. 자주 쓰는 문구 템플릿 CRUD.

## 7.7 휴대폰번호 그룹 `sms_admin/num_group.php`
- **필드**: `bg_name`(그룹추가), 인라인 `bg_name[]`(수정), `select_bg_no[]`(이동 그룹).
- **컬럼**: 전체선택 / 그룹명 / 총 / 회원 / 비회원 / 수신 / 거부 / 이동 / 보기. [그룹추가][선택수정][선택삭제][선택비우기].

## 7.8 휴대폰번호 관리 `sms_admin/num_book.php`
- **검색**: `st`(이름+휴대폰/이름/휴대폰) + `sv` + `bg_no`(그룹) + `no_hp`(휴대폰 소유자만).
- **컬럼**: 전체선택 / 번호 / 그룹 / 이름 / 휴대폰 / 수신 / 아이디 / 업데이트 / 관리.
- **액션**: [검색][선택삭제][수신허용][수신거부][선택이동][선택복사]. 주소록 CRUD + 수신동의 관리.

## 7.9 휴대폰번호 파일 `sms_admin/num_book_file.php`
- **필드**: `upload_bg_no`(대상 그룹 select), `csv`(파일선택, CSV 업로드). [파일전송]. 번호 대량 일괄 등록.

---

# 8. 구현 우선순위 제안 (데모 클론 기준)

| 단계 | 범위 | 이유 |
|---|---|---|
| **P0 기반** | 기본환경설정 · 회원관리 · 권한모델 · 공통 리스트 컴포넌트 | 모든 화면이 의존 |
| **P1 커뮤니티** | 게시판관리 + 게시판 추가폼 + 글/댓글 + 포인트 + 1:1문의 + 내용관리 | CMS 코어 |
| **P2 커머스 코어** | 분류관리 → 상품관리/상품폼 → 주문내역 → 쇼핑몰설정 | 쇼핑몰 최소 동작 |
| **P3 커머스 확장** | 재고/옵션재고/상품유형 · 쿠폰/쿠폰존 · 배송비 · 상품문의/후기 · 미완료주문 | 운영 기능 |
| **P4 현황/마케팅** | 매출현황 · 판매순위 · 주문출력 · 배너 · 이벤트 · 보관함 · 가격비교 | 통계·마케팅 |
| **P5 부가** | 정기결제(6장) · SMS(7장) · 접속자집계 · 인기검색어 | 외부연동 의존(PG/문자 게이트웨이) |

## 부록: 공통 구현 체크리스트
- [ ] 리스트 공통 컴포넌트(검색폼 + 전체선택 + 인라인 일괄수정 `name[]` + 일괄삭제 + 페이징)
- [ ] 권한 가드 미들웨어(`mb_level`, 최고관리자, 게시판/분류별 레벨)
- [ ] "그룹적용/전체적용" 값 전파 헬퍼(게시판폼·상품폼 공통)
- [ ] 분류 트리(계층 코드) 컴포넌트(게시판그룹·쇼핑 분류 공용)
- [ ] 주문 상태머신(주문→입금→준비→배송→완료, 취소/반품/품절) + 합계 계산
- [ ] 포인트 원장(증감 시 회원 누적 동기화, 만료 배치)
- [ ] 파일 업로드(개수·용량 제한, 이미지 리사이즈)
- [ ] 외부 연동 어댑터: PG(이니시스/KCP/나이스/토스), SMS 게이트웨이, 공휴일 OpenAPI

---
*본 명세는 demo.sir.kr 그누보드5(영카트5) 데모의 화면 구조를 관찰하여 정리한 재구현용 참고 문서입니다. 실제 DB 컬럼명은 그누보드 표준 스키마를 따랐으며, 데모에서 직접 확인된 폼 필드명(`bo_*`, `it_*`, `ca_*`, `od_*`, `cp_*`, `su_*`, `cf_*` 등)은 화면에서 추출한 실제 값입니다.*
