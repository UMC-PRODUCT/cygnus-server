# GraphQL Schema

현재 GraphQL pilot은 다섯 SDL 파일을 Spring GraphQL이 합쳐 하나의 unified schema로 로드한다.
파일은 선언 소유권을 나누지만 endpoint와 전역 type namespace는 공유한다.

| 파일 | 소유 계약 |
| --- | --- |
| `common.graphqls` | `Long`, `ChallengerPart` |
| `form.graphqls` | 공통 Form interface, 질문·옵션 type, form enum |
| `member.graphqls` | canonical `Member`와 member query |
| `organization.graphqls` | root `Query`, canonical `Gisu`, `Chapter`, `School` |
| `project.graphqls` | Project query와 Project concrete type |

```mermaid
flowchart TD
  Common["common.graphqls"] --> Loader["Spring GraphQL schema loader"]
  FormFile["form.graphqls"] --> Loader
  MemberFile["member.graphqls"] --> Loader
  OrganizationFile["organization.graphqls"] --> Loader
  ProjectFile["project.graphqls"] --> Loader
  Loader --> Schema["Unified Schema"]
  Schema --> Query["Query"]
  Query --> MemberQuery["me, member, members, memberSearch"]
  Query --> OrganizationQuery["gisuOrganizations, gisu, activeGisu, chapters, chapter, schools, school"]
  Query --> ProjectQuery["project, projects"]
```

## Form과 Project

`ProjectApplicationForm`과 `ApplicationFormSection`은 공통 interface를 구현한다. Project에만 필요한
`type`, `allowedParts`는 `ApplicationFormSection`에 남고 질문·옵션은 공통 object type을 재사용한다.

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
    +String description
    +Int! orderNo
    +FormQuestion[]! questions
  }
  class FormQuestion {
    +ID! questionId
    +QuestionType! type
    +String! title
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
  class ProjectApplicationForm {
    +ID! projectId
    +ID! applicationFormId
    +ApplicationFormSection[]! sections
  }
  class ApplicationFormSection {
    +FormSectionType! type
    +ChallengerPart[]! allowedParts
  }
  class ProjectApplicationFormResponse
  class ProjectApplicationResponseSection
  class ProjectApplicationResponseQuestion
  class ProjectApplicationAnswer

  Form <|.. ProjectApplicationForm
  FormSection <|.. ApplicationFormSection
  Form "1" o-- "0..*" FormSection : sections
  FormSection "1" *-- "0..*" FormQuestion : questions
  FormQuestion "1" *-- "0..*" FormOption : options
  ProjectApplicationForm "1" *-- "0..*" ApplicationFormSection : sections
  ProjectApplicationFormResponse "1" *-- "0..*" ProjectApplicationResponseSection : sections
  ProjectApplicationResponseSection "1" *-- "0..*" ProjectApplicationResponseQuestion : questions
  ProjectApplicationResponseQuestion "1" *-- "0..1" ProjectApplicationAnswer : answer
```

## Member, Organization, Project

회원 identity는 `Member` 하나를 사용하고, 조직 identity는 `Gisu`, `Chapter`, `School`을 사용한다.
Project의 owner와 member field도 member schema가 소유하는 같은 `Member` type을 참조한다.

```mermaid
classDiagram
  direction LR

  class Member {
    +ID! memberId
    +String name
    +String nickname
    +String email
    +MemberStatus status
    +School school
    +MemberChallenger[]! challengers
  }
  class MemberChallenger {
    +ID! challengerId
    +ID! gisuId
    +ChallengerPart! part
    +Gisu gisu
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
  class Project {
    +ID! id
    +Member productOwner
    +Member[]! coProductOwners
    +ProjectMember[]! members
    +ProjectApplicationForm applicationForm
  }
  class ProjectMember {
    +ID! projectMemberId
    +Member! member
    +ProjectApplication application
  }
  class ProjectApplication
  class ProjectApplicationForm

  Member "1" *-- "0..*" MemberChallenger : challengers
  Member --> School : school
  MemberChallenger --> Gisu : gisu
  Gisu "1" *-- "0..*" Chapter : chapters
  Gisu "1" *-- "0..*" School : schools
  Chapter "1" *-- "0..*" School : schools
  Project --> Member : productOwner
  Project --> Member : coProductOwners
  Project "1" *-- "0..*" ProjectMember : members
  ProjectMember --> Member : member
  ProjectMember "1" *-- "0..1" ProjectApplication : application
  Project --> ProjectApplicationForm : applicationForm
```

`ProjectApplicant`는 지원 당시 정보를 나타내는 Project-owned snapshot이므로 canonical `Member`와 별도 type이다.
그 외 진입 경로별 field 수 차이는 `Summary`나 `Detail` type을 추가하지 않고 client selection set으로 조절한다.

상세 설계 기준은 [GraphQL Schema 관계](onboarding/graphql/schema-relationships.md), 권한 적용 방식은
[GraphQL 권한 관리](onboarding/graphql/authorization.md)를 참고한다.
