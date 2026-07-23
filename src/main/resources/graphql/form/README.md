# Form GraphQL Contract

Form aggregate의 표준 구조를 provider contract로 제공한다.

- `form(id)`은 section, question, option을 포함한 표준 Form 구조를 반환한다.
- draft는 생성자에게만 반환하고, 게시된 Form은 인증 회원이 조회할 수 있다.
- Form 응답의 접근 권한과 lifecycle은 Feedback, Recruiting, Project 등 consumer가 자기 projection에서 제공한다.
- consumer 전용 상태나 식별자는 Form 타입에 추가하지 않고 해당 도메인의 field에 둔다.

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
