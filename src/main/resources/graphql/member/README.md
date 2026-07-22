# Member GraphQL Contract

Member 도메인의 표준 조회 operation과 canonical `Member` 응답을 제공한다.

## 파일

| 파일 | 내용 |
|---|---|
| `request.graphqls` | `me`, `member`, `members`, `memberSearch`와 검색 input |
| `response.graphqls` | `Member`, 검색 page, Challenger projection |

## 관계

| field | 대상 | 방식 | 의미 |
|---|---|---|---|
| `Member.schoolId` | `School.id` | ID | Member가 현재 참조하는 Organization School |
| `Member.school` | `School` | 직접 참조 | `schoolId`로 조회한 Organization 표준 응답 |
| `Member.schoolName` | 없음 | snapshot | Member 저장 시점의 학교명 문자열 |
| `Member.challengers` | Challenger | projection | Member 조회에 필요한 기수별 활동 이력 |
| `MemberChallenger.gisu` | `Gisu` | 직접 참조 | `gisuId`로 조회한 Organization 표준 응답 |
| `MemberSearchResult.school` | `School` | 직접 참조 | 검색 결과의 학교 상세 |
| `MemberSearchChallenger.gisu` | `Gisu` | 직접 참조 | 검색 결과의 활동 기수 상세 |

Member의 부분집합이 필요해도 `MemberSummary`나 `MemberBrief`를 추가하지 않는다. 클라이언트가 GraphQL
selection set으로 필요한 `Member` field만 요청한다. Challenger 이력처럼 Member 조회 문맥에서 형태와
의미를 바꾼 데이터만 Member 소유 projection으로 선언한다.
