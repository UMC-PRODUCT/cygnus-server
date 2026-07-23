# GraphQL IDL

이 디렉터리는 Spring GraphQL이 조립하는 schema이자 각 도메인이 외부에 제공하는 표준 IDL이다.
파일은 배포 단위가 아니라 계약 소유권을 기준으로 나눈다.

## 디렉터리 구조

| 경로 | 소유 계약 |
|---|---|
| `schema.graphqls` | 도메인 extension을 조립하기 위한 기술 root `Query`, `Mutation` |
| `shared/` | transport scalar와 pagination 같은 platform contract |
| `analytics/` | 운영 범위 기반 dashboard read model |
| `audit/` | 감사 로그와 actor 관계 |
| `authentication/` | OAuth 연결 조회와 credential 가용성 |
| `authorization/` | ChallengerRole과 resource permission 판정 |
| `blog/`, `community/`, `notice/` | 게시 resource, 작성자, 댓글과 viewer 상태 |
| `certificate/` | 인증서 조회·발급·검증과 download |
| `challenger/` | Challenger resource와 part, track, status enum |
| `chat/` | consumer-owned room 식별자 기반 채팅 조회 |
| `curriculum/` | curriculum, workbook, mission, submission |
| `documentation/` | client error code catalog |
| `feedback/` | Feedback consumer projection과 제출 |
| `form/` | Form engine의 표준 form provider contract |
| `maintenance/` | 점검 상태와 admin lifecycle |
| `member/` | 표준 `MemberPublic`, 본인 전용 `MemberPrivate` |
| `notification/` | FCM installation과 비동기 발송 command |
| `organization/` | Gisu, Chapter, School, StudyGroup, UMC PRODUCT 조직 |
| `project/` | Project resource와 Project 전용 Form projection |
| `recruiting/` | canonical Season, Round, Application resource |
| `schedule/` | 일정, 참여자와 권한 capability |
| `storage/` | upload 준비·확정·삭제 command |
| `term/` | 약관 조회·동의·관리 |

각 도메인의 `request.graphqls`는 root operation과 input을, `response.graphqls` 또는
`output.graphqls`는 도메인이 제공하는 output과 enum을 소유한다. 조회나 입력 계약이 없는 도메인은
불필요한 빈 파일을 만들지 않는다.

## 소유권 규칙

1. 도메인이 표준으로 제공하는 resource는 제공 도메인의 output에 한 번만 선언한다.
2. 소비 도메인이 의미를 바꾸지 않으면 제공 타입을 직접 참조한다. 예: `Project.productOwner: MemberPublic`.
3. 소비 도메인이 필터링하거나 정책 필드를 결합하면 자체 타입과 converter를 둔다. 예:
   `ProjectApplicationForm`, `RecruitingApplicationFormStructure`.
4. 과거 값을 보존해야 하면 소비 도메인이 snapshot 타입 또는 scalar field를 소유한다. 예:
   `ProjectApplicant.schoolName`, Recruiting 지원서의 applicant profile.
5. 외래 도메인 aggregate 객체를 Java domain model에 직접 보관하지 않는다. GraphQL 관계는 ID와
   resolver/use case 조합으로 해석한다.

## IDL 배포와 조립

도메인 IDL은 해당 도메인의 표준 request/response 계약을 나타내지만, 참조 타입까지 복제하지 않는다.
독립 서비스로 분리할 때는 대상 도메인 IDL과 `shared` 및 직접 참조한 provider IDL을 함께 조립하거나,
경계 API를 ID-only 계약으로 바꾸고 소비 서비스에서 자체 projection으로 변환한다.

Spring GraphQL은 `classpath*:graphql/**/*.graphqls`를 모두 읽는다. 따라서 모든 root operation은
`extend type Query` 또는 `extend type Mutation`으로 선언하고, 이름이 같은 type을 여러 파일에서
재선언하지 않는다.

## 공개 계약 제외

다음 top-level package는 public GraphQL API 도메인이 아니다.

| package | 제외 이유 |
|---|---|
| `common`, `global` | domain resource가 아닌 공통 모델과 runtime infrastructure |
| `llm` | 다른 application service가 호출하는 내부 port이며 직접 client API가 아님 |
| `figma`, `survey` | 현재 구현된 inbound use case가 없는 예약 package |
| `test` | 개발·검증 전용 transport |

`GraphQlDomainCoverageTest`가 모든 top-level package를 공개 계약 또는 위 제외 목록 중 하나로
분류하도록 강제한다.

## 문서화 규칙

- API 사용자에게 필요한 설명은 `#` 주석이 아니라 GraphQL description(`"""..."""`)으로 작성한다.
- 외부 도메인 ID에는 소유 도메인과 연결 대상 type을 명시한다.
- 변환된 type에는 원본 도메인과 변환 이유를 명시한다.
- 필드 추가 시 해당 도메인 README의 관계 표와 architecture test를 함께 갱신한다.
