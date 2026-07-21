# Community Thread 클라이언트 계약

상태: PR3 구현 계약

이 문서는 Issue #1127의 invitation-only Community thread에 대한 client-facing 계약이다. 알람
delivery는 이 범위에 포함하지 않으며, REST는 control/query/recovery/moderation, WebSocket은
interactive message/reaction/read mutation을 담당한다.

브라우저용 AsyncAPI 문서는 `/docs/asyncapi`, machine-readable 원본은 `/docs/asyncapi.yaml`, 실제
frame을 전송하는 테스트 콘솔은 `/docs/community-thread.html`에서 제공한다.

## 1. 식별자와 공통 규칙

- 외부 API는 `threadId`, `messageId`, `memberId`만 사용한다. Chat engine의 `chatRoomId`/`roomId`는
  외부 요청·응답·event payload에 노출하지 않는다.
- REST numeric path ID는 양의 십진수 전체 일치만 허용한다. `0`, 음수, overflow, query fragment,
  suffix, 중복 slash와 alternate encoding은 거부한다.
- REST response의 ID/count/offset/total은 문자열이며 boolean과 null은 JSON 원시 타입을 유지한다.
- STOMP SEND는 `x-command-id` native header에 canonical lowercase UUID가 있어야 한다.
- domain state event는 stable `eventId`, `type`, string `threadId`, `occurredAt`, typed payload를
  가진다. 동일 event가 retry로 중복될 수 있으므로 client는 `eventId`로 deduplicate한다.

구현 기준 심볼은 `CommunityThreadStompController`,
`CommunityStompDestinationParser`, `CommunityStompSendAuthorizer`,
`CommunityStompSubscriptionAuthorizer`, `CommunityStompCommandSupport`,
`CommunityStompAckPublisher`, `CommunityStompErrorMapper`다. 외부 broker fan-out은
`CommunityThreadRealtimeEventListener` → `CommunityThreadRealtimeFanOutService` →
`CommunityThreadRealtimeDelivery` → `CommunityThreadRealtimeBroadcastAdapter` 경로를 따른다.

## 2. REST 책임과 경로

Base URI: `/api/v1/community`

| Method | Path | 책임 |
| --- | --- | --- |
| GET, POST | `/threads` | thread list/create |
| GET, PATCH, DELETE | `/threads/{threadId}` | detail/update/soft-delete |
| POST, DELETE | `/threads/{threadId}/pin` | pin/unpin |
| POST, DELETE | `/threads/{threadId}/mute` | mute/unmute |
| GET | `/threads/{threadId}/members` | member query |
| GET | `/threads/{threadId}/invitable` | Challenger 이력과 무관한 ACTIVE 회원 검색 |
| POST | `/threads/{threadId}/invite` | invite/LEFT re-entry |
| DELETE | `/threads/{threadId}/members/{memberId}` | kick |
| POST | `/threads/{threadId}/leave` | leave |
| PATCH | `/threads/{threadId}/members/{memberId}/role` | role/ownership change |
| GET | `/threads/{threadId}/messages` | initial/reconnect/history cursor |
| POST | `/messages/{messageId}/report` | thread message report |
| GET | `/admin/thread-message-reports` | super-admin moderation inbox |

Admin surface의 완전한 경로는 `/api/v1/community/admin/thread-message-reports`다. top-level
`/api/v1/admin/...` path는 제공하지 않는다.

다음 REST mutation은 존재하지 않는다.

- message create/edit/delete
- reaction add/remove
- read watermark update

메시지 history는 newest-first, exclusive `before` cursor, 기본 limit 30, 최대 100이다. broker가
끊기거나 event가 누락되면 이 endpoint와 thread detail/member query로 상태를 backfill한다.

## 3. WebSocket 연결

SockJS/STOMP endpoint는 `/ws`, native WebSocket transport endpoint는 `/ws/websocket`이다. 두
endpoint 모두 application protocol로 STOMP 1.2를 사용한다. CONNECT에 다음 header를 전송한다.

```text
Authorization: Bearer <access-token>
```

`/topic`, `/queue`, `/user`로 client가 직접 SEND하는 frame은 차단된다. protocol attack, JWT CONNECT
실패, broker 직접 SEND와 malformed SUBSCRIBE는 terminal STOMP ERROR다.

## 4. SEND command

모든 command는 `x-command-id`를 요구한다. 아래 six destination만 허용된다.

