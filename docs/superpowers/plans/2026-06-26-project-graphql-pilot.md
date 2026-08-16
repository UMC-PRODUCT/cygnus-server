# Project GraphQL Pilot 계획

## Summary

- 기존 organization GraphQL pilot 위에 project 도메인 조회를 GraphQL답게 확장한다.
- 이번 범위는 Query만 포함하며 Mutation은 제외한다.
- 목표 응답 구조는 `project -> members -> application`, `project -> applicationForm -> sections -> questions -> options` 형태의 중첩 그래프다.
- REST DTO/assembler를 GraphQL에 직접 노출하지 않고 `project/adapter/in/graphql` 전용 DTO와 resolver를 둔다.
- Project 도메인은 공개 데이터가 아니므로 `/graphql` 경로가 public이어도 project resolver에서 REST와 동등한 권한 검사를 수행한다.
- SpectaQL, Apollo Sandbox embedded page, 정적 GraphQL 문서 배포, `npx` 기반 실행 도구는 이번 범위에서 제외한다.

## Current Commit Status

- `19758c14 feat: add organization graphql pilot`
- `c797c3ad chore: configure graphql local tooling and metrics`
- `daa0227c refactor: resolve organization graphql fields lazily`

위 3개 작업은 이미 단위별로 커밋되어 있다. 이후 project GraphQL 작업도 아래 단위로 커밋한다.

## Target Query Shape

```graphql
query ProjectPilot($projectId: ID!) {
  project(id: $projectId) {
    id
    status
    name
    description
    thumbnailImageUrl
    logoImageUrl
    externalLink
    gisuId
    chapterId
    productOwner {
      memberId
      nickname
      name
      schoolName
    }
    coProductOwners {
      memberId
      nickname
      name
      schoolName
    }
    partQuotas {
      part
      quota
      currentCount
      remainingCount
    }
    members {
      projectMemberId
      member {
        memberId
        nickname
        name
        schoolName
      }
      part
      leader
      description
      decidedAt
      status
      application {
        applicationId
        applicantPart
        status
        submittedAt
        statusChangedAt
        formResponse {
          sections {
            questions {
              questionId
              title
              answer {
                textValue
                selectedOptions {
                  questionOptionId
                  answeredAsContent
                }
              }
            }
          }
        }
      }
    }
    applicationForm {
      applicationFormId
      title
      description
      sections {
        sectionId
        type
        allowedParts
        title
        orderNo
        questions {
          questionId
          type
          title
          required
          orderNo
          options {
            optionId
            content
            orderNo
            other
          }
        }
      }
    }
  }
}
```

## Schema Draft

