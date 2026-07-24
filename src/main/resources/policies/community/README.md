# Community JSON Policy

Community의 게시글·댓글 resource 권한을 `community-1.0` bundle에서 평가한다.

## 현재 action

- `community-post:read/write/update/delete`
- `community-comment:read/write/update/delete`

`update`는 작성자, `delete`는 작성자·SUPER_ADMIN·활성 중앙 회장단에게 허용한다.
ChallengerRole은 연결된 Gisu의 `[startAt, endAt)` 동안만 중앙 회장단 권한을 만든다.

`write`는 현재 evaluator가 subject가 아닌 resource 작성자의 Challenger 이력을 검사하는
동작을 그대로 기록한 transitional action이다. 실제 생성 REST는 active Challenger 조회를
선행한다. 생성 정책을 service boundary에 연결할 때 `relation.subjectActiveChallenger`로
교체하려면 별도 expected difference 검토가 필요하다.

## 현재 rollout 범위

게시글·댓글 `ResourcePermissionEvaluator`만 공용 SHADOW rollout에 연결했다. thread owner/admin/
member, message author, WebSocket send/subscribe, report 생성·관리 정책은 아직 직접 코드에 남아
있으므로 domain coverage는 `PLANNED`다.
