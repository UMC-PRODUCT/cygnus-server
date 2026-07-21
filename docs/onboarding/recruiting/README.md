# Recruiting 온보딩

## 책임과 경계

Recruiting은 학교별 모집 Season, Round, 지원서, 평가, 면접 일정, 최종 등록 준비를 소유한다.

- Form은 section/question/option, 조건부 이동, 익명 FormResponse와 답변을 소유한다.
- Recruiting은 Form 관련 ID만 저장하고 공개 UseCase로 연동한다.
- Organization의 학교와 지부는 ID로 조회한다. 지부 변경을 반영하기 위해 `chapterId`는 반정규화하지 않는다.
- Authorization은 실제 Season/Round/Application 소속에 따라 권한을 판정한다.
- Challenger 등록은 공개 `addTrack(memberId, gisuId, track)` UseCase로 위임한다.
- Recruiting 내부 자식만 부모를 `LAZY @ManyToOne`으로 참조하며 역방향 `@OneToMany`는 사용하지 않는다.

관계와 흐름은 [다이어그램](../domain/recruiting-diagrams.md), 검증 범위는 [테스트 카탈로그](test-cases.md)를 참고한다.

## 엔티티 제한

| 엔티티 | 적용 제한 |
|---|---|
| `RecruitingSeason` | `(gisuId, schoolId)` 유일. 상태는 없으며 Season 공용 `memo`를 가진다. |
| `RecruitingSeasonTrackQuota` | `(season, track)` 유일, `targetCount >= 0`. Recruiting 지원 트랙만 허용하고 `INFRA_PLUS`는 금지한다. Round가 사용하는 트랙을 제거할 수 없고 `READY + REGISTERED`보다 TO를 낮출 수 없다. |
| `RecruitingRound` | `title`은 trim 후 1~100자이며 Season 안에서 대소문자 무시 유일. 본모집은 Season당 하나이고 `roundNo=1`. 추가모집은 최대 차수의 다음 번호만 허용한다. |
| `RecruitingRound` 일정 | 시간은 `Instant`. 접수 시작 < 접수 종료 <= 서류 발표 <= 면접 시작 < 면접 종료 <= 최종 발표 순서다. 면접 미진행 시 면접 기간과 `availabilityFormId`는 null이다. |
| `RecruitingRound` 트랙 | Round 트랙은 양수 TO를 가진 Season 트랙의 부분집합이며 중복과 `INFRA_PLUS`를 허용하지 않는다. 2지망 허용 여부를 소유한다. |
| `RecruitingApplicationForm` | Round당 최대 하나. Round 제목을 Form 제목으로 사용한다. 구조는 Round가 DRAFT일 때만 Upsert할 수 있다. |
| 실제 `Form` 생명주기 | Round와 함께 `DRAFT -> PUBLISHED -> CLOSED`로 전환한다. 지원서와 FormResponse가 모두 없으면 `OPEN -> DRAFT` 게시 취소가 가능하다. CLOSED Form은 응답 생성·수정·제출과 개별 답변 변경을 거부한다. 면접 일정 Form은 이 생명주기에 포함하지 않는다. |
| `RecruitingFormSectionPolicy` | `formSectionId`당 하나. `COMMON`은 track이 null, `TRACK`은 Round 모집 트랙이어야 한다. TRACK section의 조건부 이동은 COMMON 또는 같은 TRACK만 허용한다. |
| `RecruitingApplication` | Round별 동일 정규화 email 또는 동일 member 지원은 한 건만 허용한다. `applicationKey`는 `[A-Z0-9]{6}`이며 `(email, key)`가 유일하다. |
| `RecruitingApplication` 선택 | 1지망은 필수. 2지망은 Round가 허용할 때만 가능하고 1지망과 달라야 한다. `acceptedTrack`은 최종 합격 시 1·2지망 중 하나다. |
| `RecruitingApplication` 익명 | 원문 email을 trim/lowercase해 저장한다. 활성 PRIVACY 약관 ID와 서버 동의 시각, 내부 Form access key가 필요하다. 지원 키와 Form access key는 관리자 응답, CSV와 로그에 노출하지 않는다. |
| `RecruitingRoundEvaluator` | `(round, memberId)` 유일. 등록 시 DOCUMENT와 INTERVIEW 평가 및 관련 조회가 모두 가능하지만 합불 판정 권한은 없다. |
| `RecruitingRoundInterviewQuestion` | content 비공백, `orderNo >= 0`, active. 생성자와 최종 변경자 member ID를 저장한다. 최초 면접 평가 후에는 비활성화만 가능하다. |
| `RecruitingApplicationInterviewQuestion` | Application별 개별 질문. content 비공백, `orderNo >= 0`, active. 최초 면접 평가 후 변경을 제한한다. |
| `RecruitingApplicationEvaluation` | `(application, evaluator, stage)` 유일. `DOCUMENT` 또는 `INTERVIEW`, `APPROVED` 또는 `REJECTED`. 최종 판정 전까지 본인 row를 Upsert할 수 있다. |
| `RecruitingInterviewSchedule` | Application당 하나. `AVAILABILITY_REQUESTED`, `AVAILABILITY_SUBMITTED`, `CONFIRMED`, `CANCELLED`. 확정 시 면접 기간 안의 시작/종료와 장소가 필요하다. 연락처는 snapshot으로 보존한다. |

