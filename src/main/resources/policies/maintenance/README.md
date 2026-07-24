# Maintenance JSON Policy

`maintenance-1.0`은 점검 우회와 점검 관리 진입 권한을 동일한
`maintenance:bypass` decision으로 관리한다.

| Action | Required attribute | Target |
|---|---|---|
| `maintenance:bypass` | `relation.superAdmin: BOOLEAN` | `SUPER_ADMIN`만 ALLOW |

회원 ID는 외부 attribute로 신뢰하지 않는다. 인증된 principal의 member ID로
공용 `AuthorizationSubjectSnapshot`을 한 번 로드하고, legacy와 target이 같은
snapshot을 평가한다. 회원이 없거나 snapshot/target 평가에 실패하면 bypass는
fail closed한다.
