# Recruiting GraphQL Contract

Recruiting의 공개·운영 조회, command 요청, 지원서 및 전형 응답 계약을 제공한다.

## 파일

| 파일 | 내용 |
|---|---|
| `request.graphqls` | Recruiting `Query`, `Mutation`, 모든 input. Paging은 `PageInput` 참조 |
| `response.graphqls` | canonical Season/Round/Application graph, 권한 그룹, Form projection과 enum |

## 주요 관계

| field | 대상 | 방식 | 의미 |
|---|---|---|---|
| `RecruitingSeason.gisu` | `Gisu` | 직접 참조 | 모집 season이 속한 기수 |
| `RecruitingSeason.school` | `School` | 직접 참조 | 모집을 운영하는 학교 |
| `RecruitingSeason.management` | Recruiting | 권한 그룹 | 운영 메모, quota, round, 상태 집계 |
| `RecruitingRound.season` | `RecruitingSeason` | 직접 참조 | round가 속한 season |
| `RecruitingRound.management.evaluators` | `MemberPublic` | 직접 참조 | round 평가자 목록 |
| `RecruitingApplication.round` | `RecruitingRound` | 직접 참조 | 지원 대상 round |
| `RecruitingApplication.private` | Recruiting | 권한 그룹 | 지원자 본인 또는 credential 전용 데이터 |
| `RecruitingApplication.review` | Recruiting | 권한 그룹 | 운영자 또는 평가자 전용 심사 데이터 |
| `RecruitingApplicationReview.applicant` | `MemberPublic` | 직접 참조 | 회원 지원자인 경우의 공개 Member |
| `RecruitingApplicationEvaluation.evaluator` | `MemberPublic` | 직접 참조 | 평가 제출자 |
| `RecruitingApplicationForm.formId` | `Form.formId` | ID | 지원 질문 구조를 소유하는 Form |
| `RecruitingApplicationFormStructure` | Form | projection | 지원자의 트랙에 따라 section을 필터링한 구조 |
| `RecruitingApplicationReview.formResponseId` | Form response | ID | Form 도메인이 소유하는 원본 응답 |
| `availabilityFormId` | `Form.formId` | ID | 면접 가능 시간 수집용 Form |

## Form 변환 경계

Recruiting은 공통 section과 1·2지망 트랙 section만 선택하여 반환한다. 따라서 Form 타입을 직접
노출하지 않고 `RecruitingApplicationFormStructure`, `RecruitingFormSection`,
`RecruitingFormQuestion`, `RecruitingFormQuestionOption`을 소유한다.

Java 변환은
`recruiting/adapter/in/graphql/converter/RecruitingApplicationFormStructureGraphQlConverter.java`가
담당한다. 필터링 정책은 use case가 결정하고 converter는 반환 구조만 변환한다.

## Operation 범위

Query root는 `recruitingSeasons`, `recruitingSeason`, `recruitingRounds`, `recruitingRound`,
`recruitingApplication` 다섯 resource 진입점만 제공한다. 지원서 접근은 `applicationId` 또는
`credential` 중 하나를 요구한다. 운영·지원자·평가자 전용 데이터는 각각 nullable `management`,
`private`, `review` field에서 판정하므로 권한이 없어도 base resource와 sibling field는 유지된다.

Mutation은 lifecycle command를 유지하되 evaluator와 면접 질문은 full-replace diff로 처리한다. 생성과
상태 변경은 canonical resource를 반환하고 삭제만 `RecruitingDeletedPayload`를 반환한다. 목록 nested
field는 batch query use case를 사용한다.
