# Recruiting Deferred Integrations

Recruiting은 로그인 및 익명 지원서 생성·조회·수정·제출을 제공한다. 아래 연동은 외부 도메인의 공개 계약이 준비될 때까지 구현하지 않으며, 제품 코드의 TODO를 실제 후속 연결 지점으로 사용한다.

## #1146 Form 일정 응답 및 공통 가능 시간 계산 기능

- Issue: [#1146 `[Feat] Form 일정 응답 및 공통 가능 시간 계산 기능`](https://github.com/UMC-PRODUCT/umc-product-server/issues/1146)
- 상태: Open

### 계획

- Form 도메인의 기존 `QuestionType.SCHEDULE` 질문과 UTC `Instant` 일정 답변을 사용한다.
- Form에 이미 구현된 question ID 소속, submit scope와 조건부 방문 경로 검증을 유지한다.
- `formId`와 `formResponseIds`를 받아 제출된 응답의 공통 가능 시간대를 반환하는 공개 Query UseCase를 제공한다.
- 다른 Form의 response/question ID 혼합, 잘못된 시간 순서, 중복 슬롯과 허용 범위 초과를 거부한다.
- Recruiting의 unavailable schedule-overlap adapter를 실제 Form 공개 UseCase 연동 adapter로 교체한다.

### 완료 조건

- Recruiting이 Form 공개 UseCase만으로 일정 후보를 조회한다.
- 전달된 Question과 FormResponse가 대상 Form에 속함이 보장되고, 다른 Form의 ID를 혼합할 수 없다.
- 완전·부분 교집합, 교집합 없음, UTC 정규화와 익명·로그인 응답의 동일 동작이 검증된다.
- REST/GraphQL 계약과 Recruiting/Form 온보딩 문서가 함께 갱신된다.

### 현재 경계

- `RecruitingApplicationCommandService`: 선택한 트랙의 allowed/required question scope를 Form에 전달한다. Form은 다른 Form question과 scope 밖 question을 거부한다.
- `UnavailableRecruitingScheduleOverlapAdapter`: 공개 일정 교집합 Query UseCase가 없어 명시적으로 unavailable 상태를 유지한다.

## Issue 미지정 Form published/window dependency

사용자 요구에 따라 Form start/end 연동은 별도 Issue를 만들지 않고 plain TODO로 보류한다. 이 dependency는 `#1146` 범위가 아니다.

### 후속 범위

- Form 공개 상태와 start/end window를 조회·설정하는 공개 계약을 제공한다.
- 최초 Form 연결 시 Round의 `documentStartAt`/`documentEndAt`을 Form에 동기화한다.
- 연결 후 Round 서류 일정 변경 시 Form window를 재동기화한다.
- 지원 호출 경계에서 실제 Form 공개 상태와 기간을 함께 집행한다.

### 현재 경계

- `RecruitingApplicationFormCommandService`: 최초 Form 연결 시 기간 동기화를 plain TODO로 보류한다.
- `RecruitingRoundCommandService`: Round 일정 변경 후 기간 재동기화를 plain TODO로 보류한다.
- `RecruitingRound.isLocalApplicationPeriodOpenAt`: Recruiting 내부 Season/Round/ApplicationForm 상태와 Round 서류 기간만 계산한다. 실제 Form의 published 상태와 Form window는 아직 조회하지 않는다.

## #1147 Thymeleaf 기반 HTML 템플릿 이메일 발송 기능

- Issue: [#1147 `[Feat] Thymeleaf 기반 HTML 템플릿 이메일 발송 기능`](https://github.com/UMC-PRODUCT/umc-product-server/issues/1147)
- 상태: Open

### 계획

- Notification 공개 `SendEmailUseCase`에 수신자, 제목, 허용된 template type, template variables와 idempotency key를 받는 범용 템플릿 메일 command를 추가한다.
- 호출자는 임의 template path나 완성 HTML을 전달하지 않고, Notification이 타입별 필수 변수 검증과 Thymeleaf 렌더링을 소유한다.
- 면접 가능 시간 제출 요청과 최종 면접 시간·장소·학교 연락처 확정 안내 template을 제공한다.
- 발신자 설정은 `EmailSenderProperties`를 사용하고, transaction commit 이후 outbox로 발송한다.
- idempotency key로 중복 발송을 막고 `PENDING/SENT/FAILED`, 시도 횟수, 마지막 오류와 발송 시각을 추적한다.
- 로그에서 원문 이메일, template variables의 개인정보와 application key를 제외한다.

### 완료 조건

- Recruiting이 Notification outbound port를 직접 참조하지 않는다.
- 서버 소유 Thymeleaf template과 허용된 variables로 두 안내 메일을 발송한다.
- 실패 재시도와 중복 방지가 evidence로 검증된다.
- Notification/Recruiting 온보딩 문서가 함께 갱신된다.

### 현재 경계

- Recruiting에는 면접 안내 메일을 발송하는 public UseCase, Notification outbound port 의존, inline HTML dispatch가 없다.
- `RecruitingInterviewScheduleCommandService`는 면접 일정과 delivery 상태만 저장하며, 두 `TODO(#1147)`이 후속 Notification 공개 UseCase 연결 지점을 표시한다.
- #1147 전까지 template, generic HTML command, outbox 또는 email dispatch를 구현하지 않는다.

## 명시적 보류 범위

- 익명 FormResponse를 로그인 회원에게 이전하는 ownership claim
- Form published/window 조회, 기간 동기화와 접수 집행
- schedule intersection 및 availability response
- Thymeleaf HTML mail과 email dispatch 확장
