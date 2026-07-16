# Recruiting Entity and Flow Diagrams

이 문서는 `recruiting` 도메인의 현재 엔티티 관계와 application service가 실행하는 주요 흐름을 코드 기준으로 시각화한다. 제품 정책과 API 목록은 [Recruiting Domain](recruiting.md)을 함께 참고한다.

## 표기 원칙

- 실선 ERD 관계는 Recruiting 테이블 사이의 실제 FK를 뜻한다.
- Recruiting 내부 child만 `@ManyToOne(fetch = LAZY)`로 parent를 참조한다. parent에는 `@OneToMany` collection이 없다.
- 다른 도메인은 JPA 관계로 연결하지 않고 ID와 공개 UseCase만 사용한다.
- 상태 전이는 엔티티의 domain method가 수행하고, application service는 권한·중복·동시성·외부 UseCase 호출을 조율한다.

## 엔티티 관계

```mermaid
erDiagram
    RECRUITING_SEASON ||--o{ RECRUITING_SEASON_TRACK_QUOTA : "defines"
    RECRUITING_SEASON ||--o{ RECRUITING_ROUND : "contains"
    RECRUITING_ROUND ||--o| RECRUITING_APPLICATION_FORM : "links one"
    RECRUITING_ROUND ||--o{ RECRUITING_ROUND_EVALUATOR : "whitelists"
    RECRUITING_ROUND ||--o{ RECRUITING_ROUND_INTERVIEW_QUESTION : "provides"
    RECRUITING_ROUND ||--o{ RECRUITING_APPLICATION : "receives"
    RECRUITING_APPLICATION_FORM ||--o{ RECRUITING_FORM_SECTION_POLICY : "classifies"
    RECRUITING_APPLICATION_FORM ||--o{ RECRUITING_APPLICATION : "collects"
    RECRUITING_APPLICATION ||--o{ RECRUITING_APPLICATION_EVALUATION : "is evaluated by"
    RECRUITING_APPLICATION ||--o{ RECRUITING_APPLICATION_INTERVIEW_QUESTION : "has"
    RECRUITING_APPLICATION ||--o| RECRUITING_INTERVIEW_SCHEDULE : "has one"

    RECRUITING_SEASON {
        bigint id PK
        bigint gisu_id "Organization ID"
        bigint school_id "Organization ID"
        enum status "DRAFT ACTIVE CLOSED"
    }
    RECRUITING_SEASON_TRACK_QUOTA {
        bigint id PK
        bigint recruiting_season_id FK
        enum track
        int target_count
    }
    RECRUITING_ROUND {
        bigint id PK
        bigint recruiting_season_id FK
        enum type "REGULAR ADDITIONAL"
        int round_no
        enum_array recruitable_tracks
        boolean second_choice_enabled
        boolean interview_required
        enum status "DRAFT OPEN CLOSED"
        bigint availability_form_id "Form ID"
    }
    RECRUITING_APPLICATION_FORM {
        bigint id PK
        bigint recruiting_round_id FK, UK
        bigint form_id "Form ID"
        enum status "DRAFT PUBLISHED CLOSED"
    }
    RECRUITING_FORM_SECTION_POLICY {
        bigint id PK
        bigint recruiting_application_form_id FK
        bigint form_section_id "Form Section ID"
        enum type "COMMON TRACK"
        enum track "nullable for COMMON"
    }
    RECRUITING_APPLICATION {
        bigint id PK
        bigint recruiting_round_id FK
        bigint recruiting_application_form_id FK
        bigint form_response_id "Form ID"
        bigint applicant_member_id "nullable Member ID"
        bigint privacy_term_id "Term ID"
        string application_key
        string form_response_access_key "nullable internal credential"
        enum first_choice
        enum second_choice
        enum accepted_track
        enum status
        enum registration_status
    }
    RECRUITING_ROUND_EVALUATOR {
        bigint id PK
        bigint recruiting_round_id FK
        bigint member_id "Member ID"
    }
    RECRUITING_ROUND_INTERVIEW_QUESTION {
        bigint id PK
        bigint recruiting_round_id FK
        string content
        int order_no
        boolean active
        bigint creator_member_id "Member ID"
        bigint last_modified_by_member_id "Member ID"
    }
    RECRUITING_APPLICATION_INTERVIEW_QUESTION {
        bigint id PK
        bigint recruiting_application_id FK
        string content
        int order_no
        boolean active
    }
    RECRUITING_APPLICATION_EVALUATION {
        bigint id PK
        bigint recruiting_application_id FK
        bigint evaluator_member_id "Member ID"
        enum stage "DOCUMENT INTERVIEW"
        enum status "DRAFT SUBMITTED"
        enum decision "APPROVED REJECTED"
    }
    RECRUITING_INTERVIEW_SCHEDULE {
        bigint id PK
        bigint recruiting_application_id FK, UK
        bigint availability_form_response_id "Form ID"
        enum status
        instant starts_at
        instant ends_at
        string location
        string contact_snapshot
    }
```

