# Form GraphQL Contract

Form aggregate의 표준 구조를 `output.graphqls`로 제공한다. 현재 Form 자체 root operation은 없고
Project와 Recruiting이 use case를 통해 읽은 뒤 각자의 공개 계약으로 제공한다.

## 구조

| type | 관계 | 의미 |
|---|---|---|
| `Form` | `sections -> FormSection` | form metadata와 순서가 보장된 section 목록 |
| `FormSection` | `questions -> FormQuestion` | form 안의 화면·논리 구획 |
| `FormQuestion` | `options -> FormOption` | 입력 유형과 검증 규칙을 가진 질문 |
| `FormOption` | `nextSectionId -> FormSection.sectionId` | 선택형 질문의 조건부 section 이동 |

## 외부 관계

| field | 대상 | 처리 방식 |
|---|---|---|
| `Form.createdMemberId` | `Member.memberId` | 생성자 식별자만 제공하며 Member 객체는 자동 조립하지 않음 |

`ProjectApplicationForm`과 `RecruitingApplicationFormStructure`는 `Form`의 별칭이 아니다. 두 도메인은
section을 정책에 따라 보강하거나 필터링하므로 자체 output과 converter를 소유한다.