## Round 생성 규칙

- `REGULAR`: 입력 차수는 생략하거나 1이어야 하며 이미 본모집이 있으면 `409`다.
- `ADDITIONAL`: 기존 추가모집 최대 차수 + 1을 사용한다. 입력을 생략하면 자동 계산하고, 입력값이 예상 차수와 다르면 `409`다.
- 생성 시 Season row를 pessimistic lock으로 조회하므로 동시 요청에서도 같은 차수나 제목이 중복되지 않는다.
- 제목 중복은 서비스 검증과 `LOWER(title)` DB unique index가 함께 막는다.
- HTTP에서 존재하지 않는 자원은 `404`, 순서·중복·삭제 가능 상태 충돌은 `409`로 응답한다.

## 조회 모델

`RECRUITING-ADMIN-011` 관리자 Round 목록은 `gisuId`를 필수로 받고 `chapterId`, `schoolId`, `seasonId`, `track`, `sort`를 선택적으로 받는다. DRAFT를 포함한 관리 설정을 Season 단위로 묶으며 학교의 현재 지부 정보를 조회 시점에 결합한다.

`RECRUITING-PUBLIC-001` 공개 Round 목록은 지원자의 모집 탐색용이다. `gisuId`를 필수로 받고 `chapterId`, `schoolIds`, `roundIds`, `schoolName`, `seasonId`, `track`, `phase`, `sort`를 선택적으로 받는다. ID 목록 내부는 OR, 서로 다른 필터끼리는 AND이며 학교명은 trim 후 대소문자를 무시한 부분 일치다.

- 기본 `phase=OPEN`: `Round OPEN + Form PUBLISHED + start <= now < end`인 지원 가능한 모집만 제공한다.
- `phase=PAST`: CLOSED이거나 접수 종료 시각에 도달한 모집을 제공한다.
- DRAFT Round와 DRAFT Form은 공개하지 않는다.
- 정렬은 `NEWEST`, `REGISTERED`, `RECRUITMENT`만 허용한다.

`RECRUITING-ADMIN-081`은 모집 목록이 아니라 운영 현황 집계다. `gisuId` 내에서 `schoolIds`, `roundIds`, `schoolName`으로 범위를 좁히며 전체 합계와 학교별·Round별 상태 합계를 함께 반환한다. 조건에 포함된 학교나 Round에 지원서가 없어도 0건 그룹을 반환한다.

### Round와 Form 상태

| 요청 | 허용 조건 | 결과 |
|---|---|---|
| `DRAFT -> OPEN` | 지원 Form 구조가 유효하고, 면접 Round면 게시된 availability Form이 존재 | Round OPEN, RecruitingApplicationForm/실제 Form PUBLISHED |
| `OPEN -> DRAFT` | RecruitingApplication과 실제 FormResponse가 모두 0건 | Round/RecruitingApplicationForm/실제 Form DRAFT |
| `OPEN -> CLOSED` | OPEN 상태 | Round/RecruitingApplicationForm/실제 Form CLOSED |
| `CLOSED -> DRAFT/OPEN` | 허용하지 않음 | `409` |

