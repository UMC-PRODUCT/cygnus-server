# GraphQL IDL

이 디렉터리는 Spring GraphQL이 조립하는 schema이자 각 도메인이 외부에 제공하는 표준 IDL이다.
파일은 배포 단위가 아니라 계약 소유권을 기준으로 나눈다.

## 디렉터리 구조

| 경로 | 소유 계약 |
|---|---|
| `schema.graphqls` | 도메인 extension을 조립하기 위한 기술 root `Query`, `Mutation` |
| `shared/` | transport scalar와 pagination 같은 platform contract |
| `challenger/` | Challenger 도메인이 제공하는 part, track, status enum |
| `form/` | Form 도메인이 제공하는 표준 form 구조 |
| `member/` | Member 조회 요청과 표준 Member 응답 |
| `organization/` | Gisu, Chapter, School 조회 요청과 표준 응답 |
| `project/` | Project 조회 요청과 Project 전용 응답 projection |
| `recruiting/` | Recruiting 요청과 Recruiting 전용 응답 projection |

각 도메인의 `request.graphqls`는 root operation과 input을, `response.graphqls` 또는
`output.graphqls`는 도메인이 제공하는 output과 enum을 소유한다. 조회나 입력 계약이 없는 도메인은
불필요한 빈 파일을 만들지 않는다.

## 소유권 규칙

1. 도메인이 표준으로 제공하는 resource는 제공 도메인의 output에 한 번만 선언한다.
2. 소비 도메인이 의미를 바꾸지 않으면 제공 타입을 직접 참조한다. 예: `Project.productOwner: Member`.
3. 소비 도메인이 필터링하거나 정책 필드를 결합하면 자체 타입과 converter를 둔다. 예:
   `ProjectApplicationForm`, `RecruitingApplicationFormStructure`.
4. 과거 값을 보존해야 하면 소비 도메인이 snapshot 타입 또는 scalar field를 소유한다. 예:
   `ProjectApplicant.schoolName`, `RecruitingSeasonSummary.chapterName`.
5. 외래 도메인 aggregate 객체를 Java domain model에 직접 보관하지 않는다. GraphQL 관계는 ID와
   resolver/use case 조합으로 해석한다.

## IDL 배포와 조립

도메인 IDL은 해당 도메인의 표준 request/response 계약을 나타내지만, 참조 타입까지 복제하지 않는다.
독립 서비스로 분리할 때는 대상 도메인 IDL과 `shared` 및 직접 참조한 provider IDL을 함께 조립하거나,
경계 API를 ID-only 계약으로 바꾸고 소비 서비스에서 자체 projection으로 변환한다.

Spring GraphQL은 `classpath*:graphql/**/*.graphqls`를 모두 읽는다. 따라서 모든 root operation은
`extend type Query` 또는 `extend type Mutation`으로 선언하고, 이름이 같은 type을 여러 파일에서
재선언하지 않는다.

## 문서화 규칙

- API 사용자에게 필요한 설명은 `#` 주석이 아니라 GraphQL description(`"""..."""`)으로 작성한다.
- 외부 도메인 ID에는 소유 도메인과 연결 대상 type을 명시한다.
- 변환된 type에는 원본 도메인과 변환 이유를 명시한다.
- 필드 추가 시 해당 도메인 README의 관계 표와 architecture test를 함께 갱신한다.