### 외부 도메인 경계

```mermaid
flowchart LR
    subgraph Recruiting["Recruiting domain"]
        Season["RecruitingSeason"]
        Round["RecruitingRound"]
        AppForm["RecruitingApplicationForm / SectionPolicy"]
        Application["RecruitingApplication"]
        EvaluatorRefs["RoundEvaluator / ApplicationEvaluation"]
        Schedule["RecruitingInterviewSchedule"]
        Registration["RecruitingRegistrationCommandService"]
    end

    Organization["Organization<br/>Gisu / School"]
    Authorization["Authorization<br/>role and permission UseCase"]
    Form["Form<br/>Form / Section / Response"]
    Member["Member<br/>memberId"]
    Term["Term<br/>privacyTermId"]
    Challenger["Challenger<br/>AddChallengerTrackUseCase"]

    Season -.->|"gisuId, schoolId"| Organization
    Round -.->|"availabilityFormId"| Form
    AppForm -.->|"formId, formSectionId"| Form
    Application -.->|"formResponseId, anonymous raw access key"| Form
    Schedule -.->|"availabilityFormResponseId"| Form
    Application -.->|"applicantMemberId"| Member
    EvaluatorRefs -.->|"memberId, evaluatorMemberId"| Member
    Application -.->|"privacyTermId"| Term
    Season -.->|"scope IDs"| Authorization
    Registration -->|"public command UseCase"| Challenger
```

외부 ID에는 Recruiting FK를 만들지 않는다. 예를 들어 Form section 삭제 여부나 FormResponse 소유권은 Form의 공개 Query UseCase로 확인한다.

## 모집 설정 흐름

아래 흐름은 API 호출의 의존 순서를 나타낸다. Season 활성화, Round 열기, Form 게시는 별도 command지만 지원 접수를 위해 모두 완료되어야 한다.

```mermaid
flowchart TD
    Actor["학교 운영진 / 중앙 운영진"] --> Auth{"기수·학교 관리 권한"}
    Auth -->|"거부"| Denied["요청 거부"]
    Auth -->|"허용"| Season["Season 생성<br/>gisuId + schoolId"]
    Season --> Quota["Track별 quota 저장<br/>INFRA_PLUS 제외"]
    Quota --> Round["REGULAR 또는 ADDITIONAL Round 생성"]
    Round --> TrackCheck{"모집 Track이<br/>targetCount > 0 quota의 부분집합인가"}
    TrackCheck -->|"아니오"| Denied
    TrackCheck -->|"예"| Link["Round에 Form 하나 연결"]
    Link --> Policy["FormSection을 COMMON 또는 TRACK으로 분류"]
    Policy --> PublishCheck{"모든 Form section에 정책 존재<br/>모든 모집 Track section 존재<br/>조건부 이동이 선택 scope 내부인가"}
    PublishCheck -->|"아니오"| Denied
    PublishCheck -->|"예"| PublishForm["Form domain publish"]
    PublishForm --> PublishRecruiting["RecruitingApplicationForm PUBLISHED"]
    PublishRecruiting --> Open["Season ACTIVE + Round OPEN"]
    Open --> Gate{"현재 시각이 Round 서류 접수 기간인가"}
    Gate -->|"예"| Accept["지원서 접수 가능"]
    Gate -->|"아니오"| Closed["지원서 접수 거부"]
```

Form publish와 section policy 추가는 같은 `RecruitingApplicationForm` row의 `PESSIMISTIC_WRITE` lock을 사용한다. 따라서 게시와 정책 변경이 동시에 실행되어도 게시된 Form에 정책이 뒤늦게 추가되지 않는다.

## 로그인 지원서 작성 흐름