## Form 구조 관리

`RECRUITING-ADMIN-021`은 기존 Form ID 연결 API가 아니라 Form 전체 구조 Upsert다.

1. Round가 DRAFT인지 확인한다.
2. 요청의 section client key 중복과 모든 기존 section/question/option ID의 Form 소속을 검증한다.
3. section, question, option을 diff 기반으로 생성·수정·삭제한다.
4. 임시 `nextSectionKey`를 실제 section ID로 변환한다.
5. COMMON/TRACK 정책을 같은 요청에서 동기화한다.
6. Round OPEN 시 모든 section 정책과 모집 트랙별 TRACK section을 검증하고 Form을 게시한다.

별도 Form 게시·마감 및 section 정책 추가 API는 제공하지 않는다. Form 자체 접수 기간 동기화는 공개 UseCase가 제공될 때 연결할 TODO로 남아 있다.

## 지원과 재지원

- 로그인 지원자는 CurrentMember로 초안 생성, 수정, 제출, 철회를 수행한다.
- 익명 지원자는 생성 시 한 번 받은 `applicationKey`와 email로 조회, 수정, 제출, 철회를 수행한다.
- credential은 URL에 넣지 않고 body 또는 GraphQL variables로 전달한다.
- credential 조회·수정·제출·철회는 client IP 기준 동일한 분당 5회 bucket을 공유한다.
- 제출 완료 지원서도 접수 종료 전에는 Form scope를 유지해 수정할 수 있다.
- 같은 Round의 중복 지원은 상태와 무관하게 금지한다.
- 이후 Round 재지원은 이전 지원이 `DOCUMENT_FAILED`, `FINAL_FAILED`, `CANCELLED`일 때만 허용한다.
- 같은 대상 기수의 어느 학교에서든 `FINAL_PASSED`가 있으면 재지원과 중복 합격을 금지한다.

## 평가와 판정

Round evaluator는 지원서 목록, 상세와 Form 답변을 조회하고 서류·면접 평가를 Upsert한다. evaluator가 자신의 평가를 제출하기 전에는 타 evaluator 평가를 볼 수 없고, 제출 후 같은 stage 평가를 볼 수 있다. Season 관리자는 제출 여부와 관계없이 전체 평가를 조회한다.

평가와 실제 판정은 분리한다.

- 평가: evaluator가 `APPROVED` 또는 `REJECTED` 의견과 최대 2000자 comment를 저장한다.
- 서류 판정: 학교 회장단 또는 중앙 총괄단 이상이 수행한다.
- 최종 판정: 같은 관리 권한이 수행하며 합격 시 지원 지망 중 `acceptedTrack`을 선택한다.
- 등록: 중앙 총괄단 이상만 `NOT_READY -> READY -> REGISTERED`를 수행한다.

서류 합격 시 면접 Round는 `INTERVIEW_ASSIGNED`와 일정 row/Outbox를 같은 transaction에 저장한다. 면접 미진행 Round는 바로 `INTERVIEW_SKIPPED`가 된다. 운영진이 면접을 생략하면 이미 생성된 일정은 `CANCELLED`로 보존한다.

## REST API

`admin`은 역할 이름이 아니라 Recruiting 관리 surface를 나타낸다. 실제 허용 역할은 각 UseCase가 자원 소속과 함께 판정한다.

Swagger `operationId`는 suffix 없이 숫자 3자리를 사용한다. 관리자 API는 Season `001~004`, Round `011~017`,
Form `021`, evaluator `031~033`, 질문 `041~048`, 일정 `051~052`, 판정 `061~063`, 등록 `071~073`,
통계 `081~082` 대역으로 구분한다.

### 공개 및 지원자