| Command | SEND destination | Body |
| --- | --- | --- |
| create | `/app/community/threads/{threadId}/messages` | `clientMessageId`, `type`, `content`, `fileMetadataIds`, `mentionedMemberIds`, `replyToId` |
| edit | `/app/community/threads/{threadId}/messages/{messageId}/edit` | `content` |
| tombstone | `/app/community/threads/{threadId}/messages/{messageId}/delete` | empty `{}` |
| reaction add | `/app/community/threads/{threadId}/messages/{messageId}/reactions/add` | `emoji` |
| reaction remove | `/app/community/threads/{threadId}/messages/{messageId}/reactions/remove` | `emoji` |
| read | `/app/community/threads/{threadId}/read` | `lastReadMessageId` |

create의 `clientMessageId`는 canonical UUID이며 thread/sender와 함께 저장 idempotency key를 이룬다.
같은 key와 같은 canonical payload는 기존 결과를 반환하고 `deduplicated=true` ACK를 만들며, payload가
다르면 409 conflict다. `x-command-id` 재사용 자체는 command idempotency가 아니다.

### Payload validation

- TEXT는 non-blank content, 최대 2,000 code point, file 없음이다.
- IMAGE는 optional caption(최대 2,000 code point)과 1–4개의 owned image file을 요구한다. 한 파일은
  최대 10 MiB, 합계는 최대 40 MiB다.
- mention은 unique ACTIVE same-thread member만 허용하며 최대 100개다. reply는 같은 thread의
  positive message ID여야 한다.
- edit는 content만 바꿀 수 있고 type/files/reply/mentions/clientMessageId는 immutable이다.
- tombstone body는 unknown field가 없는 빈 JSON object여야 한다. 원본 ID/sender/reply/timestamp는
  보존하고 file/mention/reaction은 비운 SYSTEM tombstone으로 바꾼다.
- reaction은 하나의 extended grapheme cluster, 최대 32 code point다.
- read watermark는 thread에 속한 positive message ID이고 낮거나 같은 값은 state-idempotent no-op다.

## 5. Subscription namespace

다음 세 subscription namespace만 지원한다.

```text
/topic/community/threads/{threadId}/members/{memberId}/events
/topic/community/members/{memberId}/events
/user/queue/errors
```

thread subscription path의 `{memberId}`는 authenticated principal과 같아야 하고 ACTIVE Community
membership 및 non-deleted thread를 동시에 만족해야 한다. personal topic도 자기 member ID만 허용한다.
공유 `/topic/community/threads/{threadId}/events`는 사용하지 않는다.

## 6. Event와 ACK

state event envelope의 payload는 `Map<String,Object>`가 아닌 typed record다. 지원 event type은 다음과
같다.

```text
command.acknowledged
message.created       message.updated       message.deleted
reaction.changed       read.updated
thread.invited         thread.updated       thread.deleted
member.kicked          member.left
```

`command.acknowledged`는 caller의 personal member topic으로 best-effort 전송하며 `commandId`, command,
affected IDs, nullable `messageId`/`clientMessageId`, `deduplicated`를 포함한다. state 저장의 ACK로
간주하지 않는다. command 처리 후 commit된 경우에만 전송하며 state event와 순서는 보장하지 않는다.

payload는 다음 typed record와 일치한다. `Map<String,Object>` envelope나 global serializer override는
사용하지 않는다.

| type | payload record/fields |
| --- | --- |
| `command.acknowledged` | `CommunityCommandAcknowledgement(commandId, command, messageId, clientMessageId, deduplicated)` |
| `message.created` | `MessageCreated(message, clientMessageId)` |
| `message.updated` / `message.deleted` | `MessageUpdated(message)` / `MessageDeleted(message)` |
| `reaction.changed` | `ReactionChanged(messageId, reactions)` |
| `read.updated` | `ReadUpdated(memberId, lastReadMessageId)` |
| `thread.invited` | `ThreadInvited(thread summary)` |
| `thread.updated` | `ThreadUpdated(threadId, title, description, category, icon, memberCount, maxMembers, lastActivityAt, updatedAt)` |
| `thread.deleted` | `ThreadDeleted(threadId, deletedAt)` |
| `member.kicked` / `member.left` | `MemberKicked(memberId, memberCount)` / `MemberLeft(memberId, memberCount)` |

