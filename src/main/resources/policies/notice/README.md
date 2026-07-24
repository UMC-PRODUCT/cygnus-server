# Notice JSON Policy

Notice의 생성·단건 조회·수정·삭제·수신 현황 권한을 `notice-1.0` bundle로 평가한다.

## Relation

- `relation.isNoticeAuthor`
- `relation.isTargetChallenger`
- `relation.activeCentralCore`
- `relation.activeRoleCanReadTarget`
- `relation.activeManagerForTarget`
- `relation.activeCreatorForTarget`

`activeRoleCanReadTarget`과 creator/manager relation은 role의 `roleType`, `gisuId`,
`organizationType`, `organizationId`, `responsiblePart` tuple을 Notice target과 함께 비교한다.
서로 다른 role의 Gisu·학교·파트를 섞지 않으며 모든 role은 Gisu `[startAt, endAt)` 경계를
통과해야 한다.

## 정책과 invariant의 경계

`NoticeTargetPattern.from`의 불가능한 target 조합 검증은 domain invariant로 남는다. 유효한
target에 대한 작성 권한만 JSON policy가 결정한다.

목록 visibility, viewer tab 조립, 인증 없이 남아 있는 reminder surface를 아직 전환해야 하므로
domain coverage는 `PLANNED`다.
