# Community 도메인 온보딩

## 현재 책임

Community는 게시글(`Post`), 댓글(`Comment`), 스크랩(`Scrap`), 신고(`Report`)와 초대 전용
thread(`CommunityThread`, `CommunityThreadMember`)의 business 정책을 소유한다. 주차별 우수 워크북은
Curriculum이 소유한다.

thread의 메타데이터, category, membership/role/state, invite eligibility, capacity, pin/mute,
unread/last-activity projection, report와 외부 API/destination은 Community 책임이다. 메시지, 답장,
첨부, mention, reaction, edit/tombstone, read watermark의 불변식은 재사용되는 Chat engine 책임이다.

## 경계와 persistence

- `CommunityThread`가 `chatRoomId`를 unique scalar로 저장한다. Community와 Chat 사이에 JPA relation,
  cross-domain FK, entity 참조를 만들지 않는다.
- `CommunityThread`와 `CommunityThreadMember`는 domain 패키지의 직접 JPA entity이며 `BaseEntity`를
  상속한다. `@OneToMany` 컬렉션은 금지한다.
- Community application service는 Member/Storage/Chat의 공개 UseCase와 ID만 사용한다. Controller가
  foreign repository/entity 또는 Chat adapter/in을 호출하지 않는다.
- thread soft delete는 Chat room을 hard delete하지 않는다. Chat history가 reconnect/backfill의
  source of truth로 남는다.

## API 책임 구분

### REST (`/api/v1/community`)

REST는 thread control/query/recovery/moderation을 담당한다.

- thread list/create/detail/update/soft-delete
- pin/mute, member list/invitable search, invite/kick/leave/role change
- newest-first message history cursor query
- thread-message report와 `/api/v1/community/admin/thread-message-reports` inbox

메시지 create/edit/tombstone, reaction add/remove, read watermark mutation은 REST에 추가하지 않는다.
admin path는 반드시 `/api/v1/community/admin/...`이어야 하며 top-level `/api/v1/admin/...`는 금지한다.

현재 retained REST inventory는 다음과 같다.

| Method | Path |
| --- | --- |
| GET, POST | `/api/v1/community/threads` |
| GET, PATCH, DELETE | `/api/v1/community/threads/{threadId}` |
| POST, DELETE | `/api/v1/community/threads/{threadId}/pin` |
| POST, DELETE | `/api/v1/community/threads/{threadId}/mute` |
| GET | `/api/v1/community/threads/{threadId}/members` |
| GET | `/api/v1/community/threads/{threadId}/invitable` |
| POST | `/api/v1/community/threads/{threadId}/invite` |
| DELETE | `/api/v1/community/threads/{threadId}/members/{memberId}` |
| POST | `/api/v1/community/threads/{threadId}/leave` |
| PATCH | `/api/v1/community/threads/{threadId}/members/{memberId}/role` |
| GET | `/api/v1/community/threads/{threadId}/messages` |
| POST | `/api/v1/community/messages/{messageId}/report` |
| GET | `/api/v1/community/admin/thread-message-reports` |

이 목록 밖의 message/reaction/read REST mutation과 top-level admin route는 forbidden surface다.

### WebSocket/STOMP (`/ws`)

메시지와 읽음·reaction의 interactive mutation은 STOMP SEND만 제공한다. native CONNECT에 JWT를 넣고,
모든 SEND에 canonical lowercase UUID `x-command-id`를 넣는다.

```text
/app/community/threads/{threadId}/messages
/app/community/threads/{threadId}/messages/{messageId}/edit
/app/community/threads/{threadId}/messages/{messageId}/delete
/app/community/threads/{threadId}/messages/{messageId}/reactions/add
/app/community/threads/{threadId}/messages/{messageId}/reactions/remove
/app/community/threads/{threadId}/read
```

구독은 다음 두 user destination만 허용한다.

```text
/user/queue/community/threads/events
/user/queue/errors
```

event queue는 정상 상태 event와 caller ACK를 모두 전달하고 error queue는 recoverable error만
전달한다. client는 각 session에서 두 queue를 한 번씩 구독하고 event의 `threadId`와
`type`으로 분류한다. 한 회원의 여러 활성 session이 같은 user destination을 구독하면
모든 session이 전달 대상이다.

공용 Thread topic은 사용하지 않는다. event마다 ACTIVE 멤버를 다시 계산해 사용자별로
fan-out하므로 kick/leave 후 stale subscription이 남아도 후속 event를 받지 않는다.

## Realtime와 복구

Chat generic message/reaction/read event는 Community facade가 thread payload로 바꾸고, invite·role·
member·thread lifecycle event는 Community가 만든다. 각 state event는 stable `eventId`, typed payload,
string `threadId`, `occurredAt`를 가진다. caller ACK는 같은 Community Thread event queue에
best-effort로만 전송하며 storage
ack로 간주하지 않는다.

실제 변환 경로는 `CommunityThreadRealtimeEventListener` →
`CommunityThreadRealtimeFanOutService` → `CommunityThreadChatRealtimeRelay` 또는
`CommunityThreadLifecycleRealtimeRelay` → `CommunityThreadRealtimeDelivery` →
`CommunityThreadRealtimeBroadcastAdapter`다. envelope는
`CommunityThreadRealtimeEvent`와 `CommunityThreadRealtimePayload`의 typed record를 사용한다.

