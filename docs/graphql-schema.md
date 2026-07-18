# GraphQL Schema

현재 GraphQL pilot은 여섯 SDL 파일을 Spring GraphQL이 합쳐 하나의 unified schema로 로드한다.
파일은 type 소유권을 나누지만 endpoint와 전역 type namespace는 공유한다.

| 파일 | 소유 계약 |
| --- | --- |
| `common.graphqls` | `Long`, `ChallengerPart` |
| `form.graphqls` | `Form`, `FormSection`, `FormQuestion`, `FormOption`, `FormStatus`, `FormResponseStatus`, `QuestionType` |
| `member.graphqls` | 공통 `Member` type과 member query |
| `organization.graphqls` | organization `Query`와 `Gisu`, 학교·지부 type |
| `project.graphqls` | Project query와 Project concrete extension |
| `feedback.graphqls` | Feedback query와 concrete form/section |

`ProjectApplicationForm`/`ApplicationFormSection`은 각각 `Form`/`FormSection`의 Project concrete
extension이다. `UserFeedbackTemplateForm`/`UserFeedbackTemplateSection`은 각각 같은 공용 계약을
구현하는 Feedback concrete extension이다. 질문·옵션과 form/status enum은 `form.graphqls`가 단일
소유한다.

```mermaid
flowchart TD
  Loader["Spring GraphQL schema loader"] --> Schema["unified schema"]
  Common["common.graphqls<br/>Long, ChallengerPart"] --> Loader
  FormFile["form.graphqls<br/>Form, FormSection<br/>FormQuestion, FormOption<br/>Form/status enums"] --> Loader
  MemberFile["member.graphqls<br/>Member"] --> Loader
  OrganizationFile["organization.graphqls<br/>type Query"] --> Loader
  ProjectFile["project.graphqls<br/>Project concrete types"] --> Loader
  FeedbackFile["feedback.graphqls<br/>Feedback concrete form/section"] --> Loader

  Schema --> Query["Query"]
  Member["Member queries"]
  Organization["Organization queries"]
  Project["Project queries"]
  Feedback["Feedback queries"]
  Query -->|"me, member, members, memberSearch"| Member
  Query -->|"gisuOrganizations, gisu, activeGisu, chapters, chapter, schools, school"| Organization
  Query -->|"project, projects"| Project
  Query -->|"userFeedbackTemplates, userFeedbackTemplate"| Feedback
```

## ERD 스타일 관계도

아래 diagram은 database ERD가 아니라 unified GraphQL schema의 type 관계를 나타낸다. GraphQL
interface 구현은 `<|..`, 응답 내부의 중첩 구조는 `*--`, 다른 domain type 참조는 `-->`로 표시한다.
cardinality는 schema의 nullability와 list 형태를 기준으로 작성했으며 JPA 연관관계나 foreign key를
의미하지 않는다.

### Form, Project, Feedback