| Method | URI | 역할 |
|---|---|---|
| GET | `/api/v1/recruiting/public/rounds` | 지원 가능 또는 지난 Round를 Season별로 조회 |
| GET | `/api/v1/recruiting/public/forms/{applicationFormId}/structure` | 선택한 1·2지망 scope의 Form 구조 조회 |
| POST | `/api/v1/recruiting/public/applications` | 익명 지원서 초안 생성 |
| POST | `/api/v1/recruiting/public/applications/lookup` | email + applicationKey로 익명 지원서 조회 |
| PUT | `/api/v1/recruiting/public/applications` | 익명 지원서 수정 |
| POST | `/api/v1/recruiting/public/applications/submit` | 익명 지원서 제출 |
| POST | `/api/v1/recruiting/public/applications/cancel` | 익명 지원서 철회 |
| POST | `/api/v1/recruiting/applications` | 로그인 지원서 초안 생성 |
| PUT | `/api/v1/recruiting/applications/{applicationId}` | 로그인 지원서 수정 |
| POST | `/api/v1/recruiting/applications/{applicationId}/submit` | 로그인 지원서 제출 |
| PATCH | `/api/v1/recruiting/applications/{applicationId}/cancel` | 로그인 지원서 철회 |
| GET | `/api/v1/recruiting/rounds/{roundId}/applications` | evaluator/운영진용 지원서 페이지 조회 |
| GET | `/api/v1/recruiting/rounds/{roundId}/applications/{applicationId}` | evaluator/운영진용 지원서 답변 상세 조회 |
| PUT | `/api/v1/recruiting/rounds/{roundId}/applications/{applicationId}/evaluations/{stage}` | 본인 평가 Upsert |
| GET | `/api/v1/recruiting/rounds/{roundId}/applications/{applicationId}/evaluations/{stage}` | 공개 정책을 적용한 stage 평가 조회 |
| GET | `/api/v1/recruiting/applications/{applicationId}/interview-schedule` | 본인 면접 일정 조회 |

### 관리 surface

| Method | URI | 역할 |
|---|---|---|
| GET | `/api/v1/recruiting/admin/rounds` | 내부 설정과 모든 상태를 포함한 운영진용 Season별 Round 조회 (`ADMIN-011`) |
| GET | `/api/v1/recruiting/admin/seasons/{seasonId}` | Season memo, TO와 Round 설정 조회 |
| GET | `/api/v1/recruiting/admin/seasons/{seasonId}/rounds/title-availability` | 제목 중복 확인 |
| POST | `/api/v1/recruiting/admin/seasons` | Season과 초기 TO 생성 |
| PATCH | `/api/v1/recruiting/admin/seasons/{seasonId}` | Season 공용 memo 수정 |
| PUT | `/api/v1/recruiting/admin/seasons/{seasonId}/quotas` | 트랙별 TO 전체 교체 |
| POST | `/api/v1/recruiting/admin/seasons/{seasonId}/rounds` | Round 생성 |
| PUT | `/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}` | Round 설정 수정 |
| PATCH | `/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}/status` | Round/Form OPEN·CLOSED 동기화와 데이터 없는 OPEN의 DRAFT 게시 취소 |
| POST | `/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}/clone` | 다른 Season으로 Round 구조 복제 |
| DELETE | `/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}` | 지원서·Form 응답 없는 DRAFT Round hard delete |
| PUT | `/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}/form` | Form 전체 구조와 section 정책 Upsert |
| POST/DELETE/GET | `/api/v1/recruiting/admin/rounds/{roundId}/evaluators/{memberId?}` | Round evaluator 관리 |
| POST/PUT/DELETE/GET | `/api/v1/recruiting/admin/rounds/{roundId}/questions/{questionId?}` | 공통 면접 질문 관리 |
| POST/PUT/DELETE/GET | `/api/v1/recruiting/admin/applications/{applicationId}/questions/{questionId?}` | 개별 면접 질문 관리 |
| PATCH | `/api/v1/recruiting/admin/applications/{applicationId}/document-decision` | 서류 최종 판정 |
| POST | `/api/v1/recruiting/admin/applications/{applicationId}/interview/skip` | 면접 생략 및 일정 취소 |
| PATCH | `/api/v1/recruiting/admin/applications/{applicationId}/final-decision` | 최종 판정과 acceptedTrack 결정 |
| POST | `/api/v1/recruiting/admin/applications/{applicationId}/interview-schedule/request` | 자동 요청 실패 시 멱등 재시도 |
| PUT | `/api/v1/recruiting/admin/applications/{applicationId}/interview-schedule/confirmation` | 면접 시각·장소 확정 |
| POST/DELETE | `/api/v1/recruiting/admin/applications/{applicationId}/registration/ready` | TO 예약/반환 |
| POST | `/api/v1/recruiting/admin/applications/{applicationId}/registration/registered` | Challenger 등록 확정 |
| GET | `/api/v1/recruiting/admin/summary` | 복수 학교·Round 및 학교명 조건의 상태 집계 (`ADMIN-081`) |
| GET | `/api/v1/recruiting/admin/statistics.csv` | 민감정보를 제외한 CSV 다운로드 |