| type | typed payload |
| --- | --- |
| `command.acknowledged` | `CommunityCommandAcknowledgement(commandId, command, messageId, clientMessageId, deduplicated)` |
| `message.created` | `MessageCreated(message, clientMessageId)` |
| `message.updated`, `message.deleted` | `MessageUpdated(message)`, `MessageDeleted(message)` |
| `reaction.changed` | `ReactionChanged(messageId, reactions)` |
| `read.updated` | `ReadUpdated(memberId, lastReadMessageId)` |
| `thread.invited` | `ThreadInvited(thread summary)` |
| `thread.updated` | `ThreadUpdated(threadId, metadata, memberCount, maxMembers, activity timestamps)` |
| `thread.deleted` | `ThreadDeleted(threadId, deletedAt)` |
| `member.kicked`, `member.left` | `MemberKicked(memberId, memberCount)`, `MemberLeft(memberId, memberCount)` |

`/user/queue/errors`는 validation, permission, conflict, not-found, application failure, rate-limit을
session 종료 없이 전달한다. 21번째 SEND는 correlated typed 429로 거부하고 다음 rate bucket에서 같은
session이 회복되어야 한다. broker 장애나 disconnect는 business transaction을 rollback하지 않으며,
재접속한 client는 thread detail/member/history REST query로 backfill한다.

outbox relay는 commit 이후 동작한다. recipient별 fan-out은 모두 시도한 뒤 실패를 집계하고 stable
event ID를 유지한 채 retry한다. delivery는 best-effort이므로 REST query가 최종 상태의 source of truth다.

message/reaction/read gap은 `GET /api/v1/community/threads/{threadId}/messages`의 newest-first
history cursor로 복구하고, metadata/member/settings gap은 thread detail/list/member query로 복구한다.
kick/leave/delete terminal event는 broker replay를 전제하지 않고 REST가 terminal 상태를 반영하거나
거부한다. recoverable command error는 `WebSocketErrorPayload`의 nullable `commandId`/`clientMessageId`,
numeric status, stable code/message, retryable을 `/user/queue/errors`로 보내며 session을 닫지 않는다.

## 알람 seam

알람 발송은 이번 범위에서 구현하지 않는다. 미래 consumer를 위해
`CommunityThreadInvitedEvent`, `CommunityThreadMessageCreatedEvent`, `CommunityThreadMentionedEvent`를
ID-only immutable snapshot으로 발행할 수 있는 seam만 둔다. text, title, name, token, deeplink, provider
payload, mute/offline/DND 결정과 notification dependency는 event에 넣지 않는다.
`CommunityThreadInvitedEvent`는 lifecycle `thread.invited` realtime source이기도 하며,
`CommunityThreadMessageCreatedEvent`와 `CommunityThreadMentionedEvent`만 Chat generic realtime
event와 분리된 alarm-ready snapshot으로 realtime relay가 다시 소비하지 않는다.

`CommunityThreadMessageCreatedEvent`는 thread/message/sender/recipient member ID snapshot,
`CommunityThreadMentionedEvent`는 thread/message/sender/mentioned member ID snapshot,
`CommunityThreadInvitedEvent`는 thread/inviter/invited member ID snapshot만 보관한다. 알림 발송, FCM/APNs,
token/deeplink consumer는 이 문서의 구현 범위가 아니다.

## Broker와 운영 조건

- 단일 application instance 운영에서는 모든 프로필이 `app.websocket.broker.mode=SIMPLE`을 사용할 수
  있다. `WebSocketBrokerPropertiesValidator`는 `RELAY`를 선택한 경우에만 relay
  host/virtual-host/system·client credentials 누락과 port 범위를 startup에서 fail-fast로 검증한다.
- `dev`/`prod`에서 `RELAY`를 선택하면 `tls-enabled=true`를 요구한다.
  `StompRelayTcpClientFactory`가 TLS hostname verification(`HTTPS`)을 적용하고,
  `WebSocketBrokerProperties.Relay.toString()`은 credentials를 redacted한다. 비밀번호는
  환경변수/secret injection으로만 주입하고 로그에 남기지 않는다.
- relay 운영에서는 `StompBrokerRelayMonitor`의 availability/reconnect metric과
  `WebSocketBrokerRelayStartupValidator`의 startup-timeout readiness 검증을 통과해야 한다.
- simple broker의 구독과 session registry는 instance-local이다. rolling deploy의 일시적 instance 중첩을
  포함해 다중 instance에서 무손실 실시간 전달이 필요해지면, 공유 RabbitMQ STOMP plugin/relay,
  secret injection, ALB SockJS fallback stickiness, configured heartbeat보다 긴 idle timeout과 전환
  검증을 scale-out 선행 조건으로 갖춘다. simple 운영 중 연결 종료·전달 공백은 client reconnect와 REST
  backfill로 복구한다.

## Observability 규칙

`CommunityThreadRealtimeMetrics`의 send/reject/rate-limit/fan-out/broadcast-failure/backfill metric은
`operation`, `outcome`, `reason`처럼 유한한 bucket만 tag로 사용한다. `threadId`, `memberId`,
`messageId`, `eventId` 및 raw destination을 tag로 넣지 않는다. broker relay availability/reconnect와
event-outbox retry/failed metric에도 ID를 태그하지 않는 low-cardinality 원칙을 적용한다. Community
metric 이름은 `community.thread.realtime.send.commands`, `.reject.commands`,
`.rate.limit.rejections`, `.fanout.events`, `.fanout.recipients`, `.broadcast.failures`,
`.backfill.requests`로 고정한다.

## 테스트를 시작할 위치

| 확인할 관심사 | 주요 위치 |
| --- | --- |
| entity invariant/unique/index/capacity | `community/domain`, `community/adapter/out/persistence` |
| lifecycle/permission/lock/idempotency | `community/application/service` |
| REST contract와 recovery | `community/adapter/in/web` |
| STOMP command/subscription/relay | `community/adapter/in/websocket`, `global/websocket` |
| source boundary/금지 surface | `CommunityThreadArchitectureTest` |

GraphQL Community API는 이 thread 계약의 문서 산출물에 포함하지 않는다.
