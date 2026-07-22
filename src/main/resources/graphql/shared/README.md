# Shared GraphQL Contract

비즈니스 도메인이 소유하지 않는 transport scalar와 전 API가 합의한 platform contract를 제공한다.

| 파일 | 선언 | 의미 |
|---|---|---|
| `scalars.graphqls` | `Long` | GraphQL 기본 `Int` 범위를 넘는 64-bit signed integer |
| `scalars.graphqls` | `Instant` | ISO-8601 UTC 시각 값 |
| `pagination.graphqls` | `PageInput` | 0-based offset pagination 요청. 기본 20개, 최대 100개 |
| `pagination.graphqls` | `PageInfo` | page 결과가 공통으로 제공하는 metadata |

각 도메인은 `MemberPage`, `ProjectPage`처럼 content type이 구체적인 page output을 소유하고,
metadata는 `pageInfo: PageInfo!`로 제공한다. GraphQL SDL에는 generic이 없으므로 `Page<T>`는 선언하지 않는다.

`ChallengerPart`처럼 여러 도메인이 사용하더라도 의미와 lifecycle을 특정 도메인이 소유하면
`shared`로 옮기지 않는다. 해당 provider IDL을 직접 참조한다.
