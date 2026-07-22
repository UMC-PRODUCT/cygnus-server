# Project GraphQL Contract

Project 조회 요청과 Project가 외부에 제공하는 표준 응답을 소유한다.

## 파일

| 파일 | 내용 |
|---|---|
| `request.graphqls` | `project`, `projects`와 검색·정렬 input. Paging은 `PageInput` 참조 |
| `response.graphqls` | Project, `pageInfo: PageInfo`, 소속 Member, 지원서, 지원 Form projection |

## 외부 도메인 관계

| field | 대상 | 방식 | 의미 |
|---|---|---|---|
| `Project.gisuId` | `Gisu.id` | ID | Project가 속한 기수 |
| `Project.chapterId` | `Chapter.id` | ID | Project가 속한 지부 |
| `Project.productOwner` | `Member` | 직접 참조 | product owner의 최신 Member 표준 응답 |
| `Project.coProductOwners` | `Member` | 직접 참조 | co-product owner의 최신 Member 표준 응답 |
| `ProjectMember.member` | `Member` | 직접 참조 | Project 소속 관계의 회원 |
| `ProjectApplicant` | Member | snapshot | 지원 당시 이름·학교·파트를 보존하는 Project 데이터 |
| `ProjectApplicationForm` | Form | projection | Project part 정책을 결합한 지원 폼 |
| `ProjectApplicationFormOption.nextSectionId` | `FormSection.sectionId` | ID | 원본 Form의 조건부 이동 대상 |

## Form 변환 경계

`ProjectApplicationForm`은 Form 표준 응답을 그대로 전달하지 않는다. Project가 section의 `type`,
`allowedParts`를 소유하고 지원 응답 snapshot도 함께 제공하므로 Project 전용 type을 사용한다.

Java 변환은
`project/adapter/in/graphql/converter/ProjectApplicationFormGraphQlConverter.java`가 담당한다.
GraphQL DTO에 application `Info` 변환 책임을 넣지 않는다.
