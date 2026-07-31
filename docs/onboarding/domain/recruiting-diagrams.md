# Recruiting Entity And Flow Diagrams

이 문서는 Recruiting 엔티티 관계와 핵심 생명주기를 시각화한다. 필드 제한, API 목록과 후속 작업은 [Recruiting 온보딩](../recruiting/README.md)을 참고한다.

## 엔티티 관계

아래 관계는 DB의 자식 엔티티가 부모를 참조하는 FK 방향이다. Java 부모 엔티티에는 역방향 `@OneToMany`를 두지 않는다.

```mermaid
erDiagram
    RECRUITING_SEASON ||--o{ RECRUITING_SEASON_TRACK_QUOTA : "defines quota"
    RECRUITING_SEASON ||--o{ RECRUITING_ROUND : "contains"
    RECRUITING_ROUND ||--o| RECRUITING_APPLICATION_FORM : "owns one"
    RECRUITING_ROUND ||--o{ RECRUITING_ROUND_EVALUATOR : "whitelists"
    RECRUITING_ROUND ||--o{ RECRUITING_ROUND_INTERVIEW_QUESTION : "shares"
    RECRUITING_ROUND ||--o{ RECRUITING_APPLICATION : "receives"
    RECRUITING_APPLICATION_FORM ||--o{ RECRUITING_FORM_SECTION_POLICY : "classifies"
    RECRUITING_APPLICATION_FORM ||--o{ RECRUITING_APPLICATION : "uses"
    RECRUITING_APPLICATION ||--o{ RECRUITING_APPLICATION_EVALUATION : "has"
    RECRUITING_APPLICATION ||--o{ RECRUITING_APPLICATION_INTERVIEW_QUESTION : "has"
    RECRUITING_APPLICATION ||--o| RECRUITING_INTERVIEW_SCHEDULE : "has"

    RECRUITING_SEASON {
        bigint id PK
        bigint gisu_id
        bigint school_id
        text memo
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
        string title
        enum type
        int round_no
        enum status
        enum_array recruitable_tracks
        boolean second_choice_enabled
        boolean interview_required
        bigint availability_form_id
    }
    RECRUITING_APPLICATION_FORM {
        bigint id PK
        bigint recruiting_round_id FK_UK
        bigint form_id
        enum status
    }
    RECRUITING_FORM_SECTION_POLICY {
        bigint id PK
        bigint recruiting_application_form_id FK
        bigint form_section_id UK
        enum type
        enum track
    }
    RECRUITING_APPLICATION {
        bigint id PK
        bigint recruiting_round_id FK
        bigint recruiting_application_form_id FK
        bigint form_response_id UK
        bigint applicant_member_id
        string applicant_email
        string application_key
        enum status
        enum registration_status
    }
    RECRUITING_ROUND_EVALUATOR {
        bigint id PK
        bigint recruiting_round_id FK
        bigint member_id
    }
    RECRUITING_ROUND_INTERVIEW_QUESTION {
        bigint id PK
        bigint recruiting_round_id FK
        bigint creator_member_id
        bigint last_modified_by_member_id
    }
    RECRUITING_APPLICATION_EVALUATION {
        bigint id PK
        bigint recruiting_application_id FK
        bigint evaluator_member_id
        enum stage
        enum decision
    }
    RECRUITING_INTERVIEW_SCHEDULE {
        bigint id PK
        bigint recruiting_application_id FK_UK
        bigint availability_form_response_id
        enum status
        string contact_snapshot
    }
```

## 외부 도메인 경계

```mermaid
flowchart LR
    Recruiting["Recruiting"]
    Organization["Organization<br/>gisuId, schoolId"]
    Authorization["Authorization<br/>permission UseCase"]
    Form["Form<br/>form, section, response ID"]
    Term["Term<br/>privacyTermId"]
    Member["Member<br/>memberId"]
    Challenger["Challenger<br/>addTrack UseCase"]

    Recruiting -. "ID로 조회" .-> Organization
    Recruiting --> "공개 UseCase" Authorization
    Recruiting --> "공개 UseCase" Form
    Recruiting -. "ID로 증적 보존" .-> Term
    Recruiting -. "ID로 소유자 기록" .-> Member
    Recruiting --> "등록 확정" Challenger
```

학교의 지부 소속은 변경될 수 있으므로 Season에 `chapterId`를 저장하지 않는다. 목록 조회 시 Organization의 현재 학교 정보를 결합한다.