```graphql
scalar Long

extend type Query {
  project(id: ID!): Project
  projects(input: ProjectSearchInput!, page: ProjectPageInput): ProjectPage!
}

input ProjectSearchInput {
  gisuId: ID!
  keyword: String
  chapterId: ID
  productOwnerSchoolIds: [ID!]
  parts: [ChallengerPart!]
  partQuotaStatus: PartQuotaStatus
  statuses: [ProjectStatus!]
}

input ProjectPageInput {
  page: Int = 0
  size: Int = 20
  sort: [ProjectSort!] = [CREATED_AT_ASC, NAME_ASC]
}

type ProjectPage {
  content: [Project!]!
  page: Int!
  size: Int!
  totalElements: Long!
  totalPages: Int!
  hasNext: Boolean!
}

type Project {
  id: ID!
  status: ProjectStatus!
  name: String!
  description: String
  thumbnailImageUrl: String
  logoImageUrl: String
  externalLink: String
  gisuId: ID!
  chapterId: ID!
  productOwner: MemberBrief
  coProductOwners: [MemberBrief!]!
  partQuotas: [ProjectPartQuota!]!
  createdAt: String!
  updatedAt: String!
  members: [ProjectMember!]!
  applicationForm: ProjectApplicationForm
}

type ProjectMember {
  projectMemberId: ID!
  projectId: ID!
  member: MemberBrief!
  part: ChallengerPart!
  leader: Boolean!
  description: String
  decidedAt: String
  status: ProjectMemberStatus!
  application: ProjectApplication
}

type ProjectApplication {
  applicationId: ID!
  applicant: ProjectApplicant!
  applicantPart: ChallengerPart!
  matchingRound: ProjectMatchingRoundBrief
  status: ProjectApplicationViewStatus
  submittedAt: String
  statusChangedAt: String
  formResponse: ProjectApplicationFormResponse
}

type ProjectApplicationForm {
  projectId: ID!
  applicationFormId: ID!
  title: String!
  description: String
  sections: [ApplicationFormSection!]!
}

type ApplicationFormSection {
  sectionId: ID!
  type: FormSectionType!
  allowedParts: [ChallengerPart!]!
  title: String!
  description: String
  orderNo: Int!
  questions: [ApplicationFormQuestion!]!
}

type ApplicationFormQuestion {
  questionId: ID!
  type: QuestionType!
  title: String!
  description: String
  required: Boolean!
  orderNo: Int!
  options: [ApplicationFormOption!]!
}

type ApplicationFormOption {
  optionId: ID!
  content: String!
  orderNo: Int!
  other: Boolean!
}

type MemberBrief {
  memberId: ID!
  nickname: String
  name: String
  schoolName: String
}

type ProjectPartQuota {
  part: ChallengerPart!
  quota: Int!
  currentCount: Int!
  remainingCount: Int!
}

type ProjectApplicant {
  memberId: ID!
  nickname: String
  name: String
  schoolName: String
  part: ChallengerPart!
}

type ProjectMatchingRoundBrief {
  id: ID!
  type: MatchingType!
  phase: MatchingRoundPhaseView!
}

type ProjectApplicationFormResponse {
  formResponseId: ID!
  formId: ID!
  status: FormResponseStatus!
  submittedAt: String
  lastSavedAt: String
  sections: [ProjectApplicationResponseSection!]!
}

type ProjectApplicationResponseSection {
  sectionId: ID!
  type: FormSectionType!
  allowedParts: [ChallengerPart!]!
  title: String!
  description: String
  orderNo: Int!
  questions: [ProjectApplicationResponseQuestion!]!
}

type ProjectApplicationResponseQuestion {
  questionId: ID!
  type: QuestionType!
  title: String!
  description: String
  required: Boolean!
  orderNo: Int!
  options: [ApplicationFormOption!]!
  answer: ProjectApplicationAnswer
}

type ProjectApplicationAnswer {
  answerId: ID!
  answeredAsType: QuestionType!
  textValue: String
  selectedOptions: [ProjectApplicationSelectedOption!]!
  files: [ProjectApplicationFile!]!
  times: [String!]!
}

type ProjectApplicationSelectedOption {
  questionOptionId: ID
  answeredAsContent: String!
}

type ProjectApplicationFile {
  fileId: ID!
  originalFileName: String!
  url: String!
}

enum ProjectSort {
  CREATED_AT_ASC
  CREATED_AT_DESC
  NAME_ASC
  NAME_DESC
}
```

필드 nullability는 권한과 데이터 부재를 반영한다.

- `project(id)`는 권한 거부/미존재를 GraphQL error로 반환할 수 있으므로 nullable로 둔다.
- `Project.applicationForm`은 아직 지원 폼이 없을 수 있으므로 nullable이다.
- `ProjectMember.application`은 랜덤 매칭/운영진 강제 배정처럼 지원서 없이 생성된 멤버가 있으므로 nullable이다.
- Java DTO의 `isLeader`, `isRequired`, `isOther`는 GraphQL field에서 `leader`, `required`, `other`로 노출한다.

## Resolver Design

### Controller

- 새 inbound adapter: `src/main/java/com/umc/product/project/adapter/in/graphql/ProjectGraphQlController.java`
- DTO 위치: `src/main/java/com/umc/product/project/adapter/in/graphql/dto`
- schema 위치: `src/main/resources/graphql/project.graphqls`
- REST `ProjectResponseAssembler`, `ProjectApplicationResponseAssembler`는 직접 사용하지 않는다.
- GraphQL resolver는 application `port/in/query` usecase만 호출한다.

### Root Queries