```mermaid
classDiagram
  direction LR

  class Form {
    <<interface>>
    +String! title
    +String description
    +FormSection[]! sections
  }
  class FormSection {
    <<interface>>
    +ID! sectionId
    +String! title
    +Int! orderNo
    +FormQuestion[]! questions
  }
  class FormQuestion {
    +ID! questionId
    +QuestionType! type
    +Boolean! required
    +Int! orderNo
    +FormOption[]! options
  }
  class FormOption {
    +ID! optionId
    +String! content
    +Int! orderNo
    +Boolean! other
  }

  class Project {
    +ID! id
    +ProjectStatus! status
    +ProjectApplicationForm applicationForm
  }
  class ProjectApplicationForm {
    +ID! projectId
    +ID! applicationFormId
    +ApplicationFormSection[]! sections
  }
  class ApplicationFormSection {
    +FormSectionType! type
    +ChallengerPart[]! allowedParts
    +FormQuestion[]! questions
  }
  class ProjectApplicationFormResponse {
    +ID! formResponseId
    +FormResponseStatus! status
    +ProjectApplicationResponseSection[]! sections
  }
  class ProjectApplicationResponseSection {
    +ID! sectionId
    +ProjectApplicationResponseQuestion[]! questions
  }
  class ProjectApplicationResponseQuestion {
    +ID! questionId
    +FormOption[]! options
    +ProjectApplicationAnswer answer
  }
  class ProjectApplicationAnswer {
    +ID! answerId
    +QuestionType! answeredAsType
    +String textValue
  }
  class ProjectApplicationSelectedOption {
    +ID questionOptionId
    +String! answeredAsContent
  }
  class ProjectApplicationFile {
    +ID! fileId
    +String! originalFileName
    +String! url
  }

  class UserFeedbackTemplate {
    +ID! templateId
    +UserFeedbackContext! context
    +UserFeedbackTargetType! targetType
    +Boolean! active
  }
  class UserFeedbackTemplateSummary {
    +ID! templateId
    +ID! formId
    +String! title
    +Boolean! active
  }
  class UserFeedbackTemplateForm {
    +ID! formId
    +FormStatus! status
    +Boolean! anonymous
    +Boolean! allowDuplicateResponses
    +UserFeedbackTemplateSection[]! sections
  }
  class UserFeedbackTemplateSection {
    +ID! sectionId
    +FormQuestion[]! questions
  }

  Form <|.. ProjectApplicationForm
  Form <|.. UserFeedbackTemplateForm
  FormSection <|.. ApplicationFormSection
  FormSection <|.. UserFeedbackTemplateSection
  Form "1" o-- "0..*" FormSection : sections
  FormSection "1" *-- "0..*" FormQuestion : questions
  FormQuestion "1" *-- "0..*" FormOption : options

  Project "1" --> "0..1" ProjectApplicationForm : applicationForm
  ProjectApplicationForm "1" *-- "0..*" ApplicationFormSection : sections
  ApplicationFormSection "1" *-- "0..*" FormQuestion : questions
  ProjectApplicationFormResponse "1" *-- "0..*" ProjectApplicationResponseSection : sections
  ProjectApplicationResponseSection "1" *-- "0..*" ProjectApplicationResponseQuestion : questions
  ProjectApplicationResponseQuestion "1" --> "0..*" FormOption : options
  ProjectApplicationResponseQuestion "1" *-- "0..1" ProjectApplicationAnswer : answer
  ProjectApplicationAnswer "1" *-- "0..*" ProjectApplicationSelectedOption : selectedOptions
  ProjectApplicationAnswer "1" *-- "0..*" ProjectApplicationFile : files

  UserFeedbackTemplateSummary ..> UserFeedbackTemplate : list projection
  UserFeedbackTemplate "1" *-- "1" UserFeedbackTemplateForm : form
  UserFeedbackTemplateForm "1" *-- "0..*" UserFeedbackTemplateSection : sections
  UserFeedbackTemplateSection "1" *-- "0..*" FormQuestion : questions
```

Project의 `ApplicationFormSection.type`, `allowedParts`는 concrete type에만 존재한다. Feedback의
`UserFeedbackTemplateSection`은 같은 `FormSection` interface를 구현하지만 두 Project 전용 field를
노출하지 않는다.

### Member, Organization, Project

```mermaid
classDiagram
  direction LR

  class Member {
    +ID! memberId
    +ID schoolId
    +School school
    +MemberChallenger[]! challengers
  }
  class MemberChallenger {
    +ID! challengerId
    +ID! gisuId
    +ChallengerPart! part
  }
  class MemberSearchResult {
    +ID! memberId
    +School school
    +MemberSearchChallenger[]! challengerRecords
  }
  class MemberSearchChallenger {
    +ID! challengerId
    +ID! gisuId
    +ChallengerPart! part
  }

  class Gisu {
    +ID! id
    +ID! generation
    +Chapter[]! chapters
    +School[]! schools
  }
  class Chapter {
    +ID! id
    +String! name
    +School[]! schools
  }
  class School {
    +ID! id
    +String! name
    +SchoolLink[]! links
  }
  class SchoolLink {
    +SchoolLinkType! type
    +String! url
  }

  class Project {
    +ID! id
    +ID! gisuId
    +ID! chapterId
    +Member productOwner
    +Member[]! coProductOwners
    +ProjectMember[]! members
  }
  class ProjectMember {
    +ID! projectMemberId
    +ChallengerPart! part
    +Member! member
    +ProjectApplication application
  }
  class ProjectApplication {
    +ID! applicationId
    +ProjectApplicant! applicant
    +ProjectMatchingRoundBrief matchingRound
    +ProjectApplicationFormResponse formResponse
  }
  class ProjectApplicant {
    +ID! memberId
    +ChallengerPart! part
  }
  class ProjectMatchingRoundBrief {
    +ID! id
    +MatchingType! type
    +MatchingRoundPhaseView! phase
  }
  class ProjectApplicationFormResponse {
    +ID! formResponseId
  }

  Member "1" *-- "0..*" MemberChallenger : challengers
  Member "1" --> "0..1" School : school
  MemberChallenger "0..*" --> "0..1" Gisu : gisu
  MemberSearchResult "1" --> "0..1" School : school
  MemberSearchResult "1" *-- "0..*" MemberSearchChallenger : challengerRecords
  MemberSearchChallenger "0..*" --> "0..1" Gisu : gisu

  Gisu "1" *-- "0..*" Chapter : chapters
  Gisu "1" *-- "0..*" School : schools
  Chapter "1" *-- "0..*" School : schools
  School "1" *-- "0..*" SchoolLink : links

  Project "1" --> "0..1" Member : productOwner
  Project "1" --> "0..*" Member : coProductOwners
  Project "1" *-- "0..*" ProjectMember : members
  ProjectMember "0..*" --> "1" Member : member
  ProjectMember "1" *-- "0..1" ProjectApplication : application
  ProjectApplication "1" *-- "1" ProjectApplicant : applicant
  ProjectApplication "1" --> "0..1" ProjectMatchingRoundBrief : matchingRound
  ProjectApplication "1" *-- "0..1" ProjectApplicationFormResponse : formResponse
```

