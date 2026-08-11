# GraphQL Onboarding

이 문서는 UMC PRODUCT 서버의 GraphQL API를 실행하고 Relay 계약에 맞게 query와 mutation을 작성하는 방법을
설명한다. 세부 구현 규칙은 [`docs/graphql-relay-conventions.md`](../graphql-relay-conventions.md)를 함께 참고한다.

## 현재 범위

GraphQL endpoint는 `POST /graphql` 하나이며 다음 도메인을 제공한다.

- `organization`: Gisu, Chapter, School 조회
- `member`: 현재 회원, 회원 단건·검색, 소속 관계 조회
- `project`: 프로젝트 단건·검색과 멤버·지원서·지원서 양식 조회
- `recruiting`: 모집 공개/관리 조회와 모집 관련 mutation

스키마는 `src/main/resources/graphql` 아래 파일을 하나로 합쳐 로드한다.

```text
relay.graphqls
organization.graphqls
member.graphqls
project.graphqls
recruiting.graphqls
```

`relay.graphqls`가 `Query`, `Node`, `PageInfo`를 선언하고 도메인 파일은 `extend type Query`를 사용한다.
`recruiting.graphqls`는 현재 유일한 root `Mutation` 선언도 포함한다.

## Relay 공통 계약

### Global Object Identification

스키마의 모든 `ID`는 raw DB ID가 아닌 불투명 전역 ID다. 서버는
`base64url("{TypeName}:{rawId}")` 형식으로 인코딩하지만 클라이언트는 내부 형식을 해석하지 않고 받은 값을
그대로 다음 요청에 사용해야 한다.

다음 타입은 `Node`를 구현한다.

- `Member`
- `Gisu`
- `Chapter`
- `School`
- `Project`
- `RecruitingApplication`
- `RecruitingSeason`

모든 Node는 `id: ID!`를 제공하며 root query로 다시 조회할 수 있다.

```graphql
query NodeById($id: ID!) {
  node(id: $id) {
    id
    ... on Member {
      name
      nickname
    }
  }
}
```

여러 Node를 조회할 때는 입력 순서와 길이를 보존하는 `nodes(ids:)`를 사용한다. 존재하지 않거나 지원하지 않는
ID 위치에는 `null`이 들어간다.

```graphql
query NodesById($ids: [ID!]!) {
  nodes(ids: $ids) {
    id
  }
}
```

### Connection 페이지네이션

root 목록 query는 모두 `Connection`을 반환한다.

```graphql
type ExampleConnection {
  edges: [ExampleEdge!]!
  pageInfo: PageInfo!
  totalCount: Long!
}

type ExampleEdge {
  cursor: String!
  node: Example!
}
```

공통 인자는 다음과 같다.

- forward: `first`, `after`
- backward: `last`, `before`
- `first`와 `last`를 모두 생략하면 `first: 20`
- `first`, `last` 허용 범위는 0~100
- 둘을 함께 전달하면 cursor 범위에 `first`, `last` 순서로 적용
- cursor는 opaque 문자열이므로 `page * size`로 만들거나 수정하지 않음

다음 페이지는 이전 응답의 `pageInfo.endCursor`를 `after`에 전달한다.

```graphql
query Members($first: Int, $after: String) {
  members(first: $first, after: $after) {
    edges {
      cursor
      node {
        memberId
        name
      }
    }
    pageInfo {
      hasNextPage
      hasPreviousPage
      startCursor
      endCursor
    }
    totalCount
  }
}
```

Connection 크기는 GraphQL complexity에 반영된다. 범위를 벗어난 크기는 최대 크기 비용으로 계산하므로 alias나
잘못된 인자로 전역 복잡도 제한을 우회할 수 없다.

## 실행과 도구

로컬 실행:

```bash
./gradlew bootRun
```

`bootRun`은 shell 환경변수를 상속하지만 `.env`를 자동으로 읽지 않는다.