```mermaid
sequenceDiagram
    actor Applicant as 지원자
    participant API as REST / GraphQL Adapter
    participant Service as RecruitingApplicationCommandService
    participant Lock as RecruitingConcurrencyLockService
    participant Form as Form public UseCase
    participant DB as Recruiting Port / DB

    Applicant->>API: 지원서 초안 생성
    API->>Service: createDraft(CurrentMember, profile, choices)
    Service->>DB: ApplicationForm과 Round 조회
    Service->>Service: PUBLISHED + Season ACTIVE + Round OPEN + 접수 기간 검증
    Service->>Lock: gisu/member/email advisory lock
    Lock->>DB: 동일 Round·기수 재지원 가능 여부 조회
    Service->>Service: applicationKey 발급
    Service->>Form: FormResponse DRAFT 생성
    Form-->>Service: formResponseId
    Service->>DB: RecruitingApplication DRAFT 저장
    Service-->>API: applicationId + applicationKey
    API-->>Applicant: 초안 생성 결과

    opt 초안 수정
        Applicant->>API: 기본 정보·지망·답변 수정
        API->>Service: updateDraft(CurrentMember)
        Service->>Lock: applicant key 이후 application row lock
        Service->>Form: FormResponse 소유권·Form 일치 확인
        Service->>Form: 답변 DRAFT 수정
        Service->>DB: ApplicantProfile 수정
    end

    opt 제출
        Applicant->>API: 지원서 제출
        API->>Service: submit(CurrentMember)
        Service->>Lock: applicant key 이후 application row lock
        Service->>Form: FormResponse 소유권·Form 일치 확인
        Service->>DB: COMMON + 1·2지망 section policy 조회
        Service->>Form: allowedQuestionIds / requiredQuestionIds로 제출
        Service->>DB: Application 상태를 SUBMITTED로 전이
        Service-->>Applicant: 제출 결과
    end
```

지원자가 응답해야 하는 범위는 `COMMON` section과 선택한 1·2지망의 `TRACK` section이다. 이 section들에 포함된 모든 문항은 `allowedQuestionIds`, 그중 Form에서 필수로 지정한 문항만 `requiredQuestionIds`가 된다.

## 익명 지원서 작성 흐름

```mermaid
sequenceDiagram
    actor Applicant as 비로그인 지원자
    participant API as REST / GraphQL Adapter
    participant Service as RecruitingApplicationCommandService
    participant Form as Form public UseCase
    participant DB as Recruiting Port / DB

    Applicant->>API: 이름·email·지망·개인정보 동의로 초안 생성
    API->>Service: createAnonymousDraft
    Service->>Service: 활성 PRIVACY 약관·접수 기간·중복 지원 검증
    Service->>Form: 익명 FormResponse DRAFT 생성
    Form-->>Service: formResponseId + raw responseAccessKey
    Service->>DB: raw Form key를 내부 필드에 저장
    Service-->>Applicant: applicationId + 6자리 applicationKey 최초 1회 반환

    Applicant->>API: email + applicationKey로 조회·수정·제출
    API->>Service: credential command/query
    Service->>DB: 정규화 email + applicationKey로 익명 지원서 조회
    Service->>Form: 내부 raw key로 FormResponse 소유권·연결 검증
    alt DRAFT 수정
        Service->>Form: 익명 draft 답변 전체 교체
    else SUBMITTED 수정
        Service->>Form: allowed/required scope로 제출 완료 답변 교체
    else 제출
        Service->>Form: 조건부 방문 경로와 required scope 검증 후 제출
        Service->>DB: DRAFT에서 SUBMITTED로 전이
    end
    Service-->>Applicant: Form raw key를 제외한 결과
```

조회 결과는 `documentResultPublishedAt`과 `finalResultPublishedAt` 경계에서 각각 공개한다. 발표 전 결과는 `PENDING`이고 최종 발표 전에는 `acceptedTrack`도 숨긴다. credential REST·GraphQL 요청은 client IP 기준 분당 5회로 제한한다.

