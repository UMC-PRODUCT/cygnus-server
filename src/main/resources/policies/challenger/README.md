# Challenger policy

Challenger lifecycle, 상벌점, 활동 기록의 actor 권한을 `challenger-1.0`
JSON policy로 관리한다. 대상 Challenger/Point 조회와 상태 전이, 점수·사유 검증,
활동 기간 invariant는 domain code에 남는다.

## Action matrix

| Action | Target relation |
|---|---|
| `challenger:create`, `challenger-record:read` | active 학교 회장단 또는 SUPER_ADMIN |
| `challenger:update/delete` | active 중앙 CORE 또는 SUPER_ADMIN |
| `challenger-point:create/update` | target Gisu 중앙 운영진 또는 같은 target 학교 회장단 |
| `challenger-point:delete` | active 중앙 CORE 또는 SUPER_ADMIN |
| `challenger-record:create/delete` | active 중앙 CORE 또는 SUPER_ADMIN |

Point create/update는 `roleType`, `gisuId`, `organizationType`,
`organizationId`를 동일 role tuple에서 비교한다. 대상 Gisu와 학교는 서버가
Challenger/Member를 조회해 만든 resource fact이며 요청이 임의로 제공하지 않는다.

## Rollout

SHADOW에서는 기존 evaluator가 authoritative다. 만료된 역할의 legacy ALLOW와
target DENY만 expected difference다. `ChallengerSearchService`의 목록 scope를 후속
slice에서 policy outcome으로 옮기기 전까지 도메인 coverage는 `PLANNED`를 유지한다.