- `project(id)`는 `GetProjectUseCase.getById(projectId)`를 호출한다.
- `projects(input, page)`는 `SearchProjectUseCase.search(query, requesterMemberId)`를 호출한다.
- search input은 REST `SearchProjectRequest`를 재사용하지 않고 GraphQL 전용 request record에서 `SearchProjectQuery.forChallenger/forAdmin`으로 변환한다.
- `Pageable`은 GraphQL request DTO에서 정규화한다. `page < 0`, `size <= 0`, 과도한 `size`는 record 생성자에서 차단한다.

### Nested Fields

- `Project.members`
  - `@BatchMapping(typeName = "Project", field = "members")`
  - 기존 `LoadProjectMemberPort.listByProjectIds`는 outbound port이므로 GraphQL adapter에서 직접 호출하지 않는다.
  - `GetProjectMemberUseCase`를 새 query port로 추가하고 `ProjectMemberQueryService`가 구현한다.
  - 반환 DTO는 `ProjectMemberInfo`를 확장하거나 GraphQL 전용 source DTO가 `applicationId`를 가질 수 있도록 query contract를 보강한다.

- `Project.applicationForm`
  - `@BatchMapping(typeName = "Project", field = "applicationForm")`
  - 현재 `GetProjectApplicationFormUseCase.findByProjectId(projectId, requesterMemberId)`는 단건 조회다.
  - project 목록에서 `applicationForm`을 선택할 수 있으므로 `findAllByProjectIds(projectIds, requesterMemberId)` 형태의 query contract를 추가한다.
  - 내부에서는 기존 마스킹 정책을 유지한다. 호출자의 역할/파트에 따라 노출 가능한 section만 내려간다.

- `ProjectMember.member`
  - `@BatchMapping(typeName = "ProjectMember", field = "member")`
  - `GetMemberUseCase`의 batch 조회 API가 있으면 사용하고, 없으면 먼저 application query contract를 확인한 뒤 N+1 없이 보강한다.

- `ProjectMember.application`
  - `@BatchMapping(typeName = "ProjectMember", field = "application")`
  - `applicationId == null`이면 `null`을 반환한다.
  - `PROJECT_APPLICATION:READ` 권한을 통과한 지원서만 반환한다.
  - 현재 `GetProjectApplicationDetailUseCase.getDetail(query)`는 단건 중심이므로 pilot에서는 단건 resolver로 시작하지 않고 batch detail query contract를 추가한 뒤 구현한다.

### Why This Is GraphQL-like

- `includeMembers`, `includeApplicationForm`, `includeApplication` 같은 REST식 옵션을 두지 않는다.
- 클라이언트가 selection set에 넣은 field만 resolver가 실행되도록 한다.
- heavy field는 root DTO에 미리 합치지 않고 field resolver로 분리한다.
- 목록과 nested field는 `@BatchMapping`으로 묶어 N+1을 방지한다.

## Authorization Plan

Project GraphQL은 organization처럼 public data가 아니다.

- `/graphql` 경로는 organization pilot 때문에 public path로 유지한다.
- project resolver는 인증된 member를 SecurityContext에서 직접 확인하거나 GraphQL 전용 current member resolver를 둔다.
- `project(id)`, `Project.members`, `Project.applicationForm`은 `PROJECT:READ` 권한을 검사한다.
- `ProjectMember.application`은 `PROJECT_APPLICATION:READ` 권한을 검사한다.
- batch resolver에서는 `CheckPermissionUseCase.loadSubject(memberId)`를 한 번 호출하고, source별 `ResourcePermission`을 평가해 불필요한 subject 재조회가 반복되지 않게 한다.
- 권한이 없는 root field는 GraphQL error로 반환한다.
- 권한이 없는 nested application field는 보안 정책 결정이 필요하다. pilot 기본값은 error 대신 `null` 반환으로 두고, 에러 확장이 필요하면 `DataFetcherResult`로 path-specific error를 추가한다.

## Runtime Metrics

- Spring GraphQL runtime metrics는 기존 작업에서 활성화했다.
- project resolver는 operation name을 명시한 query 예시를 문서화해 `/actuator/prometheus`에서 GraphQL operation 단위 지표를 구분할 수 있게 한다.
- 비즈니스 통계(`ProjectStatisticsInfo`)는 이번 nested pilot 범위에 포함하지 않는다. 필요하면 후속으로 `Project.statistics` field를 별도 설계한다.

## Implementation Units

1. `docs: plan project graphql pilot`
   - 이 문서 추가.

