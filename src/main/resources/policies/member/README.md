# Member policy

Member 단건 조회와 관리자 강제 삭제 권한을 `member-1.0` JSON policy로 관리한다.
프로필 visibility, 본인 credential 변경, 검색 scope와 GraphQL field masking은 별도
surface이며 후속 module에서 같은 namespace로 통합한다.

## Action matrix

| Action | Rule |
|---|---|
| `member:read` | Challenger 이력이 있는 인증 회원 |
| `member:delete` | active 중앙 CORE 또는 SUPER_ADMIN |

`relation.challenger`는 서버가 조회한 Challenger 이력에서 계산하고,
`relation.activeCentralCore`는 공용 role tuple의 Gisu `[startAt, endAt)`을 적용한다.
만료된 중앙 CORE의 삭제 권한 제거만 expected difference다.

도메인 검색 scope와 summary/GraphQL 직접 role 판정이 남아 있으므로 coverage는
후속 slice 완료 전까지 `PLANNED`를 유지한다.