```bash
set -a
source .env
set +a
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

실행 중인 schema 확인:

```bash
curl -s http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"query { __schema { queryType { fields { name } } } }"}'
```

`/graphql`은 transport rate limit 대상이다. 기본 한도는 인증 요청 초당 20회·분당 300회, 익명 요청 초당
5회·분당 60회이며 초과 시 HTTP 429를 반환한다.

## 인증과 권한

`/graphql` 경로 자체는 public path지만 private resolver는 `MemberPrincipal`과 권한 UseCase를 통해 접근을
검사한다. GraphiQL 또는 Apollo Sandbox에서는 다음 header를 넣는다.

```json
{
  "Authorization": "Bearer <access-token>"
}
```

주요 정책:

| 범위 | 권한 처리 |
| --- | --- |
| organization 조회 | 공개 |
| `me` | 로그인 필요, private Member 표현 |
| `member`, `members`, Member `node` | `MEMBER:READ` 검사 |
| `project`, `projects`, Project `node` | `PROJECT:READ` 검사 |
| `ProjectMember.application` | `PROJECT_APPLICATION:READ`, 권한 없으면 `null` |
| recruiting 관리 API | 모집 리소스별 권한 검사 |

같은 요청자가 같은 전역 ID의 Member를 `me`, `member(id:)`, `node(id:)`로 조회하면 동일한 필드 값을 받는다.
다른 회원의 public 표현에서는 `email`, `status`가 `null`이다.

## 도메인별 예시

### Organization

활성 기수와 관계를 조회한다.

```graphql
query {
  activeGisu {
    id
    generation
    active
    chapters {
      chapterId
      chapterName
      schools {
        schoolId
        schoolName
      }
    }
    schools {
      schoolId
      schoolName
      chapterName
    }
  }
}
```

기수 목록은 `ids`, `generations`, `active` 중 정확히 하나를 selector로 받는다.

```graphql
query {
  gisus(filter: { active: true }, first: 10) {
    edges {
      node {
        id
        generation
      }
    }
    pageInfo { hasNextPage endCursor }
  }
}
```

### Member

본인 프로필은 private 표현이다.

```graphql
query {
  me {
    id
    name
    nickname
    email
    status
    school {
      id
      schoolName
    }
  }
}
```

회원 검색의 관계 필드는 selection set에 포함된 경우에만 BatchMapping으로 조회한다.

```graphql
query Members($gisuId: ID!, $first: Int, $after: String) {
  members(
    filter: { keyword: "kim", gisuId: $gisuId, part: SPRINGBOOT }
    first: $first
    after: $after
  ) {
    edges {
      cursor
      node {
        memberId
        name
        nickname
        school { id schoolName }
        currentChallenger {
          challengerId
          part
          gisu { id generation }
        }
      }
    }
    pageInfo { hasNextPage endCursor }
    totalCount
  }
}
```

검색 권한 scope는 pagination과 count 전에 DB query에 적용된다. 결과 `email`은 원문을 노출하지 않고 마스킹한다.

### Project

`filter`의 ID도 전역 ID다. `orderBy`를 생략하면 `CREATED_AT_ASC`, `NAME_ASC`를 사용하고 최종적으로
`project.id`를 tie-breaker로 적용한다.

```graphql
query Projects($gisuId: ID!, $after: String) {
  projects(
    filter: { gisuId: $gisuId, keyword: "server" }
    orderBy: [CREATED_AT_DESC, NAME_ASC]
    first: 20
    after: $after
  ) {
    edges {
      node {
        id
        name
        status
        productOwner { memberId name schoolName }
        members {
          projectMemberId
          part
          member { memberId name }
          application { applicationId status submittedAt }
        }
      }
    }
    pageInfo { hasNextPage endCursor }
    totalCount
  }
}
```

### Recruiting Mutation

모든 mutation은 하나의 non-null `input`과 mutation별 Payload를 사용한다. ID 입력과 출력은 전역 ID다.

```graphql
mutation CreateDraft($input: CreateRecruitingApplicationDraftInput!) {
  createRecruitingApplicationDraft(input: $input) {
    applicationId
    applicationKey
    status
  }
}
```

```json
{
  "input": {
    "applicationFormId": "<RecruitingApplicationForm global ID>",
    "applicantName": "지원자",
    "applicantEmail": "applicant@example.com",
    "firstChoice": "PLAN"
  }
}
```

## Resolver 구현 규칙

GraphQL adapter는 `adapter/in/graphql`에 두고 application inbound port만 호출한다.

```text
adapter/in/graphql -> application/port/in/query 또는 command
```

금지 사항:

```text
adapter/in/graphql -> adapter/out/persistence  금지
adapter/in/graphql -> JpaRepository            금지
GraphQL response -> Domain Entity 직접 반환     금지
```

- request DTO에서 전역 ID를 raw ID로 디코딩한다.
- response DTO에서 raw ID를 전역 ID로 인코딩한다.
- Node는 `RelayNode`와 도메인별 `NodeFetcher`를 구현한다.
- root 목록은 Connection을 반환한다.
- 반복되는 nested field는 `@BatchMapping`으로 N+1을 피한다.
- command와 query UseCase 경계를 유지한다.

## 테스트와 변경 체크리스트

주요 테스트:

```text
src/test/java/com/umc/product/global/graphql/relay
src/test/java/com/umc/product/organization/adapter/in/graphql
src/test/java/com/umc/product/member/adapter/in/graphql
src/test/java/com/umc/product/project/adapter/in/graphql
src/test/java/com/umc/product/recruiting/adapter/in/graphql
```

실행:

```bash
./gradlew test --tests 'com.umc.product.*.adapter.in.graphql.*' \
  --tests 'com.umc.product.global.graphql.*'
```

변경 시 확인한다.

- SDL과 GraphQL DTO의 필드·nullability가 일치하는가
- 모든 `ID`가 adapter 경계에서 전역 ID로 변환되는가
- root 목록이 Edge와 PageInfo를 포함한 Connection인가
- Node 단건 query와 `node(id:)`의 권한·표현이 같은가
- mutation이 단일 `input`과 전용 Payload를 사용하는가
- QueryRepository가 임의 cursor offset에 `pageable.getOffset()`을 사용하는가
- 정렬에 고유한 tie-breaker가 있는가
- slice test와 schema introspection test가 새 계약을 고정하는가
