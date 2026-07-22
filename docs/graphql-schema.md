# GraphQL Schema Snapshot

GraphQL pilot은 `src/main/resources/graphql` 아래의 도메인 IDL을 하나의 runtime schema로 조립한다.
파일 구조와 소유권 규칙은 [GraphQL IDL README](../src/main/resources/graphql/README.md), 설계 기준은
[GraphQL Schema 관계](onboarding/graphql/schema-relationships.md)를 참고한다.

## Root

```mermaid
flowchart TD
  Schema["Unified GraphQL Schema"] --> Query
  Schema --> Mutation
  Query --> Member["me / member / members / memberSearch"]
  Query --> Organization["gisu / chapter / school"]
  Query --> Project["project / projects"]
  Query --> Recruiting["5 Recruiting resource queries"]
  Mutation --> RecruitingMutation["25 Recruiting lifecycle commands"]
```

## Domain 관계

```mermaid
classDiagram
  direction LR

  class MemberPublic
  class MemberPrivate
  class MemberSearchEdge
  class Gisu
  class Chapter
  class School
  class Form
  class Project
  class ProjectApplicationForm {
    <<Project projection>>
  }
  class RecruitingApplicationFormStructure {
    <<Recruiting projection>>
  }
  class ProjectApplicant {
    <<snapshot>>
  }
  class RecruitingSeason
  class RecruitingSeasonManagement {
    <<authorized group>>
  }
  class RecruitingRound
  class RecruitingRoundManagement {
    <<authorized group>>
  }
  class RecruitingApplication
  class RecruitingApplicationPrivate {
    <<applicant group>>
  }
  class RecruitingApplicationReview {
    <<reviewer group>>
  }

  MemberPublic --> MemberPrivate : private
  MemberSearchEdge --> MemberPublic : member
  MemberPublic --> School : school
  MemberPublic --> Gisu : challengers.gisu
  Gisu --> Chapter : chapters
  Gisu --> School : schools
  Chapter --> School : schools
  Project --> MemberPublic : owners / members
  Project --> ProjectApplicant : application snapshot
  Form ..> ProjectApplicationForm : converter
  Form ..> RecruitingApplicationFormStructure : filter + converter
  RecruitingSeason --> RecruitingSeasonManagement : management
  RecruitingSeason --> Gisu : gisu
  RecruitingSeason --> School : school
  RecruitingSeason --> RecruitingRound : rounds
  RecruitingRound --> RecruitingRoundManagement : management
  RecruitingApplication --> RecruitingRound : round
  RecruitingApplication --> RecruitingApplicationPrivate : private
  RecruitingApplication --> RecruitingApplicationReview : review
  RecruitingRoundManagement --> MemberPublic : evaluators
  RecruitingApplicationReview --> MemberPublic : applicant
```

Provider resource를 그대로 노출할 때는 canonical type을 직접 참조한다. 소비 문맥에서 구조를
필터링하거나 정책을 결합하면 소비 도메인이 projection과 converter를 소유한다.
