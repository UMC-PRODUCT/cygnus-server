# GraphQL Onboarding

UMC PRODUCT 서버의 GraphQL pilot 실행 방법과 IDL 관리 원칙을 설명한다.

## 현재 범위

GraphQL public contract는 application inbound port가 있는 23개 도메인에 적용한다.

| 분류 | 도메인 |
|---|---|
| Identity·Access | Authentication, Authorization, Member, Challenger |
| Organization·Operation | Organization, Schedule, Curriculum, Analytics, Audit |
| Content·Communication | Blog, Community, Notice, Chat, Notification |
| Product workflow | Form, Feedback, Project, Recruiting |
| Platform resource | Storage, Term, Certificate, Maintenance, Documentation |

`common`, `global`은 runtime/shared layer이고, `llm`은 내부 port이다. 구현된 inbound use case가 없는
`figma`, `survey`와 개발 전용 `test`는 public GraphQL IDL에서 제외한다. 이 분류는
`GraphQlDomainCoverageTest`가 검증한다.

상세 문서는 다음을 참고한다.

- [GraphQL Schema 관계](schema-relationships.md): provider IDL, 직접 참조, projection, snapshot
- [GraphQL 권한 관리](authorization.md): root와 nested field의 인증·인가
- [GraphQL Schema Snapshot](../../graphql-schema.md): 현재 주요 type 관계도
- [IDL 디렉터리 가이드](../../../src/main/resources/graphql/README.md): 파일별 선언과 변경 규칙

## IDL 구조

Spring GraphQL은 `src/main/resources/graphql/**/*.graphqls`를 하나의 runtime schema로 조립한다.
동시에 각 하위 디렉터리는 해당 도메인이 외부에 제공하는 표준 IDL 경계를 나타낸다.

| 경로 | 역할 |
|---|---|
| `schema.graphqls` | 빈 기술 root `Query`, `Mutation` |
| `shared/scalars.graphqls` | owner가 없는 transport scalar |
| `shared/pagination.graphqls` | 표준 `PageInput`, `PageInfo` platform contract |
| `challenger/output.graphqls` | Challenger enum |
| `form/output.graphqls` | Form 표준 output |
| `{domain}/request.graphqls` | root operation과 input |
| `{domain}/response.graphqls` | output과 domain enum |
| `{domain}/README.md` | field 의미와 cross-domain 관계 |

도메인 root는 모두 `extend type Query` 또는 `extend type Mutation`으로 추가한다. type 이름은 전역
namespace를 사용하므로 같은 선언을 여러 파일에서 반복하지 않는다.

## Provider와 Consumer

제공 도메인은 자신이 표준적으로 제공하는 request/response를 소유한다. 소비 도메인이 provider의
resource 의미를 바꾸지 않으면 해당 type을 직접 참조한다.

```graphql
type Project {
  productOwner: MemberPublic
}
```

필터링, 정책 필드 결합, 과거 값 보존이 필요하면 소비 도메인이 자체 projection 또는 snapshot을
소유하고 adapter 경계에 converter를 둔다.

```graphql
type ProjectApplicationForm {
  sections: [ProjectApplicationFormSection!]!
}

type RecruitingApplicationFormStructure {
  sections: [RecruitingFormSection!]!
}
```

도메인 IDL만 별도로 배포할 때 참조한 provider type까지 복제해서 넣지 않는다. composition 단계에서
`shared`와 provider IDL을 함께 조립하거나, 서비스 경계를 ID-only request/response로 바꾼 뒤 소비
서비스가 자체 projection으로 변환한다.

## 실행

GraphQL endpoint는 `POST /graphql`이다.

```bash
./gradlew bootRun
```

local과 dev profile에서는 다음 도구를 사용할 수 있다.

```text
http://localhost:8080/graphiql
http://localhost:8080/docs/apollo-sandbox.html
```

실행 schema의 root field는 introspection으로 확인한다.

```bash
curl -s http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"query { __schema { queryType { fields { name } } } }"}'
```

## Query 작성

같은 domain identity는 하나의 canonical type을 사용하고 필요한 field만 selection set으로 고른다.

```graphql
query ProjectMembers($projectId: ID!) {
  project(id: $projectId) {
    id
    productOwner {
      memberId
      name
      school { id name }
    }
    applicationForm {
      sections {
        title
        type
        allowedParts
        questions { questionId title }
      }
    }
  }
}
```

## 구현 규칙

- API 설명은 GraphQL description(`"""..."""`)으로 작성한다.
- Offset pagination은 `PageInput`을 받고 domain page의 `pageInfo: PageInfo!`로 metadata를 반환한다.
- 외부 도메인 ID field에는 대상 owner와 resource를 설명한다.
- resolver는 application inbound use case만 호출한다.
- nested collection은 batch use case, `@BatchMapping`, DataLoader로 N+1을 방지한다.
- 권한에 따라 값이 없을 수 있는 field는 nullable 계약과 fail-closed resolver를 함께 설계한다.
- schema, DTO, converter, resolver, README, architecture test를 같은 변경에서 맞춘다.
- OAuth redirect/token 교환, binary download, WebSocket, webhook, email 같은 transport protocol은
  해당 REST/WebSocket adapter를 유지하고 GraphQL resource로 억지로 변환하지 않는다.
- 공개 field 삭제, type 변경, nullability 강화, enum value 삭제는 breaking change다.

```bash
./gradlew test --tests '*GraphQl*'
./gradlew test
```
