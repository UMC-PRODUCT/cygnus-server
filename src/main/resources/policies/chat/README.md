# Chat policy

채팅방 membership, 메시지 작성자, moderator 관계를 `chat-1.0` JSON policy로
관리한다. 메시지 payload·mention·attachment 검증, reaction emoji 유효성, tombstone
상태 전이와 room/message 존재 여부는 domain invariant로 코드에 남는다.

## Action matrix

| Action | Room member | Message author | Moderator |
|---|---:|---:|---:|
| `chat-room:read` | ALLOW | - | - |
| `chat-room-summary:read` | ALLOW | - | - |
| `chat-message:create` | ALLOW | - | - |
| `chat-message:read` | ALLOW | - | - |
| `chat-message:update` | required | required | - |
| `chat-message:delete` | required | author 또는 moderator | author 또는 moderator |
| `chat-reaction:update` | ALLOW | - | - |
| `chat-read:update` | ALLOW | - | - |
| `chat-read-status:read` | ALLOW | - | - |

모든 action은 room membership을 전제로 한다. 수정은 작성자만 가능하고, 삭제는
작성자 또는 호출 context에서 신뢰된 moderator만 가능하다는 현행 규칙을 보존한다.

## Attribute 연결

`relation.isRoomMember`는 서버가 `chat_member`에서 조회하거나, 이미 membership으로
필터링한 room 집합에서 계산한다. `relation.isMessageAuthor`는 저장된 메시지의
`senderMemberId`와 호출 member ID를 비교해 계산한다. `relation.isModerator`는 Chat을
호출하는 소유 도메인이 검증해 전달한 command fact다. 외부 요청이 이 attribute
map을 직접 제공할 수 없다.

세 attribute는 모든 action에서 required다. 해당 action과 무관한 fact도 `false`로
명시해 missing/null 평가 차이를 없앤다.

## Rollout

SHADOW에서는 기존 membership/author/moderator 판정이 authoritative이고 target JSON은
같은 fact snapshot으로만 비교한다. command나 DB 조회를 두 번 실행하지 않는다.
승인된 차이는 없으며 artifact·fingerprint 검토와 관측 및 enforcement receipt 없이는
ENFORCE로 전환할 수 없다.
