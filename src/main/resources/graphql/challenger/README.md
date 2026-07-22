# Challenger GraphQL Contract

Challenger 활동 분류와 lifecycle enum의 표준 output 계약을 제공한다. 현재 독립 root operation은 없다.

| 선언 | 나타내는 값 | 주요 소비 도메인 |
|---|---|---|
| `ChallengerPart` | 프로젝트 활동 직군 | Member, Project |
| `ChallengerTrack` | 모집 및 활동의 통합 트랙 | Member, Recruiting |
| `ChallengerStatus` | 기수별 Challenger 활동 상태 | Member |

소비 도메인은 enum 값을 복제하지 않고 `output.graphqls`의 선언을 참조한다. 소비 문맥에서 값의
의미 자체가 달라질 때만 소비 도메인 enum을 별도로 정의한다.