2. `feat: add project graphql schema`
   - `project.graphqls` 추가.
   - `ProjectPage.totalElements`에 필요한 `Long` scalar를 등록하고 scalar wiring test를 추가한다.
   - enum/field naming을 기존 domain enum과 맞춘다.
   - Query 확장 충돌 여부를 schema startup test로 확인한다.

3. `feat: add project graphql query contracts`
   - `GetProjectMemberUseCase` 추가.
   - `GetProjectApplicationFormUseCase` batch 조회 보강.
   - `ProjectMemberInfo` 또는 신규 query DTO에 `applicationId`를 포함한다.
   - 필요 시 `GetProjectApplicationDetailUseCase` batch 조회를 보강한다.

4. `feat: resolve project graphql queries`
   - `ProjectGraphQlController` 추가.
   - root query와 `@BatchMapping` 구현.
   - GraphQL 전용 request/response DTO 추가.
   - resolver 단위 권한 검사 추가.

5. `test: cover project graphql pilot`
   - GraphQL slice test와 권한/N+1/batch resolver 테스트 추가.
   - 기존 REST project controller 회귀 테스트 실행.

## Test Plan

### GraphQL Slice

- `@GraphQlTest(ProjectGraphQlController.class)`와 `GraphQlTester`를 사용한다.
- `project(id)`가 `GetProjectUseCase.getById`를 호출하고 기본 field를 응답하는지 검증한다.
- `project { members { ... } }` 선택 시 `GetProjectMemberUseCase.listByProjectIds`가 한 번 호출되는지 검증한다.
- `project { applicationForm { sections { questions { options }}}}` 선택 시 form 구조와 `required/other` field가 응답되는지 검증한다.
- `projects { content { members { ... } } }`에서 여러 project가 반환되어도 members batch resolver가 project 묶음으로 한 번 호출되는지 검증한다.
- `ProjectMember.application`은 `applicationId == null`일 때 `null`을 반환하는지 검증한다.
- selector에 없는 nested field의 usecase가 호출되지 않는지 검증한다.
- 권한 거부 시 root `project`는 GraphQL error를 반환하는지 검증한다.
- nested `application` 권한 거부는 pilot 정책대로 `null` 또는 path-specific error를 반환하는지 검증한다.

### Existing REST Regression

- `ProjectQueryControllerTest`
- `ProjectApplicationQueryControllerTest`
- `ProjectApplicationFormControllerTest`
- `ProjectPermissionControllerTest`
- `ProjectStatisticsQueryControllerTest`

### Build/Smoke

- `./gradlew test --tests com.umc.product.project.adapter.in.graphql.ProjectGraphQlControllerTest`
- `./gradlew test --tests com.umc.product.project.adapter.in.web.ProjectQueryControllerTest --tests com.umc.product.project.adapter.in.web.ProjectApplicationQueryControllerTest --tests com.umc.product.project.adapter.in.web.ProjectApplicationFormControllerTest`
- `./gradlew spotlessCheck`
- `GRAPHQL_GRAPHIQL_ENABLED=true ./gradlew bootRun`
- `/graphiql`에서 인증 토큰을 HTTP header에 넣고 `ProjectPilot` query 실행
- 인증 없이 project query를 실행하면 권한 error가 나는지 확인

## Risks

- 현재 `/graphql` 경로는 public이므로 project resolver 권한 검사를 빠뜨리면 민감 데이터가 노출될 수 있다.
- REST assembler는 일부 outbound port를 직접 호출한다. GraphQL adapter가 이를 재사용하면 hexagonal boundary가 더 흐려지므로 application query port를 먼저 보강해야 한다.
- application form은 호출자 역할/파트에 따라 section masking이 달라진다. batch 조회에서도 단건 REST와 동일한 masking 정책을 유지해야 한다.
- project member와 application 사이의 연결에는 `applicationId`가 필요하다. 현재 `ProjectMemberInfo`에 없는 값이므로 query contract 보강 없이 resolver를 구현하면 불필요한 entity 노출이나 N+1이 발생한다.

## Deferred

- Project Mutation
- Project business statistics GraphQL field
- SpectaQL 문서 자동화
- Apollo Sandbox embedded page
- 정적 GraphQL 문서 배포
- `npx` 기반 tooling