## 지원서 전형 상태

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> SUBMITTED: 지원자 제출
    DRAFT --> CANCELLED: 지원자 철회
    SUBMITTED --> CANCELLED: 지원자 철회
    SUBMITTED --> DOCUMENT_PASSED: 서류 합격 결정
    SUBMITTED --> DOCUMENT_FAILED: 서류 불합격 결정
    DOCUMENT_PASSED --> INTERVIEW_ASSIGNED: 면접 진행 대상으로 전환
    DOCUMENT_PASSED --> INTERVIEW_SKIPPED: 면접 생략
    DOCUMENT_PASSED --> FINAL_PASSED: 면접 없이 최종 합격
    DOCUMENT_PASSED --> FINAL_FAILED: 면접 없이 최종 불합격
    INTERVIEW_ASSIGNED --> FINAL_PASSED: 최종 합격
    INTERVIEW_ASSIGNED --> FINAL_FAILED: 최종 불합격
    INTERVIEW_SKIPPED --> FINAL_PASSED: 최종 합격
    INTERVIEW_SKIPPED --> FINAL_FAILED: 최종 불합격
    DOCUMENT_FAILED --> [*]
    FINAL_FAILED --> [*]
    CANCELLED --> [*]
    FINAL_PASSED --> [*]
```

`DOCUMENT_FAILED`, `FINAL_FAILED`, `CANCELLED`만 이후 Round 재지원을 허용한다. 동일 기수에서 이미 `FINAL_PASSED`인 지원자는 다른 Round나 학교에 다시 합격할 수 없다.

현재 `RecruitingApplication.assignInterview()` domain method는 존재하지만 이를 호출하는 inbound UseCase와 REST/GraphQL mutation은 연결되어 있지 않다. 따라서 실제 API만으로 `INTERVIEW_ASSIGNED`에 진입하는 경로는 후속 구현이 필요하다.

## 평가 작성과 공개 범위

### 평가자와 면접 질문 구성

```mermaid
flowchart TD
    Manager["학교 운영진 / 중앙 운영진"] --> ManageAuth{"Round가 속한 Season 관리 권한"}
    ManageAuth -->|"허용"| Evaluator["Round evaluator 등록·해제"]
    Evaluator --> Whitelist["RoundEvaluator<br/>round + member unique"]
    ManageAuth -->|"허용"| Common["Round 공통 면접 질문<br/>생성·수정·비활성화"]
    Whitelist --> Individual["지원자별 면접 질문<br/>생성·수정·비활성화"]
    Common --> Freeze{"해당 Round에 제출된<br/>INTERVIEW 평가가 있는가"}
    Individual --> Freeze
    Freeze -->|"없음"| Mutable["질문 변경 허용"]
    Freeze -->|"있음"| Frozen["질문 변경 거부"]
```

한 Round에 등록된 evaluator는 서류와 면접 평가에 모두 참여할 수 있다. 공통 질문은 Season 관리 권한, 지원자별 질문은 해당 Round의 evaluator whitelist가 필요하다. 공통 질문은 생성자와 최종 변경자 회원 ID를 별도로 보존한다.

### 평가 작성과 조회

```mermaid
flowchart TD
    Request["평가 조회 또는 작성 요청"] --> Action{"작성 요청인가"}
    Action -->|"예"| Scope{"Round evaluator인가"}
    Scope -->|"아니오"| Reject["요청 거부"]
    Scope -->|"예"| Stage{"지원서 상태가 stage 평가 가능 상태인가"}
    Stage -->|"아니오"| Reject
    Stage -->|"예"| Draft["평가 DRAFT 저장<br/>APPROVED / REJECTED"]
    Draft --> Submit["SUBMITTED 제출<br/>이후 수정 불가"]

    Action -->|"조회"| Operator{"시즌 RECRUITMENT READ 운영자인가"}
    Operator -->|"예"| All["해당 stage 전체 평가 조회"]
    Operator -->|"아니오"| ReadScope{"Round evaluator인가"}
    ReadScope -->|"아니오"| Reject
    ReadScope -->|"예"| Own{"본인 평가가 있는가"}
    Own -->|"없음"| Empty["빈 목록"]
    Own -->|"DRAFT"| OwnOnly["본인 평가만 조회"]
    Own -->|"SUBMITTED"| All
```

평가자 whitelist는 평가와 관련 조회만 허용한다. 서류·최종 합불 결정, quota 변경, Challenger 등록 권한은 부여하지 않는다. 질문은 최초 INTERVIEW 평가가 제출된 뒤 수정·비활성화할 수 없다.

## 면접 일정 흐름

```mermaid
stateDiagram-v2
    state "INTERVIEW_ASSIGNED 지원서" as Assigned
    state "AVAILABILITY_REQUESTED" as Requested
    state "AVAILABILITY_SUBMITTED" as Submitted
    state "CONFIRMED" as Confirmed

    [*] --> Assigned
    Assigned --> Requested: 운영진이 가능 일정 요청
    Requested --> Submitted: 지원자가 availability FormResponse 연결
    Submitted --> Confirmed: 운영진이 시각·장소·연락처 확정
    Confirmed --> [*]
