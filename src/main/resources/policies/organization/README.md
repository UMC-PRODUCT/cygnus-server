# Organization policy

Gisu·Chapter·School 구조 관리와 StudyGroup 단건·변경 권한을
`organization-1.0` JSON policy로 관리한다. 이 문서는 첫 evaluator slice이며,
StudyGroup 목록 scope와 UMC PRODUCT leadership/본인 관계는 같은 namespace의 후속
module로 확장한 뒤 도메인 전체를 SHADOW로 전환한다.

## Action matrix

| Action family | Required relation |
|---|---|
| `gisu:create/update/delete` | active 중앙 CORE |
| `chapter:create/delete` | active 중앙 CORE |
| `school:create/update/delete` | active 중앙 CORE |
| `study-group:read` | active 학교 운영진, subject school 일치 |
| `study-group:create/update/delete` | active 학교 회장단, subject school 일치 |

`SUPER_ADMIN`은 전역 override다. ChallengerRole은 연결된 Gisu의
`[startAt, endAt)`에서만 활성이다. StudyGroup resource 자체의 Gisu·학교를
검사하지 않는 현행 evaluator 범위는 이번 slice에서 그대로 보존한다.

## Attribute 연결

`relation.activeCentralCore`, `relation.activeSchoolAdminForSubjectSchool`,
`relation.activeSchoolCoreForSubjectSchool`은 공용 subject snapshot의 role tuple에서
Gisu 기간, organization type, organization ID를 한 tuple로 비교해 계산한다.
요청 body/path가 이 fact를 직접 제공할 수 없다.

## Rollout

만료 역할로 인한 legacy ALLOW/target DENY만 expected difference다. 아직
StudyGroup scope와 UMC PRODUCT 직접 정책이 남아 있어 `domain-coverage.json`의
Organization 상태는 후속 slice 완료 전까지 `PLANNED`를 유지한다.