Project는 member schema가 소유하는 공통 `Member` type을 직접 참조한다. `Member`,
`MemberSearchResult`가 참조하는 `School`과 `Gisu`는 organization schema가 소유한다.

## 전체 Type 관계

```mermaid
flowchart LR
  subgraph COMMON_SCHEMA["common.graphqls"]
    Long["scalar Long"]
    ChallengerPart["enum ChallengerPart"]
  end

  subgraph FORM_SCHEMA["form.graphqls"]
    FormType["interface Form"]
    FormSectionType["interface FormSection"]
    FormQuestion["FormQuestion"]
    FormOption["FormOption"]
    FormStatus["FormStatus"]
    FormResponseStatus["FormResponseStatus"]
    QuestionType["QuestionType"]
    FormType -->|sections| FormSectionType
    FormSectionType -->|questions| FormQuestion
    FormQuestion -->|options| FormOption
  end

  subgraph MEMBER_SCHEMA["member.graphqls"]
    Member["Member"]
    MemberChallenger["MemberChallenger"]
    MemberSearchInput["MemberSearchInput"]
    Member -->|challengers| MemberChallenger
  end

  subgraph ORGANIZATION_SCHEMA["organization.graphqls"]
    Gisu["Gisu"]
    GisuOrganizationInput["GisuOrganizationInput"]
    GisuOrganizationPayload["GisuOrganizationPayload"]
    Chapter["Chapter"]
    School["School"]
    SchoolLink["SchoolLink"]
    GisuOrganizationPayload -->|gisus| Gisu
    Gisu -->|chapters| Chapter
    Gisu -->|schools| School
    Chapter -->|schools| School
    School -->|links| SchoolLink
    GisuOrganizationInput -.->|filters| Gisu
  end

  subgraph PROJECT_SCHEMA["project.graphqls"]
    Project["Project"]
    ProjectPage["ProjectPage"]
    ProjectSearchInput["ProjectSearchInput"]
    ProjectMember["ProjectMember"]
    ProjectPartQuota["ProjectPartQuota"]
    ProjectApplicationForm["ProjectApplicationForm"]
    ApplicationFormSection["ApplicationFormSection"]
    ProjectApplication["ProjectApplication"]
    ProjectApplicationFormResponse["ProjectApplicationFormResponse"]
    ProjectApplicationResponseSection["ProjectApplicationResponseSection"]
    ProjectApplicationResponseQuestion["ProjectApplicationResponseQuestion"]
    ProjectApplicationAnswer["ProjectApplicationAnswer"]
    ProjectApplicationSelectedOption["ProjectApplicationSelectedOption"]
    ProjectApplicationFile["ProjectApplicationFile"]
    ProjectApplicant["ProjectApplicant"]
    ProjectMatchingRoundBrief["ProjectMatchingRoundBrief"]

    ProjectPage -->|content| Project
    Project -->|productOwner, coProductOwners| Member
    Project -->|partQuotas| ProjectPartQuota
    Project -->|members| ProjectMember
    Project -->|applicationForm| ProjectApplicationForm
    ProjectMember -->|member| Member
    ProjectMember -->|application| ProjectApplication
    ProjectApplication -->|applicant| ProjectApplicant
    ProjectApplication -->|matchingRound| ProjectMatchingRoundBrief
    ProjectApplication -->|formResponse| ProjectApplicationFormResponse
    ProjectApplicationForm -.->|implements Form| FormType
    ApplicationFormSection -.->|implements FormSection| FormSectionType
    ProjectApplicationForm -->|sections| ApplicationFormSection
    ApplicationFormSection -->|questions| FormQuestion
    ProjectApplicationFormResponse -->|sections| ProjectApplicationResponseSection
    ProjectApplicationResponseSection -->|questions| ProjectApplicationResponseQuestion
    ProjectApplicationResponseQuestion -->|answer| ProjectApplicationAnswer
    ProjectApplicationResponseQuestion -->|options| FormOption
    ProjectApplicationAnswer -->|selectedOptions| ProjectApplicationSelectedOption
    ProjectApplicationAnswer -->|files| ProjectApplicationFile
  end

  subgraph FEEDBACK_SCHEMA["feedback.graphqls"]
    UserFeedbackTemplate["UserFeedbackTemplate"]
    UserFeedbackTemplateForm["UserFeedbackTemplateForm"]
    UserFeedbackTemplateSection["UserFeedbackTemplateSection"]
    UserFeedbackTemplateSummary["UserFeedbackTemplateSummary"]
    UserFeedbackTemplateSearchInput["UserFeedbackTemplateSearchInput"]

    UserFeedbackTemplateSummary -.->|list result| UserFeedbackTemplate
    UserFeedbackTemplateSearchInput -.->|context, targetType, active| UserFeedbackTemplate
    UserFeedbackTemplate -->|form| UserFeedbackTemplateForm
    UserFeedbackTemplateForm -.->|implements Form| FormType
    UserFeedbackTemplateSection -.->|implements FormSection| FormSectionType
    UserFeedbackTemplateForm -->|sections| UserFeedbackTemplateSection
    UserFeedbackTemplateSection -->|questions| FormQuestion
  end

  Member -->|school| School
  MemberChallenger -->|gisu| Gisu
  MemberSearchInput -.->|part| ChallengerPart
  ProjectSearchInput -.->|gisuId, parts| ChallengerPart
  ProjectPage -.->|totalElements| Long
  ProjectApplicationFormResponse -.->|status| FormResponseStatus
  ProjectApplicationResponseQuestion -.->|type| QuestionType
  FormQuestion -.->|type| QuestionType
  UserFeedbackTemplateForm -.->|status| FormStatus
```

