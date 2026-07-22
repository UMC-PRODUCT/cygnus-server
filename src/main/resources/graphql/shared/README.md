# Shared GraphQL Contract

비즈니스 도메인이 소유하지 않는 transport scalar만 제공한다.

| 파일 | 선언 | 의미 |
|---|---|---|
| `scalars.graphqls` | `Long` | GraphQL 기본 `Int` 범위를 넘는 64-bit signed integer |
| `scalars.graphqls` | `Instant` | ISO-8601 UTC 시각 값 |

`ChallengerPart`처럼 여러 도메인이 사용하더라도 의미와 lifecycle을 특정 도메인이 소유하면
`shared`로 옮기지 않는다. 해당 provider IDL을 직접 참조한다.
