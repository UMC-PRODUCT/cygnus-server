# Recruiting GraphQL Contract

Recruiting의 공개·운영 조회, command 요청, 지원서 및 전형 응답 계약을 제공한다.

## 파일

| 파일 | 내용 |
|---|---|
| `request.graphqls` | Recruiting `Query`, `Mutation`, 모든 input. Paging은 `PageInput` 참조 |
| `response.graphqls` | season, round, application, evaluation, `PageInfo`, Form projection과 enum |

## 주요 관계

| field | 대상 | 방식 | 의미 |
|---|---|---|---|
| `RecruitingSeasonConfiguration.gisuId` | `Gisu.id` | ID | 모집 season이 속한 기수 |
| `RecruitingSeasonConfiguration.schoolId` | `School.id` | ID | 모집을 운영하는 학교 |
| `RecruitingSeasonSummary.chapterName` | Organization | snapshot | 검색 시점의 지부명 |
| `RecruitingSeasonSummary.schoolName` | Organization | snapshot | 검색 시점의 학교명 |
| `RecruitingRoundEvaluator.evaluatorMemberId` | `Member.memberId` | ID | round 평가자 |
| `RecruitingApplicationEvaluation.evaluatorMemberId` | `Member.memberId` | ID | 평가 제출자 |
| `RecruitingApplicationReview.applicantMemberId` | `Member.memberId` | ID | 회원 지원자인 경우의 Member |
| `RecruitingApplicationForm.formId` | `Form.formId` | ID | 지원 질문 구조를 소유하는 Form |
| `RecruitingApplicationFormStructure` | Form | projection | 지원자의 트랙에 따라 section을 필터링한 구조 |
| `RecruitingApplicationReviewDetail.formResponseId` | Form response | ID | Form 도메인이 소유하는 원본 응답 |
| `availabilityFormId` | `Form.formId` | ID | 면접 가능 시간 수집용 Form |

## Form 변환 경계

Recruiting은 공통 section과 1·2지망 트랙 section만 선택하여 반환한다. 따라서 Form 타입을 직접
노출하지 않고 `RecruitingApplicationFormStructure`, `RecruitingFormSection`,
`RecruitingFormQuestion`, `RecruitingFormQuestionOption`을 소유한다.

Java 변환은
`recruiting/adapter/in/graphql/converter/RecruitingApplicationFormStructureGraphQlConverter.java`가
담당한다. 필터링 정책은 use case가 결정하고 converter는 반환 구조만 변환한다.

## Operation 범위

공개 조회는 모집 round와 application key 기반 지원서 조회를 제공한다. 인증 조회는 본인 지원서와
면접 일정을 제공한다. 운영 조회·mutation은 season, round, 평가자, 질문, 심사, 등록 상태를 다루며
권한 정책은 resolver/controller의 authorization 경계에서 별도로 판정한다.