### Project selection 호환성

기존 Project query의 선택 경로와 field는 유지한다. `project.productOwner`,
`project.coProductOwners`, `project.members.member`, `project.members.application`,
`project.applicationForm.sections.questions.options` 경로와 기존 `Project` field 선택을 이 pilot이
변경하거나 확장하지 않는다. 새 Project field는 이 문서 범위에 추가하지 않는다.

### Feedback query 및 권한

Feedback은 query-only다. `userFeedbackTemplates` 목록과 `userFeedbackTemplate` 상세는 pilot에서
`SUPER_ADMIN` 전용 관리자 조회이며, 두 query 모두 `FEEDBACK READ` 권한을 검사한다. 목록 input filter는
`context`, `targetType`, `active`다. 상세 결과는 nullable이고 대상이 없으면 data는 `null`, GraphQL error는
`FEEDBACK-0001` `NOT_FOUND` 및 HTTP `404`를 반환한다. `active: false`인 template도 FEEDBACK READ
권한을 가진 관리자에게는 계속 읽힌다.

```graphql
query {
  userFeedbackTemplates(input: { active: true }) {
    templateId
    context
    targetType
    active
    formId
    title
  }
  userFeedbackTemplate(id: 42) {
    templateId
    form {
      title
      sections {
        title
        questions { questionId type options { optionId content } }
      }
    }
  }
}
```

mutation과 응답자 access는 제공하지 않으며, local fixture가 `SUPER_ADMIN` 계정을 보장한다고 가정하지
않는다.

### Pilot migration note: 제거된 type 이름

기존 client의 fragment 또는 `__typename`이 제거된 `MemberBrief`, `MemberSummary`,
`ApplicationFormQuestion`, `ApplicationFormOption` 이름을 사용한다면 각각 `Member`, `Member`,
`FormQuestion`, `FormOption`으로 마이그레이션해야 한다. 이 note 밖에서는 제거된 이름을 schema type으로
사용하지 않는다.
