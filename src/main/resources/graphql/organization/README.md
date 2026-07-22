# Organization GraphQL Contract

Gisu, Chapter, School의 표준 조회 operation과 canonical 응답을 제공한다.

## 파일

| 파일 | 내용 |
|---|---|
| `request.graphqls` | 조직 조회 root operation과 `GisuOrganizationInput` |
| `response.graphqls` | `Gisu`, `Chapter`, `School`, `SchoolLink` |

## 관계

| field | 대상 | 의미 |
|---|---|---|
| `Gisu.chapters` | `Chapter` | 해당 기수에 편성된 지부 목록 |
| `Gisu.schools` | `School` | 해당 기수에 참여한 학교 목록 |
| `Chapter.schools` | `School` | 해당 지부에 소속된 학교 목록 |
| `School.links` | `SchoolLink` | 학교가 관리하는 외부 채널 링크 |

`GisuChapter`, `GisuSchool`, `ChapterSchool`은 persistence 관계나 조회 중간 모델일 수 있지만 공개
resource가 아니다. GraphQL에서는 관계를 `Gisu`, `Chapter`, `School` field로 노출하며, 관계 자체에
추가 속성이 생길 때만 별도 edge type을 도입한다.

`Gisu.startAt/endAt`과 `School.createdAt/updatedAt`은 Java 원본과 동일한 GraphQL `Instant`이다.
adapter에서 문자열로 변환하지 않고 runtime scalar coercing에 직접 전달한다.
