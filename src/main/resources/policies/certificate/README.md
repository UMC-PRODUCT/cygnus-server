# Certificate policy

Certificate 운영진 발급과 폐기 권한을 `certificate-1.0` typed context와 JSON policy로 관리한다.
셀프 발급의 수료·수상 자격, template, 중복/재발급, PDF 생성과 상태 전이는 domain rule에 남는다.

## Action

| Action | Surface | Target |
|---|---|---|
| `certificate:issue-admin` | `POST /api/v1/certificates/admin` | `SUPER_ADMIN` 또는 target Gisu의 활성 중앙 총괄단 |
| `certificate:revoke` | `PATCH /api/v1/certificates/admin/{certificateId}/revoke` | `SUPER_ADMIN` 또는 certificate Gisu의 활성 중앙 총괄단 |

두 action은 같은 ALLOW statement를 공유하지만 action ID, rollout mode, metric, enforcement receipt는
서로 독립적이다.

## Attribute

| Attribute | Type | 계산 |
|---|---|---|
| `relation.superAdmin` | `BOOLEAN` | 공용 subject snapshot의 member system role |
| `relation.activeCentralCoreInTargetGisu` | `BOOLEAN` | 하나의 role tuple에서 중앙 총괄단, `role.gisuId == targetGisuId`, `gisu.startAt <= evaluatedAt < gisu.endAt`을 모두 확인 |

Target Gisu ID는 admin issue request 또는 서버가 조회한 Certificate에서 얻는다. 외부 요청이 role,
Gisu 기간이나 system role을 제공할 수 없다. `SUPER_ADMIN`은 전역 member system role이므로 Gisu
기간과 무관하다.

## Rollout

SHADOW에서는 공용 snapshot의 모든 role로 현행 `isCentralCoreInGisu` 의미를 재현하고, 같은
snapshot에서 기간을 적용한 target JSON과 비교한다. 만료된 target Gisu 중앙 총괄단의
ALLOW→DENY만 `expected-differences.json`으로 분류한다. 실제 발급·폐기 command는 한 번만
실행한다.

Generated artifact의 policyVersion/fingerprint 검토와 enforcement receipt 없이는 ENFORCE로
전환할 수 없다.
