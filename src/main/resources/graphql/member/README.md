# Member GraphQL Contract

Member 도메인의 표준 조회 operation과 canonical `MemberPublic`, `MemberPrivate` 응답을 제공한다.

## 파일

| 파일 | 내용 |
|---|---|
| `request.graphqls` | `me`, `member`, `members`, `memberSearch`와 검색 input. Paging은 `PageInput` 참조 |
| `response.graphqls` | `MemberPublic`, 본인 전용 `MemberPrivate`, 검색 edge와 Challenger projection |

## 관계

| field | 대상 | 방식 | 의미 |
|---|---|---|---|
| `MemberPublic.schoolId` | `School.id` | ID | Member가 현재 참조하는 Organization School |
| `MemberPublic.school` | `School` | 직접 참조 | `schoolId`로 조회한 Organization 표준 응답 |
| `MemberPublic.schoolName` | 없음 | snapshot | Member 저장 시점의 학교명 문자열 |
| `MemberPublic.challengers` | Challenger | projection | Member 조회에 필요한 기수별 활동 이력 |
| `MemberPublic.private` | `MemberPrivate` | 권한 그룹 | 본인에게만 제공하는 email과 status |
| `MemberChallenger.gisu` | `Gisu` | 직접 참조 | `gisuId`로 조회한 Organization 표준 응답 |
| `MemberSearchEdge.member` | `MemberPublic` | 직접 참조 | 검색 결과의 표준 회원 resource |
| `MemberSearchEdge.maskedEmail` | 없음 | 검색 projection | 운영자 검색 문맥에만 제공하는 마스킹 이메일 |
| `MemberSearchEdge.currentChallenger` | `MemberChallenger` | 검색 projection | 검색 시점의 현재 활동 |

Member의 부분집합이 필요해도 `MemberSummary`나 `MemberBrief`를 추가하지 않는다. 클라이언트가 GraphQL
selection set으로 필요한 `MemberPublic` field만 요청한다. Challenger 이력처럼 Member 조회 문맥에서 형태와
의미를 바꾼 데이터만 Member 소유 projection으로 선언한다.