## 모집 설정과 공개

```mermaid
flowchart TD
    A["Season 생성<br/>gisu + school 유일"] --> B["트랙별 TO 설정"]
    B --> C["Round 생성<br/>제목, 차수, 일정, 트랙"]
    C --> D["Form 전체 구조 Upsert<br/>COMMON / TRACK 정책"]
    D --> E{"Round OPEN 검증"}
    E -->|"Form 없음 또는 정책 누락"| X["요청 거부"]
    E -->|"면접 Round인데 availability Form 없음"| X
    E -->|"검증 통과"| F["Form 게시 + Round OPEN"]
    F --> G{"현재 시각이<br/>start 이상 end 미만"}
    G -->|"예"| H["공개 OPEN 목록 및 지원 허용"]
    G -->|"아니오"| I["지원 차단"]
    F --> J["Round CLOSED"]
    J --> I
```

Season에는 상태가 없다. 실제 지원 가능 여부는 `Round OPEN + RecruitingApplicationForm PUBLISHED + [documentStartAt, documentEndAt)`로 결정한다.

## 지원서 상태

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> SUBMITTED: 제출
    DRAFT --> CANCELLED: 철회
    SUBMITTED --> CANCELLED: 철회
    SUBMITTED --> DOCUMENT_FAILED: 서류 불합격
    SUBMITTED --> INTERVIEW_ASSIGNED: 서류 합격 + 면접 진행
    SUBMITTED --> INTERVIEW_SKIPPED: 서류 합격 + 면접 미진행
    INTERVIEW_ASSIGNED --> INTERVIEW_SKIPPED: 운영진 면접 생략
    INTERVIEW_ASSIGNED --> FINAL_PASSED: 최종 합격
    INTERVIEW_ASSIGNED --> FINAL_FAILED: 최종 불합격
    INTERVIEW_SKIPPED --> FINAL_PASSED: 최종 합격
    INTERVIEW_SKIPPED --> FINAL_FAILED: 최종 불합격
    DOCUMENT_FAILED --> [*]
    FINAL_FAILED --> [*]
    FINAL_PASSED --> [*]
    CANCELLED --> [*]
```

`DOCUMENT_PASSED` 중간 상태는 없다. 서류 합격 command가 면접 정책까지 같은 transaction에서 반영한다.

## 서류 합격과 일정 요청

```mermaid
sequenceDiagram
    actor Operator as 운영진
    participant Decision as Decision Service
    participant DB as Recruiting DB
    participant Outbox as Event Outbox
    participant Mail as Notification

    Operator->>Decision: 서류 APPROVED 결정
    Decision->>DB: 지원서 row lock 및 권한 확인
    alt 면접 진행 Round
        Decision->>DB: INTERVIEW_ASSIGNED
        Decision->>DB: AVAILABILITY_REQUESTED 일정 생성
        Decision->>Outbox: 요청 이벤트 저장
    else 면접 미진행 Round
        Decision->>DB: INTERVIEW_SKIPPED
    end
    Decision-->>Operator: transaction commit
    Outbox->>Mail: commit 후 Thymeleaf 메일 발송
    Mail-->>DB: SENT 또는 FAILED와 시도 횟수 기록
```

면접을 생략하면 기존 일정은 삭제하지 않고 `CANCELLED`로 남긴다. 이벤트 처리기는 발송 직전 일정 상태를 다시 읽어 취소된 요청 메일을 보내지 않는다.

## 평가와 최종 등록

```mermaid
flowchart TD
    E["Round evaluator"] --> R["지원서 목록 및 답변 조회"]
    E --> V["DOCUMENT / INTERVIEW 평가 Upsert"]
    V --> P{"본인 평가 제출 여부"}
    P -->|"제출 전"| Own["타 평가 비공개"]
    P -->|"제출 후"| All["동일 stage 평가 조회"]
    M["Season 관리 권한"] --> All
    M --> D["서류 및 최종 합불 결정"]
    E -. "권한 없음" .-> D
    D --> F["FINAL_PASSED + acceptedTrack"]
    F --> N["registration NOT_READY"]
    C["중앙 총괄단 이상"] --> Y["READY로 TO 예약"]
    Y --> Z["Challenger track 추가 후 REGISTERED"]
```

평가자는 서류와 면접 stage 모두 평가할 수 있지만 최종 판정, TO 변경, Challenger 등록 권한은 얻지 않는다.
