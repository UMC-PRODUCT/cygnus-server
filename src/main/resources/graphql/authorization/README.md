# Authorization GraphQL IDL

`ChallengerRole`은 특정 Gisu에서 Challenger가 맡은 운영 역할이다. `organizationType`과
`organizationId` 조합은 역할의 scope를 나타내며, `gisuId`는 Organization의 `Gisu`를 참조한다.

`ResourcePermission`은 resource의 상태가 아니라 현재 요청자와 resource 사이의 계산된 권한
projection이다. 클라이언트는 enum과 boolean grant만 사용하고 내부 evaluator 구현을 알지 않는다.
