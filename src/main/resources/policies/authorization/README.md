# Authorization policy

ChallengerRole 조회·생성·삭제 권한을 `authorization-1.0` JSON policy로 관리한다.
역할 데이터의 유효성, 조직 타입과 role 조합, 생성·삭제 transaction은 domain rule에
남고 “누가 action을 실행할 수 있는가”만 정책에서 결정한다.

## Action matrix

| Action | 인증 회원 | Active 중앙 CORE | SUPER_ADMIN |
|---|---:|---:|---:|
| `challenger-role:read` | ALLOW | ALLOW | ALLOW |
| `challenger-role:create` | DENY | ALLOW | ALLOW |
| `challenger-role:delete` | DENY | ALLOW | ALLOW |

`SUPER_ADMIN`은 member system role이므로 Gisu 기간과 무관한 전역 override다.
ChallengerRole 기반 중앙 CORE는 연결된 Gisu의 `[startAt, endAt)`에서만 활성이다.

## Attribute 연결

`subject.authenticated`는 서버가 만든 MEMBER subject 여부다.
`relation.activeCentralCore`는 공용 subject snapshot의 role tuple을
`roleType`과 Gisu 기간을 함께 확인해 계산한다. role type과 Gisu를 독립 set으로
펼치지 않으며 외부 요청이 attribute를 제공할 수 없다.

## Rollout

SHADOW에서는 기존 `isCentralCoreInAnyGisu()`가 authoritative다. 만료된 중앙 CORE가
legacy ALLOW, target DENY가 되는 경우만 expected difference로 분류한다. 검토 artifact,
24시간 관측, enforcement receipt 없이는 ENFORCE로 전환할 수 없다.
