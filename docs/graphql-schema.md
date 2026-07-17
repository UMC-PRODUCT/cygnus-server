# GraphQL Schema

현재 GraphQL pilot은 여섯 SDL 파일을 Spring GraphQL이 합쳐 하나의 unified schema로 로드한다.
파일은 type 소유권을 나누지만 endpoint와 전역 type namespace는 공유한다.

| 파일 | 소유 계약 |
| --- | --- |
| `common.graphqls` | `Long`, `ChallengerPart` |
| `form.graphqls` | `Form`, `FormSection`, `FormQuestion`, `FormOption`, `FormStatus`, `FormResponseStatus`, `QuestionType` |
| `member.graphqls` | `MemberSummary`를 포함한 member type과 member query |
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
  MemberFile["member.graphqls<br/>MemberSummary"] --> Loader
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

## Type 관계

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
    MemberSummary["MemberSummary"]
    MemberSearchInput["MemberSearchInput"]
    Member -->|challengers| MemberChallenger
  end

  subgraph ORGANIZATION_SCHEMA["organization.graphqls"]
    Gisu["Gisu"]
    GisuOrganizationInput["GisuOrganizationInput"]
    GisuOrganizationPayload["GisuOrganizationPayload"]
    GisuChapter["GisuChapter"]
    GisuSchool["GisuSchool"]
    Chapter["Chapter"]
    ChapterSchool["ChapterSchool"]
    SchoolName["SchoolName"]
    SchoolDetail["SchoolDetail"]
    SchoolLink["SchoolLink"]
    GisuOrganizationPayload -->|gisus| Gisu
    Gisu -->|chapters| GisuChapter
    Gisu -->|schools| GisuSchool
    GisuChapter -->|schools| ChapterSchool
    GisuSchool -->|links| SchoolLink
    SchoolDetail -->|links| SchoolLink
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
    Project -->|productOwner, coProductOwners| MemberSummary
    Project -->|partQuotas| ProjectPartQuota
    Project -->|members| ProjectMember
    Project -->|applicationForm| ProjectApplicationForm
    ProjectMember -->|member| MemberSummary
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

  Member -->|school| SchoolDetail
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

기존 client의 fragment 또는 `__typename`이 제거된 `MemberBrief`, `ApplicationFormQuestion`,
`ApplicationFormOption` 이름을 사용한다면 각각 `MemberSummary`, `FormQuestion`, `FormOption`으로
마이그레이션해야 한다. 이 note 밖에서는 제거된 이름을 schema type으로 사용하지 않는다.