```

`contactSnapshot`은 요청·확정 당시 학교 연락처를 보존한다. #1146의 Form 일정 교집합과 FormResponse 소유권 계약, #1147의 Thymeleaf HTML 메일 발송은 아직 연결하지 않았으므로 현재 상태 변경만 수행한다.

## 최종 판정과 Challenger 등록

```mermaid
sequenceDiagram
    participant Operator as 학교 / 중앙 운영진
    participant Decision as RecruitingDecisionCommandService
    participant Registration as RecruitingRegistrationCommandService
    participant DB as Recruiting Port / DB
    participant Challenger as AddChallengerTrackUseCase

    Operator->>Decision: 최종 PASS + acceptedTrack
    Decision->>DB: applicant key 이후 application row lock
    Decision->>Decision: 학교·기수 권한과 동일 기수 중복 합격 검증
    Decision->>DB: FINAL_PASSED + NOT_READY 저장

    Operator->>Registration: READY 준비
    Registration->>Registration: 중앙 총괄단 이상 권한 검증
    Registration->>DB: application row + season/track quota row lock
    Registration->>DB: READY + REGISTERED 사용량 계산
    alt TO 없음
        Registration-->>Operator: RECRUITING_QUOTA_EXCEEDED
    else TO 있음
        Registration->>DB: registrationStatus READY
    end

    alt READY 취소
        Operator->>Registration: 등록 준비 취소
        Registration->>Registration: 중앙 총괄단 이상 권한 검증
        Registration->>DB: registrationStatus NOT_READY<br/>예약한 TO 반환
    else REGISTERED 확정
        Operator->>Registration: 등록 확정
        Registration->>Registration: 중앙 권한·회원 연결·acceptedTrack 검증
        Registration->>Challenger: addTrack(memberId, gisuId, acceptedTrack)
        Challenger-->>Registration: 멱등 추가 완료
        Registration->>DB: registrationStatus REGISTERED
    end
```

지원서 전형 상태와 등록 상태는 분리된다. `FINAL_PASSED`가 되어도 자동 등록되지 않으며, 중앙운영사무국 총괄단 이상이 `NOT_READY -> READY -> REGISTERED`를 완료해야 한다.

## 동시성 경계

```mermaid
flowchart LR
    New["신규 지원"] --> NewApplicantLock["gisu + member/email advisory lock"]
    Existing["지원 수정·제출·철회"] --> ExistingApplicantLock["gisu + member/email advisory lock"]
    ExistingApplicantLock --> ApplicationLock["application row lock"]
    RoundQuestion["Round 공통 질문 변경"] --> RoundOnlyLock["round row lock"]
    ApplicationMutation["지원자별 질문·평가 변경"] --> RoundApplicationLock["round row lock"]
    RoundApplicationLock --> ApplicationLock
    FormPolicy["Form 게시·section 정책"] --> FormLock["applicationForm root row lock"]
    Registration["READY 예약"] --> ApplicationLock
    Registration --> QuotaLock["season + acceptedTrack quota row lock"]
```

잠금 순서는 같은 경쟁 경로에서 고정한다. 지원자 중복 생성, 평가 제출과 질문 변경, Form 게시와 정책 추가, 마지막 TO 예약이 동시에 실행되어도 하나의 트랜잭션만 불변식을 통과하도록 PostgreSQL 통합 테스트로 검증한다.

## 현재 보류된 흐름

| 흐름 | 현재 상태 | 후속 작업 |
|---|---|---|
| 익명 지원서 ownership claim | API 미제공 | FormResponse ownership 이전 공개 UseCase 이후 구현 |
| Form 자체 응답 기간 동기화 | Round의 local 기간만 검증 | Form 기간 공개 UseCase 이후 동기화 |
| 다른 Form question ID 차단 | Form이 question 소속, required subset, 실제 조건부 방문 경로를 검증 | 구현 완료 |
| 면접 가능 시간 교집합 | `FindRecruitingScheduleOverlapPort`는 unavailable | #1146 |
| 요청·확정 HTML 메일 | delivery 상태만 저장 | #1147 |
| `INTERVIEW_ASSIGNED` API 전이 | domain method만 존재 | 별도 inbound UseCase와 권한 정책 필요 |