recoverable error는 `/user/queue/errors`로 보낸다. payload는 nullable parsed `commandId`, optional
`clientMessageId`, HTTP-like `status`, stable `code`/`message`, `retryable`을 포함하고 session은
닫지 않는다. missing/malformed command ID, validation, permission, not-found, conflict, application
failure와 rate-limit(typed 429)을 포함한다. rate-limit은 해당 command만 drop하고 같은 session을
살린다.

## 7. Lifecycle와 recovery

ACTIVE OWNER/ADMIN/MEMBER만 send/reply/mention/react/read/report를 수행한다. author만 edit할 수 있고,
author 또는 OWNER/ADMIN만 tombstone할 수 있다. OWNER가 leave/kick/demote하려면 먼저 atomic ownership
transfer가 필요하다. LEFT는 재초대할 수 있지만 KICKED는 재초대할 수 없다.

thread soft delete 뒤에는 모든 REST/STOMP access가 거부되지만 Chat row와 history는 보존된다.
message/reaction/read gap은 history/latest query로, metadata/member/settings gap은 thread detail/list/
member query로 복구한다. kick/leave/delete의 terminal event는 broker replay 대상이 아니므로 REST가
terminal state를 반영하거나 거부하는지 확인한다.

fan-out은 transaction outbox commit 이후에 수행한다. relay는 at-least-once이며 stable event ID와
partial-fan-out retry를 사용한다. broker failure는 original business row를 rollback하지 않는다.

outbox relay-enabled 경로에서 `EventOutboxRelayService`가 commit 이후 event를 publish해
`CommunityThreadRealtimeEventListener`를 호출하고,
`CommunityThreadRealtimeDelivery`는 모든 recipient를 시도한 뒤 실패를 aggregate/rethrow한다. 따라서
message/reaction/read gap은 history endpoint로, metadata/member/settings gap은 thread detail/list/member
query로 backfill한다. kick/leave/delete terminal event는 broker replay가 아니라 REST terminal state를
기준으로 한다.

## Broker 운영 전제와 merge/deploy blocker

- local/test는 `app.websocket.broker.mode=SIMPLE`을 허용한다. `dev`/`prod`는 shared external STOMP
  relay(`RELAY`)만 허용하며 `WebSocketBrokerPropertiesValidator`가 simple mode를 거부하고
  host/virtual-host/system·client credentials 누락과 port 범위를 fail-fast로 검증한다.
- dev/prod relay는 `tls-enabled=true`여야 한다. `StompRelayTcpClientFactory`가 TLS hostname
  verification(`HTTPS`)을 사용하고 `WebSocketBrokerProperties.Relay.toString()`은 credential을
  redacted한다. secret은 환경/secret injection으로만 주입하고 로그에 남기지 않는다.
- `StompBrokerRelayMonitor` availability/reconnect와 `WebSocketBrokerRelayStartupValidator`의
  `startup-timeout` 내 readiness를 확인한다. RabbitMQ STOMP plugin/relay provisioning, secret injection,
  ALB SockJS fallback stickiness, configured heartbeat보다 긴 idle timeout, multi-instance readiness/전환
  증거는 로컬에서 입증할 수 없는 외부 인프라 산출물이며, 누락 시 PR3 merge/deploy blocker다.

## Metrics cardinality

`CommunityThreadRealtimeMetrics`의 send/reject/rate-limit/fan-out/broadcast-failure/backfill metric은
`operation`, `outcome`, `reason` 같은 고정 bucket만 태그한다. `threadId`, `memberId`, `messageId`,
`eventId`, raw destination을 IDs-as-tags로 사용하지 않는다. relay availability/reconnect와 outbox
retry/failed metric도 low-cardinality를 유지한다. Community metric 이름은
`community.thread.realtime.send.commands`, `.reject.commands`, `.rate.limit.rejections`,
`.fanout.events`, `.fanout.recipients`, `.broadcast.failures`, `.backfill.requests`로 고정한다.

## 8. 알람 발송 seam

이 계약은 알람을 발송하지 않는다. 미래 알람 consumer용 business fact만 ID-only snapshot으로 제공한다.

- `CommunityThreadInvitedEvent`
- `CommunityThreadMessageCreatedEvent`
- `CommunityThreadMentionedEvent`

세 event에는 message text, thread title, member name, token, deeplink, provider payload,
mute/offline/DND decision 또는 notification dependency를 넣지 않는다. `CommunityThreadInvitedEvent`는
lifecycle `thread.invited` realtime source이기도 하지만, 나머지 두 fact는 Chat generic realtime
event와 분리되어 realtime delivery를 대체하거나 다시 소비하는 source가 아니다.