## GraphQL API

GraphQL은 REST와 같은 UseCase를 사용한다. CSV만 REST 전용이다.

### Query

`publicRecruitingRounds`, `recruitingApplicationFormStructure`, `recruitingApplicationByCredential`, `recruitingApplication`, `recruitingRoundGroups`, `recruitingRoundTitleAvailable`, `recruitingSeasonConfiguration`, `recruitingRoundEvaluators`, `recruitingRoundInterviewQuestions`, `recruitingApplicationInterviewQuestions`, `recruitingApplicationEvaluations`, `recruitingInterviewSchedule`, `recruitingStatusSummary`, `recruitingRoundApplications`, `recruitingRoundApplication`

### Mutation

`createRecruitingSeason`, `updateRecruitingSeason`, `replaceRecruitingSeasonTrackQuotas`, `createRecruitingRound`, `updateRecruitingRoundStatus`, `updateRecruitingRound`, `upsertRecruitingApplicationForm`, `cloneRecruitingRound`, `deleteRecruitingRound`, `addRecruitingRoundEvaluator`, `removeRecruitingRoundEvaluator`, `createRecruitingRoundInterviewQuestion`, `updateRecruitingRoundInterviewQuestion`, `deactivateRecruitingRoundInterviewQuestion`, `createRecruitingApplicationInterviewQuestion`, `updateRecruitingApplicationInterviewQuestion`, `deactivateRecruitingApplicationInterviewQuestion`, `createRecruitingApplicationDraft`, `createAnonymousRecruitingApplicationDraft`, `updateRecruitingApplicationDraft`, `updateAnonymousRecruitingApplication`, `submitRecruitingApplication`, `submitAnonymousRecruitingApplication`, `cancelRecruitingApplication`, `cancelAnonymousRecruitingApplication`, `decideRecruitingDocument`, `decideRecruitingFinal`, `prepareRecruitingRegistration`, `cancelRecruitingRegistration`, `confirmRecruitingRegistration`, `skipRecruitingInterview`, `requestRecruitingInterviewAvailability`, `confirmRecruitingInterviewSchedule`, `submitRecruitingApplicationEvaluation`

`RECRUITING-SCHEDULE-001`과 GraphQL `submitRecruitingInterviewAvailability`는 외부 FormResponse ID를 받지 않는다. Form Issue #1146 병합 전까지 REST는 `501 RECRUITING-0419`를 반환하고 GraphQL은 같은 오류 코드를 extension으로 반환한다. 이후 Recruiting이 회원·비회원 일정 draft 생성, 수정, 제출과 내부 access key 보관을 직접 중개하는 계약으로 교체한다.

정확한 input/output 계약은 [`recruiting.graphqls`](../../../src/main/resources/graphql/recruiting.graphqls)를 기준으로 한다.

## CSV와 민감정보

CSV header는 다음으로 고정한다.

```text
gisuId,schoolId,roundType,roundNo,applicationId,maskedEmail,firstChoiceTrack,secondChoiceTrack,acceptedTrack,status,registrationStatus,submittedAt
```

원문 email, 이름, application key, Form access key, Form 답변, 평가 comment와 면접 질문은 포함하지 않는다.

## 후속 작업

- Form 시작·종료 기한 공개 UseCase가 생기면 Round 접수 기간과 Form window를 동기화한다.
- 익명 FormResponse를 로그인 회원에게 이전하는 공개 UseCase가 생기면 claim을 구현한다.
- Form schedule question과 응답 교집합은 GitHub Issue #1146 범위다.
- Thymeleaf HTML 메일 계약 강화와 확정 메일은 GitHub Issue #1147 범위다.
- 통합 모집 권한 조회 API는 policy engine 변경이 반영된 뒤 구현한다.
