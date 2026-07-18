# GraphQL Onboarding

이 문서는 UMC PRODUCT 서버의 GraphQL pilot을 로컬과 dev 환경에서 실행하고 schema와 query를 확인하는 방법을 정리한다.

## 현재 범위

GraphQL은 아직 pilot 범위이며 Query만 제공한다. Mutation은 제공하지 않는다.

- `organization`: `gisu`, `chapter`, `school` 조회
- `member`: `me`, `member`, `members`, `memberSearch` 조회와 회원의 학교·챌린저 nested field
- `project`: `project`, `projects` 조회와 멤버·지원서·지원 폼 nested field

주제별 상세 문서는 다음과 같다.

- [GraphQL Schema 관계](schema-relationships.md): unified SDL 소유권, 공통 interface, domain 간 type 관계
- [GraphQL 권한 관리](authorization.md): root와 nested field의 인증·인가, batch 처리, partial response
- [GraphQL Schema Snapshot](../../graphql-schema.md): 현재 주요 type과 field 관계도

## Unified Schema

Spring GraphQL은 `src/main/resources/graphql`의 모든 `*.graphqls`를 하나의 schema로 합친다.

| 파일 | 소유하는 계약 |
| --- | --- |
| `common.graphqls` | `Long`, `ChallengerPart` |
| `form.graphqls` | 공통 Form interface, 질문·옵션 type, form enum |
| `member.graphqls` | Member query와 canonical `Member` type |
| `organization.graphqls` | root `Query`, canonical `Gisu`, `Chapter`, `School` type |
| `project.graphqls` | Project query와 Project 전용 concrete type |

`organization.graphqls`가 `type Query`를 선언하고 `member.graphqls`와 `project.graphqls`가
`extend type Query`로 field를 추가한다. 파일은 소유권을 나눌 뿐 endpoint나 type namespace를 분리하지 않는다.

## 실행

GraphQL endpoint는 다음 하나다.

```text
POST /graphql
```

로컬 서버를 실행한다.

```bash
./gradlew bootRun
```

local과 dev profile에서는 GraphiQL을 사용할 수 있다.

```text
http://localhost:8080/graphiql
```

Apollo Sandbox 정적 페이지도 제공한다.

```text
http://localhost:8080/docs/apollo-sandbox.html
```

실행 중인 schema의 root field는 introspection으로 확인할 수 있다.

```bash
curl -s http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"query { __schema { queryType { fields { name } } } }"}'
```

## Query 작성

필요한 field만 selection set에 적는다. 동일한 회원 identity는 진입 경로와 관계없이 `Member` 하나로 표현한다.

```graphql
query ProjectMembers($projectId: ID!) {
  project(id: $projectId) {
    id
    name
    productOwner {
      memberId
      name
      school {
        id
        name
      }
    }
    applicationForm {
      title
      sections {
        title
        questions {
          questionId
          title
          options {
            optionId
            content
          }
        }
      }
    }
  }
}
```

`ProjectApplicationForm.sections`는 concrete `ApplicationFormSection`을 반환하므로 Project 전용 field도 직접 선택할 수 있다.

```graphql
query ProjectForm($projectId: ID!) {
  project(id: $projectId) {
    applicationForm {
      title
      sections {
        title
        type
        allowedParts
      }
    }
  }
}
```

## 구현 규칙

- SDL은 먼저 소유 파일을 정하고, 같은 identity의 `Summary`, `Detail`, parent-prefixed type을 추가하지 않는다.
- 여러 domain이 같은 의미와 nullability로 제공하는 field만 공통 interface에 둔다.
- resolver는 `adapter/in/graphql`에서 application Query UseCase만 호출한다.
- 반복되는 nested 조회는 `@BatchMapping`, batch Query UseCase 또는 DataLoader로 N+1을 방지한다.
- 권한에 따라 숨길 field는 nullable로 설계하고 field resolver에서 fail closed로 처리한다.
- schema, resolver, DTO, architecture test를 같은 변경에서 맞춘다.

주요 검증 명령은 다음과 같다.

```bash
./gradlew test --tests '*GraphQl*'
./gradlew test
```
