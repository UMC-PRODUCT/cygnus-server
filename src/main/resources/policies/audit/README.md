# Audit JSON Policy

`audit-1.0`은 감사 로그 조회 권한을 관리한다.

| Action | Required attribute | Target |
|---|---|---|
| `audit-log:list` | `relation.activeCentralMember: BOOLEAN` | active 중앙운영사무국 또는 `SUPER_ADMIN` |

`active`는 서버가 선택한 단일 `evaluatedAt`에 대해
`gisu.startAt <= evaluatedAt < gisu.endAt`으로 계산한다. 만료된 중앙 역할을
허용하던 legacy 결과는 `expected-differences.json`의 정확한 predicate로만
`EXPECTED_DIFFERENCE` 분류하며 target decision 자체를 바꾸는 allowlist로 쓰지 않는다.
